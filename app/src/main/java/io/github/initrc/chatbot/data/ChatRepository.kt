package io.github.initrc.chatbot.data

class ChatRepository {
    private val localDataSource = ChatLocalDataSource()

    suspend fun sendMessage(message: String): String {
        return localDataSource.sendMessage(message)
    }
}
