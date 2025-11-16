package com.example.cs501_micro_chat.ui.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.example.cs501_micro_chat.data.repository.FirebaseAuthRepository
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SignupViewModel(
    private val repository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var hasInitializedLanguage = false

    fun initializeLanguage(option: LanguageOption) {
        if (hasInitializedLanguage) return
        hasInitializedLanguage = true
        _uiState.update { it.copy(selectedLanguage = option) }
    }

    fun onLanguageSelected(option: LanguageOption) {
        _uiState.update { it.copy(selectedLanguage = option) }
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
            email.isBlank() -> localized(language, "邮箱不能为空。", "Email is required.")
            password.length < 8 -> localized(language, "密码长度至少 8 位。", "Password must be at least 8 characters.")
            password != confirmPassword -> localized(language, "两次输入的密码不一致。", "Passwords do not match.")
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
                errorMessage = localized(language, "Google 注册已取消。", "Google sign-in was cancelled.")
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
                    errorMessage = localized(language, "Google 注册失败，请重试。", "Google sign-in failed. Please try again.")
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
            is FirebaseAuthWeakPasswordException -> localized(language, "密码过于简单，请添加数字或符号。", "Password is too weak. Try adding numbers or symbols.")
            is FirebaseAuthInvalidCredentialsException -> localized(language, "邮箱或密码无效，请检查输入。", "Invalid credentials. Please double-check your input.")
            is FirebaseAuthUserCollisionException -> localized(language, "该邮箱已注册账户。", "An account already exists for this email.")
            is ApiException -> when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> localized(language, "Google 注册已取消。", "Google sign-in was cancelled.")
                GoogleSignInStatusCodes.NETWORK_ERROR -> localized(language, "网络错误，请检查连接后重试。", "Network error. Check your connection and try again.")
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> localized(language, "Google 注册配置错误，请检查 OAuth 客户端 ID 和 SHA-1。", "Google sign-in is misconfigured. Please verify the OAuth client ID and SHA-1 fingerprints.")
                CommonStatusCodes.TIMEOUT -> localized(language, "Google 注册超时，请重试。", "Google sign-in timed out. Please try again.")
                else -> statusMessage?.takeIf { it.isNotBlank() }
                    ?: localized(language, "Google 注册失败，请重试。", "Google sign-in failed. Please try again.")
            }
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: localized(language, "发生错误，请稍后重试。", "Something went wrong. Please try again.")
        }
    }

    private fun localized(language: LanguageOption, zh: String, en: String): String {
        return if (language == LanguageOption.Chinese) zh else en
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
