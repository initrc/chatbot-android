package io.github.initrc.chatbot.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRemoteDataSource @Inject constructor(
    private val settingsLocalDataSource: SettingsLocalDataSource
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val modelCache = mutableMapOf<String, Model>()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getAllModels(): List<String> {
        val apiKey = settingsLocalDataSource.getApiKey()
        val baseUrl = settingsLocalDataSource.getBaseUrl()

        val response: ModelsResponse = client.get("$baseUrl/models") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        modelCache.clear()
        response.data.forEach { modelCache[it.id] = it }
        return response.data.map { it.id }
    }

    suspend fun getModelContextWindow(modelId: String): Int? {
        modelCache[modelId]?.contextWindow?.let { return it }

        val apiKey = settingsLocalDataSource.getApiKey()
        val baseUrl = settingsLocalDataSource.getBaseUrl()

        return runCatching {
            val response: Model = client.get("$baseUrl/models/${modelId.asPathSegment()}") {
                header("Authorization", "Bearer $apiKey")
            }.body()
            modelCache[response.id] = response
            response.contextWindow
        }.getOrNull()
    }

    fun clearModelCache() {
        modelCache.clear()
    }

    private fun String.asPathSegment(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }
}

@Serializable
data class ModelsResponse(
    val data: List<Model>
)

@Serializable
data class Model(
    val id: String,
    @SerialName("context_window")
    val contextWindow: Int? = null,
)
