/**
 * HomeViewModel.kt
 *
 * 主界面 ViewModel - 管理聊天列表数据
 * Home Screen ViewModel - Manages chat list data
 *
 * 主要功能 / Main Functions:
 * - 从 Firebase 获取会话列表 / Fetch conversation list from Firebase
 * - 实时监听会话更新 / Real-time conversation updates
 * - 格式化时间显示 / Format time display
 *
 * @author CS501 Team
 * @date 2025-11-06
 */
package com.example.cs501_micro_chat.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.model.User
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.example.cs501_micro_chat.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth,
    private val storageRepository: StorageRepository
) : ViewModel() {

    val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 用户信息缓存：userId -> User
    private val _userCache = MutableStateFlow<Map<String, com.example.cs501_micro_chat.data.model.User>>(emptyMap())
    val userCache: StateFlow<Map<String, com.example.cs501_micro_chat.data.model.User>> = _userCache.asStateFlow()
    private val _isUsersLoading = MutableStateFlow(false)
    val isUsersLoading: StateFlow<Boolean> = _isUsersLoading.asStateFlow()
    private val _isContactsReady = MutableStateFlow(false)
    val isContactsReady: StateFlow<Boolean> = _isContactsReady.asStateFlow()

    // 搜索相关状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Conversation>>(emptyList())
    val searchResults: StateFlow<List<Conversation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 添加好友搜索相关状态
    private val _addFriendSearchQuery = MutableStateFlow("")
    val addFriendSearchQuery: StateFlow<String> = _addFriendSearchQuery.asStateFlow()

    private val _addFriendSearchResults = MutableStateFlow<List<com.example.cs501_micro_chat.data.model.User>>(emptyList())
    val addFriendSearchResults: StateFlow<List<com.example.cs501_micro_chat.data.model.User>> = _addFriendSearchResults.asStateFlow()

    private val _isAddFriendSearching = MutableStateFlow(false)
    val isAddFriendSearching: StateFlow<Boolean> = _isAddFriendSearching.asStateFlow()

    // 添加群组搜索相关状态
    private val _addGroupSearchQuery = MutableStateFlow("")
    val addGroupSearchQuery: StateFlow<String> = _addGroupSearchQuery.asStateFlow()

    private val _addGroupSearchResults = MutableStateFlow<List<Conversation>>(emptyList())
    val addGroupSearchResults: StateFlow<List<Conversation>> = _addGroupSearchResults.asStateFlow()

    private val _isAddGroupSearching = MutableStateFlow(false)
    val isAddGroupSearching: StateFlow<Boolean> = _isAddGroupSearching.asStateFlow()

    // 已有联系人的 ID 集合（用于判断用户是否已添加）
    private val _existingContactIds = MutableStateFlow<Set<String>>(emptySet())
    val existingContactIds: StateFlow<Set<String>> = _existingContactIds.asStateFlow()

    // 所有联系人的完整信息（用于判断详细状态）
    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    // 待确认的好友请求列表（isNew = true 的联系人）
    private val _pendingFriendRequests = MutableStateFlow<List<Contact>>(emptyList())
    val pendingFriendRequests: StateFlow<List<Contact>> = _pendingFriendRequests.asStateFlow()

    // 置顶会话 ID 集合
    private val _pinnedConversationIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedConversationIds: StateFlow<Set<String>> = _pinnedConversationIds.asStateFlow()

    private val TAG = "HomeViewModel"

    init {
        loadConversations()
        loadExistingContacts()
        observePinnedConversations()
    }

    /**
     * 加载当前用户的所有会话
     */
    fun loadConversations() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            _error.value = "用户未登录"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Loading conversations for user: $userId")

                // 从 ChatRepository 获取会话列表
                val flow = chatRepository.observeUserConversations()
                if (flow == null) {
                    _error.value = "无法获取会话列表"
                    _isLoading.value = false
                    return@launch
                }

                flow.collect { conversations ->
                    Log.d(TAG, "Received ${conversations.size} conversations")
                    val adjusted = applyUserClears(conversations)
                    _conversations.value = applyPinnedSorting(adjusted)
                    _isLoading.value = false

                    // 加载会话中所有参与者的用户信息
                    loadUsersForConversations(adjusted)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                _error.value = "加载会话失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载会话中所有参与者的用户信息
     */
    private fun loadUsersForConversations(conversations: List<Conversation>) {
        viewModelScope.launch {
            _isUsersLoading.value = true
            try {
                // 收集所有需要加载的用户 ID
                val userIds = conversations.flatMap { it.participants }.toSet()
                val currentUserId = auth.currentUser?.uid

                // 过滤掉当前用户自己，其余全部刷新，避免头像缓存过期
                val idsToLoad = userIds.filter { it != currentUserId }
                if (idsToLoad.isEmpty()) {
                    _isUsersLoading.value = false
                    return@launch
                }

                Log.d(TAG, "Loading ${idsToLoad.size} users (refreshing cache): $idsToLoad")

                // 批量获取用户信息（即便已缓存也强制刷新，保证头像同步）
                val result = chatRepository.getUsers(idsToLoad)
                result.onSuccess { users ->
                    Log.d(TAG, "Loaded/updated ${users.size} users")
                    // 覆盖式合并，确保头像等字段使用最新值
                    _userCache.value = _userCache.value + users
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load users", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users for conversations", e)
            } finally {
                _isUsersLoading.value = false
            }
        }
    }

    /**
     * 格式化时间显示
     */
    fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val instantDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val todayDate = LocalDate.now()
        val yesterdayDate = todayDate.minusDays(1)

        Log.d(
            TAG,
            "formatTime ts=$timestamp now=$now diff=$diff msgDate=$instantDate today=$todayDate yesterday=$yesterdayDate"
        )

        return when {
            // 今天 - 显示时间
            instantDate == todayDate -> {
                val formatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> today: $formatted")
                formatted
            }
            // 昨天 - 返回本地化“昨天”
            instantDate == yesterdayDate -> {
                val yesterdayLabel = "Yesterday" // UI layer can swap to stringResource
                Log.d(TAG, "formatTime -> yesterday: $yesterdayLabel")
                yesterdayLabel
            }
            // 一周内 - 显示星期
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                val formatted = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> weekday: $formatted")
                formatted
            }
            // 更早 - 显示日期
            else -> {
                val formatted = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> date: $formatted")
                formatted
            }
        }
    }

    /**
     * 获取当前用户的未读消息数
     */
    fun getUnreadCount(conversation: Conversation): Int {
        val userId = auth.currentUser?.uid ?: return 0
        return conversation.unreadCounts[userId] ?: 0
    }

    /**
     * 从 participants 中获取对方用户的 ID
     * 对于私聊，返回 participants 中不是当前用户的那个 ID
     */
    fun getOtherUserId(conversation: Conversation): String? {
        val currentUserId = auth.currentUser?.uid ?: return null

        // 如果 participants 为空或只有一个人，返回 null
        if (conversation.participants.size < 2) {
            return null
        }

        // 返回第一个不是当前用户的 ID
        return conversation.participants.firstOrNull { it != currentUserId }
    }

    /**
     * 获取会话的显示名称
     * 对于私聊，从缓存中获取对方用户的真实用户名
     */
    fun getDisplayName(conversation: Conversation): String {
        // 对于群聊，使用 conversation.name
        if (conversation.type == com.example.cs501_micro_chat.data.model.ConversationType.GROUP) {
            return conversation.name.ifEmpty { "群聊" }
        }

        // 对于私聊，从缓存中获取对方用户的真实用户名
        val otherUserId = getOtherUserId(conversation)
        if (otherUserId != null) {
            val contactAlias = _allContacts.value.firstOrNull { it.contactId == otherUserId }?.getDisplayName()
            if (!contactAlias.isNullOrBlank()) {
                return contactAlias
            }
        }
        if (otherUserId != null) {
            val otherUser = _userCache.value[otherUserId]
            if (otherUser != null) {
                val name = otherUser.username.ifBlank { otherUser.email.substringBefore("@") }
                if (name.isNotBlank()) return name
            }
        }

        // 如果缓存中没有，返回默认值
        val currentName = auth.currentUser?.displayName.orEmpty()
        val currentEmailPrefix = auth.currentUser?.email?.substringBefore("@").orEmpty()
        val convoName = conversation.name
        // Avoid falling back to my own name; prefer other user's id prefix if possible
        if (convoName.isNotBlank() && convoName != currentName && convoName != currentEmailPrefix) {
            return convoName
        }

        return otherUserId?.takeIf { it.isNotBlank() } ?: "加载中..."
    }

    /**
     * 获取会话的头像 URL
     * 对于私聊，从缓存中获取对方用户的真实头像
     */
    fun getAvatarUrl(conversation: Conversation): String {
        // 对于群聊，使用 conversation.avatarUrl
        if (conversation.type == com.example.cs501_micro_chat.data.model.ConversationType.GROUP) {
            if (conversation.avatarUrl.isNotBlank()) return conversation.avatarUrl
            // 兜底：从联系人缓存读取群头像
            val contact = _allContacts.value.firstOrNull { it.conversationId == conversation.id || it.contactId == conversation.id }
            if (contact != null && contact.contactAvatarUrl.isNotBlank()) {
                return contact.contactAvatarUrl
            }
            return ""
        }

        // 对于私聊，从缓存中获取对方用户的真实头像
        val otherUserId = getOtherUserId(conversation)
        if (otherUserId != null) {
            // 优先使用联系人里的头像（联系人的头像更新最快）
            val contactAvatar = _allContacts.value.firstOrNull { it.contactId == otherUserId }?.contactAvatarUrl
            if (!contactAvatar.isNullOrBlank()) {
                return contactAvatar
            }

            val otherUser = _userCache.value[otherUserId]
            if (otherUser != null) {
                return otherUser.avatarUrl
            }
        }

        // 如果缓存中没有，回退到会话内置头像或空
        return conversation.avatarUrl
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        } else {
            searchConversations(query)
        }
    }

    /**
     * 搜索对话
     * 在当前用户的所有对话中搜索，支持搜索对话名称和最后一条消息
     */
    private fun searchConversations(query: String) {
        _isSearching.value = true

        val normalizedQuery = query.lowercase().trim()

        // 在所有对话中搜索
        val results = _conversations.value.filter { conversation ->
            // 获取显示名称
            val displayName = getDisplayName(conversation)

            // 搜索条件：对话名称 或 最后一条消息内容
            displayName.lowercase().contains(normalizedQuery) ||
            conversation.lastMessage.lowercase().contains(normalizedQuery)
        }

        // 按相关性排序：完全匹配 > 开头匹配 > 包含匹配
        val sortedResults = results.sortedWith(compareBy(
            { conversation ->
                val displayName = getDisplayName(conversation).lowercase()
                when {
                    displayName == normalizedQuery -> 0
                    displayName.startsWith(normalizedQuery) -> 1
                    else -> 2
                }
            },
            // 二级排序：按时间倒序
            { -it.lastMessageTime }
        ))

        _searchResults.value = sortedResults
        _isSearching.value = false

        Log.d(TAG, "Search for '$query' found ${sortedResults.size} results")
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    suspend fun createGroup(
        name: String,
        memberIds: List<String>,
        avatarBytes: ByteArray? = null,
        avatarMimeType: String = "image/jpeg",
        avatarExtension: String? = null
    ): Result<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("Group name cannot be empty"))
        val creatorId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        Log.d(TAG, "createGroup start name=$trimmed members=${memberIds.size}")

        val groupResult = runCatching {
            chatRepository.createGroup(
                name = trimmed,
                description = "",
                avatarUrl = "",
                memberIds = memberIds
            ).getOrThrow()
        }.onFailure {
            Log.e(TAG, "createGroup failed name=$trimmed", it)
        }
        val group = groupResult.getOrElse { return Result.failure(it) }

        if (avatarBytes != null) {
            val upload = storageRepository.uploadImage(
                bytes = avatarBytes,
                conversationId = group.id,
                ownerId = creatorId,
                mimeType = avatarMimeType,
                extension = avatarExtension
            )
            upload.onSuccess { media ->
                Log.d(TAG, "createGroup avatar uploaded url=${media.downloadUrl}")
                chatRepository.updateGroup(group.copy(avatarUrl = media.downloadUrl))
                chatRepository.getConversation(group.id).getOrNull()?.let { convo ->
                    chatRepository.updateConversation(convo.copy(avatarUrl = media.downloadUrl))
                }
            }.onFailure {
                Log.e(TAG, "createGroup avatar upload failed group=${group.id}", it)
            }
        }

        loadConversations()
        return Result.success(group.id)
    }

    /**
     * 搜索全局用户（用于添加好友）
     */
    fun searchUsersForAddFriend(query: String) {
        _addFriendSearchQuery.value = query

        if (query.isBlank()) {
            _addFriendSearchResults.value = emptyList()
            _isAddFriendSearching.value = false
            return
        }

        viewModelScope.launch {
            _isAddFriendSearching.value = true
            try {
                val result = chatRepository.searchUsers(query)
                result.onSuccess { users ->
                    // 过滤掉当前用户自己
                    val currentUserId = auth.currentUser?.uid
                    val filteredUsers = users
                        .filter { it.id != currentUserId }
                        .map { user ->
                            if (user.username.isBlank() && user.email.isNotBlank()) {
                                user.copy(username = user.email.substringBefore("@"))
                            } else {
                                user
                            }
                        }

                    _addFriendSearchResults.value = filteredUsers
                    Log.d(TAG, "Global user search for '$query' found ${filteredUsers.size} results")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to search users", error)
                    _error.value = "搜索失败: ${error.message}"
                    _addFriendSearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users", e)
                _error.value = "搜索失败: ${e.message}"
                _addFriendSearchResults.value = emptyList()
            } finally {
                _isAddFriendSearching.value = false
            }
        }
    }

    /**
     * 加载已有联系人列表（用于判断用户是否已添加）
     * 同时加载待确认的好友请求和已发送的好友请求
     */
    private fun loadExistingContacts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot load contacts")
            _isContactsReady.value = true
            return
        }

        viewModelScope.launch {
            try {
                // 监听联系人列表变化
                val flow = chatRepository.observeContacts()
                if (flow == null) {
                    Log.e(TAG, "Cannot observe contacts")
                    _isContactsReady.value = true
                    return@launch
                }

                flow.collect { contacts ->
                    // 存储所有联系人信息
                    _allContacts.value = contacts

                    Log.d(TAG, "📱 Contacts updated from Firebase:")
                    contacts.forEach { contact ->
                        Log.d(TAG, "  - ${contact.contactId} (${contact.contactName}): isNew=${contact.isNew}, isPending=${contact.isPending}, conversationId=${contact.conversationId}")
                    }

                    // 提取所有联系人的 ID（包括 PRIVATE 和 GROUP）
                    // 包括：已确认的好友、待确认的请求、已发送的请求
                    val contactIds = contacts.map { it.contactId }.toSet()
                    _existingContactIds.value = contactIds
                    Log.d(TAG, "Loaded ${contactIds.size} existing contact IDs: $contactIds")

                    // 提取待确认的好友请求（别人发给我的，isNew = true）
                    val pendingRequests = contacts.filter { it.isNew && it.type == "PRIVATE" }
                    _pendingFriendRequests.value = pendingRequests
                    Log.d(TAG, "Loaded ${pendingRequests.size} pending friend requests")

                    // 统计已发送的请求数量（我发给别人的，isPending = true）
                    val sentRequests = contacts.filter { it.isPending && it.type == "PRIVATE" }
                    Log.d(TAG, "Loaded ${sentRequests.size} sent friend requests")

                    _isContactsReady.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading existing contacts", e)
                _isContactsReady.value = true
            }
        }
    }

    /**
     * 检查用户是否已经是联系人
     */
    fun isUserAlreadyAdded(userId: String): Boolean {
        return _existingContactIds.value.contains(userId)
    }

    /**
     * 获取用户的联系人状态
     * @return "added" - 已添加为好友, "pending" - 已发送请求等待接受, "new" - 收到对方请求, null - 不是联系人
     */
    fun getContactStatus(userId: String): String? {
        val contact = _allContacts.value.find { it.contactId == userId }

        val status = when {
            contact == null -> null
            contact.isPending -> "pending" // 已发送请求，等待对方接受
            contact.isNew -> "new" // 收到对方的请求
            else -> "added" // 已经是好友
        }

        Log.d(TAG, "==================== getContactStatus DEBUG ====================")
        Log.d(TAG, "🔍 Checking status for userId: $userId")
        Log.d(TAG, "📊 Total contacts in _allContacts: ${_allContacts.value.size}")
        if (_allContacts.value.isNotEmpty()) {
            Log.d(TAG, "📋 All contacts:")
            _allContacts.value.forEach { c ->
                Log.d(TAG, "  - ${c.contactId} (${c.contactName}): isNew=${c.isNew}, isPending=${c.isPending}, conversationId='${c.conversationId}'")
            }
        } else {
            Log.d(TAG, "⚠️ _allContacts is EMPTY!")
        }

        if (contact != null) {
            Log.d(TAG, "✅ Found contact for $userId:")
            Log.d(TAG, "  - contactId: ${contact.contactId}")
            Log.d(TAG, "  - contactName: ${contact.contactName}")
            Log.d(TAG, "  - isNew: ${contact.isNew}")
            Log.d(TAG, "  - isPending: ${contact.isPending}")
            Log.d(TAG, "  - conversationId: '${contact.conversationId}'")
            Log.d(TAG, "  - type: ${contact.type}")
        } else {
            Log.d(TAG, "❌ No contact found for $userId")
        }

        Log.d(TAG, "📍 Final status for $userId: $status")
        Log.d(TAG, "===============================================================")

        return status
    }

    /**
     * 清空添加好友搜索
     */
    fun clearAddFriendSearch() {
        _addFriendSearchQuery.value = ""
        _addFriendSearchResults.value = emptyList()
        _isAddFriendSearching.value = false
    }

    /**
     * 手动刷新联系人列表（用于调试或确保数据最新）
     */
    fun refreshContacts() {
        Log.d(TAG, "🔄 Manually refreshing contacts from Firebase...")
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "Cannot refresh: User not logged in")
                    return@launch
                }

                // 直接从 Firebase 获取最新的联系人列表
                val result = chatRepository.getContacts()
                result.onSuccess { contacts ->
                    Log.d(TAG, "✅ Refreshed contacts: ${contacts.size} total")
                    _allContacts.value = contacts
                    _existingContactIds.value = contacts.map { it.contactId }.toSet()

                    contacts.forEach { contact ->
                        Log.d(TAG, "  - ${contact.contactId}: isNew=${contact.isNew}, isPending=${contact.isPending}")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to refresh contacts: ${error.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during refresh: ${e.message}", e)
            }
        }
    }

    private fun observePinnedConversations() {
        val flow = chatRepository.observePinnedConversations() ?: return
        viewModelScope.launch {
            flow.collect { pinned ->
                _pinnedConversationIds.value = pinned
                if (_conversations.value.isNotEmpty()) {
                    _conversations.value = applyPinnedSorting(_conversations.value)
                }
            }
        }
    }

    /**
     * 发送好友请求
     */
    fun sendFriendRequest(targetUser: User) {
        viewModelScope.launch {
            try {
                val targetUserId = targetUser.id
                val currentUserId = auth.currentUser?.uid
                if (currentUserId.isNullOrBlank()) {
                    _error.value = "用户未登录"
                    return@launch
                }
                if (targetUserId.isBlank()) {
                    _error.value = "目标用户信息不完整，无法发送请求"
                    return@launch
                }

                Log.d(TAG, "Sending friend request to user: $targetUserId")
                val result = chatRepository.sendFriendRequest(targetUserId)
                result.onSuccess {
                    Log.d(TAG, "Friend request sent successfully")
                    // Optimistically mark as pending locally so UI reflects immediately
                    val updatedContacts = _allContacts.value.toMutableList()
                    val existingIndex = updatedContacts.indexOfFirst { it.contactId == targetUserId }
                    val pendingContact = Contact(
                        userId = currentUserId,
                        contactId = targetUserId,
                        contactName = targetUser.username.ifBlank { targetUser.email.substringBefore("@") },
                        contactAvatarUrl = targetUser.avatarUrl,
                        type = "PRIVATE",
                        isNew = false,
                        isPending = true,
                        conversationId = ""
                    )
                    if (existingIndex >= 0) {
                        updatedContacts[existingIndex] = pendingContact
                    } else {
                        updatedContacts.add(pendingContact)
                    }
                    _allContacts.value = updatedContacts
                    _existingContactIds.value = _existingContactIds.value + targetUserId

                    // 清空搜索结果
                    clearAddFriendSearch()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to send friend request", error)
                    _error.value = "发送好友请求失败: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending friend request", e)
                _error.value = "发送好友请求失败: ${e.message}"
            }
        }
    }

    /**
     * 接受好友请求
     */
    fun acceptFriendRequest(requesterId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Accepting friend request from user: $requesterId")
                val result = chatRepository.acceptFriendRequest(requesterId)
                result.onSuccess {
                    Log.d(TAG, "Friend request accepted successfully")
                    // 重新加载对话列表
                    loadConversations()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to accept friend request", error)
                    _error.value = "接受好友请求失败: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting friend request", e)
                _error.value = "接受好友请求失败: ${e.message}"
            }
        }
    }

    /**
     * 拒绝好友请求
     */
    fun rejectFriendRequest(requesterId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Rejecting friend request from user: $requesterId")
                val result = chatRepository.rejectFriendRequest(requesterId)
                result.onSuccess {
                    Log.d(TAG, "Friend request rejected successfully")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to reject friend request", error)
                    _error.value = "拒绝好友请求失败: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting friend request", e)
                _error.value = "拒绝好友请求失败: ${e.message}"
            }
        }
    }

    private fun applyPinnedSorting(conversations: List<Conversation>): List<Conversation> {
        if (conversations.isEmpty()) return conversations
        val pinnedConversationIds = _pinnedConversationIds.value
        if (pinnedConversationIds.isEmpty()) {
            return conversations.sortedByDescending { it.lastMessageTime }
        }
        return conversations.sortedWith(
            compareByDescending<Conversation> { pinnedConversationIds.contains(it.id) }
                .thenByDescending { it.lastMessageTime }
        )
    }

    /**
     * Apply per-user clear timestamp: hide last message and time if it was cleared.
     */
    private fun applyUserClears(conversations: List<Conversation>): List<Conversation> {
        val userId = currentUserId
        if (userId.isBlank()) return conversations
        return conversations.map { convo ->
            val clearedAt = convo.clearedAt[userId] ?: 0L
            if (clearedAt > 0 && convo.lastMessageTime <= clearedAt) {
                // Hide last message preview for this user, but keep timestamp for sorting
                convo.copy(lastMessage = "")
            } else {
                convo
            }
        }
    }
}
