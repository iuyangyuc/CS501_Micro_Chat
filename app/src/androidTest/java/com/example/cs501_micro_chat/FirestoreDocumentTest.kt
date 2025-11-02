package com.example.cs501_micro_chat

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth // <-- ADD THIS IMPORT
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RunWith(AndroidJUnit4::class)
class FirestoreDocumentTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    /**
     * This function runs BEFORE every @Test.
     */
    @Before
    fun setup() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance() // <-- Get auth instance

        // Use runBlocking to call suspend functions
        runBlocking {
            try {
                // 1. SIGN IN ANONYMOUSLY
                // This makes "request.auth" available in your security rules
                auth.signInWithEmailAndPassword("test@bu.edu", "123456").await()
                Log.d(TAG, "Signed in as test@bu.edu")

                // 2. Clear persistence and enable network (as before)
                db.clearPersistence().await()
                db.enableNetwork().await()
                Log.d(TAG, "Firestore network enabled and persistence cleared.")

            } catch (e: Exception) {
                Log.e(TAG, "Error in setup: ", e)
            }
        }
    }

    /**
     * This function runs AFTER every @Test.
     */
    @After
    fun tearDown() {
        runBlocking {
            try {
                // 1. Disable network (as before)
                db.disableNetwork().await()
                Log.d(TAG, "Firestore network disabled.")

                // 2. SIGN OUT
                // This isolates the test and cleans up the anonymous user
                auth.signOut()
                Log.d(TAG, "Signed out.")

            } catch (e: Exception) {
                Log.e(TAG, "Error in tearDown: ", e)
            }
        }
    }

    @Test
    fun logAndUpdateTestDocument() {
        // This test now runs as an authenticated (anonymous) user
        runBlocking {
            val documentRef = db
                .collection(TEST_COLLECTION)
                .document(TEST_DOCUMENT_ID)

            val snapshot = documentRef.get().await()
            val data = snapshot.data.orEmpty()

            Log.d(TAG, "user1=${data["user1"]}")
            Log.d(TAG, "user2=${data["user2"]}")
            Log.d(TAG, "message=${data["message"]}")

            val newMessage = Instant.now().toString()
            documentRef.update(MESSAGE_FIELD, newMessage).await()

            Log.d(TAG, "Updated message field to $newMessage")
        }
    }

    companion object {
        private const val TAG = "FirestoreDocumentTest"
        private const val TEST_COLLECTION = "testchat"
        private const val TEST_DOCUMENT_ID = "x2rXhkV33KfmyMiVEDQL"
        private const val MESSAGE_FIELD = "message"
    }
}

/**
 * Your await() extension function. This is perfectly fine.
 */
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