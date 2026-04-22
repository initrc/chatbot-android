package io.github.initrc.chatbot.data

import androidx.room.withTransaction
import io.github.initrc.chatbot.data.db.ChatDao
import io.github.initrc.chatbot.data.db.ChatDatabase
import io.github.initrc.chatbot.data.db.ConversationEntity
import io.github.initrc.chatbot.data.db.ConversationSummary
import io.github.initrc.chatbot.data.db.MessageEntity
import io.github.initrc.chatbot.data.db.MessageStatus
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DEFAULT_TITLE = "New chat"
private const val DEFAULT_RECENT_CONVERSATION_LIMIT = 30
private const val MAX_TITLE_LENGTH = 48
private const val MAX_PREVIEW_LENGTH = 96

class ChatHistoryLocalDataSource @Inject constructor(
    private val database: ChatDatabase,
    private val chatDao: ChatDao,
) {
    fun observeRecentConversations(
        limit: Int = DEFAULT_RECENT_CONVERSATION_LIMIT,
    ): Flow<List<ConversationSummary>> {
        return chatDao.observeRecentConversations(limit)
    }

    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return chatDao.observeMessages(conversationId)
            .map { entities -> entities.toMessages() }
    }

    suspend fun getCompleteMessagesSnapshot(conversationId: String): List<Message> {
        return chatDao.getCompleteMessagesSnapshot(conversationId).toMessages()
    }

    suspend fun createConversation(
        model: String,
        id: String = UUID.randomUUID().toString(),
        nowMillis: Long = System.currentTimeMillis(),
    ): ConversationEntity {
        val conversation = ConversationEntity(
            id = id,
            title = DEFAULT_TITLE,
            model = model,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
            lastMessagePreview = null,
            messageCount = 0,
        )
        database.withTransaction {
            chatDao.insertConversation(conversation)
        }
        return conversation
    }

    suspend fun appendMessage(
        conversationId: String,
        message: Message,
        status: MessageStatus = MessageStatus.COMPLETE,
        messageId: String = UUID.randomUUID().toString(),
        nowMillis: Long = System.currentTimeMillis(),
    ): MessageEntity = database.withTransaction {
        val conversation = chatDao.requireConversation(conversationId)
        val entity = message.toMessageEntity(
            id = messageId,
            conversationId = conversationId,
            position = conversation.messageCount,
            createdAtMillis = nowMillis,
            status = status,
        )
        chatDao.insertMessage(entity)
        chatDao.updateConversationSummary(
            conversationId = conversationId,
            title = conversation.titleWithInitialUserMessage(message),
            preview = message.content.previewOrNull() ?: conversation.lastMessagePreview,
            updatedAtMillis = nowMillis,
            messageDelta = 1,
        )
        entity
    }

    suspend fun updateMessageContent(
        conversationId: String,
        messageId: String,
        content: String,
        status: MessageStatus,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = database.withTransaction {
        val updatedRows = chatDao.updateMessageContent(
            conversationId = conversationId,
            messageId = messageId,
            content = content,
            updatedAtMillis = nowMillis,
            status = status.value,
        )
        if (updatedRows == 0) {
            return@withTransaction false
        }

        val conversation = chatDao.requireConversation(conversationId)
        chatDao.updateConversationSummary(
            conversationId = conversationId,
            title = conversation.title,
            preview = content.previewOrNull() ?: conversation.lastMessagePreview,
            updatedAtMillis = nowMillis,
            messageDelta = 0,
        )
        true
    }

    suspend fun deleteConversation(conversationId: String): Boolean = chatDao.deleteConversation(conversationId) > 0

    private suspend fun ChatDao.requireConversation(conversationId: String): ConversationEntity {
        return getConversation(conversationId)
            ?: error("Conversation not found: $conversationId")
    }

    private fun ConversationEntity.titleWithInitialUserMessage(message: Message): String {
        if (title != DEFAULT_TITLE || message.role != ChatRole.USER) {
            return title
        }
        return message.content.toDisplaySnippet(MAX_TITLE_LENGTH) ?: title
    }
}

private fun String.previewOrNull(): String? = toDisplaySnippet(MAX_PREVIEW_LENGTH)

private fun String.toDisplaySnippet(maxLength: Int): String? {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isEmpty()) {
        return null
    }
    if (normalized.length <= maxLength) {
        return normalized
    }
    return normalized.take(maxLength - 3).trimEnd() + "..."
}
