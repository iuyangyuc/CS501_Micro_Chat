/**
 * HomeScreen.kt
 *
 * 主界面 - 登录后的主要交互界面（基于 Figma 设计）
 * Home Screen - Main interaction interface after login (Based on Figma design)
 *
 * 主要功能 / Main Functions:
 * - 底部导航栏（聊天、联系人、我的）/ Bottom navigation (Chat, Contacts, Me)
 * - 顶部渐变蓝色 AppBar / Top gradient blue AppBar
 * - 聊天列表页面 / Chat list page
 * - 联系人页面 / Contacts page
 * - 个人设置页面 / Profile settings page
 *
 * 设计参考 / Design Reference:
 * - Figma Chat Interface Design
 * - 颜色：#3296FA → #66B3FF (渐变蓝)
 * - 响应式设计，适配不同屏幕尺寸
 *
 * @author CS501 Team
 * @date 2025-11-04
 */
package com.example.cs501_micro_chat.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.ui.settings.AboutScreen
import com.example.cs501_micro_chat.ui.settings.PrivacySettingsScreen
import com.example.cs501_micro_chat.ui.settings.ProfileEditScreen
import com.example.cs501_micro_chat.ui.settings.SettingsScreen
import com.example.cs501_micro_chat.ui.theme.ThemeOption
import com.example.cs501_micro_chat.ui.theme.ThemeViewModel

// Figma Design Colors (still used for branding)
private val PrimaryBlue = Color(0xFF3296FA)
private val LightBlue = Color(0xFF66B3FF)
private val UnreadBadgeRed = Color(0xFFEF4444)

// Helper functions for text colors (supports Dark Mode)
@Composable
private fun primaryTextColor() = MaterialTheme.colorScheme.onSurface

@Composable
private fun secondaryTextColor() = MaterialTheme.colorScheme.onSurfaceVariant
private const val LANGUAGE_SETTINGS_ROUTE = "language_settings"
private const val PRIVACY_SETTINGS_ROUTE = "privacy_settings"
private const val ABOUT_ROUTE = "about"
private const val PROFILE_EDIT_ROUTE = "profile_edit"
private const val PROFILE_UPDATED_KEY = "profile_updated"


/**
 * 主界面底部导航项（基于 Figma 设计）
 * Bottom navigation items for home screen (Based on Figma design)
 */
sealed class HomeDestination(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    data object Chats : HomeDestination(
        route = "chats",
        icon = Icons.AutoMirrored.Filled.Chat,
        labelResId = R.string.nav_chats
    )

    data object Contacts : HomeDestination(
        route = "contacts",
        icon = Icons.Filled.People,
        labelResId = R.string.nav_contacts
    )

    data object Me : HomeDestination(
        route = "me",
        icon = Icons.Filled.Person,
        labelResId = R.string.nav_me
    )
}

// 在外部定义 items 列表，避免类初始化问题
private val homeDestinationItems: List<HomeDestination>
    get() = listOf(
        HomeDestination.Chats,
        HomeDestination.Contacts,
        HomeDestination.Me
    )

