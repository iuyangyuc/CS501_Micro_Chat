/**
 * Message.kt
 *
 * 消息数据模型 - 表示聊天消息
 * Message Data Model - Represents chat messages
 *
 * Firebase 路径: /conversations/{conversationId}/messages/{messageId}
 *
 * @property id 消息唯一标识符
 * @property conversationId 所属会话 ID
 * @property senderId 发送者用户 ID
 * @property senderName 发送者用户名
 * @property senderAvatarUrl 发送者头像 URL
 * @property content 消息内容（文本/文件名等）
 * @property type 消息类型（文本/图片/语音/视频等）
 * @property mediaUrl 媒体文件 URL（图片、语音、视频等）
 * @property timestamp 发送时间戳
 * @property readBy 已读用户 ID 列表
 * @property isDeleted 是否已删除
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
    // 转换为 Firebase Map 格式
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
    TEXT,      // 文本消息
    IMAGE,     // 图片消息
    VOICE,     // 语音消息
    VIDEO,     // 视频消息
    FILE,      // 文件消息
    SYSTEM     // 系统消息（如：xxx 加入群聊）
}

enum class MessageStatus {
    SENT,
    FAILED
}
