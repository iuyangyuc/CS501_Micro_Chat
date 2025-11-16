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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// Figma Design Colors
private val PrimaryBlue = Color(0xFF3296FA)
private val LightBlue = Color(0xFF66B3FF)
private val BackgroundGray = Color(0xFFF9FAFB)
private val SearchBarGray = Color(0xFFF3F4F6)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val UnreadBadgeRed = Color(0xFFEF4444)


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
 * 路由配置 - 定义哪些页面需要显示底部导航栏
 * Route configuration - Define which pages should show bottom navigation
 */
private object RouteConfig {
    /**
     * 检查给定路由是否应该显示底部导航栏
     * 只有主页面（Chats, Contacts, Me）显示底部导航栏
     * 其他所有子页面（如 ChatDetail、设置页等）都不显示
     */
    fun shouldShowBottomBar(route: String?): Boolean {
        if (route == null) return false

        // 定义显示底部导航栏的路由列表
        val bottomBarRoutes = setOf(
            HomeDestination.Chats.route,
            HomeDestination.Contacts.route,
            HomeDestination.Me.route
        )

        // 只有这些主页面才显示底部导航栏
        return route in bottomBarRoutes
    }
}

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

    // 使用 RouteConfig 判断是否显示底部导航栏
    val shouldShowBottomBar = RouteConfig.shouldShowBottomBar(currentRoute)
    // 判断是否在对话详情页面（用于顶栏显示）
    val isInChatDetail = currentRoute?.startsWith("chat_detail") == true

    // ========== 固定布局：顶栏 + 内容 + 底栏 ==========
    Column(
        modifier = modifier.fillMaxSize()
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
                targetState = isInChatDetail to currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "TopBarContent"
            ) { (inChatDetail, route) ->
                if (inChatDetail) {
                    // ChatDetail 顶栏内容
                    ChatDetailTopBar(
                        conversationName = navBackStackEntry?.arguments?.getString("conversationName")?.let {
                            URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                        } ?: "",
                        conversationAvatar = navBackStackEntry?.arguments?.getString("conversationAvatar")?.let {
                            URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                        } ?: "",
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    // HomeScreen 顶栏内容
                    HomeTopBar(
                        currentRoute = route,
                        onAddClick = { /* TODO */ }
                    )
                }
            }
        }

        // ========== 内容区域（可动画） ==========
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BackgroundGray)
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
                    ContactsScreen()
                }

                composable(HomeDestination.Me.route) {
                    ProfileScreen(onLogout = onLogout)
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

        // ========== 固定底栏（根据路由配置决定是否显示） ==========
        if (shouldShowBottomBar) {
            NavigationBar(
                containerColor = Color.White,
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
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
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
private fun HomeTopBar(
    currentRoute: String?,
    onAddClick: () -> Unit
) {
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

        // Chats 和 Contacts 页面都显示加号按钮
        if (currentRoute == HomeDestination.Chats.route || currentRoute == HomeDestination.Contacts.route) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/**
 * ChatDetail 的顶栏内容
 */
@Composable
private fun ChatDetailTopBar(
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
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (conversationAvatar.isNotBlank()) {
            AsyncImage(
                model = conversationAvatar,
                contentDescription = "$conversationName avatar",
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
                contentDescription = "More",
                tint = Color.White
            )
        }
    }
}

/**
 * ChatDetail 的内容区域（不包含顶栏）
 */
@Composable
private fun ChatDetailContent(conversationId: String) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Nothing Here Yet",
                        color = TextSecondary,
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
            color = Color.White,
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
                        contentDescription = "Voice",
                        tint = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SearchBarGray)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                color = TextSecondary,
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
                            .background(PrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
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
                            contentDescription = "Attachment",
                            tint = TextSecondary
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
private fun ChatListScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onChatClick: (Conversation) -> Unit = {}
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 搜索栏
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SearchBarGray)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        }

        // 错误提示
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Conversations Yet",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Start a new chat to connect with others!",
                        color = TextSecondary,
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
                        color = Color(0xFFE5E7EB)
                    )
                }
            }
        }
    }
}

/**
 * 会话列表项（基于 Figma 设计）- 显示真实会话数据
 * Conversation list item (Based on Figma design) - Displays real conversation data
 */
@Composable
private fun ConversationListItem(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像 - 使用 Coil 加载网络图片
        if (avatarUrl.isNotBlank()) {
            Log.d("ConversationListItem", "Loading image for $displayName: $avatarUrl")
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$displayName avatar",
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
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedTime,
                    color = TextSecondary,
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
                    color = TextSecondary,
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
 * 联系人页面（基于 Figma 设计）- 显示分组和联系人列表
 * Contacts screen (Based on Figma design) - Shows groups and contacts list
 */
@Composable
private fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val privateContacts by viewModel.privateContacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val conversationCache by viewModel.conversationCache.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 搜索栏
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SearchBarGray)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search contacts",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        }

        // 错误提示
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Contacts Yet",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add friends to start chatting!",
                        color = TextSecondary,
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
                            onClick = { /* TODO: 打开群聊详情 */ }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFE5E7EB)
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
                            onClick = { /* TODO: 打开私聊详情 */ }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFE5E7EB)
                        )
                    }
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
private fun SectionHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BackgroundGray
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = TextSecondary,
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
private fun ContactListItem(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
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
                    text = displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 联系人信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 如果有备注名，显示原始名称
            if (contact.alias.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.contactName,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 群组图标
        if (contact.isGroup()) {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = "Group",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 个人设置页面（包含退出登录）
 * Profile settings screen (Includes logout)
 */
@Composable
private fun ProfileScreen(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 用户信息卡片
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { /* TODO: Edit profile */ }
                    .padding(16.dp),
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
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "User Name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "user@example.com",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 设置项列表
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                SettingItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFE5E7EB)
                )
                SettingItem(
                    icon = Icons.Filled.Lock,
                    title = "Privacy",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFE5E7EB)
                )
                SettingItem(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFE5E7EB)
                )
                SettingItem(
                    icon = Icons.Filled.Info,
                    title = "About",
                    onClick = { /* TODO */ }
                )
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_logout),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 设置项组件
 * Setting item component
 */
@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
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
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

