package com.example.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a breadcrumb location record along a journey.
 * Foreign-keyed to JourneyEntity with CASCADE deletion.
 * Indexed on journeyId and timestamp for fast chronological reconstruction.
 */
@Entity(
  tableName = "journey_locations",
  foreignKeys = [
    ForeignKey(
      entity = JourneyEntity::class,
      parentColumns = ["journeyId"],
      childColumns = ["journeyId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["journeyId"]),
    Index(value = ["journeyId", "timestamp"])
  ]
)
data class JourneyLocationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val journeyId: String,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val speedKmh: Double = 0.0,
  val timestamp: Long,
  val address: String = "",
  val syncStatus: SyncStatus = SyncStatus.PENDING
)
