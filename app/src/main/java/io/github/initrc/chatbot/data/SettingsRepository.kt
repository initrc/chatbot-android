package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsRepository @Inject constructor(
    val localDataSource: SettingsLocalDataSource
) {

    // TODO: https://console.groq.com/docs/api-reference#models-retrieve
    suspend fun getAllModels(): List<String> {
        return localDataSource.getAllModels()
    }

    suspend fun getCurrentModel(): String {
        return localDataSource.getCurrentModel()
    }

    suspend fun setCurrentModel(model: String) {
        localDataSource.setCurrentModel(model)
    }
}