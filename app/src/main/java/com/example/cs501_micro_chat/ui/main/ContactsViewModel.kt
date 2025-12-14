/**
 * ContactsViewModel.kt
 *
 * 联系人页面 ViewModel - 管理联系人列表数据
 * Contacts Screen ViewModel - Manages contacts list data
 *
 * 主要功能 / Main Functions:
 * - 从 Firebase 获取联系人列表 / Fetch contacts list from Firebase
 * - 实时监听联系人更新 / Real-time contacts updates
 * - 按类型和字母顺序排序 / Sort by type and alphabetical order
 * - 为 GROUP 类型从 Conversation 获取真实信息
 *
 * @author CS501 Team
 * @date 2025-11-16
 */
package com.example.cs501_micro_chat.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _groups = MutableStateFlow<List<Contact>>(emptyList())
    val groups: StateFlow<List<Contact>> = _groups.asStateFlow()

    private val _privateContacts = MutableStateFlow<List<Contact>>(emptyList())
    val privateContacts: StateFlow<List<Contact>> = _privateContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pinnedConversationIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedConversationIds: StateFlow<Set<String>> = _pinnedConversationIds.asStateFlow()

    // Cache Conversation data to display real GROUP information
    private val _conversationCache = MutableStateFlow<Map<String, Conversation>>(emptyMap())
    val conversationCache: StateFlow<Map<String, Conversation>> = _conversationCache.asStateFlow()

    // Search-related state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val TAG = "ContactsViewModel"

    init {
        loadContacts()
        observePinnedConversations()
    }

    /**
     * Load all contacts for the current user
     */
    fun loadContacts() {
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
                Log.d(TAG, "Loading contacts for user: $userId")

                // Fetch the contacts list from ChatRepository
                val flow = chatRepository.observeContacts()
                if (flow == null) {
                    _error.value = "无法获取联系人列表"
                    _isLoading.value = false
                    return@launch
                }

                flow.collect { allContacts ->
                    Log.d(TAG, "Received ${allContacts.size} contacts")

                    // Filter confirmed contacts
                    // Rules:
                    // - GROUP: always display
                    // - PRIVATE with isNew = false and isPending = false: confirmed friend, display
                    // - PRIVATE with isNew = false and isPending = true: request sent and pending approval, do not display
                    // - PRIVATE with isNew = true and isPending = false: incoming request pending confirmation, do not display
                    //   (shown in the top requests section)o
                    val confirmedContacts = allContacts.filter { contact ->
                        val shouldShow = if (contact.type == "GROUP") {
                            Log.d(TAG, "✅ Contact ${contact.contactId} (GROUP): SHOW")
                            true
                        } else {
                            // Personal contacts: display confirmed friends only
                            val result = !contact.isNew && !contact.isPending
                            val reason = when {
                                contact.isNew && contact.isPending -> "isNew=true & isPending=true (异常状态)"
                                contact.isNew -> "isNew=true (收到的请求，等待确认)"
                                contact.isPending -> "isPending=true (已发送的请求，等待接受)"
                                else -> "已确认好友"
                            }
                            Log.d(TAG, "${if (result) "✅" else "❌"} Contact ${contact.contactId} (${contact.contactName}): isNew=${contact.isNew}, isPending=${contact.isPending} → $reason → ${if (result) "SHOW" else "HIDE"}")
                            result
                        }
                        shouldShow
                    }

                    Log.d(TAG, "Confirmed contacts: ${confirmedContacts.size} (filtered out ${allContacts.size - confirmedContacts.size} pending/new)")

                    _contacts.value = confirmedContacts
                    _isLoading.value = false

                    // Load Conversation information for GROUP-type contacts
                    loadConversationsForGroups(confirmedContacts)

                    // 分类并排序
                    val (groups, privateContacts) = confirmedContacts.partition { it.isGroup() }

                    // Sort by display name in alphabetical order
                    _groups.value = groups.sortedBy { it.getDisplayName().lowercase() }
                    _privateContacts.value = privateContacts.sortedBy { it.getDisplayName().lowercase() }

                    Log.d(TAG, "Groups: ${_groups.value.size}, Private: ${_privateContacts.value.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading contacts", e)
                _error.value = "加载联系人失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun observePinnedConversations() {
        val flow = chatRepository.observePinnedConversations() ?: return
        viewModelScope.launch {
            flow.collect { pinned ->
                _pinnedConversationIds.value = pinned
            }
        }
    }

    /**
     * Load Conversation information for GROUP-type contacts
     */
    private fun loadConversationsForGroups(contacts: List<Contact>) {
        viewModelScope.launch {
            try {
                // Retrieve all conversationIds for GROUP-type contacts
                val groupConversationIds = contacts
                    .filter { it.isGroup() && it.conversationId.isNotBlank() }
                    .map { it.conversationId }
                    .distinct()

                if (groupConversationIds.isEmpty()) {
                    Log.d(TAG, "No group conversations to load")
                    return@launch
                }

                Log.d(TAG, "Loading ${groupConversationIds.size} group conversations")

                // Fetch Conversation information in bulk
                val newCache = mutableMapOf<String, Conversation>()
                groupConversationIds.forEach { conversationId ->
                    val result = chatRepository.getConversation(conversationId)
                    result.onSuccess { conversation ->
                        if (conversation != null) {
                            newCache[conversationId] = conversation
                            Log.d(TAG, "Loaded conversation: ${conversation.name} (${conversationId})")
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to load conversation $conversationId", error)
                    }
                }

                // update cache
                _conversationCache.value = newCache
                Log.d(TAG, "Conversation cache updated with ${newCache.size} entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations for groups", e)
            }
        }
    }

    /**
     * Get the display name of a contact
     * For GROUP contacts, retrieve the actual name from the Conversation
     */
    fun getDisplayName(contact: Contact): String {
        if (contact.isGroup()) {
            val conversation = _conversationCache.value[contact.conversationId]
            if (conversation != null) {
                return conversation.name.ifEmpty { contact.getDisplayName() }
            }
        }
        return contact.getDisplayName()
    }

    /**
     * Get the avatar URL of a contact
     * For GROUP contacts, retrieve the actual avatar from the Conversation
     */
    fun getAvatarUrl(contact: Contact): String {
        if (contact.isGroup()) {
            val conversation = _conversationCache.value[contact.conversationId]
            if (conversation != null) {
                return conversation.avatarUrl.ifEmpty { contact.contactAvatarUrl }
            }
        }
        return contact.contactAvatarUrl
    }

    /**
     * Refresh the contacts list
     */
    fun refresh() {
        loadContacts()
    }

    /**
     * Update the search keyword
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        } else {
            searchContacts(query)
        }
    }

    /**
     * Search contacts
     * Searches within the current user's contacts, supporting contactName and contactId
     */

    private fun searchContacts(query: String) {
        _isSearching.value = true

        val normalizedQuery = query.lowercase().trim()

        // Search across all contacts (including groups and private contacts)
        val allContacts = _contacts.value

        val results = allContacts.filter { contact ->
            // Get display name
            val displayName = getDisplayName(contact)

            // Search criteria: contactId or contactName (including remarks)
            contact.contactId.lowercase().contains(normalizedQuery) ||
            displayName.lowercase().contains(normalizedQuery) ||
            contact.contactName.lowercase().contains(normalizedQuery)
        }

        // Sort by relevance: exact match > prefix match > partial match
        val sortedResults = results.sortedWith(compareBy(
            { contact ->
                val displayName = getDisplayName(contact).lowercase()
                when {
                    displayName == normalizedQuery -> 0
                    displayName.startsWith(normalizedQuery) -> 1
                    else -> 2
                }
            },
            { getDisplayName(it).lowercase() }
        ))

        _searchResults.value = sortedResults
        _isSearching.value = false

        Log.d(TAG, "Search for '$query' found ${sortedResults.size} results")
    }

    /**
     * Clear search state
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
