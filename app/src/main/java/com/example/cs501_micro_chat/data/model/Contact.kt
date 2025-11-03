/**
 * Contact.kt
 *
 * 联系人关系数据模型 - 表示用户之间的好友关系
 * Contact Relationship Data Model - Represents friendship between users
 *
 * Firebase 路径: /users/{userId}/contacts/{contactId}
 *
 * @property userId 当前用户 ID
 * @property contactId 联系人用户 ID
 * @property contactName 联系人名称
 * @property contactAvatarUrl 联系人头像 URL
 * @property alias 备注名
 * @property tags 标签列表
 * @property isFavorite 是否为特别关注
 * @property isBlocked 是否已屏蔽
 * @property addedAt 添加时间戳
 * @property conversationId 对应的会话 ID
 */
package com.example.cs501_micro_chat.data.model

data class Contact(
    val userId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val contactAvatarUrl: String = "",
    val alias: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val conversationId: String = ""
) {
    // 转换为 Firebase Map 格式
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "contactId" to contactId,
        "contactName" to contactName,
        "contactAvatarUrl" to contactAvatarUrl,
        "alias" to alias,
        "tags" to tags,
        "isFavorite" to isFavorite,
        "isBlocked" to isBlocked,
        "addedAt" to addedAt,
        "conversationId" to conversationId
    )

    // 获取显示名称（备注名优先）
    fun getDisplayName(): String = alias.ifEmpty { contactName }
}

