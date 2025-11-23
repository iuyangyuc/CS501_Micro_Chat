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

    // 缓存 Conversation 数据，用于显示 GROUP 的真实信息
    private val _conversationCache = MutableStateFlow<Map<String, Conversation>>(emptyMap())
    val conversationCache: StateFlow<Map<String, Conversation>> = _conversationCache.asStateFlow()

    // 搜索相关状态
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
     * 加载当前用户的所有联系人
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

                // 从 ChatRepository 获取联系人列表
                val flow = chatRepository.observeContacts()
                if (flow == null) {
                    _error.value = "无法获取联系人列表"
                    _isLoading.value = false
                    return@launch
                }

                flow.collect { allContacts ->
                    Log.d(TAG, "Received ${allContacts.size} contacts")

                    // 过滤出已确认的联系人
                    // 规则：
                    // - GROUP: 直接显示
                    // - PRIVATE 且 isNew = false, isPending = false: 已确认好友，显示
                    // - PRIVATE 且 isNew = false, isPending = true: 已发送请求等待接受，不显示
                    // - PRIVATE 且 isNew = true, isPending = false: 收到请求等待确认，不显示（在顶部请求区域显示）
                    val confirmedContacts = allContacts.filter { contact ->
                        val shouldShow = if (contact.type == "GROUP") {
                            Log.d(TAG, "✅ Contact ${contact.contactId} (GROUP): SHOW")
                            true // 群组直接显示
                        } else {
                            // 个人联系人：只显示已确认的好友
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

                    // 为 GROUP 类型的联系人加载 Conversation 信息
                    loadConversationsForGroups(confirmedContacts)

                    // 分类并排序
                    val (groups, privateContacts) = confirmedContacts.partition { it.isGroup() }

                    // 按显示名称的字典序排序
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
     * 为 GROUP 类型的联系人加载 Conversation 信息
     */
    private fun loadConversationsForGroups(contacts: List<Contact>) {
        viewModelScope.launch {
            try {
                // 获取所有 GROUP 类型的 conversationId
                val groupConversationIds = contacts
                    .filter { it.isGroup() && it.conversationId.isNotBlank() }
                    .map { it.conversationId }
                    .distinct()

                if (groupConversationIds.isEmpty()) {
                    Log.d(TAG, "No group conversations to load")
                    return@launch
                }

                Log.d(TAG, "Loading ${groupConversationIds.size} group conversations")

                // 批量获取 Conversation 信息
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

                // 更新缓存
                _conversationCache.value = newCache
                Log.d(TAG, "Conversation cache updated with ${newCache.size} entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations for groups", e)
            }
        }
    }

    /**
     * 获取联系人的显示名称
     * 对于 GROUP，从 Conversation 中获取真实名称
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
     * 获取联系人的头像 URL
     * 对于 GROUP，从 Conversation 中获取真实头像
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
     * 刷新联系人列表
     */
    fun refresh() {
        loadContacts()
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
            searchContacts(query)
        }
    }

    /**
     * 搜索联系人
     * 在当前用户的 contacts 中搜索，支持搜索 contactName 和 contactId
     */
    private fun searchContacts(query: String) {
        _isSearching.value = true

        val normalizedQuery = query.lowercase().trim()

        // 在所有联系人中搜索（包括 groups 和 privateContacts）
        val allContacts = _contacts.value

        val results = allContacts.filter { contact ->
            // 获取显示名称
            val displayName = getDisplayName(contact)

            // 搜索条件：contactId 或 contactName（包括备注名）
            contact.contactId.lowercase().contains(normalizedQuery) ||
            displayName.lowercase().contains(normalizedQuery) ||
            contact.contactName.lowercase().contains(normalizedQuery)
        }

        // 按相关性排序：完全匹配 > 开头匹配 > 包含匹配
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
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
