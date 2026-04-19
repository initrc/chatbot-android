package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow

interface ChatService {
    suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        maxCompletionTokens: Int? = null,
    ): Flow<String>
}
