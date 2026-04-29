package io.github.initrc.chatbot.data

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechToTextRemoteDataSourceTest {

    @Test
    fun createSpeechTranscriptionRequestUsesGroqMultipartFields() {
        val audioFile = File.createTempFile("speech-", ".m4a")
        val audioBytes = byteArrayOf(1, 2, 3, 4)
        audioFile.writeBytes(audioBytes)

        try {
            val request = createSpeechTranscriptionRequest(audioFile)

            assertEquals(audioFile.name, request.fileName)
            assertArrayEquals(audioBytes, request.fileBytes)
            assertEquals("audio/mp4", request.audioContentType)
            assertEquals("whisper-large-v3-turbo", request.model)
            assertEquals("text", request.responseFormat)
            assertTrue(
                request.toMultipartContent().contentType.toString()
                    .startsWith("multipart/form-data")
            )
        } finally {
            audioFile.delete()
        }
    }

    @Test
    fun createSpeechTranscriptionRequestRejectsMissingAudioFile() {
        val missingAudioFile = File.createTempFile("speech-", ".m4a")
        missingAudioFile.delete()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            createSpeechTranscriptionRequest(missingAudioFile)
        }

        assertTrue(exception.message.orEmpty().contains(missingAudioFile.name))
    }

    @Test
    fun speechTranscriptionEndpointAppendsAudioTranscriptionsPath() {
        assertEquals(
            "https://api.groq.com/openai/v1/audio/transcriptions",
            speechTranscriptionEndpoint("https://api.groq.com/openai/v1")
        )
    }

    @Test
    fun parseSpeechTranscriptionResponseTrimsTextResponse() {
        assertEquals(
            "turn on the porch light",
            parseSpeechTranscriptionResponse("\n  turn on the porch light  \n")
        )
    }
}
