/**
 * ChatDetailViewModel.kt
 *
 * 对话详情 ViewModel - 管理消息列表和发送消息
 * Chat Detail ViewModel - Manages message list and sending messages
 *
 * 功能 / Features:
 * - 从 Firebase 加载历史消息
 * - 实时监听新消息
 * - 发送文本消息
 * - 标记消息已读
 * - 获取当前用户 ID
 *
 * @author CS501 Team
 * @date 2025-01-08
 */
package com.example.cs501_micro_chat.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid ?: "")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _otherUserId = MutableStateFlow("")
    val otherUserId: StateFlow<String> = _otherUserId.asStateFlow()

    // 用户信息缓存：userId -> User
    private val userCache = mutableMapOf<String, com.example.cs501_micro_chat.data.model.User>()

    private var currentConversationId: String? = null

    /**
     * 加载会话消息并实时监听
     * Load conversation messages and listen for real-time updates
     */
    fun loadMessages(conversationId: String) {
        if (currentConversationId == conversationId) {
            // 已经在监听这个会话
            return
        }

        currentConversationId = conversationId
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            loadConversationMeta(conversationId)
            try {
                // 实时监听消息变化
                chatRepository.observeMessages(conversationId).collect { messageList ->
                    Log.d("ChatDetailViewModel", "Received ${messageList.size} messages")

                    // 补充缺失的用户信息
                    val enrichedMessages = enrichMessagesWithUserInfo(messageList)

                    _messages.value = enrichedMessages.sortedBy { it.timestamp }
                    _isLoading.value = false
                    _error.value = null

                    // 标记所有消息为已读
                    markAllAsRead(conversationId)
                }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error loading messages", e)
                _error.value = "加载消息失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadConversationMeta(conversationId: String) {
        chatRepository.getConversation(conversationId).onSuccess { conversation ->
            val convo = conversation ?: return@onSuccess
            if (convo.type == com.example.cs501_micro_chat.data.model.ConversationType.PRIVATE) {
                val currentId = _currentUserId.value
                val other = convo.participants.firstOrNull { it != currentId }.orEmpty()
                _otherUserId.value = other
            } else {
                _otherUserId.value = ""
            }
        }.onFailure { error ->
            Log.e("ChatDetailViewModel", "Failed to load conversation meta", error)
        }
    }

    /**
     * 补充消息中缺失的用户信息
     * Enrich messages with missing user information
     */
    private suspend fun enrichMessagesWithUserInfo(messages: List<Message>): List<Message> {
        // 收集所有需要加载的用户 ID（senderName 或 senderAvatarUrl 为空的）
        val userIdsToLoad = messages
            .filter { it.senderName.isBlank() || it.senderAvatarUrl.isBlank() }
            .map { it.senderId }
            .toSet()
            .filter { !userCache.containsKey(it) }

        if (userIdsToLoad.isNotEmpty()) {
            Log.d("ChatDetailViewModel", "Loading user info for ${userIdsToLoad.size} users")

            // 批量加载用户信息
            chatRepository.getUsers(userIdsToLoad).onSuccess { users ->
                userCache.putAll(users)
                Log.d("ChatDetailViewModel", "Loaded ${users.size} users into cache")
            }.onFailure { error ->
                Log.e("ChatDetailViewModel", "Failed to load users", error)
            }
        }

        // 使用缓存的用户信息补充消息
        return messages.map { message ->
            if (message.senderName.isBlank() || message.senderAvatarUrl.isBlank()) {
                val user = userCache[message.senderId]
                if (user != null) {
                    message.copy(
                        senderName = user.username,
                        senderAvatarUrl = user.avatarUrl
                    )
                } else {
                    message
                }
            } else {
                message
            }
        }
    }

    /**
     * 发送文本消息
     * Send text message
     */
    fun sendMessage(conversationId: String, content: String) {
        if (content.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    conversationId = conversationId,
                    content = content,
                    type = MessageType.TEXT
                ).onSuccess {
                    Log.d("ChatDetailViewModel", "Message sent successfully")
                    // 消息会通过 observeMessages 自动更新到列表
                }.onFailure { error ->
                    Log.e("ChatDetailViewModel", "Error sending message", error)
                    _error.value = "发送消息失败: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Exception sending message", e)
                _error.value = "发送消息失败: ${e.message}"
            }
        }
    }

    /**
     * 标记所有消息为已读
     * Mark all messages as read
     */
    private fun markAllAsRead(conversationId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _messages.value.forEach { message ->
                if (!message.readBy.contains(userId) && message.senderId != userId) {
                    chatRepository.markMessageAsRead(conversationId, message.id)
                        .onFailure { error ->
                            Log.e("ChatDetailViewModel", "Error marking message as read", error)
                        }
                }
            }

            // 清空未读数
            chatRepository.clearUnreadCount(conversationId)
                .onFailure { error ->
                    Log.e("ChatDetailViewModel", "Error clearing unread count", error)
                }
        }
    }

    /**
     * 清除错误信息
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
