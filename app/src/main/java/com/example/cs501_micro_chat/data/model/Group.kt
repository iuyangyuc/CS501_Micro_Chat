/**
 * Group.kt
 *
 * Group Data Model - Represents detailed group chat information
 *
 * Firebase Path: /groups/{groupId}
 *
 * @property id Group unique identifier
 * @property name Group name
 * @property description Group description
 * @property avatarUrl Group avatar URL
 * @property ownerId Group owner user ID
 * @property adminIds Admin user ID list
 * @property memberIds Member user ID list
 * @property conversationId Corresponding conversation ID
 * @property maxMembers Maximum number of members
 * @property createdAt Creation timestamp
 * @property settings Group settings
 */
package com.example.cs501_micro_chat.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val ownerId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val conversationId: String = "",  // Corresponding conversation ID
    val maxMembers: Int = 500,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: GroupSettings = GroupSettings()
) {
    // Convert to Firebase Map format
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "avatarUrl" to avatarUrl,
        "ownerId" to ownerId,
        "adminIds" to adminIds,
        "memberIds" to memberIds,
        "conversationId" to conversationId,
        "maxMembers" to maxMembers,
        "createdAt" to createdAt,
        "settings" to settings.toMap()
    )

    // Get total member count
    fun getMemberCount(): Int = memberIds.size

    // Check if user is owner
    fun isOwner(userId: String): Boolean = ownerId == userId

    // Check if user is admin
    fun isAdmin(userId: String): Boolean = adminIds.contains(userId)

    // Check if user is member
    fun isMember(userId: String): Boolean = memberIds.contains(userId)
}

data class GroupSettings(
    val allowMemberInvite: Boolean = true,        // Whether to allow regular members to invite
    val requireAdminApproval: Boolean = false,    // Whether admin approval is required to join
    val muteAll: Boolean = false,                 // Whether to mute all members
    val showMemberList: Boolean = true,           // Whether to show member list
    val allowMemberNickname: Boolean = true       // Whether to allow members to set group nickname
) {
    fun toMap(): Map<String, Any> = mapOf(
        "allowMemberInvite" to allowMemberInvite,
        "requireAdminApproval" to requireAdminApproval,
        "muteAll" to muteAll,
        "showMemberList" to showMemberList,
        "allowMemberNickname" to allowMemberNickname
    )
}

