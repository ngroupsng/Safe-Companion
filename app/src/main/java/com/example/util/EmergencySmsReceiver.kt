package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * BroadcastReceiver for incoming SMS messages.
 * Detects, validates, and routes genuine Safe Companion emergency SMS messages
 * to trigger the dedicated emergency siren and display the emergency screen.
 */
class EmergencySmsReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

    try {
      val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
      if (messages.isNullOrEmpty()) return

      // Group multi-part SMS messages by originating address
      val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: "" }

      for ((sender, msgList) in messagesBySender) {
        val fullBody = msgList.joinToString(separator = "") { it.displayMessageBody ?: "" }

        val emergencyAlert = EmergencySmsManager.parseEmergencySms(fullBody, sender)
        if (emergencyAlert != null) {
          Log.i("EmergencySmsReceiver", "Valid Safe Companion emergency SMS detected from $sender!")
          EmergencyCommunicationHub.onEmergencyAlertReceived(emergencyAlert, context)
        }
      }
    } catch (e: Throwable) {
      Log.e("EmergencySmsReceiver", "Error processing incoming SMS: ${e.message}")
    }
  }
}
