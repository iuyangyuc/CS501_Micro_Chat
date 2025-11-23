/**
 * AvatarImage.kt
 *
 * 头像加载组件 - 使用 Coil 加载头像图片或显示首字母
 * Avatar Image Component - Loads avatar using Coil or shows initial letter
 *
 * 功能 / Features:
 * - 使用 avatarUrl 加载头像图片
 * - 如果没有 URL，显示用户名首字母
 * - 支持圆形和方形显示
 * - 使用 Coil 进行图片加载和缓存
 *
 * @author CS501 Team
 * @date 2025-01-22
 */
package com.example.cs501_micro_chat.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 头像加载组件
 * Loads avatar image or shows user initial letter
 */
@Composable
fun AvatarImage(
    avatarUrl: String,
    userName: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null
) {
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


