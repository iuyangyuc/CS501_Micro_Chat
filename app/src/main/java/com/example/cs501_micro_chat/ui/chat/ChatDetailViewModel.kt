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
import com.example.cs501_micro_chat.data.preferences.LanguagePreferencesRepository
import com.example.cs501_micro_chat.data.repository.StorageRepository
import com.example.cs501_micro_chat.data.repository.TranscriptionRepository
import com.example.cs501_micro_chat.data.repository.TranslationRepository
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

data class TranslationResultState(
    val translatedText: String? = null,
    val targetLanguage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class VoiceTranscriptionState(
    val text: String? = null,
    val translatedText: String? = null,
    val translatedLanguage: String? = null,
    val isLoading: Boolean = false,
    val isTranslating: Boolean = false,
    val errorMessage: String? = null,
    val translationError: String? = null
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
    private val transcriptionRepository: TranscriptionRepository,
    private val translationRepository: TranslationRepository,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasLoadedInitial = MutableStateFlow(false)
    val hasLoadedInitial: StateFlow<Boolean> = _hasLoadedInitial.asStateFlow()

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

    private val _isConversationBlocked = MutableStateFlow(false)
    val isConversationBlocked: StateFlow<Boolean> = _isConversationBlocked.asStateFlow()

    private val _mediaUploadState = MutableStateFlow(MediaUploadState())
    val mediaUploadState: StateFlow<MediaUploadState> = _mediaUploadState.asStateFlow()

    private val _translationStates = MutableStateFlow<Map<String, TranslationResultState>>(emptyMap())
    val translationStates: StateFlow<Map<String, TranslationResultState>> = _translationStates.asStateFlow()

    private val _voiceTranscriptionStates = MutableStateFlow<Map<String, VoiceTranscriptionState>>(emptyMap())
    val voiceTranscriptionStates: StateFlow<Map<String, VoiceTranscriptionState>> = _voiceTranscriptionStates.asStateFlow()

    private val _preferredTranslationLanguage = MutableStateFlow(LanguageOption.English)
    val preferredTranslationLanguage: StateFlow<LanguageOption> = _preferredTranslationLanguage.asStateFlow()

    private val _autoTranslateEnabled = MutableStateFlow(false)
    val autoTranslateEnabled: StateFlow<Boolean> = _autoTranslateEnabled.asStateFlow()

    private val _suppressedTranslationKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _suppressedTranscriptionKeys = MutableStateFlow<Set<String>>(emptySet())

    // 用户信息缓存：userId -> User
    private val userCache = mutableMapOf<String, com.example.cs501_micro_chat.data.model.User>()

    private var currentConversationId: String? = null
    private var initialEmptyJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                languagePreferencesRepository.translationLanguage,
                languagePreferencesRepository.autoTranslateEnabled
            ) { translationLanguage, autoTranslate ->
                translationLanguage to autoTranslate
            }.collect { (translationLanguage, autoTranslate) ->
                _preferredTranslationLanguage.value = translationLanguage
                _autoTranslateEnabled.value = autoTranslate
                if (autoTranslate) {
                    maybeAutoTranslate(_messages.value)
                }
            }
        }
    }

    /**
     * Load conversation messages and listen for real-time updates.
     */
    fun loadMessages(conversationId: String) {
        currentConversationId = conversationId
        _conversationId.value = conversationId
        _isLoading.value = true
        _hasLoadedInitial.value = false
        _error.value = null
        _isConversationBlocked.value = false
        _translationStates.value = emptyMap()
        _voiceTranscriptionStates.value = emptyMap()
        _suppressedTranslationKeys.value = emptySet()
        _suppressedTranscriptionKeys.value = emptySet()
        initialEmptyJob?.cancel()
        initialEmptyJob = null

        viewModelScope.launch {
            loadConversationMeta(conversationId)
            try {
                // Listen for message changes in real time
                chatRepository.observeMessages(conversationId).collect { messageList ->
                    Log.d("ChatDetailViewModel", "Received ${messageList.size} messages")

                    // Fill missing user info
                    val enrichedMessages = enrichMessagesWithUserInfo(messageList)

                    initialEmptyJob?.cancel()

                    if (enrichedMessages.isEmpty()) {
                        // Stay in loading until a small timeout to avoid flashing empty state
                        _messages.value = emptyList()
                        _error.value = null
                        _isLoading.value = true
                        _hasLoadedInitial.value = false

                        initialEmptyJob = viewModelScope.launch {
                            delay(1200)
                            if (_messages.value.isEmpty()) {
                                _hasLoadedInitial.value = true
                                _isLoading.value = false
                            }
                        }
                    } else {
                        _messages.value = enrichedMessages.sortedBy { it.timestamp }
                        _hasLoadedInitial.value = true
                        _isLoading.value = false
                        _error.value = null

                        maybeAutoTranslate(enrichedMessages)
                    }

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
     * Upload an mp3 voice clip and send it as a message.
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

    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset media upload indicator state.
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
        _suppressedTranslationKeys.update { it - key }
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

    private fun maybeAutoTranslate(messages: List<Message>) {
        if (!_autoTranslateEnabled.value) return
        val targetLanguage = _preferredTranslationLanguage.value.displayName
        val currentUser = _currentUserId.value

        messages.forEach { message ->
            if (message.type == MessageType.TEXT && message.senderId != currentUser) {
                val key = messageKey(message)
                if (_suppressedTranslationKeys.value.contains(key)) return@forEach
                val existing = _translationStates.value[key]
                if (existing == null) {
                    translateMessage(message, targetLanguage)
                }
            }
        }
    }

    fun clearTranslationFor(message: Message) {
        val key = messageKey(message)
        _translationStates.update { current -> current - key }
        _suppressedTranslationKeys.update { it + key }
    }

    fun clearTranscriptionFor(message: Message) {
        val key = messageKey(message)
        _voiceTranscriptionStates.update { current -> current - key }
        _suppressedTranscriptionKeys.update { it + key }
        _suppressedTranslationKeys.update { it + key }
    }

    fun transcribeVoiceMessage(message: Message) {
        if (message.type != MessageType.VOICE || message.mediaUrl.isBlank()) return
        val key = messageKey(message)

        _suppressedTranscriptionKeys.update { it - key }
        _suppressedTranslationKeys.update { it - key }
        _voiceTranscriptionStates.update { current ->
            current + (key to VoiceTranscriptionState(isLoading = true))
        }

        viewModelScope.launch {
            val result = transcriptionRepository.transcribe(message.mediaUrl)
            val shouldTranslate = _autoTranslateEnabled.value && message.senderId != _currentUserId.value
            val targetLanguage = _preferredTranslationLanguage.value.displayName

            _voiceTranscriptionStates.update { current ->
                val next = result.fold(
                    onSuccess = { text ->
                        VoiceTranscriptionState(
                            text = text,
                            translatedLanguage = if (shouldTranslate) targetLanguage else null,
                            isLoading = false,
                            isTranslating = shouldTranslate,
                            errorMessage = null
                        )
                    },
                    onFailure = { error ->
                        VoiceTranscriptionState(
                            text = null,
                            translatedLanguage = null,
                            isLoading = false,
                            isTranslating = false,
                            errorMessage = "transcription_failed"
                        )
                    }
                )
                current + (key to next)
            }

            if (result.isSuccess && shouldTranslate && !_suppressedTranslationKeys.value.contains(key)) {
                val text = result.getOrThrow()
                val translationResult = translationRepository.translate(
                    text = text,
                    targetLanguage = targetLanguage,
                    sourceLanguage = "auto",
                    instructions = "Sound professional"
                )

                _voiceTranscriptionStates.update { current ->
                    val existing = current[key]
                    val next = translationResult.fold(
                        onSuccess = { translated ->
                            existing?.copy(
                                translatedText = translated,
                                isTranslating = false,
                                translationError = null,
                                translatedLanguage = targetLanguage
                            ) ?: VoiceTranscriptionState(
                                text = text,
                                translatedText = translated,
                                translatedLanguage = targetLanguage,
                                isTranslating = false
                            )
                        },
                        onFailure = { error ->
                            existing?.copy(
                                isTranslating = false,
                                translationError = "translation_failed",
                                translatedLanguage = targetLanguage
                            ) ?: VoiceTranscriptionState(
                                text = text,
                                isTranslating = false,
                                translationError = "translation_failed",
                                translatedLanguage = targetLanguage
                            )
                        }
                    )
                    current + (key to next)
                }
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
