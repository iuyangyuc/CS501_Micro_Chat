/**
 * FirebaseInitializer.kt
 *
 * Firebase Database Initializer - Used to create test data and initialize database structure
 *
 * Note: Most test data initialization functions have been removed after initial setup.
 * Only essential functions are retained.
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
     * Initialize current logged-in user's data
     * This may be needed when a new user registers
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
     * Add a friend for specified user and create empty conversation (bidirectional)
     *
     * Important: Implements bidirectional friendship, allowing conversation testing on different devices
     *
     * @param currentUserId Current user ID
     * @param friendUserId Friend user ID
     */
    suspend fun addFriendAndCreateEmptyConversation(
        currentUserId: String,
        friendUserId: String
    ): Result<String> = runCatching {
        Log.d(TAG, "Adding bidirectional friend relationship: currentUserId=$currentUserId, friendUserId=$friendUserId")

        // 1. Check and get current user info
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
            throw Exception("Current user does not exist: $currentUserId")
        }

        // 2. Check if friend user exists, create if not
        val friendDoc = firestore.collection("users")
            .document(friendUserId)
            .get()
            .await()

        val friend = if (friendDoc.exists()) {
            // Friend exists, read info
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
            // Friend doesn't exist, create new user
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

            // Write to Firebase
            firestore.collection("users")
                .document(friendUserId)
                .set(newFriend.toMap())
                .await()

            Log.d(TAG, "Created new user: ${newFriend.username}")
            newFriend
        }

        // 3. Create shared empty conversation (both users share the same conversation)
        val conversationId = firestore.collection("conversations").document().id

        val conversation = Conversation(
            id = conversationId,
            type = ConversationType.PRIVATE,
            name = "", // Private chat doesn't need a name, will display dynamically based on current user
            avatarUrl = "", // Private chat doesn't need a fixed avatar
            participants = listOf(currentUserId, friendUserId),
            lastMessage = "", // Empty message
            lastMessageTime = System.currentTimeMillis(), // Current time
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

        Log.d(TAG, "Created shared empty conversation: conversationId=$conversationId")

        // 4. Add friend to current user's contact list
        val contactForCurrentUser = Contact(
            userId = currentUserId,
            contactId = friendUserId,
            contactName = friend.username,
            contactAvatarUrl = friend.avatarUrl,
            alias = "", // No alias set
            tags = emptyList(),
            isFavorite = false,
            isBlocked = false,
            addedAt = System.currentTimeMillis(),
            conversationId = conversationId // Shared conversation ID
        )

        firestore.collection("users")
            .document(currentUserId)
            .collection("contacts")
            .document(friendUserId)
            .set(contactForCurrentUser.toMap())
            .await()

        Log.d(TAG, "Added ${friend.username} to ${currentUser.username}'s contact list")

        // 5. Add current user to friend's contact list (bidirectional relationship)
        val contactForFriend = Contact(
            userId = friendUserId,
            contactId = currentUserId,
            contactName = currentUser.username,
            contactAvatarUrl = currentUser.avatarUrl,
            alias = "", // No alias set
            tags = emptyList(),
            isFavorite = false,
            isBlocked = false,
            addedAt = System.currentTimeMillis(),
            conversationId = conversationId // Shared conversation ID
        )

        firestore.collection("users")
            .document(friendUserId)
            .collection("contacts")
            .document(currentUserId)
            .set(contactForFriend.toMap())
            .await()

        Log.d(TAG, "Added ${currentUser.username} to ${friend.username}'s contact list")

        """
            ✅ Successfully created bidirectional friend relationship and shared conversation!
            
            👤 User 1:
            - Username: ${currentUser.username}
            - User ID: $currentUserId
            - Email: ${currentUser.email}
            
            👤 User 2:
            - Username: ${friend.username}
            - User ID: $friendUserId
            - Email: ${friend.email}
            
            💬 Shared Conversation:
            - Conversation ID: $conversationId
            - Type: Private (bidirectional)
            - Messages: 0 (empty conversation)
        """.trimIndent()
    }
}
