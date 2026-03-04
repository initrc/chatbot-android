package io.github.initrc.chatbot.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatLocalDataSource : ChatService {
    override suspend fun sendMessage(prompt: String): Flow<String> = flow {
        delay(500) // simulate thinking time
        for (word in prompt.split(" ")) {
            repeat(word.length) { emit("$word ") }
            delay(100)
        }
    }
}