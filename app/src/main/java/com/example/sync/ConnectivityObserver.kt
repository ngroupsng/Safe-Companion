package com.example.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining connectivity state observation for offline-first sync.
 */
interface ConnectivityObserver {
  val isOnline: StateFlow<Boolean>
  fun isConnected(): Boolean
}
