package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "+237 690 00 00 00",
    val role: String = UserRole.CITOYEN.name,
    val city: String = "Douala",
    val neighborhood: String = "Akwa",
    val landmark: String = "Carrefour Ndokoti",
    val radiusKm: Float = 3.0f,
    val solidarityGrains: Int = 45,
    val isVerified: Boolean = true,
    val isAdmin: Boolean = false,
    val associationRegNumber: String = "",
    val rating: Float = 4.9f,
    val reviewCount: Int = 18,
    val giftsGiven: Int = 11,
    val trocsCompleted: Int = 6,
    val co2SavedKg: Float = 42.5f,
    val avatarInitials: String = "AK",
    val photoUrl: String? = null
)
