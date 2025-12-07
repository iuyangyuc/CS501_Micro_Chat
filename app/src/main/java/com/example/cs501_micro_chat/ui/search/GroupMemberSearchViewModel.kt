package com.example.cs501_micro_chat.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_micro_chat.data.model.Conversation
import com.example.cs501_micro_chat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupMemberItem(
    val id: String,
    val name: String,
    val avatarUrl: String
)

data class GroupMemberSearchUiState(
    val query: String = "",
    val members: List<GroupMemberItem> = emptyList(),
    val filteredMembers: List<GroupMemberItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GroupMemberSearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }

    private val _uiState = MutableStateFlow(GroupMemberSearchUiState())
    val uiState: StateFlow<GroupMemberSearchUiState> = _uiState.asStateFlow()

    init {
        if (conversationId.isNotBlank()) {
            loadMembers()
        }
    }

    fun onQueryChange(query: String) {
        val members = _uiState.value.members
        _uiState.update {
            it.copy(
                query = query,
                filteredMembers = filterAndSort(members, query)
            )
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            chatRepository.getConversation(conversationId)
                .onSuccess { conversation: Conversation? ->
                    val participants = conversation?.participants.orEmpty()
                    if (participants.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, members = emptyList(), filteredMembers = emptyList()) }
                        return@onSuccess
                    }

                    // Load contacts once to find aliases/display names for participants
                    val contactMap = chatRepository.getContacts()
                        .getOrNull()
                        ?.associateBy { it.contactId }
                        .orEmpty()

                    chatRepository.getUsers(participants).onSuccess { userMap ->
                        val members = participants.map { id ->
                            val user = userMap[id]
                            val contact = contactMap[id]
                            val contactDisplayName = contact?.alias?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: contact?.contactName?.trim().takeIf { !it.isNullOrEmpty() }
                            val name = contactDisplayName
                                ?: when {
                                    !user?.username.isNullOrBlank() -> user!!.username
                                    !user?.email.isNullOrBlank() -> user!!.email.substringBefore("@")
                                    else -> id
                                }
                            val avatarUrl = listOf(
                                contact?.contactAvatarUrl,
                                user?.avatarUrl
                            ).firstOrNull { !it.isNullOrBlank() }.orEmpty()

                            GroupMemberItem(
                                id = id,
                                name = name,
                                avatarUrl = avatarUrl
                            )
                        }
                        val sortedMembers = sortMembers(members)
                        val filtered = filterAndSort(sortedMembers, _uiState.value.query)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                members = sortedMembers,
                                filteredMembers = filtered
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun sortMembers(members: List<GroupMemberItem>): List<GroupMemberItem> {
        return members.sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    private fun filterAndSort(members: List<GroupMemberItem>, query: String): List<GroupMemberItem> {
        val trimmed = query.trim()
        val filtered = if (trimmed.isBlank()) {
            members
        } else {
            members.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
        return sortMembers(filtered)
    }

}
