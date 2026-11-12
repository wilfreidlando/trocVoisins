package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    val targetUserId: String,
    val authorName: String,
    val rating: Int,
    val badge: String, // e.g. "Donneur ponctuel", "Super voisin", "Éco-responsable"
    val comment: String,
    val date: String
)
