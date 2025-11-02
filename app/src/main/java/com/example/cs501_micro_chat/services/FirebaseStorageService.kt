package com.example.cs501_micro_chat.services

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Simple helper wrapped around Firebase Storage that can add, modify, download, and delete files.
 *
 * Exposes suspend functions so it can be consumed from coroutine scopes (e.g. ViewModels).
 */
class FirebaseStorageService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    /**
     * Adds a brand new file at `/uploads/{path}` if it does not already exist.
     * @throws IllegalStateException when the file already exists.
     */
    suspend fun addFile(path: String, fileUri: Uri): Uri {
        val reference = uploadsFolder().child(path)
        ensureFileAbsent(reference)
        return uploadInternal(reference, fileUri)
    }

    /**
     * Replaces an existing file at `/uploads/{path}` with the new contents pointed at [fileUri].
     * @throws NoSuchElementException when no file exists at the requested path.
     */
    suspend fun modifyFile(path: String, fileUri: Uri): Uri {
        val reference = uploadsFolder().child(path)
        ensureFilePresent(reference)
        return uploadInternal(reference, fileUri)
    }

    /**
     * Alias kept for the original sample usage where "upload" was the terminology.
     * Behaves the same as Firebase Storage's default behaviour (add or overwrite).
     */
    suspend fun uploadFile(path: String, fileUri: Uri): Uri {
        val reference = uploadsFolder().child(path)
        return uploadInternal(reference, fileUri)
    }

    /**
     * Downloads the file stored at `/uploads/{path}` into [destinationFile] and returns that file.
     * @throws NoSuchElementException when the file does not exist.
     */
    suspend fun downloadFile(path: String, destinationFile: File): File {
        val reference = uploadsFolder().child(path)
        ensureFilePresent(reference)
        reference.getFile(destinationFile).await()
        return destinationFile
    }

    /**
     * Deletes the file stored at `/uploads/{path}`.
     * @throws NoSuchElementException when the file does not exist.
     */
    suspend fun deleteFile(path: String) {
        val reference = uploadsFolder().child(path)
        ensureFilePresent(reference)
        reference.delete().await()
    }

    private suspend fun uploadInternal(reference: StorageReference, fileUri: Uri): Uri {
        reference.putFile(fileUri).await()
        return reference.downloadUrl.await()
    }

    private suspend fun ensureFilePresent(reference: StorageReference) {
        try {
            reference.metadata.await()
        } catch (error: Exception) {
            if (error is StorageException && error.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                throw NoSuchElementException("File not found at ${reference.path}.")
            }
            throw error
        }
    }

    private suspend fun ensureFileAbsent(reference: StorageReference) {
        try {
            reference.metadata.await()
            throw IllegalStateException("File already exists at ${reference.path}.")
        } catch (error: Exception) {
            if (error is IllegalStateException) {
                throw error
            }
            if (error is StorageException && error.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return
            }
            throw error
        }
    }

    /**
     * Helper to keep all example files under a single folder. Adjust as needed for production use.
     */
    private fun uploadsFolder(): StorageReference = storage.reference.child("uploads")
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
    cont.invokeOnCancellation {
        if (this is StorageTask<*>) {
            this.cancel()
        }
    }
}
