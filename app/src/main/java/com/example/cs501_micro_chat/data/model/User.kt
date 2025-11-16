/**
 * User.kt
 *
 * 用户数据模型 - 表示应用中的用户信息
 * User Data Model - Represents user information in the application
 *
 * Firebase 路径: /users/{userId}
 *
 * @property id 用户唯一标识符 (Firebase UID)
 * @property username 用户名
 * @property email 邮箱地址
 * @property avatarUrl 头像 URL
 * @property status 在线状态 (online/offline/away)
 * @property statusMessage 个性签名
 * @property createdAt 账户创建时间戳
 * @property lastSeenAt 最后在线时间戳
 */
package com.example.cs501_micro_chat.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    val statusMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
) {
    // 转换为 Firebase Map 格式
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "username" to username,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "status" to status.name,
        "statusMessage" to statusMessage,
        "createdAt" to createdAt,
        "lastSeenAt" to lastSeenAt
    )
}

enum class UserStatus {
    ONLINE,    // 在线
    OFFLINE,   // 离线
    AWAY       // 离开
}
