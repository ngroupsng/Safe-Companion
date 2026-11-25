package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight Android connectivity monitor using ConnectivityManager and NetworkCallback.
 * Exposes real-time network availability to the synchronization layer.
 * Does not block or perform checks in the core GPS tracking path.
 */
class NetworkConnectivityMonitor(
  context: Context? = null
) : ConnectivityObserver {

  private val _isOnline = MutableStateFlow(false)
  override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var connectivityManager: ConnectivityManager? = null
  private var networkCallback: ConnectivityManager.NetworkCallback? = null

  init {
    if (context != null) {
      try {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        _isOnline.value = checkInitialConnectivity()
        registerNetworkCallback()
      } catch (e: Throwable) {
        Log.w("NetworkMonitor", "Could not initialize network callback: ${e.message}")
        _isOnline.value = true // Default safe fallback
      }
    } else {
      _isOnline.value = false
    }
  }

  private fun checkInitialConnectivity(): Boolean {
    val cm = connectivityManager ?: return false
    return try {
      val activeNetwork = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Throwable) {
      false
    }
  }

  private fun registerNetworkCallback() {
    val cm = connectivityManager ?: return
    try {
      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

      val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          _isOnline.value = true
        }

        override fun onLost(network: Network) {
          _isOnline.value = checkInitialConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
          _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
      }
      networkCallback = callback
      cm.registerNetworkCallback(request, callback)
    } catch (e: Throwable) {
      Log.w("NetworkMonitor", "Failed registering network callback: ${e.message}")
    }
  }

  fun unregister() {
    try {
      networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
      networkCallback = null
    } catch (_: Throwable) {}
  }

  override fun isConnected(): Boolean = _isOnline.value

  /**
   * For testing and offline/online state simulation.
   */
  fun setOnlineForTesting(online: Boolean) {
    _isOnline.value = online
  }
}
