package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource
) {

    suspend fun getAllModels(): List<String> {
        return remoteDataSource.getAllModels()
    }

    suspend fun getModelContextWindow(model: String): Int? {
        return remoteDataSource.getModelContextWindow(model)
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
        if (localDataSource.getApiKey() != key) {
            remoteDataSource.clearModelCache()
        }
        localDataSource.setApiKey(key)
    }

    suspend fun getBaseUrl(): String {
        return localDataSource.getBaseUrl()
    }

    suspend fun setBaseUrl(url: String) {
        if (localDataSource.getBaseUrl() != url) {
            remoteDataSource.clearModelCache()
        }
        localDataSource.setBaseUrl(url)
    }
}