/**
 * 主界面组合项（基于 Figma 设计）
 * Home screen composable (Based on Figma design)
 *
 * @param onLogout 登出回调 / Logout callback
 * @param modifier 修饰符 / Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = homeDestinationItems
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isFullScreenRoute = currentRoute?.let {
        it.startsWith("chat_detail") || it == LANGUAGE_SETTINGS_ROUTE || it == PRIVACY_SETTINGS_ROUTE || it == ABOUT_ROUTE || it == PROFILE_EDIT_ROUTE
    } == true
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    // ========== 固定布局：顶栏 + 内容 + 底栏 ==========
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // ========== 固定顶栏（始终存在，只改变内容） ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlue, LightBlue)
                    )
                )
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            // 顶栏内容平滑切换
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "TopBarContent"
            ) { route ->
                when {
                    route?.startsWith("chat_detail") == true -> {
                        ChatDetailTopBar(
                            conversationName = navBackStackEntry?.arguments?.getString("conversationName")?.let {
                                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                            } ?: "",
                            conversationAvatar = navBackStackEntry?.arguments?.getString("conversationAvatar")?.let {
                                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                            } ?: "",
                            onBack = { navController.popBackStack() }
                        )
                    }

                    route == LANGUAGE_SETTINGS_ROUTE -> {
                        LanguageSettingsTopBar(onBack = { navController.popBackStack() })
                    }

                    route == PRIVACY_SETTINGS_ROUTE -> {
                        PrivacySettingsTopBar(onBack = { navController.popBackStack() })
                    }

                    route == ABOUT_ROUTE -> {
                        AboutTopBar(onBack = { navController.popBackStack() })
                    }

                    route == PROFILE_EDIT_ROUTE -> {
                        ProfileHeaderTopBar()
                    }

                    else -> {
                        HomeTopBar(
                            currentRoute = route
                        )
                    }
                }
            }
        }

        // ========== 内容区域（可动画） ==========
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(backgroundColor)
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeDestination.Chats.route
            ) {
                composable(HomeDestination.Chats.route) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    ChatListScreen(
                        viewModel = homeViewModel,
                        onChatClick = { conversation ->
                            val displayName = homeViewModel.getDisplayName(conversation)
                            val avatarUrl = homeViewModel.getAvatarUrl(conversation)
                            val encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString())
                            val encodedAvatar = URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                "chat_detail/${conversation.id}/$encodedName/$encodedAvatar"
                            )
                        }
                    )
                }

                composable(HomeDestination.Contacts.route) {
                    val contactsViewModel: ContactsViewModel = hiltViewModel()
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        onContactClick = { contact ->
                            // 获取 conversationId
                            val conversationId = contact.conversationId

                            if (conversationId.isNotBlank()) {
                                // 获取显示名称和头像
                                val displayName = contactsViewModel.getDisplayName(contact)
                                val avatarUrl = contactsViewModel.getAvatarUrl(contact)

                                // URL 编码
                                val encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString())
                                val encodedAvatar = URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8.toString())

                                // 导航到对话详情页面
                                navController.navigate(
                                    "chat_detail/$conversationId/$encodedName/$encodedAvatar"
                                )
                            }
                        }
                    )
                }

                composable(HomeDestination.Me.route) { backStackEntry ->
                    val themeViewModel: ThemeViewModel = hiltViewModel()
                    val themeOption by themeViewModel.themeOption.collectAsStateWithLifecycle()
                    val profileViewModel: ProfileSettingsViewModel = hiltViewModel()
                    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                    val savedStateHandle = backStackEntry.savedStateHandle
                    val profileUpdatedFlow = remember(savedStateHandle) {
                        savedStateHandle.getStateFlow(PROFILE_UPDATED_KEY, false)
                    }
                    val profileUpdated by profileUpdatedFlow.collectAsStateWithLifecycle()

                    LaunchedEffect(profileUpdated) {
                        if (profileUpdated) {
                            profileViewModel.refreshProfile()
                            savedStateHandle[PROFILE_UPDATED_KEY] = false
                        }
                    }

                    ProfileScreen(
                        onLogout = onLogout,
                        onLanguageClick = { navController.navigate(LANGUAGE_SETTINGS_ROUTE) },
                        onPrivacyClick = { navController.navigate(PRIVACY_SETTINGS_ROUTE) },
                        onAboutClick = { navController.navigate(ABOUT_ROUTE) },
                        onProfileClick = { navController.navigate(PROFILE_EDIT_ROUTE) },
                        themeOption = themeOption,
                        onThemeSelected = themeViewModel::selectTheme,
                        profileState = profileState
                    )
                }
                composable(LANGUAGE_SETTINGS_ROUTE) {
                    SettingsScreen()
                }
                composable(PRIVACY_SETTINGS_ROUTE) {
                    PrivacySettingsScreen()
                }
                composable(ABOUT_ROUTE) {
                    AboutScreen()
                }
                composable(PROFILE_EDIT_ROUTE) {
                    ProfileEditScreen(
                        onBack = { navController.popBackStack() },
                        onProfileSaved = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(PROFILE_UPDATED_KEY, true)
                            navController.popBackStack()
                        }
                    )
                }

                // ChatDetail 页面 - 从右侧滑入
                composable(
                    route = "chat_detail/{conversationId}/{conversationName}/{conversationAvatar}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("conversationName") { type = NavType.StringType },
                        navArgument("conversationAvatar") { type = NavType.StringType }
                    ),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    ChatDetailContent(conversationId = conversationId)
                }
            }
        }

        // ========== 固定底栏（非 ChatDetail 时显示） ==========
        if (!isFullScreenRoute) {
            NavigationBar(
                containerColor = surfaceColor,
                tonalElevation = 8.dp
            ) {
                items.forEach { destination ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelResId),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelResId),
                                fontSize = 12.sp
                            )
                        },
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = secondaryTextColor(),
                            unselectedTextColor = secondaryTextColor(),
                            indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * HomeScreen 的顶栏内容
 */
