package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CollectionPointEntity
import com.example.data.model.DisputeEntity
import com.example.data.model.FundraiserEntity
import com.example.data.model.ItemEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrocDao {

    // Items
    @Query("SELECT * FROM items WHERE isApprovedByAdmin = 1 ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItemsForAdmin(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE isReported = 1 ORDER BY createdAt DESC")
    fun getReportedItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE donorId = :userId ORDER BY createdAt DESC")
    fun getItemsByDonor(userId: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Query("UPDATE items SET status = :status WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: String, status: String)

    @Query("UPDATE items SET isApprovedByAdmin = :isApproved WHERE id = :itemId")
    suspend fun updateItemApproval(itemId: String, isApproved: Boolean)

    @Query("UPDATE items SET isReported = 1, reportedReason = :reason WHERE id = :itemId")
    suspend fun reportItem(itemId: String, reason: String)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemsCount(): Int

    // Fundraisers (Cagnottes solidaires)
    @Query("SELECT * FROM fundraisers ORDER BY (currentAmountFcfa * 1.0 / targetAmountFcfa) ASC")
    fun getAllFundraisers(): Flow<List<FundraiserEntity>>

    @Query("SELECT * FROM fundraisers WHERE id = :id")
    fun getFundraiserById(id: String): Flow<FundraiserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFundraiser(fundraiser: FundraiserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFundraisers(fundraisers: List<FundraiserEntity>)

    @Query("UPDATE fundraisers SET currentAmountFcfa = currentAmountFcfa + :amountFcfa, currentGrains = currentGrains + :grains, donorsCount = donorsCount + 1 WHERE id = :id")
    suspend fun contributeToFundraiser(id: String, amountFcfa: Long, grains: Int)

    // Collection Points (Permanences & Relais)
    @Query("SELECT * FROM collection_points ORDER BY city, neighborhood")
    fun getAllCollectionPoints(): Flow<List<CollectionPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionPoints(points: List<CollectionPointEntity>)

    // Disputes & Mediation
    @Query("SELECT * FROM disputes ORDER BY timestamp DESC")
    fun getAllDisputes(): Flow<List<DisputeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: DisputeEntity)

    @Query("UPDATE disputes SET status = :status, mediatorNotes = :notes WHERE id = :disputeId")
    suspend fun resolveDispute(disputeId: String, status: String, notes: String)

    // Chat messages
    @Query("SELECT * FROM chat_messages WHERE itemId = :itemId ORDER BY timestamp ASC")
    fun getMessagesForItem(itemId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    // User Profile & Roles
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET solidarityGrains = solidarityGrains + :grains WHERE id = :userId")
    suspend fun addGrains(userId: String, grains: Int)

    @Query("UPDATE users SET solidarityGrains = CASE WHEN solidarityGrains >= :grains THEN solidarityGrains - :grains ELSE 0 END WHERE id = :userId")
    suspend fun deductGrains(userId: String, grains: Int)

    @Query("UPDATE users SET role = :newRole, isAdmin = :isAdmin WHERE id = :userId")
    suspend fun switchUserRole(userId: String, newRole: String, isAdmin: Boolean)

    @Query("UPDATE users SET city = :city, neighborhood = :neighborhood, landmark = :landmark, radiusKm = :radiusKm WHERE id = :userId")
    suspend fun updateCameroonLocation(userId: String, city: String, neighborhood: String, landmark: String, radiusKm: Float)

    @Query("UPDATE users SET giftsGiven = giftsGiven + 1, solidarityGrains = solidarityGrains + 20, co2SavedKg = co2SavedKg + 4.2 WHERE id = :userId")
    suspend fun rewardGivingUser(userId: String)

    // Reviews
    @Query("SELECT * FROM reviews WHERE targetUserId = :userId ORDER BY date DESC")
    fun getReviewsForUser(userId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    // Complete Database Reset
    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    @Query("DELETE FROM fundraisers")
    suspend fun clearAllFundraisers()

    @Query("DELETE FROM collection_points")
    suspend fun clearAllCollectionPoints()

    @Query("DELETE FROM disputes")
    suspend fun clearAllDisputes()

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM reviews")
    suspend fun clearAllReviews()

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}
