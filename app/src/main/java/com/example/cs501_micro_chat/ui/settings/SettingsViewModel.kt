package com.example.cs501_micro_chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.preferences.LanguagePreferencesRepository
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val interfaceLanguage: LanguageOption = LanguageOption.English,
    val translationLanguage: LanguageOption = LanguageOption.English,
    val autoTranslateEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePreferencesRepository: LanguagePreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                languagePreferencesRepository.interfaceLanguage,
                languagePreferencesRepository.translationLanguage,
                languagePreferencesRepository.autoTranslateEnabled
            ) { interfaceLanguage, translationLanguage, autoTranslate ->
                SettingsUiState(
                    interfaceLanguage = interfaceLanguage,
                    translationLanguage = translationLanguage,
                    autoTranslateEnabled = autoTranslate
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    fun selectInterfaceLanguage(option: LanguageOption) {
        viewModelScope.launch {
            languagePreferencesRepository.setInterfaceLanguage(option)
        }
    }

    fun selectTranslationLanguage(option: LanguageOption) {
        viewModelScope.launch {
            languagePreferencesRepository.setTranslationLanguage(option)
        }
    }

    fun toggleAutoTranslate(enabled: Boolean) {
        viewModelScope.launch {
            languagePreferencesRepository.setAutoTranslateEnabled(enabled)
        }
    }
}
