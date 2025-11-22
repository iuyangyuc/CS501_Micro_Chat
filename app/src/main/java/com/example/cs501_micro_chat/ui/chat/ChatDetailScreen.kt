/**
 * ChatDetailScreen.kt
 *
 * 对话详情界面 - 显示聊天消息和发送消息（基于 Figma 设计）
 * Chat Detail Screen - Display chat messages and send messages (Based on Figma design)
 *
 * 主要功能 / Main Functions:
 * - 显示历史消息列表（从 Firebase 加载）
 * - 实时接收新消息
 * - 发送文本消息
 * - 消息气泡展示（自己/对方）
 * - 消息已读状态
 *
 * 设计参考 / Design Reference:
 * - Figma Chat Interface Design - ChatDetail 组件
 * - 渐变蓝色顶栏 #3296FA → #66B3FF
 * - 消息气泡：自己（蓝色）/ 对方（白色）
 *
 * @author CS501 Team
 * @date 2025-01-08
 */
package com.example.cs501_micro_chat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageType
import java.text.SimpleDateFormat
import java.util.*

// Figma Design Colors
private val PrimaryBlue = Color(0xFF3296FA)
private val LightBlue = Color(0xFF66B3FF)
@Composable
private fun chatBackgroundColor() = MaterialTheme.colorScheme.background

@Composable
private fun chatSurfaceColor() = MaterialTheme.colorScheme.surface

@Composable
private fun chatInputBackground() = MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun chatPrimaryTextColor() = MaterialTheme.colorScheme.onSurface

@Composable
private fun chatSecondaryTextColor() = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 对话详情主界面
 * Chat detail main screen
 *
 * @param conversationId 会话 ID
 * @param conversationName 会话名称
 * @param conversationAvatar 会话头像 URL
 * @param onBack 返回回调
 * @param viewModel ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    conversationName: String,
    conversationAvatar: String,
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    // 初始化加载会话消息
    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val backgroundColor = chatBackgroundColor()
    val surfaceColor = chatSurfaceColor()
    val inputBackground = chatInputBackground()

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            // Figma 设计的渐变蓝色顶部栏
            Surface(
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryBlue, LightBlue)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                                tint = Color.White
                            )
                        }

                        // 会话头像和名称
                        if (conversationAvatar.isNotBlank()) {
                            AsyncImage(
                                model = conversationAvatar,
                                contentDescription = stringResource(R.string.content_description_avatar, conversationName),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversationName.firstOrNull()?.toString() ?: "?",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = conversationName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        // 更多选项
                        IconButton(onClick = { /* TODO: 显示更多菜单 */ }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.content_description_more),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // 输入栏
        Surface(
            color = surfaceColor,
            shadowElevation = 8.dp
        ) {
                Column {
                    // 附件菜单
                    if (showAttachmentMenu) {
                        AttachmentMenu(
                            onDismiss = { showAttachmentMenu = false },
                            onImageClick = { /* TODO: 选择图片 */ },
                            onCameraClick = { /* TODO: 拍照 */ },
                            onFileClick = { /* TODO: 选择文件 */ },
                            onVoiceClick = { /* TODO: 录音 */ }
                        )
                        HorizontalDivider()
                    }

                    // 输入框区域
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 语音按钮
                        IconButton(
                            onClick = { /* TODO: 语音输入 */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.content_description_voice),
                                tint = chatSecondaryTextColor()
                            )
                        }

                        // 输入框
                        Row(
                            modifier = Modifier
                                .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(inputBackground)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.chat_input_placeholder),
                                        color = chatSecondaryTextColor(),
                                        fontSize = 14.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 4
                            )

                            // 表情按钮
                            IconButton(
                                onClick = { /* TODO: 显示表情选择器 */ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = stringResource(R.string.content_description_emoji),
                                    tint = chatSecondaryTextColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // 附件/发送按钮
                        if (inputText.trim().isNotEmpty()) {
                            // 发送按钮
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(conversationId, inputText.trim())
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.content_description_send),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // 附件按钮
                            IconButton(
                                onClick = { showAttachmentMenu = !showAttachmentMenu },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = if (showAttachmentMenu) {
                                        stringResource(R.string.content_description_close)
                                    } else {
                                        stringResource(R.string.content_description_attachment)
                                    },
                                    tint = if (showAttachmentMenu) PrimaryBlue else chatSecondaryTextColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            // 错误提示
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 加载指示器
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            }
            // 空状态
            else if (messages.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = chatSecondaryTextColor(),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.chat_placeholder_no_messages),
                        color = chatSecondaryTextColor(),
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.chat_detail_empty_subtitle),
                        color = chatSecondaryTextColor(),
                        fontSize = 14.sp
                    )
                }
            }
            // 消息列表
            else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { message ->
                            // 使用消息ID，如果为空则使用时间戳+发送者ID组合
                            if (message.id.isNotBlank()) {
                                message.id
                            } else {
                                "${message.timestamp}_${message.senderId}_${message.content.hashCode()}"
                            }
                        }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isSelf = message.senderId == currentUserId
                        )
                    }
                }
            }
        }
    }
}

