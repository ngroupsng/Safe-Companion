package com.example.model

enum class PlaceCategory(val displayName: String, val iconName: String) {
  HOME("Home", "home"),
  WORK("Work", "work"),
  COLLEGE("College / University", "school"),
  GYM("Gym / Fitness", "fitness_center"),
  FAVORITE("Favorite Spot", "star"),
  OTHER("Other Address", "place")
}

data class SavedPlace(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val address: String,
  val latitude: Double = 37.7749,
  val longitude: Double = -122.4194,
  val category: PlaceCategory = PlaceCategory.OTHER,
  val isHome: Boolean = false,
  val isWork: Boolean = false
)

data class UserProfile(
  val name: String = "",
  val phone: String = "",
  val email: String = "",
  val avatarUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuBacyBMwEukiqEtqaGU9fec4OR4O0ZAYinMKyx1essctGSyRB51Yirahy_--pB7tTZkBt0Ayvf3nOGTD5R3iOlk7_925RXuLgQIWm6LgOs5Fpek-pXDvZDhRRhJiF5ayjy5fdkQdpk-6RpOf851i4yQEA2-zcCMArVnpMgr9xN_dC3UV1aPPi5oBYkj9ikCajUNj0e7QuaoGGJIdBYUtFGVcfelz0hJxDV6pG2hfiY1ee_gKcd3twuIWA",
  val emergencyNotes: String = "No known allergies.",
  val preRecordedVoicePath: String? = null,
  val preRecordedVoiceTranscription: String = "EMERGENCY SOS: I have had an accident and cannot speak. Please send immediate help to my current GPS coordinates!",
  val preRecordedVoiceDurationSeconds: Int = 0,
  val hasPreRecordedVoiceNote: Boolean = false,
  val savedPlaces: List<SavedPlace> = emptyList()
)

data class TrustedContact(
  val id: String,
  val name: String,
  val relationship: String,
  val phone: String,
  val isPrimary: Boolean = false,
  val avatarUrl: String? = null,
  val initials: String = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
)

enum class HazardType(val displayName: String, val iconName: String) {
  POTHOLE("Pothole", "warning"),
  BROKEN_STREETLIGHT("Broken Streetlight", "lightbulb"),
  BLOCKED_ROAD("Blocked Road", "block"),
  UNSAFE_AREA("Unsafe Area", "security"),
  OTHER("Other", "more_horiz")
}

enum class HazardStatus(val label: String) {
  REPORTED("Reported"),
  UNDER_REVIEW("Under Review"),
  VERIFIED("Verified"),
  RESOLVED("Resolved")
}

data class HazardReport(
  val id: String,
  val title: String,
  val type: HazardType,
  val locationAddress: String,
  val reportedTime: String,
  val status: HazardStatus,
  val description: String,
  val photoUrl: String? = null,
  val confirmationsCount: Int = 3,
  val userVoted: Boolean? = null,
  val distanceAway: String = "450m away",
  val latitude: Double = 37.7749,
  val longitude: Double = -122.4194,
  val isReportedByMe: Boolean = false
)

enum class RouteDeviationState {
  NORMAL,
  WARNING,
  DEVIATED
}

enum class LongStopState {
  NORMAL,
  STATIONARY,
  LONG_STOP_WARNING
}

data class JourneyStop(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String = "",
  val stayDurationMinutes: Int = 10,
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val isCompleted: Boolean = false,
  val isCurrentTarget: Boolean = false,
  val arrivedAtTimestamp: Long? = null,
  val departedAtTimestamp: Long? = null
)

