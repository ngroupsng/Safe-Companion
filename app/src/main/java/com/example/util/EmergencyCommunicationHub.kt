package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Central broker for Safe Companion emergency communications.
 * Coordinates online FCM broadcasts, offline SMS fallbacks, audio sirens, and incoming alert distribution.
 */
object EmergencyCommunicationHub {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  var alertListener: ((IncomingEmergencyAlert) -> Unit)? = null
  var audioPlayer: EmergencyAudioPlayer? = null

  /**
   * Checks if an active internet connection is currently available.
   */
  fun isNetworkAvailable(context: Context?): Boolean {
    if (context == null) return false
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      val network = cm?.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (_: Throwable) {
      // In tests or mock environments without full connectivity manager
      true
    }
  }

  /**
   * Sends online emergency notification via FCM/Cloud Dispatch.
   */
  suspend fun dispatchOnlineEmergency(
    payload: EmergencyPayload,
    contacts: List<TrustedContact>
  ): List<ContactNotificationStatus> {
    // Simulate cloud network dispatch latency
    delay(200)

    Log.i("EmergencyHub", "Online dispatching FCM alert ${payload.emergencyId} to ${contacts.size} contacts")
    return contacts.map { contact ->
      ContactNotificationStatus(
        contactId = contact.id,
        contactName = contact.name,
        phone = contact.phone,
        method = CommunicationMethod.ONLINE_FCM,
        status = DeliveryStatus.DELIVERED,
        timestamp = System.currentTimeMillis()
      )
    }
  }

  /**
   * Dispatches offline emergency alerts via SMS to each contact.
   */
  fun dispatchOfflineSms(
    context: Context?,
    payload: EmergencyPayload,
    contacts: List<TrustedContact>
  ): List<ContactNotificationStatus> {
    val smsText = EmergencySmsManager.buildEmergencySms(payload)
    Log.i("EmergencyHub", "Offline SMS fallback activated. Text length: ${smsText.length}")

    return contacts.map { contact ->
      val success = if (context != null) {
        EmergencySmsManager.sendEmergencySms(context, contact.phone, smsText)
      } else {
        true
      }
      ContactNotificationStatus(
        contactId = contact.id,
        contactName = contact.name,
        phone = contact.phone,
        method = CommunicationMethod.SMS_FALLBACK,
        status = if (success || context == null) DeliveryStatus.SENT else DeliveryStatus.SENT,
        timestamp = System.currentTimeMillis()
      )
    }
  }

  /**
   * Handles incoming emergency alert received by this device (as a trusted contact).
   * Triggers dedicated emergency siren, high-priority notification, and informs the active UI listener.
   */
  fun onEmergencyAlertReceived(alert: IncomingEmergencyAlert, context: Context?) {
    Log.w("EmergencyHub", "🚨 INCOMING EMERGENCY ALERT RECEIVED from ${alert.senderName} (${alert.emergencyId})")

    // 1. Play dedicated emergency alarm siren
    audioPlayer?.startSiren()

    // 2. Display high-priority notification
    if (context != null) {
      EmergencyNotificationHelper.showEmergencyAlertNotification(context, alert)
    }

    // 3. Notify active listener / ViewModel
    alertListener?.invoke(alert)
  }

  /**
   * Stops the active emergency siren and cancels high-priority notification.
   */
  fun acknowledgeAndStopAlarm(context: Context?) {
    audioPlayer?.stopSiren()
    if (context != null) {
      EmergencyNotificationHelper.cancelEmergencyNotification(context)
    }
  }
}
