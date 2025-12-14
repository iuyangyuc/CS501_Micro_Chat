/**
 * ChatViewModel.kt
 *
 * Chat Screen ViewModel Example - Demonstrates how to use ChatRepository
 *
 * Function Examples:
 * - Load and observe conversation list
 * - Send and receive messages
 * - Create groups
 * - Manage contacts
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.*
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Chat UI State
 */
data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentMessages: List<Message> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        loadContacts()
    }

    // ==================== Conversation Operations ====================

    /**
     * Load all conversations for user
     */
    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Real-time observe conversation list changes
            chatRepository.observeUserConversations()?.collect { conversations ->
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    /**
     * Create or get private conversation with a user
     */
    fun startChatWithUser(otherUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            chatRepository.createOrGetPrivateConversation(otherUserId)
                .onSuccess { conversation ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    // Navigate to chat screen (handled by UI layer)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    /**
     * Delete conversation
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
                .onSuccess {
                    // Conversation deleted successfully, list will auto-update (via observer)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== Message Operations ====================

    /**
     * Load message list for conversation
     */
    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // First load history messages
            chatRepository.getMessages(conversationId, limit = 50)
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        currentMessages = messages,
                        isLoading = false
                    )
                }

            // Then real-time observe new messages
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(
                    currentMessages = messages,
                    isLoading = false
                )

                // Clear unread count
                chatRepository.clearUnreadCount(conversationId)
            }
        }
    }

    /**
     * Send text message
     */
    fun sendTextMessage(conversationId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                type = MessageType.TEXT
            ).onSuccess { message ->
                // Message sent successfully, list will auto-update (via observer)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    /**
     * Send image message
     */
    fun sendImageMessage(conversationId: String, imageUrl: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                content = "Image",
                type = MessageType.IMAGE,
                mediaUrl = imageUrl
            ).onSuccess {
                // Send successful
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    /**
     * Delete message
     */
    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(conversationId, messageId)
                .onSuccess {
                    // Message deleted successfully
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== Group Operations ====================

    /**
     * Create group
     */
    fun createGroup(
        groupName: String,
        memberIds: List<String>,
        avatarUrl: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            chatRepository.createGroup(
                name = groupName,
                description = "Welcome to $groupName",
                avatarUrl = avatarUrl,
                memberIds = memberIds
            ).onSuccess { group ->
                // Send system message
                chatRepository.sendMessage(
                    conversationId = group.id,
                    content = "Group created, welcome everyone!",
                    type = MessageType.SYSTEM
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    /**
     * Add group members
     */
    fun addGroupMembers(groupId: String, memberIds: List<String>) {
        viewModelScope.launch {
            chatRepository.addGroupMembers(groupId, memberIds)
                .onSuccess {
                    // Send system message
                    val memberNames = memberIds.joinToString(", ")
                    chatRepository.sendMessage(
                        conversationId = groupId,
                        content = "$memberNames joined the group",
                        type = MessageType.SYSTEM
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * Leave group
     */
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            chatRepository.leaveGroup(groupId)
                .onSuccess {
                    // Left successfully
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * Dismiss group
     */
    fun dismissGroup(groupId: String) {
        viewModelScope.launch {
            chatRepository.dismissGroup(groupId)
                .onSuccess {
                    // Group dismissed
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== Contact Operations ====================

    /**
     * Load contact list
     */
    private fun loadContacts() {
        viewModelScope.launch {
            // Real-time observe contact list changes
            chatRepository.observeContacts()?.collect { contacts ->
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    error = null
                )
            }
        }
    }

    /**
     * Add contact
     */
    fun addContact(userId: String, alias: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            chatRepository.addContact(
                contactId = userId,
                alias = alias
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    /**
     * Delete contact
     */
    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            chatRepository.deleteContact(contactId)
                .onSuccess {
                    // Delete successful
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * Update contact alias
     */
    fun updateContactAlias(contactId: String, alias: String) {
        viewModelScope.launch {
            chatRepository.updateContactAlias(contactId, alias)
                .onSuccess {
                    // Update successful
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * Search users
     */
    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        viewModelScope.launch {
            chatRepository.searchUsers(query)
                .onSuccess { users ->
                    onResult(users)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

