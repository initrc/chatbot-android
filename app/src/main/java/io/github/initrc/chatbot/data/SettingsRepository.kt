package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource
) {

    suspend fun getAllModels(): List<String> {
        return remoteDataSource.getAllModels()
    }

    suspend fun getCurrentModel(): String {
        return localDataSource.getCurrentModel()
    }

    suspend fun setCurrentModel(model: String) {
        localDataSource.setCurrentModel(model)
    }
}