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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
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
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.ui.chat.ChatDetailScreen

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
    Log.d("HomeScreen", "HomeScreen composable started")
    val items = homeDestinationItems
    Log.d("HomeScreen", "Navigation items count: ${items.size}")
    items.forEachIndexed { index, item ->
        Log.d("HomeScreen", "Item $index: ${item.route}")
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    Log.d("HomeScreen", "Current route: $currentRoute")

    // 判断是否在对话详情页面
    val isInChatDetail = currentRoute?.startsWith("chat_detail") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // 在对话详情页面时不显示顶栏
            if (!isInChatDetail) {
                // Figma 设计的渐变蓝色顶部栏 - 适配系统状态栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                        .statusBarsPadding() // 自动适配系统状态栏高度
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(32.dp))

                        Text(
                            text = when (currentRoute) {
                                HomeDestination.Chats.route -> stringResource(R.string.nav_chats)
                                HomeDestination.Contacts.route -> stringResource(R.string.nav_contacts)
                                HomeDestination.Me.route -> stringResource(R.string.nav_me)
                                else -> stringResource(R.string.app_name)
                            },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // 仅在聊天页面显示添加按钮
                        if (currentRoute == HomeDestination.Chats.route) {
                            IconButton(
                                onClick = { /* TODO: Show menu */ }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(32.dp))
                        }
                    }
                }
            }
            } // 结束 if (!isInChatDetail)
        },
        bottomBar = {
            // 在对话详情页面时不显示底栏
            if (!isInChatDetail) {
                // Figma 设计的底部导航栏
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Log.d("HomeScreen", "Rendering bottom bar, items count: ${items.size}")
                    items.forEachIndexed { index, destination ->
                        if (destination == null) {
                            Log.e("HomeScreen", "Null destination at index $index")
                            return@forEachIndexed
                        }

                        Log.d("HomeScreen", "Destination $index: route=${destination.route}")
                        val selected = currentRoute == destination.route

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelResId),
                                tint = if (selected) PrimaryBlue else TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(destination.labelResId),
                                color = if (selected) PrimaryBlue else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            } // 结束 if (!isInChatDetail) for bottomBar
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 只有在非对话详情页面时才应用padding
                .then(
                    if (!isInChatDetail) {
                        Modifier.padding(innerPadding)
                    } else {
                        Modifier
                    }
                )
                .background(BackgroundGray)
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeDestination.Chats.route
            ) {
                composable(HomeDestination.Chats.route) {
                    // 获取 HomeViewModel 实例
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    ChatListScreen(
                        viewModel = homeViewModel,
                        onChatClick = { conversation ->
                            // 使用 viewModel 获取对方用户的显示名称和头像
                            val displayName = homeViewModel.getDisplayName(conversation)
                            val avatarUrl = homeViewModel.getAvatarUrl(conversation)

                            // 导航到对话详情页面，URL编码避免特殊字符问题
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

                // 对话详情页面
                composable(
                    route = "chat_detail/{conversationId}/{conversationName}/{conversationAvatar}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("conversationName") { type = NavType.StringType },
                        navArgument("conversationAvatar") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    val encodedName = backStackEntry.arguments?.getString("conversationName") ?: ""
                    val encodedAvatar = backStackEntry.arguments?.getString("conversationAvatar") ?: ""

                    // URL解码
                    val conversationName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                    val conversationAvatar = URLDecoder.decode(encodedAvatar, StandardCharsets.UTF_8.toString())

                    ChatDetailScreen(
                        conversationId = conversationId,
                        conversationName = conversationName,
                        conversationAvatar = conversationAvatar,
                        onBack = { navController.popBackStack() }
                    )
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
                        text = "暂无聊天",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "开始一个新对话吧！",
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

    val unreadCount = viewModel.getUnreadCount(conversation)
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
 * 联系人页面（占位符）
 * Contacts screen (Placeholder)
 */
@Composable
private fun ContactsScreen() {
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

        // 占位符内容
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextSecondary
                )
                Text(
                    text = "Contacts page",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Coming soon...",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
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

