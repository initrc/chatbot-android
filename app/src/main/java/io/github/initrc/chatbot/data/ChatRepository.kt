package io.github.initrc.chatbot.data

import io.github.initrc.chatbot.data.db.ConversationSummary
import io.github.initrc.chatbot.data.db.MessageStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val STREAMING_PERSIST_INTERVAL_MILLIS = 100L

class ChatRepository @Inject constructor(
    private val remoteDataSource: ChatRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val contextCompressor: ChatContextCompressor,
    private val historyLocalDataSource: ChatHistoryLocalDataSource,
) {
    fun observeRecentConversations(limit: Int = 30): Flow<List<ConversationSummary>> {
        return historyLocalDataSource.observeRecentConversations(limit)
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return historyLocalDataSource.observeMessages(conversationId)
    }

    suspend fun createConversation(model: String): String {
        return historyLocalDataSource.createConversation(model).id
    }

    suspend fun deleteConversation(conversationId: String): Boolean {
        return historyLocalDataSource.deleteConversation(conversationId)
    }

    suspend fun sendMessage(messages: List<Message>, model: String): Flow<String> {
        return sendCompressedMessages(messages, model)
    }

    suspend fun sendMessage(
        conversationId: String,
        inputText: String,
        model: String,
    ): Flow<String> {
        val promptText = inputText.trim()
        if (promptText.isEmpty()) {
            return emptyFlow()
        }

        return flow {
            historyLocalDataSource.appendMessage(
                conversationId = conversationId,
                message = Message(role = ChatRole.USER, content = promptText),
            )
            val assistantMessage = historyLocalDataSource.appendMessage(
                conversationId = conversationId,
                message = Message(role = ChatRole.ASSISTANT, content = ""),
                status = MessageStatus.STREAMING,
            )

            var assistantText = ""
            var lastPersistedAtMillis = 0L

            try {
                val requestMessages = historyLocalDataSource.getCompleteMessagesSnapshot(conversationId)
                sendCompressedMessages(requestMessages, model).collect { chunk ->
                    assistantText += chunk
                    emit(chunk)

                    val nowMillis = System.currentTimeMillis()
                    if (nowMillis - lastPersistedAtMillis >= STREAMING_PERSIST_INTERVAL_MILLIS) {
                        historyLocalDataSource.updateMessageContent(
                            conversationId = conversationId,
                            messageId = assistantMessage.id,
                            content = assistantText,
                            status = MessageStatus.STREAMING,
                            nowMillis = nowMillis,
                        )
                        lastPersistedAtMillis = nowMillis
                    }
                }

                historyLocalDataSource.updateMessageContent(
                    conversationId = conversationId,
                    messageId = assistantMessage.id,
                    content = assistantText,
                    status = MessageStatus.COMPLETE,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                historyLocalDataSource.updateMessageContent(
                    conversationId = conversationId,
                    messageId = assistantMessage.id,
                    content = assistantText,
                    status = MessageStatus.FAILED,
                )
                throw e
            }
        }
    }

    private suspend fun sendCompressedMessages(
        messages: List<Message>,
        model: String,
    ): Flow<String> {
        val contextWindow = settingsRepository.getModelContextWindow(model)
        val compressedMessages = contextCompressor.compress(messages, contextWindow)
        return remoteDataSource.sendMessage(
            messages = compressedMessages,
            model = model,
            maxCompletionTokens = contextCompressor.maxCompletionTokens(contextWindow),
        )
    }
}
