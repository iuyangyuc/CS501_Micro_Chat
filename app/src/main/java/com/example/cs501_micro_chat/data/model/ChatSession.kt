/**
 * Conversation.kt (renamed from ChatSession.kt)
 *
 * Conversation Data Model - Represents chat conversations (private or group)
 *
 * Firebase Path: /conversations/{conversationId}
 *
 * @property id Conversation unique identifier
 * @property type Conversation type (private/group)
 * @property name Conversation name (other user's name for private, group name for group)
 * @property avatarUrl Conversation avatar URL
 * @property participants List of participant user IDs
 * @property lastMessage Last message content
 * @property lastMessageTime Last message timestamp
 * @property unreadCount Unread message count (for specific user)
 * @property createdAt Conversation creation timestamp
 * @property createdBy Creator user ID
 * @property isActive Whether active (used to mark deleted conversations)
 */
package com.example.cs501_micro_chat.data.model

data class Conversation(
    val id: String = "",
    val type: ConversationType = ConversationType.PRIVATE,
    val name: String = "",
    val avatarUrl: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCounts: Map<String, Int> = emptyMap(), // userId -> unreadCount
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val isActive: Boolean = true,
    val blockedParticipants: Map<String, Boolean> = emptyMap(), // userId -> isBlocked
    val clearedAt: Map<String, Long> = emptyMap() // userId -> last cleared timestamp
) {
    // Convert to Firebase Map format
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.name,
        "name" to name,
        "avatarUrl" to avatarUrl,
        "participants" to participants,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "unreadCounts" to unreadCounts,
        "createdAt" to createdAt,
        "createdBy" to createdBy,
        "isActive" to isActive,
        "blockedParticipants" to blockedParticipants,
        "clearedAt" to clearedAt
    )

    // Get unread message count for specific user
    fun getUnreadCount(userId: String): Int = unreadCounts[userId] ?: 0
}

enum class ConversationType {
    PRIVATE,   // Private chat (1-on-1)
    GROUP      // Group chat (multiple users)
}

// Keep ChatSession alias for backward compatibility
typealias ChatSession = Conversation
