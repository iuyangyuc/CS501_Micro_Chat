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

        val validationError = when {
            email.isBlank() -> "Email is required."
            password.length < 8 -> "Password must be at least 8 characters."
            password != confirmPassword -> "Passwords do not match."
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
                Log.d(SIGNUP_VIEWMODEL_TAG, "Email signup started for $email")
                repository.createUser(email, password)
                Log.d(SIGNUP_VIEWMODEL_TAG, "Email signup succeeded for $email")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(SignupEvent.SignupSuccess)
            } catch (throwable: Throwable) {
                Log.w(SIGNUP_VIEWMODEL_TAG, "Email signup failed", throwable)
                _uiState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    fun onGoogleSignInStarted() {
        Log.d(SIGNUP_VIEWMODEL_TAG, "Google sign-in intent launched")
        _uiState.update {
            it.copy(
                loadingProvider = AuthProvider.Google,
                errorMessage = null
            )
        }
    }

    fun onGoogleSignInCancelled() {
        Log.d(SIGNUP_VIEWMODEL_TAG, "Google sign-in cancelled by user")
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = "Google sign-in was cancelled."
            )
        }
    }

    fun onGoogleIdTokenReceived(idToken: String?) {
        Log.d(SIGNUP_VIEWMODEL_TAG, "Google sign-in returned token: ${!idToken.isNullOrBlank()}")
        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = "Google sign-in failed. Please try again."
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                repository.signInWithGoogle(idToken)
                Log.d(SIGNUP_VIEWMODEL_TAG, "Google sign-in succeeded")
                _uiState.update { it.copy(loadingProvider = null) }
                _events.send(SignupEvent.SignupSuccess)
            } catch (throwable: Throwable) {
                Log.w(SIGNUP_VIEWMODEL_TAG, "Google sign-in failed", throwable)
                _uiState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    fun onGoogleSignInFailed(throwable: Throwable) {
        Log.w(SIGNUP_VIEWMODEL_TAG, "Google sign-in intent failed", throwable)
        _uiState.update {
            it.copy(
                loadingProvider = null,
                errorMessage = throwable.toUserMessage()
            )
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Try adding numbers or symbols."
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials. Please double-check your input."
            is FirebaseAuthUserCollisionException -> "An account already exists for this email."
            is ApiException -> when (statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
                GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error. Check your connection and try again."
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> "Google sign-in is misconfigured. Please verify the OAuth client ID and SHA-1 fingerprints."
                CommonStatusCodes.TIMEOUT -> "Google sign-in timed out. Please try again."
                else -> statusMessage?.takeIf { it.isNotBlank() } ?: "Google sign-in failed. Please try again."
            }
            else -> localizedMessage?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        }
    }
}

sealed interface SignupEvent {
    data object SignupSuccess : SignupEvent
}

private const val SIGNUP_VIEWMODEL_TAG = "SignupViewModel"
