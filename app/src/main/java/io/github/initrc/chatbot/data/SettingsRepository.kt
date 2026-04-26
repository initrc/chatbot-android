package io.github.initrc.chatbot.data

import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val localDataSource: SettingsLocalDataSource,
    private val remoteDataSource: SettingsRemoteDataSource
) {

    suspend fun getAllModels(): List<String> {
        if (!hasApiSettings()) return emptyList()
        return remoteDataSource.getAllModels()
    }

    suspend fun getModelContextWindow(model: String): Int? {
        if (!hasApiSettings()) return null
        return remoteDataSource.getModelContextWindow(model)
    }

    suspend fun hasApiSettings(): Boolean {
        return localDataSource.getApiKey().isNotBlank() &&
            localDataSource.getBaseUrl().isNotBlank()
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
        val trimmedKey = key.trim()
        if (localDataSource.getApiKey() != trimmedKey) {
            remoteDataSource.clearModelCache()
        }
        localDataSource.setApiKey(trimmedKey)
    }

    suspend fun getBaseUrl(): String {
        return localDataSource.getBaseUrl()
    }

    suspend fun setBaseUrl(url: String) {
        val trimmedUrl = url.trim()
        if (localDataSource.getBaseUrl() != trimmedUrl) {
            remoteDataSource.clearModelCache()
        }
        localDataSource.setBaseUrl(trimmedUrl)
    }

    suspend fun setApiSettings(apiKey: String, baseUrl: String) {
        val trimmedApiKey = apiKey.trim()
        val trimmedBaseUrl = baseUrl.trim()
        if (
            localDataSource.getApiKey() != trimmedApiKey ||
            localDataSource.getBaseUrl() != trimmedBaseUrl
        ) {
            remoteDataSource.clearModelCache()
        }
        localDataSource.setApiKey(trimmedApiKey)
        localDataSource.setBaseUrl(trimmedBaseUrl)
    }
}
