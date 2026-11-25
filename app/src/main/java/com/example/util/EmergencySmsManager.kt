package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.EmergencyPayload
import com.example.model.IncomingEmergencyAlert
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Manages construction, transmission, and validation of Safe Companion emergency SMS messages.
 */
object EmergencySmsManager {

  const val PROTOCOL_HEADER = "🚨 [SAFE COMPANION EMERGENCY]"

  /**
   * Constructs the standardized emergency SMS payload text.
   * Includes emergency message, latitude, longitude, GPS accuracy, timestamp, maps reference,
   * offline voice note transcription, and emergency ID.
   */
  fun buildEmergencySms(payload: EmergencyPayload): String {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(payload.timestamp))
    return buildString {
      appendLine(PROTOCOL_HEADER)
      appendLine("SOS from: ${payload.userName.ifBlank { "User" }}")
      appendLine("Status: ${payload.status}")
      appendLine("Location: %.5f, %.5f (±%dm)".format(payload.latitude, payload.longitude, payload.accuracyMeters))
      appendLine("Maps: https://maps.google.com/?q=%.5f,%.5f".format(payload.latitude, payload.longitude))
      appendLine("Time: $dateStr")
      if (!payload.journeyDestination.isNullOrBlank()) {
        appendLine("Destination: ${payload.journeyDestination}")
      }
      if (!payload.voiceTranscription.isNullOrBlank()) {
        appendLine("Voice Note: \"${payload.voiceTranscription}\"")
      } else if (payload.voiceDurationSeconds > 0 || !payload.localVoiceFilePath.isNullOrBlank()) {
        appendLine("Voice Note: [Audio ${payload.voiceDurationSeconds}s stored on device; auto-syncs when online]")
      }
      append("ID: ${payload.emergencyId}")
    }
  }

  /**
   * Validates and parses an incoming SMS to determine if it is a genuine Safe Companion emergency message.
   * Returns IncomingEmergencyAlert if valid, or null if unrelated SMS.
   */
  fun parseEmergencySms(messageBody: String, senderAddress: String = ""): IncomingEmergencyAlert? {
    if (!messageBody.contains(PROTOCOL_HEADER) && !messageBody.contains("[SAFE COMPANION EMERGENCY]")) {
      return null
    }

    try {
      // Extract sender
      val senderName = Regex("SOS from:\\s*([^\r\n]+)").find(messageBody)?.groupValues?.get(1)?.trim()
        ?: "Safe Companion User"

      // Extract coordinates: Location: 37.77490, -122.41940
      var latitude = 0.0
      var longitude = 0.0
      var accuracy = 10

      val locMatch = Regex("Location:\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)").find(messageBody)
      if (locMatch != null) {
        latitude = locMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        longitude = locMatch.groupValues[2].toDoubleOrNull() ?: 0.0
      }

      val accuracyMatch = Regex("\\(±(\\d+)m\\)").find(messageBody)
      if (accuracyMatch != null) {
        accuracy = accuracyMatch.groupValues[1].toIntOrNull() ?: 10
      }

      // Extract destination
      val destMatch = Regex("Destination:\\s*([^\r\n]+)").find(messageBody)
      val destination = destMatch?.groupValues?.get(1)?.trim()

      // Extract Voice Note transcription if available
      val voiceMatch = Regex("Voice Note:\\s*\"?([^\r\n\"]+)\"?").find(messageBody)
      val voiceTranscription = voiceMatch?.groupValues?.get(1)?.trim()

      // Extract ID
      val idMatch = Regex("ID:\\s*([^\r\n]+)").find(messageBody)
      val emergencyId = idMatch?.groupValues?.get(1)?.trim() ?: UUID.randomUUID().toString()

      // If coordinates are missing, fallback to parsing Maps link
      if (latitude == 0.0 && longitude == 0.0) {
        val mapsMatch = Regex("maps\\.google\\.com/\\?q=([-\\d.]+),([-\\d.]+)").find(messageBody)
        if (mapsMatch != null) {
          latitude = mapsMatch.groupValues[1].toDoubleOrNull() ?: 0.0
          longitude = mapsMatch.groupValues[2].toDoubleOrNull() ?: 0.0
        }
      }

      return IncomingEmergencyAlert(
        emergencyId = emergencyId,
        senderName = senderName,
        senderPhone = senderAddress,
        relationship = "Trusted Contact",
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        timestamp = System.currentTimeMillis(),
        message = "Emergency SOS alert received via SMS fallback with live GPS coordinates.",
        journeyDestination = destination,
        voiceTranscription = voiceTranscription,
        receivedViaSms = true,
        isAlarmPlaying = true,
        isAcknowledged = false
      )
    } catch (e: Throwable) {
      Log.e("EmergencySmsManager", "Failed to parse emergency SMS: ${e.message}")
      return null
    }
  }

  /**
   * Dispatches emergency SMS to the specified recipient phone number.
   */
  fun sendEmergencySms(context: Context, destinationPhone: String, messageText: String): Boolean {
    val cleanPhone = destinationPhone.trim()
    if (cleanPhone.isBlank()) return false

    // Check permission
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    return try {
      val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
      } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
      }

      val parts = smsManager.divideMessage(messageText)
      if (parts.size > 1) {
        smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
      } else {
        smsManager.sendTextMessage(cleanPhone, null, messageText, null, null)
      }
      Log.i("EmergencySmsManager", "Dispatched emergency SMS to $cleanPhone")
      true
    } catch (e: Throwable) {
      Log.w("EmergencySmsManager", "SMS transmission failed (or permission missing in test): ${e.message}")
      false
    }
  }

  /**
   * Dispatches emergency SMS to multiple contacts.
   */
  fun sendEmergencySmsToAll(context: Context, recipientPhones: List<String>, messageText: String): Int {
    var sentCount = 0
    for (phone in recipientPhones) {
      if (sendEmergencySms(context, phone, messageText)) {
        sentCount++
      }
    }
    return sentCount
  }

  /**
   * Opens the system Messages app directly pre-populated with emergency text and recipient phone numbers.
   * Allows the user to inspect, review, and confirm the outgoing SOS in their Android Messaging application.
   */
  fun openEmergencyInMessagesApp(
    context: Context,
    recipientPhones: List<String>,
    messageText: String
  ): Boolean {
    val cleanPhones = recipientPhones
      .map { it.trim().replace("[^0-9+]".toRegex(), "") }
      .filter { it.isNotBlank() }

    val phoneListComma = cleanPhones.joinToString(",")
    val phoneListSemicolon = cleanPhones.joinToString(";")
    val primaryAddress = if (cleanPhones.isNotEmpty()) cleanPhones.first() else ""

    // 1. Try ACTION_SENDTO with smsto: scheme (Standard Android Messages format)
    try {
      val uri = if (phoneListComma.isNotBlank()) Uri.parse("smsto:$phoneListComma") else Uri.parse("smsto:")
      val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", messageText)
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (cleanPhones.isNotEmpty()) {
          putExtra("address", phoneListComma)
          putExtra(Intent.EXTRA_PHONE_NUMBER, phoneListComma)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      return true
    } catch (_: Throwable) {
      // Fall through to next intent option
    }

    // 2. Try ACTION_VIEW with sms: scheme
    try {
      val uri = if (phoneListComma.isNotBlank()) Uri.parse("sms:$phoneListComma") else Uri.parse("sms:")
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        putExtra("sms_body", messageText)
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (cleanPhones.isNotEmpty()) {
          putExtra("address", phoneListComma)
          putExtra(Intent.EXTRA_PHONE_NUMBER, phoneListComma)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      return true
    } catch (_: Throwable) {
      // Fall through to next intent option
    }

    // 3. Try with semicolon-separated numbers (Samsung Messages & older clients)
    try {
      val uri = if (phoneListSemicolon.isNotBlank()) Uri.parse("smsto:$phoneListSemicolon") else Uri.parse("smsto:")
      val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", messageText)
        putExtra(Intent.EXTRA_TEXT, messageText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      return true
    } catch (_: Throwable) {
      // Fall through to next intent option
    }

    // 4. Try ACTION_SEND with text/plain via Chooser
    return try {
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (phoneListComma.isNotBlank()) {
          putExtra("address", phoneListComma)
          putExtra(Intent.EXTRA_PHONE_NUMBER, phoneListComma)
        }
        putExtra(Intent.EXTRA_TEXT, messageText)
        putExtra("sms_body", messageText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val chooser = Intent.createChooser(sendIntent, "Open SOS in Messages App").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
      true
    } catch (e: Throwable) {
      Log.e("EmergencySmsManager", "Failed to launch Messages app: ${e.message}")
      false
    }
  }

  /**
   * Opens the system messaging/MMS composer with the recorded audio file attached,
   * allowing the user to send the actual voice recording over cellular MMS when offline.
   */
  fun shareEmergencyMms(
    context: Context,
    recipientPhones: List<String>,
    audioFilePath: String?,
    messageText: String
  ): Boolean {
    return try {
      val audioFile = if (!audioFilePath.isNullOrBlank()) File(audioFilePath) else null
      val cleanPhones = recipientPhones.map { it.trim() }.filter { it.isNotBlank() }
      val addressString = cleanPhones.joinToString(";")

      val intent = Intent(Intent.ACTION_SEND).apply {
        if (audioFile != null && audioFile.exists()) {
          type = "audio/*"
          val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            audioFile
          )
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
          type = "text/plain"
        }
        if (addressString.isNotBlank()) {
          putExtra("address", addressString)
          putExtra(Intent.EXTRA_PHONE_NUMBER, addressString)
        }
        putExtra(Intent.EXTRA_TEXT, messageText)
        putExtra("sms_body", messageText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Throwable) {
      Log.e("EmergencySmsManager", "Failed to launch MMS share intent: ${e.message}")
      false
    }
  }
}
