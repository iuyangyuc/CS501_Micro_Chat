package com.example.cs501_micro_chat.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value, errorMessage = null) }
    }

    fun onAvatarSelected(uri: Uri) {
        _uiState.update { it.copy(newAvatarUri = uri, errorMessage = null) }
    }

    fun clearAvatarSelection() {
        _uiState.update { it.copy(newAvatarUri = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = repository.updateProfile(
                displayName = state.displayName.trim(),
                bio = state.bio.trim(),
                avatarUri = state.newAvatarUri
            )
            _uiState.update { it.copy(isSaving = false) }
            result.onSuccess {
                _events.send(ProfileEditEvent.ProfileSaved)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to update profile") }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getProfile()
            result.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayName = profile.displayName,
                        bio = profile.bio,
                        avatarUrl = profile.avatarUrl,
                        newAvatarUri = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load profile") }
            }
        }
    }
}

data class ProfileEditUiState(
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val newAvatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ProfileEditEvent {
    data object ProfileSaved : ProfileEditEvent
}