@Composable
fun HomeTopBar(
    currentRoute: String?
) {
    // 控制下拉菜单的显示状态
    var showAddMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(48.dp))

        Text(
            text = when (currentRoute) {
                HomeDestination.Chats.route -> stringResource(R.string.nav_chats)
                HomeDestination.Contacts.route -> stringResource(R.string.nav_contacts)
                HomeDestination.Me.route -> stringResource(R.string.nav_me)
                else -> ""
            },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Chats 和 Contacts 页面都显示加号按钮和下拉菜单
        if (currentRoute == HomeDestination.Chats.route || currentRoute == HomeDestination.Contacts.route) {
            Box {
                IconButton(onClick = { showAddMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }

                // 下拉菜单
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    // 新增联系人选项
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = "Add Contact",
                                    tint = primaryTextColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "New Contact",
                                    color = primaryTextColor(),
                                    fontSize = 15.sp
                                )
                            }
                        },
                        onClick = {
                            showAddMenu = false
                            // TODO: 实现新增联系人功能
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // 新增群组选项
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = "Create Group",
                                    tint = primaryTextColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "New Group",
                                    color = primaryTextColor(),
                                    fontSize = 15.sp
                                )
                            }
                        },
                        onClick = {
                            showAddMenu = false
                            // TODO: 实现新增群组功能
                        }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun ProfileHeaderTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.profile_header_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LanguageSettingsTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.language_settings_title),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PrivacySettingsTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.settings_privacy),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AboutTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.settings_about),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * ChatDetail 的顶栏内容
 */
@Composable
fun ChatDetailTopBar(
    conversationName: String,
    conversationAvatar: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

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
                    .background(Color.White.copy(alpha = 0.3f)),
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

        IconButton(onClick = { /* TODO */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.content_description_more),
                tint = Color.White
            )
        }
    }
}

/**
 * ChatDetail 的内容区域（不包含顶栏）
 */
@Composable
fun ChatDetailContent(conversationId: String) {
    val viewModel: com.example.cs501_micro_chat.ui.chat.ChatDetailViewModel = hiltViewModel()

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColorChat = MaterialTheme.colorScheme.surface
    val searchBackgroundChat = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 消息列表
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.chat_placeholder_no_messages),
                        color = secondaryTextColor(),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { message ->
                            if (message.id.isNotBlank()) message.id
                            else "${message.timestamp}_${message.senderId}_${message.content.hashCode()}"
                        }
                    ) { message ->
                        com.example.cs501_micro_chat.ui.chat.MessageBubble(
                            message = message,
                            isSelf = message.senderId == currentUserId
                        )
                    }
                }
            }
        }

        // 输入栏
        Surface(
            color = surfaceColorChat,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.content_description_voice),
                        tint = secondaryTextColor()
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(searchBackgroundChat)
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
                                color = secondaryTextColor(),
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
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (inputText.trim().isNotEmpty()) {
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
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.content_description_send),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.content_description_attachment),
                            tint = secondaryTextColor()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 聊天列表页面（基于 Figma 设计）- 从 Firebase 读取真实数据
 * Chat list screen (Based on Figma design) - Reads real data from Firebase
 */
