package com.example.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.local.dao.*
import com.example.local.entity.*

/**
 * Type converters for Room persistence.
 */
class SafeCompanionTypeConverters {

  @TypeConverter
  fun fromSyncStatus(status: SyncStatus?): String {
    return status?.name ?: SyncStatus.PENDING.name
  }

  @TypeConverter
  fun toSyncStatus(value: String?): SyncStatus {
    return try {
      if (value != null) SyncStatus.valueOf(value) else SyncStatus.PENDING
    } catch (e: Exception) {
      SyncStatus.PENDING
    }
  }
}

/**
 * SafeCompanion Room Database.
 * Holds:
 * - JourneyEntity
 * - JourneyLocationEntity
 * - EmergencyEntity
 * - TrustedContactEntity
 * - CommunityHazardEntity
 */
@Database(
  entities = [
    JourneyEntity::class,
    JourneyLocationEntity::class,
    EmergencyEntity::class,
    TrustedContactEntity::class,
    CommunityHazardEntity::class
  ],
  version = 4,
  exportSchema = false
)
@TypeConverters(SafeCompanionTypeConverters::class)
abstract class SafeCompanionDatabase : RoomDatabase() {

  abstract fun journeyDao(): JourneyDao
  abstract fun journeyLocationDao(): JourneyLocationDao
  abstract fun emergencyDao(): EmergencyDao
  abstract fun trustedContactDao(): TrustedContactDao
  abstract fun communityHazardDao(): CommunityHazardDao

  companion object {
    private const val DATABASE_NAME = "safe_companion.db"

    @Volatile
    private var INSTANCE: SafeCompanionDatabase? = null

    /**
     * Retrieves the singleton SafeCompanionDatabase instance.
     */
    fun getInstance(context: Context): SafeCompanionDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          SafeCompanionDatabase::class.java,
          DATABASE_NAME
        )
          .fallbackToDestructiveMigration()
          .build()
          .also { INSTANCE = it }
      }
    }

    /**
     * Builds an in-memory database instance primarily for tests.
     */
    fun buildInMemoryDatabase(context: Context): SafeCompanionDatabase {
      return Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        SafeCompanionDatabase::class.java
      )
        .allowMainThreadQueries()
        .build()
    }
  }
}
