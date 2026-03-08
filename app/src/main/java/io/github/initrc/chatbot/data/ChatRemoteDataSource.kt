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

private const val BASE_URL = "https://api.groq.com/openai/v1"
private const val API_KEY = ""
private const val MODEL_NAME = "llama-3.1-8b-instant"
private const val TAG = "ChatRemoteDataSource"

private val json = Json { ignoreUnknownKeys = true }

class ChatRemoteDataSource : ChatService {

    val client = HttpClient(CIO) {
        install(SSE)
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun sendMessage(messages: List<Message>): Flow<String> = flow {
        try {
            client.sse(
                {
                    url("$BASE_URL/chat/completions")
                    method = HttpMethod.Post
                    header("Content-Type", "application/json")
                    header("Authorization", "Bearer $API_KEY")
                    setBody(createChatRequest(messages))
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

    private fun createChatRequest(messages: List<Message>) = ChatRequest(
        model = MODEL_NAME,
        messages = listOf(
            Message(role = ChatRole.SYSTEM, content = "You are a helpful assistant.")
        ) + messages,
        stream = true,
    )
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean
)

@Serializable
data class Message(
    val role: ChatRole,
    val content: String,
)

@Serializable
enum class ChatRole {
    @SerialName("system") SYSTEM,
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
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