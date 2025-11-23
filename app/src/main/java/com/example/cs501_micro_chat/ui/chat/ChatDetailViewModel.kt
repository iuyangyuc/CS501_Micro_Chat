/**
 * Chat detail ViewModel that manages message lists and sending.
 *
 * Features:
 * - Load historical messages from Firebase
 * - Listen for new messages in real time
 * - Send text messages
 * - Mark messages as read
 * - Get current user ID
 */
package com.example.cs501_micro_chat.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageStatus
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.example.cs501_micro_chat.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaUploadState(
    val isUploading: Boolean = false,
    val uploadingType: MessageType? = null,
    val lastUploadedUrl: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository,
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

    private val _conversationType = MutableStateFlow(com.example.cs501_micro_chat.data.model.ConversationType.PRIVATE)
    val conversationType: StateFlow<com.example.cs501_micro_chat.data.model.ConversationType> = _conversationType.asStateFlow()

    private val _conversationId = MutableStateFlow("")
    val conversationId: StateFlow<String> = _conversationId.asStateFlow()
    private val _mediaUploadState = MutableStateFlow(MediaUploadState())
    val mediaUploadState: StateFlow<MediaUploadState> = _mediaUploadState.asStateFlow()

    private val _isConversationBlocked = MutableStateFlow(false)
    val isConversationBlocked: StateFlow<Boolean> = _isConversationBlocked.asStateFlow()

    // User info cache: userId -> User
    private val userCache = mutableMapOf<String, com.example.cs501_micro_chat.data.model.User>()

    private var currentConversationId: String? = null

    /**
     * Load conversation messages and listen for real-time updates.
     */
    fun loadMessages(conversationId: String) {
        if (currentConversationId == conversationId) {
            // Already listening to this conversation
            return
        }

        currentConversationId = conversationId
        _conversationId.value = conversationId
        _isLoading.value = true
        _error.value = null
        _isConversationBlocked.value = false

        viewModelScope.launch {
            loadConversationMeta(conversationId)
            try {
                // Listen for message changes in real time
                chatRepository.observeMessages(conversationId).collect { messageList ->
                    Log.d("ChatDetailViewModel", "Received ${messageList.size} messages")

                    // Fill missing user info
                    val enrichedMessages = enrichMessagesWithUserInfo(messageList)

                    _messages.value = enrichedMessages.sortedBy { it.timestamp }
                    _isLoading.value = false
                    _error.value = null

                    // Mark messages as read
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
            _conversationId.value = convo.id
            _conversationType.value = convo.type
            if (convo.type == com.example.cs501_micro_chat.data.model.ConversationType.PRIVATE) {
                val currentId = _currentUserId.value
                val other = convo.participants.firstOrNull { it != currentId }.orEmpty()
                _otherUserId.value = other
            } else {
                _otherUserId.value = ""
            }
            val blocked = convo.blockedParticipants[_currentUserId.value] == true
            _isConversationBlocked.value = blocked
        }.onFailure { error ->
            Log.e("ChatDetailViewModel", "Failed to load conversation meta", error)
        }
    }

    /**
     * Enrich messages with missing user information.
     */
    private suspend fun enrichMessagesWithUserInfo(messages: List<Message>): List<Message> {
        // Collect sender IDs that need loading when name or avatar is missing
        val userIdsToLoad = messages
            .filter { it.senderName.isBlank() || it.senderAvatarUrl.isBlank() }
            .map { it.senderId }
            .toSet()
            .filter { !userCache.containsKey(it) }

        if (userIdsToLoad.isNotEmpty()) {
            Log.d("ChatDetailViewModel", "Loading user info for ${userIdsToLoad.size} users")

            // Fetch user info in batch
            chatRepository.getUsers(userIdsToLoad).onSuccess { users ->
                userCache.putAll(users)
                Log.d("ChatDetailViewModel", "Loaded ${users.size} users into cache")
            }.onFailure { error ->
                Log.e("ChatDetailViewModel", "Failed to load users", error)
            }
        }

        // Apply cached user info back to messages
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
     * Send text message.
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
                ).onSuccess { sent ->
                    if (sent.status == MessageStatus.FAILED) {
                        _isConversationBlocked.value = true
                    }
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
     * Upload an image to Firebase Storage and send the image message.
     *fun uploadImageMessage(
        conversationId: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        extension: String? = null
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "用户未登录"
            return
        }

        viewModelScope.launch {
            _mediaUploadState.value = _mediaUploadState.value.copy(
                isUploading = true,
                uploadingType = MessageType.IMAGE
            )

            val uploadResult = storageRepository.uploadImage(
                bytes = imageBytes,
                conversationId = conversationId,
                ownerId = userId,
                mimeType = mimeType,
                extension = extension
            )

            if (uploadResult.isFailure) {
                val message = uploadResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "上传图片失败: $message"
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    isUploading = false,
                    uploadingType = null
                )
                return@launch
            }

            val media = uploadResult.getOrThrow()
            val sendResult = chatRepository.sendMessage(
                conversationId = conversationId,
                content = "图片",
                type = MessageType.IMAGE,
                mediaUrl = media.downloadUrl
            )

            if (sendResult.isFailure) {
                val message = sendResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "发送图片消息失败: $message"
                storageRepository.deleteByPath(media.storagePath)
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    isUploading = false,
                    uploadingType = null
                )
                return@launch
            }

            _mediaUploadState.value = _mediaUploadState.value.copy(
                isUploading = false,
                uploadingType = null,
                lastUploadedUrl = media.downloadUrl
            )
        }
    }

    /** Upload an mp3 voice clip and send it as a message. */
    fun uploadVoiceMessage(
        conversationId: String,
        audioBytes: ByteArray,
        durationMillis: Long,
        mimeType: String = "audio/mpeg",
        extension: String? = null
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "用户未登录"
            return
        }

        viewModelScope.launch {
            _mediaUploadState.value = _mediaUploadState.value.copy(
                isUploading = true,
                uploadingType = MessageType.VOICE
            )

            val uploadResult = storageRepository.uploadVoiceMessage(
                bytes = audioBytes,
                conversationId = conversationId,
                ownerId = userId,
                mimeType = mimeType,
                extension = extension
            )

            if (uploadResult.isFailure) {
                val message = uploadResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "上传语音失败: $message"
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    isUploading = false,
                    uploadingType = null
                )
                return@launch
            }

            val media = uploadResult.getOrThrow()
            val seconds = (durationMillis / 1000).coerceAtLeast(1)
            val sendResult = chatRepository.sendMessage(
                conversationId = conversationId,
                content = "语音消息 (${seconds}s)",
                type = MessageType.VOICE,
                mediaUrl = media.downloadUrl
            )

            if (sendResult.isFailure) {
                val message = sendResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "发送语音消息失败: $message"
                storageRepository.deleteByPath(media.storagePath)
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    isUploading = false,
                    uploadingType = null
                )
                return@launch
            }

            _mediaUploadState.value = _mediaUploadState.value.copy(
                isUploading = false,
                uploadingType = null,
                lastUploadedUrl = media.downloadUrl
            )
        }
    }

    /** Mark all messages as read. */
    private fun markAllAsRead(conversationId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            for (message in _messages.value) {
                if (!message.readBy.contains(userId) && message.senderId != userId) {
                    chatRepository.markMessageAsRead(conversationId, message.id)
                        .onFailure { error ->
                            Log.e("ChatDetailViewModel", "Error marking message as read", error)
                        }
                }
            }

            // Clear unread count
            chatRepository.clearUnreadCount(conversationId)
                .onFailure { error ->
                    Log.e("ChatDetailViewModel", "Error clearing unread count", error)
                }
        }
    }

    /** Clear error message. */
    fun clearError() {
        _error.value = null
    }

    /** Reset media upload indicator state. */
    fun clearMediaUploadState() {
        _mediaUploadState.value = MediaUploadState(
            isUploading = false,
            uploadingType = null,
            lastUploadedUrl = null
        )
    }
}
