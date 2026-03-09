package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsLocalDataSource @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun getCurrentModel(): String {
        return settingsDataStore.getCurrentModel()
    }

    suspend fun setCurrentModel(model: String) {
        settingsDataStore.setCurrentModel(model)
    }
}