package io.github.initrc.chatbot.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SettingsLocalDataSource : SettingsService {
    override suspend fun getAllModels(): List<String> = listOf(
        "llama-3.1-8b-instant",
        "openai/gpt-oss-120b"
    )

    override suspend fun getCurrentModel(): String {
        return "llama-3.1-8b-instant"
    }
}