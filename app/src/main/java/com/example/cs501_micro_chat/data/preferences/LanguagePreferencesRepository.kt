package com.example.cs501_micro_chat.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.theme.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languagePreferencesDataStore by preferencesDataStore(
    name = "language_preferences"
)

@Singleton
class LanguagePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val dataStore = context.languagePreferencesDataStore

    val interfaceLanguage: Flow<LanguageOption> = dataStore.data.map { preferences ->
        preferences[INTERFACE_LANGUAGE_KEY].toLanguageOption(LanguageOption.English)
    }

    val translationLanguage: Flow<LanguageOption> = dataStore.data.map { preferences ->
        preferences[TRANSLATION_LANGUAGE_KEY].toLanguageOption(LanguageOption.English)
    }

    val autoTranslateEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_TRANSLATE_KEY] ?: false
    }

    val themeOption: Flow<ThemeOption> = dataStore.data.map { preferences ->
        val stored = preferences[THEME_MODE_KEY]
        stored?.let { runCatching { ThemeOption.valueOf(it) }.getOrNull() } ?: ThemeOption.SYSTEM
    }

    suspend fun setInterfaceLanguage(option: LanguageOption) {
        dataStore.edit { preferences ->
            preferences[INTERFACE_LANGUAGE_KEY] = option.name
        }
    }

    suspend fun setTranslationLanguage(option: LanguageOption) {
        dataStore.edit { preferences ->
            preferences[TRANSLATION_LANGUAGE_KEY] = option.name
        }
    }

    suspend fun setAutoTranslateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_TRANSLATE_KEY] = enabled
        }
    }

    suspend fun setThemeOption(option: ThemeOption) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = option.name
        }
    }

    private fun String?.toLanguageOption(default: LanguageOption): LanguageOption {
        return this?.let { LanguageOption.fromName(it) } ?: default
    }

    private companion object {
        val INTERFACE_LANGUAGE_KEY: Preferences.Key<String> =
            stringPreferencesKey("interface_language")
        val TRANSLATION_LANGUAGE_KEY: Preferences.Key<String> =
            stringPreferencesKey("translation_language")
        val AUTO_TRANSLATE_KEY: Preferences.Key<Boolean> =
            booleanPreferencesKey("auto_translate_enabled")
        val THEME_MODE_KEY: Preferences.Key<String> =
            stringPreferencesKey("theme_mode")
    }
}
