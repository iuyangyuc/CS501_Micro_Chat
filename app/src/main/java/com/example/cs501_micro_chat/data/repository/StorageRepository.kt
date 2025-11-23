package com.example.cs501_micro_chat.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StorageRepository
 *
 * 处理 Firebase Storage 操作
 * Handles Firebase Storage operations
 *
 * 主要功能:
 * - 上传和下载头像 / Upload and download avatars
 * - 检查头像是否存在 / Check if avatar exists
 */
interface StorageRepository {
    suspend fun getAvatarUrl(userId: String): Result<String?>
    suspend fun uploadAvatar(userId: String, imageData: ByteArray): Result<String>
    suspend fun deleteAvatar(userId: String): Result<Unit>
}

@Singleton
class FirebaseStorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {

    /**
     * 获取用户头像 URL
     * Get user avatar URL from Firebase Storage
     *
     * 如果 Storage 中没有头像，返回 null
     * Returns null if no avatar exists in Storage
     */
    override suspend fun getAvatarUrl(userId: String): Result<String?> = runCatching {
        val reference = storage.reference.child("Avatars/$userId.jpg")
        try {
            reference.downloadUrl.await().toString()
        } catch (e: Exception) {
            // Avatar doesn't exist in storage, return null
            null
        }
    }

    /**
     * 上传用户头像到 Firebase Storage
     * Upload user avatar to Firebase Storage
     */
    override suspend fun uploadAvatar(userId: String, imageData: ByteArray): Result<String> = runCatching {
        val reference = storage.reference.child("Avatars/$userId.jpg")
        reference.putBytes(imageData).await()
        reference.downloadUrl.await().toString()
    }

    /**
     * 删除用户头像
     * Delete user avatar from Firebase Storage
     */
    override suspend fun deleteAvatar(userId: String): Result<Unit> = runCatching {
        val reference = storage.reference.child("Avatars/$userId.jpg")
        reference.delete().await()
    }
}


