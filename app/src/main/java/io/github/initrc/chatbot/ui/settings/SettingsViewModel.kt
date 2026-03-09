package io.github.initrc.chatbot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.initrc.chatbot.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _currentModel = MutableStateFlow("")
    val currentModel: StateFlow<String> = _currentModel

    private val _allModels = MutableStateFlow(listOf<String>())
    val allModels: StateFlow<List<String>> = _allModels

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl

    init {
        viewModelScope.launch {
            _apiKey.value = settingsRepository.getApiKey()
            _baseUrl.value = settingsRepository.getBaseUrl()
            _allModels.value = settingsRepository.getAllModels()
            _currentModel.value = settingsRepository.getCurrentModel()
        }
    }

    fun setCurrentModel(model: String) {
        viewModelScope.launch {
            settingsRepository.setCurrentModel(model)
            _currentModel.value = model
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setApiKey(key)
            _apiKey.value = key
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setBaseUrl(url)
            _baseUrl.value = url
        }
    }
}