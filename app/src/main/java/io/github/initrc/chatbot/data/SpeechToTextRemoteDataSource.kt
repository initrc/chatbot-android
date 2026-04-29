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
        val apiKey = settingsLocalDataSource.getApiKey()
        val baseUrl = settingsLocalDataSource.getBaseUrl()

        val response: String = client.post(speechTranscriptionEndpoint(baseUrl)) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(createSpeechTranscriptionRequest(audioFile).toMultipartContent())
        }.body()

        return parseSpeechTranscriptionResponse(response)
    }
}

internal class SpeechTranscriptionRequest(
    val fileName: String,
    val fileBytes: ByteArray,
    val audioContentType: String = SPEECH_AUDIO_CONTENT_TYPE,
    val model: String = SPEECH_TO_TEXT_MODEL,
    val responseFormat: String = SPEECH_RESPONSE_FORMAT,
) {
    fun toMultipartContent(): MultiPartFormDataContent {
        return MultiPartFormDataContent(
            formData {
                append("file", fileBytes, audioFileHeaders())
                append("model", model)
                append("response_format", responseFormat)
            }
        )
    }

    private fun audioFileHeaders(): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, audioContentType)
            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
        }
    }
}

internal fun createSpeechTranscriptionRequest(audioFile: File): SpeechTranscriptionRequest {
    require(audioFile.isFile) { "Audio file does not exist: ${audioFile.name}" }

    return SpeechTranscriptionRequest(
        fileName = audioFile.name,
        fileBytes = audioFile.readBytes(),
    )
}

internal fun speechTranscriptionEndpoint(baseUrl: String): String {
    return "$baseUrl/audio/transcriptions"
}

internal fun parseSpeechTranscriptionResponse(response: String): String {
    return response.trim()
}
