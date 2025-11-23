package com.example.cs501_micro_chat.data.repository

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    suspend fun createUser(email: String, password: String)
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun changePassword(currentPassword: String, newPassword: String)
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) : AuthRepository {

    override suspend fun createUser(email: String, password: String) {
        Log.d(TAG, "createUser: Starting user creation for $email")
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            Log.d(TAG, "createUser: Firebase auth succeeded")
            persistUser(authResult, fallbackEmail = email)
            Log.d(TAG, "createUser: User created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "createUser: Failed", e)
            throw e
        }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        Log.d(TAG, "signInWithEmail: Starting email login for $email")
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "signInWithEmail: Firebase auth succeeded")
            persistUser(authResult, fallbackEmail = email)
            Log.d(TAG, "signInWithEmail: User persisted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail: Failed", e)
            throw e
        }
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        persistUser(authResult)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
            ?: throw IllegalStateException("User is not logged in.")
        val email = user.email
            ?: throw IllegalStateException("User email is unavailable. Please reauthenticate and try again.")

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    private suspend fun persistUser(
        authResult: AuthResult,
        fallbackEmail: String? = null
    ) {
        Log.d(TAG, "persistUser: Starting user persistence")
        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Authentication failed. Could not resolve Firebase user.")

        Log.d(TAG, "persistUser: User ID = ${firebaseUser.uid}")

        val profile = mutableMapOf<String, Any>()
        val email = firebaseUser.email ?: fallbackEmail
        if (!email.isNullOrBlank()) profile["email"] = email

        val emailPrefix = email?.substringBefore("@").orEmpty()
        val displayName = firebaseUser.displayName
        val username = when {
            !displayName.isNullOrBlank() -> displayName
            emailPrefix.isNotBlank() -> emailPrefix
            else -> ""
        }

        if (!displayName.isNullOrBlank()) profile["displayName"] = displayName
        if (username.isNotBlank()) {
            profile["username"] = username
            if (displayName.isNullOrBlank()) {
                profile["displayName"] = username
            }
        }
        firebaseUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { profile["photoUrl"] = it }
        profile["updatedAt"] = FieldValue.serverTimestamp()
        if (authResult.additionalUserInfo?.isNewUser != false) {
            profile["createdAt"] = FieldValue.serverTimestamp()
        }

        Log.d(TAG, "persistUser: Saving user document to Firestore")
        try {
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(profile, SetOptions.merge())
                .await()
            Log.d(TAG, "persistUser: Firestore write succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "persistUser: Firestore write failed", e)
            throw e
        }
    }
}

private const val TAG = "AuthRepository"