/**
 * 消息气泡组件（基于 Figma 设计）
 * Message bubble component (Based on Figma design)
 */
@Composable
internal fun MessageBubble(
    message: Message,
    isSelf: Boolean,
    onAvatarClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        if (!isSelf) {
            // 对方头像
            val avatarModifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { if (message.senderId.isNotBlank()) onAvatarClick(message.senderId) }

            if (message.senderAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = stringResource(R.string.content_description_avatar, message.senderName),
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = avatarModifier.background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderName.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
        ) {
            // 消息内容
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isSelf) 12.dp else 2.dp,
                    topEnd = if (isSelf) 2.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                ),
                color = if (isSelf) PrimaryBlue else chatSurfaceColor(),
                shadowElevation = if (isSelf) 0.dp else 1.dp
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            color = if (isSelf) Color.White else chatPrimaryTextColor(),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    MessageType.IMAGE -> {
                        // TODO: 显示图片消息
                        Text(
                            text = stringResource(R.string.chat_media_image_label),
                            color = if (isSelf) Color.White else chatPrimaryTextColor(),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.chat_media_unknown_label, message.type.name),
                            color = if (isSelf) Color.White else chatPrimaryTextColor(),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 时间戳
            Text(
                text = formatMessageTime(message.timestamp),
                color = chatSecondaryTextColor(),
                fontSize = 11.sp
            )
        }

        if (isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            // 自己的头像
            if (message.senderAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = stringResource(R.string.content_description_my_avatar),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 附件菜单（基于 Figma 设计）
 * Attachment menu (Based on Figma design)
 */
@Composable
private fun AttachmentMenu(
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Surface(
        color = chatInputBackground()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentMenuItem(
                icon = Icons.Default.Image,
                label = stringResource(R.string.attachment_option_photo),
                onClick = {
                    onImageClick()
                    onDismiss()
                }
            )
            AttachmentMenuItem(
                icon = Icons.Default.CameraAlt,
                label = stringResource(R.string.attachment_option_camera),
                onClick = {
                    onCameraClick()
                    onDismiss()
                }
            )
            AttachmentMenuItem(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                label = stringResource(R.string.attachment_option_file),
                onClick = {
                    onFileClick()
                    onDismiss()
                }
            )
            AttachmentMenuItem(
                icon = Icons.Default.Mic,
                label = stringResource(R.string.attachment_option_voice),
                onClick = {
                    onVoiceClick()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * 附件菜单项
 * Attachment menu item
 */
@Composable
private fun AttachmentMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = chatPrimaryTextColor(),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = chatPrimaryTextColor(),
            fontSize = 12.sp
        )
    }
}

/**
 * 格式化消息时间
 * Format message time
 */
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
