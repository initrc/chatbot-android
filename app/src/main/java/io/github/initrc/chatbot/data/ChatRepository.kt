package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val remoteDataSource: ChatRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val contextCompressor: ChatContextCompressor,
) {
    suspend fun sendMessage(messages: List<Message>, model: String): Flow<String> {
        val contextWindow = settingsRepository.getModelContextWindow(model)
        val compressedMessages = contextCompressor.compress(messages, contextWindow)
        return remoteDataSource.sendMessage(
            messages = compressedMessages,
            model = model,
            maxCompletionTokens = contextCompressor.maxCompletionTokens(contextWindow),
        )
    }
}
