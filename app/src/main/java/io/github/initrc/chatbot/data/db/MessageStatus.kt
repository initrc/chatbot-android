package io.github.initrc.chatbot.data.db

enum class MessageStatus(val value: String) {
    COMPLETE("complete"),
    STREAMING("streaming"),
    FAILED("failed");

    companion object {
        fun fromValue(value: String): MessageStatus {
            return entries.firstOrNull { it.value == value }
                ?: error("Unknown message status: $value")
        }
    }
}