data class JourneySession(
  val journeyId: String = "",
  val startLocation: String = "Current Location",
  val destination: String = "Home",
  val note: String = "",
  val startLat: Double = 37.7749,
  val startLng: Double = -122.4194,
  val destLat: Double = 37.7833,
  val destLng: Double = -122.4064,
  val initialDistanceKm: Double = 4.2,
  val remainingKm: Double = 4.2,
  val etaMinutes: Int = 18,
  val progress: Float = 0.0f,
  val isDestinationValid: Boolean = true,
  val isDestinationReached: Boolean = false,
  val currentSpeedKmh: Double = 5.0,
  val batteryPercent: Int = 78,
  val networkStatus: String = "Online",
  val warningsCount: Int = 0,
  val emergencyAlertsCount: Int = 0,
  val isMonitoring: Boolean = true,
  val durationMinutes: Int = 32,
  val totalKm: Double = 8.4,
  val routeDeviationState: RouteDeviationState = RouteDeviationState.NORMAL,
  val deviationDistanceMeters: Float = 0f,
  val deviationThresholdMeters: Float = 50f,
  val consecutiveDeviationReadings: Int = 0,
  val lastDeviationTimestamp: Long = 0L,
  val isDeviationAcknowledgedByUser: Boolean = false,
  val longStopState: LongStopState = LongStopState.NORMAL,
  val stationarySinceTimestamp: Long = 0L,
  val stationaryDurationMinutes: Int = 0,
  val stationaryAnchorLat: Double = 0.0,
  val stationaryAnchorLng: Double = 0.0,
  val isLongStopAcknowledged: Boolean = false,
  val roadRoutePoints: List<Pair<Double, Double>> = emptyList(),
  val selectedContactIds: Set<String> = emptySet(),
  val safetyScore: Int = 100,
  val nearbyHazardsCount: Int = 0,
  val routeRecommendationNote: String = "",
  val stops: List<JourneyStop> = emptyList(),
  val currentStopIndex: Int = 0,
  val currentLegDistanceKm: Double = 0.0,
  val currentLegEtaMinutes: Int = 0,
  val totalStaysMinutes: Int = 0,
  val isAtStop: Boolean = false,
  val currentStopStayRemainingMinutes: Int = 0
) {
  val hasMultipleDestinations: Boolean
    get() = stops.isNotEmpty()

  val hasUncompletedStops: Boolean
    get() = stops.isNotEmpty() && currentStopIndex < stops.size

  val currentTargetName: String
    get() = if (hasUncompletedStops) {
      stops[currentStopIndex].name.ifBlank { "Stop ${currentStopIndex + 1}" }
    } else {
      destination
    }

  val nextTargetNameAfterCurrent: String?
    get() = if (stops.isNotEmpty()) {
      if (currentStopIndex + 1 < stops.size) {
        stops[currentStopIndex + 1].name.ifBlank { "Stop ${currentStopIndex + 2}" }
      } else if (currentStopIndex < stops.size) {
        destination
      } else null
    } else null

  val completedStopsCount: Int
    get() = stops.count { it.isCompleted }

  val totalStopsCount: Int
    get() = stops.size
}

enum class CommunicationMethod(val displayName: String) {
  NONE("None"),
  ONLINE_FCM("Online Cloud (FCM)"),
  SMS_FALLBACK("Offline SMS Fallback")
}

enum class DeliveryStatus(val label: String) {
  PENDING("Pending"),
  SENT("Sent"),
  DELIVERED("Delivered"),
  FAILED("Failed"),
  ACKNOWLEDGED("Acknowledged")
}

data class ContactNotificationStatus(
  val contactId: String,
  val contactName: String,
  val phone: String,
  val method: CommunicationMethod,
  val status: DeliveryStatus,
  val timestamp: Long = System.currentTimeMillis(),
  val errorMessage: String? = null
)

enum class VoiceRecordingState {
  IDLE,
  RECORDING,
  RECORDED,
  UPLOADING,
  UPLOADED,
  FAILED
}

data class VoiceMessageData(
  val localFilePath: String? = null,
  val audioDurationSeconds: Int = 0,
  val audioUrl: String? = null,
  val state: VoiceRecordingState = VoiceRecordingState.IDLE,
  val errorMessage: String? = null,
  val transcription: String? = null,
  val isOfflineRecorded: Boolean = false,
  val isQueuedForSync: Boolean = false
)

data class EmergencyPayload(
  val emergencyId: String,
  val status: String = "ACTIVE",
  val userId: String = "user_default",
  val userName: String = "Safe Companion User",
  val userPhone: String = "",
  val journeyId: String? = null,
  val journeyDestination: String? = null,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Int,
  val timestamp: Long,
  val message: String,
  val locationAddress: String = "",
  val voiceMessageUrl: String? = null,
  val voiceTranscription: String? = null,
  val localVoiceFilePath: String? = null,
  val voiceDurationSeconds: Int = 0,
  val isOfflineFallback: Boolean = false,
  val targetContactIds: List<String> = emptyList()
)

