package com.example.cs501_micro_chat.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemberMessageSearchUiState(
    val memberId: String = "",
    val memberName: String = "",
    val memberAvatarUrl: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class MemberMessageSearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()
    private val memberNameArg: String = savedStateHandle.get<String>("memberName").orEmpty()
    private val memberAvatarArg: String = savedStateHandle.get<String>("memberAvatar").orEmpty()
    private var clearedAtMs: Long = 0L

    private val _uiState = MutableStateFlow(
        MemberMessageSearchUiState(
            memberId = memberId,
            memberName = decode(memberNameArg),
            memberAvatarUrl = decode(memberAvatarArg),
            currentUserId = chatRepository.currentUserIdOrNull().orEmpty()
        )
    )
    val uiState: StateFlow<MemberMessageSearchUiState> = _uiState.asStateFlow()

    init {
        if (conversationId.isBlank() || memberId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid parameters") }
        } else {
            viewModelScope.launch {
                if (_uiState.value.memberName.isBlank()) {
                    loadMemberName()
                }
                loadClearedAt()
                observeMessages()
            }
        }
    }

    private suspend fun loadMemberName() {
        var alias: String? = null
        chatRepository.getContact(memberId).onSuccess { contact ->
            alias = contact?.alias?.trim().takeIf { !it.isNullOrEmpty() }
            val contactName = contact?.contactName.orEmpty()
            val contactAvatar = contact?.contactAvatarUrl.orEmpty()
            _uiState.update {
                it.copy(
                    memberName = when {
                        !alias.isNullOrEmpty() -> alias!!
                        it.memberName.isNotBlank() -> it.memberName
                        contactName.isNotBlank() -> contactName
                        else -> it.memberName
                    },
                    memberAvatarUrl = if (it.memberAvatarUrl.isNotBlank()) it.memberAvatarUrl else contactAvatar
                )
            }
        }

        chatRepository.getUser(memberId).onSuccess { user ->
            val name = user?.username
                ?: user?.email?.substringBefore("@")
                ?: ""
            val avatar = user?.avatarUrl.orEmpty()
            _uiState.update {
                val preferredName = when {
                    !alias.isNullOrEmpty() -> alias!!
                    it.memberName.isNotBlank() -> it.memberName
                    else -> name
                }
                it.copy(
                    memberName = preferredName,
                    memberAvatarUrl = if (it.memberAvatarUrl.isNotBlank()) it.memberAvatarUrl else avatar
                )
            }
        }
    }

    private suspend fun observeMessages() {
        chatRepository
            .observeMessages(conversationId)
            .catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
            .collect { messages ->
                val filtered = messages
                    .filter { it.senderId == memberId && it.timestamp > clearedAtMs }
                    .sortedBy { it.timestamp }
                _uiState.update {
                    it.copy(
                        messages = filtered,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
    }

    private fun decode(value: String): String {
        return runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        }.getOrDefault(value)
    }

    private suspend fun loadClearedAt() {
        val currentUserId = _uiState.value.currentUserId
        if (currentUserId.isBlank()) return
        chatRepository.getConversation(conversationId).onSuccess { convo ->
            clearedAtMs = convo?.clearedAt?.get(currentUserId) ?: 0L
        }
    }
}
