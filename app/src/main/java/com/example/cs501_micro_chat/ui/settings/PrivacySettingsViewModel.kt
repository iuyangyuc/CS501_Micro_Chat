package com.example.cs501_micro_chat.ui.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun changePassword() {
        val state = _uiState.value
        val validationError = when {
            state.currentPassword.isBlank() -> R.string.error_password_required
            state.newPassword.length < 8 -> R.string.error_password_length
            state.newPassword != state.confirmPassword -> R.string.error_password_mismatch
            state.newPassword == state.currentPassword -> R.string.error_password_same_as_old
            else -> null
        }

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = getString(validationError), successMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                authRepository.changePassword(state.currentPassword, state.newPassword)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = getString(R.string.privacy_password_updated)
                    )
                }
            } catch (throwable: Throwable) {
                val messageRes = if (throwable is FirebaseAuthInvalidCredentialsException) {
                    R.string.error_current_password_incorrect
                } else {
                    R.string.error_generic
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getString(messageRes),
                        successMessage = null
                    )
                }
            }
        }
    }

    private fun getString(@StringRes resId: Int): String = appContext.getString(resId)
}
