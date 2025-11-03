/**
 * FirebaseInitializer.kt
 *
 * Firebase 数据库初始化工具 - 用于创建测试数据和初始化数据库结构
 * Firebase Database Initializer - Used to create test data and initialize database structure
 *
 * 使用方法：
 * 1. 确保用户已登录
 * 2. 调用相应的初始化方法
 * 3. 在 Firebase Console 中查看创建的数据
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat.utils

import android.util.Log
import com.example.cs501_micro_chat.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseInitializer @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "FirebaseInitializer"

    /**
     * 初始化当前登录用户的数据
     */
    suspend fun initializeCurrentUser(): Result<User> = runCatching {
        val currentUser = auth.currentUser ?: throw Exception("No user logged in")

        val user = User(
            id = currentUser.uid,
            username = currentUser.displayName ?: "User_${currentUser.uid.take(6)}",
            email = currentUser.email ?: "",
            avatarUrl = currentUser.photoUrl?.toString() ?: "",
            status = UserStatus.ONLINE,
            statusMessage = "Hey there! I'm using Micro Chat",
            createdAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(user.id)
            .set(user.toMap())
            .await()

        Log.d(TAG, "User initialized: ${user.username}")
        user
    }

    /**
     * 创建测试用户（仅用于测试环境）
     */
    suspend fun createTestUsers(count: Int = 5): Result<List<User>> = runCatching {
        val users = mutableListOf<User>()

        for (i in 1..count) {
            val userId = "test_user_$i"
            val user = User(
                id = userId,
                username = "TestUser$i",
                email = "testuser$i@example.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=TestUser$i",
                status = if (i % 2 == 0) UserStatus.ONLINE else UserStatus.OFFLINE,
                statusMessage = "This is test user $i",
                createdAt = System.currentTimeMillis() - (i * 86400000L), // 每个用户早一天创建
                lastSeenAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .set(user.toMap())
                .await()

            users.add(user)
            Log.d(TAG, "Test user created: ${user.username}")
        }

        users
    }

    /**
     * 为当前用户创建测试联系人
     */
    suspend fun createTestContacts(testUserIds: List<String>): Result<List<Contact>> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("No user logged in")
        val contacts = mutableListOf<Contact>()

        for ((index, testUserId) in testUserIds.withIndex()) {
            // 获取测试用户信息
            val userDoc = firestore.collection("users").document(testUserId).get().await()
            val username = userDoc.getString("username") ?: "User$index"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            val contact = Contact(
                userId = currentUserId,
                contactId = testUserId,
                contactName = username,
                contactAvatarUrl = avatarUrl,
                alias = if (index % 2 == 0) "好友$index" else "", // 偶数设置备注
                tags = if (index % 3 == 0) listOf("测试", "朋友") else emptyList(),
                isFavorite = index == 0, // 第一个设为特别关注
                isBlocked = false,
                addedAt = System.currentTimeMillis() - (index * 3600000L),
                conversationId = "" // 稍后创建会话时更新
            )

            firestore.collection("users")
                .document(currentUserId)
                .collection("contacts")
                .document(testUserId)
                .set(contact.toMap())
                .await()

            contacts.add(contact)
            Log.d(TAG, "Contact added: $username")
        }

        contacts
    }

    /**
     * 创建测试私聊会话和消息
     */
    suspend fun createTestPrivateConversations(contactIds: List<String>): Result<List<Conversation>> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("No user logged in")
        val conversations = mutableListOf<Conversation>()

        for ((index, contactId) in contactIds.withIndex()) {
            // 获取联系人信息
            val userDoc = firestore.collection("users").document(contactId).get().await()
            val username = userDoc.getString("username") ?: "User"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            // 创建会话
            val conversationId = firestore.collection("conversations").document().id
            val conversation = Conversation(
                id = conversationId,
                type = ConversationType.PRIVATE,
                name = username,
                avatarUrl = avatarUrl,
                participants = listOf(currentUserId, contactId),
                lastMessage = "Hello! 这是测试消息 $index",
                lastMessageTime = System.currentTimeMillis() - (index * 3600000L),
                unreadCounts = mapOf(currentUserId to index, contactId to 0),
                createdAt = System.currentTimeMillis() - (index * 86400000L),
                createdBy = currentUserId,
                isActive = true
            )

            firestore.collection("conversations")
                .document(conversationId)
                .set(conversation.toMap())
                .await()

            // 创建一些测试消息
            createTestMessages(conversationId, currentUserId, contactId, username, avatarUrl, 5)

            conversations.add(conversation)
            Log.d(TAG, "Private conversation created with: $username")
        }

        conversations
    }

    /**
     * 创建测试消息
     */
    private suspend fun createTestMessages(
        conversationId: String,
        userId1: String,
        userId2: String,
        user2Name: String,
        user2Avatar: String,
        count: Int
    ) {
        val currentUser = auth.currentUser ?: return
        val currentUsername = currentUser.displayName ?: "Me"
        val currentAvatar = currentUser.photoUrl?.toString() ?: ""

        val messages = listOf(
            "Hello! 👋",
            "How are you?",
            "I'm doing great! How about you?",
            "That's wonderful to hear! 😊",
            "Let's catch up soon!",
            "Sounds good! When are you free?",
            "How about this weekend?",
            "Perfect! See you then! 🎉"
        )

        for (i in 0 until count) {
            val isFromCurrentUser = i % 2 == 0
            val senderId = if (isFromCurrentUser) userId1 else userId2
            val senderName = if (isFromCurrentUser) currentUsername else user2Name
            val senderAvatar = if (isFromCurrentUser) currentAvatar else user2Avatar

            val message = Message(
                id = "",
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                senderAvatarUrl = senderAvatar,
                content = messages.getOrElse(i) { "Test message $i" },
                type = MessageType.TEXT,
                mediaUrl = "",
                timestamp = System.currentTimeMillis() - ((count - i) * 600000L), // 每条消息间隔10分钟
                readBy = if (isFromCurrentUser) listOf(senderId) else listOf(senderId, userId1),
                isDeleted = false
            )

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message.toMap())
                .await()
        }

        Log.d(TAG, "Created $count test messages in conversation: $conversationId")
    }

    /**
     * 创建测试群组
     */
    suspend fun createTestGroup(): Result<Group> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("No user logged in")

        // 获取一些测试用户作为群成员
        val testUsers = firestore.collection("users")
            .whereGreaterThanOrEqualTo("id", "test_user_")
            .limit(3)
            .get()
            .await()
            .documents
            .map { it.id }

        val groupId = firestore.collection("groups").document().id

        val group = Group(
            id = groupId,
            name = "测试群组 🎉",
            description = "这是一个测试群组，用于演示群聊功能",
            avatarUrl = "https://api.dicebear.com/7.x/identicon/svg?seed=TestGroup",
            ownerId = currentUserId,
            adminIds = listOf(currentUserId),
            memberIds = listOf(currentUserId) + testUsers,
            maxMembers = 500,
            createdAt = System.currentTimeMillis(),
            settings = GroupSettings(
                allowMemberInvite = true,
                requireAdminApproval = false,
                muteAll = false,
                showMemberList = true,
                allowMemberNickname = true
            )
        )

        // 创建群组
        firestore.collection("groups")
            .document(groupId)
            .set(group.toMap())
            .await()

        // 创建对应的群聊会话
        val conversation = Conversation(
            id = groupId,
            type = ConversationType.GROUP,
            name = group.name,
            avatarUrl = group.avatarUrl,
            participants = group.memberIds,
            lastMessage = "欢迎加入群聊！",
            lastMessageTime = System.currentTimeMillis(),
            unreadCounts = group.memberIds.associateWith { 0 },
            createdAt = System.currentTimeMillis(),
            createdBy = currentUserId,
            isActive = true
        )

        firestore.collection("conversations")
            .document(groupId)
            .set(conversation.toMap())
            .await()

        // 创建欢迎消息
        val welcomeMessage = Message(
            id = "",
            conversationId = groupId,
            senderId = "system",
            senderName = "系统",
            senderAvatarUrl = "",
            content = "欢迎加入群聊！",
            type = MessageType.SYSTEM,
            mediaUrl = "",
            timestamp = System.currentTimeMillis(),
            readBy = emptyList(),
            isDeleted = false
        )

        firestore.collection("conversations")
            .document(groupId)
            .collection("messages")
            .add(welcomeMessage.toMap())
            .await()

        Log.d(TAG, "Test group created: ${group.name}")
        group
    }

    /**
     * 一键初始化完整的测试数据
     */
    suspend fun initializeAllTestData(): Result<String> = runCatching {
        Log.d(TAG, "Starting full database initialization...")

        // 1. 初始化当前用户
        val currentUser = initializeCurrentUser().getOrThrow()
        Log.d(TAG, "✓ Current user initialized")

        // 2. 创建测试用户
        val testUsers = createTestUsers(5).getOrThrow()
        val testUserIds = testUsers.map { it.id }
        Log.d(TAG, "✓ Test users created: ${testUsers.size}")

        // 3. 添加联系人
        val contacts = createTestContacts(testUserIds).getOrThrow()
        Log.d(TAG, "✓ Contacts added: ${contacts.size}")

        // 4. 创建私聊会话
        val conversations = createTestPrivateConversations(testUserIds.take(3)).getOrThrow()
        Log.d(TAG, "✓ Private conversations created: ${conversations.size}")

        // 5. 创建群组
        val group = createTestGroup().getOrThrow()
        Log.d(TAG, "✓ Test group created: ${group.name}")

        val summary = """
            数据库初始化完成！
            
            创建内容：
            - 当前用户: ${currentUser.username}
            - 测试用户: ${testUsers.size} 个
            - 联系人: ${contacts.size} 个
            - 私聊会话: ${conversations.size} 个
            - 群组: 1 个
            
            请在 Firebase Console 查看创建的数据
        """.trimIndent()

        Log.d(TAG, summary)
        summary
    }

    /**
     * 清除所有测试数据（慎用！）
     */
    suspend fun clearAllTestData(): Result<String> = runCatching {
        Log.d(TAG, "Starting to clear test data...")

        // 删除测试用户
        val testUsers = firestore.collection("users")
            .whereGreaterThanOrEqualTo("id", "test_user_")
            .get()
            .await()

        testUsers.documents.forEach { doc ->
            doc.reference.delete().await()
            Log.d(TAG, "Deleted test user: ${doc.id}")
        }

        Log.d(TAG, "Test data cleared")
        "测试数据已清除"
    }
}

