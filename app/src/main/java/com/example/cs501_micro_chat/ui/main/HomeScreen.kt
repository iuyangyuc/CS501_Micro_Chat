package com.example.cs501_micro_chat.ui.main

/**
 * HomeScreen.kt
 *
 * Main screen – primary interaction interface after user login
 * (based on the Figma design).
 *
 * Main Features:
 * - Bottom navigation bar (Chat, Contacts, Me)
 * - Top gradient blue AppBar
 * - Chat list screen
 * - Contacts screen
 * - Profile / settings screen
 *
 * Design Reference:
 * - Figma chat interface design
 * - Color gradient: #3296FA → #66B3FF (blue gradient)
 * - Responsive layout for different screen sizes
 *
 * @author CS501 Team
 * @date 2025-11-04
 */


import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.model.ConversationType
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.ui.chat.ChatDetailViewModel
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.chat.TranslationLanguageChooser
import com.example.cs501_micro_chat.ui.chat.messageKey
import com.example.cs501_micro_chat.ui.group.CreateGroupScreen
import com.example.cs501_micro_chat.ui.profile.GroupProfileScreen
import com.example.cs501_micro_chat.ui.profile.UserProfileScreen
import com.example.cs501_micro_chat.ui.search.ChatSearchScreen
import com.example.cs501_micro_chat.ui.search.GroupMemberSearchScreen
import com.example.cs501_micro_chat.ui.search.MessageFilterSearchScreen
import com.example.cs501_micro_chat.ui.search.MessageSearchFilter
import com.example.cs501_micro_chat.ui.search.MemberMessageSearchScreen
import com.example.cs501_micro_chat.ui.settings.AboutScreen
import com.example.cs501_micro_chat.ui.settings.PrivacySettingsScreen
import com.example.cs501_micro_chat.ui.settings.ProfileEditScreen
import com.example.cs501_micro_chat.ui.settings.SettingsScreen
import com.example.cs501_micro_chat.ui.theme.ThemeOption
import com.example.cs501_micro_chat.ui.theme.ThemeViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

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
private const val USER_PROFILE_ROUTE = "user_profile"
private const val GROUP_PROFILE_ROUTE = "group_profile"
private const val CHAT_SEARCH_ROUTE = "chat_search"
private const val GROUP_MEMBER_SEARCH_ROUTE = "group_member_search"
private const val MEMBER_MESSAGE_SEARCH_ROUTE = "member_message_search"
private const val MESSAGE_FILTER_SEARCH_ROUTE = "message_filter_search"
private const val CREATE_GROUP_ROUTE = "create_group"
private const val PROFILE_UPDATED_KEY = "profile_updated"


