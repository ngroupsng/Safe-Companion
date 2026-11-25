package com.example.firebase.fcm

import android.content.Context
import android.util.Log
import com.example.firebase.model.FirestoreDeviceToken
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Cloud Messaging token retrieval and association
 * with authenticated user devices in Cloud Firestore.
 * Path: users/{userId}/devices/{deviceId}
 *
 * NOTE: The Android client never contains FCM server keys or credentials.
 */
class FcmTokenManager(
  private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
  private val messaging: FirebaseMessaging? = try {
    FirebaseMessaging.getInstance().apply {
      isAutoInitEnabled = false
    }
  } catch (_: Throwable) {
    null
  }
) {

  companion object {
    private const val TAG = "FcmTokenManager"
  }

  /**
   * Retrieves the current FCM token for this device.
   */
  suspend fun retrieveToken(): Result<String> = runCatching {
    val fcm = messaging ?: error("Firebase Cloud Messaging is not available on this device or environment")
    fcm.token.await()
  }

  /**
   * Registers or updates the device token under users/{userId}/devices/{deviceId}.
   */
  suspend fun registerDeviceToken(
    userId: String,
    deviceId: String,
    token: String
  ): Result<Unit> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    require(deviceId.isNotBlank()) { "deviceId must not be blank" }
    require(token.isNotBlank()) { "token must not be blank" }

    val deviceData = FirestoreDeviceToken(
      deviceId = deviceId,
      fcmToken = token,
      platform = "ANDROID"
    )

    firestore.collection("users")
      .document(userId)
      .collection("devices")
      .document(deviceId)
      .set(deviceData.toMap())
      .await()

    Log.i(TAG, "Device token registered successfully for user: $userId (device: $deviceId)")
  }

  /**
   * Unregisters the device token (e.g., on logout).
   */
  suspend fun unregisterDeviceToken(
    userId: String,
    deviceId: String
  ): Result<Unit> = runCatching {
    firestore.collection("users")
      .document(userId)
      .collection("devices")
      .document(deviceId)
      .delete()
      .await()
  }
}
