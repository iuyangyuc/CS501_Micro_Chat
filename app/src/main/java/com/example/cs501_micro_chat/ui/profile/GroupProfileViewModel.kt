package com.example.cs501_micro_chat.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.ConversationType
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
class GroupProfileViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupProfileUiState())
    val uiState: StateFlow<GroupProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<GroupProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val conversationId: String? = savedStateHandle["conversationId"]

    init {
        conversationId?.let { loadGroup(it) }
    }

    fun loadGroup(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            chatRepository.getConversation(conversationId).onSuccess { conversation ->
                val convo = conversation ?: return@onSuccess
                if (convo.type != ConversationType.GROUP) {
                    _uiState.update { it.copy(errorMessage = "Not a group", isLoading = false) }
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(
                        conversationId = convo.id,
                        name = convo.name,
                        avatarUrl = convo.avatarUrl,
                        participants = convo.participants,
                        isLoading = false
                    )
                }
                loadMembers(convo.participants)
                loadPinFromContact(convo.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    private suspend fun loadMembers(userIds: List<String>) {
        if (userIds.isEmpty()) return
        chatRepository.getUsers(userIds).onSuccess { map ->
            val members = userIds.map { id ->
                val user = map[id]
                GroupMember(
                    id = id,
                    name = user?.username.orEmpty(),
                    avatarUrl = user?.avatarUrl.orEmpty()
                )
            }
            _uiState.update { it.copy(members = members) }
        }
    }

    private fun loadPinFromContact(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getContact(conversationId).onSuccess { contact ->
                val favorite = contact?.isFavorite == true
                _uiState.update {
                    it.copy(
                        isPinned = favorite,
                        canPin = true,
                        isPinUpdating = false,
                        contactFavorite = favorite
                    )
                }
                refreshPinnedState(conversationId)
            }.onFailure {
                _uiState.update { it.copy(canPin = true, isPinUpdating = false) }
                refreshPinnedState(conversationId)
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
                } else if (!current.contactFavorite && finalPinned) {
                    chatRepository.updateContactFavorite(conversationId, true)
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
                _events.send(
                    GroupProfileEvent.ShowMessage(
                        messageRes = R.string.group_profile_error_pin_status,
                        message = error.message
                    )
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun saveName() {
        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val newName = _uiState.value.name

            chatRepository.getConversation(conversationId).onSuccess { conversation ->
                conversation?.let {
                    chatRepository.updateConversation(it.copy(name = newName))
                        .onFailure { error ->
                            _events.trySend(
                                GroupProfileEvent.ShowMessage(
                                    messageRes = R.string.group_profile_error_update_name,
                                    message = error.message
                                )
                            )
                        }
                }
            }

            chatRepository.getGroup(conversationId).onSuccess { group ->
                val existing = group ?: return@onSuccess
                chatRepository.updateGroup(existing.copy(name = newName)).onFailure { error ->
                    _events.trySend(
                        GroupProfileEvent.ShowMessage(
                            messageRes = R.string.group_profile_error_update_name,
                            message = error.message
                        )
                    )
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            _events.trySend(GroupProfileEvent.Renamed(newName))
        }
    }

    fun startChat() {
        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) return
        viewModelScope.launch {
            _events.send(
                GroupProfileEvent.OpenChat(
                    conversationId = conversationId,
                    name = _uiState.value.name,
                    avatarUrl = _uiState.value.avatarUrl
                )
            )
        }
    }

    fun leaveGroup() {
        val groupId = _uiState.value.conversationId
        if (groupId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true) }
            chatRepository.leaveGroup(groupId).onSuccess {
                _events.send(GroupProfileEvent.LeftGroup)
                _uiState.update {
                    it.copy(
                        isLeaving = false,
                        isRemoved = true,
                        canPin = false,
                        isPinned = false,
                        contactFavorite = false,
                        conversationId = ""
                    )
                }
            }.onFailure { error ->
                _events.send(
                    GroupProfileEvent.ShowMessage(
                        messageRes = R.string.group_profile_error_leave,
                        message = error.message
                    )
                )
                _uiState.update { it.copy(isLeaving = false) }
            }
        }
    }

    fun togglePinned() {
        val state = _uiState.value
        val conversationId = state.conversationId
        if (!state.canPin || conversationId.isBlank() || state.isPinUpdating) return
        val newStatus = !state.isPinned
        viewModelScope.launch {
            _uiState.update { it.copy(isPinUpdating = true) }
            chatRepository.setPinnedConversation(conversationId, newStatus).onSuccess {
                chatRepository.updateContactFavorite(conversationId, newStatus)
                _uiState.update {
                    it.copy(
                        isPinned = newStatus,
                        isPinUpdating = false,
                        contactFavorite = newStatus
                    )
                }
                _events.send(GroupProfileEvent.PinStatusChanged(newStatus))
            }.onFailure { error ->
                _uiState.update { it.copy(isPinUpdating = false) }
                _events.send(
                    GroupProfileEvent.ShowMessage(
                        messageRes = R.string.group_profile_error_pin_update,
                        message = error.message
                    )
                )
            }
        }
    }

    fun searchHistory() {
        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) return
        viewModelScope.launch {
            _events.send(GroupProfileEvent.SearchHistory(conversationId))
        }
    }

    fun clearHistory() {
        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            chatRepository.clearConversationForCurrentUser(conversationId).onSuccess {
                _events.send(GroupProfileEvent.ShowStatus(R.string.group_profile_clear_history_success, true))
            }.onFailure { error ->
                _events.send(
                    GroupProfileEvent.ShowMessage(
                        messageRes = R.string.group_profile_error_clear_history,
                        message = error.message
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

data class GroupProfileUiState(
    val conversationId: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val participants: List<String> = emptyList(),
    val members: List<GroupMember> = emptyList(),
    val isPinned: Boolean = false,
    val canPin: Boolean = false,
    val isPinUpdating: Boolean = false,
    val contactFavorite: Boolean = false,
    val isRemoved: Boolean = false,
    val isSaving: Boolean = false,
    val isLeaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class GroupMember(
    val id: String,
    val name: String,
    val avatarUrl: String
)

sealed interface GroupProfileEvent {
    data class OpenChat(val conversationId: String, val name: String, val avatarUrl: String) : GroupProfileEvent
    data class SearchHistory(val conversationId: String) : GroupProfileEvent
    data object LeftGroup : GroupProfileEvent
    data class Renamed(val name: String) : GroupProfileEvent
    data class ShowMessage(val message: String? = null, @StringRes val messageRes: Int? = null) : GroupProfileEvent
    data class PinStatusChanged(val isPinned: Boolean) : GroupProfileEvent
    data class ShowStatus(@StringRes val messageRes: Int, val success: Boolean) : GroupProfileEvent
}