/**
 * Bottom navigation items displayed on the home screen,
 * designed according to the Figma layout.
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

// Define the items list outside to avoid class initialization issues

private val homeDestinationItems: List<HomeDestination>
    get() = listOf(
        HomeDestination.Chats,
        HomeDestination.Contacts,
        HomeDestination.Me
    )

/**
 * Home screen composable
 * (based on the Figma design).
 *
 * @param onLogout Callback triggered when the user logs out
 * @param modifier Modifier applied to this composable
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
        it.startsWith("chat_detail") ||
                it == LANGUAGE_SETTINGS_ROUTE ||
                it == PRIVACY_SETTINGS_ROUTE ||
        it == ABOUT_ROUTE ||
        it == PROFILE_EDIT_ROUTE ||
        it.startsWith(USER_PROFILE_ROUTE) ||
        it.startsWith(GROUP_PROFILE_ROUTE) ||
        it.startsWith(CHAT_SEARCH_ROUTE) ||
        it.startsWith(GROUP_MEMBER_SEARCH_ROUTE) ||
        it.startsWith(MEMBER_MESSAGE_SEARCH_ROUTE) ||
        it.startsWith(MESSAGE_FILTER_SEARCH_ROUTE) ||
        it == CREATE_GROUP_ROUTE
    } == true
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Create a shared HomeViewModel for global features (e.g., adding friends)

    val sharedHomeViewModel: HomeViewModel = hiltViewModel()

    // ========== Fixed layout: top bar + content + bottom bar ==========

    Scaffold(
        topBar = {
            // ========== Fixed top bar (shared across all screens, including chat_detail) ==========
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
                    // Smooth transition of top bar content

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
                                // Use ChatDetailViewModel to retrieve the correct avatar

                                val chatDetailViewModel: ChatDetailViewModel = hiltViewModel(navBackStackEntry!!)
                                val otherUserId by chatDetailViewModel.otherUserId.collectAsStateWithLifecycle()
                                val otherUserAvatarUrl by chatDetailViewModel.otherUserAvatarUrl.collectAsStateWithLifecycle()
                                val conversationType by chatDetailViewModel.conversationType.collectAsStateWithLifecycle()
                                val convoId by chatDetailViewModel.conversationId.collectAsStateWithLifecycle()
                                val conversationNameVm by chatDetailViewModel.conversationName.collectAsStateWithLifecycle()
                                val conversationAvatarVm by chatDetailViewModel.conversationAvatarUrl.collectAsStateWithLifecycle()

                                // Get the conversation name from route parameters

                                val conversationNameArg = navBackStackEntry?.arguments?.getString("conversationName")?.let {
                                    URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                                } ?: ""

                                // Get the initial avatar from route parameters (used as a fallback)

                                val fallbackAvatarArg = navBackStackEntry?.arguments?.getString("conversationAvatar")?.let {
                                    URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                                } ?: ""

                                val resolvedConversationName = conversationNameVm.ifBlank { conversationNameArg }
                                val resolvedConversationAvatar = when {
                                    otherUserAvatarUrl.isNotBlank() -> otherUserAvatarUrl
                                    conversationAvatarVm.isNotBlank() -> conversationAvatarVm
                                    else -> fallbackAvatarArg
                                }

                                ChatDetailTopBar(
                                    conversationName = resolvedConversationName,
                                    // Prefer the resolved avatar URL from the ViewModel; fall back to the route parameter if null

                                    conversationAvatar = resolvedConversationAvatar,
                                    onBack = { navController.popBackStack() },
                                    onProfileClick = {
                                        when (conversationType) {
                                            ConversationType.PRIVATE -> if (otherUserId.isNotBlank()) {
                                                navController.navigate("$USER_PROFILE_ROUTE/$otherUserId?conversationId=$convoId&source=chat")
                                            }

                                            ConversationType.GROUP -> if (convoId.isNotBlank()) {
                                                navController.navigate("$GROUP_PROFILE_ROUTE/$convoId?source=chat")
                                            }
                                        }
                                    }
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
                                ProfileHeaderTopBar(onBack = { navController.popBackStack() })
                            }

                            route?.startsWith(USER_PROFILE_ROUTE) == true -> {
                                ProfileHeaderTopBar(onBack = { navController.popBackStack() })
                            }

                            route?.startsWith(GROUP_PROFILE_ROUTE) == true -> {
                                val groupProfileViewModel: com.example.cs501_micro_chat.ui.profile.GroupProfileViewModel = hiltViewModel(navBackStackEntry!!)
                                val groupState by groupProfileViewModel.uiState.collectAsStateWithLifecycle()
                            GroupHeaderTopBar(
                                memberCount = groupState.members.size,
                                onBack = { navController.popBackStack() }
                            )
                        }

                            route?.startsWith(GROUP_MEMBER_SEARCH_ROUTE) == true ||
                                    route?.startsWith(MEMBER_MESSAGE_SEARCH_ROUTE) == true -> {
                                GroupMemberSearchTopBar(onBack = { navController.popBackStack() })
                            }

                            route?.startsWith(MESSAGE_FILTER_SEARCH_ROUTE) == true -> {
                                val filterArg = navBackStackEntry?.arguments?.getString("filter")
                                val filter = MessageSearchFilter.fromArg(filterArg)
                                MessageFilterSearchTopBar(
                                    title = stringResource(filter.titleRes),
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            route?.startsWith(CHAT_SEARCH_ROUTE) == true -> {
                                ChatSearchTopBar(onBack = { navController.popBackStack() })
                            }

                            route == CREATE_GROUP_ROUTE -> {
                                CreateGroupTopBar(onBack = { navController.popBackStack() })
                            }

                            else -> {
                                HomeTopBar(
                                    currentRoute = route,
                                    homeViewModel = sharedHomeViewModel,
                                    onNavigateToChat = { convoId, name, avatar ->
                                        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                                        val encodedAvatar = URLEncoder.encode(avatar, StandardCharsets.UTF_8.toString())
                        navController.navigate("chat_detail/$convoId/$encodedName/$encodedAvatar") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                                    onNavigateToCreateGroup = {
                                        navController.navigate(CREATE_GROUP_ROUTE)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // ========== Fixed bottom bar (visible on non-fullscreen routes) ==========

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
    ) { paddingValues ->
        // ========== Content area (NavHost) ==========

        NavHost(
            navController = navController,
            startDestination = HomeDestination.Chats.route,
            modifier = Modifier.padding(paddingValues)
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

                composable(HomeDestination.Contacts.route) { backStackEntry ->
                    val contactsViewModel: ContactsViewModel = hiltViewModel()
                    val refreshFlow = remember(backStackEntry) {
                        backStackEntry.savedStateHandle.getStateFlow("contacts_refresh", false)
                    }
                    val refreshRequested by refreshFlow.collectAsStateWithLifecycle()

                    LaunchedEffect(refreshRequested) {
                        if (refreshRequested) {
                            contactsViewModel.refresh()
                            backStackEntry.savedStateHandle["contacts_refresh"] = false
                        }
                    }
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        onContactClick = { contact ->
                            if (contact.isGroup() && contact.conversationId.isNotBlank()) {
                                navController.navigate("$GROUP_PROFILE_ROUTE/${contact.conversationId}?source=contacts")
                            } else if (!contact.isGroup() && contact.contactId.isNotBlank()) {
                                navController.navigate("$USER_PROFILE_ROUTE/${contact.contactId}?conversationId=${contact.conversationId}&source=contacts")
                            }
                        },
                        onAvatarClick = { contact ->
                            if (contact.isGroup() && contact.conversationId.isNotBlank()) {
                                navController.navigate("$GROUP_PROFILE_ROUTE/${contact.conversationId}?source=contacts")
                            } else if (!contact.isGroup() && contact.contactId.isNotBlank()) {
                                navController.navigate("$USER_PROFILE_ROUTE/${contact.contactId}?conversationId=${contact.conversationId}&source=contacts")
                            }
                        }
                    )
                }

                composable(HomeDestination.Me.route) { backStackEntry ->
                    val themeViewModel: ThemeViewModel = hiltViewModel()
                    val themeOption by themeViewModel.themeOption.collectAsStateWithLifecycle()
                    val profileViewModel: ProfileSettingsViewModel = hiltViewModel()
                    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current
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
                        onPrivacyClick = {
                            if (profileState.isGoogleSignIn) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.privacy_google_sign_in_blocked),
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            } else {
                                navController.navigate(PRIVACY_SETTINGS_ROUTE)
                            }
                        },
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

                // ChatDetail screen – slides in from the right
                composable(
                    route = "chat_detail/{conversationId}/{conversationName}/{conversationAvatar}?targetDate={targetDate}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("conversationName") { type = NavType.StringType },
                        navArgument("conversationAvatar") { type = NavType.StringType },
                        navArgument("targetDate") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
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
                    val targetDate = backStackEntry.arguments?.getLong("targetDate")?.takeIf { it > 0 }
                    ChatDetailContent(
                        conversationId = conversationId,
                        targetDateMillis = targetDate,
                        onAvatarClick = { userId ->
                            navController.navigate("$USER_PROFILE_ROUTE/$userId?conversationId=$conversationId&source=chat")
                        }
                    )
                }

                // Create Group Screen
                composable(route = CREATE_GROUP_ROUTE) {
                    CreateGroupScreen(
                        homeViewModel = sharedHomeViewModel,
                        onBack = { navController.popBackStack() },
                        onGroupCreated = { groupId, groupName ->
                            val encodedName = URLEncoder.encode(groupName, StandardCharsets.UTF_8.toString())
                            navController.navigate("chat_detail/$groupId/$encodedName/") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(
                    route = "$GROUP_PROFILE_ROUTE/{conversationId}?source={source}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("source") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                    val sourceArg = backStackEntry.arguments?.getString("source").orEmpty()
                    val onStartChatHandler: (String, String, String) -> Unit =
                        if (sourceArg == "chat") {
                            { _, _, _ -> navController.popBackStack() }
                        } else {
                            { convoId, name, avatar ->
                                val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                                val encodedAvatar = URLEncoder.encode(avatar, StandardCharsets.UTF_8.toString())
                                navController.navigate("chat_detail/$convoId/$encodedName/$encodedAvatar") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    GroupProfileScreen(
                        conversationId = conversationId,
                        onBack = { navController.popBackStack() },
                        onStartChat = onStartChatHandler,
                        onLeftGroup = { navController.popBackStack() },
                        onOpenSearch = { convoId ->
                            val encodedId = URLEncoder.encode(convoId, StandardCharsets.UTF_8.toString())
                            navController.navigate("$CHAT_SEARCH_ROUTE/$encodedId")
                        },
                        onMemberClick = { userId ->
                            navController.navigate("$USER_PROFILE_ROUTE/$userId")
                        },
                        onRefreshContacts = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("contacts_refresh", true)
                        }
                    )
                }

                composable(
                    route = "$USER_PROFILE_ROUTE/{userId}?conversationId={conversationId}&source={source}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType },
                        navArgument("conversationId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("source") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                    val sourceArg = backStackEntry.arguments?.getString("source").orEmpty()
                    val onBackHandler: () -> Unit = {
                        val popped = navController.popBackStack()
                        if (!popped && sourceArg == "contacts") {
                            navController.navigate(HomeDestination.Contacts.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    val startChatHandler: (String, String, String) -> Unit = { conversationId, name, avatarUrl ->
                        if (sourceArg == "chat") {
                            navController.popBackStack()
                        } else {
                            val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                            val encodedAvatar = URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("chat_detail/$encodedId/$encodedName/$encodedAvatar") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    UserProfileScreen(
                        userId = userId,
                        onBack = onBackHandler,
                        onStartChat = startChatHandler,
                        onSearchHistory = { conversationId ->
                            val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            navController.navigate("$CHAT_SEARCH_ROUTE/$encodedId")
                        },
                        onDeleted = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "$CHAT_SEARCH_ROUTE/{conversationId}",
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
                    ChatSearchScreen(
                        conversationId = conversationId,
                        onBack = { navController.popBackStack() },
                        onGroupMembersClick = {
                            val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            navController.navigate("$GROUP_MEMBER_SEARCH_ROUTE/$encodedId")
                        },
                        onDateSelected = { targetMillis ->
                            val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            val encodedName = URLEncoder.encode("", StandardCharsets.UTF_8.toString())
                            val encodedAvatar = URLEncoder.encode("", StandardCharsets.UTF_8.toString())
                            navController.navigate("chat_detail/$encodedId/$encodedName/$encodedAvatar?targetDate=$targetMillis")
                        },
                        onFilterSelected = { filter ->
                            val encodedId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            navController.navigate("$MESSAGE_FILTER_SEARCH_ROUTE/$encodedId?filter=${filter.arg}")
                        }
                    )
                }
                composable(
                    route = "$GROUP_MEMBER_SEARCH_ROUTE/{conversationId}",
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
                    GroupMemberSearchScreen(
                        onBack = { navController.popBackStack() },
                        onMemberClick = { member ->
                            val encodedConversation = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
                            val encodedMemberId = URLEncoder.encode(member.id, StandardCharsets.UTF_8.toString())
                            val encodedName = URLEncoder.encode(member.name, StandardCharsets.UTF_8.toString())
                            val encodedAvatar = URLEncoder.encode(member.avatarUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("$MEMBER_MESSAGE_SEARCH_ROUTE/$encodedConversation/$encodedMemberId?memberName=$encodedName&memberAvatar=$encodedAvatar")
                        }
                    )
                }
                composable(
                    route = "$MEMBER_MESSAGE_SEARCH_ROUTE/{conversationId}/{memberId}?memberName={memberName}&memberAvatar={memberAvatar}",
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("memberId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("memberName") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("memberAvatar") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) {
                    MemberMessageSearchScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = "$MESSAGE_FILTER_SEARCH_ROUTE/{conversationId}?filter={filter}",
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("filter") {
                            type = NavType.StringType
                            defaultValue = MessageSearchFilter.Photos.arg
                        }
                    )
                ) {
                    MessageFilterSearchScreen(onBack = { navController.popBackStack() })
                }
            }
    }


}

/**
 * Top bar content for the HomeScreen
 */

