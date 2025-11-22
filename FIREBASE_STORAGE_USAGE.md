# Firebase Storage CDN Usage

These snippets show how to treat Firebase Storage as a lightweight CDN for chat media (images and MP3 voice notes) without touching the Compose frontend.

## Folder layout

All blobs are stored under a single `cdn/` prefix so cache rules or Cloud CDN settings can be applied uniformly:

```
cdn/
├── images/{conversationId}/{uploaderId}_{timestamp}_{rand}.jpg
└── audio/{conversationId}/{uploaderId}_{timestamp}_{rand}.mp3
```

The helper `StorageRepository` (`app/src/main/java/com/example/cs501_micro_chat/data/repository/StorageRepository.kt`) builds these paths, pushes bytes, and returns a `MediaUploadResult` containing:

- `downloadUrl`: public HTTPS URL to embed in chat messages
- `storagePath`: relative path (`images/...` or `audio/...`) used for deletes or rehydrating URLs
- `contentType`, `sizeBytes`: metadata for debugging/quota enforcement

## Upload image example

```kotlin
@Inject lateinit var storageRepository: StorageRepository
@Inject lateinit var chatRepository: ChatRepository

suspend fun sendImageMessage(
    conversationId: String,
    imageBytes: ByteArray,
    mimeType: String = "image/jpeg"
) {
    val uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: error("Not logged in")

    val upload = storageRepository.uploadImage(
        bytes = imageBytes,
        conversationId = conversationId,
        ownerId = uploaderId,
        mimeType = mimeType,
        extension = null // auto infer from mimeType
    ).getOrThrow()

    chatRepository.sendMessage(
        conversationId = conversationId,
        content = "图片",
        type = MessageType.IMAGE,
        mediaUrl = upload.downloadUrl
    )
}
```

## Upload MP3 voice note example

```kotlin
suspend fun sendVoiceMessage(
    conversationId: String,
    audioBytes: ByteArray,
    durationMillis: Long,
    mimeType: String = "audio/mpeg"
) {
    val uploaderId = FirebaseAuth.getInstance().currentUser?.uid ?: error("Not logged in")

    val upload = storageRepository.uploadVoiceMessage(
        bytes = audioBytes,
        conversationId = conversationId,
        ownerId = uploaderId,
        mimeType = mimeType,
        extension = null
    ).getOrThrow()

    val seconds = (durationMillis / 1000).coerceAtLeast(1)
    chatRepository.sendMessage(
        conversationId = conversationId,
        content = "语音消息 (${seconds}s)",
        type = MessageType.VOICE,
        mediaUrl = upload.downloadUrl
    )
}
```

If `chatRepository.sendMessage` fails for any reason, call `storageRepository.deleteByPath(upload.storagePath)` to avoid orphaned blobs.

## Download existing media

Store the `storagePath` (e.g. inside Firestore alongside the public URL) to regenerate download links if you ever rotate tokens:

```kotlin
val downloadUrl = storageRepository
    .getDownloadUrl(media.storagePath)
    .getOrElse { error("Cannot fetch CDN URL: $it") }
```

For most chat scenarios, serving the cached `mediaUrl` from `Message.mediaUrl` is sufficient since Firebase Storage URLs are long-lived.

## Delete image or MP3 file

When messages are deleted, also clean up the associated blob by path:

```kotlin
val path = "images/{conversationId}/{fileName}.jpg" // persisted earlier
storageRepository.deleteByPath(path)
    .onSuccess { /* removed from bucket */ }
    .onFailure { Timber.w(it, "Failed to delete media $path") }
```

Deleting via `storagePath` ensures the `cdn/` prefix remains tidy and saves storage costs.
