package com.example.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.local.entity.JourneyLocationEntity
import com.example.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyLocationDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLocation(location: JourneyLocationEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLocations(locations: List<JourneyLocationEntity>): List<Long>

  @Query("SELECT * FROM journey_locations WHERE journeyId = :journeyId ORDER BY timestamp ASC")
  suspend fun getLocationsForJourney(journeyId: String): List<JourneyLocationEntity>

  @Query("SELECT * FROM journey_locations WHERE journeyId = :journeyId ORDER BY timestamp ASC")
  fun observeLocationsForJourney(journeyId: String): Flow<List<JourneyLocationEntity>>

  @Query("SELECT COUNT(*) FROM journey_locations WHERE journeyId = :journeyId")
  suspend fun getLocationCountForJourney(journeyId: String): Int

  @Query("SELECT * FROM journey_locations WHERE id = :id LIMIT 1")
  suspend fun getLocationById(id: Long): JourneyLocationEntity?

  @Query("SELECT * FROM journey_locations WHERE syncStatus != 'SYNCED' ORDER BY timestamp ASC")
  suspend fun getPendingLocations(): List<JourneyLocationEntity>

  @Query("SELECT * FROM journey_locations WHERE journeyId = :journeyId AND syncStatus != 'SYNCED' ORDER BY timestamp ASC")
  suspend fun getPendingLocationsForJourney(journeyId: String): List<JourneyLocationEntity>

  @Query("UPDATE journey_locations SET syncStatus = :syncStatus WHERE id = :id")
  suspend fun updateLocationSyncStatus(id: Long, syncStatus: SyncStatus)

  @Query("UPDATE journey_locations SET syncStatus = :syncStatus WHERE id IN (:ids)")
  suspend fun updateLocationsSyncStatus(ids: List<Long>, syncStatus: SyncStatus)

  @Query("UPDATE journey_locations SET syncStatus = :syncStatus WHERE journeyId = :journeyId")
  suspend fun updateSyncStatusForJourney(journeyId: String, syncStatus: SyncStatus)

  @Query("DELETE FROM journey_locations WHERE journeyId = :journeyId")
  suspend fun deleteLocationsForJourney(journeyId: String)
}
