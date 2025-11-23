package com.example.cs501_micro_chat.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.R
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    userId = userId,
                    conversationId = "",
                    alias = "",
                    aliasInput = "",
                    canEditAlias = false,
                    isAliasEditing = false,
                    canPin = false,
                    isPinned = false,
                    isPinUpdating = false,
                    contactFavorite = false
                )
            }

            val user = chatRepository.getUser(userId).getOrElse { error ->
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = error.message) }
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to load profile"))
                return@launch
            } ?: run {
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = "User not found") }
                _events.send(UserProfileEvent.ShowError("User not found"))
                return@launch
            }

            _uiState.update {
                it.copy(
                    displayName = user.username,
                    originalName = user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    statusMessage = user.statusMessage
                )
            }

            chatRepository.getContact(userId).onSuccess { contact ->
                val alias = contact?.alias?.trim().orEmpty()
                val conversationId = contact?.conversationId.orEmpty()
                val canPin = conversationId.isNotBlank()
                val favorite = contact?.isFavorite == true
                _uiState.update { state ->
                    state.copy(
                        alias = alias,
                        aliasInput = alias,
                        displayName = if (alias.isNotBlank()) alias else state.originalName,
                        conversationId = conversationId,
                        canEditAlias = contact != null && !contact.isNew && !contact.isPending,
                        isAliasEditing = false,
                        canPin = canPin,
                        isPinned = if (canPin) favorite else false,
                        isPinUpdating = false,
                        contactFavorite = favorite
                    )
                }
                if (canPin) {
                    refreshPinnedState(conversationId)
                }
            }

            _uiState.update { it.copy(isLoading = false) }
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
                val preferredName = _uiState.value.alias.takeIf { it.isNotBlank() } ?: _uiState.value.originalName
                _events.send(
                    UserProfileEvent.OpenChat(
                        conversationId = convo.id,
                        displayName = preferredName.ifBlank { convo.name.ifBlank { _uiState.value.displayName } },
                        avatarUrl = convo.avatarUrl
                    )
                )
                _uiState.update { it.copy(conversationId = convo.id, canPin = true) }
                refreshPinnedState(convo.id)
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
                _events.send(UserProfileEvent.ShowStatus(R.string.user_profile_delete_success, true))
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

    fun onAliasChange(value: String) {
        _uiState.update { it.copy(aliasInput = value) }
    }

    fun startAliasEdit() {
        val current = _uiState.value
        if (!current.canEditAlias) return
        _uiState.update {
            it.copy(
                isAliasEditing = true,
                aliasInput = it.alias
            )
        }
    }

    fun cancelAliasEdit() {
        _uiState.update {
            it.copy(
                isAliasEditing = false,
                aliasInput = it.alias
            )
        }
    }

    fun saveAlias() {
        val contactId = _uiState.value.userId
        if (contactId.isBlank() || !_uiState.value.canEditAlias) return
        val newAlias = _uiState.value.aliasInput.trim()
        if (newAlias == _uiState.value.alias) {
            _uiState.update { it.copy(isAliasEditing = false) }
            viewModelScope.launch { _events.send(UserProfileEvent.AliasSaved) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAliasSaving = true, errorMessage = null) }
            chatRepository.updateContactAlias(contactId, newAlias).onSuccess {
                _uiState.update {
                    it.copy(
                        isAliasSaving = false,
                        alias = newAlias,
                        aliasInput = newAlias,
                        displayName = if (newAlias.isNotBlank()) newAlias else it.originalName,
                        isAliasEditing = false
                    )
                }
                _events.send(UserProfileEvent.AliasSaved)
            }.onFailure { error ->
                _uiState.update { it.copy(isAliasSaving = false) }
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to update alias"))
            }
        }
    }

    fun togglePinned() {
        val state = _uiState.value
        val conversationId = state.conversationId
        if (!state.canPin || conversationId.isBlank() || state.isPinUpdating) return
        val newPinned = !state.isPinned
        viewModelScope.launch {
            _uiState.update { it.copy(isPinUpdating = true, errorMessage = null) }
            chatRepository.setPinnedConversation(conversationId, newPinned).onSuccess {
                if (state.userId.isNotBlank()) {
                    chatRepository.updateContactFavorite(state.userId, newPinned)
                }
                _uiState.update {
                    it.copy(
                        isPinned = newPinned,
                        contactFavorite = newPinned,
                        isPinUpdating = false
                    )
                }
                _events.send(UserProfileEvent.PinStatusChanged(newPinned))
            }.onFailure { error ->
                _uiState.update { it.copy(isPinUpdating = false) }
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to update pin status"))
            }
        }
    }

    private fun refreshPinnedState(conversationId: String) {
        viewModelScope.launch {
            chatRepository.isConversationPinned(conversationId).onSuccess { persisted ->
                val current = _uiState.value
                var finalPinned = persisted
                if (current.contactFavorite && !finalPinned) {
                    chatRepository.setPinnedConversation(conversationId, true)
                    finalPinned = true
                } else if (!current.contactFavorite && finalPinned && current.userId.isNotBlank()) {
                    chatRepository.updateContactFavorite(current.userId, true)
                }
                _uiState.update {
                    it.copy(
                        isPinned = finalPinned,
                        canPin = true,
                        isPinUpdating = false,
                        contactFavorite = finalPinned
                    )
                }
            }.onFailure { error ->
                _events.send(UserProfileEvent.ShowError(error.message ?: "Failed to load pin status"))
            }
        }
    }
}

data class UserProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val originalName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val statusMessage: String = "",
    val alias: String = "",
    val aliasInput: String = "",
    val canEditAlias: Boolean = false,
    val isAliasSaving: Boolean = false,
    val isAliasEditing: Boolean = false,
    val canPin: Boolean = false,
    val isPinned: Boolean = false,
    val isPinUpdating: Boolean = false,
    val contactFavorite: Boolean = false,
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
    data object AliasSaved : UserProfileEvent
    data class PinStatusChanged(val isPinned: Boolean) : UserProfileEvent
    data class ShowStatus(@StringRes val messageRes: Int, val success: Boolean) : UserProfileEvent
}
