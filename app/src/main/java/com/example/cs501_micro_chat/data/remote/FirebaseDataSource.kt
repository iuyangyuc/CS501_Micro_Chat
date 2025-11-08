/**
 * FirebaseDataSource.kt
 * 
 * Firebase 数据源 - 处理所有与 Firebase Firestore 的交互
 * Firebase Data Source - Handles all interactions with Firebase Firestore
 * 
 * Firebase 数据库结构 / Database Structure:
 * 
 * /users/{userId}
 *   - 用户基本信息
 *   /contacts/{contactId} - 用户的联系人列表
 * 
 * /conversations/{conversationId}
 *   - 会话基本信息（私聊或群聊）
 *   /messages/{messageId} - 会话中的消息
 * 
 * /groups/{groupId}
 *   - 群组详细信息
 * 
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.data.remote

import android.util.Log
import com.example.cs501_micro_chat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")
    private val conversationsCollection = firestore.collection("conversations")
    private val groupsCollection = firestore.collection("groups")

    companion object {
        private const val TAG = "FirebaseDataSource"
    }

    // ==================== 用户相关 User Operations ====================
    
    /**
     * 创建或更新用户信息
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit> = runCatching {
        usersCollection.document(user.id).set(user.toMap()).await()
    }

    /**
     * 获取用户信息
     */
    suspend fun getUser(userId: String): Result<User?> = runCatching {
        val snapshot = usersCollection.document(userId).get().await()
        snapshot.toObject(User::class.java)
    }

    /**
     * 监听用户在线状态
     */
    fun observeUserStatus(userId: String): Flow<UserStatus> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                user?.let { trySend(it.status) }
            }
        awaitClose { listener.remove() }
    }

    /**
     * 更新用户在线状态
     */
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> = runCatching {
        usersCollection.document(userId).update(
            mapOf(
                "status" to status.name,
                "lastSeenAt" to System.currentTimeMillis()
            )
        ).await()
    }

    /**
     * 搜索用户（通过用户名或邮箱）
     */
    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val byUsername = usersCollection
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .await()
            .toObjects(User::class.java)

        val byEmail = usersCollection
            .whereEqualTo("email", query)
            .get()
            .await()
            .toObjects(User::class.java)

        (byUsername + byEmail).distinctBy { it.id }
    }

    // ==================== 联系人相关 Contact Operations ====================

    /**
     * 添加联系人
     */
    suspend fun addContact(contact: Contact): Result<Unit> = runCatching {
        usersCollection
            .document(contact.userId)
            .collection("contacts")
            .document(contact.contactId)
            .set(contact.toMap())
            .await()
    }

    /**
     * 获取联系人列表
     */
    suspend fun getContacts(userId: String): Result<List<Contact>> = runCatching {
        usersCollection
            .document(userId)
            .collection("contacts")
            .whereEqualTo("isBlocked", false)
            .get()
            .await()
            .toObjects(Contact::class.java)
    }

    /**
     * 监听联系人列表变化
     */
    fun observeContacts(userId: String): Flow<List<Contact>> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .collection("contacts")
            .whereEqualTo("isBlocked", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val contacts = snapshot?.toObjects(Contact::class.java) ?: emptyList()
                trySend(contacts)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 删除联系人
     */
    suspend fun deleteContact(userId: String, contactId: String): Result<Unit> = runCatching {
        usersCollection
            .document(userId)
            .collection("contacts")
            .document(contactId)
            .delete()
            .await()
    }

    /**
     * 更新联系人备注
     */
    suspend fun updateContactAlias(userId: String, contactId: String, alias: String): Result<Unit> = runCatching {
        usersCollection
            .document(userId)
            .collection("contacts")
            .document(contactId)
            .update("alias", alias)
            .await()
    }

    // ==================== 会话相关 Conversation Operations ====================

    /**
     * 创建或获取私聊会话
     */
    suspend fun createOrGetPrivateConversation(currentUserId: String, otherUserId: String): Result<Conversation> = runCatching {
        // 查找已存在的会话
        val existingConversations = conversationsCollection
            .whereEqualTo("type", ConversationType.PRIVATE.name)
            .whereArrayContains("participants", currentUserId)
            .get()
            .await()
            .toObjects(Conversation::class.java)
            .filter { it.participants.contains(otherUserId) }

        if (existingConversations.isNotEmpty()) {
            existingConversations.first()
        } else {
            // 创建新会话
            val otherUser = getUser(otherUserId).getOrNull()
            val conversationId = conversationsCollection.document().id
            val conversation = Conversation(
                id = conversationId,
                type = ConversationType.PRIVATE,
                name = otherUser?.username ?: "",
                avatarUrl = otherUser?.avatarUrl ?: "",
                participants = listOf(currentUserId, otherUserId),
                createdBy = currentUserId,
                unreadCounts = mapOf(currentUserId to 0, otherUserId to 0)
            )
            conversationsCollection.document(conversationId).set(conversation.toMap()).await()
            conversation
        }
    }

    /**
     * 创建群聊会话
     */
    suspend fun createGroupConversation(
        name: String,
        avatarUrl: String,
        participants: List<String>,
        createdBy: String
    ): Result<Conversation> = runCatching {
        val conversationId = conversationsCollection.document().id
        val conversation = Conversation(
            id = conversationId,
            type = ConversationType.GROUP,
            name = name,
            avatarUrl = avatarUrl,
            participants = participants,
            createdBy = createdBy,
            unreadCounts = participants.associateWith { 0 }
        )
        conversationsCollection.document(conversationId).set(conversation.toMap()).await()
        conversation
    }

    /**
     * 获取用户的所有会话列表
     */
    suspend fun getUserConversations(userId: String): Result<List<Conversation>> = runCatching {
        conversationsCollection
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Conversation::class.java)
    }

    /**
     * 监听用户的会话列表
     *
     * 注意：移除了 orderBy 以避免需要复合索引
     * 排序改为在客户端（ViewModel）进行
     */
    fun observeUserConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsCollection
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            // 移除 .orderBy() 以避免需要复合索引
            // 排序将在 HomeViewModel 中进行
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing conversations", error)
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.toObjects(Conversation::class.java) ?: emptyList()
                Log.d(TAG, "Received ${conversations.size} conversations from Firestore")
                // 在客户端按时间排序
                val sortedConversations = conversations.sortedByDescending { it.lastMessageTime }
                trySend(sortedConversations)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 获取单个会话信息
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?> = runCatching {
        conversationsCollection
            .document(conversationId)
            .get()
            .await()
            .toObject(Conversation::class.java)
    }

    /**
     * 更新会话信息
     */
    suspend fun updateConversation(conversation: Conversation): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversation.id)
            .set(conversation.toMap())
            .await()
    }

    /**
     * 删除会话（标记为不活跃）
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversationId)
            .update("isActive", false)
            .await()
    }

    // ==================== 消息相关 Message Operations ====================

    /**
     * 发送消息
     */
    suspend fun sendMessage(message: Message): Result<Message> = runCatching {
        val messageId = conversationsCollection
            .document(message.conversationId)
            .collection("messages")
            .document().id

        val messageWithId = message.copy(id = messageId)

        // 保存消息
        conversationsCollection
            .document(message.conversationId)
            .collection("messages")
            .document(messageId)
            .set(messageWithId.toMap())
            .await()

        // 更新会话的最后消息信息
        val conversation = getConversation(message.conversationId).getOrNull()
        conversation?.let {
            val updatedUnreadCounts = it.unreadCounts.toMutableMap()
            it.participants.forEach { participantId ->
                if (participantId != message.senderId) {
                    updatedUnreadCounts[participantId] = (updatedUnreadCounts[participantId] ?: 0) + 1
                }
            }

            conversationsCollection
                .document(message.conversationId)
                .update(
                    mapOf(
                        "lastMessage" to message.content,
                        "lastMessageTime" to message.timestamp,
                        "unreadCounts" to updatedUnreadCounts
                    )
                )
                .await()
        }

        messageWithId
    }

    /**
     * 获取会话的消息列表
     */
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>> = runCatching {
        conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Message::class.java)
            .reversed()
    }

    /**
     * 监听会话的新消息
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 标记消息为已读
     */
    suspend fun markMessageAsRead(conversationId: String, messageId: String, userId: String): Result<Unit> = runCatching {
        val messageRef = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .document(messageId)

        val message = messageRef.get().await().toObject(Message::class.java)
        message?.let {
            if (!it.readBy.contains(userId)) {
                val updatedReadBy = it.readBy + userId
                messageRef.update("readBy", updatedReadBy).await()
            }
        }
    }

    /**
     * 清空会话未读数
     */
    suspend fun clearUnreadCount(conversationId: String, userId: String): Result<Unit> = runCatching {
        val conversation = getConversation(conversationId).getOrNull()
        conversation?.let {
            val updatedUnreadCounts = it.unreadCounts.toMutableMap()
            updatedUnreadCounts[userId] = 0

            conversationsCollection
                .document(conversationId)
                .update("unreadCounts", updatedUnreadCounts)
                .await()
        }
    }

    /**
     * 删除消息
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .update("isDeleted", true)
            .await()
    }

    // ==================== 群组相关 Group Operations ====================

    /**
     * 创建群组
     */
    suspend fun createGroup(group: Group): Result<Group> = runCatching {
        val groupId = groupsCollection.document().id
        val groupWithId = group.copy(id = groupId)
        groupsCollection.document(groupId).set(groupWithId.toMap()).await()

        // 同时创建对应的群聊会话
        createGroupConversation(
            name = group.name,
            avatarUrl = group.avatarUrl,
            participants = group.memberIds,
            createdBy = group.ownerId
        )

        groupWithId
    }

    /**
     * 获取群组信息
     */
    suspend fun getGroup(groupId: String): Result<Group?> = runCatching {
        groupsCollection
            .document(groupId)
            .get()
            .await()
            .toObject(Group::class.java)
    }

    /**
     * 更新群组信息
     */
    suspend fun updateGroup(group: Group): Result<Unit> = runCatching {
        groupsCollection
            .document(group.id)
            .set(group.toMap())
            .await()
    }

    /**
     * 添加群成员
     */
    suspend fun addGroupMembers(groupId: String, memberIds: List<String>): Result<Unit> = runCatching {
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")
        val updatedMembers = (group.memberIds + memberIds).distinct()

        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()

        // 更新对应会话的参与者列表
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
    }

    /**
     * 移除群成员
     */
    suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> = runCatching {
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")
        val updatedMembers = group.memberIds.filter { it != memberId }

        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()

        // 更新对应会话的参与者列表
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
    }

    /**
     * 转让群主
     */
    suspend fun transferGroupOwnership(groupId: String, newOwnerId: String): Result<Unit> = runCatching {
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        groupsCollection
            .document(groupId)
            .update(
                mapOf(
                    "ownerId" to newOwnerId,
                    "adminIds" to (group.adminIds + group.ownerId).distinct()
                )
            )
            .await()
    }

    /**
     * 解散群组
     */
    suspend fun dismissGroup(groupId: String): Result<Unit> = runCatching {
        // 删除群组信息
        groupsCollection.document(groupId).delete().await()

        // 标记会话为不活跃
        deleteConversation(groupId)
    }
}


