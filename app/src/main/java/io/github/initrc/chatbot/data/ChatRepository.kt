package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepository @Inject constructor() {
    private val localDataSource = ChatLocalDataSource()
    private val remoteDataSource = ChatRemoteDataSource()

    suspend fun sendMessage(messages: List<Message>): Flow<String> {
        return remoteDataSource.sendMessage(messages)
    }
}
