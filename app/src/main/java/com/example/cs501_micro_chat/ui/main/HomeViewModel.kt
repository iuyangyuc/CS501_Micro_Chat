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
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val TAG = "HomeViewModel"

    init {
        loadConversations()
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
                    // 数据已经在 FirebaseDataSource 中按时间排序了
                    _conversations.value = conversations
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                _error.value = "加载会话失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 格式化时间显示
     */
    fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val today = Calendar.getInstance()

        return when {
            // 今天 - 显示时间
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            // 昨天
            diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
            // 一周内 - 显示星期
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            }
            // 更早 - 显示日期
            else -> {
                SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
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
     * 清除错误消息
     */
    fun clearError() {
        _error.value = null
    }
}

