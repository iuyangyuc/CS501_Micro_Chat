package com.example.cs501_micro_chat.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cs501_micro_chat.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun GroupProfileScreen(
    conversationId: String,
    onBack: () -> Unit,
    onStartChat: (conversationId: String, name: String, avatarUrl: String) -> Unit,
    onLeftGroup: () -> Unit,
    onOpenSearch: (conversationId: String) -> Unit,
    onMemberClick: (String) -> Unit = {},
    onRefreshContacts: () -> Unit = {},
    viewModel: GroupProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(conversationId) {
        viewModel.loadGroup(conversationId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GroupProfileEvent.OpenChat -> onStartChat(event.conversationId, event.name, event.avatarUrl)
                GroupProfileEvent.LeftGroup -> onLeftGroup()
                is GroupProfileEvent.SearchHistory -> onOpenSearch(event.conversationId)
                is GroupProfileEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is GroupProfileEvent.Renamed -> onRefreshContacts()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                GroupProfileContent(
                    state = state,
                    onNameChange = viewModel::onNameChange,
                    onSaveName = viewModel::saveName,
                    onStartChat = viewModel::startChat,
                    onSearchHistory = viewModel::searchHistory,
                    onTogglePinned = viewModel::togglePinned,
                    onClearHistory = viewModel::clearHistory,
                    onLeaveGroup = viewModel::leaveGroup,
                    onMemberClick = onMemberClick
                )
            }
        }
    }
}

@Composable
private fun GroupProfileContent(
    state: GroupProfileUiState,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onStartChat: () -> Unit,
    onSearchHistory: () -> Unit,
    onTogglePinned: () -> Unit,
    onClearHistory: () -> Unit,
    onLeaveGroup: () -> Unit,
    onMemberClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.group_profile_name_label)) },
                    singleLine = true,
                    trailingIcon = {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = onSaveName, enabled = state.name.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.group_profile_save_name)
                                )
                            }
                        }
                    }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.group_profile_members_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowMembers(members = state.members, onMemberClick = onMemberClick)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = stringResource(R.string.user_profile_send_message),
                    onClick = onStartChat
                )
                ActionButton(
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.user_profile_search_history),
                    onClick = onSearchHistory
                )
                ActionButton(
                    icon = Icons.Filled.PushPin,
                    label = if (state.isPinned) stringResource(R.string.group_profile_unpin_chat) else stringResource(R.string.group_profile_pin_chat),
                    onClick = onTogglePinned
                )
                ActionButton(
                    icon = Icons.Outlined.Clear,
                    label = stringResource(R.string.user_profile_clear_history),
                    onClick = onClearHistory
                )
                ActionButton(
                    icon = Icons.Filled.Delete,
                    label = if (state.isLeaving) stringResource(R.string.group_profile_leaving) else stringResource(R.string.group_profile_leave),
                    onClick = onLeaveGroup,
                    enabled = !state.isLeaving,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
private fun FlowMembers(
    members: List<GroupMember>,
    onMemberClick: (String) -> Unit
) {
    if (members.isEmpty()) {
        Text(
            text = stringResource(R.string.group_profile_no_members),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        members.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { member ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val avatarModifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { onMemberClick(member.id) }
                        if (member.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = member.avatarUrl,
                                contentDescription = member.name,
                                modifier = avatarModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = avatarModifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = if (member.name.isBlank()) member.id else member.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
