package io.github.initrc.chatbot.data

class ChatLocalDataSource : ChatService {
    override suspend fun sendMessage(message: String): String {
        return "$message!"
    }
}