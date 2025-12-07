package com.example.cs501_micro_chat.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageFilterSearchUiState(
    val filter: MessageSearchFilter,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class MessageFilterSearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val filterArg: String = savedStateHandle.get<String>("filter").orEmpty()
    private val filter: MessageSearchFilter = MessageSearchFilter.fromArg(filterArg)
    private val currentUserId: String = chatRepository.currentUserIdOrNull().orEmpty()
    private var clearedAtMs: Long = 0L
    private val contactCache = mutableMapOf<String, Contact>()

    private val _uiState = MutableStateFlow(
        MessageFilterSearchUiState(
            filter = filter,
            currentUserId = currentUserId
        )
    )
    val uiState: StateFlow<MessageFilterSearchUiState> = _uiState.asStateFlow()

    init {
        if (conversationId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid parameters") }
        } else {
            viewModelScope.launch {
                loadContacts()
                loadClearedAt()
                observeMessages()
            }
        }
    }

    private suspend fun loadContacts() {
        chatRepository.getContacts().onSuccess { contacts ->
            contactCache.clear()
            contacts.forEach { contactCache[it.contactId] = it }
        }
    }

    private suspend fun loadClearedAt() {
        if (currentUserId.isBlank()) return
        chatRepository.getConversation(conversationId).onSuccess { convo ->
            clearedAtMs = convo?.clearedAt?.get(currentUserId) ?: 0L
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
                    .asSequence()
                    .filter { it.timestamp > clearedAtMs }
                    .filter { matchesFilter(it) }
                    .filterNot { it.isDeleted }
                    .sortedBy { it.timestamp }
                    .map { message ->
                        message.copy(
                            senderName = resolveDisplayName(message.senderId, message.senderName),
                            senderAvatarUrl = resolveAvatar(message.senderId, message.senderAvatarUrl)
                        )
                    }
                    .toList()

                _uiState.update {
                    it.copy(
                        messages = filtered,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
    }

    private fun matchesFilter(message: Message): Boolean {
        return when (filter) {
            MessageSearchFilter.Photos -> message.type == MessageType.IMAGE || message.type == MessageType.VIDEO
            MessageSearchFilter.Files -> message.type == MessageType.FILE
            MessageSearchFilter.Audio -> message.type == MessageType.VOICE
            MessageSearchFilter.Links -> {
                val content = message.content.lowercase()
                message.type == MessageType.TEXT &&
                        (content.contains("http://") || content.contains("https://"))
            }
        }
    }

    private fun resolveDisplayName(userId: String, fallback: String): String {
        val contact = contactCache[userId]
        val alias = contact?.alias?.trim()
        val contactName = contact?.contactName?.trim()
        return when {
            !alias.isNullOrEmpty() -> alias
            !contactName.isNullOrEmpty() -> contactName
            fallback.isNotBlank() -> fallback
            else -> userId
        }
    }

    private fun resolveAvatar(userId: String, fallback: String): String {
        val contactAvatar = contactCache[userId]?.contactAvatarUrl
        return when {
            !contactAvatar.isNullOrEmpty() -> contactAvatar
            fallback.isNotBlank() -> fallback
            else -> ""
        }
    }
}
