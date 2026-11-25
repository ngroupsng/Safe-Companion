package com.example.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a trusted contact.
 */
@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
  @PrimaryKey val id: String,
  val name: String,
  val relationship: String,
  val phone: String,
  val isPrimary: Boolean = false,
  val avatarUrl: String? = null,
  val syncStatus: SyncStatus = SyncStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
