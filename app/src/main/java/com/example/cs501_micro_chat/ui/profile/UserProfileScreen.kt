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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val context = LocalContext.current
    var snackbarStyle by remember { mutableStateOf(SnackbarStyle.INFO) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UserProfileEvent.OpenChat -> onStartChat(event.conversationId, event.displayName, event.avatarUrl)
                UserProfileEvent.Deleted -> onDeleted()
                is UserProfileEvent.ShowError -> {
                    snackbarStyle = SnackbarStyle.ERROR
                    snackbarHostState.showSnackbar(event.message)
                }
                UserProfileEvent.AliasSaved -> {
                    snackbarStyle = SnackbarStyle.SUCCESS
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.user_profile_alias_update_success)
                    )
                }
                is UserProfileEvent.PinStatusChanged -> {
                    snackbarStyle = SnackbarStyle.SUCCESS
                    snackbarHostState.showSnackbar(
                        message = if (event.isPinned) {
                            context.getString(R.string.user_profile_pin_enabled)
                        } else {
                            context.getString(R.string.user_profile_pin_disabled)
                        }
                    )
                }
                is UserProfileEvent.ShowStatus -> {
                    snackbarStyle = if (event.success) SnackbarStyle.SUCCESS else SnackbarStyle.ERROR
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
                is UserProfileEvent.SearchHistory -> onSearchHistory(event.conversationId)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.user_profile_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.user_profile_delete_confirm_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteContact()
                    }
                ) {
                    Text(text = stringResource(R.string.user_profile_delete_confirm_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.user_profile_delete_confirm_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                ColoredSnackbar(data = data, style = snackbarStyle)
            }
        }
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
                    onDeleteContact = { showDeleteDialog = true },
                    onSearchHistory = viewModel::searchHistory,
                    onClearHistory = viewModel::clearHistory,
                    onAliasChange = viewModel::onAliasChange,
                    onSaveAlias = viewModel::saveAlias,
                    onBeginAliasEdit = viewModel::startAliasEdit,
                    onCancelAliasEdit = viewModel::cancelAliasEdit,
                    onTogglePinned = viewModel::togglePinned
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
    onClearHistory: () -> Unit,
    onAliasChange: (String) -> Unit,
    onSaveAlias: () -> Unit,
    onBeginAliasEdit: () -> Unit,
    onCancelAliasEdit: () -> Unit,
    onTogglePinned: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.isAliasEditing) {
        if (state.isAliasEditing) {
            focusRequester.requestFocus()
        }
    }

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
            Box(modifier = Modifier.fillMaxWidth()) {
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

                    if (state.isAliasEditing) {
                        OutlinedTextField(
                            value = state.aliasInput,
                            onValueChange = onAliasChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            placeholder = { Text(text = stringResource(R.string.user_profile_alias_placeholder)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (!state.isAliasSaving) {
                                        onSaveAlias()
                                    }
                                }
                            ),
                            trailingIcon = {
                                if (state.isAliasSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row {
                                        IconButton(onClick = onCancelAliasEdit) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.user_profile_alias_cancel),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = onSaveAlias) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = stringResource(R.string.user_profile_alias_save),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = state.displayName.ifBlank { stringResource(R.string.user_profile_unknown_user) },
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (state.originalName.isNotBlank() && state.displayName != state.originalName) {
                        Text(
                            text = stringResource(R.string.user_profile_original_name_label, state.originalName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    if (state.statusMessage.isNotBlank()) {
                        Text(
                            text = state.statusMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    InfoRow(value = state.email)
                }

                if (state.canEditAlias && !state.isAliasEditing) {
                    IconButton(
                        onClick = onBeginAliasEdit,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.user_profile_alias_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val pinDescription = when {
            !state.canPin -> stringResource(R.string.user_profile_pin_unavailable)
            state.isPinned -> stringResource(R.string.user_profile_pin_on_description)
            else -> stringResource(R.string.user_profile_pin_off_description)
        }
        PinSettingRow(
            title = stringResource(R.string.user_profile_pin_title),
            description = pinDescription,
            isPinned = state.isPinned,
            enabled = state.canPin && !state.isDeleted,
            isLoading = false,
            onToggle = onTogglePinned
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartChat,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isChatting && state.userId.isNotBlank() && !state.isDeleted
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
                enabled = state.conversationId.isNotBlank() && !state.isDeleted
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.user_profile_search_history))
            }
            Button(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.conversationId.isNotBlank() && !state.isDeleted,
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
                enabled = !state.isDeleting && state.userId.isNotBlank() && !state.isDeleted
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

@Composable
private fun PinSettingRow(
    title: String,
    description: String,
    isPinned: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = isPinned,
                    enabled = enabled,
                    onCheckedChange = {
                        if (enabled) {
                            onToggle()
                        }
                    }
                )
            }
        }
    }
}

private enum class SnackbarStyle { INFO, SUCCESS, ERROR }

@Composable
private fun ColoredSnackbar(
    data: SnackbarData,
    style: SnackbarStyle
) {
    val (containerColor, contentColor) = when (style) {
        SnackbarStyle.SUCCESS -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        SnackbarStyle.ERROR -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        SnackbarStyle.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Snackbar(
        snackbarData = data,
        containerColor = containerColor,
        contentColor = contentColor
    )
}
