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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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

    private fun parseUser(doc: DocumentSnapshot): User? {
        val data = doc.data ?: return null
        return try {
            val createdAt = when (val created = data["createdAt"]) {
                is Long -> created
                is com.google.firebase.Timestamp -> created.toDate().time
                else -> System.currentTimeMillis()
            }

            val lastSeenAt = when (val lastSeen = data["lastSeenAt"]) {
                is Long -> lastSeen
                is com.google.firebase.Timestamp -> lastSeen.toDate().time
                else -> System.currentTimeMillis()
            }

        val displayName = (data["displayName"] as? String)?.takeIf { it.isNotBlank() }
            ?: data["username"] as? String
            ?: ""

        User(
            id = doc.id,
            username = displayName,
            email = data["email"] as? String ?: "",
            avatarUrl = data["avatarUrl"] as? String ?: "",
            status = try {
                UserStatus.valueOf(data["status"] as? String ?: "OFFLINE")
            } catch (_: Exception) {
                    UserStatus.OFFLINE
                },
                statusMessage = data["statusMessage"] as? String ?: "",
                createdAt = createdAt,
                lastSeenAt = lastSeenAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user ${doc.id}: ${e.message}")
            null
        }
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
        Log.d(TAG, "🔍 getUser: Fetching user with ID: '$userId' (length: ${userId.length})")

        val snapshot = usersCollection.document(userId).get().await()

        if (snapshot.exists()) {
            Log.d(TAG, "  ✅ User document exists in Firebase")
            Log.d(TAG, "  📄 Document data: ${snapshot.data}")

            try {
                // 手动构造 User 对象，处理 Timestamp 类型
                val data = snapshot.data
                if (data != null) {
                    val createdAt = when (val created = data["createdAt"]) {
                        is Long -> created
                        is com.google.firebase.Timestamp -> created.toDate().time
                        else -> System.currentTimeMillis()
                    }

                    val lastSeenAt = when (val lastSeen = data["lastSeenAt"]) {
                        is Long -> lastSeen
                        is com.google.firebase.Timestamp -> lastSeen.toDate().time
                        else -> System.currentTimeMillis()
                    }

                    val user = User(
                        id = userId,
                        username = data["username"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        avatarUrl = data["avatarUrl"] as? String ?: "",
                        status = try {
                            UserStatus.valueOf(data["status"] as? String ?: "OFFLINE")
                        } catch (e: Exception) {
                            UserStatus.OFFLINE
                        },
                        statusMessage = data["statusMessage"] as? String ?: "",
                        createdAt = createdAt,
                        lastSeenAt = lastSeenAt
                    )

                    Log.d(TAG, "  ✅ User object created: ${user.username} (${user.email})")
                    user
                } else {
                    Log.e(TAG, "  ❌ Document data is null")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Failed to deserialize user document: ${e.message}", e)
                null
            }
        } else {
            Log.e(TAG, "  ❌ User document does NOT exist at path: /users/$userId")
            Log.d(TAG, "  🔍 Attempting to list all user IDs to debug...")

            // 尝试列出所有用户ID来帮助调试
            try {
                val allUsers = usersCollection.limit(10).get().await()
                Log.d(TAG, "  📋 First 10 user IDs in database:")
                allUsers.documents.forEach { doc ->
                    Log.d(TAG, "    - ${doc.id} (username: ${doc.getString("username")})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Failed to list users: ${e.message}")
            }

            null
        }
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
                parseUser(doc)?.let { user ->
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
                val user = snapshot?.let { parseUser(it)?.copy(id = it.id) }
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
     * 搜索用户（支持 username 和 email 搜索）
     * Search users by username or email
     */
    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val results = mutableListOf<User>()
        val trimmedQuery = query.trim()
        val lowerQuery = trimmedQuery.lowercase()

        if (trimmedQuery.isBlank()) {
            return@runCatching emptyList()
        }

        // 1. 通过用户名搜索（前缀匹配）- 尝试原始大小写
        try {
            val byUsername = usersCollection
                .whereGreaterThanOrEqualTo("username", trimmedQuery)
                .whereLessThanOrEqualTo("username", trimmedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> parseUser(doc)?.copy(id = doc.id) }
            results.addAll(byUsername)
        } catch (e: Exception) {
            Log.d(TAG, "Username search (original case) failed: ${e.message}")
        }

        // 1b. 通过用户名搜索（小写前缀匹配）
        if (lowerQuery != trimmedQuery) {
            try {
                val byUsernameLower = usersCollection
                    .whereGreaterThanOrEqualTo("username", lowerQuery)
                    .whereLessThanOrEqualTo("username", lowerQuery + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()
                .documents
                .mapNotNull { doc -> parseUser(doc)?.copy(id = doc.id) }
                results.addAll(byUsernameLower)
            } catch (e: Exception) {
                Log.d(TAG, "Username search (lowercase) failed: ${e.message}")
            }
        }

        // 2. 通过邮箱搜索（前缀匹配）
        try {
            val byEmail = usersCollection
                .whereGreaterThanOrEqualTo("email", lowerQuery)
                .whereLessThanOrEqualTo("email", lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> parseUser(doc)?.copy(id = doc.id) }
            results.addAll(byEmail)
        } catch (e: Exception) {
            Log.d(TAG, "Email search failed: ${e.message}")
        }

        // 3. 如果结果太少，尝试精确匹配 ID
        if (results.isEmpty()) {
            try {
                val byId = usersCollection
                    .document(trimmedQuery)
                    .get()
                    .await()
                parseUser(byId)?.let { user ->
                    results.add(user.copy(id = byId.id))
                }
            } catch (e: Exception) {
                Log.d(TAG, "User ID search failed: ${e.message}")
            }
        }

        // 4. 如果还是没结果，尝试获取所有用户并在本地进行大小写不敏感的匹配
        if (results.isEmpty()) {
            try {
                Log.d(TAG, "Falling back to client-side search for: $trimmedQuery")
                val allUsers = usersCollection
                    .limit(100) // 限制返回数量以提高性能
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> parseUser(doc)?.copy(id = doc.id) }

                Log.d(TAG, "Client-side search: Retrieved ${allUsers.size} users")

                // 记录所有用户的ID和用户名以便调试
                allUsers.forEach { user ->
                    Log.d(TAG, "  👤 Found user: id='${user.id}' username='${user.username}' email='${user.email}'")
                }

                // 在客户端进行大小写不敏感的搜索
                val matchedUsers = allUsers.filter { user ->
                    val usernameMatch = user.username.contains(trimmedQuery, ignoreCase = true)
                    val emailMatch = user.email.contains(lowerQuery, ignoreCase = true)
                    val idMatch = user.id == trimmedQuery

                    if (usernameMatch || emailMatch || idMatch) {
                        Log.d(TAG, "  ✅ MATCH: ${user.username} (id=${user.id}) - usernameMatch=$usernameMatch, emailMatch=$emailMatch, idMatch=$idMatch")
                    }

                    usernameMatch || emailMatch || idMatch
                }

                Log.d(TAG, "Client-side search: Matched ${matchedUsers.size} users for query '$trimmedQuery'")
                results.addAll(matchedUsers)
            } catch (e: Exception) {
                Log.e(TAG, "Client-side search failed: ${e.message}", e)
            }
        }

        // 去重并按相关性排序
        results.distinctBy { it.id }
            .sortedWith(compareBy(
                // 优先显示用户名完全匹配的
                { !it.username.equals(trimmedQuery, ignoreCase = true) },
                // 其次显示邮箱完全匹配的
                { !it.email.equals(lowerQuery, ignoreCase = true) },
                // 然后显示用户名包含搜索词的
                { !it.username.contains(trimmedQuery, ignoreCase = true) },
                // 最后按用户名字母顺序
                { it.username.lowercase() }
            ))
            .take(10) // 最多返回10个结果
    }

    /**
     * 搜索群组（通过群组名称搜索）
     * Search groups by name
     */
    suspend fun searchGroups(query: String): Result<List<Group>> = runCatching {
        val results = mutableListOf<Group>()
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            return@runCatching emptyList()
        }

        Log.d(TAG, "🔍 searchGroups: query='$trimmedQuery'")

        // 1. 通过群组名称搜索（前缀匹配）
        try {
            val byName = groupsCollection
                .whereGreaterThanOrEqualTo("name", trimmedQuery)
                .whereLessThanOrEqualTo("name", trimmedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                }
            results.addAll(byName)
            Log.d(TAG, "Group search by name prefix found ${byName.size} results")
        } catch (e: Exception) {
            Log.d(TAG, "Group name prefix search failed: ${e.message}")
        }

        // 2. 如果结果太少，尝试客户端搜索
        if (results.isEmpty()) {
            try {
                Log.d(TAG, "Falling back to client-side group search for: $trimmedQuery")
                val allGroups = groupsCollection
                    .limit(100)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject(Group::class.java)?.copy(id = doc.id)
                    }

                Log.d(TAG, "Client-side group search: Retrieved ${allGroups.size} groups")

                // 在客户端进行大小写不敏感的搜索
                val matchedGroups = allGroups.filter { group ->
                    group.name.contains(trimmedQuery, ignoreCase = true) ||
                    group.id == trimmedQuery
                }

                Log.d(TAG, "Client-side group search: Matched ${matchedGroups.size} groups for query '$trimmedQuery'")
                results.addAll(matchedGroups)
            } catch (e: Exception) {
                Log.e(TAG, "Client-side group search failed: ${e.message}", e)
            }
        }

        // 去重并按相关性排序
        results.distinctBy { it.id }
            .sortedWith(compareBy(
                // 优先显示名称完全匹配的
                { !it.name.equals(trimmedQuery, ignoreCase = true) },
                // 然后显示名称包含搜索词的
                { !it.name.contains(trimmedQuery, ignoreCase = true) },
                // 最后按名称字母顺序
                { it.name.lowercase() }
            ))
            .take(10) // 最多返回10个结果
    }

    /**
     * 加入群组
     * Join a group: add user to group members and create contact for user
     */
    suspend fun joinGroup(groupId: String, userId: String): Result<Unit> = runCatching {
        Log.d(TAG, "🚀 joinGroup: groupId=$groupId, userId=$userId")

        // 1. 获取群组信息
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        // 2. 检查用户是否已经是成员
        if (group.memberIds.contains(userId)) {
            Log.d(TAG, "⚠️ User $userId is already a member of group $groupId")
            return@runCatching // 已经是成员，直接返回成功
        }

        // 3. 更新群组成员列表
        val updatedMembers = group.memberIds + userId
        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated groups/$groupId/memberIds")

        // 4. 更新对应会话的参与者列表
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated conversations/$groupId/participants")

        // 5. 为用户创建 contact 记录
        val contact = Contact(
            userId = userId,
            contactId = groupId,
            contactName = group.name,
            contactAvatarUrl = group.avatarUrl,
            type = "GROUP",
            conversationId = group.conversationId,
            isNew = false,
            isPending = false,
            isBlocked = false,
            isFavorite = false,
            addedAt = System.currentTimeMillis()
        )

        usersCollection
            .document(userId)
            .collection("contacts")
            .document(groupId)
            .set(contact.toMap())
            .await()
        Log.d(TAG, "✅ Created contact for user $userId: users/$userId/contacts/$groupId")

        Log.d(TAG, "🎉 Successfully joined group $groupId")
    }

    // ==================== 联系人相关 Contact Operations ====================

    /**
     * 添加联系人
     */
    suspend fun addContact(contact: Contact): Result<Unit> = runCatching {
        Log.d(TAG, "🔄 addContact: user=${contact.userId}, contact=${contact.contactId}")
        Log.d(TAG, "  📋 Contact details: name=${contact.contactName}, type=${contact.type}, isNew=${contact.isNew}, isPending=${contact.isPending}")

        val docRef = usersCollection
            .document(contact.userId)
            .collection("contacts")
            .document(contact.contactId)

        val contactMap = contact.toMap()
        Log.d(TAG, "  📦 Contact map: $contactMap")

        docRef.set(contactMap).await()

        Log.d(TAG, "  ✅ Contact added successfully to Firebase: /users/${contact.userId}/contacts/${contact.contactId}")

        // 验证数据是否真的写入了
        val verification = docRef.get().await()
        if (verification.exists()) {
            Log.d(TAG, "  ✅ Verification: Document exists in Firebase")
            Log.d(TAG, "  📄 Verification data: ${verification.data}")
        } else {
            Log.e(TAG, "  ❌ Verification: Document does NOT exist in Firebase!")
        }
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
     * 获取单个联系人信息
     */
    suspend fun getContact(userId: String, contactId: String): Result<Contact?> = runCatching {
        usersCollection
            .document(userId)
            .collection("contacts")
            .document(contactId)
            .get()
            .await()
            .toObject(Contact::class.java)
    }

    /**
     * 监听联系人列表变化
     * 自动补充 contactName 和 contactAvatarUrl（如果为空）
     */
    fun observeContacts(userId: String): Flow<List<Contact>> = callbackFlow {
        val scope = this // Capture the ProducerScope to use inside the listener

        val listener = usersCollection
            .document(userId)
            .collection("contacts")
            .whereEqualTo("isBlocked", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // 获取联系人基础信息
                val contacts = snapshot?.documents?.mapNotNull { doc ->
                    // 打印原始 Firebase 数据用于调试
                    Log.d(TAG, "📄 Firebase Contact Document ${doc.id}:")
                    Log.d(TAG, "  - Raw data: ${doc.data}")
                    Log.d(TAG, "  - isNew: ${doc.get("isNew")} (${doc.get("isNew")?.javaClass?.simpleName})")
                    Log.d(TAG, "  - isPending: ${doc.get("isPending")} (${doc.get("isPending")?.javaClass?.simpleName})")

                    // 手动构造 Contact 对象，确保 isPending 字段被正确读取
                    try {
                        val data = doc.data
                        if (data != null) {
                            val contact = Contact(
                                userId = data["userId"] as? String ?: "",
                                contactId = data["contactId"] as? String ?: doc.id,
                                contactName = data["contactName"] as? String ?: "",
                                contactAvatarUrl = data["contactAvatarUrl"] as? String ?: "",
                                type = data["type"] as? String ?: "PRIVATE",
                                alias = data["alias"] as? String ?: "",
                                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                isFavorite = data["isFavorite"] as? Boolean ?: false,
                                isBlocked = data["isBlocked"] as? Boolean ?: false,
                                isNew = data["isNew"] as? Boolean ?: false,
                                isPending = data["isPending"] as? Boolean ?: false,
                                addedAt = (data["addedAt"] as? Long) ?: System.currentTimeMillis(),
                                conversationId = data["conversationId"] as? String ?: ""
                            )

                            Log.d(TAG, "  → Manually constructed Contact: isNew=${contact.isNew}, isPending=${contact.isPending}, conversationId=${contact.conversationId}")

                            // 检查是否缺少字段
                            if (!data.containsKey("isPending")) {
                                Log.w(TAG, "  ⚠️ Contact ${contact.contactId} missing 'isPending' field in Firebase document (old data)")
                                if (contact.conversationId.isEmpty() && !contact.isNew) {
                                    Log.w(TAG, "  ⚠️ Possible old pending request, please update manually in Firebase")
                                }
                            }

                            contact
                        } else {
                            Log.e(TAG, "  → Document data is null")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "  → Error constructing Contact: ${e.message}", e)
                        null
                    }
                } ?: emptyList()

                // 异步补充每个联系人的详细信息（如果缺失）
                scope.launch {
                    try {
                        Log.d(TAG, "🔧 Starting contact enrichment for ${contacts.size} contacts")
                        val enrichedContacts = contacts.map { contact ->
                            Log.d(TAG, "  📝 Before enrichment - ${contact.contactId}: isNew=${contact.isNew}, isPending=${contact.isPending}")

                            // 如果 contactName 或 contactAvatarUrl 为空，从对应用户获取
                            val result = if (contact.type == "PRIVATE" && (contact.contactName.isEmpty() || contact.contactAvatarUrl.isEmpty())) {
                                val user = getUser(contact.contactId).getOrNull()
                                val enriched = contact.copy(
                                    contactName = if (contact.contactName.isEmpty()) user?.username ?: "" else contact.contactName,
                                    contactAvatarUrl = if (contact.contactAvatarUrl.isEmpty()) user?.avatarUrl ?: "" else contact.contactAvatarUrl
                                )
                                Log.d(TAG, "  ✏️ After enrichment - ${contact.contactId}: isNew=${enriched.isNew}, isPending=${enriched.isPending}")
                                enriched
                            } else {
                                Log.d(TAG, "  ⏭️ No enrichment needed - ${contact.contactId}")
                                contact
                            }
                            result
                        }
                        Log.d(TAG, "🔧 Enrichment complete, sending ${enrichedContacts.size} contacts")
                        trySend(enrichedContacts)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enriching contacts", e)
                        trySend(contacts) // Fall back to original contacts
                    }
                }
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
     * 移除联系人（别名，与 deleteContact 相同）
     */
    suspend fun removeContact(userId: String, contactId: String): Result<Unit> = deleteContact(userId, contactId)

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

    /**
     * 更新联系人置顶状态
     */
    suspend fun updateContactFavorite(userId: String, contactId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        usersCollection
            .document(userId)
            .collection("contacts")
            .document(contactId)
            .update("isFavorite", isFavorite)
            .await()
    }

    /**
     * 监听置顶会话
     */
    fun observePinnedConversations(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .collection("pinned_conversations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val pinned = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("conversationId")
                }?.toSet() ?: emptySet()
                trySend(pinned)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 设置置顶状态
     */
    suspend fun setPinnedConversation(userId: String, conversationId: String, pinned: Boolean): Result<Unit> = runCatching {
        val docRef = usersCollection
            .document(userId)
            .collection("pinned_conversations")
            .document(conversationId)
        if (pinned) {
            docRef.set(
                mapOf(
                    "conversationId" to conversationId,
                    "pinnedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.delete().await()
        }
    }

    /**
     * 检查会话是否置顶
     */
    suspend fun isConversationPinned(userId: String, conversationId: String): Result<Boolean> = runCatching {
        usersCollection
            .document(userId)
            .collection("pinned_conversations")
            .document(conversationId)
            .get()
            .await()
            .exists()
    }

    /**
     * 设置会话对某个用户的屏蔽状态
     */
    suspend fun setConversationParticipantBlocked(conversationId: String, participantId: String, blocked: Boolean): Result<Unit> = runCatching {
        val field = "blockedParticipants.$participantId"
        val docRef = conversationsCollection.document(conversationId)
        if (blocked) {
            docRef.update(field, true).await()
        } else {
            docRef.update(field, FieldValue.delete()).await()
        }
    }

    /**
     * 检查用户是否被屏蔽
     */
    suspend fun isConversationParticipantBlocked(conversationId: String, participantId: String): Result<Boolean> = runCatching {
        val snapshot = conversationsCollection
            .document(conversationId)
            .get()
            .await()
        val blockedMap = snapshot.get("blockedParticipants") as? Map<*, *> ?: emptyMap<String, Boolean>()
        blockedMap[participantId] == true
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

        // 获取两个用户的详细信息
        val currentUser = getUser(currentUserId).getOrNull()

        // 为 currentUserId 添加 otherUserId 作为联系人
        val contactForCurrentUser = Contact(
            userId = currentUserId,
            contactId = otherUserId,
            contactName = otherUser?.username ?: "",
            contactAvatarUrl = otherUser?.avatarUrl ?: "",
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
            contactName = currentUser?.username ?: "",
            contactAvatarUrl = currentUser?.avatarUrl ?: "",
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

        // 2. 过滤出已确认的联系人
        // 只保留：isNew = false && isPending = false 的联系人和群组
        val confirmedContacts = contacts.filter { contact ->
            // 群组直接显示
            if (contact.type == "GROUP") {
                true
            } else {
                // 个人联系人：必须是已确认的好友（isNew = false && isPending = false）
                // isNew = false, isPending = true: 已发送请求，等待对方接受 → 不显示
                // isNew = true, isPending = false: 收到请求，等待我接受 → 不显示
                // isNew = false, isPending = false: 已确认的好友 → 显示
                !contact.isNew && !contact.isPending
            }
        }

        Log.d(TAG, "getUserConversations: Total contacts: ${contacts.size}, Confirmed: ${confirmedContacts.size}, Pending: ${contacts.size - confirmedContacts.size}")

        // 3. 提取所有 conversationId（过滤掉空的）
        // 无论是个人联系人还是群组，都有 conversationId 字段
        val conversationIds = confirmedContacts.mapNotNull { it.conversationId.takeIf { id -> id.isNotEmpty() } }

        Log.d(TAG, "getUserConversations: Found ${conversationIds.size} conversation IDs from confirmed contacts (including groups)")

        // 4. 如果没有会话，直接返回空列表
        if (conversationIds.isEmpty()) {
            return@runCatching emptyList()
        }

        // 5. 批量获取会话信息
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

        // 6. 按最后消息时间排序
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

                // **手动构造 Contact 对象，确保 isPending 和 isNew 字段被正确读取**
                val contacts = contactsSnapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            Contact(
                                userId = data["userId"] as? String ?: "",
                                contactId = data["contactId"] as? String ?: doc.id,
                                contactName = data["contactName"] as? String ?: "",
                                contactAvatarUrl = data["contactAvatarUrl"] as? String ?: "",
                                type = data["type"] as? String ?: "PRIVATE",
                                alias = data["alias"] as? String ?: "",
                                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                isFavorite = data["isFavorite"] as? Boolean ?: false,
                                isBlocked = data["isBlocked"] as? Boolean ?: false,
                                isNew = data["isNew"] as? Boolean ?: false,
                                isPending = data["isPending"] as? Boolean ?: false,
                                addedAt = (data["addedAt"] as? Long) ?: System.currentTimeMillis(),
                                conversationId = data["conversationId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing contact ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                // 详细日志：显示所有 contacts 的状态
                Log.d(TAG, "observeUserConversations: ====== ALL CONTACTS ======")
                contacts.forEach { contact ->
                    val status = when {
                        contact.type == "GROUP" -> "GROUP (always show)"
                        !contact.isNew && !contact.isPending -> "✅ CONFIRMED (show)"
                        contact.isPending -> "⏳ PENDING (hide - waiting for accept)"
                        contact.isNew -> "🆕 NEW REQUEST (hide - waiting for me to accept)"
                        else -> "❓ UNKNOWN"
                    }
                    Log.d(TAG, "  Contact ${contact.contactId}: isNew=${contact.isNew}, isPending=${contact.isPending}, conversationId=${contact.conversationId} → $status")
                }

                // 过滤出已确认的联系人
                // 只保留：isNew = false && isPending = false 的联系人和群组
                val confirmedContacts = contacts.filter { contact ->
                    // 群组直接显示
                    if (contact.type == "GROUP") {
                        true
                    } else {
                        // 个人联系人：必须是已确认的好友（isNew = false && isPending = false）
                        !contact.isNew && !contact.isPending
                    }
                }

                Log.d(TAG, "observeUserConversations: Total contacts: ${contacts.size}, Confirmed: ${confirmedContacts.size}, Pending: ${contacts.size - confirmedContacts.size}")

                // 提取所有 conversationId（无论是个人还是群组）
                val conversationIds = confirmedContacts.mapNotNull {
                    it.conversationId.takeIf { id -> id.isNotEmpty() }
                }

                Log.d(TAG, "observeUserConversations: Found ${conversationIds.size} conversation IDs from confirmed contacts (including groups)")
                Log.d(TAG, "observeUserConversations: Contacts breakdown - ${confirmedContacts.count { it.type == "GROUP" }} groups, ${confirmedContacts.count { it.type == "PRIVATE" }} private")

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
     * 生成新的会话 ID
     */
    fun generateConversationId(): String {
        return conversationsCollection.document().id
    }

    /**
     * 创建会话（不自动创建 contacts）
     */
    suspend fun createConversation(conversation: Conversation): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversation.id)
            .set(conversation.toMap())
            .await()
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

    /**
     * 记录当前用户清空聊天的时间戳（单向清除）
     */
    suspend fun clearConversationForUser(conversationId: String, userId: String, clearedAt: Long): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversationId)
            .update("clearedAt.$userId", clearedAt)
            .await()
    }

    // ==================== 群组相关 Group Operations ====================

    /**
     * 创建群组
     * 1. 生成 groupId 和 conversationId
     * 2. 创建群组记录（包含 conversationId）
     * 3. 创建群聊会话（使用 groupId 作为 conversationId）
     * 4. 为所有成员创建 contacts 记录
     */
    suspend fun createGroup(group: Group): Result<Group> = runCatching {
        Log.d(TAG, "📝 createGroup: name=${group.name}, members=${group.memberIds.size}")

        // 1. 生成 groupId，同时作为 conversationId
        val groupId = groupsCollection.document().id
        val conversationId = groupId  // 群组的 conversationId 就是 groupId

        Log.d(TAG, "🆔 Generated groupId/conversationId: $conversationId")

        // 2. 创建群组记录，包含 conversationId
        val groupWithId = group.copy(
            id = groupId,
            conversationId = conversationId
        )
        groupsCollection.document(groupId).set(groupWithId.toMap()).await()
        Log.d(TAG, "✅ Created group document: groups/$groupId")

        // 3. 创建对应的群聊会话（使用 groupId 作为 conversationId）
        val conversation = Conversation(
            id = conversationId,
            type = ConversationType.GROUP,
            name = group.name,
            avatarUrl = group.avatarUrl,
            participants = group.memberIds,
            createdBy = group.ownerId,
            unreadCounts = group.memberIds.associateWith { 0 }
        )
        conversationsCollection.document(conversationId).set(conversation.toMap()).await()
        Log.d(TAG, "✅ Created conversation document: conversations/$conversationId")

        // 4. 为所有成员创建 contacts 记录
        group.memberIds.forEach { memberId ->
            val contact = Contact(
                userId = memberId,
                contactId = groupId,
                contactName = group.name,
                contactAvatarUrl = group.avatarUrl,
                type = "GROUP",
                conversationId = conversationId,
                isNew = false,
                isPending = false,
                isBlocked = false,
                isFavorite = false,
                addedAt = System.currentTimeMillis()
            )

            usersCollection
                .document(memberId)
                .collection("contacts")
                .document(groupId)
                .set(contact.toMap())
                .await()

            Log.d(TAG, "✅ Created contact for user $memberId: users/$memberId/contacts/$groupId")
        }

        Log.d(TAG, "🎉 Group creation completed: $groupId with ${group.memberIds.size} members")
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
        Log.d(TAG, "🚪 removeGroupMember: groupId=$groupId, memberId=$memberId")

        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        // 检查成员是否存在
        if (!group.memberIds.contains(memberId)) {
            Log.w(TAG, "⚠️ User $memberId is not a member of group $groupId")
            return@runCatching  // 不是成员，直接返回成功
        }

        val updatedMembers = group.memberIds.filter { it != memberId }
        Log.d(TAG, "📝 Updating group members: ${group.memberIds.size} -> ${updatedMembers.size}")

        // 1. 更新群组成员列表
        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated groups/$groupId/memberIds")

        // 2. 更新对应会话的参与者列表
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated conversations/$groupId/participants")

        // 3. 从用户的 contacts 中删除该群组
        usersCollection
            .document(memberId)
            .collection("contacts")
            .document(groupId)
            .delete()
            .await()
        Log.d(TAG, "✅ Deleted users/$memberId/contacts/$groupId")

        Log.d(TAG, "🎉 Successfully removed user $memberId from group $groupId")
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
     * 完整删除群组：
     * 1. 从所有成员的 contacts 中删除该群组
     * 2. 删除群组本身 (groups/{groupId})
     * 3. 删除绑定的会话及其消息 (conversations/{groupId})
     */
    suspend fun dismissGroup(groupId: String): Result<Unit> = runCatching {
        Log.d(TAG, "💥 dismissGroup: groupId=$groupId")

        // 1. 获取群组信息，获取所有成员列表
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        Log.d(TAG, "📝 Group has ${group.memberIds.size} members to remove contacts")

        // 2. 从所有成员的 contacts 中删除该群组
        group.memberIds.forEach { memberId ->
            try {
                usersCollection
                    .document(memberId)
                    .collection("contacts")
                    .document(groupId)
                    .delete()
                    .await()
                Log.d(TAG, "✅ Deleted contact for user $memberId: users/$memberId/contacts/$groupId")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to delete contact for user $memberId", e)
                // 继续处理其他成员，不因为一个失败而中断
            }
        }

        // 3. 删除会话中的所有消息
        try {
            val messagesSnapshot = conversationsCollection
                .document(groupId)
                .collection("messages")
                .get()
                .await()

            messagesSnapshot.documents.forEach { messageDoc ->
                try {
                    messageDoc.reference.delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to delete message ${messageDoc.id}", e)
                }
            }
            Log.d(TAG, "✅ Deleted ${messagesSnapshot.size()} messages from conversation: $groupId")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to delete messages for conversation $groupId", e)
        }

        // 4. 删除会话本身
        try {
            conversationsCollection
                .document(groupId)
                .delete()
                .await()
            Log.d(TAG, "✅ Deleted conversation: conversations/$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to delete conversation $groupId", e)
        }

        // 5. 删除群组本身
        try {
            groupsCollection
                .document(groupId)
                .delete()
                .await()
            Log.d(TAG, "✅ Deleted group: groups/$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to delete group $groupId", e)
        }

        Log.d(TAG, "🎉 Group completely dismissed and deleted: $groupId")
    }
}
