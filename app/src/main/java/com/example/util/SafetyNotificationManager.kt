package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.IncomingEmergencyAlert

/**
 * Central manager for dispatching and configuring system-level Android notifications
 * for Safe Companion (Emergencies, Safe Arrivals, Journey Alerts, Hazards, and Tests).
 */
object SafetyNotificationManager {

  const val CHANNEL_EMERGENCY = "safe_companion_emergency_channel"
  const val CHANNEL_JOURNEY = "safe_companion_journey_channel"
  const val CHANNEL_HAZARDS = "safe_companion_hazards_channel"
  const val CHANNEL_GENERAL = "safe_companion_general_channel"

  const val NOTIF_ID_EMERGENCY = 9110
  const val NOTIF_ID_ARRIVAL = 9120
  const val NOTIF_ID_JOURNEY_ALERT = 9130
  const val NOTIF_ID_HAZARD = 9140
  const val NOTIF_ID_TEST = 9150

  const val EXTRA_TARGET_ROUTE = "target_route"
  const val EXTRA_NOTIFICATION_ID = "notification_id"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

      // 1. Emergency Channel (High priority, urgent alarm sound, heavy vibration)
      val emergencyChannel = NotificationChannel(
        CHANNEL_EMERGENCY,
        "Emergency SOS Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Urgent SOS alerts, active emergency broadcasts, and distress signals"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        setSound(alarmSound, audioAttributes)
      }

      // 2. Journey & Safe Arrival Channel (High priority, notification sound, vibration)
      val journeyChannel = NotificationChannel(
        CHANNEL_JOURNEY,
        "Journey & Safe Arrival Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notifications when you reach a destination safely, or route deviations occur"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 300, 200, 300)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }

      // 3. Community Hazards Channel (Default priority)
      val hazardChannel = NotificationChannel(
        CHANNEL_HAZARDS,
        "Community Safety & Hazard Alerts",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Alerts about safety hazards reported along your route or neighborhood"
        enableLights(true)
        enableVibration(true)
      }

      // 4. General & Test Channel
      val generalChannel = NotificationChannel(
        CHANNEL_GENERAL,
        "General Safety & Diagnostics",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Routine safety reminders, diagnostics, and test notifications"
      }

      notificationManager.createNotificationChannel(emergencyChannel)
      notificationManager.createNotificationChannel(journeyChannel)
      notificationManager.createNotificationChannel(hazardChannel)
      notificationManager.createNotificationChannel(generalChannel)
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  /**
   * Dispatches a Safe Arrival notification when a journey is successfully completed.
   */
  fun showSafeArrivalNotification(
    context: Context,
    destination: String,
    companionCount: Int = 0
  ) {
    if (!hasNotificationPermission(context)) return
    createNotificationChannels(context)

    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_TARGET_ROUTE, "journey_completed")
        putExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_ARRIVAL)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIF_ID_ARRIVAL,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val subtitle = if (companionCount > 0) {
        "Arrived safely at $destination. $companionCount trusted contacts notified."
      } else {
        "Arrived safely at $destination. Safe journey completed."
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_JOURNEY)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("🎉 Safe Arrival Confirmed!")
        .setContentText(subtitle)
        .setStyle(
          NotificationCompat.BigTextStyle().bigText(
            "$subtitle\nYour trip monitoring has ended and your status has been updated to Safe."
          )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager.notify(NOTIF_ID_ARRIVAL, notification)
    } catch (e: Throwable) {
      Log.e("SafetyNotificationManager", "Failed to dispatch arrival notification: ${e.message}")
    }
  }

  /**
   * Dispatches a Route Deviation or Long Stop warning notification.
   */
  fun showJourneyAlertNotification(
    context: Context,
    title: String,
    message: String,
    isDeviation: Boolean = false
  ) {
    if (!hasNotificationPermission(context)) return
    createNotificationChannels(context)

    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_TARGET_ROUTE, "active_journey")
        putExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_JOURNEY_ALERT)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIF_ID_JOURNEY_ALERT,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val icon = if (isDeviation) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_popup_reminder

      val notification = NotificationCompat.Builder(context, CHANNEL_JOURNEY)
        .setSmallIcon(icon)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nTap to check your journey status or send SOS."))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager.notify(NOTIF_ID_JOURNEY_ALERT, notification)
    } catch (e: Throwable) {
      Log.e("SafetyNotificationManager", "Failed to dispatch journey alert: ${e.message}")
    }
  }

  /**
   * Dispatches a Community Hazard safety alert notification.
   */
  fun showHazardAlertNotification(
    context: Context,
    title: String,
    address: String
  ) {
    if (!hasNotificationPermission(context)) return
    createNotificationChannels(context)

    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_TARGET_ROUTE, "community_reports")
        putExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_HAZARD)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIF_ID_HAZARD,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, CHANNEL_HAZARDS)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("⚠️ Hazard Reported: $title")
        .setContentText("Reported near $address. Tap to view community reports.")
        .setStyle(
          NotificationCompat.BigTextStyle().bigText(
            "A safety hazard ($title) was reported near $address.\nStay alert and choose illuminated, verified routes."
          )
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager.notify(NOTIF_ID_HAZARD, notification)
    } catch (e: Throwable) {
      Log.e("SafetyNotificationManager", "Failed to dispatch hazard notification: ${e.message}")
    }
  }

  /**
   * Dispatches an interactive test system notification so the user can verify
   * that sound, vibration, and system alerts are working on their device.
   */
  fun showTestNotification(
    context: Context,
    title: String = "🔔 Safe Companion Notification Test",
    message: String = "System alerts, GPS monitoring, and emergency notifications are functioning properly."
  ): Boolean {
    createNotificationChannels(context)
    if (!hasNotificationPermission(context)) {
      Log.w("SafetyNotificationManager", "Cannot show test notification: permission missing")
      return false
    }

    return try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return false

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_TARGET_ROUTE, "notifications")
        putExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_TEST)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIF_ID_TEST,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(
          NotificationCompat.BigTextStyle().bigText(
            "$message\nTimestamp: ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}"
          )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager.notify(NOTIF_ID_TEST, notification)
      true
    } catch (e: Throwable) {
      Log.e("SafetyNotificationManager", "Failed to dispatch test notification: ${e.message}")
      false
    }
  }

  fun cancelNotification(context: Context, id: Int) {
    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.cancel(id)
    } catch (_: Throwable) {}
  }
}
