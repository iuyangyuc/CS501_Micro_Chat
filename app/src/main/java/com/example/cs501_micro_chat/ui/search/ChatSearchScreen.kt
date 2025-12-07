package com.example.cs501_micro_chat.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.cs501_micro_chat.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneOffset
import java.time.LocalDateTime
import coil.compose.AsyncImage
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.MessageType
import com.example.cs501_micro_chat.ui.search.MessageSearchFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchScreen(
    conversationId: String,
    onBack: () -> Unit,
    onGroupMembersClick: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onFilterSelected: (MessageSearchFilter) -> Unit,
    viewModel: ChatSearchViewModel = hiltViewModel()
) {
    val (query, setQuery) = remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val locale = if (configuration.locales.size() > 0) configuration.locales[0] else Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("yyyy-MM-dd", locale) }
    val dateTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale) }
    val zoneId = remember { ZoneId.systemDefault() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults = remember(query, state.messages) {
        if (query.isBlank()) emptyList() else state.messages
            .filter { it.content.contains(query, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }
    val selectableDates = remember(state.availableDayStartMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val dateUtc = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                val dayStartLocal = dateUtc.atStartOfDay(zoneId).toInstant().toEpochMilli()
                return state.availableDayStartMillis.contains(dayStartLocal)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return state.availableDates.any { it.year == year }
            }
        }
    }
    val initialSelected = state.availableDates.maxOrNull()
        ?.atStartOfDay(zoneId)
        ?.toInstant()
        ?.toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelected,
        selectableDates = selectableDates
    )
    val hasDates = state.availableDates.isNotEmpty()
    val selectedDateLabel = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().format(dateFormatter)
    }
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = setQuery,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(text = stringResource(id = R.string.search_placeholder))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
            TextButton(onClick = { setQuery("") }) {
                Text(text = stringResource(id = R.string.search_cancel))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (query.isBlank()) {
                val items = listOf(
                    R.string.search_chip_group_members,
                    R.string.search_chip_date,
                    R.string.search_chip_photos_videos,
                    R.string.search_chip_files,
                    R.string.search_chip_audio,
                    R.string.search_chip_links
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items) { resId ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .let { base ->
                                    if (resId == R.string.search_chip_date && !hasDates) {
                                        base
                                    } else {
                                        base.clickable {
                                    when (resId) {
                                        R.string.search_chip_group_members -> onGroupMembersClick()
                                        R.string.search_chip_date -> if (hasDates) showDatePicker = true
                                        R.string.search_chip_photos_videos -> onFilterSelected(MessageSearchFilter.Photos)
                                        R.string.search_chip_files -> onFilterSelected(MessageSearchFilter.Files)
                                        R.string.search_chip_audio -> onFilterSelected(MessageSearchFilter.Audio)
                                        R.string.search_chip_links -> onFilterSelected(MessageSearchFilter.Links)
                                        else -> Unit
                                    }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = resId),
                                color = accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.chat_placeholder_no_messages),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(searchResults, key = { _, msg -> msg.id.ifBlank { msg.timestamp.toString() } }) { index, message ->
                            val isSelf = message.senderId == state.currentUserId
                            val timeLabel = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(message.timestamp),
                                zoneId
                            ).format(dateTimeFormatter)
                            val displayName = message.senderName.ifBlank { message.senderId }
                            val content = messageContentLabel(message)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (!isSelf) {
                                    SearchResultAvatar(name = displayName, avatarUrl = message.senderAvatarUrl)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(
                                    modifier = Modifier.weight(1f, fill = false),
                                    horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = timeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelf) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    SearchResultAvatar(name = displayName, avatarUrl = message.senderAvatarUrl)
                                }
                            }

                            if (index < searchResults.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedDateLabel?.let { label ->
            Text(
                text = stringResource(R.string.search_chip_date) + ": $label",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        if (!hasDates) {
            Text(
                text = stringResource(R.string.chat_search_no_dates),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millis)
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun messageContentLabel(message: Message): String {
    if (message.content.isNotBlank()) return message.content
    return when (message.type) {
        MessageType.IMAGE -> "[Image]"
        MessageType.VIDEO -> "[Video]"
        MessageType.VOICE -> "[Audio]"
        MessageType.FILE -> "[File]"
        else -> "[Message]"
    }
}

@Composable
private fun SearchResultAvatar(name: String, avatarUrl: String) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    val size = 40.dp
    if (avatarUrl.isNotBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
