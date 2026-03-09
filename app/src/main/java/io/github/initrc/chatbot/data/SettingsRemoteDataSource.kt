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

private const val BASE_URL = "https://api.groq.com/openai/v1"
private const val API_KEY = ""

@Singleton
class SettingsRemoteDataSource @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getAllModels(): List<String> {
        val response: ModelsResponse = client.get("$BASE_URL/models") {
            header("Authorization", "Bearer $API_KEY")
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
