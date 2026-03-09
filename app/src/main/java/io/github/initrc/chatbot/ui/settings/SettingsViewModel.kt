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

    init {
        viewModelScope.launch {
            _currentModel.value = settingsRepository.getCurrentModel()
        }
    }
}