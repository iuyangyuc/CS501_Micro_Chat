package com.example.cs501_micro_chat.data.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false
)
