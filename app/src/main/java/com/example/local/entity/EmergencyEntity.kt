package com.example.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an emergency / SOS event.
 * Preserves emergency ID, journey ID, location, timestamp, status,
 * notified contacts, communication details, and voice message reference.
 */
@Entity(
  tableName = "emergencies",
  indices = [
    Index(value = ["journeyId"]),
    Index(value = ["timestamp"])
  ]
)
data class EmergencyEntity(
  @PrimaryKey val emergencyId: String,
  val journeyId: String? = null,
  val userId: String = "user_default",
  val status: String = "ACTIVE", // ACTIVE, RESOLVED, CANCELLED
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Int,
  val timestamp: Long,
  val message: String = "EMERGENCY SOS TRIGGERED",
  val locationAddress: String = "",
  val notifiedContactsCsv: String = "",
  val communicationMethod: String = "NONE",
  val voiceMessageUrl: String? = null,
  val localVoiceFilePath: String? = null,
  val voiceTranscription: String? = null,
  val isResolved: Boolean = false,
  val syncStatus: SyncStatus = SyncStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
