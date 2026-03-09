package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsLocalDataSource @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsService {
    override suspend fun getAllModels(): List<String> = listOf(
        "llama-3.1-8b-instant",
        "openai/gpt-oss-120b"
    )

    override suspend fun getCurrentModel(): String {
        return settingsDataStore.getCurrentModel()
    }

    override suspend fun setCurrentModel(model: String) {
        settingsDataStore.setCurrentModel(model)
    }
}