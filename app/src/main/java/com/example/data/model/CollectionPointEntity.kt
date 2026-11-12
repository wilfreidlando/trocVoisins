package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_points")
data class CollectionPointEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val organizationName: String,
    val type: String, // CollectionPointType enum name
    val city: String,
    val neighborhood: String,
    val landmark: String,
    val contactPhone: String,
    val openingHours: String,
    val acceptedCategories: String,
    val lat: Double,
    val lng: Double
)
