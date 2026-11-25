package com.example.local.entity

/**
 * Status of local records with respect to remote synchronization (e.g. Firebase).
 */
enum class SyncStatus {
  PENDING,
  SYNCING,
  SYNCED,
  FAILED
}
