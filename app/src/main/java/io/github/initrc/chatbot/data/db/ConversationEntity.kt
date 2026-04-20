package io.github.initrc.chatbot.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val model: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastMessagePreview: String?,
    val messageCount: Int,
)
