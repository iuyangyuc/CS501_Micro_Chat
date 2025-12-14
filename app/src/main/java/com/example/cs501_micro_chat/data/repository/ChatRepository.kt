/**
 * ChatRepository.kt
 *
 * Chat Repository - Handles chat-related business logic
 *
 * Responsibilities:
 * - Conversation management (create, get, update, delete)
 * - Message management (send, receive, mark as read)
 * - Group management (create, update, member management)
 * - Contact management
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

    // ==================== Conversation Operations ====================

    /**
     * Get all conversations for the user
     */
    suspend fun getUserConversations(): Result<List<Conversation>> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getUserConversations(userId)
    }

    /**
     * Observe user's conversation list
     */
    fun observeUserConversations(): Flow<List<Conversation>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observeUserConversations(userId)
    }

    /**
     * Create or get private conversation with another user
     */
    suspend fun createOrGetPrivateConversation(otherUserId: String): Result<Conversation> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.createOrGetPrivateConversation(userId, otherUserId)
    }

    /**
     * Get conversation info
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?> {
        return firebaseDataSource.getConversation(conversationId)
    }

    /**
     * Update conversation info
     */
    suspend fun updateConversation(conversation: Conversation): Result<Unit> {
        return firebaseDataSource.updateConversation(conversation)
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return firebaseDataSource.deleteConversation(conversationId)
    }

    // ==================== Message Operations ====================

    /**
     * Send message
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = ""
    ): Result<Message> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        val isBlocked = firebaseDataSource.isConversationParticipantBlocked(conversationId, userId).getOrElse { false }

        // Get current user info
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
            status = if (isBlocked) MessageStatus.FAILED else MessageStatus.SENT
        )

        if (isBlocked) {
            val localId = "local_${userId}_${message.timestamp}"
            return Result.success(message.copy(id = localId, status = MessageStatus.FAILED))
        }

        return runCatching {
            Log.d("ChatRepository", "sendMessage convo=$conversationId type=${type.name} media=${mediaUrl.isNotBlank()}")
            firebaseDataSource.sendMessage(message).getOrThrow()
        }.onFailure {
            Log.e("ChatRepository", "sendMessage failed convo=$conversationId type=${type.name}", it)
        }
    }

    /**
     * Get message list for a conversation
     */
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>> {
        return firebaseDataSource.getMessages(conversationId, limit)
    }

    /**
     * Observe new messages in a conversation
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return firebaseDataSource.observeMessages(conversationId)
    }

    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(conversationId: String, messageId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.markMessageAsRead(conversationId, messageId, userId)
    }

    /**
     * Clear unread count for a conversation
     */
    suspend fun clearUnreadCount(conversationId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.clearUnreadCount(conversationId, userId)
    }

    /**
     * Delete message
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return firebaseDataSource.deleteMessage(conversationId, messageId)
    }

    /**
     * Record timestamp when current user clears chat (one-way clear)
     */
    suspend fun clearConversationForCurrentUser(conversationId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.clearConversationForUser(conversationId, userId, System.currentTimeMillis())
    }

    fun currentUserIdOrNull(): String? = currentUserId

    // ==================== Group Operations ====================

    /**
     * Create group
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
            memberIds = memberIds + userId // Ensure creator is also in member list
        )

        return firebaseDataSource.createGroup(group)
    }

    /**
     * Get group info
     */
    suspend fun getGroup(groupId: String): Result<Group?> {
        return firebaseDataSource.getGroup(groupId)
    }

    /**
     * Update group info
     */
    suspend fun updateGroup(group: Group): Result<Unit> {
        return firebaseDataSource.updateGroup(group)
    }

    /**
     * Add group members
     */
    suspend fun addGroupMembers(groupId: String, memberIds: List<String>): Result<Unit> {
        return firebaseDataSource.addGroupMembers(groupId, memberIds)
    }

    /**
     * Remove group member
     */
    suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> {
        return firebaseDataSource.removeGroupMember(groupId, memberId)
    }

    /**
     * Leave group
     */
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.removeGroupMember(groupId, userId)
    }

    /**
     * Transfer group ownership
     */
    suspend fun transferGroupOwnership(groupId: String, newOwnerId: String): Result<Unit> {
        return firebaseDataSource.transferGroupOwnership(groupId, newOwnerId)
    }

    /**
     * Dismiss group
     */
    suspend fun dismissGroup(groupId: String): Result<Unit> {
        return firebaseDataSource.dismissGroup(groupId)
    }

    // ==================== Contact Operations ====================

    /**
     * Send friend request
     * 1. Check if there's an old conversationId in both users' contacts
     * 2. If found, preserve the old conversationId in the new contact
     * 3. Add current user to target user's contacts with isNew = true (pending confirmation)
     * 4. Add target user to current user's contacts with isPending = true (sent, waiting for acceptance)
     *
     * Note: Even if conversationId is preserved, conversation won't show in chat list because isNew or isPending is true
     */
    suspend fun sendFriendRequest(targetUserId: String, alias: String = ""): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        try {
            Log.d("ChatRepository", "🔄 sendFriendRequest: currentUser=$userId, targetUser=$targetUserId")

            // Get current user info
            val currentUserResult = firebaseDataSource.getUser(userId)
            val currentUser = currentUserResult.getOrNull()
            if (currentUser == null) {
                Log.e("ChatRepository", "❌ Current user not found: $userId")
                return Result.failure(Exception("Current user not found"))
            }
            Log.d("ChatRepository", "✅ Current user found: ${currentUser.username} (${currentUser.id})")

            // Get target user info
            val targetUserResult = firebaseDataSource.getUser(targetUserId)
            val targetUser = targetUserResult.getOrNull()
            if (targetUser == null) {
                Log.e("ChatRepository", "❌ Target user not found: $targetUserId")
                return Result.failure(Exception("Target user not found"))
            }
            Log.d("ChatRepository", "✅ Target user found: ${targetUser.username} (${targetUser.id})")

            // Key step: Check for old conversationId in both users' contacts
            Log.d("ChatRepository", "  🔍 Checking for old conversationId in both users' contacts...")

            // Check sender's (current user) old contact
            val senderOldContactResult = firebaseDataSource.getContact(userId, targetUserId)
            val senderOldConversationId = senderOldContactResult.getOrNull()?.conversationId?.takeIf { it.isNotBlank() }
            Log.d("ChatRepository", "    📋 Sender's old conversationId: ${senderOldConversationId ?: "null"}")

            // Check receiver's (target user) old contact
            val receiverOldContactResult = firebaseDataSource.getContact(targetUserId, userId)
            val receiverOldConversationId = receiverOldContactResult.getOrNull()?.conversationId?.takeIf { it.isNotBlank() }
            Log.d("ChatRepository", "    📋 Receiver's old conversationId: ${receiverOldConversationId ?: "null"}")

            // Prefer any preserved old conversationId
            val existingConversationId = senderOldConversationId ?: receiverOldConversationId ?: ""

            if (existingConversationId.isNotBlank()) {
                Log.d("ChatRepository", "  ♻️ Found old conversationId: $existingConversationId, will preserve it")
            } else {
                Log.d("ChatRepository", "  🆕 No old conversationId found, will create new one on acceptance")
            }

            // Create pending contact in target user's contacts (current user)
            // Preserve old conversationId if exists
            val receiverContact = Contact(
                userId = targetUserId, // Target user's ID
                contactId = userId, // Current user's ID
                contactName = currentUser.username,
                contactAvatarUrl = currentUser.avatarUrl,
                alias = "",
                type = "PRIVATE",
                isNew = true, // Mark as pending confirmation (receiver sees this)
                isPending = false,
                conversationId = existingConversationId // Preserve old conversationId
            )
            Log.d("ChatRepository", "📝 Creating receiver contact: user=${receiverContact.userId}, contact=${receiverContact.contactId}, isNew=${receiverContact.isNew}, isPending=${receiverContact.isPending}, conversationId=$existingConversationId")

            val receiverResult = firebaseDataSource.addContact(receiverContact)
            if (receiverResult.isFailure) {
                Log.e("ChatRepository", "❌ Failed to add receiver contact", receiverResult.exceptionOrNull())
                return Result.failure(receiverResult.exceptionOrNull() ?: Exception("Failed to add receiver contact"))
            }
            Log.d("ChatRepository", "✅ Receiver contact added successfully")

            // Create sent contact in current user's contacts (target user)
            // Preserve old conversationId if exists
            val senderContact = Contact(
                userId = userId, // Current user's ID
                contactId = targetUserId, // Target user's ID
                contactName = targetUser.username,
                contactAvatarUrl = targetUser.avatarUrl,
                alias = alias,
                type = "PRIVATE",
                isNew = false,
                isPending = true, // Mark as sent waiting for acceptance (sender sees this)
                conversationId = existingConversationId // Preserve old conversationId
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
     * Accept friend request
     * 1. Get conversationId from contacts (already preserved when sending request)
     * 2. If conversationId exists, check if conversation exists, recreate if not; otherwise create new
     * 3. Set isNew = false in current user's contacts
     * 4. Set isPending = false in requester's contacts
     */
    suspend fun acceptFriendRequest(requesterId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        try {
            Log.d("ChatRepository", "🤝 Accepting friend request from: $requesterId")

            // Get requester info
            val requesterResult = firebaseDataSource.getUser(requesterId)
            val requester = requesterResult.getOrNull()
                ?: return Result.failure(Exception("Requester not found"))

            // Get current user info
            val currentUserResult = firebaseDataSource.getUser(userId)
            val currentUser = currentUserResult.getOrNull()
                ?: return Result.failure(Exception("Current user not found"))

            // Get conversationId from current contacts (preserved during sendFriendRequest)
            Log.d("ChatRepository", "  🔍 Checking conversationId from contacts...")

            val receiverContactResult = firebaseDataSource.getContact(userId, requesterId)
            val receiverConversationId = receiverContactResult.getOrNull()?.conversationId
            Log.d("ChatRepository", "    📋 Receiver's conversationId: ${receiverConversationId ?: "null"}")

            val requesterContactResult = firebaseDataSource.getContact(requesterId, userId)
            val requesterConversationId = requesterContactResult.getOrNull()?.conversationId
            Log.d("ChatRepository", "    📋 Requester's conversationId: ${requesterConversationId ?: "null"}")

            // Use preserved conversationId
            val existingConversationId = receiverConversationId?.takeIf { it.isNotBlank() }
                ?: requesterConversationId?.takeIf { it.isNotBlank() }

            val conversationId: String

            if (!existingConversationId.isNullOrBlank()) {
                // Use preserved conversationId
                Log.d("ChatRepository", "  ♻️ Using preserved conversationId: $existingConversationId")
                conversationId = existingConversationId

                // Check if conversation still exists
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
                // First time adding friend, create new conversation
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

            // Update current user's contact (set isNew = false, ensure conversationId exists)
            Log.d("ChatRepository", "  📝 Updating current user's contact")
            val myContact = Contact(
                userId = userId,
                contactId = requesterId,
                contactName = requester.username,
                contactAvatarUrl = requester.avatarUrl,
                type = "PRIVATE",
                isNew = false, // From true to false
                isPending = false,
                conversationId = conversationId
            )
            firebaseDataSource.addContact(myContact).getOrThrow()

            // Update requester's contact (set isPending = false, ensure conversationId exists)
            Log.d("ChatRepository", "  📝 Updating requester's contact")
            val theirContact = Contact(
                userId = requesterId,
                contactId = userId,
                contactName = currentUser.username,
                contactAvatarUrl = currentUser.avatarUrl,
                type = "PRIVATE",
                isNew = false,
                isPending = false, // From true to false
                conversationId = conversationId
            )
            firebaseDataSource.addContact(theirContact).getOrThrow()

            // Unblock both parties if previously blocked
            firebaseDataSource.setConversationParticipantBlocked(conversationId, userId, false)
            firebaseDataSource.setConversationParticipantBlocked(conversationId, requesterId, false)

            Log.d("ChatRepository", "  🎉 Friend request accepted successfully with conversationId: $conversationId")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "  ❌ Failed to accept friend request", e)
            return Result.failure(e)
        }
    }

    /**
     * Reject friend request
     * Delete contact records from both users to restore unknown state
     */
    suspend fun rejectFriendRequest(requesterId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        Log.d("ChatRepository", "🚫 Rejecting friend request from: $requesterId")

        try {
            // Remove contact from receiver (current user)
            Log.d("ChatRepository", "  🗑️ Removing contact from receiver: /users/$userId/contacts/$requesterId")
            firebaseDataSource.removeContact(userId, requesterId).getOrThrow()
            Log.d("ChatRepository", "  ✅ Receiver contact removed")

            // Remove contact from sender
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
     * Add contact (legacy version, kept for compatibility)
     */
    suspend fun addContact(contactId: String, alias: String = ""): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        // Get contact user info
        val contactUserResult = firebaseDataSource.getUser(contactId)
        val contactUser = contactUserResult.getOrNull()
            ?: return Result.failure(Exception("Contact user not found"))

        // Create or get private conversation
        val conversationResult = createOrGetPrivateConversation(contactId)
        val conversation = conversationResult.getOrNull()
            ?: return Result.failure(Exception("Failed to create conversation"))

        // Unblock both parties if previously blocked
        runCatching { firebaseDataSource.setConversationParticipantBlocked(conversation.id, userId, false) }
        runCatching { firebaseDataSource.setConversationParticipantBlocked(conversation.id, contactId, false) }

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
     * Get contact list
     */
    suspend fun getContacts(): Result<List<Contact>> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getContacts(userId)
    }

    /**
     * Get single contact
     */
    suspend fun getContact(contactId: String): Result<Contact?> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.getContact(userId, contactId)
    }

    /**
     * Observe contact list
     */
    fun observeContacts(): Flow<List<Contact>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observeContacts(userId)
    }

    /**
     * Delete contact
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
     * Update contact alias
     */
    suspend fun updateContactAlias(contactId: String, alias: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.updateContactAlias(userId, contactId, alias)
    }

    /**
     * Observe pinned conversations
     */
    fun observePinnedConversations(): Flow<Set<String>>? {
        val userId = currentUserId ?: return null
        return firebaseDataSource.observePinnedConversations(userId)
    }

    /**
     * Set pinned status
     */
    suspend fun setPinnedConversation(conversationId: String, pinned: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.setPinnedConversation(userId, conversationId, pinned)
    }

    /**
     * Check if conversation is pinned
     */
    suspend fun isConversationPinned(conversationId: String): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.isConversationPinned(userId, conversationId)
    }

    /**
     * Update contact favorite status (for syncing favorites in contact list)
     */
    suspend fun updateContactFavorite(contactId: String, isFavorite: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.updateContactFavorite(userId, contactId, isFavorite)
    }

    /**
     * Search users
     */
    suspend fun searchUsers(query: String): Result<List<User>> {
        return firebaseDataSource.searchUsers(query)
    }

    /**
     * Search groups (by group name)
     */
    suspend fun searchGroups(query: String): Result<List<com.example.cs501_micro_chat.data.model.Group>> {
        return firebaseDataSource.searchGroups(query)
    }

    /**
     * Join group
     */
    suspend fun joinGroup(groupId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))
        return firebaseDataSource.joinGroup(groupId, userId)
    }

    // ==================== User Operations ====================

    /**
     * Get user info
     */
    suspend fun getUser(userId: String): Result<User?> {
        return firebaseDataSource.getUser(userId)
    }

    /**
     * Batch get user info
     */
    suspend fun getUsers(userIds: List<String>): Result<Map<String, User>> {
        return firebaseDataSource.getUsers(userIds)
    }

}
