package com.example.firebase.repository

import com.example.firebase.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository providing clean, structured Firestore operations for Safe Companion.
 * - users/{userId}
 * - users/{userId}/trustedContacts/{contactId}
 * - journeys/{journeyId}
 * - journeys/{journeyId}/locations/{locationId}
 * - emergencies/{emergencyId}
 * - communityHazards/{hazardId}
 */
open class FirestoreRepository(
  protected val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {

  // --- Users ---

  open suspend fun saveUser(user: FirestoreUser): Result<Unit> = runCatching {
    require(user.userId.isNotBlank()) { "userId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("users")
      .document(user.userId)
      .set(user.toMap(), SetOptions.merge())
      .await()
  }

  open suspend fun getUser(userId: String): Result<FirestoreUser?> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val snapshot = fs.collection("users").document(userId).get().await()
    if (snapshot.exists()) {
      FirestoreUser(
        userId = snapshot.getString("userId") ?: userId,
        displayName = snapshot.getString("displayName") ?: "",
        email = snapshot.getString("email"),
        phoneNumber = snapshot.getString("phoneNumber"),
        createdAt = snapshot.getDate("createdAt"),
        updatedAt = snapshot.getDate("updatedAt")
      )
    } else {
      null
    }
  }

  // --- Trusted Contacts ---

  open suspend fun saveTrustedContact(
    userId: String,
    contact: FirestoreTrustedContact
  ): Result<Unit> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    require(contact.contactId.isNotBlank()) { "contactId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("users")
      .document(userId)
      .collection("trustedContacts")
      .document(contact.contactId)
      .set(contact.toMap(), SetOptions.merge())
      .await()
  }

  open suspend fun getTrustedContacts(userId: String): Result<List<FirestoreTrustedContact>> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val snapshot = fs.collection("users")
      .document(userId)
      .collection("trustedContacts")
      .get()
      .await()

    snapshot.documents.map { doc ->
      FirestoreTrustedContact(
        contactId = doc.getString("contactId") ?: doc.id,
        name = doc.getString("name") ?: "",
        phoneNumber = doc.getString("phoneNumber") ?: "",
        relationship = doc.getString("relationship") ?: "",
        isPrimary = doc.getBoolean("isPrimary") ?: false,
        createdAt = doc.getDate("createdAt"),
        updatedAt = doc.getDate("updatedAt")
      )
    }
  }

  open suspend fun deleteTrustedContact(userId: String, contactId: String): Result<Unit> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    require(contactId.isNotBlank()) { "contactId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("users")
      .document(userId)
      .collection("trustedContacts")
      .document(contactId)
      .delete()
      .await()
  }

  // --- Journeys ---

  open suspend fun createJourney(journey: FirestoreJourney): Result<Unit> = runCatching {
    require(journey.journeyId.isNotBlank()) { "journeyId must not be blank" }
    require(journey.userId.isNotBlank()) { "userId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("journeys")
      .document(journey.journeyId)
      .set(journey.toMap(), SetOptions.merge())
      .await()
  }

  open suspend fun updateJourneyStatus(
    journeyId: String,
    status: String,
    endTimestamp: Long? = null,
    finalDistanceKm: Double? = null,
    finalDurationMinutes: Int? = null
  ): Result<Unit> = runCatching {
    require(journeyId.isNotBlank()) { "journeyId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val updates = mutableMapOf<String, Any?>(
      "status" to status,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (endTimestamp != null) updates["endTimestamp"] = endTimestamp
    if (finalDistanceKm != null) updates["finalDistanceKm"] = finalDistanceKm
    if (finalDurationMinutes != null) updates["finalDurationMinutes"] = finalDurationMinutes

    fs.collection("journeys")
      .document(journeyId)
      .update(updates)
      .await()
  }

  open suspend fun addJourneyLocation(
    journeyId: String,
    location: FirestoreJourneyLocation
  ): Result<Unit> = runCatching {
    require(journeyId.isNotBlank()) { "journeyId must not be blank" }
    require(location.locationId.isNotBlank()) { "locationId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("journeys")
      .document(journeyId)
      .collection("locations")
      .document(location.locationId)
      .set(location.toMap(), SetOptions.merge())
      .await()
  }

  // --- Emergencies ---

  open suspend fun createEmergency(emergency: FirestoreEmergency): Result<Unit> = runCatching {
    require(emergency.emergencyId.isNotBlank()) { "emergencyId must not be blank" }
    require(emergency.userId.isNotBlank()) { "userId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("emergencies")
      .document(emergency.emergencyId)
      .set(emergency.toMap(), SetOptions.merge())
      .await()
  }

  open suspend fun updateEmergencyStatus(
    emergencyId: String,
    status: String,
    voiceMessageUrl: String? = null
  ): Result<Unit> = runCatching {
    require(emergencyId.isNotBlank()) { "emergencyId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val updates = mutableMapOf<String, Any?>(
      "status" to status,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (voiceMessageUrl != null) updates["voiceMessageUrl"] = voiceMessageUrl

    fs.collection("emergencies")
      .document(emergencyId)
      .update(updates)
      .await()
  }

  // --- Community Hazards (Foundation) ---

  open suspend fun reportHazard(hazard: FirestoreCommunityHazard): Result<Unit> = runCatching {
    require(hazard.hazardId.isNotBlank()) { "hazardId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    fs.collection("communityHazards")
      .document(hazard.hazardId)
      .set(hazard.toMap(), SetOptions.merge())
      .await()
  }

  open suspend fun getCommunityHazards(): Result<List<FirestoreCommunityHazard>> = runCatching {
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val snapshot = fs.collection("communityHazards").get().await()
    snapshot.documents.mapNotNull { doc ->
      val id = doc.id
      val type = doc.getString("hazardType") ?: "OTHER"
      val lat = doc.getDouble("latitude") ?: 0.0
      val lng = doc.getDouble("longitude") ?: 0.0
      val desc = doc.getString("description") ?: ""
      val rep = doc.getString("reportedBy") ?: ""
      val status = doc.getString("status") ?: "REPORTED"
      val title = doc.getString("title") ?: ""
      val address = doc.getString("locationAddress") ?: ""
      val conf = doc.getLong("confirmationsCount")?.toInt() ?: 0
      FirestoreCommunityHazard(
        hazardId = id,
        hazardType = type,
        latitude = lat,
        longitude = lng,
        description = desc,
        reportedBy = rep,
        status = status,
        title = title,
        locationAddress = address,
        confirmationsCount = conf
      )
    }
  }

  open suspend fun updateHazardStatus(
    hazardId: String,
    status: String? = null,
    confirmationsCount: Int? = null
  ): Result<Unit> = runCatching {
    require(hazardId.isNotBlank()) { "hazardId must not be blank" }
    val fs = firestore ?: throw IllegalStateException("Firebase is not initialized")
    val updates = mutableMapOf<String, Any?>(
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (status != null) updates["status"] = status
    if (confirmationsCount != null) updates["confirmationsCount"] = confirmationsCount
    fs.collection("communityHazards")
      .document(hazardId)
      .set(updates, SetOptions.merge())
      .await()
  }
}
