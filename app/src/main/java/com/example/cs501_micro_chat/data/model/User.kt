package com.example.cs501_micro_chat.data.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
