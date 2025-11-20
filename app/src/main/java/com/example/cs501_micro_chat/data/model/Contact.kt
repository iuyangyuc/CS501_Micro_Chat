/**
 * Contact.kt
 *
 * 联系人关系数据模型 - 表示用户之间的好友关系或群组关系
 * Contact Relationship Data Model - Represents friendship between users or group membership
 *
 * Firebase 路径: /users/{userId}/contacts/{contactId}
 *
 * 字段说明 / Field Descriptions:
 * - userId: 当前用户 ID
 * - contactId: 联系人用户 ID 或群组 ID
 * - contactName: 联系人名称或群组名称
 * - contactAvatarUrl: 联系人头像 URL 或群组头像 URL
 * - type: 类型（PRIVATE 或 GROUP）
 * - alias: 备注名
 * - tags: 标签列表
 * - isFavorite: 是否为特别关注
 * - isBlocked: 是否已屏蔽
 * - isNew: 是否为待确认的好友请求（接收者端：true = 等待确认）
 * - isPending: 是否为已发送但等待对方接受的请求（发送者端：true = 等待接受）
 * - addedAt: 添加时间戳
 * - conversationId: 对应的会话 ID
 */
package com.example.cs501_micro_chat.data.model

data class Contact(
    val userId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val contactAvatarUrl: String = "",
    val type: String = "PRIVATE", // "PRIVATE" 或 "GROUP"
    val alias: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isNew: Boolean = false, // 是否为待确认的好友请求（true 表示等待确认，false 表示已确认）
    val isPending: Boolean = false, // 是否为已发送但等待对方接受的请求（发送者端标记）
    val addedAt: Long = System.currentTimeMillis(),
    val conversationId: String = ""
) {
    // 转换为 Firebase Map 格式
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

    // 获取显示名称（备注名优先）
    fun getDisplayName(): String = alias.ifEmpty { contactName }

    // 判断是否为群组
    fun isGroup(): Boolean = type == "GROUP"
}

