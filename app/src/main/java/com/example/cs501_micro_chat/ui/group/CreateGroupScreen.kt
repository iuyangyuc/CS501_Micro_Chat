package com.example.cs501_micro_chat.ui.group

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.data.model.Contact
import com.example.cs501_micro_chat.ui.main.HomeViewModel
import com.example.cs501_micro_chat.ui.settings.composables.ImageCropDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PrimaryBlue = Color(0xFF3296FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onGroupCreated: (groupId: String, groupName: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State
    var groupName by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var croppedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedMemberIds = remember { mutableStateOf(setOf<String>()) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Observe contacts from ViewModel
    val allContacts by homeViewModel.allContacts.collectAsStateWithLifecycle()
    val userCache by homeViewModel.userCache.collectAsStateWithLifecycle()

    // Filter contacts to show only PRIVATE (user) contacts, not groups
    val userContacts = remember(allContacts) {
        allContacts.filter { it.type == "PRIVATE" && !it.isNew && !it.isPending }
    }

    // Filter contacts based on search query
    val filteredContacts = remember(userContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            userContacts
        } else {
            userContacts.filter { contact ->
                contact.contactName.contains(searchQuery, ignoreCase = true) ||
                contact.alias.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Get selected members info
    val selectedMembers = remember(selectedMemberIds.value, userContacts, userCache) {
        selectedMemberIds.value.mapNotNull { id ->
            userContacts.find { it.contactId == id }?.let { contact ->
                val user = userCache[id]
                SelectedMember(
                    id = id,
                    name = contact.alias.ifBlank { contact.contactName },
                    avatarUrl = user?.avatarUrl ?: contact.contactAvatarUrl
                )
            }
        }
    }

    // Image picker
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedAvatarUri = it
            showCropDialog = true
        }
    }

    // Crop dialog
    if (showCropDialog && selectedAvatarUri != null) {
        ImageCropDialog(
            imageUri = selectedAvatarUri!!,
            onDismiss = {
                showCropDialog = false
                selectedAvatarUri = null
            },
            onCropComplete = { croppedUri ->
                croppedAvatarUri = croppedUri
                showCropDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Avatar and Name Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, PrimaryBlue, CircleShape)
                    .clickable {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (croppedAvatarUri != null) {
                    AsyncImage(
                        model = croppedAvatarUri,
                        contentDescription = stringResource(R.string.content_description_group),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.create_group_avatar_hint),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group Name Input
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text(stringResource(R.string.create_group_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    cursorColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        HorizontalDivider()

        // Selected Members Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.create_group_selected_members, selectedMembers.size),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedMembers.isEmpty()) {
                Text(
                    text = stringResource(R.string.create_group_no_members_selected),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedMembers, key = { it.id }) { member ->
                        SelectedMemberItem(
                            member = member,
                            onRemove = {
                                selectedMemberIds.value = selectedMemberIds.value - member.id
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.create_group_search_members)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.content_description_search)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.content_description_close)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                cursorColor = PrimaryBlue
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Contacts Section Header
        Text(
            text = stringResource(R.string.create_group_your_contacts),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Contacts List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredContacts, key = { it.contactId }) { contact ->
                val isSelected = selectedMemberIds.value.contains(contact.contactId)
                val user = userCache[contact.contactId]

                ContactSelectItem(
                    contact = contact,
                    avatarUrl = user?.avatarUrl ?: contact.contactAvatarUrl,
                    isSelected = isSelected,
                    onToggle = {
                        selectedMemberIds.value = if (isSelected) {
                            selectedMemberIds.value - contact.contactId
                        } else {
                            selectedMemberIds.value + contact.contactId
                        }
                    }
                )
            }
        }

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Bottom Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel Button
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = !isCreating
            ) {
                Text(stringResource(R.string.create_group_cancel))
            }

            // Create Button
            Button(
                onClick = {
                    val name = groupName.trim()
                    if (name.isEmpty()) {
                        errorMessage = context.getString(R.string.create_group_name_required)
                        return@Button
                    }
                    if (selectedMemberIds.value.isEmpty()) {
                        errorMessage = context.getString(R.string.group_create_min_members_error)
                        return@Button
                    }

                    isCreating = true
                    errorMessage = null

                    coroutineScope.launch {
                        var avatarBytes: ByteArray? = null
                        var mimeType = "image/jpeg"
                        var extension: String? = null

                        croppedAvatarUri?.let { uri ->
                            try {
                                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                extension = MimeTypeMap.getSingleton()
                                    .getExtensionFromMimeType(mimeType)
                                avatarBytes = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to read image"
                                isCreating = false
                                return@launch
                            }
                        }

                        val result = homeViewModel.createGroup(
                            name = name,
                            memberIds = selectedMemberIds.value.toList(),
                            avatarBytes = avatarBytes,
                            avatarMimeType = mimeType,
                            avatarExtension = extension
                        )

                        result.onSuccess { groupId ->
                            isCreating = false
                            onGroupCreated(groupId, name)
                        }.onFailure { error ->
                            errorMessage = error.message ?: "Failed to create group"
                            isCreating = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isCreating) {
                        stringResource(R.string.create_group_creating)
                    } else {
                        stringResource(R.string.create_group_save)
                    }
                )
            }
        }
    }
}

data class SelectedMember(
    val id: String,
    val name: String,
    val avatarUrl: String
)

@Composable
private fun SelectedMemberItem(
    member: SelectedMember,
    onRemove: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box {
            if (member.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = member.name,
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
                        text = member.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Remove button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.content_description_close),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Text(
            text = member.name,
            fontSize = 11.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ContactSelectItem(
    contact: Contact,
    avatarUrl: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = contact.contactName,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.contactName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.alias.ifBlank { contact.contactName },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (contact.alias.isNotBlank()) {
                Text(
                    text = contact.contactName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryBlue,
                checkmarkColor = Color.White
            )
        )
    }
}
