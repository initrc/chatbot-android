package io.github.initrc.chatbot.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatLocalDataSource : ChatService {
    override suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        maxCompletionTokens: Int?,
    ): Flow<String> = flow {
        delay(500) // simulate thinking time
        for (word in messages.last().content.split(" ")) {
            repeat(word.length) { emit("$word ") }
            delay(100)
        }
    }
}
