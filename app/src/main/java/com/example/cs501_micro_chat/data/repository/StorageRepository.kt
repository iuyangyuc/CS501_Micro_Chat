package com.example.cs501_micro_chat.data.repository

import com.example.cs501_micro_chat.data.model.MediaUploadResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StorageRepository
 *
 * Thin wrapper around Firebase Storage that exposes CDN friendly helpers for chat media.
 * It handles path conventions, metadata, and download URL retrieval so the rest of the
 * app can treat Firebase Storage like an origin CDN.
 */
@Singleton
class StorageRepository @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) {

    private val cdnRootRef
        get() = firebaseStorage.reference.child(CDN_ROOT_FOLDER)

    /**
     * Upload an image (jpeg/png/webp) and receive a signed CDN URL.
     */
    suspend fun uploadImage(
        bytes: ByteArray,
        conversationId: String,
        ownerId: String,
        mimeType: String = "image/jpeg",
        extension: String? = null
    ): Result<MediaUploadResult> = uploadBinary(
        bytes = bytes,
        folder = IMAGES_FOLDER,
        conversationId = conversationId,
        ownerId = ownerId,
        mimeType = mimeType,
        extension = extension
    )

    /**
     * Upload a voice note (mp3) that can later be streamed from the CDN URL.
     */
    suspend fun uploadVoiceMessage(
        bytes: ByteArray,
        conversationId: String,
        ownerId: String,
        mimeType: String = "audio/mpeg",
        extension: String? = null
    ): Result<MediaUploadResult> = uploadBinary(
        bytes = bytes,
        folder = AUDIO_FOLDER,
        conversationId = conversationId,
        ownerId = ownerId,
        mimeType = mimeType,
        extension = extension
    )

    /**
     * Remove a file from Firebase Storage when a message is withdrawn.
     */
    suspend fun deleteByPath(storagePath: String): Result<Unit> = runCatching {
        cdnRootRef.child(storagePath).delete().await()
    }


    /**
     * Get download URL from Firebase Storage root (for avatars stored in root/Avatars folder).
     */
    suspend fun getDownloadUrlFromRoot(storagePath: String): Result<String> = runCatching {
        firebaseStorage.reference.child(storagePath).downloadUrl.await().toString()
    }

    private suspend fun uploadBinary(
        bytes: ByteArray,
        folder: String,
        conversationId: String,
        ownerId: String,
        mimeType: String,
        extension: String?
    ): Result<MediaUploadResult> = runCatching {
        val sanitizedExtension = extension.orEmpty().removePrefix(".")
            .ifBlank { fallbackExtensionForMime(mimeType) }
            .lowercase(Locale.US)

        val fileName = buildString {
            append(ownerId)
            append("_")
            append(System.currentTimeMillis())
            append("_")
            append(UUID.randomUUID().toString().take(8))
            append(".")
            append(sanitizedExtension)
        }
        val storagePath = "$folder/$conversationId/$fileName"
        val metadata = storageMetadata {
            contentType = mimeType
            cacheControl = "public,max-age=$CDN_CACHE_SECONDS"
            setCustomMetadata("conversationId", conversationId)
            setCustomMetadata("ownerId", ownerId)
        }

        val ref = cdnRootRef.child(storagePath)
        val snapshot = ref.putBytes(bytes, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        MediaUploadResult(
            downloadUrl = downloadUrl,
            storagePath = storagePath,
            contentType = mimeType,
            sizeBytes = snapshot.totalByteCount
        )
    }

    private fun fallbackExtensionForMime(mimeType: String): String {
        return when {
            mimeType.contains("png", true) -> "png"
            mimeType.contains("webp", true) -> "webp"
            mimeType.contains("gif", true) -> "gif"
            mimeType.contains("audio") -> "mp3"
            else -> "jpg"
        }
    }

    companion object {
        private const val CDN_ROOT_FOLDER = "cdn"
        private const val IMAGES_FOLDER = "images"
        private const val AUDIO_FOLDER = "audio"
        private const val CDN_CACHE_SECONDS = 60L * 60L * 24L * 30L // 30 days
    }
}
