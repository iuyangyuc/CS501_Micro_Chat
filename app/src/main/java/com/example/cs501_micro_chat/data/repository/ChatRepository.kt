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

import android.util.Log
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

        val isBlocked = firebaseDataSource.isConversationParticipantBlocked(conversationId, userId).getOrElse { false }
        if (isBlocked) {
            return Result.failure(IllegalStateException("Conversation blocked for current user"))
        }

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
            readBy = listOf(userId),
            status = MessageStatus.SENT
        )

        return runCatching {
            Log.d("ChatRepository", "sendMessage convo=$conversationId type=${type.name} media=${mediaUrl.isNotBlank()}")
            firebaseDataSource.sendMessage(message).getOrThrow()
        }.onFailure {
            Log.e("ChatRepository", "sendMessage failed convo=$conversationId type=${type.name}", it)
        }
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

    /**
     * 记录当前用户清空聊天的时间戳（单向清除）
     */
    suspend fun clearConversationForCurrentUser(conversationId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.clearConversationForUser(conversationId, userId, System.currentTimeMillis())
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
     * 发送好友请求
     * 1. **检查双方的旧 contacts 中是否有旧的 conversationId**
     * 2. 如果找到旧的 conversationId，在新的 contact 中保留它
     * 3. 在目标用户的 contacts 中添加当前用户，设置 isNew = true（待确认）
     * 4. 在当前用户的 contacts 中添加目标用户，设置 isPending = true（已发送，等待接受）
     *
     * 注意：即使保留了 conversationId，因为 isNew 或 isPending 为 true，会话也不会显示在聊天列表中
     */
    suspend fun sendFriendRequest(targetUserId: String, alias: String = ""): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        try {
            Log.d("ChatRepository", "🔄 sendFriendRequest: currentUser=$userId, targetUser=$targetUserId")

            // 获取当前用户信息
            val currentUserResult = firebaseDataSource.getUser(userId)
            val currentUser = currentUserResult.getOrNull()
            if (currentUser == null) {
                Log.e("ChatRepository", "❌ Current user not found: $userId")
                return Result.failure(Exception("Current user not found"))
            }
            Log.d("ChatRepository", "✅ Current user found: ${currentUser.username} (${currentUser.id})")

            // 获取目标用户信息
            val targetUserResult = firebaseDataSource.getUser(targetUserId)
            val targetUser = targetUserResult.getOrNull()
            if (targetUser == null) {
                Log.e("ChatRepository", "❌ Target user not found: $targetUserId")
                return Result.failure(Exception("Target user not found"))
            }
            Log.d("ChatRepository", "✅ Target user found: ${targetUser.username} (${targetUser.id})")

            // **关键步骤：检查双方的旧 contacts 中是否有旧的 conversationId**
            Log.d("ChatRepository", "  🔍 Checking for old conversationId in both users' contacts...")

            // 检查发送方（当前用户）的旧 contact
            val senderOldContactResult = firebaseDataSource.getContact(userId, targetUserId)
            val senderOldConversationId = senderOldContactResult.getOrNull()?.conversationId?.takeIf { it.isNotBlank() }
            Log.d("ChatRepository", "    📋 Sender's old conversationId: ${senderOldConversationId ?: "null"}")

            // 检查接收方（目标用户）的旧 contact
            val receiverOldContactResult = firebaseDataSource.getContact(targetUserId, userId)
            val receiverOldConversationId = receiverOldContactResult.getOrNull()?.conversationId?.takeIf { it.isNotBlank() }
            Log.d("ChatRepository", "    📋 Receiver's old conversationId: ${receiverOldConversationId ?: "null"}")

            // 优先使用任何一方保留的旧 conversationId
            val existingConversationId = senderOldConversationId ?: receiverOldConversationId ?: ""

            if (existingConversationId.isNotBlank()) {
                Log.d("ChatRepository", "  ♻️ Found old conversationId: $existingConversationId, will preserve it")
            } else {
                Log.d("ChatRepository", "  🆕 No old conversationId found, will create new one on acceptance")
            }

            // 在目标用户的 contacts 中创建一个待确认的联系人（当前用户）
            // **保留旧的 conversationId（如果存在）**
            val receiverContact = Contact(
                userId = targetUserId, // 目标用户的ID
                contactId = userId, // 当前用户的ID
                contactName = currentUser.username,
                contactAvatarUrl = currentUser.avatarUrl,
                alias = "",
                type = "PRIVATE",
                isNew = true, // 标记为待确认（接收者看到的）
                isPending = false,
                conversationId = existingConversationId // **保留旧的 conversationId**
            )
            Log.d("ChatRepository", "📝 Creating receiver contact: user=${receiverContact.userId}, contact=${receiverContact.contactId}, isNew=${receiverContact.isNew}, isPending=${receiverContact.isPending}, conversationId=$existingConversationId")

            val receiverResult = firebaseDataSource.addContact(receiverContact)
            if (receiverResult.isFailure) {
                Log.e("ChatRepository", "❌ Failed to add receiver contact", receiverResult.exceptionOrNull())
                return Result.failure(receiverResult.exceptionOrNull() ?: Exception("Failed to add receiver contact"))
            }
            Log.d("ChatRepository", "✅ Receiver contact added successfully")

            // 在当前用户的 contacts 中创建一个已发送的联系人（目标用户）
            // **保留旧的 conversationId（如果存在）**
            val senderContact = Contact(
                userId = userId, // 当前用户的ID
                contactId = targetUserId, // 目标用户的ID
                contactName = targetUser.username,
                contactAvatarUrl = targetUser.avatarUrl,
                alias = alias,
                type = "PRIVATE",
                isNew = false,
                isPending = true, // 标记为已发送等待接受（发送者看到的）
                conversationId = existingConversationId // **保留旧的 conversationId**
            )
            Log.d("ChatRepository", "📝 Creating sender contact: user=${senderContact.userId}, contact=${senderContact.contactId}, isNew=${senderContact.isNew}, isPending=${senderContact.isPending}, conversationId=$existingConversationId")

            val senderResult = firebaseDataSource.addContact(senderContact)
            if (senderResult.isFailure) {
                Log.e("ChatRepository", "❌ Failed to add sender contact", senderResult.exceptionOrNull())
                return Result.failure(senderResult.exceptionOrNull() ?: Exception("Failed to add sender contact"))
            }
            Log.d("ChatRepository", "✅ Sender contact added successfully")

            Log.d("ChatRepository", "🎉 Friend request sent successfully with conversationId: $existingConversationId")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "❌ Exception in sendFriendRequest", e)
            return Result.failure(e)
        }
    }

    /**
     * 接受好友请求
     * 1. 从 contacts 中获取 conversationId（在发送请求时已经保留了旧的 conversationId）
     * 2. 如果有 conversationId，检查会话是否存在，不存在则重建；如果没有则创建新的
     * 3. 将当前用户 contacts 中的 isNew 设置为 false
     * 4. 将请求者 contacts 中的 isPending 设置为 false
     */
    suspend fun acceptFriendRequest(requesterId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        try {
            Log.d("ChatRepository", "🤝 Accepting friend request from: $requesterId")

            // 获取请求者信息
            val requesterResult = firebaseDataSource.getUser(requesterId)
            val requester = requesterResult.getOrNull()
                ?: return Result.failure(Exception("Requester not found"))

            // 获取当前用户信息
            val currentUserResult = firebaseDataSource.getUser(userId)
            val currentUser = currentUserResult.getOrNull()
                ?: return Result.failure(Exception("Current user not found"))

            // 从当前的 contacts 中获取 conversationId（在 sendFriendRequest 时已经保留）
            Log.d("ChatRepository", "  🔍 Checking conversationId from contacts...")

            val receiverContactResult = firebaseDataSource.getContact(userId, requesterId)
            val receiverConversationId = receiverContactResult.getOrNull()?.conversationId
            Log.d("ChatRepository", "    📋 Receiver's conversationId: ${receiverConversationId ?: "null"}")

            val requesterContactResult = firebaseDataSource.getContact(requesterId, userId)
            val requesterConversationId = requesterContactResult.getOrNull()?.conversationId
            Log.d("ChatRepository", "    📋 Requester's conversationId: ${requesterConversationId ?: "null"}")

            // 使用已保留的 conversationId
            val existingConversationId = receiverConversationId?.takeIf { it.isNotBlank() }
                ?: requesterConversationId?.takeIf { it.isNotBlank() }

            val conversationId: String

            if (!existingConversationId.isNullOrBlank()) {
                // 使用已保留的 conversationId
                Log.d("ChatRepository", "  ♻️ Using preserved conversationId: $existingConversationId")
                conversationId = existingConversationId

                // 检查会话是否仍然存在
                val existingConversation = firebaseDataSource.getConversation(conversationId).getOrNull()
                if (existingConversation == null) {
                    Log.d("ChatRepository", "  ⚠️ Conversation doesn't exist, recreating it with same ID")
                    val conversation = Conversation(
                        id = conversationId,
                        type = ConversationType.PRIVATE,
                        name = requester.username,
                        avatarUrl = requester.avatarUrl,
                        participants = listOf(userId, requesterId),
                        createdBy = userId,
                        unreadCounts = mapOf(userId to 0, requesterId to 0)
                    )
                    firebaseDataSource.createConversation(conversation).getOrThrow()
                } else {
                    Log.d("ChatRepository", "  ✅ Conversation already exists, no need to recreate")
                }
            } else {
                // 第一次加好友，创建新的会话
                Log.d("ChatRepository", "  🆕 No conversationId found, creating new conversation")
                conversationId = firebaseDataSource.generateConversationId()
                val conversation = Conversation(
                    id = conversationId,
                    type = ConversationType.PRIVATE,
                    name = requester.username,
                    avatarUrl = requester.avatarUrl,
                    participants = listOf(userId, requesterId),
                    createdBy = userId,
                    unreadCounts = mapOf(userId to 0, requesterId to 0)
                )
                firebaseDataSource.createConversation(conversation).getOrThrow()
            }

            // 更新当前用户的 contact（将 isNew 设置为 false，确保有 conversationId）
            Log.d("ChatRepository", "  📝 Updating current user's contact")
            val myContact = Contact(
                userId = userId,
                contactId = requesterId,
                contactName = requester.username,
                contactAvatarUrl = requester.avatarUrl,
                type = "PRIVATE",
                isNew = false, // 从 true 变为 false
                isPending = false,
                conversationId = conversationId
            )
            firebaseDataSource.addContact(myContact).getOrThrow()

            // 更新请求者的 contact（将 isPending 设置为 false，确保有 conversationId）
            Log.d("ChatRepository", "  📝 Updating requester's contact")
            val theirContact = Contact(
                userId = requesterId,
                contactId = userId,
                contactName = currentUser.username,
                contactAvatarUrl = currentUser.avatarUrl,
                type = "PRIVATE",
                isNew = false,
                isPending = false, // 从 true 变为 false
                conversationId = conversationId
            )
            firebaseDataSource.addContact(theirContact).getOrThrow()

            Log.d("ChatRepository", "  🎉 Friend request accepted successfully with conversationId: $conversationId")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "  ❌ Failed to accept friend request", e)
            return Result.failure(e)
        }
    }

    /**
     * 拒绝好友请求
     * 删除双方的 contact 记录，让双方回到互不相知的状态
     */
    suspend fun rejectFriendRequest(requesterId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        Log.d("ChatRepository", "🚫 Rejecting friend request from: $requesterId")

        try {
            // 删除接收者（当前用户）的 contact
            Log.d("ChatRepository", "  🗑️ Removing contact from receiver: /users/$userId/contacts/$requesterId")
            firebaseDataSource.removeContact(userId, requesterId).getOrThrow()
            Log.d("ChatRepository", "  ✅ Receiver contact removed")

            // 删除发送者的 contact
            Log.d("ChatRepository", "  🗑️ Removing contact from sender: /users/$requesterId/contacts/$userId")
            firebaseDataSource.removeContact(requesterId, userId).getOrThrow()
            Log.d("ChatRepository", "  ✅ Sender contact removed")

            Log.d("ChatRepository", "  🎉 Friend request rejected successfully, both contacts removed")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "  ❌ Failed to reject friend request", e)
            return Result.failure(e)
        }
    }

    /**
     * 添加联系人（旧版本，保留兼容性）
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
     * 获取单个联系人
     */
    suspend fun getContact(contactId: String): Result<Contact?> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getContact(userId, contactId)
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
        val contact = firebaseDataSource.getContact(userId, contactId).getOrNull()
        val conversationId = contact?.conversationId
        val deleteResult = firebaseDataSource.deleteContact(userId, contactId)
        deleteResult.onSuccess {
            if (!conversationId.isNullOrBlank()) {
                firebaseDataSource.setPinnedConversation(userId, conversationId, false)
                firebaseDataSource.setConversationParticipantBlocked(conversationId, contactId, true)
                firebaseDataSource.setConversationParticipantBlocked(conversationId, userId, false)
            }
        }
        return deleteResult
    }

    /**
     * 更新联系人备注
     */
    suspend fun updateContactAlias(contactId: String, alias: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.updateContactAlias(userId, contactId, alias)
    }

    /**
     * 监听置顶会话
     */
    fun observePinnedConversations(): Flow<Set<String>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observePinnedConversations(userId)
    }

    /**
     * 设置置顶状态
     */
    suspend fun setPinnedConversation(conversationId: String, pinned: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.setPinnedConversation(userId, conversationId, pinned)
    }

    /**
     * 检查会话是否置顶
     */
    suspend fun isConversationPinned(conversationId: String): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.isConversationPinned(userId, conversationId)
    }

    /**
     * 更新联系人置顶状态（用于同步联系人列表中的星标）
     */
    suspend fun updateContactFavorite(contactId: String, isFavorite: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.updateContactFavorite(userId, contactId, isFavorite)
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
