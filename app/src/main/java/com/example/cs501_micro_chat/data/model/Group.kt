/**
 * Group.kt
 *
 * 群组数据模型 - 表示群聊的详细信息
 * Group Data Model - Represents detailed group chat information
 *
 * Firebase 路径: /groups/{groupId}
 *
 * @property id 群组唯一标识符
 * @property name 群组名称
 * @property description 群组描述
 * @property avatarUrl 群组头像 URL
 * @property ownerId 群主用户 ID
 * @property adminIds 管理员用户 ID 列表
 * @property memberIds 成员用户 ID 列表
 * @property conversationId 对应的会话 ID
 * @property maxMembers 最大成员数
 * @property createdAt 创建时间戳
 * @property settings 群组设置
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
    val conversationId: String = "",  // 对应的会话ID
    val maxMembers: Int = 500,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: GroupSettings = GroupSettings()
) {
    // 转换为 Firebase Map 格式
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

    // 获取成员总数
    fun getMemberCount(): Int = memberIds.size

    // 检查是否为群主
    fun isOwner(userId: String): Boolean = ownerId == userId

    // 检查是否为管理员
    fun isAdmin(userId: String): Boolean = adminIds.contains(userId)

    // 检查是否为成员
    fun isMember(userId: String): Boolean = memberIds.contains(userId)
}

data class GroupSettings(
    val allowMemberInvite: Boolean = true,        // 是否允许普通成员邀请
    val requireAdminApproval: Boolean = false,    // 加入是否需要管理员审批
    val muteAll: Boolean = false,                 // 是否全员禁言
    val showMemberList: Boolean = true,           // 是否显示成员列表
    val allowMemberNickname: Boolean = true       // 是否允许成员设置群昵称
) {
    fun toMap(): Map<String, Any> = mapOf(
        "allowMemberInvite" to allowMemberInvite,
        "requireAdminApproval" to requireAdminApproval,
        "muteAll" to muteAll,
        "showMemberList" to showMemberList,
        "allowMemberNickname" to allowMemberNickname
    )
}

