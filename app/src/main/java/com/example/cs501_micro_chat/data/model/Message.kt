/**
 * Message.kt
 *
 * Message Data Model - Represents chat messages
 *
 * Firebase Path: /conversations/{conversationId}/messages/{messageId}
 *
 * @property id Message unique identifier
 * @property conversationId Conversation ID this message belongs to
 * @property senderId Sender user ID
 * @property senderName Sender username
 * @property senderAvatarUrl Sender avatar URL
 * @property content Message content (text/filename etc.)
 * @property type Message type (text/image/voice/video etc.)
 * @property mediaUrl Media file URL (image, voice, video etc.)
 * @property timestamp Send timestamp
 * @property readBy List of user IDs who have read
 * @property isDeleted Whether deleted
 */
package com.example.cs501_micro_chat.data.model

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val readBy: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
) {
    // Convert to Firebase Map format
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatarUrl,
        "content" to content,
        "type" to type.name,
        "mediaUrl" to mediaUrl,
        "timestamp" to timestamp,
        "readBy" to readBy,
        "isDeleted" to isDeleted,
        "status" to status.name
    )
}

enum class MessageType {
    TEXT,      // Text message
    IMAGE,     // Image message
    VOICE,     // Voice message
    VIDEO,     // Video message
    FILE,      // File message
    SYSTEM     // System message (e.g.: xxx joined the group)
}

enum class MessageStatus {
    SENT,
    FAILED
}
