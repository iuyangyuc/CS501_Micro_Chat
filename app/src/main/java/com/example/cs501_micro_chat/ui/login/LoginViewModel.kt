/**
 * LoginViewModel.kt
 *
 * 登录视图模型 - 处理登录业务逻辑和状态管理
 * Login ViewModel - Handles login business logic and state management
 *
 * 主要功能 / Main Functions:
 * - 管理登录界面状态 (StateFlow) / Manage login screen state (StateFlow)
 * - 处理邮箱登录逻辑 / Handle email login logic
 * - 处理 Google 登录流程 / Handle Google Sign-In flow
 * - 表单输入验证 / Form input validation
 * - 错误处理和用户友好的错误消息 / Error handling and user-friendly error messages
 * - 多语言错误提示支持 / Multi-language error message support
 * - 登录事件通知 (Channel) / Login event notification (Channel)
 *
 * 架构设计 / Architecture:
 * - MVVM 模式 / MVVM pattern
 * - 使用 Kotlin Coroutines 处理异步操作 / Uses Kotlin Coroutines for async operations
 * - Repository 模式访问数据层 / Repository pattern for data access
 * - StateFlow 用于状态管理 / StateFlow for state management
 * - Channel 用于一次性事件 / Channel for one-time events
 *
 * 依赖 / Dependencies:
 * - AuthRepository: 认证数据仓库接口 / Authentication repository interface
 * - Firebase Authentication: 后端认证服务 / Backend authentication service
 *
 * @author CS501 Team
 * @date 2025-11-02
 */
package com.example.cs501_micro_chat.ui.login

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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            languagePreferencesRepository.interfaceLanguage.collect { option ->
                _uiState.update { it.copy(selectedLanguage = option) }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onAgreementCheckedChange(checked: Boolean) {
        _uiState.update { it.copy(agreementChecked = checked) }
    }

    fun onLanguageSelected(option: LanguageOption) {
        _uiState.update { it.copy(selectedLanguage = option) }
        viewModelScope.launch {
            languagePreferencesRepository.setInterfaceLanguage(option)
        }
    }

    fun onProviderSelected(provider: AuthProvider) {
        _uiState.update {
            it.copy(
                activeProvider = provider,
                loadingProvider = null,
                agreementChecked = false,
                errorMessage = null
            )
        }
    }

    fun resetProviderSelection() {
        _uiState.update {
            it.copy(
                activeProvider = null,
                loadingProvider = null,
                agreementChecked = false,
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signInWithEmail() {
        val current = _uiState.value
        val email = current.email.trim()
        val password = current.password
        val language = current.selectedLanguage

        val validationError = when {
            current.activeProvider != AuthProvider.Email -> localizedString(language, R.string.error_select_email_provider)
            email.isBlank() -> localizedString(language, R.string.error_email_required)
            password.isBlank() -> localizedString(language, R.string.error_password_required)
            !current.agreementChecked -> localizedString(language, R.string.error_terms_required)
            else -> null
        }

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            Log.d(LOGIN_VIEWMODEL_TAG, "Email login started for $email")
            _uiState.update { it.copy(loadingProvider = AuthProvider.Email, errorMessage = null) }
            try {
                repository.signInWithEmail(email, password)
                Log.d(LOGIN_VIEWMODEL_TAG, "Email login succeeded for $email")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(LoginEvent.LoginSuccess)
            } catch (throwable: Throwable) {
                Log.w(LOGIN_VIEWMODEL_TAG, "Email login failed", throwable)
                _uiState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = throwable.toUserMessage(language)
                    )
                }
            }
        }
    }

    fun onGoogleSignInRequest(): Boolean {
        val current = _uiState.value
        val language = current.selectedLanguage
        if (current.activeProvider != AuthProvider.Google) {
            _uiState.update { it.copy(errorMessage = localizedString(language, R.string.error_select_google_provider)) }
            return false
        }
        if (!current.agreementChecked) {
            _uiState.update { it.copy(errorMessage = localizedString(language, R.string.error_terms_required)) }
            return false
        }
        _uiState.update {
            it.copy(
                loadingProvider = AuthProvider.Google,
                errorMessage = null
            )
        }
        Log.d(LOGIN_VIEWMODEL_TAG, "Google sign-in intent launched")
        return true
    }

    fun onGoogleSignInCancelled() {
        Log.d(LOGIN_VIEWMODEL_TAG, "Google sign-in cancelled by user")
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = localizedString(it.selectedLanguage, R.string.error_google_cancelled)
            )
        }
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        Log.d(LOGIN_VIEWMODEL_TAG, "Google sign-in returned token: ${!idToken.isNullOrBlank()}")
        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    loadingProvider = null,
                errorMessage = localizedString(it.selectedLanguage, R.string.error_google_failed)
            )
        }
            return
        }

        viewModelScope.launch {
            val language = _uiState.value.selectedLanguage
            try {
                repository.signInWithGoogle(idToken)
                Log.d(LOGIN_VIEWMODEL_TAG, "Google sign-in succeeded")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(LoginEvent.LoginSuccess)
            } catch (throwable: Throwable) {
                Log.w(LOGIN_VIEWMODEL_TAG, "Google sign-in failed", throwable)
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
        Log.w(LOGIN_VIEWMODEL_TAG, "Google sign-in intent failed", throwable)
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = throwable.toUserMessage(it.selectedLanguage)
            )
        }
    }

    private fun Throwable.toUserMessage(language: LanguageOption): String {
        return when (this) {
            is FirebaseAuthInvalidCredentialsException -> localizedString(language, R.string.error_invalid_credentials)
            is FirebaseAuthInvalidUserException -> localizedString(language, R.string.error_user_not_found)
            is FirebaseAuthRecentLoginRequiredException -> localizedString(language, R.string.error_reauth_required)
            is ApiException -> when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> localizedString(language, R.string.error_google_cancelled)
                GoogleSignInStatusCodes.NETWORK_ERROR -> localizedString(language, R.string.error_network)
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> localizedString(language, R.string.error_google_misconfigured)
                CommonStatusCodes.TIMEOUT -> localizedString(language, R.string.error_google_timeout)
                else -> statusMessage?.takeIf { it.isNotBlank() } ?: localizedString(language, R.string.error_google_failed)
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

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
}

private const val LOGIN_VIEWMODEL_TAG = "LoginViewModel"
