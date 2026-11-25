package com.example.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.local.entity.EmergencyEntity
import com.example.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEmergency(emergency: EmergencyEntity)

  @Update
  suspend fun updateEmergency(emergency: EmergencyEntity)

  @Query("SELECT * FROM emergencies WHERE emergencyId = :emergencyId LIMIT 1")
  suspend fun getEmergencyById(emergencyId: String): EmergencyEntity?

  @Query("SELECT * FROM emergencies WHERE emergencyId = :emergencyId LIMIT 1")
  fun observeEmergencyById(emergencyId: String): Flow<EmergencyEntity?>

  @Query("SELECT * FROM emergencies ORDER BY timestamp DESC")
  fun getAllEmergencies(): Flow<List<EmergencyEntity>>

  @Query("SELECT * FROM emergencies ORDER BY timestamp DESC")
  suspend fun getAllEmergenciesList(): List<EmergencyEntity>

  @Query("SELECT * FROM emergencies WHERE isResolved = 0 ORDER BY timestamp DESC LIMIT 1")
  suspend fun getActiveEmergency(): EmergencyEntity?

  @Query("SELECT * FROM emergencies WHERE isResolved = 0 ORDER BY timestamp DESC LIMIT 1")
  fun observeActiveEmergency(): Flow<EmergencyEntity?>

  @Query("UPDATE emergencies SET isResolved = 1, status = 'RESOLVED', syncStatus = :syncStatus, updatedAt = :updatedAt WHERE emergencyId = :emergencyId")
  suspend fun resolveEmergency(
    emergencyId: String,
    syncStatus: SyncStatus = SyncStatus.PENDING,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("SELECT * FROM emergencies WHERE syncStatus != 'SYNCED' ORDER BY timestamp ASC")
  suspend fun getPendingEmergencies(): List<EmergencyEntity>

  @Query("UPDATE emergencies SET voiceMessageUrl = :voiceUrl, localVoiceFilePath = :localPath, voiceTranscription = COALESCE(:transcription, voiceTranscription), updatedAt = :updatedAt WHERE emergencyId = :emergencyId")
  suspend fun updateVoiceMessage(emergencyId: String, voiceUrl: String?, localPath: String?, transcription: String? = null, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE emergencies SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE emergencyId = :emergencyId")
  suspend fun updateSyncStatus(emergencyId: String, syncStatus: SyncStatus, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM emergencies WHERE emergencyId = :emergencyId")
  suspend fun deleteEmergencyById(emergencyId: String)
}