data class EmergencyAlertState(
  val isTriggered: Boolean = false,
  val emergencyId: String = "",
  val stepIndex: Int = 3, // 1: Created, 2: Location captured, 3: Alert being sent, 4: Delivery, 5: Acknowledged
  val accuracyMeters: Int = 5,
  val isLocationAvailable: Boolean = true,
  val internetAvailable: Boolean = true,
  val communicationMethod: CommunicationMethod = CommunicationMethod.NONE,
  val communicationFailed: Boolean = false,
  val failureReason: String? = null,
  val notifiedContacts: List<String> = listOf("Mom", "Dad"),
  val contactStatuses: List<ContactNotificationStatus> = emptyList(),
  val secondsHeld: Float = 0f,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val address: String = "",
  val timestamp: Long = 0L,
  val voiceMessage: VoiceMessageData = VoiceMessageData(),
  val isResolved: Boolean = false
)

data class IncomingEmergencyAlert(
  val emergencyId: String,
  val senderName: String,
  val senderPhone: String = "",
  val relationship: String = "Trusted Contact",
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Int,
  val timestamp: Long,
  val message: String,
  val journeyDestination: String? = null,
  val address: String = "",
  val voiceMessageUrl: String? = null,
  val voiceTranscription: String? = null,
  val localVoiceFilePath: String? = null,
  val isAlarmPlaying: Boolean = true,
  val isAcknowledged: Boolean = false,
  val receivedViaSms: Boolean = false
)

data class UserLocationState(
  val latitude: Double = 37.7749,
  val longitude: Double = -122.4194,
  val address: String = "Locating GPS...",
  val accuracyMeters: Float = 0f,
  val isPermissionGranted: Boolean = false,
  val isLocating: Boolean = false,
  val hasRealFix: Boolean = false,
  val lastUpdatedTime: Long = System.currentTimeMillis()
)

data class AppPermissions(
  val location: Boolean = false,
  val backgroundLocation: Boolean = false,
  val camera: Boolean = false,
  val notifications: Boolean = false,
  val smsFallback: Boolean = false
)

enum class AppMode {
  PERSONAL,
  COMMUNITY
}

enum class BottomTab {
  HOME,
  JOURNEY,
  COMMUNITY,
  PROFILE
}

data class JourneyRecord(
  val id: String,
  val startLocation: String,
  val destination: String,
  val note: String = "",
  val startLat: Double = 37.7749,
  val startLng: Double = -122.4194,
  val destLat: Double = 37.7833,
  val destLng: Double = -122.4064,
  val distanceKm: Double = 0.0,
  val durationMinutes: Int = 0,
  val status: String = "COMPLETED", // COMPLETED, ACTIVE, CANCELLED
  val startTimestamp: Long = System.currentTimeMillis(),
  val endTimestamp: Long? = null,
  val safetyScore: Int = 95,
  val nearbyHazardsCount: Int = 0,
  val roadRoutePoints: List<Pair<Double, Double>> = emptyList()
) {
  fun getFormattedDate(): String {
    val date = java.util.Date(startTimestamp)
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • h:mm a", java.util.Locale.getDefault())
    return sdf.format(date)
  }

  fun getRelativeDate(): String {
    val now = System.currentTimeMillis()
    val diff = now - startTimestamp
    return when {
      diff < 3600000L -> "Just now"
      diff < 86400000L -> "Today"
      diff < 172800000L -> "Yesterday"
      else -> {
        val days = (diff / 86400000L).toInt()
        "$days days ago"
      }
    }
  }
}

enum class NotificationType(val title: String) {
  EMERGENCY("Emergency Alert"),
  SAFE_ARRIVAL("Safe Arrival"),
  JOURNEY_ALERT("Journey Warning"),
  HAZARD("Safety Hazard"),
  SYSTEM("System & Sensors")
}

data class SafetyNotification(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val type: NotificationType = NotificationType.SYSTEM,
  val isRead: Boolean = false,
  val targetScreenRoute: String? = null,
  val destination: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null
) {
  fun getRelativeTime(): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
      diff < 60_000L -> "Just now"
      diff < 3600_000L -> "${diff / 60_000L}m ago"
      diff < 86400_000L -> "${diff / 3600_000L}h ago"
      diff < 172800_000L -> "Yesterday"
      else -> {
        val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
      }
    }
  }
}

