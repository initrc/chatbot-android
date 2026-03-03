package io.github.initrc.chatbot.data

interface ChatService {
    suspend fun sendMessage(message: String): String
}