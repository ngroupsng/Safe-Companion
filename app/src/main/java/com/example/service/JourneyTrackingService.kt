package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.util.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service dedicated to background journey tracking.
 * Maintains continuous GPS location updates when the app is backgrounded,
 * screen is locked, or other apps are opened.
 */
class JourneyTrackingService : Service() {

  private val binder = LocalBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private var locationTracker: LocationTracker? = null
  private var currentDestination: String = "Active Journey"

  inner class LocalBinder : Binder() {
    fun getService(): JourneyTrackingService = this@JourneyTrackingService
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()

    // Android requires startForeground() to be called immediately when started via startForegroundService()
    val initialNotification = buildTrackingNotification(currentDestination)
    val hasLocation = hasLocationPermission(this)
    val fgsType = if (hasLocation) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
      } else 0
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      } else 0
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          initialNotification,
          fgsType
        )
      } else {
        startForeground(NOTIFICATION_ID, initialNotification)
      }
    } catch (e: SecurityException) {
      Log.e("JourneyTrackingService", "SecurityException during startForeground with type $fgsType: ${e.message}", e)
      // If location type failed due to missing permission, fallback to DATA_SYNC
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fgsType != ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
        try {
          ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
          )
        } catch (secEx: Throwable) {
          Log.e("JourneyTrackingService", "DATA_SYNC fallback also failed: ${secEx.message}", secEx)
          stopSelf()
        }
      } else {
        stopSelf()
      }
    } catch (e: Exception) {
      Log.e("JourneyTrackingService", "Failed to startForeground in onCreate: ${e.message}", e)
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
          )
        } else {
          startForeground(NOTIFICATION_ID, initialNotification)
        }
      } catch (fallbackError: Exception) {
        Log.e("JourneyTrackingService", "Fallback startForeground failed: ${fallbackError.message}", fallbackError)
        stopSelf()
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      stopJourneyTracking()
      stopSelf()
      return START_NOT_STICKY
    }

    val dest = intent?.getStringExtra(EXTRA_DESTINATION) ?: currentDestination
    currentDestination = dest
    startForegroundTracking(dest)

    return START_STICKY
  }

  private fun startForegroundTracking(destination: String) {
    // Ensure notification is displayed with the latest destination
    val notification = buildTrackingNotification(destination)
    val hasLocation = hasLocationPermission(this)
    val fgsType = if (hasLocation) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
      } else 0
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      } else 0
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification,
          fgsType
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    } catch (e: SecurityException) {
      Log.e("JourneyTrackingService", "SecurityException during tracking start: ${e.message}")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fgsType != ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
        try {
          ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
          )
        } catch (_: Throwable) {
          stopSelf()
        }
      }
    } catch (e: Exception) {
      Log.e("JourneyTrackingService", "startForeground error: ${e.message}")
    }

    _isTrackingActive = true

    // Start location updates only if location permission is granted
    if (locationTracker == null && hasLocation) {
      val tracker = LocationTracker(this)
      locationTracker = tracker

      if (tracker.hasLocationPermission()) {
        tracker.startLocationUpdates(intervalMs = 4000L) { location ->
          serviceScope.launch {
            _locationUpdatesFlow.emit(location)
            onLocationReceivedCallback?.invoke(location)
          }
        }
      }
    } else if (locationTracker != null && !hasLocation) {
      locationTracker?.stopLocationUpdates()
      locationTracker = null
    }
  }

  private fun hasLocationPermission(context: Context): Boolean {
    val fine = androidx.core.content.ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  private fun stopJourneyTracking() {
    _isTrackingActive = false
    try {
      locationTracker?.stopLocationUpdates()
    } catch (_: Exception) {}
    locationTracker = null

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(STOP_FOREGROUND_REMOVE)
      } else {
        @Suppress("DEPRECATION")
        stopForeground(true)
      }
    } catch (_: Exception) {}

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(NOTIFICATION_ID)
  }

  override fun onDestroy() {
    stopJourneyTracking()
    serviceScope.cancel()
    _isTrackingActive = false
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(NotificationManager::class.java) ?: return
      val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Safe Companion Journey Tracking",
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          description = "Displays ongoing journey safety monitoring status"
          setShowBadge(false)
          lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  private fun buildTrackingNotification(destination: String): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NAVIGATE_TO_JOURNEY, true)
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_mylocation)
      .setContentTitle("Safe Companion Active")
      .setContentText("Monitoring journey to $destination")
      .setSubText("Active Protection")
      .setOngoing(true)
      .setAutoCancel(false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setContentIntent(pendingIntent)
      .build()
  }

  companion object {
    const val CHANNEL_ID = "safe_companion_journey_tracking_channel"
    const val NOTIFICATION_ID = 9120
    const val ACTION_START_SERVICE = "com.example.service.ACTION_START_TRACKING"
    const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP_TRACKING"
    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_NAVIGATE_TO_JOURNEY = "navigate_to_journey"

    private var _isTrackingActive: Boolean = false
    val isTrackingActive: Boolean get() = _isTrackingActive

    private val _locationUpdatesFlow = MutableSharedFlow<Location>(extraBufferCapacity = 64)
    val locationUpdatesFlow: SharedFlow<Location> = _locationUpdatesFlow.asSharedFlow()

    // Optional direct callback hook for immediate ViewModel routing
    var onLocationReceivedCallback: ((Location) -> Unit)? = null

    fun start(context: Context, destination: String = "Destination") {
      val intent = Intent(context, JourneyTrackingService::class.java).apply {
        action = ACTION_START_SERVICE
        putExtra(EXTRA_DESTINATION, destination)
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
        _isTrackingActive = true
      } catch (e: Exception) {
        Log.e("JourneyTrackingService", "Failed to start JourneyTrackingService: ${e.message}", e)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, JourneyTrackingService::class.java).apply {
        action = ACTION_STOP_SERVICE
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e("JourneyTrackingService", "Failed to stop JourneyTrackingService: ${e.message}", e)
      }
      _isTrackingActive = false
    }
  }
}
