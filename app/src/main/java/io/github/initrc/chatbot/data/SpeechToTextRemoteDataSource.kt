package io.github.initrc.chatbot.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val SPEECH_TO_TEXT_MODEL = "whisper-large-v3-turbo"
private const val SPEECH_RESPONSE_FORMAT = "text"
private const val SPEECH_AUDIO_CONTENT_TYPE = "audio/mp4"

@Singleton
class SpeechToTextRemoteDataSource @Inject constructor(
    private val settingsLocalDataSource: SettingsLocalDataSource,
) : SpeechToTextService {
    private val client = HttpClient(CIO)

    override suspend fun transcribe(audioFile: File): String {
        require(audioFile.isFile) { "Audio file does not exist: ${audioFile.name}" }

        val apiKey = settingsLocalDataSource.getApiKey()
        val baseUrl = settingsLocalDataSource.getBaseUrl()

        val response: String = client.post("$baseUrl/audio/transcriptions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(createTranscriptionRequest(audioFile))
        }.body()

        return response.trim()
    }

    private fun createTranscriptionRequest(audioFile: File): MultiPartFormDataContent {
        return MultiPartFormDataContent(
            formData {
                append("file", audioFile.readBytes(), audioFileHeaders(audioFile))
                append("model", SPEECH_TO_TEXT_MODEL)
                append("response_format", SPEECH_RESPONSE_FORMAT)
            }
        )
    }

    private fun audioFileHeaders(audioFile: File): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, SPEECH_AUDIO_CONTENT_TYPE)
            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
        }
    }
}
