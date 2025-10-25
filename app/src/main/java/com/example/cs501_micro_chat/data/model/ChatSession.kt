package com.example.cs501_micro_chat.data.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<Message> = emptyList()
)
