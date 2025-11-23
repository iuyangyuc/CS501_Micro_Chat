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
import com.example.cs501_micro_chat.data.repository.StorageRepository
import com.example.cs501_micro_chat.data.repository.TranslationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranslationResultState(
    val translatedText: String? = null,
    val targetLanguage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class MediaUploadState(
    val isUploading: Boolean = false,
    val uploadingType: MessageType? = null,
    val lastUploadedUrl: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository,
    private val translationRepository: TranslationRepository,
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

    private val _translationStates = MutableStateFlow<Map<String, TranslationResultState>>(emptyMap())
    val translationStates: StateFlow<Map<String, TranslationResultState>> = _translationStates.asStateFlow()

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
        _conversationId.value = conversationId
        _isLoading.value = true
        _error.value = null
        _translationStates.value = emptyMap()

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
            _conversationId.value = convo.id
            _conversationType.value = convo.type
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
     * 上传图片到 Firebase Storage 并发送图片消息
     */
    fun uploadImageMessage(
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

    /**
     * 上传 mp3 语音并发送语音消息
     */
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

    /**
     * 上传视频并发送视频消息
     */
    fun uploadVideoMessage(
        conversationId: String,
        videoBytes: ByteArray,
        mimeType: String = "video/mp4",
        extension: String? = null
    ) {
        val userId = auth.currentUser?.uid ?: run {
            _error.value = "用户未登录"
            return
        }

        viewModelScope.launch {
            _mediaUploadState.value = _mediaUploadState.value.copy(
                isUploading = true,
                uploadingType = MessageType.VIDEO
            )

            val uploadResult = storageRepository.uploadImage(
                bytes = videoBytes,
                conversationId = conversationId,
                ownerId = userId,
                mimeType = mimeType,
                extension = extension
            )

            if (uploadResult.isFailure) {
                val message = uploadResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "上传视频失败: $message"
                _mediaUploadState.value = _mediaUploadState.value.copy(
                    isUploading = false,
                    uploadingType = null
                )
                return@launch
            }

            val media = uploadResult.getOrThrow()
            val sendResult = chatRepository.sendMessage(
                conversationId = conversationId,
                content = "视频",
                type = MessageType.VIDEO,
                mediaUrl = media.downloadUrl
            )

            if (sendResult.isFailure) {
                val message = sendResult.exceptionOrNull()?.message ?: "未知错误"
                _error.value = "发送视频消息失败: $message"
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

    /**
     * 重置媒体上传提示状态
     */
    fun clearMediaUploadState() {
        _mediaUploadState.value = MediaUploadState(
            isUploading = false,
            uploadingType = null,
            lastUploadedUrl = null
        )
    }

    fun translateMessage(message: Message, targetLanguage: String) {
        val normalizedTarget = targetLanguage.trim()
        if (message.type != MessageType.TEXT || normalizedTarget.isEmpty()) return

        val key = messageKey(message)
        _translationStates.update { current ->
            current + (key to TranslationResultState(isLoading = true, targetLanguage = normalizedTarget))
        }

        viewModelScope.launch {
            val result = translationRepository.translate(
                text = message.content,
                targetLanguage = normalizedTarget,
                sourceLanguage = "auto",
                instructions = "Sound professional"
            )

            _translationStates.update { current ->
                val nextState = result.fold(
                    onSuccess = { translated ->
                        TranslationResultState(
                            translatedText = translated,
                            targetLanguage = normalizedTarget,
                            isLoading = false,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        TranslationResultState(
                            translatedText = null,
                            targetLanguage = normalizedTarget,
                            isLoading = false,
                            errorMessage = error.message ?: "Translation failed"
                        )
                    }
                )
                current + (key to nextState)
            }
        }
    }
}

internal fun messageKey(message: Message): String {
    return if (message.id.isNotBlank()) {
        message.id
    } else {
        "${message.timestamp}_${message.senderId}_${message.content.hashCode()}"
    }
}
