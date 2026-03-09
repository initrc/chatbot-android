package io.github.initrc.chatbot.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRemoteDataSource @Inject constructor(
    private val settingsLocalDataSource: SettingsLocalDataSource
) {

    private val json = Json { ignoreUnknownKeys = true }

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
        return response.data.map { it.id }
    }
}

@Serializable
data class ModelsResponse(
    val data: List<Model>
)

@Serializable
data class Model(
    val id: String,
)
