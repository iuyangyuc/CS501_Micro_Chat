/**
 * ChatViewModel.kt
 *
 * 聊天界面 ViewModel 示例 - 展示如何使用 ChatRepository
 * Chat Screen ViewModel Example - Demonstrates how to use ChatRepository
 *
 * 功能示例：
 * - 加载和监听会话列表
 * - 发送和接收消息
 * - 创建群组
 * - 管理联系人
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
 * 聊天 UI 状态
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

    // ==================== 会话相关 Conversation Operations ====================

    /**
     * 加载用户的所有会话
     */
    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 实时监听会话列表变化
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
     * 创建或获取与某用户的私聊会话
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
                    // 导航到聊天界面（由 UI 层处理）
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
     * 删除会话
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
                .onSuccess {
                    // 会话删除成功，列表会自动更新（通过监听）
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== 消息相关 Message Operations ====================

    /**
     * 加载会话的消息列表
     */
    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 首先加载历史消息
            chatRepository.getMessages(conversationId, limit = 50)
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        currentMessages = messages,
                        isLoading = false
                    )
                }

            // 然后实时监听新消息
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(
                    currentMessages = messages,
                    isLoading = false
                )

                // 清空未读数
                chatRepository.clearUnreadCount(conversationId)
            }
        }
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(conversationId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                type = MessageType.TEXT
            ).onSuccess { message ->
                // 消息发送成功，列表会自动更新（通过监听）
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    /**
     * 发送图片消息
     */
    fun sendImageMessage(conversationId: String, imageUrl: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                content = "图片",
                type = MessageType.IMAGE,
                mediaUrl = imageUrl
            ).onSuccess {
                // 发送成功
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(conversationId, messageId)
                .onSuccess {
                    // 消息删除成功
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== 群组相关 Group Operations ====================

    /**
     * 创建群组
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
                description = "欢迎加入 $groupName",
                avatarUrl = avatarUrl,
                memberIds = memberIds
            ).onSuccess { group ->
                // 发送系统消息
                chatRepository.sendMessage(
                    conversationId = group.id,
                    content = "群聊已创建，欢迎大家！",
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
     * 添加群成员
     */
    fun addGroupMembers(groupId: String, memberIds: List<String>) {
        viewModelScope.launch {
            chatRepository.addGroupMembers(groupId, memberIds)
                .onSuccess {
                    // 发送系统消息
                    val memberNames = memberIds.joinToString(", ")
                    chatRepository.sendMessage(
                        conversationId = groupId,
                        content = "$memberNames 加入了群聊",
                        type = MessageType.SYSTEM
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * 退出群组
     */
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            chatRepository.leaveGroup(groupId)
                .onSuccess {
                    // 退出成功
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * 解散群组
     */
    fun dismissGroup(groupId: String) {
        viewModelScope.launch {
            chatRepository.dismissGroup(groupId)
                .onSuccess {
                    // 群组已解散
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    // ==================== 联系人相关 Contact Operations ====================

    /**
     * 加载联系人列表
     */
    private fun loadContacts() {
        viewModelScope.launch {
            // 实时监听联系人列表变化
            chatRepository.observeContacts()?.collect { contacts ->
                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    error = null
                )
            }
        }
    }

    /**
     * 添加联系人
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
     * 删除联系人
     */
    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            chatRepository.deleteContact(contactId)
                .onSuccess {
                    // 删除成功
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * 更新联系人备注
     */
    fun updateContactAlias(contactId: String, alias: String) {
        viewModelScope.launch {
            chatRepository.updateContactAlias(contactId, alias)
                .onSuccess {
                    // 更新成功
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    /**
     * 搜索用户
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

    // ==================== 工具方法 Utility Methods ====================

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

