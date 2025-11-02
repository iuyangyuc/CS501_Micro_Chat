package com.example.cs501_micro_chat.ui.signup

import com.example.cs501_micro_chat.ui.auth.AuthProvider

data class SignupUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loadingProvider: AuthProvider? = null,
    val errorMessage: String? = null
) {
    val isLoading: Boolean
        get() = loadingProvider != null
}
