package com.example.local.mapper

import com.example.firebase.model.*
import com.example.local.entity.*
import com.example.model.*

/**
 * Mapping functions between existing domain models and Room entities.
 * Preserves all domain models exactly as they are.
 */
object ModelMappers {

  // --- Stops Serialization Helpers ---

  fun stopsToJson(stops: List<JourneyStop>): String {
    if (stops.isEmpty()) return ""
    return try {
      val arr = org.json.JSONArray()
      stops.forEach { stop ->
        val obj = org.json.JSONObject().apply {
          put("id", stop.id)
          put("name", stop.name)
          put("stayDurationMinutes", stop.stayDurationMinutes)
          put("lat", stop.lat)
          put("lng", stop.lng)
          put("isCompleted", stop.isCompleted)
          put("isCurrentTarget", stop.isCurrentTarget)
          stop.arrivedAtTimestamp?.let { put("arrivedAtTimestamp", it) }
          stop.departedAtTimestamp?.let { put("departedAtTimestamp", it) }
        }
        arr.put(obj)
      }
      arr.toString()
    } catch (e: Exception) {
      ""
    }
  }

  fun jsonToStops(json: String?): List<JourneyStop> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
      val arr = org.json.JSONArray(json)
      val list = mutableListOf<JourneyStop>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          JourneyStop(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", ""),
            stayDurationMinutes = obj.optInt("stayDurationMinutes", 10),
            lat = obj.optDouble("lat", 0.0),
            lng = obj.optDouble("lng", 0.0),
            isCompleted = obj.optBoolean("isCompleted", false),
            isCurrentTarget = obj.optBoolean("isCurrentTarget", false),
            arrivedAtTimestamp = if (obj.has("arrivedAtTimestamp")) obj.optLong("arrivedAtTimestamp") else null,
            departedAtTimestamp = if (obj.has("departedAtTimestamp")) obj.optLong("departedAtTimestamp") else null
          )
        )
      }
      list
    } catch (e: Exception) {
      emptyList()
    }
  }

  // --- Journey Mappings ---

  fun JourneySession.toEntity(
    journeyId: String = if (this.journeyId.isNotBlank()) this.journeyId else "journey_${System.currentTimeMillis()}",
    userId: String = "user_default",
    startTimestamp: Long = System.currentTimeMillis(),
    endTimestamp: Long? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): JourneyEntity {
    val status = when {
      isDestinationReached -> "COMPLETED"
      isMonitoring -> "ACTIVE"
      else -> "PENDING"
    }

    return JourneyEntity(
      journeyId = journeyId,
      userId = userId,
      startLocation = startLocation,
      destination = destination,
      note = note,
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      initialDistanceKm = initialDistanceKm,
      remainingKm = remainingKm,
      etaMinutes = etaMinutes,
      progress = progress,
      isDestinationReached = isDestinationReached,
      isMonitoring = isMonitoring,
      status = status,
      startTimestamp = startTimestamp,
      endTimestamp = endTimestamp,
      durationMinutes = durationMinutes,
      totalKm = totalKm,
      selectedContactIdsCsv = selectedContactIds.joinToString(","),
      stopsJson = stopsToJson(stops),
      currentStopIndex = currentStopIndex,
      syncStatus = syncStatus
    )
  }

  fun JourneyEntity.toDomainModel(
    roadRoutePoints: List<Pair<Double, Double>> = emptyList()
  ): JourneySession {
    val contactIds = if (selectedContactIdsCsv.isBlank()) {
      emptySet()
    } else {
      selectedContactIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val parsedStops = jsonToStops(stopsJson)

    return JourneySession(
      journeyId = journeyId,
      startLocation = startLocation,
      destination = destination,
      note = note,
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      initialDistanceKm = initialDistanceKm,
      remainingKm = remainingKm,
      etaMinutes = etaMinutes,
      progress = progress,
      isDestinationReached = isDestinationReached,
      isMonitoring = isMonitoring,
      durationMinutes = durationMinutes,
      totalKm = totalKm,
      roadRoutePoints = roadRoutePoints,
      selectedContactIds = contactIds,
      stops = parsedStops,
      currentStopIndex = currentStopIndex
    )
  }

  fun JourneyEntity.toRecord(
    roadRoutePoints: List<Pair<Double, Double>> = emptyList()
  ): JourneyRecord {
    val finalDistance = if (totalKm > 0.0) totalKm else initialDistanceKm
    val finalDuration = if (durationMinutes > 0) durationMinutes else etaMinutes
    val calculatedSafetyScore = when {
      status == "EMERGENCY" -> 45
      status == "CANCELLED" -> 78
      isDestinationReached -> 98
      else -> 92
    }

    return JourneyRecord(
      id = journeyId,
      startLocation = startLocation,
      destination = destination,
      note = note,
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      distanceKm = finalDistance,
      durationMinutes = finalDuration,
      status = status,
      startTimestamp = startTimestamp,
      endTimestamp = endTimestamp,
      safetyScore = calculatedSafetyScore,
      nearbyHazardsCount = 0,
      roadRoutePoints = roadRoutePoints
    )
  }

  // --- Location Mappings ---

  fun UserLocationState.toEntity(
    journeyId: String,
    speedKmh: Double = 0.0,
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): JourneyLocationEntity {
    return JourneyLocationEntity(
      journeyId = journeyId,
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      speedKmh = speedKmh,
      timestamp = lastUpdatedTime,
      address = address,
      syncStatus = syncStatus
    )
  }

  // --- Emergency Mappings ---

  fun EmergencyPayload.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): EmergencyEntity {
    return EmergencyEntity(
      emergencyId = emergencyId,
      journeyId = journeyId,
      userId = userId,
      status = status,
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      timestamp = timestamp,
      message = message,
      locationAddress = locationAddress,
      notifiedContactsCsv = targetContactIds.joinToString(","),
      voiceMessageUrl = voiceMessageUrl,
      localVoiceFilePath = localVoiceFilePath,
      voiceTranscription = voiceTranscription,
      isResolved = status == "RESOLVED",
      syncStatus = syncStatus
    )
  }

  fun EmergencyAlertState.toEntity(
    emergencyId: String = if (this.emergencyId.isNotBlank()) this.emergencyId else "em_${System.currentTimeMillis()}",
    journeyId: String? = null,
    userId: String = "user_default",
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): EmergencyEntity {
    return EmergencyEntity(
      emergencyId = emergencyId,
      journeyId = journeyId,
      userId = userId,
      status = if (isResolved) "RESOLVED" else "ACTIVE",
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis(),
      message = "EMERGENCY SOS TRIGGERED",
      locationAddress = address,
      notifiedContactsCsv = notifiedContacts.joinToString(","),
      communicationMethod = communicationMethod.name,
      voiceMessageUrl = voiceMessage.audioUrl,
      localVoiceFilePath = voiceMessage.localFilePath,
      voiceTranscription = voiceMessage.transcription,
      isResolved = isResolved,
      syncStatus = syncStatus
    )
  }

  // --- Trusted Contact Mappings ---

  fun TrustedContact.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): TrustedContactEntity {
    return TrustedContactEntity(
      id = id,
      name = name,
      relationship = relationship,
      phone = phone,
      isPrimary = isPrimary,
      avatarUrl = avatarUrl,
      syncStatus = syncStatus
    )
  }

  fun TrustedContactEntity.toDomainModel(): TrustedContact {
    return TrustedContact(
      id = id,
      name = name,
      relationship = relationship,
      phone = phone,
      isPrimary = isPrimary,
      avatarUrl = avatarUrl
    )
  }

  // --- Community Hazard Mappings ---

  fun HazardReport.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING
  ): CommunityHazardEntity {
    return CommunityHazardEntity(
      id = id,
      title = title,
      hazardType = type.name,
      locationAddress = locationAddress,
      reportedTime = reportedTime,
      status = status.name,
      description = description,
      photoUrl = photoUrl,
      confirmationsCount = confirmationsCount,
      userVoted = userVoted,
      distanceAway = distanceAway,
      latitude = latitude,
      longitude = longitude,
      isReportedByMe = isReportedByMe,
      syncStatus = syncStatus
    )
  }

  fun CommunityHazardEntity.toDomainModel(): HazardReport {
    val hazardType = try {
      HazardType.valueOf(hazardType)
    } catch (e: Exception) {
      HazardType.OTHER
    }

    val hazardStatus = try {
      HazardStatus.valueOf(status)
    } catch (e: Exception) {
      HazardStatus.REPORTED
    }

    return HazardReport(
      id = id,
      title = title,
      type = hazardType,
      locationAddress = locationAddress,
      reportedTime = reportedTime,
      status = hazardStatus,
      description = description,
      photoUrl = photoUrl,
      confirmationsCount = confirmationsCount,
      userVoted = userVoted,
      distanceAway = distanceAway,
      latitude = latitude,
      longitude = longitude,
      isReportedByMe = isReportedByMe
    )
  }

  // --- Firestore Sync Mappings ---

  fun JourneyEntity.toFirestoreJourney(): FirestoreJourney {
    val contactIds = if (selectedContactIdsCsv.isBlank()) {
      emptyList()
    } else {
      selectedContactIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return FirestoreJourney(
      journeyId = journeyId,
      userId = userId,
      destination = destination,
      startLocation = startLocation,
      startTimestamp = startTimestamp,
      endTimestamp = endTimestamp,
      status = status,
      selectedContactIds = contactIds,
      finalDistanceKm = if (totalKm > 0.0) totalKm else null,
      finalDurationMinutes = if (durationMinutes > 0) durationMinutes else null
    )
  }

  fun JourneyLocationEntity.toFirestoreLocation(): FirestoreJourneyLocation {
    return FirestoreJourneyLocation(
      locationId = "loc_${id}",
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      speedKmh = speedKmh,
      timestamp = timestamp,
      address = address,
      journeyId = journeyId
    )
  }

  fun EmergencyEntity.toFirestoreEmergency(): FirestoreEmergency {
    val contactIds = if (notifiedContactsCsv.isBlank()) {
      emptyList()
    } else {
      notifiedContactsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return FirestoreEmergency(
      emergencyId = emergencyId,
      journeyId = journeyId,
      userId = userId,
      status = status,
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters.toFloat(),
      timestamp = timestamp,
      selectedContactIds = contactIds,
      voiceMessageUrl = voiceMessageUrl,
      voiceTranscription = voiceTranscription
    )
  }

  fun TrustedContactEntity.toFirestoreTrustedContact(): FirestoreTrustedContact {
    return FirestoreTrustedContact(
      contactId = id,
      name = name,
      phoneNumber = phone,
      relationship = relationship,
      isPrimary = isPrimary
    )
  }

  fun CommunityHazardEntity.toFirestoreHazard(userId: String = "user_default"): FirestoreCommunityHazard {
    return FirestoreCommunityHazard(
      hazardId = id,
      hazardType = hazardType,
      latitude = latitude,
      longitude = longitude,
      description = if (description.isNotBlank()) description else title,
      reportedBy = userId,
      status = status,
      title = title,
      locationAddress = locationAddress,
      confirmationsCount = confirmationsCount
    )
  }

  fun FirestoreCommunityHazard.toEntity(
    syncStatus: SyncStatus = SyncStatus.SYNCED
  ): CommunityHazardEntity {
    val hType = try {
      com.example.model.HazardType.valueOf(hazardType)
    } catch (_: Throwable) {
      com.example.model.HazardType.OTHER
    }
    val hStatus = try {
      com.example.model.HazardStatus.valueOf(status)
    } catch (_: Throwable) {
      com.example.model.HazardStatus.REPORTED
    }
    val resolvedTitle = if (title.isNotBlank()) title else hType.displayName
    val resolvedAddress = if (locationAddress.isNotBlank()) locationAddress else "Reported location"
    return CommunityHazardEntity(
      id = hazardId,
      title = resolvedTitle,
      hazardType = hType.name,
      locationAddress = resolvedAddress,
      reportedTime = "Recently",
      status = hStatus.name,
      description = description,
      photoUrl = null,
      confirmationsCount = confirmationsCount,
      userVoted = null,
      distanceAway = "Nearby",
      latitude = latitude,
      longitude = longitude,
      syncStatus = syncStatus
    )
  }

  fun FirestoreCommunityHazard.toDomainModel(): com.example.model.HazardReport {
    return toEntity().toDomainModel()
  }
}
