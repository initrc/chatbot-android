package io.github.initrc.chatbot.data

import kotlinx.coroutines.flow.Flow

interface SettingsService {
    suspend fun getAllModels(): List<String>
    suspend fun getCurrentModel(): String
}