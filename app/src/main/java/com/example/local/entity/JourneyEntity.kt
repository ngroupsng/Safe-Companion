package com.example.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a persistent journey session.
 * Reconstructs previous journey history: journeyId, start/destination, timestamps,
 * status, distance, duration, and selected trusted contacts.
 */
@Entity(tableName = "journeys")
data class JourneyEntity(
  @PrimaryKey val journeyId: String,
  val userId: String = "user_default",
  val startLocation: String,
  val destination: String,
  val note: String = "",
  val startLat: Double = 0.0,
  val startLng: Double = 0.0,
  val destLat: Double = 0.0,
  val destLng: Double = 0.0,
  val initialDistanceKm: Double = 0.0,
  val remainingKm: Double = 0.0,
  val etaMinutes: Int = 0,
  val progress: Float = 0f,
  val isDestinationReached: Boolean = false,
  val isMonitoring: Boolean = false,
  val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED, EMERGENCY
  val startTimestamp: Long,
  val endTimestamp: Long? = null,
  val durationMinutes: Int = 0,
  val totalKm: Double = 0.0,
  val selectedContactIdsCsv: String = "", // Comma-separated trusted contact IDs
  val stopsJson: String = "",
  val currentStopIndex: Int = 0,
  val syncStatus: SyncStatus = SyncStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
