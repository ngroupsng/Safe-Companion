package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.IncomingEmergencyAlert

/**
 * Helper to dispatch high-priority emergency notifications on Android.
 * Note: Subject to system volume, notification permissions, and Android Do Not Disturb policies.
 */
object EmergencyNotificationHelper {

  const val CHANNEL_ID = "safe_companion_emergency_channel"
  const val NOTIFICATION_ID = 9110
  const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_ALARM"
  const val EXTRA_EMERGENCY_ID = "emergency_id"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
      val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Safe Companion Emergency Alerts",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Urgent SOS alerts and emergency notifications from trusted companions"
          enableLights(true)
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 500, 200, 500)
          lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
          val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
          val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
          setSound(alarmSound, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  fun showEmergencyAlertNotification(context: Context, alert: IncomingEmergencyAlert) {
    try {
      createNotificationChannel(context)
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_EMERGENCY_ID, alert.emergencyId)
      }
      val pendingOpenIntent = PendingIntent.getActivity(
        context,
        1001,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val stopAlarmIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        action = ACTION_STOP_ALARM
        putExtra(EXTRA_EMERGENCY_ID, alert.emergencyId)
      }
      val pendingStopAlarmIntent = PendingIntent.getActivity(
        context,
        1002,
        stopAlarmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val contentText = "🚨 Emergency alert from ${alert.senderName}! Location: ${"%.4f".format(alert.latitude)}, ${"%.4f".format(alert.longitude)}"

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("🚨 EMERGENCY: ${alert.senderName} Needs Help")
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(
          "${contentText}\n${alert.message}\nDestination: ${alert.journeyDestination ?: "Unknown"}\nAccuracy: ±${alert.accuracyMeters}m"
        ))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)
        .setOngoing(true)
        .setContentIntent(pendingOpenIntent)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm & Open", pendingStopAlarmIntent)
        .build()

      notificationManager.notify(NOTIFICATION_ID, notification)
    } catch (_: Throwable) {
      // Graceful fallback in test or permission-denied environments
    }
  }

  fun cancelEmergencyNotification(context: Context) {
    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.cancel(NOTIFICATION_ID)
    } catch (_: Throwable) {}
  }
}
