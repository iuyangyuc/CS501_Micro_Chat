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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
            current.activeProvider != AuthProvider.Email -> localized(language, "请选择邮箱登录。", "Please select email login.")
            email.isBlank() -> localized(language, "邮箱不能为空。", "Email is required.")
            password.isBlank() -> localized(language, "密码不能为空。", "Password is required.")
            !current.agreementChecked -> localized(language, "请先阅读并同意服务协议。", "Please agree to the terms first.")
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
            _uiState.update { it.copy(errorMessage = localized(language, "请选择 Google 登录。", "Please select Google sign-in.")) }
            return false
        }
        if (!current.agreementChecked) {
            _uiState.update { it.copy(errorMessage = localized(language, "请先阅读并同意服务协议。", "Please agree to the terms first.")) }
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
                errorMessage = localized(it.selectedLanguage, "Google 登录已取消。", "Google sign-in was cancelled.")
            )
        }
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        Log.d(LOGIN_VIEWMODEL_TAG, "Google sign-in returned token: ${!idToken.isNullOrBlank()}")
        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = localized(it.selectedLanguage, "Google 登录失败，请重试。", "Google sign-in failed. Please try again.")
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
            is FirebaseAuthInvalidCredentialsException -> localized(language, "邮箱或密码不正确。", "Email or password is incorrect.")
            is FirebaseAuthInvalidUserException -> localized(language, "该账号不存在或已被禁用。", "This account does not exist or has been disabled.")
            is FirebaseAuthRecentLoginRequiredException -> localized(language, "请重新验证身份后继续。", "Please reauthenticate before continuing.")
            is ApiException -> when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> localized(language, "Google 登录已取消。", "Google sign-in was cancelled.")
                GoogleSignInStatusCodes.NETWORK_ERROR -> localized(language, "网络错误，请检查连接后重试。", "Network error. Check your connection and try again.")
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> localized(language, "Google 登录配置错误，请检查 OAuth 客户端 ID 和 SHA-1。", "Google sign-in is misconfigured. Please verify the OAuth client ID and SHA-1 fingerprints.")
                CommonStatusCodes.TIMEOUT -> localized(language, "Google 登录超时，请重试。", "Google sign-in timed out. Please try again.")
                else -> statusMessage?.takeIf { it.isNotBlank() } ?: localized(language, "Google 登录失败，请重试。", "Google sign-in failed. Please try again.")
            }
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: localized(language, "发生错误，请稍后重试。", "Something went wrong. Please try again.")
        }
    }

    private fun localized(language: LanguageOption, zh: String, en: String): String {
        return if (language == LanguageOption.Chinese) zh else en
    }
}

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
}

private const val LOGIN_VIEWMODEL_TAG = "LoginViewModel"
