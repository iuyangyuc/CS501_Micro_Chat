package com.example.cs501_micro_chat.ui.login

import androidx.annotation.StringRes
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.auth.AuthProvider

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loadingProvider: AuthProvider? = null,
    val errorMessage: String? = null,
    val agreementChecked: Boolean = false,
    val selectedLanguage: LanguageOption = LanguageOption.Chinese,
    val activeProvider: AuthProvider? = null
) {
    val isLoading: Boolean
        get() = loadingProvider != null

    val isEmailValid: Boolean
        get() = email.isNotBlank()

    val isPasswordValid: Boolean
        get() = password.isNotBlank()

    val isEmailFormReady: Boolean
        get() = activeProvider == AuthProvider.Email &&
            agreementChecked && isEmailValid && isPasswordValid && !isLoading
}

enum class LanguageOption(
    @StringRes val labelRes: Int,
    val languageTag: String
) {
    Chinese(R.string.login_language_chinese, "zh"),
    English(R.string.login_language_english, "en")
}
