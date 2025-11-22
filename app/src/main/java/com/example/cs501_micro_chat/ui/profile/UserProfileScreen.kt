package com.example.cs501_micro_chat.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.cs501_micro_chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onStartChat: (conversationId: String, name: String, avatarUrl: String) -> Unit,
    onDeleted: () -> Unit,
    onSearchHistory: (conversationId: String) -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UserProfileEvent.OpenChat -> onStartChat(event.conversationId, event.displayName, event.avatarUrl)
                UserProfileEvent.Deleted -> onDeleted()
                is UserProfileEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is UserProfileEvent.SearchHistory -> onSearchHistory(event.conversationId)
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
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                ProfileContent(
                    state = state,
                    onStartChat = viewModel::startChat,
                    onDeleteContact = viewModel::deleteContact,
                    onSearchHistory = viewModel::searchHistory,
                    onClearHistory = viewModel::clearHistory
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: UserProfileUiState,
    onStartChat: () -> Unit,
    onDeleteContact: () -> Unit,
    onSearchHistory: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Text(
                    text = state.displayName.ifBlank { stringResource(R.string.user_profile_unknown_user) },
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.statusMessage.isNotBlank()) {
                    Text(
                        text = state.statusMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                InfoRow(value = state.email)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartChat,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isChatting && state.userId.isNotBlank()
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isChatting) {
                        stringResource(R.string.user_profile_sending_message)
                    } else {
                        stringResource(R.string.user_profile_send_message)
                    }
                )
            }
            Button(
                onClick = onSearchHistory,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.conversationId.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.user_profile_search_history))
            }
            Button(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.conversationId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Outlined.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.user_profile_clear_history))
            }
            Button(
                onClick = onDeleteContact,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                enabled = !state.isDeleting && state.userId.isNotBlank()
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isDeleting) {
                        stringResource(R.string.user_profile_deleting_contact)
                    } else {
                        stringResource(R.string.user_profile_delete_contact)
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
                Text(
                    text = if (value.isBlank()) stringResource(R.string.user_profile_not_provided) else value,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
