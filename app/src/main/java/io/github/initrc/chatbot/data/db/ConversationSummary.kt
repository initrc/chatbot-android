package io.github.initrc.chatbot.data.db

data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String?,
    val updatedAtMillis: Long,
    val messageCount: Int,
)