@Composable
fun ChatListScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onChatClick: (Conversation) -> Unit = {}
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 搜索相关状态
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    // 控制搜索框是否激活
    var isSearchActive by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 搜索栏
            Surface(
                color = surfaceColor,
                shadowElevation = 2.dp
            ) {
                Column {
                    if (isSearchActive) {
                        // 激活状态：可编辑的搜索框
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(searchBarColor)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = "Search",
                                        color = secondaryTextColor(),
                                        fontSize = 15.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = primaryTextColor(),
                                    unfocusedTextColor = primaryTextColor(),
                                ),
                                singleLine = true
                            )
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.clearSearch() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.clearSearch()
                                    isSearchActive = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = PrimaryBlue,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // 默认状态：占位符搜索框
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(searchBarColor)
                                .clickable { isSearchActive = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search",
                                color = secondaryTextColor(),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 显示搜索结果或对话列表
            if (isSearchActive && searchQuery.isNotBlank()) {
                // 搜索结果
                ConversationSearchResultsList(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    viewModel = viewModel,
                    onChatClick = { conversation ->
                        viewModel.clearSearch()
                        isSearchActive = false
                        onChatClick(conversation)
                    }
                )
            } else {
                // 正常对话列表
                ConversationsList(
                    conversations = conversations,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    onChatClick = onChatClick
                )
            }
        }
    }
}

/**
 * 会话列表项（基于 Figma 设计）- 显示真实会话数据
 * Conversation list item (Based on Figma design) - Displays real conversation data
 */
@Composable
fun ConversationListItem(
    conversation: Conversation,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    // 监听 userCache 的变化，确保用户信息加载后界面会更新
    val userCache by viewModel.userCache.collectAsStateWithLifecycle()

    val formattedTime = viewModel.formatTime(conversation.lastMessageTime)

    // 使用新的方法获取显示名称和头像
    val displayName = viewModel.getDisplayName(conversation)
    val avatarUrl = viewModel.getAvatarUrl(conversation)

    // 调试日志
    Log.d("ConversationListItem", "Conversation ID: ${conversation.id}")
    Log.d("ConversationListItem", "Participants: ${conversation.participants}")
    Log.d("ConversationListItem", "Display Name: $displayName")
    Log.d("ConversationListItem", "Avatar URL: $avatarUrl")
    Log.d("ConversationListItem", "UserCache size: ${userCache.size}")

    val rowBackground = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(rowBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像 - 使用 Coil 加载网络图片
        if (avatarUrl.isNotBlank()) {
            Log.d("ConversationListItem", "Loading image for $displayName: $avatarUrl")
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.content_description_avatar, displayName),
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    Log.e("ConversationListItem", "Failed to load image for $displayName: ${error.result.throwable}")
                },
                onSuccess = {
                    Log.d("ConversationListItem", "Successfully loaded image for $displayName")
                }
            )
        } else {
            // 没有头像URL时显示首字母
            Log.d("ConversationListItem", "No avatarUrl for $displayName, showing initials")
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 会话信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    color = primaryTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedTime,
                    color = secondaryTextColor(),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    color = secondaryTextColor(),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

            }
        }
    }
}

/**
 * 对话搜索结果列表
 * Conversation search results list
 */
