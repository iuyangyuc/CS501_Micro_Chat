/**
 * Conversation.kt (renamed from ChatSession.kt)
 *
 * 会话数据模型 - 表示聊天会话（私聊或群聊）
 * Conversation Data Model - Represents chat conversations (private or group)
 *
 * Firebase 路径: /conversations/{conversationId}
 *
 * @property id 会话唯一标识符
 * @property type 会话类型（私聊/群聊）
 * @property name 会话名称（私聊为对方用户名，群聊为群名）
 * @property avatarUrl 会话头像 URL
 * @property participants 参与者用户 ID 列表
 * @property lastMessage 最后一条消息
 * @property lastMessageTime 最后消息时间戳
 * @property unreadCount 未读消息数（针对特定用户）
 * @property createdAt 会话创建时间戳
 * @property createdBy 创建者用户 ID
 * @property isActive 是否活跃（用于标记已删除的会话）
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
    val blockedParticipants: Map<String, Boolean> = emptyMap() // userId -> isBlocked
) {
    // 转换为 Firebase Map 格式
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
        "blockedParticipants" to blockedParticipants
    )

    // 获取特定用户的未读消息数
    fun getUnreadCount(userId: String): Int = unreadCounts[userId] ?: 0
}

enum class ConversationType {
    PRIVATE,   // 私聊（1对1）
    GROUP      // 群聊（多人）
}

// 保留 ChatSession 别名以兼容旧代码
typealias ChatSession = Conversation
