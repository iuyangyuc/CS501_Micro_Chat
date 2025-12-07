/**
 * Chat detail screen for listing messages and sending new ones (based on the Figma design).
 *
 * Features:
 * - Load historical messages from Firebase
 * - Receive new messages in real time
 * - Send text messages
 * - Message bubbles for self/others
 * - Read status display
 *
 * Design reference:
 * - Figma Chat Interface Design - ChatDetail component
 * - Gradient blue top bar #3296FA → #66B3FF
 * - Message bubbles: self (blue) / others (white)
 */
package com.example.cs501_micro_chat.ui.chat

import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.ConversationType
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageStatus
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.ui.auth.LanguageOption
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

@Composable
private fun otherMessageBubbleColor(): Color {
    val background = MaterialTheme.colorScheme.background
    // Detect effective theme by luminance so custom in-app theme toggles are respected
    val isDark = background.luminance() < 0.5f
    if (!isDark) return MaterialTheme.colorScheme.surface
    // Dark mode: match input background for consistency
    return chatInputBackground()
}

/**
 * Chat detail main screen.
 *
 * @param conversationId conversation ID
 * @param conversationName conversation name
 * @param conversationAvatar conversation avatar URL
 * @param onBack back callback
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
    // Load conversation messages on first open
    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasLoadedInitial by viewModel.hasLoadedInitial.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val conversationType by viewModel.conversationType.collectAsStateWithLifecycle()
    val otherUserAvatarUrl by viewModel.otherUserAvatarUrl.collectAsStateWithLifecycle()
    val isBlocked by viewModel.isConversationBlocked.collectAsStateWithLifecycle()
    val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
    val voiceTranscriptionStates by viewModel.voiceTranscriptionStates.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferredTranslationLanguage by viewModel.preferredTranslationLanguage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var messageAwaitingTranslation by remember { mutableStateOf<Message?>(null) }
    var selectedLanguage by remember(preferredTranslationLanguage) { mutableStateOf(preferredTranslationLanguage) }
    val languageOptions = remember { LanguageOption.entries }

    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val backgroundColor = chatBackgroundColor()
    val surfaceColor = chatSurfaceColor()
    val inputBackground = chatInputBackground()
    val inputEnabled = !isBlocked

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        textToSpeech = tts

        onDispose {
            tts.stop()
            tts.shutdown()
            textToSpeech = null
            ttsReady = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun speakMessage(text: String) {
        if (text.isBlank() || !ttsReady) return
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "chat_message_tts_${text.hashCode()}"
        )
    }

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                        // Back button
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                                tint = Color.White
                            )
                        }

                        val topAvatarUrl = if (conversationType == ConversationType.PRIVATE && otherUserAvatarUrl.isNotBlank()) {
                            otherUserAvatarUrl
                        } else {
                            conversationAvatar
                        }
                        val topAvatarSource = if (conversationType == ConversationType.PRIVATE && otherUserAvatarUrl.isNotBlank()) {
                            "otherUserAvatarUrl"
                        } else {
                            "conversationParam"
                        }

                        LaunchedEffect(conversationType, otherUserAvatarUrl, conversationAvatar, conversationName) {
                            Log.d(
                                "ChatDetailTopBar",
                                "🎨 TopBar State: " +
                                "conversationType=$conversationType, " +
                                "otherUserAvatarUrl='$otherUserAvatarUrl', " +
                                "conversationAvatar='$conversationAvatar', " +
                                "topAvatarUrl='$topAvatarUrl', " +
                                "topAvatarSource=$topAvatarSource, " +
                                "conversationName='$conversationName'"
                            )
                        }

                        // Conversation avatar and name - 使用与聊天列表相同的头像加载逻辑
                        Log.d("ChatDetailTopBar", "chatDetailTopBar: Preparing to load avatar for $conversationName from $topAvatarSource")
                        if (topAvatarUrl.isNotBlank()) {
                            Log.d("ChatDetailTopBar", "📸 Loading avatar image for $conversationName: $topAvatarUrl")
                            AsyncImage(
                                model = topAvatarUrl,
                                contentDescription = stringResource(R.string.content_description_avatar, conversationName),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                onError = { error ->
                                    Log.e("ChatDetailTopBar", "Failed to load avatar for $conversationName: ${error.result.throwable}")
                                },
                                onSuccess = {
                                    Log.d("ChatDetailTopBar", "Successfully loaded avatar for $conversationName")
                                }
                            )
                        } else {
                            Log.d("ChatDetailTopBar", "No avatarUrl for $conversationName, showing initials")
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

                        // More options
                        IconButton(onClick = { /* TODO: show more menu */ }) {
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
            // Input bar
        Surface(
            color = surfaceColor,
            shadowElevation = 8.dp
        ) {
                Column {
                    // Attachment menu
                    if (showAttachmentMenu) {
                        AttachmentMenu(
                            onDismiss = { showAttachmentMenu = false },
                            onImageClick = { /* TODO: pick image */ },
                            onCameraClick = { /* TODO: take photo */ },
                            onFileClick = { /* TODO: pick file */ },
                            onVoiceClick = { /* TODO: record voice */ }
                        )
                        HorizontalDivider()
                    }

                    // Input field area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Voice button
                        IconButton(
                            onClick = { /* TODO: voice input */ },
                            modifier = Modifier.size(40.dp),
                            enabled = inputEnabled
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.content_description_voice),
                                tint = chatSecondaryTextColor()
                            )
                        }

                        // Input box
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
                                enabled = inputEnabled,
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

                            // Emoji button
                            IconButton(
                                onClick = { /* TODO: show emoji picker */ },
                                modifier = Modifier.size(32.dp),
                                enabled = inputEnabled
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

                        // Attachment/Send button
                        if (inputText.trim().isNotEmpty()) {
                            // Send button
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
                                    .background(PrimaryBlue),
                                enabled = inputEnabled
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.content_description_send),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Attachment button
                            IconButton(
                                onClick = { showAttachmentMenu = !showAttachmentMenu },
                                modifier = Modifier.size(40.dp),
                                enabled = inputEnabled
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
            // Error message
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

            val showInitialLoading = (!hasLoadedInitial && messages.isEmpty()) || (isLoading && messages.isEmpty())
            val showEmptyState = hasLoadedInitial && !isLoading && messages.isEmpty()

            // Loading indicator (before first load or while fetching with no messages yet)
            if (showInitialLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            }
            // Empty state only after loading finished
            else if (showEmptyState) {
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
            // Message list
            else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isBlocked) {
                            RemovalBanner()
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = messages,
                                key = { message ->
                                    if (message.id.isNotBlank()) {
                                        message.id
                                    } else {
                                        "${message.timestamp}_${message.senderId}_${message.content.hashCode()}"
                                    }
                                }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    isSelf = message.senderId == currentUserId,
                                    translationState = translationStates[messageKey(message)],
                                    transcriptionState = voiceTranscriptionStates[messageKey(message)],
                                    onAvatarClick = {},
                                    onTranslateClick = { messageAwaitingTranslation = message },
                                    onClearTranslation = { viewModel.clearTranslationFor(it) },
                                    onPlayClick = { speakMessage(message.content) },
                                    onTranscribeClick = { viewModel.transcribeVoiceMessage(it) },
                                    onClearTranscription = { viewModel.clearTranscriptionFor(it) }
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }

    messageAwaitingTranslation?.let { pendingMessage ->
        AlertDialog(
            onDismissRequest = { messageAwaitingTranslation = null },
            title = { Text(text = stringResource(R.string.translate_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.translate_dialog_subtitle),
                        color = chatSecondaryTextColor(),
                        fontSize = 14.sp
                    )
                    TranslationLanguageChooser(
                        options = languageOptions,
                        selected = selectedLanguage,
                        onSelect = { selectedLanguage = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.translateMessage(pendingMessage, selectedLanguage.displayName)
                        messageAwaitingTranslation = null
                    }
                ) {
                    Text(text = stringResource(R.string.translate_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { messageAwaitingTranslation = null }) {
                    Text(text = stringResource(R.string.translate_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun RemovalBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFFFFE4E6),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_removed_notice),
            color = Color(0xFFB91C1C),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 14.sp
        )
    }
}

/**
 * Message bubble component (Based on Figma design)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    message: Message,
    isSelf: Boolean,
    timeLabel: String? = null,
    translationState: TranslationResultState? = null,
    transcriptionState: VoiceTranscriptionState? = null,
    onAvatarClick: (String) -> Unit = {},
    onTranslateClick: (Message) -> Unit = {},
    onClearTranslation: (Message) -> Unit = {},
    onPlayClick: (Message) -> Unit = {},
    onTranscribeClick: (Message) -> Unit = {},
    onClearTranscription: (Message) -> Unit = {},
    showAvatarForSelf: Boolean = false
) {
    val isTextMessage = message.type == MessageType.TEXT
    val isVoiceMessage = message.type == MessageType.VOICE
    var showActionMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        if (!isSelf || (isSelf && showAvatarForSelf)) {
            // Other user's avatar
            val avatarModifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { if (message.senderId.isNotBlank()) onAvatarClick(message.senderId) }

            if (message.senderAvatarUrl.isNotBlank()) {
                Log.d("MessageBubble", "Loading avatar for ${message.senderName}: ${message.senderAvatarUrl}")
                AsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = stringResource(R.string.content_description_avatar, message.senderName),
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        Log.e("MessageBubble", "Failed to load avatar for ${message.senderName}: ${error.result.throwable}")
                    },
                    onSuccess = {
                        Log.d("MessageBubble", "Successfully loaded avatar for ${message.senderName}")
                    }
                )
            } else {
                Log.d("MessageBubble", "No avatarUrl for ${message.senderName}, showing initials")
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
            Box {
                Surface(
                    modifier = if (isTextMessage || isVoiceMessage) {
                        Modifier.combinedClickable(
                            onClick = { showActionMenu = true },
                            onDoubleClick = { showActionMenu = true }
                        )
                    } else {
                        Modifier
                    },
                    shape = RoundedCornerShape(
                        topStart = if (isSelf) 12.dp else 2.dp,
                        topEnd = if (isSelf) 2.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    color = if (isSelf) PrimaryBlue else otherMessageBubbleColor(),
                    shadowElevation = if (isSelf) 0.dp else 1.dp
                ) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            val baseTextColor = if (isSelf) Color.White else chatPrimaryTextColor()
                            val secondaryTextColor = if (isSelf) {
                                Color.White.copy(alpha = 0.85f)
                            } else {
                                chatSecondaryTextColor()
                            }

                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    color = baseTextColor,
                                    fontSize = 15.sp
                                )

                                translationState?.let { state ->
                                    when {
                                        state.isLoading -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = if (isSelf) Color.White else PrimaryBlue
                                                )
                                                Text(
                                                    text = stringResource(R.string.translate_status_loading),
                                                    color = secondaryTextColor,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        state.errorMessage != null -> {
                                            Text(
                                                text = stringResource(R.string.translate_status_failed),
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 13.sp
                                            )
                                        }

                                        state.translatedText != null -> {
                                            val label = state.targetLanguage?.let { target ->
                                                stringResource(
                                                    R.string.translate_result_label,
                                                    target
                                                )
                                            } ?: stringResource(R.string.translate_result_fallback_label)

                                            Text(
                                                text = "$label: ${state.translatedText}",
                                                color = secondaryTextColor,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        MessageType.IMAGE -> {
                            if (message.mediaUrl.isNotBlank()) {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = stringResource(R.string.chat_media_image_label),
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .heightIn(max = 240.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.chat_media_image_label),
                                    color = if (isSelf) Color.White else chatPrimaryTextColor(),
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                        MessageType.VOICE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                VoiceMessageBubble(
                                    mediaUrl = message.mediaUrl,
                                    content = message.content,
                                    isSelf = isSelf,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                )

                                transcriptionState?.let { state ->
                                    when {
                                        state.isLoading -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = if (isSelf) Color.White else PrimaryBlue
                                                )
                                                Text(
                                                    text = stringResource(R.string.transcribe_status_loading),
                                                    color = if (isSelf) Color.White else chatSecondaryTextColor(),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        state.errorMessage != null -> {
                                            Text(
                                                text = stringResource(R.string.transcribe_status_failed),
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }

                                        state.text != null -> {
                                            val baseColor = if (isSelf) Color.White else chatSecondaryTextColor()
                                            val label = stringResource(R.string.transcribe_result_label)
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$label ${state.text}",
                                                    color = baseColor,
                                                    fontSize = 13.sp
                                                )

                                                when {
                                                    state.isTranslating -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(12.dp),
                                                                strokeWidth = 2.dp,
                                                                color = if (isSelf) Color.White else PrimaryBlue
                                                            )
                                                            Text(
                                                                text = stringResource(R.string.translate_status_loading),
                                                                color = baseColor,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }

                                                    state.translationError != null -> {
                                                        Text(
                                                            text = stringResource(R.string.translate_status_failed),
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    state.translatedText != null -> {
                                                        val translatedLabel = state.translatedLanguage?.let { lang ->
                                                            stringResource(R.string.translate_result_label, lang)
                                                        } ?: stringResource(R.string.translate_result_fallback_label)
                                                        Text(
                                                            text = "$translatedLabel: ${state.translatedText}",
                                                            color = baseColor,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        MessageType.VIDEO -> {
                            VideoMessageBubble(mediaUrl = message.mediaUrl)
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

                if (isTextMessage || isVoiceMessage) {
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        if (isTextMessage) {
                            val hasTranslation = translationState != null
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (hasTranslation) {
                                            stringResource(R.string.translate_menu_hide)
                                        } else {
                                            stringResource(R.string.translate_menu_action)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showActionMenu = false
                                    if (hasTranslation) {
                                        onClearTranslation(message)
                                    } else {
                                        onTranslateClick(message)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.translate_menu_play)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showActionMenu = false
                                    onPlayClick(message)
                                }
                            )
                        }

                        if (isVoiceMessage) {
                            val hasTranscription = transcriptionState != null
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (hasTranscription) {
                                            stringResource(R.string.transcribe_menu_hide)
                                        } else {
                                            stringResource(R.string.transcribe_menu_action)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showActionMenu = false
                                    if (hasTranscription) {
                                        onClearTranscription(message)
                                    } else {
                                        onTranscribeClick(message)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (isSelf && message.status == MessageStatus.FAILED) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.chat_message_failed),
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = stringResource(R.string.chat_message_failed_removed),
                    color = Color(0xFFDC2626),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = timeLabel ?: formatMessageTime(message.timestamp),
                color = chatSecondaryTextColor(),
                fontSize = 11.sp
            )
        }

        if (isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            // Own avatar
            if (message.senderAvatarUrl.isNotBlank()) {
                Log.d("MessageBubble", "Loading self avatar: ${message.senderAvatarUrl}")
                AsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = stringResource(R.string.content_description_my_avatar),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        Log.e("MessageBubble", "Failed to load self avatar: ${error.result.throwable}")
                    },
                    onSuccess = {
                        Log.d("MessageBubble", "Successfully loaded self avatar")
                    }
                )
            } else {
                Log.d("MessageBubble", "No self avatarUrl, showing initials")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationLanguageChooser(
    options: List<LanguageOption>,
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Column {
                        Text(
                            text = option.displayName,
                            fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Text(
                            text = option.languageTag.uppercase(Locale.ROOT),
                            fontSize = 12.sp,
                            color = chatSecondaryTextColor()
                        )
                    }
                },
                leadingIcon = if (option == selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun VoiceMessageBubble(
    mediaUrl: String,
    content: String,
    isSelf: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    var isPlaying by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(mediaUrl) {
        onDispose {
            player?.release()
            player = null
            isPlaying = false
        }
    }

    fun stopPlayback() {
        player?.apply {
            try {
                stop()
            } catch (_: Exception) {
            }
            release()
        }
        player = null
        isPlaying = false
    }

    fun startPlayback() {
        if (mediaUrl.isBlank()) return
        stopPlayback()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            mediaPlayer.setDataSource(mediaUrl)
            mediaPlayer.setOnPreparedListener {
                isPlaying = true
                it.start()
            }
            mediaPlayer.setOnCompletionListener {
                stopPlayback()
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                stopPlayback()
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    Row(
        modifier = Modifier.padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val textColor = if (isSelf) Color.White else chatPrimaryTextColor()
        val secondaryColor = if (isSelf) Color.White.copy(alpha = 0.85f) else chatSecondaryTextColor()
        val iconColor = if (isSelf) Color.White else chatPrimaryTextColor()

        IconButton(
            onClick = {
                if (isPlaying) {
                    stopPlayback()
                } else {
                    startPlayback()
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.content_description_voice_stop)
                } else {
                    stringResource(R.string.content_description_voice_play)
                },
                tint = iconColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            val durationLabel = formatVoiceLabel(content)
            Text(
                text = stringResource(R.string.voice_message_label),
                color = textColor,
                fontSize = 15.sp
            )
            Text(
                text = durationLabel,
                color = secondaryColor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun formatVoiceLabel(raw: String): String {
    val seconds = extractVoiceSecondsFromContent(raw)

    return seconds?.let { secs ->
        stringResource(R.string.last_message_voice, secs)
    } ?: stringResource(R.string.last_message_voice_no_duration)
}

private fun extractVoiceSecondsFromContent(raw: String): String? {
    // Match VOICE_12s token
    raw.removePrefix("VOICE_")
        .removePrefix("voice_")
        .removeSuffix("s")
        .trim()
        .takeIf { it.all { ch -> ch.isDigit() } }
        ?.let { return it }

    // Match any "{number}s" pattern
    val regex = Regex("(\\d+)\\s*s", RegexOption.IGNORE_CASE)
    regex.find(raw)?.groupValues?.getOrNull(1)?.let { return it }

    // Fallback: extract first digit run anywhere
    val digits = raw.filter { it.isDigit() }
    return digits.takeIf { it.isNotBlank() }
}

@Composable
private fun VideoMessageBubble(
    mediaUrl: String
) {
    if (mediaUrl.isBlank()) {
        Text(
            text = stringResource(R.string.video_message_label),
            color = chatPrimaryTextColor(),
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        return
    }

    val context = LocalContext.current
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(mediaUrl) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(mediaUrl))
                setOnPreparedListener { it.isLooping = false }
                videoView = this
            }
        },
        update = { view ->
            if (view.tag != mediaUrl) {
                view.stopPlayback()
                view.setVideoURI(Uri.parse(mediaUrl))
                view.tag = mediaUrl
            }
        },
        modifier = Modifier
            .widthIn(max = 260.dp)
            .heightIn(min = 160.dp, max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

/**
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

/** Attachment menu item */
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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

/** Format message time */
private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