@Composable
fun HomeTopBar(
    currentRoute: String?,
    homeViewModel: HomeViewModel,
    onNavigateToChat: (String, String, String) -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    // Controls the visibility of the dropdown menu
    var showAddMenu by remember { mutableStateOf(false) }
    // Controls the visibility of the add-friend dialog
    var showAddFriendDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
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

        // Both Chats and Contacts screens display the add button and dropdown menu
        if (currentRoute == HomeDestination.Chats.route || currentRoute == HomeDestination.Contacts.route) {
            Box {
                IconButton(onClick = { showAddMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    // Add contact option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = stringResource(R.string.content_description_add_friend),
                                    tint = primaryTextColor(),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.add_option_new_contact),
                                    color = primaryTextColor(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            showAddMenu = false
                            showAddFriendDialog = true
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Add group option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = "Create Group",
                                    tint = primaryTextColor(),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.add_option_new_group),
                                    color = primaryTextColor(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        onClick = {
                            showAddMenu = false
                            onNavigateToCreateGroup()
                        }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }

    // Add-friend search dialog
    if (showAddFriendDialog) {
        AddFriendDialog(
            homeViewModel = homeViewModel,
            onDismiss = {
                showAddFriendDialog = false
                homeViewModel.clearAddFriendSearch()
            }
        )
    }


}

@Composable
fun ChatSearchTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.search_this_chat_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun ProfileHeaderTopBar(onBack: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_description_back),
                    tint = Color.White
                )
            }
        }

        Text(
            text = stringResource(R.string.profile_header_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun GroupHeaderTopBar(memberCount: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.group_profile_title_with_count, memberCount),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun CreateGroupTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.create_group_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun GroupMemberSearchTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.search_by_member_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun MessageFilterSearchTopBar(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
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
 * Top bar content for ChatDetail
 */
@Composable
fun ChatDetailTopBar(
    conversationName: String,
    conversationAvatar: String,
    onBack: () -> Unit,
    onProfileClick: () -> Unit = {}
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

        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.content_description_more),
                tint = Color.White
            )
        }
    }
}

/**
 * Content area for ChatDetail (excluding the top bar)
 */
