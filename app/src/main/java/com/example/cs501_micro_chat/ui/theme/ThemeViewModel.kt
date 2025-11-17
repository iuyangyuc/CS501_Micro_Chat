package com.example.cs501_micro_chat.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.preferences.LanguagePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: LanguagePreferencesRepository
) : ViewModel() {

    val themeOption: StateFlow<ThemeOption> = preferencesRepository.themeOption
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeOption.SYSTEM
        )

    fun selectTheme(option: ThemeOption) {
        viewModelScope.launch {
            preferencesRepository.setThemeOption(option)
        }
    }
}
