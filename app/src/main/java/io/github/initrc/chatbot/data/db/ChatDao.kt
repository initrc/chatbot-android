package io.github.initrc.chatbot.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        """
        SELECT id, title, lastMessagePreview AS preview, updatedAtMillis, messageCount
        FROM conversations
        WHERE messageCount > 0
        ORDER BY updatedAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentConversations(limit: Int): Flow<List<ConversationSummary>>

    @Query(
        """
        SELECT *
        FROM conversations
        WHERE id = :conversationId
        """
    )
    suspend fun getConversation(conversationId: String): ConversationEntity?

    @Query(
        """
        SELECT *
        FROM messages
        WHERE conversationId = :conversationId
        ORDER BY position ASC
        """
    )
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT *
        FROM messages
        WHERE conversationId = :conversationId
            AND status = 'complete'
        ORDER BY position ASC
        """
    )
    suspend fun getCompleteMessagesSnapshot(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConversation(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(entity: MessageEntity)

    @Query(
        """
        UPDATE messages
        SET content = :content,
            updatedAtMillis = :updatedAtMillis,
            status = :status
        WHERE id = :messageId
        """
    )
    suspend fun updateMessageContent(
        messageId: String,
        content: String,
        updatedAtMillis: Long,
        status: String,
    ): Int

    @Query(
        """
        UPDATE conversations
        SET title = :title,
            lastMessagePreview = :preview,
            updatedAtMillis = :updatedAtMillis,
            messageCount = messageCount + :messageDelta
        WHERE id = :conversationId
        """
    )
    suspend fun updateConversationSummary(
        conversationId: String,
        title: String,
        preview: String?,
        updatedAtMillis: Long,
        messageDelta: Int,
    ): Int

    @Query(
        """
        DELETE FROM conversations
        WHERE id = :conversationId
        """
    )
    suspend fun deleteConversation(conversationId: String): Int
}
