package com.example.firebase.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore representation for a user document.
 * Collection: users/{userId}
 */
data class FirestoreUser(
  val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val phoneNumber: String? = null,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "displayName" to displayName,
    "email" to email,
    "phoneNumber" to phoneNumber,
    "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}

/**
 * Firestore representation for a trusted contact.
 * Subcollection: users/{userId}/trustedContacts/{contactId}
 */
data class FirestoreTrustedContact(
  val contactId: String = "",
  val name: String = "",
  val phoneNumber: String = "",
  val relationship: String = "",
  val isPrimary: Boolean = false,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "contactId" to contactId,
    "name" to name,
    "phoneNumber" to phoneNumber,
    "relationship" to relationship,
    "isPrimary" to isPrimary,
    "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}

/**
 * Firestore representation for a journey document.
 * Collection: journeys/{journeyId}
 */
data class FirestoreJourney(
  val journeyId: String = "",
  val userId: String = "",
  val destination: String = "",
  val startLocation: String = "",
  val startTimestamp: Long = 0L,
  val endTimestamp: Long? = null,
  val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED, EMERGENCY
  val selectedContactIds: List<String> = emptyList(),
  val finalDistanceKm: Double? = null,
  val finalDurationMinutes: Int? = null,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "journeyId" to journeyId,
    "userId" to userId,
    "destination" to destination,
    "startLocation" to startLocation,
    "startTimestamp" to startTimestamp,
    "endTimestamp" to endTimestamp,
    "status" to status,
    "selectedContactIds" to selectedContactIds,
    "finalDistanceKm" to finalDistanceKm,
    "finalDurationMinutes" to finalDurationMinutes,
    "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}

/**
 * Firestore representation for a location breadcrumb along a journey.
 * Subcollection: journeys/{journeyId}/locations/{locationId}
 */
data class FirestoreJourneyLocation(
  val locationId: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val accuracyMeters: Float = 0f,
  val speedKmh: Double = 0.0,
  val timestamp: Long = System.currentTimeMillis(),
  val address: String = "",
  val journeyId: String = ""
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "locationId" to locationId,
    "latitude" to latitude,
    "longitude" to longitude,
    "accuracyMeters" to accuracyMeters,
    "speedKmh" to speedKmh,
    "timestamp" to timestamp,
    "address" to address,
    "journeyId" to journeyId
  )
}

/**
 * Firestore representation for an emergency event.
 * Collection: emergencies/{emergencyId}
 */
data class FirestoreEmergency(
  val emergencyId: String = "",
  val journeyId: String? = null,
  val userId: String = "",
  val status: String = "ACTIVE", // ACTIVE, RESOLVED, CANCELLED
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val accuracyMeters: Float = 0f,
  val timestamp: Long = System.currentTimeMillis(),
  val selectedContactIds: List<String> = emptyList(),
  val voiceMessageUrl: String? = null,
  val voiceTranscription: String? = null,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "emergencyId" to emergencyId,
    "journeyId" to journeyId,
    "userId" to userId,
    "status" to status,
    "latitude" to latitude,
    "longitude" to longitude,
    "accuracyMeters" to accuracyMeters,
    "timestamp" to timestamp,
    "selectedContactIds" to selectedContactIds,
    "voiceMessageUrl" to voiceMessageUrl,
    "voiceTranscription" to voiceTranscription,
    "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}

/**
 * Firestore representation for user device FCM registration.
 * Subcollection: users/{userId}/devices/{deviceId}
 */
data class FirestoreDeviceToken(
  val deviceId: String = "",
  val fcmToken: String = "",
  val platform: String = "ANDROID",
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "deviceId" to deviceId,
    "fcmToken" to fcmToken,
    "platform" to platform,
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}

/**
 * Firestore representation for community hazards.
 * Collection: communityHazards/{hazardId}
 */
data class FirestoreCommunityHazard(
  val hazardId: String = "",
  val hazardType: String = "OTHER",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val description: String = "",
  val reportedBy: String = "",
  val status: String = "REPORTED", // REPORTED, UNDER_REVIEW, VERIFIED, RESOLVED
  val title: String = "",
  val locationAddress: String = "",
  val confirmationsCount: Int = 0,
  @ServerTimestamp val createdAt: Date? = null,
  @ServerTimestamp val updatedAt: Date? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "hazardId" to hazardId,
    "hazardType" to hazardType,
    "latitude" to latitude,
    "longitude" to longitude,
    "description" to description,
    "reportedBy" to reportedBy,
    "status" to status,
    "title" to title,
    "locationAddress" to locationAddress,
    "confirmationsCount" to confirmationsCount,
    "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
  )
}
