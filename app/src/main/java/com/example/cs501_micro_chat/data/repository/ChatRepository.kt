/**
 * ChatRepository.kt
 *
 * 聊天数据仓库 - 处理聊天相关的业务逻辑
 * Chat Repository - Handles chat-related business logic
 *
 * 职责：
 * - 会话管理（创建、获取、更新、删除）
 * - 消息管理（发送、接收、标记已读）
 * - 群组管理（创建、更新、成员管理）
 * - 联系人管理
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.data.repository

import com.example.cs501_micro_chat.data.model.*
import com.example.cs501_micro_chat.data.remote.FirebaseDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val auth: FirebaseAuth
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== 会话相关 Conversation Operations ====================

    /**
     * 获取用户的所有会话
     */
    suspend fun getUserConversations(): Result<List<Conversation>> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getUserConversations(userId)
    }

    /**
     * 监听用户的会话列表
     */
    fun observeUserConversations(): Flow<List<Conversation>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observeUserConversations(userId)
    }

    /**
     * 创建或获取与某用户的私聊会话
     */
    suspend fun createOrGetPrivateConversation(otherUserId: String): Result<Conversation> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.createOrGetPrivateConversation(userId, otherUserId)
    }

    /**
     * 获取会话信息
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?> {
        return firebaseDataSource.getConversation(conversationId)
    }

    /**
     * 更新会话信息
     */
    suspend fun updateConversation(conversation: Conversation): Result<Unit> {
        return firebaseDataSource.updateConversation(conversation)
    }

    /**
     * 删除会话
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return firebaseDataSource.deleteConversation(conversationId)
    }

    // ==================== 消息相关 Message Operations ====================

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = ""
    ): Result<Message> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        // 获取当前用户信息
        val userResult = firebaseDataSource.getUser(userId)
        val user = userResult.getOrNull() ?: return Result.failure(Exception("Failed to get user info"))

        val message = Message(
            conversationId = conversationId,
            senderId = userId,
            senderName = user.username,
            senderAvatarUrl = user.avatarUrl,
            content = content,
            type = type,
            mediaUrl = mediaUrl,
            timestamp = System.currentTimeMillis(),
            readBy = listOf(userId) // 发送者默认已读
        )

        return firebaseDataSource.sendMessage(message)
    }

    /**
     * 获取会话的消息列表
     */
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>> {
        return firebaseDataSource.getMessages(conversationId, limit)
    }

    /**
     * 监听会话的新消息
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return firebaseDataSource.observeMessages(conversationId)
    }

    /**
     * 标记消息为已读
     */
    suspend fun markMessageAsRead(conversationId: String, messageId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.markMessageAsRead(conversationId, messageId, userId)
    }

    /**
     * 清空会话未读数
     */
    suspend fun clearUnreadCount(conversationId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.clearUnreadCount(conversationId, userId)
    }

    /**
     * 删除消息
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return firebaseDataSource.deleteMessage(conversationId, messageId)
    }

    // ==================== 群组相关 Group Operations ====================

    /**
     * 创建群组
     */
    suspend fun createGroup(
        name: String,
        description: String,
        avatarUrl: String,
        memberIds: List<String>
    ): Result<Group> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        val group = Group(
            name = name,
            description = description,
            avatarUrl = avatarUrl,
            ownerId = userId,
            adminIds = listOf(userId),
            memberIds = memberIds + userId // 确保创建者也在成员列表中
        )

        return firebaseDataSource.createGroup(group)
    }

    /**
     * 获取群组信息
     */
    suspend fun getGroup(groupId: String): Result<Group?> {
        return firebaseDataSource.getGroup(groupId)
    }

    /**
     * 更新群组信息
     */
    suspend fun updateGroup(group: Group): Result<Unit> {
        return firebaseDataSource.updateGroup(group)
    }

    /**
     * 添加群成员
     */
    suspend fun addGroupMembers(groupId: String, memberIds: List<String>): Result<Unit> {
        return firebaseDataSource.addGroupMembers(groupId, memberIds)
    }

    /**
     * 移除群成员
     */
    suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> {
        return firebaseDataSource.removeGroupMember(groupId, memberId)
    }

    /**
     * 退出群组
     */
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.removeGroupMember(groupId, userId)
    }

    /**
     * 转让群主
     */
    suspend fun transferGroupOwnership(groupId: String, newOwnerId: String): Result<Unit> {
        return firebaseDataSource.transferGroupOwnership(groupId, newOwnerId)
    }

    /**
     * 解散群组
     */
    suspend fun dismissGroup(groupId: String): Result<Unit> {
        return firebaseDataSource.dismissGroup(groupId)
    }

    // ==================== 联系人相关 Contact Operations ====================

    /**
     * 添加联系人
     */
    suspend fun addContact(contactId: String, alias: String = ""): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        // 获取联系人信息
        val contactUserResult = firebaseDataSource.getUser(contactId)
        val contactUser = contactUserResult.getOrNull()
            ?: return Result.failure(Exception("Contact user not found"))

        // 创建或获取私聊会话
        val conversationResult = createOrGetPrivateConversation(contactId)
        val conversation = conversationResult.getOrNull()
            ?: return Result.failure(Exception("Failed to create conversation"))

        val contact = Contact(
            userId = userId,
            contactId = contactId,
            contactName = contactUser.username,
            contactAvatarUrl = contactUser.avatarUrl,
            alias = alias,
            conversationId = conversation.id
        )

        return firebaseDataSource.addContact(contact)
    }

    /**
     * 获取联系人列表
     */
    suspend fun getContacts(): Result<List<Contact>> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getContacts(userId)
    }

    /**
     * 监听联系人列表
     */
    fun observeContacts(): Flow<List<Contact>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observeContacts(userId)
    }

    /**
     * 删除联系人
     */
    suspend fun deleteContact(contactId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.deleteContact(userId, contactId)
    }

    /**
     * 更新联系人备注
     */
    suspend fun updateContactAlias(contactId: String, alias: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.updateContactAlias(userId, contactId, alias)
    }

    /**
     * 搜索用户
     */
    suspend fun searchUsers(query: String): Result<List<User>> {
        return firebaseDataSource.searchUsers(query)
    }

    // ==================== 用户相关 User Operations ====================

    /**
     * 获取用户信息
     */
    suspend fun getUser(userId: String): Result<User?> {
        return firebaseDataSource.getUser(userId)
    }

    /**
     * 批量获取用户信息
     */
    suspend fun getUsers(userIds: List<String>): Result<Map<String, User>> {
        return firebaseDataSource.getUsers(userIds)
    }
}

