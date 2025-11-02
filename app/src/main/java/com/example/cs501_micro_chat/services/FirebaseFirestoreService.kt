package com.example.cs501_micro_chat.services

import com.example.cs501_micro_chat.data.model.ChatSession
import com.example.cs501_micro_chat.data.model.Message
import com.example.cs501_micro_chat.data.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin wrapper around Cloud Firestore providing suspend and Flow based helpers for core entities.
 *
 * The service keeps Firestore specific concerns (collection names, snapshot parsing, etc.) in one
 * place, leaving higher layers to work with strongly typed models.
 */
class FirebaseFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun createOrUpdateUser(user: User) {
        usersCollection().document(user.id).set(user.toFirestoreMap()).await()
    }

    suspend fun getUser(userId: String): User? {
        return usersCollection().document(userId).get().await().toUser()
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val registration = usersCollection()
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUser())
            }
        awaitClose { registration.remove() }
    }.flowOn(ioDispatcher)

    suspend fun createChatSession(session: ChatSession) {
        chatSessionsCollection().document(session.id).set(session.toFirestoreMap()).await()
    }

    suspend fun deleteChatSession(sessionId: String) {
        chatSessionsCollection().document(sessionId).delete().await()
    }

    fun observeChatSessions(): Flow<List<ChatSession>> = observeCollection(
        query = chatSessionsCollection().orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING),
        mapper = { snapshot -> snapshot.toChatSession() }
    )

    suspend fun addMessage(sessionId: String, message: Message) {
        val sanitizedMessage = message.copy(sessionId = sessionId)
        val messageReference = messagesCollection(sessionId).document(sanitizedMessage.id)
        firestore.runBatch { batch ->
            batch.set(messageReference, sanitizedMessage.toFirestoreMap())
            batch.update(
                chatSessionsCollection().document(sessionId),
                mapOf(
                    FIELD_UPDATED_AT to sanitizedMessage.timestamp,
                    FIELD_LAST_MESSAGE_PREVIEW to sanitizedMessage.text
                )
            )
        }.await()
    }

    suspend fun deleteMessage(sessionId: String, messageId: String) {
        messagesCollection(sessionId).document(messageId).delete().await()
    }

    fun observeMessages(sessionId: String): Flow<List<Message>> = observeCollection(
        query = messagesCollection(sessionId).orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING),
        mapper = { snapshot -> snapshot.toMessage(sessionId) }
    )

    private fun usersCollection(): CollectionReference =
        firestore.collection(COLLECTION_USERS)

    private fun chatSessionsCollection(): CollectionReference =
        firestore.collection(COLLECTION_CHAT_SESSIONS)

    private fun messagesCollection(sessionId: String): CollectionReference =
        chatSessionsCollection()
            .document(sessionId)
            .collection(SUBCOLLECTION_MESSAGES)

    private fun <T : Any> observeCollection(
        query: Query,
        mapper: (DocumentSnapshot) -> T?
    ): Flow<List<T>> = callbackFlow {
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents.orEmpty().mapNotNull(mapper)
            trySend(items)
        }
        awaitClose { registration.remove() }
    }.flowOn(ioDispatcher)

    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_NAME to name,
        FIELD_EMAIL to email,
        FIELD_CREATED_AT to createdAt
    )

    private fun ChatSession.toFirestoreMap(): Map<String, Any?> {
        val lastMessage = messages.lastOrNull()
        val latestTimestamp = lastMessage?.timestamp ?: System.currentTimeMillis()
        return mapOf(
            FIELD_TITLE to title,
            FIELD_UPDATED_AT to latestTimestamp,
            FIELD_LAST_MESSAGE_PREVIEW to lastMessage?.text
        )
    }

    private fun Message.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_SESSION_ID to sessionId,
        FIELD_SENDER_ID to senderId,
        FIELD_TEXT to text,
        FIELD_TIMESTAMP to timestamp,
        FIELD_IS_OUTGOING to isOutgoing
    )

    private fun DocumentSnapshot.toUser(): User? {
        val data = data ?: return null
        return User(
            id = id,
            name = data[FIELD_NAME] as? String ?: "",
            email = data[FIELD_EMAIL] as? String ?: "",
            createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong()
                ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toChatSession(): ChatSession? {
        val data = data ?: return null
        return ChatSession(
            id = id,
            title = data[FIELD_TITLE] as? String ?: "",
            messages = emptyList()
        )
    }

    private fun DocumentSnapshot.toMessage(sessionId: String): Message? {
        val data = data ?: return null
        return Message(
            id = id,
            sessionId = sessionId,
            senderId = data[FIELD_SENDER_ID] as? String ?: "",
            text = data[FIELD_TEXT] as? String ?: "",
            timestamp = (data[FIELD_TIMESTAMP] as? Number)?.toLong()
                ?: System.currentTimeMillis(),
            isOutgoing = data[FIELD_IS_OUTGOING] as? Boolean ?: false
        )
    }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_CHAT_SESSIONS = "chatSessions"
        private const val SUBCOLLECTION_MESSAGES = "messages"

        private const val FIELD_NAME = "name"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_CREATED_AT = "createdAt"

        private const val FIELD_TITLE = "title"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_LAST_MESSAGE_PREVIEW = "lastMessagePreview"

        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_TEXT = "text"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_IS_OUTGOING = "isOutgoing"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (!cont.isCompleted) {
            cont.resume(result)
        }
    }
    addOnFailureListener { error ->
        if (!cont.isCompleted) {
            cont.resumeWithException(error)
        }
    }
}
