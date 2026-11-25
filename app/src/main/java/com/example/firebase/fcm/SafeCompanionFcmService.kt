package com.example.firebase.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.IncomingEmergencyAlert
import com.example.util.EmergencyCommunicationHub
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to handle Firebase Cloud Messaging downstream messages and token refreshes.
 * When emergency notifications arrive via FCM, forwards them into EmergencyCommunicationHub
 * so that sirens, high-priority notifications, and screen transitions are triggered.
 */
class SafeCompanionFcmService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "SafeCompanionFCM"
    private const val CHANNEL_ID = "safe_companion_fcm_emergency_channel"
    private const val CHANNEL_NAME = "Emergency SOS Dispatch"
  }

  private val serviceScope = CoroutineScope(Dispatchers.IO)

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.i(TAG, "New FCM Registration Token generated: $token")
    // If an authenticated user exists, token can be refreshed in Firestore asynchronously
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.w(TAG, "FCM Message received from: ${remoteMessage.from}")

    val data = remoteMessage.data
    val type = data["type"] ?: "EMERGENCY_ALERT"

    if (type == "EMERGENCY_ALERT") {
      val emergencyId = data["emergencyId"] ?: "alert_${System.currentTimeMillis()}"
      val senderName = data["senderName"] ?: "Companion in Distress"
      val senderPhone = data["senderPhone"] ?: ""
      val relationship = data["relationship"] ?: "Trusted Contact"
      val lat = data["latitude"]?.toDoubleOrNull() ?: 0.0
      val lng = data["longitude"]?.toDoubleOrNull() ?: 0.0
      val accuracy = data["accuracyMeters"]?.toIntOrNull() ?: 10
      val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
      val message = data["message"] ?: "Emergency SOS Triggered"
      val destination = data["journeyDestination"]
      val voiceUrl = data["voiceMessageUrl"]

      val alert = IncomingEmergencyAlert(
        emergencyId = emergencyId,
        senderName = senderName,
        senderPhone = senderPhone,
        relationship = relationship,
        latitude = lat,
        longitude = lng,
        accuracyMeters = accuracy,
        timestamp = timestamp,
        message = message,
        journeyDestination = destination,
        voiceMessageUrl = voiceUrl,
        isAlarmPlaying = true,
        isAcknowledged = false,
        receivedViaSms = false
      )

      // Feed into the existing EmergencyCommunicationHub
      EmergencyCommunicationHub.onEmergencyAlertReceived(alert, applicationContext)
    }
  }
}