private sealed interface ChatListItem {
    data class DateHeader(val date: LocalDate) : ChatListItem
    data class MessageItem(val message: Message) : ChatListItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailContent(
    conversationId: String,
    targetDateMillis: Long? = null,
    onAvatarClick: (String) -> Unit = {}
) {
    val viewModel: ChatDetailViewModel = hiltViewModel()
    val context = LocalContext.current

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasLoadedInitial by viewModel.hasLoadedInitial.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val translationStates by viewModel.translationStates.collectAsStateWithLifecycle()
    val voiceTranscriptionStates by viewModel.voiceTranscriptionStates.collectAsStateWithLifecycle()
    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()
    val preferredTranslationLanguage by viewModel.preferredTranslationLanguage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showActionSheet by remember { mutableStateOf(false) }
    var messageAwaitingTranslation by remember { mutableStateOf<Message?>(null) }
    var selectedLanguage by remember(preferredTranslationLanguage) { mutableStateOf(preferredTranslationLanguage) }
    var isSummarySelectionMode by remember { mutableStateOf(false) }
    var selectedSummaryMessages by remember { mutableStateOf(setOf<String>()) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    val languageOptions = remember { LanguageOption.entries }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val contentResolver = context.contentResolver
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStart by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) {
        isSummarySelectionMode = false
        selectedSummaryMessages = emptySet()
        showSummaryDialog = false
        viewModel.clearSummaryResult()
        viewModel.loadMessages(conversationId)
    }

    fun resolveMimeType(uri: Uri, fallback: String): Pair<String, String?> {
        val resolverType = contentResolver.getType(uri)
        val mimeType = resolverType ?: fallback
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: uri.toString().substringAfterLast('.', "")
        return mimeType to extension
    }

    fun handlePickedMedia(uri: Uri, isVideo: Boolean) {
        coroutineScope.launch {
            val (mimeType, extension) = resolveMimeType(
                uri,
                if (isVideo) "video/mp4" else "image/jpeg"
            )
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch

            if (isVideo) {
                viewModel.uploadVideoMessage(
                    conversationId = conversationId,
                    videoBytes = bytes,
                    mimeType = mimeType,
                    extension = extension
                )
            } else {
                viewModel.uploadImageMessage(
                    conversationId = conversationId,
                    imageBytes = bytes,
                    mimeType = mimeType,
                    extension = extension
                )
            }
        }
    }

    fun startRecording() {
        try {
            val output = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp4")
            val recorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordingFile = output
            recordingStart = SystemClock.elapsedRealtime()
            isRecording = true
        } catch (e: Exception) {
            Log.e("ChatDetailContent", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            recordingFile = null
            isRecording = false
        }
    }

    fun stopRecordingAndUpload() {
        val file = recordingFile ?: return
        val start = recordingStart
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("ChatDetailContent", "Failed to stop recording", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        val duration = (SystemClock.elapsedRealtime() - start).coerceAtLeast(1L)
        coroutineScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { file.readBytes() }.getOrNull()
            } ?: return@launch
            withContext(Dispatchers.IO) {
                runCatching { file.delete() }
            }
            recordingFile = null
            viewModel.uploadVoiceMessage(
                conversationId = conversationId,
                audioBytes = bytes,
                durationMillis = duration,
                mimeType = "audio/mp4",
                extension = "mp4"
            )
        }
    }

    fun readDurationMillis(uri: Uri): Long? {
        // Try direct read via content resolver
        val direct = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull()
        if (direct != null) return direct

        // Fallback: copy to temp file then read
        return runCatching {
            val temp = File.createTempFile("voice_meta_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(temp.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
                temp.delete()
            }
        }.getOrNull()
    }

    fun handlePickedFile(uri: Uri) {
        coroutineScope.launch {
            val (mimeType, extension) = resolveMimeType(uri, "application/octet-stream")
            val durationGuess = readDurationMillis(uri) ?: 1000L
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch

            when {
                mimeType.contains("pdf") -> {
                    viewModel.uploadVoiceMessage(
                        conversationId = conversationId,
                        audioBytes = bytes,
                        durationMillis = durationGuess,
                        mimeType = mimeType,
                        extension = extension
                    )
                }
                mimeType.contains("audio") || mimeType.contains("mp4") -> {
                    viewModel.uploadVoiceMessage(
                        conversationId = conversationId,
                        audioBytes = bytes,
                        durationMillis = durationGuess,
                        mimeType = mimeType,
                        extension = extension
                    )
                }
                else -> {
                    viewModel.uploadVoiceMessage(
                        conversationId = conversationId,
                        audioBytes = bytes,
                        durationMillis = durationGuess,
                        mimeType = mimeType,
                        extension = extension
                    )
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        }
    }

    fun requestAudioPermissionAndRecord() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> startRecording()
            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { handlePickedMedia(it, isVideo = false) }
    }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { handlePickedMedia(it, isVideo = true) }
    }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handlePickedFile(it) }
    }

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

    fun speakMessage(text: String) {
        if (text.isBlank() || !ttsReady) return
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "chat_message_tts_${text.hashCode()}"
        )
    }

    var hasJumpedToTarget by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(messages.size, targetDateMillis) {
        if (messages.isNotEmpty()) {
            val targetIndex = targetDateMillis
                ?.takeIf { it > 0 }
                ?.let { ts ->
                    messages.indexOfFirst { it.timestamp >= ts }.takeIf { it >= 0 }
                }
                ?: (messages.size - 1)

            if (!hasJumpedToTarget || targetDateMillis != null) {
                listState.scrollToItem(targetIndex)
                hasJumpedToTarget = true
            }
        }
    }

    LaunchedEffect(summaryState.result) {
        if (summaryState.result != null) {
            isSummarySelectionMode = false
            selectedSummaryMessages = emptySet()
            showSummaryDialog = true
        }
    }

    LaunchedEffect(summaryState.error) {
        summaryState.error?.takeIf { it.isNotBlank() }?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    fun startSummarySelection(initial: Message) {
        if (initial.type != MessageType.TEXT || summaryState.isSummarizing) return
        isSummarySelectionMode = true
        selectedSummaryMessages = setOf(messageKey(initial))
        viewModel.clearSummaryResult()
    }

    fun toggleSummarySelection(target: Message) {
        if (!isSummarySelectionMode || summaryState.isSummarizing || target.type != MessageType.TEXT) return
        val key = messageKey(target)
        selectedSummaryMessages = if (selectedSummaryMessages.contains(key)) {
            selectedSummaryMessages - key
        } else {
            selectedSummaryMessages + key
        }
    }

    fun cancelSummarySelection() {
        if (summaryState.isSummarizing) return
        isSummarySelectionMode = false
        selectedSummaryMessages = emptySet()
        viewModel.clearSummaryResult()
    }

    fun submitSummarySelection() {
        val selectedMessages = messages.filter { it.type == MessageType.TEXT && selectedSummaryMessages.contains(messageKey(it)) }
        if (selectedMessages.isEmpty()) {
            viewModel.clearSummaryResult()
            return
        }
        viewModel.summarizeMessages(selectedMessages)
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColorChat = MaterialTheme.colorScheme.surface
    val searchBackgroundChat = MaterialTheme.colorScheme.surfaceVariant
    val zoneId = remember { ZoneId.systemDefault() }
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    actionSheetState.hide()
                    showActionSheet = false
                }
            },
            sheetState = actionSheetState,
            containerColor = surfaceColorChat,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            data class SheetAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)
            val actions = listOf(
                SheetAction(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(R.string.action_sheet_photos),
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ),
                SheetAction(
                    icon = Icons.Default.AttachFile,
                    label = stringResource(R.string.action_sheet_file),
                    onClick = {
                        filePicker.launch("*/*")
                    }
                ),
                SheetAction(
                    icon = Icons.Default.Videocam,
                    label = stringResource(R.string.action_sheet_video),
                    onClick = {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                ),
                SheetAction(
                    icon = Icons.Default.Mic,
                    label = stringResource(R.string.action_sheet_voice),
                    onClick = {
                        if (isRecording) {
                            stopRecordingAndUpload()
                        } else {
                            requestAudioPermissionAndRecord()
                        }
                    }
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(actions) { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(searchBackgroundChat)
                                .clickable {
                                    coroutineScope.launch {
                                        actionSheetState.hide()
                                        showActionSheet = false
                                        action.onClick()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = action.label,
                            color = primaryTextColor(),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            recordingFile = null
            isRecording = false
        }
    }

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
            val showInitialLoading = (!hasLoadedInitial && messages.isEmpty()) || (isLoading && messages.isEmpty())
            val showEmptyState = hasLoadedInitial && !isLoading && messages.isEmpty()

            if (showInitialLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            } else if (showEmptyState) {
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
                Column(modifier = Modifier.fillMaxSize()) {

                    // ===== Summary Selection Bar（from main）=====
                    if (isSummarySelectionMode) {
                        SummarySelectionBar(
                            selectedCount = selectedSummaryMessages.size,
                            isSubmitting = summaryState.isSummarizing,
                            onCancel = { cancelSummarySelection() },
                            onSubmit = { submitSummarySelection() }
                        )
                    }

                    // ===== Date grouping logic (from xyh) =====
                    val chatItems = remember(messages) {
                        val sorted = messages.sortedBy { it.timestamp }
                        buildList<ChatListItem> {
                            var lastDate: LocalDate? = null

                            sorted.forEach { message ->
                                val date = Instant.ofEpochMilli(message.timestamp)
                                    .atZone(zoneId)
                                    .toLocalDate()

                                if (lastDate != date) {
                                    add(ChatListItem.DateHeader(date))
                                    lastDate = date
                                }

                                add(ChatListItem.MessageItem(message))
                            }
                        }
                    }

                    // ===== Render the LazyColumn (dates + messages + summary selection) =====
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = chatItems,
                            key = { item ->
                                when (item) {
                                    is ChatListItem.DateHeader -> "date_${item.date}"
                                    is ChatListItem.MessageItem -> messageKey(item.message)
                                }
                            }
                        ) { item ->
                            when (item) {

                                // === Date header ===
                                is ChatListItem.DateHeader -> {
                                    DateDivider(date = item.date)
                                }

                                // === Message item ===
                                is ChatListItem.MessageItem -> {
                                    val message = item.message

                                    com.example.cs501_micro_chat.ui.chat.MessageBubble(
                                        message = message,
                                        isSelf = message.senderId == currentUserId,
                                        translationState = translationStates[messageKey(message)],
                                        transcriptionState = voiceTranscriptionStates[messageKey(message)],
                                        onAvatarClick = onAvatarClick,
                                        onTranslateClick = { messageAwaitingTranslation = message },
                                        onClearTranslation = { viewModel.clearTranslationFor(it) },
                                        onPlayClick = { speakMessage(message.content) },
                                        onTranscribeClick = { viewModel.transcribeVoiceMessage(it) },
                                        onClearTranscription = { viewModel.clearTranscriptionFor(it) },

                                        // ===== Summary selection mode (from main) =====
                                        summarySelectionMode = isSummarySelectionMode,
                                        isSelectedForSummary = selectedSummaryMessages.contains(messageKey(message)),
                                        onSummaryToggle = { toggleSummarySelection(it) },
                                        onStartSummarySelection = { startSummarySelection(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }


        // Input bar
        Surface(
            color = surfaceColorChat,
            shadowElevation = 8.dp
        ) {
            Column {
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.voice_recording_hint),
                            color = primaryTextColor(),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { stopRecordingAndUpload() }) {
                            Text(text = stringResource(R.string.voice_recording_stop))
                        }
                    }
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            if (showActionSheet) {
                                coroutineScope.launch {
                                    actionSheetState.hide()
                                    showActionSheet = false
                                }
                            } else {
                                showActionSheet = true
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.content_description_attachment),
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

                    val hasMessage = inputText.trim().isNotEmpty()
                    if (hasMessage) {
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
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.content_description_send),
                                tint = secondaryTextColor()
                            )
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
                        color = secondaryTextColor(),
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

    if (showSummaryDialog && summaryState.result != null) {
        AlertDialog(
            onDismissRequest = {
                showSummaryDialog = false
                viewModel.clearSummaryResult()
            },
            title = { Text(text = stringResource(R.string.summary_dialog_title)) },
            text = {
                Text(
                    text = summaryState.result.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSummaryDialog = false
                        viewModel.clearSummaryResult()
                    }
                ) {
                    Text(text = stringResource(R.string.summary_dialog_close))
                }
            }
        )
    }
}

@Composable
private fun SummarySelectionBar(
    selectedCount: Int,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.summary_selection_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = stringResource(R.string.summary_selection_subtitle, selectedCount),
                        color = secondaryTextColor(),
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onCancel,
                    enabled = !isSubmitting
                ) {
                    Text(text = stringResource(R.string.summary_selection_cancel))
                }
                Button(
                    onClick = onSubmit,
                    enabled = selectedCount > 0 && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(text = stringResource(R.string.summary_selection_action))
                }
            }
            if (isSubmitting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun DateDivider(date: LocalDate) {
    val configuration = LocalConfiguration.current
    val locale = if (configuration.locales.size() > 0) {
        configuration.locales[0]
    } else {
        Locale.getDefault()
    }
    val sameYearFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val otherYearFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label = when {
        date == today -> stringResource(R.string.chat_date_today)
        date == yesterday -> stringResource(R.string.chat_date_yesterday)
        date.year == today.year -> date.format(sameYearFormatter)
        else -> date.format(otherYearFormatter)
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = lineColor
        )
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = lineColor
        )
    }
}

/**
 * Chat list screen (based on the Figma design) that reads real data from Firebase
 */
@Composable
fun ChatListScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onChatClick: (Conversation) -> Unit = {}
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isUsersLoading by viewModel.isUsersLoading.collectAsStateWithLifecycle()
    val areContactsReady by viewModel.isContactsReady.collectAsStateWithLifecycle()

    // Search-related state
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    // Controls whether the search bar is active
    var isSearchActive by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant

    val showLoading = isLoading || isUsersLoading || !areContactsReady

    Box(modifier = Modifier.fillMaxSize()) {
        if (showLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                // Search bar
                Surface(
                    color = surfaceColor,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        if (isSearchActive) {
                            // Active state: editable search bar
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
                                    contentDescription = stringResource(R.string.content_description_search),
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
                                            text = stringResource(R.string.search_hint),
                                            color = secondaryTextColor(),
                                            fontSize = 15.sp
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
                        } else {
                            // Default state: placeholder search bar
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
                                    contentDescription = stringResource(R.string.content_description_search),
                                    tint = secondaryTextColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    color = secondaryTextColor(),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Display search results or the conversation list
                if (isSearchActive && searchQuery.isNotBlank()) {
                    // Search results
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
                    // Regular conversation list
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
}

/**
 * Conversation list item (Based on Figma design) - Displays real conversation data
 */
@Composable
fun ConversationListItem(
    conversation: Conversation,
    viewModel: HomeViewModel,
    onClick: () -> Unit = {}
) {
    // Observe changes in userCache to ensure the UI updates after user data is loaded
    val userCache by viewModel.userCache.collectAsStateWithLifecycle()

    val formattedTime = if (conversation.lastMessage.isNotBlank()) {
        viewModel.formatTime(conversation.lastMessageTime)
    } else {
        ""
    }

    // Use the new method to retrieve the display name and avatar
    val displayName = viewModel.getDisplayName(conversation)
    val avatarUrl = viewModel.getAvatarUrl(conversation)

    // Debug log
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
        // Avatar - load network image using Coil
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
            // Display the initial letter when no avatar URL is available
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

        // Conversation info
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
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        color = secondaryTextColor(),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatLastMessagePreview(conversation.lastMessage),
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

@Composable
private fun formatLastMessagePreview(lastMessage: String): String {
    val imageLabel = stringResource(R.string.last_message_image)
    val videoLabel = stringResource(R.string.last_message_video)
    val voiceSeconds = extractVoiceSeconds(lastMessage)

    return when {
        lastMessage.equals("IMAGE", ignoreCase = true) ||
            lastMessage.contains("图片") -> imageLabel
        lastMessage.equals("VIDEO", ignoreCase = true) ||
            lastMessage.contains("视频") -> videoLabel
        lastMessage.startsWith("VOICE_", ignoreCase = true) -> {
            voiceSeconds?.let { stringResource(R.string.last_message_voice, it) }
                ?: stringResource(R.string.last_message_voice_no_duration)
        }
        lastMessage.contains("语音消息") || lastMessage.contains("voice", ignoreCase = true) -> {
            voiceSeconds?.let { stringResource(R.string.last_message_voice, it) }
                ?: stringResource(R.string.last_message_voice_no_duration)
        }
        else -> lastMessage
    }
}

private fun extractVoiceSeconds(text: String): String? {
    val regex = Regex("(\\d+)\\s*s", RegexOption.IGNORE_CASE)
    regex.find(text)?.groupValues?.getOrNull(1)?.let { return it }
    val digits = text.filter { it.isDigit() }
    return digits.takeIf { it.isNotBlank() }
}

/**
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
                    text = stringResource(R.string.no_results_found),
                    color = primaryTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.try_different_search_keyword),
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

    // Loading indicator
    if (isLoading && conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
    // Empty state
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
                    text = stringResource(R.string.no_conversations_yet),
                    color = secondaryTextColor(),
                    fontSize = 16.sp
                )
                Text(
                    text = stringResource(R.string.start_new_chat_hint),
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    }
    // chatting list
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
 * Contacts screen (Based on Figma design) - Shows groups and contacts list
 */
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    onContactClick: (Contact) -> Unit = {},
    onAvatarClick: (Contact) -> Unit = {}
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val privateContacts by viewModel.privateContacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Search-related state
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    // Pending friend requests
    val pendingFriendRequests by homeViewModel.pendingFriendRequests.collectAsStateWithLifecycle()

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant

    // Controls whether the search bar is active
    var isSearchActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Search bar
            Surface(
                color = surfaceColor,
                shadowElevation = 2.dp
            ) {
                Column {
                    if (isSearchActive) {
                        // Active state: editable search bar
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
                                        text = stringResource(R.string.contacts_search_hint),
                                        color = secondaryTextColor(),
                                        fontSize = 15.sp
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
                    } else {
                        // Default state: placeholder search bar
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
                                contentDescription = stringResource(R.string.content_description_search),
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.contacts_search_hint),
                                color = secondaryTextColor(),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Display search results or the contacts list
            if (isSearchActive && searchQuery.isNotBlank()) {
                // Search results
                SearchResultsList(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    viewModel = viewModel,
                    onContactClick = { contact ->
                        viewModel.clearSearch()
                        isSearchActive = false
                        onContactClick(contact)
                    },
                    onAvatarClick = onAvatarClick
                )
            } else {
                // Regular contacts list
                ContactsList(
                    groups = groups,
                    privateContacts = privateContacts,
                    pendingFriendRequests = pendingFriendRequests,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    homeViewModel = homeViewModel,
                    onContactClick = onContactClick,
                    onAvatarClick = onAvatarClick
                )
            }
        }
    }
}

/**
 * Search results list
 */
@Composable
fun SearchResultsList(
    searchResults: List<Contact>,
    isSearching: Boolean,
    viewModel: ContactsViewModel,
    onContactClick: (Contact) -> Unit,
    onAvatarClick: (Contact) -> Unit
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
                    text = stringResource(R.string.no_results_found),
                    color = primaryTextColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.try_different_search_keyword),
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
                    onClick = { onContactClick(contact) },
                    onAvatarClick = onAvatarClick
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
 * Normal contacts list
 */
@Composable
fun ContactsList(
    groups: List<Contact>,
    privateContacts: List<Contact>,
    pendingFriendRequests: List<Contact>,
    isLoading: Boolean,
    viewModel: ContactsViewModel,
    homeViewModel: HomeViewModel,
    onContactClick: (Contact) -> Unit,
    onAvatarClick: (Contact) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val collator = remember { Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY } }

    // Sort private chat contacts alphabetically
    val sortedPrivateContacts = remember(privateContacts, collator) {
        privateContacts.sortedWith { a, b ->
            collator.compare(viewModel.getDisplayName(a), viewModel.getDisplayName(b))
        }
    }

    // Group private chat contacts alphabetically
    val privateSections = remember(sortedPrivateContacts) {
        sortedPrivateContacts.groupBy { contact ->
            val name = viewModel.getDisplayName(contact).trim()
            val firstChar = name.firstOrNull()?.toString()?.uppercase(Locale.getDefault())
            firstChar?.takeIf { it.length == 1 && it[0].isLetter() } ?: "#"
        }.toSortedMap()
    }

    // Loading indicator
    if (isLoading && groups.isEmpty() && privateContacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
    // Empty state
    else if (groups.isEmpty() && privateContacts.isEmpty() && pendingFriendRequests.isEmpty()) {
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
                    text = stringResource(R.string.no_contacts_yet),
                    color = secondaryTextColor(),
                    fontSize = 16.sp
                )
                Text(
                    text = stringResource(R.string.add_friends_to_start_chatting),
                    color = secondaryTextColor(),
                    fontSize = 14.sp
                )
            }
        }
    }
    // Contacts list
    else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pending friend requests (pinned to the top)
            if (pendingFriendRequests.isNotEmpty()) {
                item(key = "pending_requests_header") {
                    SectionHeader(title = stringResource(R.string.section_friend_requests))
                }
                items(
                    items = pendingFriendRequests,
                    key = { "pending_${it.contactId}" }
                ) { contact ->
                    PendingFriendRequestItem(
                        contact = contact,
                        onAccept = { homeViewModel.acceptFriendRequest(contact.contactId) },
                        onReject = { homeViewModel.rejectFriendRequest(contact.contactId) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                }
            }

            // Group contacts
            if (groups.isNotEmpty()) {
                item(key = "groups_header") {
                    SectionHeader(title = stringResource(R.string.section_groups))
                }
                items(
                    items = groups,
                    key = { "group_${it.contactId}" }
                ) { contact ->
                    ContactListItem(
                        contact = contact,
                        viewModel = viewModel,
                        onClick = { onContactClick(contact) },
                        onAvatarClick = onAvatarClick
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                }
            }

            // Private chat contacts (grouped alphabetically)
            if (sortedPrivateContacts.isNotEmpty()) {
                item(key = "contacts_header") {
                    SectionHeader(title = stringResource(R.string.section_contacts))
                }
                privateSections.forEach { (letter, contactsInSection) ->
                    item(key = "letter_$letter") {
                        Text(
                            text = letter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = PrimaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    items(
                        items = contactsInSection,
                        key = { "contact_${it.contactId}" }
                    ) { contact ->
                        ContactListItem(
                            contact = contact,
                            viewModel = viewModel,
                            onClick = { onContactClick(contact) },
                            onAvatarClick = onAvatarClick
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
}

/**
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
 * Pending friend request item with accept/reject buttons
 */
@Composable
fun PendingFriendRequestItem(
    contact: Contact,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        val avatarModifier = Modifier
            .size(52.dp)
            .clip(CircleShape)

        if (contact.contactAvatarUrl.isNotBlank()) {
            AsyncImage(
                model = contact.contactAvatarUrl,
                contentDescription = "${contact.contactName} avatar",
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = avatarModifier.background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.contactName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contact.contactName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryTextColor()
            )
            Text(
                text = "Wants to be your friend",
                fontSize = 13.sp,
                color = secondaryTextColor()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Accept button
        Button(
            onClick = onAccept,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = stringResource(R.string.action_accept),
                fontSize = 13.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Reject button
        OutlinedButton(
            onClick = onReject,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6B7280)
            ),
            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = stringResource(R.string.action_reject),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Contact list item
 */
@Composable
fun ContactListItem(
    contact: Contact,
    viewModel: ContactsViewModel,
    onClick: () -> Unit,
    onAvatarClick: (Contact) -> Unit = {}
) {
    // Observe changes in conversationCache to ensure the UI updates after group information is loaded
    val conversationCache by viewModel.conversationCache.collectAsStateWithLifecycle()
    val pinnedConversationIds by viewModel.pinnedConversationIds.collectAsStateWithLifecycle()

    // Use ViewModel methods to retrieve the display name and avatar (for GROUP, fetched from the Conversation)
    val displayName = viewModel.getDisplayName(contact)
    val avatarUrl = viewModel.getAvatarUrl(contact)

    // debug log
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
        // Avatar
        val avatarModifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable { onAvatarClick(contact) }

        if (avatarUrl.isNotBlank()) {
            Log.d("ContactListItem", "Loading image for $displayName: $avatarUrl")
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$displayName avatar",
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
                onError = { error ->
                    Log.e("ContactListItem", "Failed to load image for $displayName: ${error.result.throwable}")
                },
                onSuccess = {
                    Log.d("ContactListItem", "Successfully loaded image for $displayName")
                }
            )
        } else {
            // Display the initial letter when no avatar URL is available
            Log.d("ContactListItem", "No avatarUrl for $displayName, showing initials")
            Box(
                modifier = avatarModifier.background(if (contact.isGroup()) Color(0xFFFF9800) else PrimaryBlue),
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

        // Contact info
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

                // Group icon
                if (contact.isGroup()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = stringResource(R.string.content_description_group),
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Display the original name if a remark is set
            if (contact.alias.isNotBlank()) {
                val originalName = contact.contactName.ifBlank { stringResource(R.string.contacts_original_name_unknown) }
                Text(
                    text = stringResource(R.string.contacts_original_name_label, originalName),
                    fontSize = 13.sp,
                    color = secondaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Favorite icon
        val isPinned = contact.conversationId.isNotBlank() && pinnedConversationIds.contains(contact.conversationId)
        if (contact.isFavorite || isPinned) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.content_description_favorite),
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(scrollState)
    ) {
        // User info card
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
                    // Avatar
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

        // Config list
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

        // log out button
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

/**
 * Add Friend Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendDialog(
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val searchQuery by homeViewModel.addFriendSearchQuery.collectAsStateWithLifecycle()
    val searchResults by homeViewModel.addFriendSearchResults.collectAsStateWithLifecycle()
    val groupSearchResults by homeViewModel.addGroupSearchResults.collectAsStateWithLifecycle()
    val isSearching by homeViewModel.isAddFriendSearching.collectAsStateWithLifecycle()

    // Observe changes in allContacts to ensure the UI recomposes when contact states update

    val allContacts by homeViewModel.allContacts.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Force refresh the contacts list when the dialog opens to ensure the data is up to date
    LaunchedEffect(Unit) {
        Log.d("AddFriendDialog", "Dialog opened, refreshing contacts...")
        homeViewModel.refreshContacts()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_friend_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor()
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.content_description_close),
                            tint = secondaryTextColor()
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Searching input field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { homeViewModel.searchUsersAndGroupsForAdd(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.add_friend_search_placeholder),
                            color = secondaryTextColor()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.content_description_search),
                            tint = secondaryTextColor()
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { homeViewModel.clearAddFriendSearch() }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.content_description_close),
                                    tint = secondaryTextColor()
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = PrimaryBlue,
                        focusedTextColor = primaryTextColor(),
                        unfocusedTextColor = primaryTextColor(),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedLeadingIconColor = secondaryTextColor(),
                        unfocusedLeadingIconColor = secondaryTextColor(),
                        focusedTrailingIconColor = secondaryTextColor(),
                        unfocusedTrailingIconColor = secondaryTextColor(),
                        focusedPlaceholderColor = secondaryTextColor(),
                        unfocusedPlaceholderColor = secondaryTextColor()
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Search results area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        // Loading state
                        isSearching -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        // Empty search state
                        searchQuery.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = secondaryTextColor()
                                    )
                                    Text(
                                        text = stringResource(R.string.add_friend_search_hint),
                                        color = secondaryTextColor(),
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.add_friend_search_subtitle),
                                        color = secondaryTextColor(),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        // No results state
                        searchResults.isEmpty() && groupSearchResults.isEmpty() -> {
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
                                        modifier = Modifier.size(64.dp),
                                        tint = secondaryTextColor()
                                    )
                                    Text(
                                        text = stringResource(R.string.no_results_found),
                                        color = primaryTextColor(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.try_different_search_keyword),
                                        color = secondaryTextColor(),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        // Display search results
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                // Group search results
                                if (groupSearchResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.group_search_section),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryTextColor(),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    items(
                                        items = groupSearchResults,
                                        key = { "group_${it.id}" }
                                    ) { group ->
                                        GroupSearchResultItem(
                                            group = group,
                                            onJoinClick = {
                                                Log.d("AddFriendDialog", "Join group: ${group.name} (${group.id})")
                                                homeViewModel.joinGroup(
                                                    groupId = group.id,
                                                    onSuccess = {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            context.getString(R.string.group_join_success),
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onError = { error ->
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            context.getString(R.string.group_join_failed) + ": $error",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }

                                // User search results
                                if (searchResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.section_contacts),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryTextColor(),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    items(
                                        items = searchResults,
                                        key = { "user_${it.id}" }
                                    ) { user ->
                                        // Observe changes in allContacts to ensure the button updates when contact states change
                                        // allContacts is a State; changes to it trigger recomposition
                                        val contactStatus = remember(allContacts, user.id) {
                                            homeViewModel.getContactStatus(user.id)
                                        }

                                        UserSearchResultItem(
                                            user = user,
                                            contactStatus = contactStatus,
                                            onAddClick = {
                                                // 发送好友请求
                                                Log.d("AddFriendDialog", "Send friend request to: ${user.username} (${user.id})")
                                                homeViewModel.sendFriendRequest(user)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Group Search Result Item
 */
@Composable
fun GroupSearchResultItem(
    group: com.example.cs501_micro_chat.data.model.Group,
    onJoinClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group Avatar
        if (group.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = group.avatarUrl,
                contentDescription = stringResource(R.string.content_description_group),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = stringResource(R.string.content_description_group),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Group Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = group.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryTextColor()
            )
            Text(
                text = stringResource(R.string.group_members_count, group.memberIds.size),
                fontSize = 12.sp,
                color = secondaryTextColor()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Add Button
        Button(
            onClick = onJoinClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.group_join_button),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * User Search Result Item
 */
@Composable
fun UserSearchResultItem(
    user: com.example.cs501_micro_chat.data.model.User,
    contactStatus: String?, // "added", "pending", "new", or null
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        if (user.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = stringResource(R.string.content_description_avatar, user.username),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryTextColor()
            )
            Text(
                text = user.email,
                fontSize = 12.sp,
                color = secondaryTextColor()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Display different buttons based on the contact status
        when (contactStatus) {
            "added" -> {
                // Already friends: show a gray "Added" button that is not clickable
                Button(
                    onClick = { /* Cannot Click */ },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.content_description_friend_added),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_friend_button_added),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            "pending" -> {
                // Request sent and pending approval: show a gray "Sent" button that is not clickable
                Button(
                    onClick = { /* Cannot Click */ },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = stringResource(R.string.content_description_request_sent),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_friend_button_sended),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            "new" -> {
                // Incoming friend request: display a prompt message
                Button(
                    onClick = { /* cannot click */ },
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_friend_button_pending),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Not added: show a blue "Add" button that is clickable
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = stringResource(R.string.content_description_add_friend),
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_friend_button_add),
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
