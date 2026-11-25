package com.example.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.local.entity.JourneyEntity
import com.example.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertJourney(journey: JourneyEntity)

  @Update
  suspend fun updateJourney(journey: JourneyEntity)

  @Query("SELECT * FROM journeys WHERE journeyId = :journeyId LIMIT 1")
  suspend fun getJourneyById(journeyId: String): JourneyEntity?

  @Query("SELECT * FROM journeys WHERE journeyId = :journeyId LIMIT 1")
  fun observeJourneyById(journeyId: String): Flow<JourneyEntity?>

  @Query("SELECT * FROM journeys ORDER BY startTimestamp DESC")
  fun getAllJourneys(): Flow<List<JourneyEntity>>

  @Query("SELECT * FROM journeys ORDER BY startTimestamp DESC")
  suspend fun getAllJourneysList(): List<JourneyEntity>

  @Query("SELECT * FROM journeys WHERE isMonitoring = 1 LIMIT 1")
  suspend fun getActiveJourney(): JourneyEntity?

  @Query("SELECT * FROM journeys WHERE isMonitoring = 1 LIMIT 1")
  fun observeActiveJourney(): Flow<JourneyEntity?>

  @Query("UPDATE journeys SET status = :status, isMonitoring = 0, isDestinationReached = :isReached, endTimestamp = :endTime, totalKm = :totalKm, durationMinutes = :durationMinutes, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE journeyId = :journeyId")
  suspend fun completeJourney(
    journeyId: String,
    status: String,
    isReached: Boolean,
    endTime: Long,
    totalKm: Double,
    durationMinutes: Int,
    syncStatus: SyncStatus = SyncStatus.PENDING,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("SELECT * FROM journeys WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
  suspend fun getPendingJourneys(): List<JourneyEntity>

  @Query("UPDATE journeys SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE journeyId = :journeyId")
  suspend fun updateSyncStatus(journeyId: String, syncStatus: SyncStatus, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM journeys WHERE journeyId = :journeyId")
  suspend fun deleteJourneyById(journeyId: String)

  @Query("DELETE FROM journeys")
  suspend fun clearAllJourneys()
}
