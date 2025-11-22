package com.example.cs501_micro_chat.data.repository

import android.content.Context
import android.net.Uri
import com.example.cs501_micro_chat.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

interface ProfileRepository {
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(displayName: String, bio: String, avatarUri: Uri?): Result<Unit>
}

data class UserProfile(
    val displayName: String,
    val bio: String,
    val avatarUrl: String,
    val email: String
)

@Singleton
class FirebaseProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : ProfileRepository {

    override suspend fun getProfile(): Result<UserProfile> = runCatching {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val snapshot = firestore.collection("users")
            .document(userId)
            .get()
            .await()
        val displayName = snapshot.getString("username") ?: auth.currentUser?.displayName.orEmpty()
        val bio = snapshot.getString("statusMessage") ?: ""
        val avatarUrl = snapshot.getString("avatarUrl") ?: auth.currentUser?.photoUrl?.toString().orEmpty()
        val email = snapshot.getString("email") ?: auth.currentUser?.email.orEmpty()
        UserProfile(
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl,
            email = email
        )
    }

    override suspend fun updateProfile(displayName: String, bio: String, avatarUri: Uri?): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User not logged in")
        val userId = user.uid
        var uploadedAvatarUrl: String? = null
        if (avatarUri != null) {
            uploadedAvatarUrl = uploadAvatar(userId, avatarUri)
        }

        val updates = mutableMapOf<String, Any>(
            "username" to displayName,
            "statusMessage" to bio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        uploadedAvatarUrl?.let { updates["avatarUrl"] = it }

        firestore.collection("users")
            .document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .await()

        val profileChange = userProfileChangeRequest {
            this.displayName = displayName
            uploadedAvatarUrl?.let { photoUri = Uri.parse(it) }
        }
        user.updateProfile(profileChange).await()
    }

    private suspend fun uploadAvatar(userId: String, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to read image data")
        val reference = storage.reference.child("avatars/$userId.jpg")
        reference.putBytes(bytes).await()
        return reference.downloadUrl.await().toString()
    }
}
