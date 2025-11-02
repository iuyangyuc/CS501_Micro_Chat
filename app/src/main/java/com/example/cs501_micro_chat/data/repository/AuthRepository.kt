package com.example.cs501_micro_chat.data.repository

import com.google.firebase.auth.AuthResult
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
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) : AuthRepository {

    override suspend fun createUser(email: String, password: String) {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        persistUser(authResult, fallbackEmail = email)
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        persistUser(authResult, fallbackEmail = email)
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        persistUser(authResult)
    }

    private suspend fun persistUser(
        authResult: AuthResult,
        fallbackEmail: String? = null
    ) {
        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Authentication failed. Could not resolve Firebase user.")

        val profile = mutableMapOf<String, Any>()
        val email = firebaseUser.email ?: fallbackEmail
        if (!email.isNullOrBlank()) profile["email"] = email
        val displayName = firebaseUser.displayName
        if (!displayName.isNullOrBlank()) profile["displayName"] = displayName
        firebaseUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { profile["photoUrl"] = it }
        profile["updatedAt"] = FieldValue.serverTimestamp()
        if (authResult.additionalUserInfo?.isNewUser != false) {
            profile["createdAt"] = FieldValue.serverTimestamp()
        }

        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(profile, SetOptions.merge())
            .await()
    }
}