@Composable
fun ConversationSearchResultsList(
    searchResults: List<Conversation>,
    isSearching: Boolean,
    viewModel: HomeViewModel,
    onChatClick: (Conversation) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    if (isSearching) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else if (searchResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No results found",
                    color = primaryTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Try searching with a different keyword",
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${searchResults.size} result${if (searchResults.size > 1) "s" else ""} found",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = secondaryTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            items(
                items = searchResults,
                key = { it.id }
            ) { conversation ->
                ConversationListItem(
                    conversation = conversation,
                    viewModel = viewModel,
                    onClick = { onChatClick(conversation) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }
    }
}

/**
 * 正常对话列表
 * Normal conversations list
 */
@Composable
fun ConversationsList(
    conversations: List<Conversation>,
    isLoading: Boolean,
    viewModel: HomeViewModel,
    onChatClick: (Conversation) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    // 加载指示器
    if (isLoading && conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
    // 空状态
    else if (conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No Conversations Yet",
                    color = secondaryTextColor(),
                    fontSize = 16.sp
                )
                Text(
                    text = "Start a new chat to connect with others!",
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    }
    // 会话列表
    else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = conversations,
                key = { it.id }
            ) { conversation ->
                ConversationListItem(
                    conversation = conversation,
                    viewModel = viewModel,
                    onClick = { onChatClick(conversation) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }
    }
}

/**
 * 联系人页面（基于 Figma 设计）- 显示分组和联系人列表
 * Contacts screen (Based on Figma design) - Shows groups and contacts list
 */
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onContactClick: (Contact) -> Unit = {}
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val privateContacts by viewModel.privateContacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 搜索相关状态
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant

    // 控制搜索框是否激活
    var isSearchActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 搜索栏
            Surface(
                color = surfaceColor,
                shadowElevation = 2.dp
            ) {
                Column {
                    if (isSearchActive) {
                        // 激活状态：可编辑的搜索框
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(searchBarColor)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = "Search contacts",
                                        color = secondaryTextColor(),
                                        fontSize = 15.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = primaryTextColor(),
                                    unfocusedTextColor = primaryTextColor(),
                                ),
                                singleLine = true
                            )
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.clearSearch() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.clearSearch()
                                    isSearchActive = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = PrimaryBlue,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // 默认状态：占位符搜索框
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(searchBarColor)
                                .clickable { isSearchActive = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search contacts",
                                color = secondaryTextColor(),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 显示搜索结果或联系人列表
            if (isSearchActive && searchQuery.isNotBlank()) {
                // 搜索结果
                SearchResultsList(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    viewModel = viewModel,
                    onContactClick = { contact ->
                        viewModel.clearSearch()
                        isSearchActive = false
                        onContactClick(contact)
                    }
                )
            } else {
                // 正常联系人列表
                ContactsList(
                    groups = groups,
                    privateContacts = privateContacts,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    onContactClick = onContactClick
                )
            }
        }
    }
}

/**
 * 搜索结果列表
 * Search results list
 */
@Composable
fun SearchResultsList(
    searchResults: List<Contact>,
    isSearching: Boolean,
    viewModel: ContactsViewModel,
    onContactClick: (Contact) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    if (isSearching) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else if (searchResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No results found",
                    color = primaryTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Try searching with a different keyword",
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${searchResults.size} result${if (searchResults.size > 1) "s" else ""} found",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = secondaryTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            items(
                items = searchResults,
                key = { it.contactId }
            ) { contact ->
                ContactListItem(
                    contact = contact,
                    viewModel = viewModel,
                    onClick = { onContactClick(contact) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
            }
        }
    }
}

/**
 * 正常联系人列表
 * Normal contacts list
 */
@Composable
fun ContactsList(
    groups: List<Contact>,
    privateContacts: List<Contact>,
    isLoading: Boolean,
    viewModel: ContactsViewModel,
    onContactClick: (Contact) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    // 加载指示器
    if (isLoading && groups.isEmpty() && privateContacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
    // 空状态
    else if (groups.isEmpty() && privateContacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = secondaryTextColor()
                )
                Text(
                    text = "No Contacts Yet",
                    color = secondaryTextColor(),
                    fontSize = 16.sp
                )
                Text(
                    text = "Add friends to start chatting!",
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    }
    // 联系人列表
    else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Groups 分组
            if (groups.isNotEmpty()) {
                item {
                    SectionHeader(title = "GROUPS (${groups.size})")
                }
                items(
                    items = groups,
                    key = { it.contactId }
                ) { contact ->
                    ContactListItem(
                        contact = contact,
                        viewModel = viewModel,
                        onClick = { onContactClick(contact) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                }
            }

            // Contacts 分组
            if (privateContacts.isNotEmpty()) {
                item {
                    SectionHeader(title = "CONTACTS (${privateContacts.size})")
                }
                items(
                    items = privateContacts,
                    key = { it.contactId }
                ) { contact ->
                    ContactListItem(
                        contact = contact,
                        viewModel = viewModel,
                        onClick = { onContactClick(contact) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                }
            }
        }
    }
}

/**
 * 分组标题
 * Section header for contacts list
 */
@Composable
fun SectionHeader(title: String) {
    val sectionBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = sectionBackgroundColor
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = secondaryTextColor(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 联系人列表项
 * Contact list item
 */
@Composable
fun ContactListItem(
    contact: Contact,
    viewModel: ContactsViewModel,
    onClick: () -> Unit
) {
    // 监听 conversationCache 的变化，确保 GROUP 信息加载后界面会更新
    val conversationCache by viewModel.conversationCache.collectAsStateWithLifecycle()

    // 使用 ViewModel 的方法获取显示名称和头像（对 GROUP 会从 Conversation 中获取）
    val displayName = viewModel.getDisplayName(contact)
    val avatarUrl = viewModel.getAvatarUrl(contact)

    // 调试日志
    Log.d("ContactListItem", "Contact ID: ${contact.contactId}")
    Log.d("ContactListItem", "Contact Type: ${contact.type}")
    Log.d("ContactListItem", "Conversation ID: ${contact.conversationId}")
    Log.d("ContactListItem", "Display Name: $displayName")
    Log.d("ContactListItem", "Avatar URL: $avatarUrl")
    Log.d("ContactListItem", "ConversationCache size: ${conversationCache.size}")

    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (avatarUrl.isNotBlank()) {
            Log.d("ContactListItem", "Loading image for $displayName: $avatarUrl")
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$displayName avatar",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    Log.e("ContactListItem", "Failed to load image for $displayName: ${error.result.throwable}")
                },
                onSuccess = {
                    Log.d("ContactListItem", "Successfully loaded image for $displayName")
                }
            )
        } else {
            // 没有头像URL时显示首字母
            Log.d("ContactListItem", "No avatarUrl for $displayName, showing initials")
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (contact.isGroup()) Color(0xFFFF9800) else PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 联系人信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 群组图标
                if (contact.isGroup()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = "Group",
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 显示备注或标签（如果有）
            if (contact.alias.isNotBlank()) {
                Text(
                    text = "备注: ${contact.alias}",
                    fontSize = 13.sp,
                    color = secondaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 收藏图标
        if (contact.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 个人设置页面（包含退出登录）
 * Profile settings screen (Includes logout)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onLanguageClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onProfileClick: () -> Unit,
    themeOption: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
    profileState: ProfileSettingsUiState
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val displayName = if (profileState.displayName.isNotBlank()) {
        profileState.displayName
    } else {
        stringResource(R.string.profile_placeholder_name)
    }
    val email = if (profileState.email.isNotBlank()) {
        profileState.email
    } else {
        stringResource(R.string.profile_placeholder_email)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 用户信息卡片
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val headerElevation = if (isDark) 10.dp else 4.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = surfaceColor,
            shadowElevation = headerElevation,
            tonalElevation = if (isDark) 6.dp else 0.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            val highlightColor =
                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) else surfaceColor
            Box(
                modifier = Modifier
                    .shadow(if (isDark) 12.dp else 4.dp, RoundedCornerShape(12.dp), clip = false)
                    .border(
                        width = 1.dp,
                        color = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(highlightColor)
                    .clickable(onClick = onProfileClick)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileState.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = profileState.avatarUrl,
                                contentDescription = stringResource(R.string.profile_edit_avatar_content_description),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryTextColor()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email,
                            fontSize = 14.sp,
                            color = secondaryTextColor()
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = secondaryTextColor()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 设置项列表
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor
        ) {
            Column {
                SettingItem(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_subtitle),
                    onClick = { }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
                SettingItem(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.settings_privacy),
                    onClick = onPrivacyClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
                SettingItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    onClick = onLanguageClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
                SettingItem(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_about),
                    onClick = onAboutClick
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.theme_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.theme_section_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeOption.entries.forEach { option ->
                        val chipBorder = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = option == themeOption,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                        FilterChip(
                            selected = option == themeOption,
                            onClick = { onThemeSelected(option) },
                            label = {
                                Text(text = stringResource(option.labelRes))
                            },
                            border = chipBorder,
                            leadingIcon = if (option == themeOption) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 退出登录按钮
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UnreadBadgeRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_logout),
                fontSize = 16.sp
            )
        } // End Button
    } // End Column (ProfileScreen main content)
} // End ProfileScreen function

/**
 * 设置项组件
 * Setting item component
 */
@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = secondaryTextColor(),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = primaryTextColor()
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = secondaryTextColor()
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = secondaryTextColor()
        )
    }
}
