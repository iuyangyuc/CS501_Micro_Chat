package com.example.cs501_micro_chat.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<UserProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val initialUserId: String? = savedStateHandle["userId"]
    private val currentUserId: String? = savedStateHandle["currentUserId"]

    init {
        initialUserId?.let { loadProfile(it) }
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, userId = userId) }
            val result = chatRepository.getUser(userId)
            result.onSuccess { user ->
                val resolved = user ?: return@onSuccess
                _uiState.update {
                    it.copy(
                        displayName = resolved.username,
                        email = resolved.email,
                        avatarUrl = resolved.avatarUrl,
                        statusMessage = resolved.statusMessage,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to load profile"))
            }
        }
    }

    fun startChat() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChatting = true, errorMessage = null) }
            val result = chatRepository.createOrGetPrivateConversation(userId)
            result.onSuccess { conversation ->
                val convo = conversation ?: return@onSuccess
                _events.send(
                    UserProfileEvent.OpenChat(
                        conversationId = convo.id,
                        displayName = convo.name.ifBlank { _uiState.value.displayName },
                        avatarUrl = convo.avatarUrl
                    )
                )
            }.onFailure { error ->
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to start chat"))
            }
            _uiState.update { it.copy(isChatting = false) }
        }
    }

    fun deleteContact() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            val result = chatRepository.deleteContact(userId)
            result.onSuccess {
                _events.send(UserProfileEvent.Deleted)
            }.onFailure { error ->
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to delete contact"))
            }
            _uiState.update { it.copy(isDeleting = false) }
        }
    }

    fun searchHistory() {
        val convoId = _uiState.value.conversationId
        if (convoId.isBlank()) {
            viewModelScope.launch {
                _events.send(UserProfileEvent.ShowError("No conversation to search"))
            }
        } else {
            viewModelScope.launch {
                _events.send(UserProfileEvent.SearchHistory(convoId))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _events.send(UserProfileEvent.ShowError("Clear chat history is not implemented yet"))
        }
    }
}

data class UserProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val statusMessage: String = "",
    val isLoading: Boolean = false,
    val isChatting: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val conversationId: String = ""
)

sealed interface UserProfileEvent {
    data class OpenChat(val conversationId: String, val displayName: String, val avatarUrl: String) : UserProfileEvent
    data object Deleted : UserProfileEvent
    data class SearchHistory(val conversationId: String) : UserProfileEvent
    data class ShowError(val message: String) : UserProfileEvent
}
