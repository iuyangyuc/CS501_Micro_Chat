package com.example.cs501_micro_chat.data.model

/**
 * Result returned after uploading a media file to Firebase Storage.
 *
 * @property downloadUrl Public CDN URL that can be shared inside chat messages.
 * @property storagePath The path inside Firebase Storage (useful for deleting files later).
 * @property contentType MIME type stored with the blob.
 * @property sizeBytes File size, helps show upload stats or limit enforcement.
 */
data class MediaUploadResult(
    val downloadUrl: String,
    val storagePath: String,
    val contentType: String,
    val sizeBytes: Long
)
