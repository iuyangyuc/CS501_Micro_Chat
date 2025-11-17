package com.example.cs501_micro_chat.ui.signup

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.preferences.LanguagePreferencesRepository
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.auth.localized
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            languagePreferencesRepository.interfaceLanguage.collect { option ->
                _uiState.update { it.copy(selectedLanguage = option) }
            }
        }
    }

    fun onLanguageSelected(option: LanguageOption) {
        _uiState.update { it.copy(selectedLanguage = option) }
        viewModelScope.launch {
            languagePreferencesRepository.setInterfaceLanguage(option)
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signUpWithEmail() {
        val current = _uiState.value
        val email = current.email.trim()
        val password = current.password
        val confirmPassword = current.confirmPassword
        val language = current.selectedLanguage

        val validationError = when {
            email.isBlank() -> localizedString(language, R.string.error_email_required)
            password.length < 8 -> localizedString(language, R.string.error_password_length)
            password != confirmPassword -> localizedString(language, R.string.error_password_mismatch)
            else -> null
        }

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingProvider = AuthProvider.Email,
                    errorMessage = null
                )
            }
            try {
                logDebug("Email signup started for $email")
                repository.createUser(email, password)
                logDebug("Email signup succeeded for $email")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(SignupEvent.SignupSuccess)
            } catch (throwable: Throwable) {
                logWarn("Email signup failed", throwable)
                val language = _uiState.value.selectedLanguage
                _uiState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = throwable.toUserMessage(language)
                    )
                }
            }
        }
    }

    fun onGoogleSignInStarted() {
        logDebug("Google sign-in intent launched")
        _uiState.update {
            it.copy(
                loadingProvider = AuthProvider.Google,
                errorMessage = null
            )
        }
    }

    fun onGoogleSignInCancelled() {
        logDebug("Google sign-in cancelled by user")
        val language = _uiState.value.selectedLanguage
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = localizedString(language, R.string.error_google_cancelled)
            )
        }
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        logDebug("Google sign-in returned token: ${!idToken.isNullOrBlank()}")
        val language = _uiState.value.selectedLanguage
        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = localizedString(language, R.string.error_google_failed)
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                repository.signInWithGoogle(idToken)
                logDebug("Google sign-in succeeded")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(SignupEvent.SignupSuccess)
            } catch (throwable: Throwable) {
                logWarn("Google sign-in failed", throwable)
                val language = _uiState.value.selectedLanguage
                _uiState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = throwable.toUserMessage(language)
                    )
                }
            }
        }
    }

    fun onGoogleSignInFailed(throwable: Throwable) {
        logWarn("Google sign-in intent failed", throwable)
        val language = _uiState.value.selectedLanguage
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = throwable.toUserMessage(language)
            )
        }
    }

    private fun Throwable.toUserMessage(language: LanguageOption): String {
        return when (this) {
            is FirebaseAuthWeakPasswordException -> localizedString(language, R.string.error_password_weak)
            is FirebaseAuthInvalidCredentialsException -> localizedString(language, R.string.error_invalid_credentials)
            is FirebaseAuthUserCollisionException -> localizedString(language, R.string.error_email_exists)
            is ApiException -> when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> localizedString(language, R.string.error_google_cancelled)
                GoogleSignInStatusCodes.NETWORK_ERROR -> localizedString(language, R.string.error_network)
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> localizedString(language, R.string.error_google_misconfigured)
                CommonStatusCodes.TIMEOUT -> localizedString(language, R.string.error_google_timeout)
                else -> statusMessage?.takeIf { it.isNotBlank() }
                    ?: localizedString(language, R.string.error_google_failed)
            }
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: localizedString(language, R.string.error_generic)
        }
    }

    private fun localizedString(language: LanguageOption, @StringRes resId: Int): String {
        val localizedContext = appContext.localized(language)
        return localizedContext.getString(resId)
    }
}

sealed interface SignupEvent {
    data object SignupSuccess : SignupEvent
}

private const val SIGNUP_VIEWMODEL_TAG = "SignupViewModel"

private fun logDebug(message: String) {
    runCatching { Log.d(SIGNUP_VIEWMODEL_TAG, message) }
}

private fun logWarn(message: String, throwable: Throwable) {
    runCatching { Log.w(SIGNUP_VIEWMODEL_TAG, message, throwable) }
}
