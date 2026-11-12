package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CollectionPointEntity
import com.example.data.model.DisputeEntity
import com.example.data.model.FundraiserEntity
import com.example.data.model.ItemEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        ItemEntity::class,
        UserEntity::class,
        ChatMessageEntity::class,
        ReviewEntity::class,
        FundraiserEntity::class,
        DisputeEntity::class,
        CollectionPointEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TrocDatabase : RoomDatabase() {
    abstract fun trocDao(): TrocDao

    companion object {
        @Volatile
        private var INSTANCE: TrocDatabase? = null

        fun getDatabase(context: Context): TrocDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrocDatabase::class.java,
                    "troc_voisins.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
