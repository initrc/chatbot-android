package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepository @Inject constructor() {
    private val localDataSource = ChatLocalDataSource()

    suspend fun sendMessage(prompt: String): Flow<String> {
        return localDataSource.sendMessage(prompt)
    }
}
