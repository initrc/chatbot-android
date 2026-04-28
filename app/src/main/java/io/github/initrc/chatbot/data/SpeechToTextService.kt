package io.github.initrc.chatbot.data

import java.io.File

interface SpeechToTextService {
    suspend fun transcribe(audioFile: File): String
}
