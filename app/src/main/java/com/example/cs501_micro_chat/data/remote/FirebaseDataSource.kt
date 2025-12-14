/**
 * FirebaseDataSource.kt
 * 
 * Firebase Data Source - Handles all interactions with Firebase Firestore
 * 
 * Database Structure:
 *
 * /users/{userId}
 *   - User basic information
 *   /contacts/{contactId} - User's contact list
 *
 * /conversations/{conversationId}
 *   - Conversation basic information (private or group chat)
 *   /messages/{messageId} - Messages in the conversation
 *
 * /groups/{groupId}
 *   - Group detailed information
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

    // ==================== User Operations ====================

    /**
     * Create or update user information
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit> = runCatching {
        usersCollection.document(user.id).set(user.toMap()).await()
    }

    /**
     * Get user information
     */
    suspend fun getUser(userId: String): Result<User?> = runCatching {
        Log.d(TAG, "🔍 getUser: Fetching user with ID: '$userId' (length: ${userId.length})")

        val snapshot = usersCollection.document(userId).get().await()

        if (snapshot.exists()) {
            Log.d(TAG, "  ✅ User document exists in Firebase")
            Log.d(TAG, "  📄 Document data: ${snapshot.data}")

            try {
                // Manually construct User object, handle Timestamp type
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

            // Attempt to list all user IDs for debugging
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
     * Batch get user information
     * @param userIds List of user IDs
     * @return Map<userId, User?>
     */
    suspend fun getUsers(userIds: List<String>): Result<Map<String, User>> = runCatching {
        if (userIds.isEmpty()) {
            return@runCatching emptyMap()
        }

        val users = mutableMapOf<String, User>()

        // Firebase whereIn is limited to 10 elements, need to query in batches
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
     * Observe user online status
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
     * Update user online status
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

        // 1. Search by username (prefix match) - try original case
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

        // 1b. Search by username (lowercase prefix match)
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

        // 2. Search by email (prefix match)
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

        // 3. If too few results, try exact ID match
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

        // 4. If still no results, fall back to client-side case-insensitive search
        if (results.isEmpty()) {
            try {
                Log.d(TAG, "Falling back to client-side search for: $trimmedQuery")
                val allUsers = usersCollection
                    .limit(100) // Limit results for performance
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> parseUser(doc)?.copy(id = doc.id) }

                Log.d(TAG, "Client-side search: Retrieved ${allUsers.size} users")

                // Log all user IDs and usernames for debugging
                allUsers.forEach { user ->
                    Log.d(TAG, "  👤 Found user: id='${user.id}' username='${user.username}' email='${user.email}'")
                }

                // Client-side case-insensitive search
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

        // Deduplicate and sort by relevance
        results.distinctBy { it.id }
            .sortedWith(compareBy(
                // Prioritize exact username match
                { !it.username.equals(trimmedQuery, ignoreCase = true) },
                // Then exact email match
                { !it.email.equals(lowerQuery, ignoreCase = true) },
                // Then username contains search term
                { !it.username.contains(trimmedQuery, ignoreCase = true) },
                // Finally sort alphabetically by username
                { it.username.lowercase() }
            ))
            .take(10) // Return max 10 results
    }

    /**
     * Search groups by name
     */
    suspend fun searchGroups(query: String): Result<List<Group>> = runCatching {
        val results = mutableListOf<Group>()
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            return@runCatching emptyList()
        }

        Log.d(TAG, "🔍 searchGroups: query='$trimmedQuery'")

        // 1. Search by group name (prefix match)
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

        // 2. If too few results, fall back to client-side search
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

                // Client-side case-insensitive search
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

        // Deduplicate and sort by relevance
        results.distinctBy { it.id }
            .sortedWith(compareBy(
                // Prioritize exact name match
                { !it.name.equals(trimmedQuery, ignoreCase = true) },
                // Then name contains search term
                { !it.name.contains(trimmedQuery, ignoreCase = true) },
                // Finally sort alphabetically by name
                { it.name.lowercase() }
            ))
            .take(10) // Return max 10 results
    }

    /**
     * Join a group
     * Add user to group members and create contact for user
     */
    suspend fun joinGroup(groupId: String, userId: String): Result<Unit> = runCatching {
        Log.d(TAG, "🚀 joinGroup: groupId=$groupId, userId=$userId")

        // 1. Get group info
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        // 2. Check if user is already a member
        if (group.memberIds.contains(userId)) {
            Log.d(TAG, "⚠️ User $userId is already a member of group $groupId")
            return@runCatching // Already a member, return success
        }

        // 3. Update group member list
        val updatedMembers = group.memberIds + userId
        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated groups/$groupId/memberIds")

        // 4. Update conversation participants list
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated conversations/$groupId/participants")

        // 5. Create contact record for user
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

    // ==================== Contact Operations ====================

    /**
     * Add contact
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
     * Get contact list
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
     * Get single contact information
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
     * Observe contact list changes
     * Automatically fills in contactName and contactAvatarUrl (if empty)
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

                // Get contact basic info
                val contacts = snapshot?.documents?.mapNotNull { doc ->
                    // Print raw Firebase data for debugging
                    Log.d(TAG, "📄 Firebase Contact Document ${doc.id}:")
                    Log.d(TAG, "  - Raw data: ${doc.data}")
                    Log.d(TAG, "  - isNew: ${doc.get("isNew")} (${doc.get("isNew")?.javaClass?.simpleName})")
                    Log.d(TAG, "  - isPending: ${doc.get("isPending")} (${doc.get("isPending")?.javaClass?.simpleName})")

                    // Manually construct Contact object to ensure isPending field is read correctly
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

                            // Check if missing fields
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

                // Asynchronously enrich each contact's details (if missing)
                scope.launch {
                    try {
                        Log.d(TAG, "🔧 Starting contact enrichment for ${contacts.size} contacts")
                        val enrichedContacts = contacts.map { contact ->
                            Log.d(TAG, "  📝 Before enrichment - ${contact.contactId}: isNew=${contact.isNew}, isPending=${contact.isPending}")

                            // If contactName or contactAvatarUrl is empty, get from corresponding user
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
     * Delete contact
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
     * Remove contact (alias, same as deleteContact)
     */
    suspend fun removeContact(userId: String, contactId: String): Result<Unit> = deleteContact(userId, contactId)

    /**
     * Update contact alias
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
     * Update contact favorite status
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
     * Observe pinned conversations
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
     * Set pinned status
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
     * Check if conversation is pinned
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
     * Set conversation blocked status for a participant
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
     * Check if user is blocked
     */
    suspend fun isConversationParticipantBlocked(conversationId: String, participantId: String): Result<Boolean> = runCatching {
        val snapshot = conversationsCollection
            .document(conversationId)
            .get()
            .await()
        val blockedMap = snapshot.get("blockedParticipants") as? Map<*, *> ?: emptyMap<String, Boolean>()
        blockedMap[participantId] == true
    }

    // ==================== Conversation Operations ====================

    /**
     * Create or get private conversation
     *
     * Optimized logic:
     * 1. Check if there's already a conversation with otherUserId in currentUserId's contacts
     * 2. If found, return that conversation
     * 3. If not found, create new conversation and add contact info to currentUserId's contacts
     */
    suspend fun createOrGetPrivateConversation(currentUserId: String, otherUserId: String): Result<Conversation> = runCatching {
        Log.d(TAG, "createOrGetPrivateConversation: currentUser=$currentUserId, otherUser=$otherUserId")

        // 1. Check if there's already a contact with otherUserId in current user's contacts
        val contactDoc = usersCollection
            .document(currentUserId)
            .collection("contacts")
            .document(otherUserId)
            .get()
            .await()

        val existingContact = contactDoc.toObject(Contact::class.java)

        // 2. If contact exists and has conversationId, get that conversation
        if (existingContact != null && existingContact.conversationId.isNotEmpty()) {
            Log.d(TAG, "Found existing conversation: ${existingContact.conversationId}")
            val conversation = getConversation(existingContact.conversationId).getOrNull()
            if (conversation != null) {
                return@runCatching conversation
            }
            Log.w(TAG, "Conversation ${existingContact.conversationId} not found in Firestore, creating new one")
        }

        // 3. If no conversation found, create new one
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

        // Save conversation to Firestore
        conversationsCollection.document(conversationId).set(conversation.toMap()).await()
        Log.d(TAG, "Conversation created: $conversationId")

        // 4. Add contact info to both users' contacts (bidirectional relationship)
        val currentTime = System.currentTimeMillis()

        // Get both users' details
        val currentUser = getUser(currentUserId).getOrNull()

        // Add otherUserId as contact for currentUserId
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

        // Add currentUserId as contact for otherUserId
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
     * Create group conversation
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
     * Get user's conversation list (including private and group conversations)
     * Gets conversationId from user's contact list instead of querying all conversations
     *
     * Supports:
     * - Private contact conversations (type = "PRIVATE")
     * - Group conversations (type = "GROUP")
     */
    suspend fun getUserConversations(userId: String): Result<List<Conversation>> = runCatching {
        // 1. Get all contacts for user (including private and group)
        val contacts = getContacts(userId).getOrNull() ?: emptyList()

        // 2. Filter confirmed contacts
        // Only keep: isNew = false && isPending = false contacts and groups
        val confirmedContacts = contacts.filter { contact ->
            // Groups are always shown
            if (contact.type == "GROUP") {
                true
            } else {
                // Private contacts: must be confirmed friends (isNew = false && isPending = false)
                // isNew = false, isPending = true: request sent, waiting for acceptance → don't show
                // isNew = true, isPending = false: received request, waiting for me to accept → don't show
                // isNew = false, isPending = false: confirmed friends → show
                !contact.isNew && !contact.isPending
            }
        }

        Log.d(TAG, "getUserConversations: Total contacts: ${contacts.size}, Confirmed: ${confirmedContacts.size}, Pending: ${contacts.size - confirmedContacts.size}")

        // 3. Extract all conversationIds (filter out empty ones)
        // Both private contacts and groups have conversationId field
        val conversationIds = confirmedContacts.mapNotNull { it.conversationId.takeIf { id -> id.isNotEmpty() } }

        Log.d(TAG, "getUserConversations: Found ${conversationIds.size} conversation IDs from confirmed contacts (including groups)")

        // 4. If no conversations, return empty list
        if (conversationIds.isEmpty()) {
            return@runCatching emptyList()
        }

        // 5. Batch get conversation info
        // Firebase whereIn is limited to 10 elements, need to query in batches
        val conversations = mutableListOf<Conversation>()
        conversationIds.chunked(10).forEach { chunk ->
            val snapshot = conversationsCollection
                .whereIn("__name__", chunk) // Query by document ID
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents.mapNotNullTo(conversations) { doc ->
                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
            }
        }

        Log.d(TAG, "getUserConversations: Retrieved ${conversations.size} conversations from Firestore")

        // 6. Sort by last message time
        conversations.sortedByDescending { it.lastMessageTime }
    }

    /**
     * Observe user's conversation list (including private and group conversations)
     * Gets conversationId from user's contact list instead of querying all conversations
     *
     * Supports:
     * - Private contact conversations (type = "PRIVATE")
     * - Group conversations (type = "GROUP")
     *
     * Note: This method first observes contact list changes, then observes corresponding conversations
     */
    fun observeUserConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        // Observe user's contact list (including private contacts and groups)
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

                // Manually construct Contact objects to ensure isPending and isNew fields are read correctly
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

                // Detailed log: show all contacts' status
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

                // Filter confirmed contacts
                // Only keep: isNew = false && isPending = false contacts and groups
                val confirmedContacts = contacts.filter { contact ->
                    // Groups are always shown
                    if (contact.type == "GROUP") {
                        true
                    } else {
                        // Private contacts: must be confirmed friends (isNew = false && isPending = false)
                        !contact.isNew && !contact.isPending
                    }
                }

                Log.d(TAG, "observeUserConversations: Total contacts: ${contacts.size}, Confirmed: ${confirmedContacts.size}, Pending: ${contacts.size - confirmedContacts.size}")

                // Extract all conversationIds (both private and group)
                val conversationIds = confirmedContacts.mapNotNull {
                    it.conversationId.takeIf { id -> id.isNotEmpty() }
                }

                Log.d(TAG, "observeUserConversations: Found ${conversationIds.size} conversation IDs from confirmed contacts (including groups)")
                Log.d(TAG, "observeUserConversations: Contacts breakdown - ${confirmedContacts.count { it.type == "GROUP" }} groups, ${confirmedContacts.count { it.type == "PRIVATE" }} private")

                if (conversationIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Observe these conversations' changes
                // Since Firebase doesn't support directly observing multiple document IDs, we need to batch observe or use combined queries
                // Here we use a simple approach: query all matching conversations
                conversationsCollection
                    .whereEqualTo("isActive", true)
                    .addSnapshotListener { conversationsSnapshot, conversationsError ->
                        if (conversationsError != null) {
                            Log.e(TAG, "Error observing conversations", conversationsError)
                            close(conversationsError)
                            return@addSnapshotListener
                        }

                        // Filter conversations belonging to user (both private and group)
                        val allConversations = conversationsSnapshot?.documents?.mapNotNull { doc ->
                            if (conversationIds.contains(doc.id)) {
                                doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                            } else null
                        } ?: emptyList()

                        Log.d(TAG, "observeUserConversations: Received ${allConversations.size} conversations from Firestore")
                        Log.d(TAG, "observeUserConversations: Breakdown - ${allConversations.count { it.type == ConversationType.GROUP }} groups, ${allConversations.count { it.type == ConversationType.PRIVATE }} private")

                        // Sort by time on client side
                        val sortedConversations = allConversations.sortedByDescending { it.lastMessageTime }
                        trySend(sortedConversations)
                    }
            }

        awaitClose {
            // Note: only remove the outermost listener here
            // Inner listeners will be automatically updated when outer listener fires
            contactsListener.remove()
        }
    }

    /**
     * Get single conversation info
     */
    suspend fun getConversation(conversationId: String): Result<Conversation?> = runCatching {
        val doc = conversationsCollection
            .document(conversationId)
            .get()
            .await()

        // Manually map and set id
        doc.toObject(Conversation::class.java)?.copy(id = doc.id)
    }

    /**
     * Generate new conversation ID
     */
    fun generateConversationId(): String {
        return conversationsCollection.document().id
    }

    /**
     * Create conversation (without auto-creating contacts)
     */
    suspend fun createConversation(conversation: Conversation): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversation.id)
            .set(conversation.toMap())
            .await()
    }

    /**
     * Update conversation info
     */
    suspend fun updateConversation(conversation: Conversation): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversation.id)
            .set(conversation.toMap())
            .await()
    }

    /**
     * Delete conversation (mark as inactive)
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversationId)
            .update("isActive", false)
            .await()
    }

    // ==================== Message Operations ====================

    /**
     * Send message
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
     * Get message list for conversation
     */
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>> = runCatching {
        val snapshot = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        // Manually map to ensure id field is correctly set to document ID
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Message::class.java)?.copy(id = doc.id)
        }.reversed()
    }

    /**
     * Observe new messages in conversation
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
                // Manually map to ensure id field is correctly set to document ID
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Mark message as read
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
     * Clear conversation unread count
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
     * Delete message
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
     * Record timestamp when current user clears chat (one-way clear)
     */
    suspend fun clearConversationForUser(conversationId: String, userId: String, clearedAt: Long): Result<Unit> = runCatching {
        conversationsCollection
            .document(conversationId)
            .update("clearedAt.$userId", clearedAt)
            .await()
    }

    // ==================== Group Operations ====================

    /**
     * Create group
     * 1. Generate groupId and conversationId
     * 2. Create group record (including conversationId)
     * 3. Create group conversation (using groupId as conversationId)
     * 4. Create contacts record for all members
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
     * Get group info
     */
    suspend fun getGroup(groupId: String): Result<Group?> = runCatching {
        groupsCollection
            .document(groupId)
            .get()
            .await()
            .toObject(Group::class.java)
    }

    /**
     * Update group info
     */
    suspend fun updateGroup(group: Group): Result<Unit> = runCatching {
        groupsCollection
            .document(group.id)
            .set(group.toMap())
            .await()
    }

    /**
     * Add group members
     */
    suspend fun addGroupMembers(groupId: String, memberIds: List<String>): Result<Unit> = runCatching {
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")
        val updatedMembers = (group.memberIds + memberIds).distinct()

        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()

        // Update corresponding conversation participants list
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
    }

    /**
     * Remove group member
     */
    suspend fun removeGroupMember(groupId: String, memberId: String): Result<Unit> = runCatching {
        Log.d(TAG, "🚪 removeGroupMember: groupId=$groupId, memberId=$memberId")

        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        // Check if member exists
        if (!group.memberIds.contains(memberId)) {
            Log.w(TAG, "⚠️ User $memberId is not a member of group $groupId")
            return@runCatching  // Not a member, return success
        }

        val updatedMembers = group.memberIds.filter { it != memberId }
        Log.d(TAG, "📝 Updating group members: ${group.memberIds.size} -> ${updatedMembers.size}")

        // 1. Update group member list
        groupsCollection
            .document(groupId)
            .update("memberIds", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated groups/$groupId/memberIds")

        // 2. Update corresponding conversation participants list
        conversationsCollection
            .document(groupId)
            .update("participants", updatedMembers)
            .await()
        Log.d(TAG, "✅ Updated conversations/$groupId/participants")

        // 3. Delete group from user's contacts
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
     * Transfer group ownership
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
     * Dismiss group
     * Completely delete group:
     * 1. Delete group from all members' contacts
     * 2. Delete group itself (groups/{groupId})
     * 3. Delete bound conversation and its messages (conversations/{groupId})
     */
    suspend fun dismissGroup(groupId: String): Result<Unit> = runCatching {
        Log.d(TAG, "💥 dismissGroup: groupId=$groupId")

        // 1. Get group info and member list
        val group = getGroup(groupId).getOrNull() ?: throw Exception("Group not found")

        Log.d(TAG, "📝 Group has ${group.memberIds.size} members to remove contacts")

        // 2. Delete group from all members' contacts
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
                // Continue processing other members, don't interrupt due to one failure
            }
        }

        // 3. Delete all messages in conversation
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

        // 4. Delete conversation itself
        try {
            conversationsCollection
                .document(groupId)
                .delete()
                .await()
            Log.d(TAG, "✅ Deleted conversation: conversations/$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to delete conversation $groupId", e)
        }

        // 5. Delete group itself
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
