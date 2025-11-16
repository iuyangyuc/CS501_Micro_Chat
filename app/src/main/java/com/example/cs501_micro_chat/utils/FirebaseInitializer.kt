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
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=TestUser$i",
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
            avatarUrl = "https://api.dicebear.com/7.x/identicon/png?seed=TestGroup",
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

        // 删除好友测试数据
        val friends = firestore.collection("users")
            .whereGreaterThanOrEqualTo("id", "friend_")
            .get()
            .await()

        friends.documents.forEach { doc ->
            doc.reference.delete().await()
            Log.d(TAG, "Deleted friend: ${doc.id}")
        }

        Log.d(TAG, "Test data cleared")
        "测试数据已清除"
    }

    /**
     * 为特定用户创建完整的测试数据（好友 + 对话历史）
     * 专门为 lf1991@bu.edu (ID: oQxEirc9JbOmHOTUjsm9q4mFpln2) 设计
     */
    suspend fun createCompleteTestDataForUser(userId: String): Result<String> = runCatching {
        Log.d(TAG, "开始为用户 $userId 创建完整测试数据")

        // 0. 首先更新当前用户的完整信息
        updateCurrentUserInfo(userId)
        Log.d(TAG, "当前用户信息已更新")

        // 1. 创建测试好友（虚拟用户）
        val friends = createTestFriends(userId)
        Log.d(TAG, "创建了 ${friends.size} 个测试好友")

        // 2. 为每个好友创建对话历史
        var totalMessages = 0
        for (friend in friends) {
            val messageCount = createConversationWithFriend(userId, friend)
            totalMessages += messageCount
            Log.d(TAG, "与 ${friend.username} 创建了 $messageCount 条消息")
        }

        // 3. 创建一个测试群组
        val groupResult = createTestGroupForUser(userId, friends.take(3))

        val groupName = groupResult.getOrNull()?.name ?: ""

        "成功创建：\n" +
                "✅ 用户信息已更新\n" +
                "✅ ${friends.size} 个好友\n" +
                "✅ ${friends.size} 个私聊会话\n" +
                "✅ $totalMessages 条聊天消息\n" +
                "✅ 1 个群组\n" +
                if (groupName.isNotEmpty()) "✅ 群组名称：$groupName" else ""
    }

    /**
     * 更新当前用户的完整信息
     */
    private suspend fun updateCurrentUserInfo(userId: String) {
        val currentFirebaseUser = auth.currentUser
        val email = currentFirebaseUser?.email ?: "lf1991@bu.edu"

        val user = User(
            id = userId,
            username = "Leo Fang", // 为测试账号设置一个用户名
            email = email,
            avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=LeoFang",
            status = UserStatus.ONLINE,
            statusMessage = "使用 Micro Chat 聊天中 💬",
            createdAt = System.currentTimeMillis() - 90 * 86400000L, // 90天前注册
            lastSeenAt = System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .set(user.toMap())
            .await()

        Log.d(TAG, "用户信息已更新: ${user.username} (${user.email})")
    }

    /**
     * 创建测试好友（虚拟用户）
     */
    private suspend fun createTestFriends(currentUserId: String): List<User> {
        val friends = listOf(
            User(
                id = "friend_001",
                username = "王经理",
                email = "wang.manager@company.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=WangManager",
                status = UserStatus.ONLINE,
                statusMessage = "忙碌中...",
                createdAt = System.currentTimeMillis() - 30 * 86400000L,
                lastSeenAt = System.currentTimeMillis()
            ),
            User(
                id = "friend_002",
                username = "Sarah Liu",
                email = "sarah.liu@design.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=SarahLiu",
                status = UserStatus.ONLINE,
                statusMessage = "设计中🎨",
                createdAt = System.currentTimeMillis() - 45 * 86400000L,
                lastSeenAt = System.currentTimeMillis() - 3600000L
            ),
            User(
                id = "friend_003",
                username = "张工程师",
                email = "zhang.engineer@tech.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=ZhangEngineer",
                status = UserStatus.AWAY,
                statusMessage = "Code never lies 💻",
                createdAt = System.currentTimeMillis() - 60 * 86400000L,
                lastSeenAt = System.currentTimeMillis() - 7200000L
            ),
            User(
                id = "friend_004",
                username = "Lisa Chen",
                email = "lisa.chen@pm.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=LisaChen",
                status = UserStatus.AWAY,
                statusMessage = "Meeting all day 📅",
                createdAt = System.currentTimeMillis() - 20 * 86400000L,
                lastSeenAt = System.currentTimeMillis() - 14400000L
            ),
            User(
                id = "friend_005",
                username = "Mike Developer",
                email = "mike.dev@startup.io",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=MikeDev",
                status = UserStatus.OFFLINE,
                statusMessage = "Building the future 🚀",
                createdAt = System.currentTimeMillis() - 90 * 86400000L,
                lastSeenAt = System.currentTimeMillis() - 86400000L
            )
        )

        // 写入 Firebase
        for (friend in friends) {
            firestore.collection("users")
                .document(friend.id)
                .set(friend.toMap())
                .await()

            // 同时为当前用户添加联系人
            val contact = Contact(
                userId = currentUserId,
                contactId = friend.id,
                contactName = friend.username,
                contactAvatarUrl = friend.avatarUrl,
                alias = "", // 不设置备注
                tags = emptyList(),
                isFavorite = friend.id == "friend_001", // 第一个设为特别关注
                isBlocked = false,
                addedAt = friend.createdAt,
                conversationId = "" // 稍后创建会话时更新
            )

            firestore.collection("users")
                .document(currentUserId)
                .collection("contacts")
                .document(friend.id)
                .set(contact.toMap())
                .await()
        }

        return friends
    }

    /**
     * 为特定好友创建对话历史
     */
    private suspend fun createConversationWithFriend(currentUserId: String, friend: User): Int {
        // 创建会话
        val conversationId = firestore.collection("conversations").document().id

        val conversation = Conversation(
            id = conversationId,
            type = ConversationType.PRIVATE,
            name = friend.username,
            avatarUrl = friend.avatarUrl,
            participants = listOf(currentUserId, friend.id),
            lastMessage = getLastMessageForFriend(friend.id),
            lastMessageTime = getLastMessageTimeForFriend(friend.id),
            unreadCounts = mapOf(
                currentUserId to getUnreadCountForFriend(friend.id),
                friend.id to 0
            ),
            createdAt = friend.createdAt,
            createdBy = currentUserId,
            isActive = true
        )

        firestore.collection("conversations")
            .document(conversationId)
            .set(conversation.toMap())
            .await()

        // 创建消息历史
        val messages = getMessagesForFriend(friend.id, currentUserId, friend)
        for ((index, messageData) in messages.withIndex()) {
            val message = Message(
                id = "",
                conversationId = conversationId,
                senderId = messageData.senderId,
                senderName = messageData.senderName,
                senderAvatarUrl = messageData.avatarUrl,
                content = messageData.content,
                type = MessageType.TEXT,
                mediaUrl = "",
                timestamp = System.currentTimeMillis() - ((messages.size - index) * 3600000L),
                readBy = if (messageData.senderId == currentUserId)
                    listOf(currentUserId, friend.id)
                else
                    if (index < messages.size - getUnreadCountForFriend(friend.id))
                        listOf(currentUserId, friend.id)
                    else
                        listOf(friend.id),
                isDeleted = false
            )

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message.toMap())
                .await()
        }

        return messages.size
    }

    /**
     * 获取好友的对话内容
     */
    private fun getMessagesForFriend(
        friendId: String,
        currentUserId: String,
        friend: User
    ): List<MessageData> {
        return when (friendId) {
            "friend_001" -> listOf( // 王经理
                MessageData(friend.id, friend.username, friend.avatarUrl, "明天的会议准备好了吗？"),
                MessageData(currentUserId, "我", "", "是的，PPT已经做好了"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "很好，记得提前10分钟到"),
                MessageData(currentUserId, "我", "", "收到！"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "对了，把最新的数据也带上"),
                MessageData(currentUserId, "我", "", "好的，没问题")
            )
            "friend_002" -> listOf( // Sarah Liu
                MessageData(friend.id, friend.username, friend.avatarUrl, "Hey! 看到你的设计稿了，很棒！ 🎨"),
                MessageData(currentUserId, "我", "", "谢谢！有什么建议吗？"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "配色方案可以再大胆一些"),
                MessageData(currentUserId, "我", "", "好的，我试试看"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "期待你的新版本！"),
                MessageData(currentUserId, "我", "", "周五之前给你 ✨")
            )
            "friend_003" -> listOf( // 张工程师
                MessageData(currentUserId, "我", "", "那个 bug 修好了吗？"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "正在处理，有点复杂"),
                MessageData(currentUserId, "我", "", "需要帮忙吗？"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "暂时不用，我再研究研究"),
                MessageData(currentUserId, "我", "", "好的，有需要随时说"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "👍")
            )
            "friend_004" -> listOf( // Lisa Chen
                MessageData(friend.id, friend.username, friend.avatarUrl, "项目进度更新了"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "请查看邮件里的详细报告"),
                MessageData(currentUserId, "我", "", "收到，我看看"),
                MessageData(currentUserId, "我", "", "整体进度不错啊"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "是的，基本符合预期 📊")
            )
            "friend_005" -> listOf( // Mike Developer
                MessageData(friend.id, friend.username, friend.avatarUrl, "用过这个框架吗？"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "[分享链接]"),
                MessageData(currentUserId, "我", "", "看着不错，准备在新项目中试试"),
                MessageData(friend.id, friend.username, friend.avatarUrl, "性能很好，推荐 🚀"),
                MessageData(currentUserId, "我", "", "谢谢推荐！")
            )
            else -> listOf(
                MessageData(friend.id, friend.username, friend.avatarUrl, "Hello!"),
                MessageData(currentUserId, "我", "", "Hi there!")
            )
        }
    }

    /**
     * 获取每个好友的最后一条消息
     */
    private fun getLastMessageForFriend(friendId: String): String {
        return when (friendId) {
            "friend_001" -> "好的，没问题"
            "friend_002" -> "周五之前给你 ✨"
            "friend_003" -> "👍"
            "friend_004" -> "是的，基本符合预期 📊"
            "friend_005" -> "谢谢推荐！"
            else -> "Hi there!"
        }
    }

    /**
     * 获取每个好友的最后消息时间（匹配截图中的时间）
     */
    private fun getLastMessageTimeForFriend(friendId: String): Long {
        return when (friendId) {
            "friend_001" -> System.currentTimeMillis() - 7200000L  // 2:32 PM (2小时前)
            "friend_002" -> System.currentTimeMillis() - 14400000L // 1:15 PM (4小时前)
            "friend_003" -> System.currentTimeMillis() - 28800000L // 11:20 AM (8小时前)
            "friend_004" -> System.currentTimeMillis() - 86400000L // Yesterday
            "friend_005" -> System.currentTimeMillis() - 86400000L // Yesterday
            else -> System.currentTimeMillis()
        }
    }

    /**
     * 获取每个好友的未读消息数（匹配截图）
     */
    private fun getUnreadCountForFriend(friendId: String): Int {
        return when (friendId) {
            "friend_001" -> 3 // Product Design Team - 3条未读
            "friend_002" -> 1 // Manager Wang - 1条未读
            "friend_003" -> 0 // Dev Team Weekly - 已读
            "friend_004" -> 0 // Sarah Liu - 已读
            "friend_005" -> 5 // Tech Discussion - 5条未读
            else -> 0
        }
    }

    /**
     * 为用户创建测试群组
     */
    private suspend fun createTestGroupForUser(
        currentUserId: String,
        members: List<User>
    ): Result<Group> = runCatching {
        val groupId = firestore.collection("groups").document().id

        val group = Group(
            id = groupId,
            name = "Product Design Team",
            description = "产品设计团队内部讨论组",
            avatarUrl = "https://api.dicebear.com/7.x/identicon/png?seed=ProductDesignTeam",
            ownerId = currentUserId,
            adminIds = listOf(currentUserId),
            memberIds = listOf(currentUserId) + members.map { it.id },
            maxMembers = 500,
            createdAt = System.currentTimeMillis() - 15 * 86400000L,
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
            lastMessage = "John: Updated the design files",
            lastMessageTime = System.currentTimeMillis() - 3600000L, // 1小时前
            unreadCounts = mapOf(currentUserId to 3) + members.associate { it.id to 0 },
            createdAt = group.createdAt,
            createdBy = currentUserId,
            isActive = true
        )

        firestore.collection("conversations")
            .document(groupId)
            .set(conversation.toMap())
            .await()

        // 创建一些群消息
        val groupMessages = listOf(
            MessageData("system", "系统", "", "欢迎加入 Product Design Team！"),
            MessageData(members[0].id, members[0].username, members[0].avatarUrl, "大家好！"),
            MessageData(currentUserId, "我", "", "欢迎欢迎！"),
            MessageData(members[1].id, members[1].username, members[1].avatarUrl, "新设计稿已经上传到云盘了"),
            MessageData(members[2].id, members[2].username, members[2].avatarUrl, "收到，我看看"),
            MessageData(members[0].id, "John", members[0].avatarUrl, "Updated the design files")
        )

        for ((index, messageData) in groupMessages.withIndex()) {
            val message = Message(
                id = "",
                conversationId = groupId,
                senderId = messageData.senderId,
                senderName = messageData.senderName,
                senderAvatarUrl = messageData.avatarUrl,
                content = messageData.content,
                type = if (messageData.senderId == "system") MessageType.SYSTEM else MessageType.TEXT,
                mediaUrl = "",
                timestamp = System.currentTimeMillis() - ((groupMessages.size - index) * 1800000L), // 每条消息间隔30分钟
                readBy = if (index < groupMessages.size - 3) group.memberIds else listOf(messageData.senderId),
                isDeleted = false
            )

            firestore.collection("conversations")
                .document(groupId)
                .collection("messages")
                .add(message.toMap())
                .await()
        }

        Log.d(TAG, "Group created: ${group.name}")
        group
    }

    /**
     * 为指定用户添加好友并创建空对话（双向）
     * Add a friend for specified user and create empty conversation (bidirectional)
     *
     * 重要：实现双向好友关系，两个用户可以在不同设备上进行对话测试
     * Important: Implements bidirectional friendship, allowing conversation testing on different devices
     *
     * @param currentUserId 当前用户ID (lf1991@bu.edu的ID)
     * @param friendUserId 好友用户ID
     */
    suspend fun addFriendAndCreateEmptyConversation(
        currentUserId: String,
        friendUserId: String
    ): Result<String> = runCatching {
        Log.d(TAG, "开始添加双向好友关系: currentUserId=$currentUserId, friendUserId=$friendUserId")

        // 1. 检查并获取当前用户信息
        val currentUserDoc = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        val currentUser = if (currentUserDoc.exists()) {
            User(
                id = currentUserId,
                username = currentUserDoc.getString("username") ?: "User_${currentUserId.take(6)}",
                email = currentUserDoc.getString("email") ?: "",
                avatarUrl = currentUserDoc.getString("avatarUrl") ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$currentUserId",
                status = UserStatus.valueOf(currentUserDoc.getString("status") ?: "OFFLINE"),
                statusMessage = currentUserDoc.getString("statusMessage") ?: "",
                createdAt = currentUserDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                lastSeenAt = currentUserDoc.getLong("lastSeenAt") ?: System.currentTimeMillis()
            )
        } else {
            throw Exception("当前用户不存在: $currentUserId")
        }

        // 2. 检查好友用户是否存在，如果不存在则创建
        val friendDoc = firestore.collection("users")
            .document(friendUserId)
            .get()
            .await()

        val friend = if (friendDoc.exists()) {
            // 好友已存在，读取信息
            User(
                id = friendUserId,
                username = friendDoc.getString("username") ?: "User_${friendUserId.take(6)}",
                email = friendDoc.getString("email") ?: "",
                avatarUrl = friendDoc.getString("avatarUrl") ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$friendUserId",
                status = UserStatus.valueOf(friendDoc.getString("status") ?: "OFFLINE"),
                statusMessage = friendDoc.getString("statusMessage") ?: "",
                createdAt = friendDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                lastSeenAt = friendDoc.getLong("lastSeenAt") ?: System.currentTimeMillis()
            )
        } else {
            // 好友不存在，创建新用户
            val newFriend = User(
                id = friendUserId,
                username = "User_${friendUserId.take(6)}",
                email = "user_${friendUserId.take(6)}@example.com",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$friendUserId",
                status = UserStatus.OFFLINE,
                statusMessage = "Hey there! I'm using Micro Chat",
                createdAt = System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis()
            )

            // 写入Firebase
            firestore.collection("users")
                .document(friendUserId)
                .set(newFriend.toMap())
                .await()

            Log.d(TAG, "创建了新用户: ${newFriend.username}")
            newFriend
        }

        // 3. 创建共享的空对话（两个用户共用同一个对话）
        val conversationId = firestore.collection("conversations").document().id

        val conversation = Conversation(
            id = conversationId,
            type = ConversationType.PRIVATE,
            name = "", // 私聊不需要名称，会根据当前用户动态显示
            avatarUrl = "", // 私聊不需要固定头像
            participants = listOf(currentUserId, friendUserId),
            lastMessage = "", // 空消息
            lastMessageTime = System.currentTimeMillis(), // 当前时间
            unreadCounts = mapOf(
                currentUserId to 0,
                friendUserId to 0
            ),
            createdAt = System.currentTimeMillis(),
            createdBy = currentUserId,
            isActive = true
        )

        firestore.collection("conversations")
            .document(conversationId)
            .set(conversation.toMap())
            .await()

        Log.d(TAG, "创建了共享空对话: conversationId=$conversationId")

        // 4. 在当前用户的联系人列表中添加好友
        val contactForCurrentUser = Contact(
            userId = currentUserId,
            contactId = friendUserId,
            contactName = friend.username,
            contactAvatarUrl = friend.avatarUrl,
            alias = "", // 不设置备注
            tags = emptyList(),
            isFavorite = false,
            isBlocked = false,
            addedAt = System.currentTimeMillis(),
            conversationId = conversationId // 共享对话ID
        )

        firestore.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .document(friendUserId)
            .set(contactForCurrentUser.toMap())
            .await()

        Log.d(TAG, "已将 ${friend.username} 添加到 ${currentUser.username} 的联系人列表")

        // 5. 在好友的联系人列表中添加当前用户（双向关系）
        val contactForFriend = Contact(
            userId = friendUserId,
            contactId = currentUserId,
            contactName = currentUser.username,
            contactAvatarUrl = currentUser.avatarUrl,
            alias = "", // 不设置备注
            tags = emptyList(),
            isFavorite = false,
            isBlocked = false,
            addedAt = System.currentTimeMillis(),
            conversationId = conversationId // 共享对话ID
        )

        firestore.collection("users")
            .document(friendUserId)
            .collection("contacts")
            .document(currentUserId)
            .set(contactForFriend.toMap())
            .await()

        Log.d(TAG, "已将 ${currentUser.username} 添加到 ${friend.username} 的联系人列表")

        """
            ✅ 成功创建双向好友关系并创建共享对话！
            
            👤 用户 1:
            - 用户名: ${currentUser.username}
            - 用户ID: $currentUserId
            - 邮箱: ${currentUser.email}
            
            👤 用户 2:
            - 用户名: ${friend.username}
            - 用户ID: $friendUserId
            - 邮箱: ${friend.email}
            
            💬 共享对话:
            - 对话ID: $conversationId
            - 对话类型: 私聊（双向可见）
            - 消息数: 0 (空对话)
            
            ✨ 双向关系已建立：
            • ${currentUser.username} 的联系人中有 ${friend.username}
            • ${friend.username} 的联系人中有 ${currentUser.username}
            • 两个用户共享同一个对话
            
            🎯 测试方式：
            1️⃣ 设备A：登录 ${currentUser.email}
            2️⃣ 设备B：登录 ${friend.email}
            3️⃣ 双方都能看到对话并互相发送消息
            
            现在可以在两台设备上进行对话测试了！
        """.trimIndent()
    }
}

/**
 * 消息数据辅助类
 */
private data class MessageData(
    val senderId: String,
    val senderName: String,
    val avatarUrl: String,
    val content: String
)

