package com.example.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.local.entity.CommunityHazardEntity
import com.example.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityHazardDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHazard(hazard: CommunityHazardEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHazards(hazards: List<CommunityHazardEntity>)

  @Update
  suspend fun updateHazard(hazard: CommunityHazardEntity)

  @Query("SELECT * FROM community_hazards WHERE id = :hazardId LIMIT 1")
  suspend fun getHazardById(hazardId: String): CommunityHazardEntity?

  @Query("SELECT * FROM community_hazards ORDER BY createdAt DESC")
  fun getAllHazards(): Flow<List<CommunityHazardEntity>>

  @Query("SELECT * FROM community_hazards ORDER BY createdAt DESC")
  suspend fun getAllHazardsList(): List<CommunityHazardEntity>

  @Query("SELECT * FROM community_hazards WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
  suspend fun getPendingHazards(): List<CommunityHazardEntity>

  @Query("DELETE FROM community_hazards WHERE id = :hazardId")
  suspend fun deleteHazardById(hazardId: String)

  @Query("UPDATE community_hazards SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :hazardId")
  suspend fun updateSyncStatus(hazardId: String, syncStatus: SyncStatus, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM community_hazards")
  suspend fun clearAllHazards()
}
