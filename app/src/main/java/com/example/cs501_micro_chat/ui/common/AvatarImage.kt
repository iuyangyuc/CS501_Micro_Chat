/**
 * AvatarImage.kt
 *
 * 头像加载组件 - 优先从 Firebase Storage 加载，否则使用自动生成头像
 * Avatar Image Component - Loads from Firebase Storage first, otherwise uses auto-generated avatar
 *
 * 功能 / Features:
 * - 优先从 Firebase Storage/Avatars 文件夹加载头像
 * - 如果 Storage 中没有头像，使用 avatarUrl 生成的自动头像
 * - 支持圆形和方形显示
 *
 * @author CS501 Team
 * @date 2025-11-22
 */
package com.example.cs501_micro_chat.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 头像加载组件
 * Loads avatar with priority: Firebase Storage > Auto-generated avatar
 */
@Composable
fun AvatarImage(
    userId: String,
    fallbackAvatarUrl: String,
    userName: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    viewModel: AvatarViewModel = hiltViewModel()
) {
    val avatarUrl by viewModel.getAvatarUrl(userId, fallbackAvatarUrl).collectAsState()

    Box(modifier = modifier) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription ?: "$userName avatar",
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback: Show first letter of name
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color(0xFF3296FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * ViewModel to handle avatar loading logic
 */
@HiltViewModel
class AvatarViewModel @Inject constructor(
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val avatarCache = mutableMapOf<String, MutableStateFlow<String>>()

    /**
     * 获取头像 URL，优先从 Firebase Storage 加载
     * Get avatar URL, prioritize Firebase Storage
     */
    fun getAvatarUrl(userId: String, fallbackUrl: String): StateFlow<String> {
        // Return cached flow if exists
        if (avatarCache.containsKey(userId)) {
            return avatarCache[userId]!!.asStateFlow()
        }

        // Create new flow for this user
        val flow = MutableStateFlow(fallbackUrl)
        avatarCache[userId] = flow

        // Load from Firebase Storage
        viewModelScope.launch {
            storageRepository.getAvatarUrl(userId).fold(
                onSuccess = { storageUrl ->
                    // Use storage URL if available, otherwise keep fallback
                    flow.value = storageUrl ?: fallbackUrl
                },
                onFailure = {
                    // Keep fallback URL on error
                    flow.value = fallbackUrl
                }
            )
        }

        return flow.asStateFlow()
    }

    /**
     * 清除缓存（用户上传新头像后调用）
     * Clear cache (call after user uploads new avatar)
     */
    fun clearCache(userId: String) {
        avatarCache.remove(userId)
    }

    /**
     * 清除所有缓存
     * Clear all cache
     */
    fun clearAllCache() {
        avatarCache.clear()
    }
}

