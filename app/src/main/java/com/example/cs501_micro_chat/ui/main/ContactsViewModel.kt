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

    // 缓存 Conversation 数据，用于显示 GROUP 的真实信息
    private val _conversationCache = MutableStateFlow<Map<String, Conversation>>(emptyMap())
    val conversationCache: StateFlow<Map<String, Conversation>> = _conversationCache.asStateFlow()

    private val TAG = "ContactsViewModel"

    init {
        loadContacts()
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
                    _contacts.value = allContacts
                    _isLoading.value = false

                    // 为 GROUP 类型的联系人加载 Conversation 信息
                    loadConversationsForGroups(allContacts)

                    // 分类并排序
                    val (groups, privateContacts) = allContacts.partition { it.isGroup() }

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
}
