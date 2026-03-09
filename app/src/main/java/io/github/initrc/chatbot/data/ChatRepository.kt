package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    val remoteDataSource: ChatRemoteDataSource
) {
    suspend fun sendMessage(messages: List<Message>, model: String): Flow<String> {
        return remoteDataSource.sendMessage(messages, model)
    }
}
