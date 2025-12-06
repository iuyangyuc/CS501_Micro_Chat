package com.example.cs501_micro_chat.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState: StateFlow<ProfileSettingsUiState> = _uiState.asStateFlow()

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val signedInWithGoogle = isGoogleSignIn()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isGoogleSignIn = signedInWithGoogle
                )
            }
            val result = profileRepository.getProfile()
            result
                .onSuccess { profile ->
                    _uiState.update {
                        val email = profile.email.ifBlank { firebaseAuth.currentUser?.email.orEmpty() }
                        val emailPrefix = email.substringBefore("@")
                        val display = profile.displayName
                            .ifBlank { firebaseAuth.currentUser?.displayName.orEmpty() }
                            .ifBlank { emailPrefix }
                        it.copy(
                            displayName = display,
                            email = email,
                            bio = profile.bio,
                            avatarUrl = profile.avatarUrl,
                            isLoading = false,
                            isGoogleSignIn = signedInWithGoogle
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                            isGoogleSignIn = signedInWithGoogle
                        )
                    }
                }
        }
    }

    private fun isGoogleSignIn(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }
}

data class ProfileSettingsUiState(
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isGoogleSignIn: Boolean = false
)
