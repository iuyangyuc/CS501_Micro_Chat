package com.example.cs501_micro_chat.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import com.example.cs501_micro_chat.data.model.Message
import java.time.ZoneId
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
data class ChatSearchUiState(
    val availableDates: Set<LocalDate> = emptySet(),
    val availableDayStartMillis: Set<Long> = emptySet(),
    val messages: List<Message> = emptyList(),
    val currentUserId: String = ""
)

@HiltViewModel
class ChatSearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.now(zoneId)
    private val currentUserId: String = chatRepository.currentUserIdOrNull().orEmpty()
    private var clearedAtMs: Long = 0L

    private val _uiState = MutableStateFlow(ChatSearchUiState(currentUserId = currentUserId))
    val uiState: StateFlow<ChatSearchUiState> = _uiState.asStateFlow()

    init {
        if (conversationId.isNotBlank()) {
            viewModelScope.launch {
                loadClearedAt()
                observeMessages()
            }
        }
    }

    private suspend fun observeMessages() {
        chatRepository.observeMessages(conversationId)
            .catch {
                _uiState.value = ChatSearchUiState()
            }
            .collect { messages ->
                val filtered = messages.filter { it.timestamp > clearedAtMs }
                val dates = filtered
                    .map { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
                    .filter { !it.isAfter(today) }
                val dateSet = dates.toSet()
                val dayStartMillis = dates
                    .map { it.atStartOfDay(zoneId).toInstant().toEpochMilli() }
                    .toSet()
                _uiState.value = ChatSearchUiState(
                    availableDates = dateSet,
                    availableDayStartMillis = dayStartMillis,
                    messages = filtered,
                    currentUserId = currentUserId
                )
            }
    }

    private suspend fun loadClearedAt() {
        if (currentUserId.isBlank()) return
        chatRepository.getConversation(conversationId).onSuccess { convo ->
            clearedAtMs = convo?.clearedAt?.get(currentUserId) ?: 0L
        }
    }
}
