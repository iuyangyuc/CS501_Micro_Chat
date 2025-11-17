package com.example.cs501_micro_chat.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cs501_micro_chat.data.preferences.LanguagePreferencesRepository
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class LanguagePreferenceObserver @Inject constructor(
    private val repository: LanguagePreferencesRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            repository.interfaceLanguage.collectLatest { option ->
                applyLocale(option)
            }
        }
    }

    private fun applyLocale(option: LanguageOption) {
        val localeList = LocaleListCompat.forLanguageTags(option.languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
