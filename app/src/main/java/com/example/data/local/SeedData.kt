package com.example.data.local

import com.example.data.model.ChatMessageEntity
import com.example.data.model.CollectionPointEntity
import com.example.data.model.DisputeEntity
import com.example.data.model.FundraiserEntity
import com.example.data.model.ItemEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole

object SeedData {

    val adminUser = UserEntity(
        id = "user_admin_mediator",
        name = "Médiation Solidaire Cameroun",
        email = "mediation@solidarite.cm",
        phone = "+237 677 00 11 22",
        role = UserRole.ADMIN_MEDIATEUR.name,
        city = "Douala",
        neighborhood = "Bonanjo",
        landmark = "Face Palais de Justice",
        radiusKm = 10.0f,
        solidarityGrains = 200,
        isVerified = true,
        isAdmin = true,
        rating = 5.0f,
        reviewCount = 42,
        giftsGiven = 0,
        trocsCompleted = 0,
        co2SavedKg = 0f,
        avatarInitials = "MS"
    )

    // Primary current user is the Administrator
    val currentUser = adminUser

    // Database is empty - only admin account is preserved
    val sampleReviews = emptyList<ReviewEntity>()
    val sampleItems = emptyList<ItemEntity>()
    val sampleFundraisers = emptyList<FundraiserEntity>()
    val sampleCollectionPoints = emptyList<CollectionPointEntity>()
    val sampleDisputes = emptyList<DisputeEntity>()
    val sampleChatMessages = emptyList<ChatMessageEntity>()
}
