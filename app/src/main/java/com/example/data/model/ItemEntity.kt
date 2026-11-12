package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // from Category enum name
    val condition: String, // from ItemCondition enum name
    val exchangeType: String, // DON or TROC
    val wantedInReturn: String = "", // for Troc
    val grainsReward: Int = 15, // Grains of solidarity earned on giving
    val grainsCost: Int = 10, // Grains needed if acquiring via Troc
    val imageResName: String = "",
    val imageUri: String = "",
    val donorId: String,
    val donorName: String,
    val donorRole: String = UserRole.CITOYEN.name,
    val donorRating: Float = 4.9f,
    val donorReviewsCount: Int = 12,
    val donorVerified: Boolean = true,
    val city: String = "Douala",
    val neighborhood: String = "Akwa",
    val landmark: String = "Carrefour Ndokoti",
    val approximateLat: Double = 4.0511,
    val approximateLng: Double = 9.7679,
    val distanceMeters: Int = 450,
    val status: String = ItemStatus.AVAILABLE.name,
    val isApprovedByAdmin: Boolean = true,
    val isReported: Boolean = false,
    val reportedReason: String = "",
    val isEmergencyResponse: Boolean = false,
    val targetOrphanageName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
