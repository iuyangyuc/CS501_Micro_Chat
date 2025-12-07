package com.example.cs501_micro_chat.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.chat.MessageBubble
import com.example.cs501_micro_chat.ui.chat.messageKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MemberMessageSearchScreen(
    onBack: () -> Unit,
    viewModel: MemberMessageSearchViewModel = hiltViewModel()
    // onBack kept for symmetry with other screens; actual back is handled by top bar
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val background = MaterialTheme.colorScheme.background
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val zoneId = ZoneId.systemDefault()
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.messages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.chat_placeholder_no_messages),
                        color = secondaryText
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.messages, key = { _, msg -> messageKey(msg) }) { index, message ->
                        val isSelfMessage = message.senderId == state.currentUserId
        // Keep original avatar behavior; no extra duplication
                        val bubbleMessage = if (!isSelfMessage && message.senderAvatarUrl.isBlank() && state.memberAvatarUrl.isNotBlank()) {
                            message.copy(senderAvatarUrl = state.memberAvatarUrl)
                        } else {
                            message
                        }
                        val displayName = state.memberName.ifBlank {
                            message.senderName.takeIf { it.isNotBlank() } ?: state.memberId
                        }
                        val timeLabel = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(message.timestamp),
                            zoneId
                        ).format(dateTimeFormatter)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isSelfMessage) Alignment.End else Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            MessageBubble(
                                message = bubbleMessage,
                                isSelf = isSelfMessage,
                                timeLabel = timeLabel
                            )
                        }
                        if (index < state.messages.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
