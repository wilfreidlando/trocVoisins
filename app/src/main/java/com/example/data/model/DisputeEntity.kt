package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disputes")
data class DisputeEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val reporterId: String,
    val reporterName: String,
    val reportedUserId: String,
    val reportedUserName: String,
    val reason: String, // DisputeReason enum
    val description: String,
    val status: String = DisputeStatus.OPEN.name,
    val mediatorNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
