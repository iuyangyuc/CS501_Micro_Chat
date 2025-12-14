/**
 * Contact.kt
 *
 * Contact Relationship Data Model - Represents friendship between users or group membership
 *
 * Firebase Path: /users/{userId}/contacts/{contactId}
 *
 * Field Descriptions:
 * - userId: Current user ID
 * - contactId: Contact user ID or group ID
 * - contactName: Contact name or group name
 * - contactAvatarUrl: Contact avatar URL or group avatar URL
 * - type: Type (PRIVATE or GROUP)
 * - alias: Nickname/alias
 * - tags: Tag list
 * - isFavorite: Whether marked as favorite
 * - isBlocked: Whether blocked
 * - isNew: Whether pending friend request (receiver side: true = waiting for confirmation)
 * - isPending: Whether sent request waiting for acceptance (sender side: true = waiting for acceptance)
 * - addedAt: Added timestamp
 * - conversationId: Corresponding conversation ID
 */
package com.example.cs501_micro_chat.data.model

data class Contact(
    val userId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val contactAvatarUrl: String = "",
    val type: String = "PRIVATE", // "PRIVATE" or "GROUP"
    val alias: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isNew: Boolean = false, // Whether pending friend request (true = waiting for confirmation, false = confirmed)
    val isPending: Boolean = false, // Whether sent request waiting for acceptance (sender side marker)
    val addedAt: Long = System.currentTimeMillis(),
    val conversationId: String = ""
) {
    // Convert to Firebase Map format
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "contactId" to contactId,
        "contactName" to contactName,
        "contactAvatarUrl" to contactAvatarUrl,
        "type" to type,
        "alias" to alias,
        "tags" to tags,
        "isFavorite" to isFavorite,
        "isBlocked" to isBlocked,
        "isNew" to isNew,
        "isPending" to isPending,
        "addedAt" to addedAt,
        "conversationId" to conversationId
    )

    // Get display name (alias takes priority)
    fun getDisplayName(): String = alias.ifEmpty { contactName }

    // Check if is group
    fun isGroup(): Boolean = type == "GROUP"
}

