package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isSystemNotice: Boolean = false
)
