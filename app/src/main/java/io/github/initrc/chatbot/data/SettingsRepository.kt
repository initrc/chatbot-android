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

    suspend fun getApiKey(): String {
        return localDataSource.getApiKey()
    }

    suspend fun setApiKey(key: String) {
        localDataSource.setApiKey(key)
    }

    suspend fun getBaseUrl(): String {
        return localDataSource.getBaseUrl()
    }

    suspend fun setBaseUrl(url: String) {
        localDataSource.setBaseUrl(url)
    }
}