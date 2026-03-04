package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow

interface ChatService {
    suspend fun sendMessage(prompt: String): Flow<String>
}