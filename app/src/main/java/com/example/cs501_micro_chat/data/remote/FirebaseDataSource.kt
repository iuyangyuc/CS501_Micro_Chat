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
        snapshot.toObject(User::class.java)?.copy(id = userId)
    }

    /**
     * 批量获取用户信息
     * @param userIds 用户 ID 列表
     * @return Map<userId, User?>
     */
    suspend fun getUsers(userIds: List<String>): Result<Map<String, User>> = runCatching {
        if (userIds.isEmpty()) {
            return@runCatching emptyMap()
        }

        val users = mutableMapOf<String, User>()

        // Firebase 的 whereIn 限制为 10 个元素，需要分批查询
        userIds.distinct().chunked(10).forEach { chunk ->
            val snapshot = usersCollection
                .whereIn("__name__", chunk)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.toObject(User::class.java)?.let { user ->
                    users[doc.id] = user.copy(id = doc.id)
                }
            }
        }

        users
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
     *
     * 优化逻辑：
     * 1. 从 currentUserId 的 contacts 中查找是否已有与 otherUserId 的会话
     * 2. 如果找到，返回该会话
     * 3. 如果没有，创建新会话并同时在 currentUserId 的 contacts 中添加联系人信息
     */
    suspend fun createOrGetPrivateConversation(currentUserId: String, otherUserId: String): Result<Conversation> = runCatching {
        Log.d(TAG, "createOrGetPrivateConversation: currentUser=$currentUserId, otherUser=$otherUserId")

        // 1. 从当前用户的 contacts 中查找是否已有与 otherUserId 的联系人
        val contactDoc = usersCollection
            .document(currentUserId)
            .collection("contacts")
            .document(otherUserId)
            .get()
            .await()

        val existingContact = contactDoc.toObject(Contact::class.java)

        // 2. 如果联系人存在且有 conversationId，获取该会话
        if (existingContact != null && existingContact.conversationId.isNotEmpty()) {
            Log.d(TAG, "Found existing conversation: ${existingContact.conversationId}")
            val conversation = getConversation(existingContact.conversationId).getOrNull()
            if (conversation != null) {
                return@runCatching conversation
            }
            Log.w(TAG, "Conversation ${existingContact.conversationId} not found in Firestore, creating new one")
        }

        // 3. 如果没有找到会话，创建新会话
        Log.d(TAG, "Creating new conversation")
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

        // 保存会话到 Firestore
        conversationsCollection.document(conversationId).set(conversation.toMap()).await()
        Log.d(TAG, "Conversation created: $conversationId")

        // 4. 在两个用户的 contacts 中都添加联系人信息（双向关系）
        val currentTime = System.currentTimeMillis()

        // 为 currentUserId 添加 otherUserId 作为联系人
        val contactForCurrentUser = Contact(
            userId = currentUserId,
            contactId = otherUserId,
            type = "PRIVATE",
            conversationId = conversationId,
            isBlocked = false,
            addedAt = currentTime
        )
        addContact(contactForCurrentUser).getOrThrow()
        Log.d(TAG, "Contact added for currentUser: $currentUserId -> $otherUserId")

        // 为 otherUserId 添加 currentUserId 作为联系人
        val contactForOtherUser = Contact(
            userId = otherUserId,
            contactId = currentUserId,
            type = "PRIVATE",
            conversationId = conversationId,
            isBlocked = false,
            addedAt = currentTime
        )
        addContact(contactForOtherUser).getOrThrow()
        Log.d(TAG, "Contact added for otherUser: $otherUserId -> $currentUserId")

        conversation
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
     * 获取用户的会话列表（包括个人会话和群组会话）
     * 通过用户的联系人列表获取 conversationId，而不是查询所有会话
     *
     * 支持：
     * - 个人联系人会话（type = "PRIVATE"）
     * - 群组会话（type = "GROUP"）
     */
    suspend fun getUserConversations(userId: String): Result<List<Conversation>> = runCatching {
        // 1. 获取用户的所有联系人（包括个人和群组）
        val contacts = getContacts(userId).getOrNull() ?: emptyList()

        // 2. 提取所有 conversationId（过滤掉空的）
        // 无论是个人联系人还是群组，都有 conversationId 字段
        val conversationIds = contacts.mapNotNull { it.conversationId.takeIf { id -> id.isNotEmpty() } }

        Log.d(TAG, "getUserConversations: Found ${conversationIds.size} conversation IDs (including groups)")

        // 3. 如果没有会话，直接返回空列表
        if (conversationIds.isEmpty()) {
            return@runCatching emptyList()
        }

        // 4. 批量获取会话信息
        // Firebase 的 whereIn 限制为 10 个元素，需要分批查询
        val conversations = mutableListOf<Conversation>()
        conversationIds.chunked(10).forEach { chunk ->
            val snapshot = conversationsCollection
                .whereIn("__name__", chunk) // 使用文档 ID 查询
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents.mapNotNullTo(conversations) { doc ->
                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
            }
        }

        Log.d(TAG, "getUserConversations: Retrieved ${conversations.size} conversations from Firestore")

        // 5. 按最后消息时间排序
        conversations.sortedByDescending { it.lastMessageTime }
    }

    /**
     * 监听用户的会话列表（包括个人会话和群组会话）
     * 通过用户的联系人列表获取 conversationId，而不是查询所有会话
     *
     * 支持：
     * - 个人联系人会话（type = "PRIVATE"）
     * - 群组会话（type = "GROUP"）
     *
     * 注意：此方法会先监听联系人列表变化，然后监听对应的会话
     */
    fun observeUserConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        // 监听用户的联系人列表（包括个人联系人和群组）
        val contactsListener = usersCollection
            .document(userId)
            .collection("contacts")
            .whereEqualTo("isBlocked", false)
            .addSnapshotListener { contactsSnapshot, contactsError ->
                if (contactsError != null) {
                    Log.e(TAG, "Error observing contacts", contactsError)
                    close(contactsError)
                    return@addSnapshotListener
                }

                val contacts = contactsSnapshot?.toObjects(Contact::class.java) ?: emptyList()

                // 提取所有 conversationId（无论是个人还是群组）
                val conversationIds = contacts.mapNotNull {
                    it.conversationId.takeIf { id -> id.isNotEmpty() }
                }

                Log.d(TAG, "observeUserConversations: Found ${conversationIds.size} conversation IDs from contacts (including groups)")
                Log.d(TAG, "observeUserConversations: Contacts breakdown - ${contacts.count { it.type == "GROUP" }} groups, ${contacts.count { it.type == "PRIVATE" }} private")

                if (conversationIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // 监听这些会话的变化
                // 由于 Firebase 不支持直接监听多个文档 ID，我们需要分批监听或使用组合查询
                // 这里使用一个简单的方法：查询所有匹配的会话
                conversationsCollection
                    .whereEqualTo("isActive", true)
                    .addSnapshotListener { conversationsSnapshot, conversationsError ->
                        if (conversationsError != null) {
                            Log.e(TAG, "Error observing conversations", conversationsError)
                            close(conversationsError)
                            return@addSnapshotListener
                        }

                        // 过滤出属于用户的会话（包括个人和群组）
                        val allConversations = conversationsSnapshot?.documents?.mapNotNull { doc ->
                            if (conversationIds.contains(doc.id)) {
                                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                            } else null
                        } ?: emptyList()

                        Log.d(TAG, "observeUserConversations: Received ${allConversations.size} conversations from Firestore")
                        Log.d(TAG, "observeUserConversations: Breakdown - ${allConversations.count { it.type == ConversationType.GROUP }} groups, ${allConversations.count { it.type == ConversationType.PRIVATE }} private")

                        // 在客户端按时间排序
                        val sortedConversations = allConversations.sortedByDescending { it.lastMessageTime }
                        trySend(sortedConversations)
                    }
            }

        awaitClose {
            // 注意：这里只移除最外层的监听器
            // 内层监听器会在外层监听器触发时自动更新
            contactsListener.remove()
        }
    }

    /**
     * 获取单个会话信息
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?> = runCatching {
        val doc = conversationsCollection
            .document(conversationId)
            .get()
            .await()

        // 手动映射并设置 id
        doc.toObject(Conversation::class.java)?.copy(id = doc.id)
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
        val snapshot = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        // 手动映射，确保 id 字段被正确设置为文档 ID
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Message::class.java)?.copy(id = doc.id)
        }.reversed()
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
                // 手动映射，确保 id 字段被正确设置为文档 ID
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
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
