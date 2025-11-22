package com.example.cs501_micro_chat.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                            _events.trySend(GroupProfileEvent.ShowMessage(error.message ?: "Failed to update conversation name"))
                        }
                }
            }

            chatRepository.getGroup(conversationId).onSuccess { group ->
                val existing = group ?: return@onSuccess
                chatRepository.updateGroup(existing.copy(name = newName)).onFailure { error ->
                    _events.trySend(GroupProfileEvent.ShowMessage(error.message ?: "Failed to update group name"))
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
            }.onFailure { error ->
                _events.send(GroupProfileEvent.ShowMessage(error.message ?: "Failed to leave group"))
            }
            _uiState.update { it.copy(isLeaving = false) }
        }
    }

    fun togglePinned() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        viewModelScope.launch {
            _events.send(GroupProfileEvent.ShowMessage("Pin status saved locally"))
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
        viewModelScope.launch {
            _events.send(GroupProfileEvent.ShowMessage("Clear chat history is not implemented yet"))
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
    data class ShowMessage(val message: String) : GroupProfileEvent
}
