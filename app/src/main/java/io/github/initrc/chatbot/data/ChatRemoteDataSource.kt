package io.github.initrc.chatbot.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatRemoteDataSource"

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class ChatRemoteDataSource @Inject constructor(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : ChatService {

    private fun createClient(): HttpClient = HttpClient(CIO) {
        install(SSE)
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        maxCompletionTokens: Int?,
    ): Flow<String> = flow {
        try {
            val apiKey = settingsLocalDataSource.getApiKey()
            val baseUrl = settingsLocalDataSource.getBaseUrl()
            val client = createClient()
            client.sse(
                {
                    url("$baseUrl/chat/completions")
                    method = HttpMethod.Post
                    header("Content-Type", "application/json")
                    header("Authorization", "Bearer $apiKey")
                    setBody(createChatRequest(messages, model, maxCompletionTokens))
                }
            ) {
                incoming.collect { event ->
                    val data = event.data ?: return@collect
                    if (data == "[DONE]") return@collect

                    try {
                        val chunk = json.decodeFromString<ChatCompletionChunk>(data)
                        val content = chunk.choices.firstOrNull()?.delta?.content
                        if (content != null) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chunk: $data", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during SSE request", e)
            throw e
        }
    }

    private fun createChatRequest(
        messages: List<Message>,
        model: String,
        maxCompletionTokens: Int?,
    ) = ChatRequest(
        model = model,
        messages = listOf(
            Message(role = ChatRole.SYSTEM, content = CHAT_SYSTEM_PROMPT)
        ) + messages,
        stream = true,
        maxCompletionTokens = maxCompletionTokens,
    )
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
)

@Serializable
data class Message(
    val role: ChatRole,
    val content: String,
)

@Serializable
enum class ChatRole(val value: String) {
    @SerialName("system") SYSTEM("system"),
    @SerialName("user") USER("user"),
    @SerialName("assistant") ASSISTANT("assistant");

    companion object {
        fun fromValue(value: String): ChatRole {
            return entries.firstOrNull { it.value == value }
                ?: error("Unknown chat role: $value")
        }
    }
}

@Serializable
data class ChatCompletionChunk(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val delta: Delta
)

@Serializable
data class Delta(
    val content: String? = null
)
