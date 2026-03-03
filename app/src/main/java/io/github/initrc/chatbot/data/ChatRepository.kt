package io.github.initrc.chatbot.data

import javax.inject.Inject

class ChatRepository @Inject constructor() {
    private val localDataSource = ChatLocalDataSource()

    suspend fun sendMessage(message: String): String {
        return localDataSource.sendMessage(message)
    }
}
