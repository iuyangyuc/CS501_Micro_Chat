/**
 * HomeViewModel.kt
 *
 * Home Screen ViewModel - Manages chat list data
 *
 * Main Functions:
 * - Fetch conversation list from Firebase
 * - Real-time conversation updates
 * - Format time display
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

    // User info cache: userId -> User
    private val _userCache = MutableStateFlow<Map<String, com.example.cs501_micro_chat.data.model.User>>(emptyMap())
    val userCache: StateFlow<Map<String, com.example.cs501_micro_chat.data.model.User>> = _userCache.asStateFlow()
    private val _isUsersLoading = MutableStateFlow(false)
    val isUsersLoading: StateFlow<Boolean> = _isUsersLoading.asStateFlow()
    private val _isContactsReady = MutableStateFlow(false)
    val isContactsReady: StateFlow<Boolean> = _isContactsReady.asStateFlow()

    // Search related states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Conversation>>(emptyList())
    val searchResults: StateFlow<List<Conversation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Add friend search related states
    private val _addFriendSearchQuery = MutableStateFlow("")
    val addFriendSearchQuery: StateFlow<String> = _addFriendSearchQuery.asStateFlow()

    private val _addFriendSearchResults = MutableStateFlow<List<com.example.cs501_micro_chat.data.model.User>>(emptyList())
    val addFriendSearchResults: StateFlow<List<com.example.cs501_micro_chat.data.model.User>> = _addFriendSearchResults.asStateFlow()

    private val _isAddFriendSearching = MutableStateFlow(false)
    val isAddFriendSearching: StateFlow<Boolean> = _isAddFriendSearching.asStateFlow()

    // Add group search results
    private val _addGroupSearchResults = MutableStateFlow<List<com.example.cs501_micro_chat.data.model.Group>>(emptyList())
    val addGroupSearchResults: StateFlow<List<com.example.cs501_micro_chat.data.model.Group>> = _addGroupSearchResults.asStateFlow()

    // Set of existing contact IDs (used to check whether a user has already been added)
    private val _existingContactIds = MutableStateFlow<Set<String>>(emptySet())
    val existingContactIds: StateFlow<Set<String>> = _existingContactIds.asStateFlow()

    // Full information for all contacts (used to determine detailed status)
    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    // List of pending friend requests (contacts with isNew = true)
    private val _pendingFriendRequests = MutableStateFlow<List<Contact>>(emptyList())
    val pendingFriendRequests: StateFlow<List<Contact>> = _pendingFriendRequests.asStateFlow()

    // Set of pinned conversation IDs
    private val _pinnedConversationIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedConversationIds: StateFlow<Set<String>> = _pinnedConversationIds.asStateFlow()

    private val TAG = "HomeViewModel"

    init {
        loadConversations()
        loadExistingContacts()
        observePinnedConversations()
    }

    /**
     * Load all conversations for the current user
     */
    fun loadConversations() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Loading conversations for user: $userId")

                // Fetch the conversation list from ChatRepository
                val flow = chatRepository.observeUserConversations()
                if (flow == null) {
                    _error.value = "Unable to fetch conversation list"
                    _isLoading.value = false
                    return@launch
                }

                flow.collect { conversations ->
                    Log.d(TAG, "Received ${conversations.size} conversations")
                    val adjusted = applyUserClears(conversations)
                    _conversations.value = applyPinnedSorting(adjusted)
                    _isLoading.value = false

                    // Load user information for all participants in the conversations
                    loadUsersForConversations(adjusted)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                _error.value = "Failed to load conversations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load user information for all participants in the conversations
     */
    private fun loadUsersForConversations(conversations: List<Conversation>) {
        viewModelScope.launch {
            _isUsersLoading.value = true
            try {
                // Collect all user IDs that need to be loaded
                val userIds = conversations.flatMap { it.participants }.toSet()
                val currentUserId = auth.currentUser?.uid

                // Exclude the current user and refresh all others to avoid stale avatar cache
                val idsToLoad = userIds.filter { it != currentUserId }
                if (idsToLoad.isEmpty()) {
                    _isUsersLoading.value = false
                    return@launch
                }

                Log.d(TAG, "Loading ${idsToLoad.size} users (refreshing cache): $idsToLoad")

                // Batch fetch user info (force refresh even if cached to ensure avatar sync)
                val result = chatRepository.getUsers(idsToLoad)
                result.onSuccess { users ->
                    Log.d(TAG, "Loaded/updated ${users.size} users")
                    // Overwrite merge to ensure fields like avatar use the latest values
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
     * Format time display
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
            // Today - show time
            instantDate == todayDate -> {
                val formatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> today: $formatted")
                formatted
            }
            // Yesterday - return localized "Yesterday"
            instantDate == yesterdayDate -> {
                val yesterdayLabel = "Yesterday" // UI layer can swap to stringResource
                Log.d(TAG, "formatTime -> yesterday: $yesterdayLabel")
                yesterdayLabel
            }
            // Within a week - show weekday
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                val formatted = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> weekday: $formatted")
                formatted
            }
            // Older - show date
            else -> {
                val formatted = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                Log.d(TAG, "formatTime -> date: $formatted")
                formatted
            }
        }
    }

    /**
     * Get unread message count for the current user
     */
    fun getUnreadCount(conversation: Conversation): Int {
        val userId = auth.currentUser?.uid ?: return 0
        return conversation.unreadCounts[userId] ?: 0
    }

    /**
     * Get the other user's ID from participants
     * For private chats, returns the ID that is not the current user
     */
    fun getOtherUserId(conversation: Conversation): String? {
        val currentUserId = auth.currentUser?.uid ?: return null

        // If participants is empty or has only one person, return null
        if (conversation.participants.size < 2) {
            return null
        }

        // Return the first ID that is not the current user
        return conversation.participants.firstOrNull { it != currentUserId }
    }

    /**
     * Get display name for conversation
     * For private chats, get the other user's real username from cache
     */
    fun getDisplayName(conversation: Conversation): String {
        // For group chats, use conversation.name
        if (conversation.type == com.example.cs501_micro_chat.data.model.ConversationType.GROUP) {
            return conversation.name.ifEmpty { "Group Chat" }
        }

        // For private chats, get the other user's real username from cache
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

        // If not in cache, return default value
        val currentName = auth.currentUser?.displayName.orEmpty()
        val currentEmailPrefix = auth.currentUser?.email?.substringBefore("@").orEmpty()
        val convoName = conversation.name
        // Avoid falling back to my own name; prefer other user's id prefix if possible
        if (convoName.isNotBlank() && convoName != currentName && convoName != currentEmailPrefix) {
            return convoName
        }

        return otherUserId?.takeIf { it.isNotBlank() } ?: "Loading..."
    }

    /**
     * Get avatar URL for conversation
     * For private chats, get the other user's real avatar from cache
     */
    fun getAvatarUrl(conversation: Conversation): String {
        // For group chats, use conversation.avatarUrl
        if (conversation.type == com.example.cs501_micro_chat.data.model.ConversationType.GROUP) {
            if (conversation.avatarUrl.isNotBlank()) return conversation.avatarUrl
            // Fallback: read group avatar from contact cache
            val contact = _allContacts.value.firstOrNull { it.conversationId == conversation.id || it.contactId == conversation.id }
            if (contact != null && contact.contactAvatarUrl.isNotBlank()) {
                return contact.contactAvatarUrl
            }
            return ""
        }

        // For private chats, get the other user's real avatar from cache
        val otherUserId = getOtherUserId(conversation)
        if (otherUserId != null) {
            // Prefer contact avatar (contact avatar updates fastest)
            val contactAvatar = _allContacts.value.firstOrNull { it.contactId == otherUserId }?.contactAvatarUrl
            if (!contactAvatar.isNullOrBlank()) {
                return contactAvatar
            }

            val otherUser = _userCache.value[otherUserId]
            if (otherUser != null) {
                return otherUser.avatarUrl
            }
        }

        // If not in cache, fall back to conversation's built-in avatar or empty
        return conversation.avatarUrl
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Update search query
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
     * Search conversations
     * Search in all conversations of the current user, supports searching by conversation name and last message
     */
    private fun searchConversations(query: String) {
        _isSearching.value = true

        val normalizedQuery = query.lowercase().trim()

        // Search in all conversations
        val results = _conversations.value.filter { conversation ->
            // Get display name
            val displayName = getDisplayName(conversation)

            // Search criteria: conversation name OR last message content
            displayName.lowercase().contains(normalizedQuery) ||
            conversation.lastMessage.lowercase().contains(normalizedQuery)
        }

        // Sort by relevance: exact match > prefix match > contains match
        val sortedResults = results.sortedWith(compareBy(
            { conversation ->
                val displayName = getDisplayName(conversation).lowercase()
                when {
                    displayName == normalizedQuery -> 0
                    displayName.startsWith(normalizedQuery) -> 1
                    else -> 2
                }
            },
            // Secondary sort: by time descending
            { -it.lastMessageTime }
        ))

        _searchResults.value = sortedResults
        _isSearching.value = false

        Log.d(TAG, "Search for '$query' found ${sortedResults.size} results")
    }

    /**
     * Clear search
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

        // Validation: at least 1 member is required (plus the creator makes 2)
        if (memberIds.isEmpty()) {
            return Result.failure(Exception("At least 1 member is required to create a group"))
        }

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
     * Search global users (for adding friends)
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
                    // Filter out current user
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
                    _error.value = "Search failed: ${error.message}"
                    _addFriendSearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users", e)
                _error.value = "Search failed: ${e.message}"
                _addFriendSearchResults.value = emptyList()
            } finally {
                _isAddFriendSearching.value = false
            }
        }
    }

    /**
     * Load existing contacts list (used to check if a user has already been added)
     * Also loads pending friend requests and sent friend requests
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
                // Observe contact list changes
                val flow = chatRepository.observeContacts()
                if (flow == null) {
                    Log.e(TAG, "Cannot observe contacts")
                    _isContactsReady.value = true
                    return@launch
                }

                flow.collect { contacts ->
                    // Store all contact information
                    _allContacts.value = contacts

                    Log.d(TAG, "📱 Contacts updated from Firebase:")
                    contacts.forEach { contact ->
                        Log.d(TAG, "  - ${contact.contactId} (${contact.contactName}): isNew=${contact.isNew}, isPending=${contact.isPending}, conversationId=${contact.conversationId}")
                    }

                    // Extract all contact IDs (including PRIVATE and GROUP)
                    // Including: confirmed friends, pending requests, sent requests
                    val contactIds = contacts.map { it.contactId }.toSet()
                    _existingContactIds.value = contactIds
                    Log.d(TAG, "Loaded ${contactIds.size} existing contact IDs: $contactIds")

                    // Extract pending friend requests (received from others, isNew = true)
                    val pendingRequests = contacts.filter { it.isNew && it.type == "PRIVATE" }
                    _pendingFriendRequests.value = pendingRequests
                    Log.d(TAG, "Loaded ${pendingRequests.size} pending friend requests")

                    // Count sent requests (sent by me, isPending = true)
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
     * Check if a user is already a contact
     */
    fun isUserAlreadyAdded(userId: String): Boolean {
        return _existingContactIds.value.contains(userId)
    }

    /**
     * Get contact status for a user
     * @return "added" - already friends, "pending" - request sent waiting for acceptance, "new" - received request from them, null - not a contact
     */
    fun getContactStatus(userId: String): String? {
        val contact = _allContacts.value.find { it.contactId == userId }

        val status = when {
            contact == null -> null
            contact.isPending -> "pending" // Request sent, waiting for acceptance
            contact.isNew -> "new" // Received request from them
            else -> "added" // Already friends
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
     * Clear add friend search
     */
    fun clearAddFriendSearch() {
        _addFriendSearchQuery.value = ""
        _addFriendSearchResults.value = emptyList()
        _addGroupSearchResults.value = emptyList()
        _isAddFriendSearching.value = false
    }

    /**
     * Search users and groups (for adding friends/joining groups)
     */
    fun searchUsersAndGroupsForAdd(query: String) {
        _addFriendSearchQuery.value = query

        if (query.isBlank()) {
            _addFriendSearchResults.value = emptyList()
            _addGroupSearchResults.value = emptyList()
            _isAddFriendSearching.value = false
            return
        }

        viewModelScope.launch {
            _isAddFriendSearching.value = true
            try {
                // Search users and groups in parallel
                val userResult = chatRepository.searchUsers(query)
                val groupResult = chatRepository.searchGroups(query)

                // Process user search results
                userResult.onSuccess { users ->
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
                    Log.d(TAG, "User search for '$query' found ${filteredUsers.size} results")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to search users", error)
                    _addFriendSearchResults.value = emptyList()
                }

                // Process group search results
                groupResult.onSuccess { groups ->
                    val currentUserId = auth.currentUser?.uid
                    // Filter out groups the user has already joined
                    val filteredGroups = groups.filter { group ->
                        currentUserId == null || !group.memberIds.contains(currentUserId)
                    }
                    _addGroupSearchResults.value = filteredGroups
                    Log.d(TAG, "Group search for '$query' found ${filteredGroups.size} results")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to search groups", error)
                    _addGroupSearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching users and groups", e)
                _addFriendSearchResults.value = emptyList()
                _addGroupSearchResults.value = emptyList()
            } finally {
                _isAddFriendSearching.value = false
            }
        }
    }

    /**
     * Join a group
     */
    fun joinGroup(groupId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = chatRepository.joinGroup(groupId)
                result.onSuccess {
                    Log.d(TAG, "Successfully joined group: $groupId")
                    // Remove the joined group from search results
                    _addGroupSearchResults.value = _addGroupSearchResults.value.filter { it.id != groupId }
                    // Refresh conversation list
                    loadConversations()
                    onSuccess()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to join group: $groupId", error)
                    onError(error.message ?: "Failed to join group")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error joining group: $groupId", e)
                onError(e.message ?: "Failed to join group")
            }
        }
    }

    /**
     * Manually refresh contacts list (for debugging or ensuring data is up-to-date)
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

                // Fetch the latest contact list directly from Firebase
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
     * Send friend request
     */
    fun sendFriendRequest(targetUser: User) {
        viewModelScope.launch {
            try {
                val targetUserId = targetUser.id
                val currentUserId = auth.currentUser?.uid
                if (currentUserId.isNullOrBlank()) {
                    _error.value = "User not logged in"
                    return@launch
                }
                if (targetUserId.isBlank()) {
                    _error.value = "Target user info incomplete, cannot send request"
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

                    // Clear search results
                    clearAddFriendSearch()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to send friend request", error)
                    _error.value = "Failed to send friend request: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending friend request", e)
                _error.value = "Failed to send friend request: ${e.message}"
            }
        }
    }

    /**
     * Accept friend request
     */
    fun acceptFriendRequest(requesterId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Accepting friend request from user: $requesterId")
                val result = chatRepository.acceptFriendRequest(requesterId)
                result.onSuccess {
                    Log.d(TAG, "Friend request accepted successfully")
                    // Reload conversation list
                    loadConversations()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to accept friend request", error)
                    _error.value = "Failed to accept friend request: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting friend request", e)
                _error.value = "Failed to accept friend request: ${e.message}"
            }
        }
    }

    /**
     * Reject friend request
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
                    _error.value = "Failed to reject friend request: ${error.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting friend request", e)
                _error.value = "Failed to reject friend request: ${e.message}"
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
