package com.example.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a community hazard report.
 */
@Entity(tableName = "community_hazards")
data class CommunityHazardEntity(
  @PrimaryKey val id: String,
  val title: String,
  val hazardType: String,
  val locationAddress: String,
  val reportedTime: String,
  val status: String,
  val description: String,
  val photoUrl: String? = null,
  val confirmationsCount: Int = 0,
  val userVoted: Boolean? = null,
  val distanceAway: String = "",
  val latitude: Double,
  val longitude: Double,
  val isReportedByMe: Boolean = false,
  val syncStatus: SyncStatus = SyncStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
