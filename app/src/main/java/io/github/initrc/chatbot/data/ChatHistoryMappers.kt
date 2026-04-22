package io.github.initrc.chatbot.data

import io.github.initrc.chatbot.data.db.MessageEntity
import io.github.initrc.chatbot.data.db.MessageStatus

fun MessageEntity.toMessage(): Message = Message(
    role = ChatRole.fromValue(role),
    content = content,
)

fun List<MessageEntity>.toMessages(): List<Message> = map { it.toMessage() }

fun Message.toMessageEntity(
    id: String,
    conversationId: String,
    position: Int,
    createdAtMillis: Long,
    updatedAtMillis: Long = createdAtMillis,
    status: MessageStatus = MessageStatus.COMPLETE,
): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    position = position,
    role = role.value,
    content = content,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    status = status.value,
)
