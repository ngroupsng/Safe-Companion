package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.local.SafeCompanionDatabase
import com.example.local.entity.SyncStatus
import com.example.local.mapper.ModelMappers.toDomainModel
import com.example.local.mapper.ModelMappers.toEntity
import com.example.local.mapper.ModelMappers.toRecord
import com.example.local.repository.SafeCompanionLocalRepository
import com.example.sync.SafeCompanionSyncManager
import com.example.model.*
import com.example.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed class Screen(val route: String) {
  object Onboarding : Screen("onboarding")
  object Dashboard : Screen("dashboard")
  object JourneySetup : Screen("journey_setup")
  object ActiveJourney : Screen("active_journey")
  object SosConfirmation : Screen("sos_confirmation")
  object EmergencyStatus : Screen("emergency_status")
  object TrustedContactEmergency : Screen("trusted_contact_emergency")
  object JourneyCompleted : Screen("journey_completed")
  object JourneyMap : Screen("journey_map")
  object CommunityDashboard : Screen("community_dashboard")
  object CommunityReports : Screen("community_reports")
  object ReportHazard : Screen("report_hazard")
  object ReportDetails : Screen("report_details")
  object Settings : Screen("settings")
  object SavedPlaces : Screen("saved_places")
  object TrustedContacts : Screen("trusted_contacts")
  object PermissionsSettings : Screen("permissions_settings")
  object PrivacySettings : Screen("privacy_settings")
  object Notifications : Screen("notifications")
}

class SafeCompanionViewModel(
  localRepo: SafeCompanionLocalRepository? = null,
  syncMgr: SafeCompanionSyncManager? = null
) : ViewModel() {

  private var repository: SafeCompanionLocalRepository? = localRepo
  private var syncManager: SafeCompanionSyncManager? = syncMgr
  private var activeEmergencyId: String? = null

  init {
    localRepo?.let {
      observeHazardsFromRepository(it)
      observeJourneysFromRepository(it)
    }
  }

  private val _isOnboardingCompleted = MutableStateFlow(false)
  val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

  private val _onboardingStep = MutableStateFlow(1) // 1 to 7
  val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

  private val _currentScreen = MutableStateFlow<String>(Screen.Onboarding.route)
  val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

  private val _currentMode = MutableStateFlow(AppMode.PERSONAL)
  val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

  private val _currentTab = MutableStateFlow(BottomTab.HOME)
  val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  private val _permissions = MutableStateFlow(
    AppPermissions(
      location = false,
      notifications = false,
      backgroundLocation = false,
      smsFallback = false
    )
  )
  val permissions: StateFlow<AppPermissions> = _permissions.asStateFlow()

  private val _userLocation = MutableStateFlow(
    UserLocationState(
      latitude = 37.7749,
      longitude = -122.4194,
      isPermissionGranted = false,
      hasRealFix = false,
      accuracyMeters = 5.0f
    )
  )
  val userLocation: StateFlow<UserLocationState> = _userLocation.asStateFlow()

  private val defaultContacts = listOf(
    TrustedContact(
      id = "c1",
      name = "Sarah Miller",
      relationship = "Sister",
      phone = "+1 (555) 234-5678",
      isPrimary = true,
      avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBacyBMwEukiqEtqaGU9fec4OR4O0ZAYinMKyx1essctGSyRB51Yirahy_--pB7tTZkBt0Ayvf3nOGTD5R3iOlk7_925RXuLgQIWm6LgOs5Fpek-pXDvZDhRRhJiF5ayjy5fdkQdpk-6RpOf851i4yQEA2-zcCMArVnpMgr9xN_dC3UV1aPPi5oBYkj9ikCajUNj0e7QuaoGGJIdBYUtFGVcfelz0hJxDV6pG2hfiY1ee_gKcd3twuIWA"
    ),
    TrustedContact(
      id = "c2",
      name = "David Chen",
      relationship = "Partner",
      phone = "+1 (555) 876-5432",
      isPrimary = false
    )
  )

  private val _trustedContacts = MutableStateFlow<List<TrustedContact>>(defaultContacts)
  val trustedContacts: StateFlow<List<TrustedContact>> = _trustedContacts.asStateFlow()

  private val _selectedSharingContacts = MutableStateFlow<Set<String>>(setOf("c1", "c2"))
  val selectedSharingContacts: StateFlow<Set<String>> = _selectedSharingContacts.asStateFlow()

  private val defaultHazardReports = listOf(
    HazardReport(
      id = "h1",
      title = "Broken Streetlight",
      type = HazardType.BROKEN_STREETLIGHT,
      locationAddress = "Maple Avenue, near 5th St",
      reportedTime = "Oct 24, 2023 at 8:45 PM",
      status = HazardStatus.UNDER_REVIEW,
      description = "The streetlight has been flickering for three days and finally went out completely last night. It's quite dark in this corner.",
      photoUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCvYrexAljV0X1HRwL7sB0Gy8YApUEOX8_s8zco9iyobfilapZxU4wS8B4Ek4YhDoVl-W9aEoeqSwBnEsEUo0lEunFvYlfNCXJ90V1NAR87oc8cemYzKVo_sHqm8kSW-obst06xC0a4MBSRkAh2KdeXILWwEbmoWotMtDuOFZUoWdPObTRGQUlixBVh0qx8zq1FefrO0aEyPC2BF1XniKhWKLmiwGtDbwKaO-q_J6xK-RUgJg2MD9_aWA",
      confirmationsCount = 3,
      userVoted = true,
      distanceAway = "450m away",
      latitude = 37.7790,
      longitude = -122.4140,
      isReportedByMe = true
    ),
    HazardReport(
      id = "h2",
      title = "Large Pothole",
      type = HazardType.POTHOLE,
      locationAddress = "Downtown Plaza crosswalk",
      reportedTime = "Oct 23, 2023 at 3:15 PM",
      status = HazardStatus.REPORTED,
      description = "Deep pothole on the pedestrian crosswalk lane causing tripping hazard at night.",
      photoUrl = null,
      confirmationsCount = 7,
      distanceAway = "200m away",
      latitude = 37.7770,
      longitude = -122.4160,
      isReportedByMe = false
    ),
    HazardReport(
      id = "h3",
      title = "Sidewalk Blocked",
      type = HazardType.BLOCKED_ROAD,
      locationAddress = "5th & Elm St",
      reportedTime = "Oct 21, 2023 at 11:30 AM",
      status = HazardStatus.RESOLVED,
      description = "Construction equipment blocking sidewalk has been cleared now.",
      photoUrl = null,
      confirmationsCount = 12,
      distanceAway = "800m away",
      latitude = 37.7810,
      longitude = -122.4090,
      isReportedByMe = false
    ),
    HazardReport(
      id = "h4",
      title = "Uneven Pavement at Transit Stop",
      type = HazardType.POTHOLE,
      locationAddress = "Mission & 4th Transit Bay",
      reportedTime = "3 days ago",
      status = HazardStatus.VERIFIED,
      description = "Cracked concrete slab creates a tripping hazard for passengers boarding late buses.",
      photoUrl = null,
      confirmationsCount = 9,
      userVoted = true,
      distanceAway = "600m away",
      latitude = 37.7830,
      longitude = -122.4050,
      isReportedByMe = true
    )
  )

  private val _hazardReports = MutableStateFlow<List<HazardReport>>(defaultHazardReports)
  val hazardReports: StateFlow<List<HazardReport>> = _hazardReports.asStateFlow()

  private val _selectedHazard = MutableStateFlow<HazardReport?>(null)
  val selectedHazard: StateFlow<HazardReport?> = _selectedHazard.asStateFlow()

  private val _hazardFilter = MutableStateFlow<HazardStatus?>(null)
  val hazardFilter: StateFlow<HazardStatus?> = _hazardFilter.asStateFlow()

  // Community-Aware Route Safety State
  private val _routeOptions = MutableStateFlow<List<ScoredRouteOption>>(emptyList())
  val routeOptions: StateFlow<List<ScoredRouteOption>> = _routeOptions.asStateFlow()

  private val _selectedRouteOption = MutableStateFlow<ScoredRouteOption?>(null)
  val selectedRouteOption: StateFlow<ScoredRouteOption?> = _selectedRouteOption.asStateFlow()

  private val _isLoadingRoutes = MutableStateFlow(false)
  val isLoadingRoutes: StateFlow<Boolean> = _isLoadingRoutes.asStateFlow()

  private val _journeySession = MutableStateFlow(
    JourneySession(
      startLat = 37.7749,
      startLng = -122.4194,
      startLocation = "Market St & 4th St",
      destination = "Home",
      destLat = 37.7869,
      destLng = -122.4049,
      initialDistanceKm = 1.8,
      remainingKm = 1.8,
      etaMinutes = 22,
      progress = 0.0f,
      isMonitoring = false,
      routeDeviationState = com.example.model.RouteDeviationState.NORMAL,
      deviationDistanceMeters = 0f,
      consecutiveDeviationReadings = 0,
      isDeviationAcknowledgedByUser = false,
      longStopState = com.example.model.LongStopState.NORMAL,
      stationaryDurationMinutes = 0,
      stationarySinceTimestamp = 0L,
      stationaryAnchorLat = 37.7749,
      stationaryAnchorLng = -122.4194,
      isLongStopAcknowledged = false
    )
  )
  val journeySession: StateFlow<JourneySession> = _journeySession.asStateFlow()

  // Default historical journey records to provide an instant record of user journeys
  private val defaultJourneyRecords = listOf(
    JourneyRecord(
      id = "hist_1",
      startLocation = "Market St & 4th St",
      destination = "City Central Library",
      note = "Quiet study evening",
      startLat = 37.7850,
      startLng = -122.4060,
      destLat = 37.7785,
      destLng = -122.4150,
      distanceKm = 2.1,
      durationMinutes = 24,
      status = "COMPLETED",
      startTimestamp = System.currentTimeMillis() - 86400000L * 1, // Yesterday
      endTimestamp = System.currentTimeMillis() - 86400000L * 1 + 24 * 60 * 1000L,
      safetyScore = 98,
      nearbyHazardsCount = 0
    ),
    JourneyRecord(
      id = "hist_2",
      startLocation = "742 Evergreen Terrace",
      destination = "Montgomery Transit Station",
      note = "Morning commute via boulevard",
      startLat = 37.7749,
      startLng = -122.4194,
      destLat = 37.7892,
      destLng = -122.4014,
      distanceKm = 3.4,
      durationMinutes = 38,
      status = "COMPLETED",
      startTimestamp = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
      endTimestamp = System.currentTimeMillis() - 86400000L * 2 + 38 * 60 * 1000L,
      safetyScore = 94,
      nearbyHazardsCount = 1
    ),
    JourneyRecord(
      id = "hist_3",
      startLocation = "North Science Hall",
      destination = "Westside Student Commons",
      note = "Campus study group walk",
      startLat = 37.7700,
      startLng = -122.4250,
      destLat = 37.7760,
      destLng = -122.4180,
      distanceKm = 1.5,
      durationMinutes = 19,
      status = "COMPLETED",
      startTimestamp = System.currentTimeMillis() - 86400000L * 4, // 4 days ago
      endTimestamp = System.currentTimeMillis() - 86400000L * 4 + 19 * 60 * 1000L,
      safetyScore = 96,
      nearbyHazardsCount = 0
    ),
    JourneyRecord(
      id = "hist_4",
      startLocation = "Downtown Tech Hub",
      destination = "Home",
      note = "Late return via well-lit avenues",
      startLat = 37.7880,
      startLng = -122.4000,
      destLat = 37.7749,
      destLng = -122.4194,
      distanceKm = 4.2,
      durationMinutes = 45,
      status = "COMPLETED",
      startTimestamp = System.currentTimeMillis() - 86400000L * 6, // 6 days ago
      endTimestamp = System.currentTimeMillis() - 86400000L * 6 + 45 * 60 * 1000L,
      safetyScore = 92,
      nearbyHazardsCount = 2
    )
  )

  private val _journeyHistory = MutableStateFlow<List<JourneyRecord>>(defaultJourneyRecords)
  val journeyHistory: StateFlow<List<JourneyRecord>> = _journeyHistory.asStateFlow()

  private val _selectedHistoricalJourney = MutableStateFlow<JourneyRecord?>(defaultJourneyRecords.firstOrNull())
  val selectedHistoricalJourney: StateFlow<JourneyRecord?> = _selectedHistoricalJourney.asStateFlow()

  fun selectHistoricalJourney(record: JourneyRecord) {
    _selectedHistoricalJourney.value = record
    viewModelScope.launch {
      val points = if (record.roadRoutePoints.isNotEmpty()) {
        record.roadRoutePoints
      } else {
        RoutingService.fetchRoadRoute(
          startLat = record.startLat,
          startLng = record.startLng,
          destLat = record.destLat,
          destLng = record.destLng
        ).points
      }
      _journeySession.update {
        it.copy(
          destination = record.destination,
          startLocation = record.startLocation,
          startLat = record.startLat,
          startLng = record.startLng,
          destLat = record.destLat,
          destLng = record.destLng,
          roadRoutePoints = points,
          remainingKm = 0.0,
          totalKm = record.distanceKm,
          etaMinutes = record.durationMinutes,
          safetyScore = record.safetyScore,
          nearbyHazardsCount = record.nearbyHazardsCount,
          isMonitoring = false,
          isDestinationReached = true
        )
      }
    }
  }

  private val _notifications = MutableStateFlow<List<SafetyNotification>>(
    listOf(
      SafetyNotification(
        id = "notif_welcome",
        title = "Safe Companion Shield Active",
        message = "GPS positioning, trusted contact alerts, and emergency SMS fallback are ready.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
        type = NotificationType.SYSTEM,
        isRead = false,
        targetScreenRoute = Screen.Dashboard.route
      ),
      SafetyNotification(
        id = "notif_safe_arrival",
        title = "Safe Arrival Confirmed",
        message = "Safe arrival at Downtown Tech Hub was confirmed. Trusted contacts were notified.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
        type = NotificationType.SAFE_ARRIVAL,
        isRead = false,
        targetScreenRoute = Screen.JourneyCompleted.route
      ),
      SafetyNotification(
        id = "notif_hazard_alert",
        title = "Community Safety Alert",
        message = "Broken Streetlight reported on Maple Avenue. Stay in well-lit areas.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 18,
        type = NotificationType.HAZARD,
        isRead = true,
        targetScreenRoute = Screen.CommunityReports.route
      )
    )
  )
  val notifications: StateFlow<List<SafetyNotification>> = _notifications.asStateFlow()

  fun addNotification(notification: SafetyNotification) {
    _notifications.update { current -> listOf(notification) + current }
  }

  fun markNotificationAsRead(id: String) {
    _notifications.update { current ->
      current.map { if (it.id == id) it.copy(isRead = true) else it }
    }
  }

  fun markAllNotificationsAsRead() {
    _notifications.update { current ->
      current.map { it.copy(isRead = true) }
    }
  }

  fun deleteNotification(id: String) {
    _notifications.update { current ->
      current.filter { it.id != id }
    }
  }

  fun clearAllNotifications() {
    _notifications.value = emptyList()
  }

  fun sendTestNotification(context: Context) {
    val newNotif = SafetyNotification(
      id = "test_${System.currentTimeMillis()}",
      title = "🔔 Test Safety Notification",
      message = "Emergency monitoring, GPS tracking, and notifications are functioning properly.",
      timestamp = System.currentTimeMillis(),
      type = NotificationType.SYSTEM,
      isRead = false,
      targetScreenRoute = Screen.Notifications.route
    )
    addNotification(newNotif)
    SafetyNotificationManager.showTestNotification(
      context = context,
      title = newNotif.title,
      message = newNotif.message
    )
  }

  fun triggerRouteDeviation(metersOff: Float = 185f) {
    val session = _journeySession.value
    val offLat = session.startLat + 0.0016
    val offLng = session.startLng + 0.0015
    _userLocation.update {
      it.copy(
        latitude = offLat,
        longitude = offLng,
        hasRealFix = true,
        accuracyMeters = 5.0f
      )
    }
    _journeySession.update {
      it.copy(
        isMonitoring = true,
        routeDeviationState = com.example.model.RouteDeviationState.DEVIATED,
        deviationDistanceMeters = metersOff,
        consecutiveDeviationReadings = 4,
        isDeviationAcknowledgedByUser = false
      )
    }
    _currentScreen.value = Screen.ActiveJourney.route
    _currentTab.value = BottomTab.JOURNEY

    val devNotif = SafetyNotification(
      id = "dev_${System.currentTimeMillis()}",
      title = "⚠️ Route Deviation Detected",
      message = "You are ${metersOff.toInt()}m off your expected route to ${session.destination}.",
      timestamp = System.currentTimeMillis(),
      type = NotificationType.JOURNEY_ALERT,
      isRead = false,
      targetScreenRoute = Screen.ActiveJourney.route
    )
    addNotification(devNotif)
    appContext?.let { ctx ->
      SafetyNotificationManager.showJourneyAlertNotification(
        context = ctx,
        title = devNotif.title,
        message = devNotif.message,
        isDeviation = true
      )
    }
  }

  fun triggerLongStopAlert(durationMinutes: Int = 7) {
    val now = System.currentTimeMillis()
    val session = _journeySession.value
    _userLocation.update {
      it.copy(
        latitude = session.startLat,
        longitude = session.startLng,
        hasRealFix = true,
        accuracyMeters = 5.0f
      )
    }
    _journeySession.update {
      it.copy(
        isMonitoring = true,
        longStopState = com.example.model.LongStopState.LONG_STOP_WARNING,
        stationaryDurationMinutes = durationMinutes,
        stationarySinceTimestamp = now - (durationMinutes * 60 * 1000L),
        stationaryAnchorLat = session.startLat,
        stationaryAnchorLng = session.startLng,
        isLongStopAcknowledged = false
      )
    }
    _currentScreen.value = Screen.ActiveJourney.route
    _currentTab.value = BottomTab.JOURNEY

    val stopNotif = SafetyNotification(
      id = "stop_${System.currentTimeMillis()}",
      title = "⚠️ Stationary Alert: $durationMinutes min stop",
      message = "Unusual stop detected along your route to ${session.destination}.",
      timestamp = System.currentTimeMillis(),
      type = NotificationType.JOURNEY_ALERT,
      isRead = false,
      targetScreenRoute = Screen.ActiveJourney.route
    )
    addNotification(stopNotif)
    appContext?.let { ctx ->
      SafetyNotificationManager.showJourneyAlertNotification(
        context = ctx,
        title = stopNotif.title,
        message = stopNotif.message,
        isDeviation = false
      )
    }
  }

  init {
    EmergencyCommunicationHub.alertListener = { alert ->
      onIncomingEmergencyAlert(alert)
    }

    // Connect background JourneyTrackingService updates to ViewModel logic
    viewModelScope.launch {
      com.example.service.JourneyTrackingService.locationUpdatesFlow.collect { location ->
        handleLocationUpdate(location, appContext)
      }
    }

    // Pre-calculate realistic road network route for the default demo journey
    viewModelScope.launch {
      val session = _journeySession.value
      val roadRoute = com.example.util.RoutingService.fetchRoadRoute(
        startLat = session.startLat,
        startLng = session.startLng,
        destLat = session.destLat,
        destLng = session.destLng
      )
      val roadDistKm = roadRoute.distanceMeters / 1000.0
      val roadEta = Math.round(roadRoute.durationSeconds / 60.0).toInt().coerceAtLeast(1)
      _journeySession.update {
        it.copy(
          roadRoutePoints = roadRoute.points,
          initialDistanceKm = Math.round(roadDistKm * 10.0) / 10.0,
          remainingKm = Math.round(roadDistKm * 10.0) / 10.0,
          etaMinutes = roadEta
        )
      }
    }
  }

  private val _emergencyState = MutableStateFlow(EmergencyAlertState())
  val emergencyState: StateFlow<EmergencyAlertState> = _emergencyState.asStateFlow()

  private val _incomingAlert = MutableStateFlow<IncomingEmergencyAlert?>(null)
  val incomingAlert: StateFlow<IncomingEmergencyAlert?> = _incomingAlert.asStateFlow()

  var emergencyAudioPlayer: EmergencyAudioPlayer? = null
  var voiceMessageManager: VoiceMessageManager? = null
  private var _networkOverride: Boolean? = null

  fun setNetworkConnectedForTesting(connected: Boolean?) {
    _networkOverride = connected
  }

  fun isNetworkOnline(): Boolean {
    return _networkOverride ?: EmergencyCommunicationHub.isNetworkAvailable(null)
  }

  // Onboarding controls
  private val _isOnboardingRecordingVoice = MutableStateFlow(false)
  val isOnboardingRecordingVoice: StateFlow<Boolean> = _isOnboardingRecordingVoice.asStateFlow()

  private val _isOnboardingPlayingVoice = MutableStateFlow(false)
  val isOnboardingPlayingVoice: StateFlow<Boolean> = _isOnboardingPlayingVoice.asStateFlow()

  fun setOnboardingStep(step: Int) {
    if (step in 1..8) {
      _onboardingStep.value = step
    }
  }

  fun nextOnboardingStep() {
    if (_onboardingStep.value < 8) {
      _onboardingStep.value += 1
    } else {
      completeOnboarding()
    }
  }

  fun prevOnboardingStep() {
    if (_onboardingStep.value > 1) {
      _onboardingStep.value -= 1
    }
  }

  fun startPreRecordingVoice() {
    if (voiceMessageManager == null) {
      voiceMessageManager = VoiceMessageManager(appContext)
    }
    val started = voiceMessageManager?.startRecording() ?: false
    _isOnboardingRecordingVoice.value = started
  }

  fun stopPreRecordingVoice(customTranscription: String? = null) {
    _isOnboardingRecordingVoice.value = false
    val transcription = customTranscription?.ifBlank { null }
      ?: "EMERGENCY ALERT: I have had an accident and cannot speak. Please dispatch emergency help immediately to my current GPS coordinates!"
    val recorded = voiceMessageManager?.stopRecording(transcription)
    if (recorded != null && recorded.localFilePath != null) {
      _userProfile.update { current ->
        current.copy(
          preRecordedVoicePath = recorded.localFilePath,
          preRecordedVoiceTranscription = transcription,
          preRecordedVoiceDurationSeconds = recorded.audioDurationSeconds,
          hasPreRecordedVoiceNote = true
        )
      }
    }
  }

  fun playPreRecordedVoice() {
    if (emergencyAudioPlayer == null && appContext != null) {
      val player = EmergencyAudioPlayer(appContext)
      emergencyAudioPlayer = player
      com.example.util.EmergencyCommunicationHub.audioPlayer = player
    }
    val path = _userProfile.value.preRecordedVoicePath ?: ""
    val transcription = _userProfile.value.preRecordedVoiceTranscription.ifBlank {
      "Emergency SOS alert. Assistance needed immediately."
    }

    _isOnboardingPlayingVoice.value = true
    val player = emergencyAudioPlayer
    if (player != null) {
      player.playAudioFile(path, transcription) {
        _isOnboardingPlayingVoice.value = false
      }
    } else {
      _isOnboardingPlayingVoice.value = false
    }
  }

  fun stopPreRecordedVoicePlayback() {
    emergencyAudioPlayer?.stopAudioPlayback()
    _isOnboardingPlayingVoice.value = false
  }

  fun updatePreRecordedTranscription(text: String) {
    _userProfile.update {
      it.copy(
        preRecordedVoiceTranscription = text,
        hasPreRecordedVoiceNote = true
      )
    }
  }

  fun clearPreRecordedVoiceNote() {
    stopPreRecordedVoicePlayback()
    _userProfile.update {
      it.copy(
        preRecordedVoicePath = null,
        preRecordedVoiceDurationSeconds = 0,
        hasPreRecordedVoiceNote = false
      )
    }
  }

  fun completeOnboarding() {
    _isOnboardingCompleted.value = true
    userPreferencesManager?.let { prefs ->
      prefs.setOnboardingCompleted(true)
      prefs.setDetailsSaved(true)
      prefs.saveUserProfile(_userProfile.value)
    }
    _currentScreen.value = Screen.Dashboard.route
    _currentTab.value = BottomTab.HOME
    _currentMode.value = AppMode.PERSONAL
  }

  fun saveProfileAndComplete(name: String, phone: String, email: String) {
    updateProfile(name, phone, email)
    userPreferencesManager?.let { prefs ->
      prefs.setDetailsSaved(true)
      prefs.setOnboardingCompleted(true)
      prefs.saveUserProfile(_userProfile.value)
    }
    _isOnboardingCompleted.value = true
    _currentScreen.value = Screen.Dashboard.route
    _currentTab.value = BottomTab.HOME
    _currentMode.value = AppMode.PERSONAL
  }

  fun loginAndSaveDetails(name: String, email: String, phone: String) {
    val updatedName = name.ifBlank { "Safe Companion User" }
    _userProfile.update { current ->
      current.copy(
        name = updatedName,
        email = email.ifBlank { current.email },
        phone = phone.ifBlank { current.phone }
      )
    }
    _isOnboardingCompleted.value = true
    userPreferencesManager?.let { prefs ->
      prefs.setLoggedIn(true)
      prefs.setDetailsSaved(true)
      prefs.setOnboardingCompleted(true)
      prefs.saveUserProfile(_userProfile.value)
    }
    _currentScreen.value = Screen.Dashboard.route
    _currentTab.value = BottomTab.HOME
    _currentMode.value = AppMode.PERSONAL
  }

  fun logoutUser() {
    userPreferencesManager?.clearPreferences()
    _userProfile.value = UserProfile()
    _isOnboardingCompleted.value = false
    _onboardingStep.value = 1
    _currentScreen.value = Screen.Onboarding.route
  }

  fun restartOnboarding() {
    _onboardingStep.value = 1
    _isOnboardingCompleted.value = false
    _currentScreen.value = Screen.Onboarding.route
  }

  // Navigation
  fun navigateTo(route: String) {
    _currentScreen.value = route
    when (route) {
      Screen.Dashboard.route -> {
        _currentTab.value = BottomTab.HOME
        _currentMode.value = AppMode.PERSONAL
      }
      Screen.CommunityDashboard.route, Screen.CommunityReports.route -> {
        _currentTab.value = BottomTab.COMMUNITY
        _currentMode.value = AppMode.COMMUNITY
        syncManager?.fetchAndCacheRemoteHazardsAsync()
      }
      Screen.ActiveJourney.route, Screen.JourneyMap.route -> {
        _currentTab.value = BottomTab.JOURNEY
      }
      Screen.Settings.route, Screen.TrustedContacts.route -> {
        _currentTab.value = BottomTab.PROFILE
      }
    }
  }

  fun switchMode(mode: AppMode) {
    _currentMode.value = mode
    if (mode == AppMode.PERSONAL) {
      if (_currentTab.value == BottomTab.COMMUNITY) {
        _currentTab.value = BottomTab.HOME
      }
      _currentScreen.value = Screen.Dashboard.route
    } else {
      _currentTab.value = BottomTab.COMMUNITY
      _currentScreen.value = Screen.CommunityDashboard.route
    }
  }

  fun selectBottomTab(tab: BottomTab) {
    _currentTab.value = tab
    when (tab) {
      BottomTab.HOME -> {
        _currentMode.value = AppMode.PERSONAL
        _currentScreen.value = Screen.Dashboard.route
      }
      BottomTab.JOURNEY -> {
        if (_journeySession.value.isMonitoring) {
          _currentScreen.value = Screen.ActiveJourney.route
        } else {
          _currentScreen.value = Screen.JourneyMap.route
        }
      }
      BottomTab.COMMUNITY -> {
        _currentMode.value = AppMode.COMMUNITY
        _currentScreen.value = Screen.CommunityDashboard.route
      }
      BottomTab.PROFILE -> {
        _currentScreen.value = Screen.Settings.route
      }
    }
  }

  // User Profile
  fun updateProfile(name: String, phone: String, email: String = "") {
    _userProfile.update {
      it.copy(
        name = name.ifBlank { it.name },
        phone = phone.ifBlank { it.phone },
        email = email.ifBlank { it.email }
      )
    }
    userPreferencesManager?.let { prefs ->
      prefs.saveUserProfile(_userProfile.value)
      if (name.isNotBlank() || phone.isNotBlank()) {
        prefs.setDetailsSaved(true)
      }
    }
  }

  fun addOrUpdateSavedPlace(place: com.example.model.SavedPlace) {
    _userProfile.update { current ->
      val existingList = current.savedPlaces.toMutableList()
      val index = existingList.indexOfFirst { it.id == place.id || (place.isHome && it.isHome) || (place.isWork && it.isWork) }
      if (index >= 0) {
        existingList[index] = place
      } else {
        existingList.add(place)
      }
      current.copy(savedPlaces = existingList)
    }
    userPreferencesManager?.let { prefs ->
      prefs.saveSavedPlaces(_userProfile.value.savedPlaces)
      prefs.saveUserProfile(_userProfile.value)
    }
  }

  fun deleteSavedPlace(id: String) {
    _userProfile.update { current ->
      current.copy(savedPlaces = current.savedPlaces.filter { it.id != id })
    }
    userPreferencesManager?.let { prefs ->
      prefs.saveSavedPlaces(_userProfile.value.savedPlaces)
      prefs.saveUserProfile(_userProfile.value)
    }
  }

  fun updateHomeAddress(address: String, lat: Double, lng: Double) {
    val homePlace = com.example.model.SavedPlace(
      id = "place_home",
      title = "Home",
      address = address,
      latitude = lat,
      longitude = lng,
      category = com.example.model.PlaceCategory.HOME,
      isHome = true,
      isWork = false
    )
    addOrUpdateSavedPlace(homePlace)
  }

  fun updateWorkAddress(address: String, lat: Double, lng: Double) {
    val workPlace = com.example.model.SavedPlace(
      id = "place_work",
      title = "Work Office",
      address = address,
      latitude = lat,
      longitude = lng,
      category = com.example.model.PlaceCategory.WORK,
      isHome = false,
      isWork = true
    )
    addOrUpdateSavedPlace(workPlace)
  }

  // Location & Permissions
  private var activeLocationTracker: com.example.util.LocationTracker? = null
  private var appContext: android.content.Context? = null
  private var userPreferencesManager: com.example.util.UserPreferencesManager? = null
  private var journeyStartTimestamp: Long = 0L

  fun setAppContext(context: android.content.Context) {
    val appCtx = context.applicationContext
    this.appContext = appCtx
    if (userPreferencesManager == null) {
      userPreferencesManager = com.example.util.UserPreferencesManager(appCtx)
    }
    val prefs = userPreferencesManager ?: com.example.util.UserPreferencesManager(appCtx)

    // If onboarding was already completed or user logged in & saved details, route to Dashboard immediately
    if (prefs.isOnboardingCompleted()) {
      _isOnboardingCompleted.value = true
      if (_currentScreen.value == Screen.Onboarding.route) {
        _currentScreen.value = Screen.Dashboard.route
        _currentTab.value = BottomTab.HOME
        _currentMode.value = AppMode.PERSONAL
      }
    }

    // Restore saved profile details
    val savedProfile = prefs.getUserProfile()
    if (savedProfile.name.isNotBlank() || savedProfile.phone.isNotBlank() || savedProfile.email.isNotBlank() || savedProfile.savedPlaces.isNotEmpty()) {
      _userProfile.update { current ->
        current.copy(
          name = savedProfile.name.ifBlank { current.name },
          phone = savedProfile.phone.ifBlank { current.phone },
          email = savedProfile.email.ifBlank { current.email },
          emergencyNotes = savedProfile.emergencyNotes.ifBlank { current.emergencyNotes },
          preRecordedVoicePath = savedProfile.preRecordedVoicePath ?: current.preRecordedVoicePath,
          preRecordedVoiceTranscription = savedProfile.preRecordedVoiceTranscription.ifBlank { current.preRecordedVoiceTranscription },
          preRecordedVoiceDurationSeconds = if (savedProfile.preRecordedVoiceDurationSeconds > 0) savedProfile.preRecordedVoiceDurationSeconds else current.preRecordedVoiceDurationSeconds,
          hasPreRecordedVoiceNote = savedProfile.hasPreRecordedVoiceNote || current.hasPreRecordedVoiceNote,
          savedPlaces = if (savedProfile.savedPlaces.isNotEmpty()) savedProfile.savedPlaces else current.savedPlaces
        )
      }
    }

    if (voiceMessageManager == null) {
      voiceMessageManager = VoiceMessageManager(appCtx)
    }
    if (emergencyAudioPlayer == null) {
      val player = EmergencyAudioPlayer(appCtx)
      this.emergencyAudioPlayer = player
      com.example.util.EmergencyCommunicationHub.audioPlayer = player
    }
  }
  private var lastHandledLocationTimestamp: Long = 0L
  private var lastHandledLatitude: Double = 0.0
  private var lastHandledLongitude: Double = 0.0

  private var hazardsJob: kotlinx.coroutines.Job? = null
  private var journeysJob: kotlinx.coroutines.Job? = null

  fun setLocalRepository(repo: SafeCompanionLocalRepository) {
    this.repository = repo
    observeHazardsFromRepository(repo)
    observeJourneysFromRepository(repo)
  }

  private fun observeHazardsFromRepository(repo: SafeCompanionLocalRepository) {
    hazardsJob?.cancel()
    hazardsJob = viewModelScope.launch {
      try {
        repo.getAllHazards().collect { entities ->
          if (entities.isNotEmpty()) {
            val domainHazards = entities.map { it.toDomainModel() }
            val merged = (domainHazards + defaultHazardReports).distinctBy { it.id }
            _hazardReports.value = merged
          }
        }
      } catch (_: Throwable) {}
    }
  }

  private fun observeJourneysFromRepository(repo: SafeCompanionLocalRepository) {
    journeysJob?.cancel()
    journeysJob = viewModelScope.launch {
      try {
        repo.getAllJourneys().collect { entities ->
          if (entities.isNotEmpty()) {
            val dbRecords = entities.map { it.toRecord() }
            val merged = (dbRecords + defaultJourneyRecords).distinctBy { it.id }
            _journeyHistory.value = merged
          }
        }
      } catch (_: Throwable) {}
    }
  }

  fun getLocalRepository(): SafeCompanionLocalRepository? = repository

  fun setSyncManager(manager: SafeCompanionSyncManager) {
    this.syncManager = manager
    syncManager?.fetchAndCacheRemoteHazardsAsync()
  }

  fun getSyncManager(): SafeCompanionSyncManager? = syncManager

  private fun ensureRepository(): SafeCompanionLocalRepository? {
    if (repository == null && appContext != null) {
      val db = SafeCompanionDatabase.getInstance(appContext!!)
      val repo = SafeCompanionLocalRepository(db)
      repository = repo
      observeJourneysFromRepository(repo)
    }
    return repository
  }

  fun onLocationPermissionChanged(granted: Boolean, context: android.content.Context? = null) {
    _permissions.update { it.copy(location = granted) }
    _userLocation.update { it.copy(isPermissionGranted = granted) }
    if (context != null) {
      appContext = context.applicationContext
      val repo = ensureRepository()
      if (repo != null) {
        observeJourneysFromRepository(repo)
      }
    }
    if (granted && context != null) {
      refreshLocation(context)
      startContinuousLocationUpdates(context)
      if (_journeySession.value.isMonitoring) {
        try {
          com.example.service.JourneyTrackingService.start(context, _journeySession.value.destination)
        } catch (_: Throwable) {}
      }
    }
  }

  fun refreshLocation(context: android.content.Context) {
    val tracker = com.example.util.LocationTracker(context)
    if (!tracker.hasLocationPermission()) {
      _userLocation.update { it.copy(isPermissionGranted = false, isLocating = false) }
      return
    }

    _userLocation.update { it.copy(isLocating = true, isPermissionGranted = true) }

    tracker.getCurrentLocation(
      onLocationReceived = { location ->
        val now = System.currentTimeMillis()
        lastHandledLatitude = location.latitude
        lastHandledLongitude = location.longitude
        _userLocation.update {
          it.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            isPermissionGranted = true,
            isLocating = false,
            hasRealFix = true,
            lastUpdatedTime = now
          )
        }

        if (_journeySession.value.isMonitoring) {
          updateJourneyMetrics(location.latitude, location.longitude, location.speed)
        }

        viewModelScope.launch {
          try {
            val address = tracker.reverseGeocode(location.latitude, location.longitude)
            if (address.isNotBlank()) {
              _userLocation.update { it.copy(address = address) }
            }
          } catch (_: Exception) {}
        }
      },
      onError = {
        _userLocation.update { it.copy(isLocating = false) }
      }
    )
  }

  companion object {
    const val DESTINATION_PROXIMITY_THRESHOLD_METERS = 40f
    const val ROUTE_DEVIATION_THRESHOLD_METERS = 50f
    const val ROUTE_DEVIATION_CONFIRMATION_COUNT = 3
    const val ROUTE_DEVIATION_WARNING_COUNT = 1
    const val LONG_STOP_THRESHOLD_MINUTES = 5
    const val MIN_MOVEMENT_DISTANCE_METERS = 15f
    const val MAX_RELIABLE_GPS_ACCURACY_METERS = 65f
  }

  fun handleLocationUpdate(location: android.location.Location, context: android.content.Context? = null) {
    // Filter out fixes with extreme inaccuracies if we already have a fix
    if (location.hasAccuracy() && location.accuracy > 150f && _userLocation.value.hasRealFix) {
      return
    }

    // Immediately update core position & journey metrics synchronously so state reflects immediately
    val now = System.currentTimeMillis()
    lastHandledLocationTimestamp = now
    lastHandledLatitude = location.latitude
    lastHandledLongitude = location.longitude
    _userLocation.update {
      it.copy(
        latitude = location.latitude,
        longitude = location.longitude,
        accuracyMeters = location.accuracy,
        isPermissionGranted = true,
        isLocating = false,
        hasRealFix = true,
        lastUpdatedTime = now
      )
    }

    if (_journeySession.value.isMonitoring) {
      val session = _journeySession.value
      val currentJourneyId = session.journeyId.ifBlank {
        if (journeyStartTimestamp > 0L) "journey_${journeyStartTimestamp}" else ""
      }

      // Persist location breadcrumb asynchronously to Room for every valid location update during journey
      if (currentJourneyId.isNotBlank()) {
        val speedKmh = if (location.speed > 0f) (location.speed * 3.6).toDouble() else 0.0
        val locationAccuracy = location.accuracy
        val lat = location.latitude
        val lng = location.longitude
        viewModelScope.launch {
          try {
            val repo = ensureRepository()
            if (repo != null) {
              val breadcrumb = com.example.local.entity.JourneyLocationEntity(
                journeyId = currentJourneyId,
                latitude = lat,
                longitude = lng,
                accuracyMeters = locationAccuracy,
                speedKmh = speedKmh,
                timestamp = now,
                address = _userLocation.value.address,
                syncStatus = SyncStatus.PENDING
              )
              val generatedId = repo.saveLocation(breadcrumb)
              val savedLocation = breadcrumb.copy(id = generatedId)
              syncManager?.syncLocationAsync(savedLocation)
            }
          } catch (_: Throwable) {
            // Non-blocking persistence safety
          }
        }
      }

      updateJourneyMetrics(location.latitude, location.longitude, location.speed, location.accuracy, now)
    }

    // Perform reverse geocoding in background without delaying metrics updates
    if (context != null) {
      viewModelScope.launch {
        try {
          val tracker = com.example.util.LocationTracker(context)
          val address = tracker.reverseGeocode(location.latitude, location.longitude)
          _userLocation.update { it.copy(address = address) }
        } catch (_: Exception) {
          // Ignore geocoding errors
        }
      }
    }
  }

  fun startContinuousLocationUpdates(context: android.content.Context) {
    val tracker = com.example.util.LocationTracker(context)
    if (!tracker.hasLocationPermission()) return
    activeLocationTracker = tracker

    tracker.startLocationUpdates(intervalMs = 4000L) { location ->
      handleLocationUpdate(location, context)
    }
  }

  fun updateJourneyMetrics(
    currentLat: Double,
    currentLng: Double,
    speedMps: Float,
    accuracyMeters: Float = 0f,
    currentTimeMillis: Long = System.currentTimeMillis()
  ) {
    val session = _journeySession.value
    if (!session.isMonitoring) return

    _userLocation.update {
      it.copy(
        latitude = currentLat,
        longitude = currentLng,
        accuracyMeters = if (accuracyMeters > 0f) accuracyMeters else it.accuracyMeters,
        hasRealFix = true,
        lastUpdatedTime = currentTimeMillis
      )
    }

    val hasUncompletedStops = session.stops.isNotEmpty() && session.currentStopIndex < session.stops.size
    val currentTargetLat = if (hasUncompletedStops) session.stops[session.currentStopIndex].lat else session.destLat
    val currentTargetLng = if (hasUncompletedStops) session.stops[session.currentStopIndex].lng else session.destLng

    val remainingMetersToCurrentTarget = com.example.util.LocationTracker.calculateDistanceMeters(
      currentLat,
      currentLng,
      currentTargetLat,
      currentTargetLng
    )

    // If final destination reached, complete journey
    if (!hasUncompletedStops && remainingMetersToCurrentTarget <= DESTINATION_PROXIMITY_THRESHOLD_METERS) {
      completeJourney()
      return
    }

    // Proximity to intermediate stop auto-detects arrival
    val isArrivedAtStop = hasUncompletedStops && (remainingMetersToCurrentTarget <= DESTINATION_PROXIMITY_THRESHOLD_METERS)

    val currentLegRemainingKm = remainingMetersToCurrentTarget / 1000.0

    // Speed calculation
    val speedKmh = if (speedMps > 0.5f) {
      (speedMps * 3.6).toDouble()
    } else {
      5.0 // ~5 km/h standard urban walking speed baseline
    }

    val currentLegEtaMinutes = Math.round((currentLegRemainingKm / speedKmh) * 60).toInt().coerceAtLeast(1)

    // Calculate total remaining travel distance and remaining stay times across subsequent stops to final destination
    var totalRemainingTravelKm = currentLegRemainingKm
    var totalRemainingStays = 0

    if (hasUncompletedStops) {
      var prevLat = currentTargetLat
      var prevLng = currentTargetLng
      for (i in (session.currentStopIndex + 1) until session.stops.size) {
        val nextStop = session.stops[i]
        totalRemainingTravelKm += com.example.util.LocationTracker.calculateDistanceKm(prevLat, prevLng, nextStop.lat, nextStop.lng)
        prevLat = nextStop.lat
        prevLng = nextStop.lng
      }
      totalRemainingTravelKm += com.example.util.LocationTracker.calculateDistanceKm(prevLat, prevLng, session.destLat, session.destLng)

      // Uncompleted stops stay time (including current stop)
      totalRemainingStays = session.stops.drop(session.currentStopIndex).sumOf { it.stayDurationMinutes }
    }

    val totalTravelEta = Math.round((totalRemainingTravelKm / speedKmh) * 60).toInt().coerceAtLeast(1)
    val totalEtaMinutes = totalTravelEta + totalRemainingStays

    val initialDistKm = session.initialDistanceKm.coerceAtLeast(0.05)
    val rawProgress = ((initialDistKm - totalRemainingTravelKm) / initialDistKm).coerceIn(0.0, 1.0).toFloat()

    val remainingKm = totalRemainingTravelKm

    // --- Route Deviation Calculation ---
    val expectedRoutePoints = if (session.roadRoutePoints.isNotEmpty()) {
      session.roadRoutePoints
    } else {
      listOf(
        Pair(session.startLat, session.startLng),
        Pair(session.startLat, (session.startLng + session.destLng) / 2.0),
        Pair(session.destLat, (session.startLng + session.destLng) / 2.0),
        Pair(session.destLat, session.destLng)
      )
    }

    val distFromPathMeters = com.example.util.LocationTracker.calculateDistanceToPathMeters(
      currentLat,
      currentLng,
      expectedRoutePoints
    )

    val currentDeviationState = session.routeDeviationState
    val currentConsecutive = session.consecutiveDeviationReadings
    val isAcknowledged = session.isDeviationAcknowledgedByUser

    val newDeviationState: com.example.model.RouteDeviationState
    val newConsecutive: Int
    val newAcknowledged: Boolean
    val lastTimestamp: Long

    if (accuracyMeters > MAX_RELIABLE_GPS_ACCURACY_METERS && accuracyMeters > 0f) {
      // Poor GPS accuracy reading: do not escalate deviation confidence to prevent false alarms
      newDeviationState = currentDeviationState
      newConsecutive = currentConsecutive
      newAcknowledged = isAcknowledged
      lastTimestamp = session.lastDeviationTimestamp
    } else if (distFromPathMeters > ROUTE_DEVIATION_THRESHOLD_METERS) {
      newConsecutive = currentConsecutive + 1
      newDeviationState = when {
        newConsecutive >= ROUTE_DEVIATION_CONFIRMATION_COUNT -> com.example.model.RouteDeviationState.DEVIATED
        newConsecutive >= ROUTE_DEVIATION_WARNING_COUNT -> com.example.model.RouteDeviationState.WARNING
        else -> com.example.model.RouteDeviationState.NORMAL
      }
      // If escalating to DEVIATED from a lower state, re-prompt the user
      newAcknowledged = if (newDeviationState == com.example.model.RouteDeviationState.DEVIATED && currentDeviationState != com.example.model.RouteDeviationState.DEVIATED) {
        false
      } else {
        isAcknowledged
      }
      lastTimestamp = currentTimeMillis
    } else {
      // User is within acceptable path corridor -> NORMAL
      newConsecutive = 0
      newDeviationState = com.example.model.RouteDeviationState.NORMAL
      newAcknowledged = false
      lastTimestamp = session.lastDeviationTimestamp
    }

    // --- Long-Stop (Inactivity) Calculation ---
    val currentLongStopState = session.longStopState
    val currentStationarySince = session.stationarySinceTimestamp
    val currentAnchorLat = if (session.stationaryAnchorLat != 0.0) session.stationaryAnchorLat else currentLat
    val currentAnchorLng = if (session.stationaryAnchorLng != 0.0) session.stationaryAnchorLng else currentLng
    val currentDurationMin = session.stationaryDurationMinutes
    val currentLongStopAck = session.isLongStopAcknowledged

    val newLongStopState: com.example.model.LongStopState
    val newStationarySince: Long
    val newAnchorLat: Double
    val newAnchorLng: Double
    val newDurationMin: Int
    val newLongStopAck: Boolean

    if (accuracyMeters > MAX_RELIABLE_GPS_ACCURACY_METERS && accuracyMeters > 0f) {
      // Poor GPS accuracy reading: do not alter or escalate stationary timer
      newLongStopState = currentLongStopState
      newStationarySince = currentStationarySince
      newAnchorLat = currentAnchorLat
      newAnchorLng = currentAnchorLng
      newDurationMin = currentDurationMin
      newLongStopAck = currentLongStopAck
    } else {
      val distFromAnchor = com.example.util.LocationTracker.calculateDistanceMeters(
        currentLat,
        currentLng,
        currentAnchorLat,
        currentAnchorLng
      )

      // Meaningful movement: user shifted >= MIN_MOVEMENT_DISTANCE_METERS or has speed >= 1.2 m/s with displacement
      val isMeaningfulMovement = distFromAnchor >= MIN_MOVEMENT_DISTANCE_METERS || (speedMps >= 1.2f && distFromAnchor >= 8f)

      if (isMeaningfulMovement) {
        // Movement resumed or continuing -> reset stationary tracker
        newLongStopState = com.example.model.LongStopState.NORMAL
        newStationarySince = 0L
        newAnchorLat = currentLat
        newAnchorLng = currentLng
        newDurationMin = 0
        newLongStopAck = false
      } else {
        // Stationary within anchor radius
        val stationaryStart = if (currentStationarySince == 0L) currentTimeMillis else currentStationarySince
        val elapsedMs = (currentTimeMillis - stationaryStart).coerceAtLeast(0L)
        val elapsedMin = (elapsedMs / (60 * 1000L)).toInt()

        newStationarySince = stationaryStart
        newAnchorLat = currentAnchorLat
        newAnchorLng = currentAnchorLng
        newDurationMin = elapsedMin

        if (elapsedMin >= LONG_STOP_THRESHOLD_MINUTES) {
          newLongStopState = com.example.model.LongStopState.LONG_STOP_WARNING
          newLongStopAck = if (currentLongStopState != com.example.model.LongStopState.LONG_STOP_WARNING) false else currentLongStopAck
        } else {
          newLongStopState = com.example.model.LongStopState.STATIONARY
          newLongStopAck = currentLongStopAck
        }
      }
    }

    _journeySession.update {
      it.copy(
        remainingKm = Math.round(remainingKm * 10.0) / 10.0,
        etaMinutes = totalEtaMinutes,
        progress = rawProgress,
        isDestinationReached = false,
        currentSpeedKmh = speedKmh,
        currentLegDistanceKm = Math.round(currentLegRemainingKm * 10.0) / 10.0,
        currentLegEtaMinutes = currentLegEtaMinutes,
        isAtStop = it.isAtStop || isArrivedAtStop,
        routeDeviationState = newDeviationState,
        deviationDistanceMeters = distFromPathMeters,
        consecutiveDeviationReadings = newConsecutive,
        lastDeviationTimestamp = lastTimestamp,
        isDeviationAcknowledgedByUser = newAcknowledged,
        longStopState = newLongStopState,
        stationarySinceTimestamp = newStationarySince,
        stationaryDurationMinutes = newDurationMin,
        stationaryAnchorLat = newAnchorLat,
        stationaryAnchorLng = newAnchorLng,
        isLongStopAcknowledged = newLongStopAck
      )
    }
  }

  fun startNextDestination() {
    val session = _journeySession.value
    if (session.stops.isEmpty() || session.currentStopIndex >= session.stops.size) {
      return
    }

    val currentStopIdx = session.currentStopIndex
    val currentStop = session.stops[currentStopIdx]
    val updatedStops = session.stops.mapIndexed { index, stop ->
      if (index == currentStopIdx) {
        stop.copy(isCompleted = true, isCurrentTarget = false, departedAtTimestamp = System.currentTimeMillis())
      } else if (index == currentStopIdx + 1) {
        stop.copy(isCurrentTarget = true)
      } else {
        stop
      }
    }

    val nextStopIdx = currentStopIdx + 1
    val isHeadingToFinal = nextStopIdx >= session.stops.size

    val userLoc = _userLocation.value
    val startFromLat = if (userLoc.hasRealFix) userLoc.latitude else currentStop.lat
    val startFromLng = if (userLoc.hasRealFix) userLoc.longitude else currentStop.lng

    val targetLat = if (isHeadingToFinal) session.destLat else updatedStops[nextStopIdx].lat
    val targetLng = if (isHeadingToFinal) session.destLng else updatedStops[nextStopIdx].lng

    val legDistKm = com.example.util.LocationTracker.calculateDistanceKm(startFromLat, startFromLng, targetLat, targetLng)
    val speedKmh = if (session.currentSpeedKmh > 0.5) session.currentSpeedKmh else 5.0
    val legEta = Math.round((legDistKm / speedKmh) * 60).toInt().coerceAtLeast(1)

    var remainingTravelKm = legDistKm
    if (!isHeadingToFinal) {
      var prevLat = targetLat
      var prevLng = targetLng
      for (i in (nextStopIdx + 1) until updatedStops.size) {
        val nextStop = updatedStops[i]
        remainingTravelKm += com.example.util.LocationTracker.calculateDistanceKm(prevLat, prevLng, nextStop.lat, nextStop.lng)
        prevLat = nextStop.lat
        prevLng = nextStop.lng
      }
      remainingTravelKm += com.example.util.LocationTracker.calculateDistanceKm(prevLat, prevLng, session.destLat, session.destLng)
    }

    val remainingStayMinutes = if (isHeadingToFinal) 0 else updatedStops.drop(nextStopIdx).sumOf { it.stayDurationMinutes }
    val remainingTravelEta = Math.round((remainingTravelKm / speedKmh) * 60).toInt().coerceAtLeast(1)
    val newTotalEta = remainingTravelEta + remainingStayMinutes

    _journeySession.update {
      it.copy(
        stops = updatedStops,
        currentStopIndex = nextStopIdx,
        isAtStop = false,
        currentLegDistanceKm = Math.round(legDistKm * 10.0) / 10.0,
        currentLegEtaMinutes = legEta,
        remainingKm = Math.round(remainingTravelKm * 10.0) / 10.0,
        etaMinutes = newTotalEta,
        routeDeviationState = com.example.model.RouteDeviationState.NORMAL,
        deviationDistanceMeters = 0f,
        consecutiveDeviationReadings = 0,
        isDeviationAcknowledgedByUser = false,
        longStopState = com.example.model.LongStopState.NORMAL,
        stationarySinceTimestamp = 0L,
        stationaryDurationMinutes = 0,
        isLongStopAcknowledged = false
      )
    }

    // Fetch road route for new leg asynchronously
    viewModelScope.launch {
      try {
        val roadRoute = com.example.util.RoutingService.fetchRoadRoute(
          startLat = startFromLat,
          startLng = startFromLng,
          destLat = targetLat,
          destLng = targetLng
        )
        _journeySession.update {
          if (!it.isMonitoring) return@update it
          it.copy(roadRoutePoints = roadRoute.points)
        }
      } catch (_: Exception) {}
    }
  }

  fun arriveAtCurrentStop() {
    _journeySession.update { session ->
      if (session.stops.isNotEmpty() && session.currentStopIndex < session.stops.size) {
        val stop = session.stops[session.currentStopIndex]
        val updatedStops = session.stops.mapIndexed { idx, s ->
          if (idx == session.currentStopIndex) s.copy(arrivedAtTimestamp = System.currentTimeMillis()) else s
        }
        session.copy(
          isAtStop = true,
          stops = updatedStops,
          currentStopStayRemainingMinutes = stop.stayDurationMinutes,
          longStopState = com.example.model.LongStopState.NORMAL,
          stationaryDurationMinutes = 0
        )
      } else {
        session
      }
    }
  }

  fun calculateStopsCoordinates(
    sLat: Double,
    sLng: Double,
    dLat: Double,
    dLng: Double,
    rawStops: List<JourneyStop>,
    context: android.content.Context? = null
  ): List<JourneyStop> {
    val n = rawStops.size
    if (n == 0) return emptyList()

    return rawStops.mapIndexed { idx, stop ->
      if (stop.lat != 0.0 && stop.lng != 0.0) {
        stop.copy(
          isCompleted = false,
          isCurrentTarget = (idx == 0)
        )
      } else {
        val ratio = (idx + 1).toDouble() / (n + 1)
        val lateralOffset = if (idx % 2 == 0) 0.0035 else -0.0035
        val lat = sLat + (dLat - sLat) * ratio + lateralOffset
        val lng = sLng + (dLng - sLng) * ratio + (lateralOffset * 0.8)
        stop.copy(
          lat = lat,
          lng = lng,
          isCompleted = false,
          isCurrentTarget = (idx == 0)
        )
      }
    }
  }

  fun acknowledgeDeviation() {
    _journeySession.update {
      it.copy(isDeviationAcknowledgedByUser = true)
    }
  }

  fun acknowledgeLongStop() {
    _journeySession.update {
      it.copy(isLongStopAcknowledged = true)
    }
  }

  fun togglePermission(type: String, context: android.content.Context? = null) {
    _permissions.update { current ->
      when (type) {
        "location" -> {
          val next = !current.location
          _userLocation.update { it.copy(isPermissionGranted = next) }
          if (next && context != null) {
            refreshLocation(context)
            startContinuousLocationUpdates(context)
          }
          current.copy(location = next)
        }
        "notifications" -> current.copy(notifications = !current.notifications)
        "background" -> current.copy(backgroundLocation = !current.backgroundLocation)
        "sms" -> current.copy(smsFallback = !current.smsFallback)
        else -> current
      }
    }
  }

  fun setPermissionGranted(type: String, granted: Boolean, context: android.content.Context? = null) {
    _permissions.update { current ->
      when (type) {
        "location" -> {
          _userLocation.update { it.copy(isPermissionGranted = granted) }
          if (granted && context != null) {
            refreshLocation(context)
            startContinuousLocationUpdates(context)
          }
          current.copy(location = granted)
        }
        "notifications" -> current.copy(notifications = granted)
        "background" -> current.copy(backgroundLocation = granted)
        "sms" -> current.copy(smsFallback = granted)
        else -> current
      }
    }
  }

  // Contacts
  fun addTrustedContact(name: String, relationship: String, phone: String, isPrimary: Boolean = false) {
    val newId = UUID.randomUUID().toString()
    _trustedContacts.update { list ->
      val effectiveIsPrimary = isPrimary || list.isEmpty()
      val contact = TrustedContact(
        id = newId,
        name = name,
        relationship = relationship,
        phone = phone,
        isPrimary = effectiveIsPrimary
      )
      val updated = if (effectiveIsPrimary) {
        list.map { it.copy(isPrimary = false) } + contact
      } else {
        list + contact
      }
      updated
    }
    _selectedSharingContacts.update { it + newId }
  }

  fun removeTrustedContact(id: String) {
    _trustedContacts.update { it.filter { contact -> contact.id != id } }
    _selectedSharingContacts.update { it - id }
  }

  fun toggleContactSelectionForJourney(id: String) {
    _selectedSharingContacts.update { current ->
      if (current.contains(id)) {
        current - id
      } else {
        current + id
      }
    }
  }

  fun selectRouteOption(option: ScoredRouteOption) {
    _selectedRouteOption.value = option
  }

  fun calculateRouteRecommendations(
    startLoc: String = "",
    dest: String = "",
    startLat: Double? = null,
    startLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null
  ) {
    val currentLoc = _userLocation.value
    val sLat = startLat ?: (if (currentLoc.hasRealFix) currentLoc.latitude else 37.7749)
    val sLng = startLng ?: (if (currentLoc.hasRealFix) currentLoc.longitude else -122.4194)

    val defaultDestLat = destLat ?: (sLat + 0.0120)
    val defaultDestLng = destLng ?: (sLng + 0.0145)

    viewModelScope.launch {
      _isLoadingRoutes.value = true
      try {
        var targetLat = defaultDestLat
        var targetLng = defaultDestLng

        val ctx = appContext
        if (dest.isNotBlank() && dest != "Home" && dest != "Work" && destLat == null && ctx != null) {
          try {
            val geocoded = com.example.util.LocationTracker(ctx).geocodeAddress(dest)
            if (geocoded != null) {
              targetLat = geocoded.first
              targetLng = geocoded.second
            }
          } catch (_: Exception) {}
        }

        val candidates = com.example.util.RoutingService.fetchRouteCandidates(
          startLat = sLat,
          startLng = sLng,
          destLat = targetLat,
          destLng = targetLng
        )

        val activeHazards = _hazardReports.value
        val scoredCandidates = candidates.map { candidate ->
          com.example.util.RouteSafetyScorer.scoreCandidateRoute(
            id = candidate.id,
            title = candidate.title,
            subtitle = candidate.subtitle,
            points = candidate.routeResult.points,
            distanceMeters = candidate.routeResult.distanceMeters,
            durationSeconds = candidate.routeResult.durationSeconds,
            hazards = activeHazards
          )
        }

        val recommendedOptions = com.example.util.RouteSafetyScorer.evaluateAndRecommendRoutes(scoredCandidates)
        _routeOptions.value = recommendedOptions

        val recommended = recommendedOptions.firstOrNull { it.isRecommended }
          ?: recommendedOptions.firstOrNull()
        _selectedRouteOption.value = recommended
      } catch (_: Exception) {
        // Safe fallback
      } finally {
        _isLoadingRoutes.value = false
      }
    }
  }

  // Journey
  fun startJourney(
    startLoc: String = "",
    dest: String = "",
    note: String = "",
    context: android.content.Context? = null,
    startLat: Double? = null,
    startLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null,
    contactIds: Set<String>? = null,
    selectedRoute: ScoredRouteOption? = null,
    stops: List<JourneyStop> = emptyList()
  ) {
    val currentLoc = _userLocation.value
    val sLat = startLat ?: (if (currentLoc.hasRealFix) currentLoc.latitude else 37.7749)
    val sLng = startLng ?: (if (currentLoc.hasRealFix) currentLoc.longitude else -122.4194)

    // Default destination offset (~1.8km) if not specified
    val defaultDestLat = destLat ?: (sLat + 0.0120)
    val defaultDestLng = destLng ?: (sLng + 0.0145)

    val initialDist = com.example.util.LocationTracker.calculateDistanceKm(
      sLat,
      sLng,
      defaultDestLat,
      defaultDestLng
    )
    val initialEta = Math.round((initialDist / 5.0) * 60).toInt().coerceAtLeast(1)

    val chosenRoute = selectedRoute ?: _selectedRouteOption.value
    val precomputedPoints = chosenRoute?.points ?: emptyList()
    val chosenDistKm = chosenRoute?.distanceKm ?: (Math.round(initialDist * 10.0) / 10.0)
    val chosenEta = chosenRoute?.etaMinutes ?: initialEta
    val routeSafetyScore = chosenRoute?.safetyScore ?: 100
    val routeNearbyHazardsCount = chosenRoute?.hazardCountNearby ?: 0
    val routeRecommendationNote = chosenRoute?.recommendationReason ?: ""

    val contactsForJourney = contactIds ?: _selectedSharingContacts.value

    val resolvedStops = calculateStopsCoordinates(
      sLat = sLat,
      sLng = sLng,
      dLat = defaultDestLat,
      dLng = defaultDestLng,
      rawStops = stops,
      context = context
    )

    val totalStays = resolvedStops.sumOf { it.stayDurationMinutes }

    var totalTravelDist = 0.0
    var pLat = sLat
    var pLng = sLng
    for (stop in resolvedStops) {
      totalTravelDist += com.example.util.LocationTracker.calculateDistanceKm(pLat, pLng, stop.lat, stop.lng)
      pLat = stop.lat
      pLng = stop.lng
    }
    totalTravelDist += com.example.util.LocationTracker.calculateDistanceKm(pLat, pLng, defaultDestLat, defaultDestLng)

    val multiStopDistKm = Math.round(totalTravelDist * 10.0) / 10.0
    val travelEtaMinutes = Math.round((totalTravelDist / 5.0) * 60).toInt().coerceAtLeast(1)
    val multiStopTotalEta = travelEtaMinutes + totalStays

    val leg1Dist = if (resolvedStops.isNotEmpty()) {
      com.example.util.LocationTracker.calculateDistanceKm(sLat, sLng, resolvedStops[0].lat, resolvedStops[0].lng)
    } else initialDist
    val leg1Eta = Math.round((leg1Dist / 5.0) * 60).toInt().coerceAtLeast(1)

    val effectiveInitialDistKm = if (resolvedStops.isNotEmpty()) multiStopDistKm else chosenDistKm
    val effectiveEtaMinutes = if (resolvedStops.isNotEmpty()) multiStopTotalEta else chosenEta

    journeyStartTimestamp = System.currentTimeMillis()
    val newJourneyId = "journey_${journeyStartTimestamp}"
    activeLocationTracker?.stopLocationUpdates()
    _emergencyState.value = EmergencyAlertState(isTriggered = false)

    val newSession = JourneySession(
      journeyId = newJourneyId,
      startLocation = startLoc.ifBlank { if (currentLoc.address.isNotBlank()) currentLoc.address else "Current Location" },
      destination = dest.ifBlank { "Home" },
      note = note,
      startLat = sLat,
      startLng = sLng,
      destLat = defaultDestLat,
      destLng = defaultDestLng,
      initialDistanceKm = effectiveInitialDistKm,
      remainingKm = effectiveInitialDistKm,
      totalKm = effectiveInitialDistKm,
      etaMinutes = effectiveEtaMinutes,
      durationMinutes = effectiveEtaMinutes,
      progress = 0.0f,
      isDestinationValid = true,
      isDestinationReached = false,
      isMonitoring = true,
      routeDeviationState = com.example.model.RouteDeviationState.NORMAL,
      deviationDistanceMeters = 0f,
      consecutiveDeviationReadings = 0,
      lastDeviationTimestamp = 0L,
      isDeviationAcknowledgedByUser = false,
      longStopState = com.example.model.LongStopState.NORMAL,
      stationarySinceTimestamp = 0L,
      stationaryDurationMinutes = 0,
      stationaryAnchorLat = sLat,
      stationaryAnchorLng = sLng,
      isLongStopAcknowledged = false,
      roadRoutePoints = precomputedPoints,
      safetyScore = routeSafetyScore,
      nearbyHazardsCount = routeNearbyHazardsCount,
      routeRecommendationNote = routeRecommendationNote,
      warningsCount = 0,
      emergencyAlertsCount = 0,
      selectedContactIds = contactsForJourney,
      stops = resolvedStops,
      currentStopIndex = 0,
      currentLegDistanceKm = Math.round(leg1Dist * 10.0) / 10.0,
      currentLegEtaMinutes = leg1Eta,
      totalStaysMinutes = totalStays,
      isAtStop = false,
      currentStopStayRemainingMinutes = if (resolvedStops.isNotEmpty()) resolvedStops[0].stayDurationMinutes else 0
    )
    _journeySession.value = newSession
    _currentScreen.value = Screen.ActiveJourney.route
    _currentTab.value = BottomTab.JOURNEY

    if (context != null) {
      appContext = context.applicationContext
    }

    // Persist new journey asynchronously to Room database
    viewModelScope.launch {
      try {
        val repo = ensureRepository()
        if (repo != null) {
          val entity = newSession.toEntity(
            journeyId = newJourneyId,
            userId = _userProfile.value.name.ifBlank { "user_default" },
            startTimestamp = journeyStartTimestamp,
            syncStatus = SyncStatus.PENDING
          )
          repo.saveJourney(entity)
          syncManager?.syncJourneyAsync(newJourneyId)
        }
      } catch (_: Throwable) {
        // Non-blocking persistence failure safety
      }
    }

    // Calculate/refine initial road route asynchronously if not already precomputed
    if (precomputedPoints.isEmpty()) {
      viewModelScope.launch {
        val targetFirstLat = if (resolvedStops.isNotEmpty()) resolvedStops[0].lat else defaultDestLat
        val targetFirstLng = if (resolvedStops.isNotEmpty()) resolvedStops[0].lng else defaultDestLng
        val roadRoute = com.example.util.RoutingService.fetchRoadRoute(
          startLat = sLat,
          startLng = sLng,
          destLat = targetFirstLat,
          destLng = targetFirstLng
        )
        val roadDistKm = roadRoute.distanceMeters / 1000.0
        val roadEta = Math.round(roadRoute.durationSeconds / 60.0).toInt().coerceAtLeast(1)

        val scored = com.example.util.RouteSafetyScorer.scoreCandidateRoute(
          id = "active_route",
          title = "Active Route",
          subtitle = "Active road corridor",
          points = roadRoute.points,
          distanceMeters = roadRoute.distanceMeters,
          durationSeconds = roadRoute.durationSeconds,
          hazards = _hazardReports.value
        )

        _journeySession.update {
          if (!it.isMonitoring) return@update it
          it.copy(
            roadRoutePoints = roadRoute.points,
            initialDistanceKm = Math.round(roadDistKm * 10.0) / 10.0,
            remainingKm = Math.round(roadDistKm * 10.0) / 10.0,
            totalKm = Math.round(roadDistKm * 10.0) / 10.0,
            etaMinutes = roadEta,
            durationMinutes = roadEta,
            safetyScore = scored.safetyScore,
            nearbyHazardsCount = scored.hazardCountNearby,
            routeRecommendationNote = scored.recommendationReason
          )
        }
      }
    }

    if (context != null) {
      appContext = context.applicationContext
      startContinuousLocationUpdates(context)
      try {
        com.example.service.JourneyTrackingService.start(context, dest)
      } catch (e: Exception) {
        android.util.Log.e("SafeCompanion", "Failed to start JourneyTrackingService: ${e.message}")
      }
      if (dest.isNotBlank() && dest != "Home" && dest != "Work" && destLat == null) {
        viewModelScope.launch {
          val tracker = com.example.util.LocationTracker(context)
          val geocoded = tracker.geocodeAddress(dest)
          if (geocoded != null) {
            val (gLat, gLng) = geocoded
            val roadRoute = com.example.util.RoutingService.fetchRoadRoute(
              startLat = sLat,
              startLng = sLng,
              destLat = gLat,
              destLng = gLng
            )
            val newDistKm = roadRoute.distanceMeters / 1000.0
            val newEta = Math.round(roadRoute.durationSeconds / 60.0).toInt().coerceAtLeast(1)
            _journeySession.update {
              if (!it.isMonitoring) return@update it
              it.copy(
                destLat = gLat,
                destLng = gLng,
                roadRoutePoints = roadRoute.points,
                initialDistanceKm = Math.round(newDistKm * 10.0) / 10.0,
                remainingKm = Math.round(newDistKm * 10.0) / 10.0,
                totalKm = Math.round(newDistKm * 10.0) / 10.0,
                etaMinutes = newEta,
                durationMinutes = newEta
              )
            }
          }
        }
      }
    }
  }

  fun completeJourney() {
    val currentSession = _journeySession.value
    val elapsedMinutes = if (journeyStartTimestamp > 0L) {
      val mins = ((System.currentTimeMillis() - journeyStartTimestamp) / (60 * 1000L)).toInt()
      if (mins > 0) mins else currentSession.durationMinutes.coerceAtLeast(1)
    } else {
      currentSession.durationMinutes.coerceAtLeast(1)
    }
    val finalTotalKm = if (currentSession.initialDistanceKm > 0.0) {
      currentSession.initialDistanceKm
    } else {
      currentSession.totalKm
    }

    _emergencyState.value = EmergencyAlertState(isTriggered = false)
    val finalCalculatedTotalKm = Math.round(finalTotalKm * 10.0) / 10.0
    val finalDuration = elapsedMinutes
    val completedJourneyId = currentSession.journeyId.ifBlank {
      if (journeyStartTimestamp > 0L) "journey_${journeyStartTimestamp}" else ""
    }

    _journeySession.update {
      it.copy(
        isMonitoring = false,
        isDestinationReached = true,
        remainingKm = 0.0,
        etaMinutes = 0,
        progress = 1.0f,
        totalKm = finalCalculatedTotalKm,
        durationMinutes = finalDuration,
        routeDeviationState = com.example.model.RouteDeviationState.NORMAL,
        deviationDistanceMeters = 0f,
        consecutiveDeviationReadings = 0,
        lastDeviationTimestamp = 0L,
        isDeviationAcknowledgedByUser = false,
        longStopState = com.example.model.LongStopState.NORMAL,
        stationarySinceTimestamp = 0L,
        stationaryDurationMinutes = 0,
        stationaryAnchorLat = 0.0,
        stationaryAnchorLng = 0.0,
        isLongStopAcknowledged = false
      )
    }
    activeLocationTracker?.stopLocationUpdates()
    appContext?.let { com.example.service.JourneyTrackingService.stop(it) }
    _currentScreen.value = Screen.JourneyCompleted.route

    val arrivalNotif = SafetyNotification(
      id = "arrival_${System.currentTimeMillis()}",
      title = "🎉 Safe Arrival at ${currentSession.destination}",
      message = "You have arrived safely at your destination. Monitoring session completed.",
      timestamp = System.currentTimeMillis(),
      type = NotificationType.SAFE_ARRIVAL,
      isRead = false,
      targetScreenRoute = Screen.JourneyCompleted.route,
      destination = currentSession.destination
    )
    addNotification(arrivalNotif)
    appContext?.let { ctx ->
      SafetyNotificationManager.showSafeArrivalNotification(
        context = ctx,
        destination = currentSession.destination,
        companionCount = currentSession.selectedContactIds.size
      )
    }

    // Persist journey completion to Room database asynchronously
    val completionTimestamp = System.currentTimeMillis()
    if (completedJourneyId.isNotBlank()) {
      viewModelScope.launch {
        try {
          val repo = ensureRepository()
          if (repo != null) {
            repo.completeJourney(
              journeyId = completedJourneyId,
              endTime = completionTimestamp,
              totalKm = finalCalculatedTotalKm,
              durationMinutes = finalDuration
            )
            syncManager?.syncJourneyAsync(completedJourneyId)
          }
        } catch (_: Throwable) {
          // Non-blocking persistence safety
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    activeLocationTracker?.stopLocationUpdates()
    appContext?.let { com.example.service.JourneyTrackingService.stop(it) }
  }

  // SOS & Emergency
  fun openSosConfirmation() {
    _currentScreen.value = Screen.SosConfirmation.route
  }

  fun cancelSosConfirmation() {
    if (_journeySession.value.isMonitoring) {
      _currentScreen.value = Screen.ActiveJourney.route
    } else {
      _currentScreen.value = Screen.Dashboard.route
    }
  }

  fun triggerEmergency() {
    val loc = _userLocation.value
    val session = _journeySession.value
    val lat = if (loc.hasRealFix) loc.latitude else session.startLat
    val lng = if (loc.hasRealFix) loc.longitude else session.startLng
    val realAccuracy = loc.accuracyMeters.toInt().coerceAtLeast(1)
    val hasValidCoords = !(lat == 0.0 && lng == 0.0 && !loc.hasRealFix)

    // Retrieve trusted contacts stored in current JourneySession
    val activeContactIds = if (session.isMonitoring) session.selectedContactIds else _selectedSharingContacts.value
    val matchingContacts = _trustedContacts.value.filter { activeContactIds.contains(it.id) }
    val selectedContactNames = matchingContacts.map { it.name }

    val isOnline = _networkOverride ?: EmergencyCommunicationHub.isNetworkAvailable(appContext)
    val commMethod = if (isOnline) CommunicationMethod.ONLINE_FCM else CommunicationMethod.SMS_FALLBACK
    val emergencyId = UUID.randomUUID().toString()
    activeEmergencyId = emergencyId
    val emergencyTimestamp = System.currentTimeMillis()

    // Play local emergency confirmation sound on sender's phone for immediate audible confirmation
    emergencyAudioPlayer?.playSosConfirmationBeep()

    // Determine voice note to send:
    // If user made a fresh recording during this incident, prioritize it.
    // If user could not record (e.g., sudden accident, shock, incapacitation, or timeout),
    // automatically attach the pre-recorded emergency voice note!
    val freshVoice = _emergencyState.value.voiceMessage
    val hasFreshRecording = (freshVoice.state == VoiceRecordingState.RECORDED || freshVoice.state == VoiceRecordingState.UPLOADED) &&
        (!freshVoice.localFilePath.isNullOrBlank() || !freshVoice.audioUrl.isNullOrBlank())

    val effectiveVoiceMessage: VoiceMessageData = if (hasFreshRecording) {
      freshVoice
    } else if (_userProfile.value.hasPreRecordedVoiceNote && !_userProfile.value.preRecordedVoicePath.isNullOrBlank()) {
      VoiceMessageData(
        localFilePath = _userProfile.value.preRecordedVoicePath,
        audioDurationSeconds = _userProfile.value.preRecordedVoiceDurationSeconds,
        transcription = _userProfile.value.preRecordedVoiceTranscription,
        state = VoiceRecordingState.RECORDED,
        isOfflineRecorded = true,
        isQueuedForSync = true
      )
    } else if (_userProfile.value.hasPreRecordedVoiceNote && _userProfile.value.preRecordedVoiceTranscription.isNotBlank()) {
      VoiceMessageData(
        localFilePath = null,
        audioDurationSeconds = 0,
        transcription = _userProfile.value.preRecordedVoiceTranscription,
        state = VoiceRecordingState.RECORDED,
        isOfflineRecorded = true,
        isQueuedForSync = false
      )
    } else {
      freshVoice
    }

    val payload = EmergencyPayload(
      emergencyId = emergencyId,
      status = "ACTIVE",
      userId = _userProfile.value.name.ifBlank { "Safe Companion User" },
      userName = _userProfile.value.name.ifBlank { "Safe Companion User" },
      userPhone = _userProfile.value.phone,
      journeyId = if (session.isMonitoring) session.destination else null,
      journeyDestination = if (session.isMonitoring) session.destination else null,
      latitude = lat,
      longitude = lng,
      accuracyMeters = realAccuracy,
      timestamp = emergencyTimestamp,
      message = "EMERGENCY SOS! I need immediate assistance.",
      locationAddress = loc.address,
      voiceMessageUrl = effectiveVoiceMessage.audioUrl,
      voiceTranscription = effectiveVoiceMessage.transcription,
      localVoiceFilePath = effectiveVoiceMessage.localFilePath,
      voiceDurationSeconds = effectiveVoiceMessage.audioDurationSeconds,
      isOfflineFallback = !isOnline,
      targetContactIds = activeContactIds.toList()
    )

    _emergencyState.value = EmergencyAlertState(
      isTriggered = true,
      emergencyId = emergencyId,
      stepIndex = 3,
      accuracyMeters = realAccuracy,
      isLocationAvailable = hasValidCoords,
      internetAvailable = isOnline,
      communicationMethod = commMethod,
      communicationFailed = false,
      notifiedContacts = selectedContactNames,
      contactStatuses = matchingContacts.map { contact ->
        ContactNotificationStatus(
          contactId = contact.id,
          contactName = contact.name,
          phone = contact.phone,
          method = commMethod,
          status = DeliveryStatus.PENDING,
          timestamp = emergencyTimestamp
        )
      },
      latitude = lat,
      longitude = lng,
      address = loc.address,
      timestamp = emergencyTimestamp,
      voiceMessage = effectiveVoiceMessage,
      isResolved = false
    )
    _currentScreen.value = Screen.EmergencyStatus.route

    val emergencyNotif = SafetyNotification(
      id = "sos_${System.currentTimeMillis()}",
      title = "🚨 EMERGENCY SOS ACTIVATED",
      message = "Emergency alert dispatched to ${if (selectedContactNames.isNotEmpty()) selectedContactNames.joinToString(", ") else "emergency contacts"}.",
      timestamp = emergencyTimestamp,
      type = NotificationType.EMERGENCY,
      isRead = false,
      targetScreenRoute = Screen.EmergencyStatus.route
    )
    addNotification(emergencyNotif)

    // If journey is active, preserve journey state and increment emergency alert count
    if (session.isMonitoring) {
      _journeySession.update { it.copy(emergencyAlertsCount = it.emergencyAlertsCount + 1) }
    }

    // Persist emergency entity to Room database asynchronously
    viewModelScope.launch {
      try {
        val repo = ensureRepository()
        if (repo != null) {
          val emergencyEntity = payload.toEntity(syncStatus = SyncStatus.PENDING)
          repo.saveEmergency(emergencyEntity)
          syncManager?.syncEmergencyAsync(emergencyId)
        }
      } catch (_: Throwable) {
        // Non-blocking persistence safety
      }
    }

    // Launch dispatch asynchronously without blocking SOS activation
    dispatchEmergencyAlert(payload, matchingContacts, commMethod)
  }

  fun retryEmergencyDispatch() {
    val state = _emergencyState.value
    if (!state.isTriggered) return
    val session = _journeySession.value
    val activeContactIds = if (session.isMonitoring) session.selectedContactIds else _selectedSharingContacts.value
    val matchingContacts = _trustedContacts.value.filter { activeContactIds.contains(it.id) }
    val isOnline = _networkOverride ?: EmergencyCommunicationHub.isNetworkAvailable(appContext)
    val commMethod = if (isOnline) CommunicationMethod.ONLINE_FCM else CommunicationMethod.SMS_FALLBACK

    val payload = EmergencyPayload(
      emergencyId = state.emergencyId.ifBlank { UUID.randomUUID().toString() },
      status = "ACTIVE",
      userName = _userProfile.value.name.ifBlank { "Safe Companion User" },
      latitude = state.latitude,
      longitude = state.longitude,
      accuracyMeters = state.accuracyMeters,
      timestamp = System.currentTimeMillis(),
      message = "EMERGENCY SOS! I need immediate assistance.",
      voiceMessageUrl = state.voiceMessage.audioUrl,
      voiceTranscription = state.voiceMessage.transcription,
      localVoiceFilePath = state.voiceMessage.localFilePath,
      voiceDurationSeconds = state.voiceMessage.audioDurationSeconds,
      isOfflineFallback = !isOnline,
      targetContactIds = activeContactIds.toList()
    )
    dispatchEmergencyAlert(payload, matchingContacts, commMethod)
  }

  private fun dispatchEmergencyAlert(
    payload: EmergencyPayload,
    contacts: List<TrustedContact>,
    commMethod: CommunicationMethod
  ) {
    viewModelScope.launch {
      try {
        val updatedStatuses = if (commMethod == CommunicationMethod.ONLINE_FCM) {
          EmergencyCommunicationHub.dispatchOnlineEmergency(payload, contacts)
        } else {
          EmergencyCommunicationHub.dispatchOfflineSms(appContext, payload, contacts)
        }

        _emergencyState.update { current ->
          current.copy(
            stepIndex = 4,
            communicationMethod = commMethod,
            communicationFailed = false,
            failureReason = null,
            contactStatuses = updatedStatuses
          )
        }

        delay(3000)
        _emergencyState.update { current ->
          current.copy(
            stepIndex = 5,
            contactStatuses = current.contactStatuses.map { it.copy(status = DeliveryStatus.DELIVERED) }
          )
        }
      } catch (e: Throwable) {
        // Communication failure MUST NOT cancel or deactivate the SOS state
        _emergencyState.update { current ->
          current.copy(
            communicationFailed = true,
            failureReason = e.message ?: "Emergency alert dispatch failed."
          )
        }
      }
    }
  }

  // Voice message controls
  private val _isPlayingVoiceNote = MutableStateFlow(false)
  val isPlayingVoiceNote: StateFlow<Boolean> = _isPlayingVoiceNote.asStateFlow()

  fun playVoiceNote() {
    if (emergencyAudioPlayer == null && appContext != null) {
      val player = EmergencyAudioPlayer(appContext)
      emergencyAudioPlayer = player
      com.example.util.EmergencyCommunicationHub.audioPlayer = player
    }
    val voice = _emergencyState.value.voiceMessage
    val path = voice.localFilePath ?: ""
    val transcription = voice.transcription?.ifBlank { null }
      ?: _userProfile.value.preRecordedVoiceTranscription.ifBlank {
        "Emergency SOS alert. Assistance needed immediately."
      }

    _isPlayingVoiceNote.value = true
    val player = emergencyAudioPlayer
    if (player != null) {
      player.playAudioFile(path, transcription) {
        _isPlayingVoiceNote.value = false
      }
    } else {
      _isPlayingVoiceNote.value = false
    }
  }

  fun stopVoiceNotePlayback() {
    emergencyAudioPlayer?.stopAudioPlayback()
    _isPlayingVoiceNote.value = false
  }

  fun shareVoiceNoteViaMms(context: Context) {
    val state = _emergencyState.value
    val path = state.voiceMessage.localFilePath
    val matchingContacts = _trustedContacts.value.filter { contact ->
      state.contactStatuses.any { cs -> cs.contactId == contact.id }
    }
    val phones = matchingContacts.map { it.phone }
    val smsText = EmergencySmsManager.buildEmergencySms(
      EmergencyPayload(
        emergencyId = state.emergencyId,
        userName = _userProfile.value.name,
        latitude = state.latitude,
        longitude = state.longitude,
        accuracyMeters = state.accuracyMeters,
        timestamp = state.timestamp,
        message = "EMERGENCY SOS! Voice note attached.",
        voiceTranscription = state.voiceMessage.transcription,
        localVoiceFilePath = path,
        voiceDurationSeconds = state.voiceMessage.audioDurationSeconds,
        isOfflineFallback = true
      )
    )
    EmergencySmsManager.shareEmergencyMms(context, phones, path, smsText)
  }

  fun sendSosToMessagesApp(context: Context) {
    val loc = _userLocation.value
    val session = _journeySession.value
    val lat = if (loc.hasRealFix) loc.latitude else 37.7749
    val lng = if (loc.hasRealFix) loc.longitude else -122.4194
    val accuracy = loc.accuracyMeters.toInt().coerceAtLeast(5)

    val activeContactIds = if (session.isMonitoring) session.selectedContactIds else _selectedSharingContacts.value
    val matchingContacts = _trustedContacts.value.filter { activeContactIds.contains(it.id) }
    val phones = if (matchingContacts.isNotEmpty()) matchingContacts.map { it.phone } else _trustedContacts.value.map { it.phone }

    val state = _emergencyState.value
    val transcription = state.voiceMessage.transcription?.ifBlank { null }
      ?: _userProfile.value.preRecordedVoiceTranscription.ifBlank { null }

    val payload = EmergencyPayload(
      emergencyId = state.emergencyId.ifBlank { UUID.randomUUID().toString() },
      status = "ACTIVE",
      userName = _userProfile.value.name.ifBlank { "Safe Companion User" },
      userPhone = _userProfile.value.phone,
      latitude = lat,
      longitude = lng,
      accuracyMeters = accuracy,
      timestamp = System.currentTimeMillis(),
      message = "EMERGENCY SOS! I need immediate assistance.",
      journeyDestination = if (session.isMonitoring) session.destination else null,
      voiceTranscription = transcription,
      isOfflineFallback = true
    )

    val smsText = EmergencySmsManager.buildEmergencySms(payload)

    // 1. Dispatch background cellular SMS via SmsManager to all trusted contacts
    EmergencySmsManager.sendEmergencySmsToAll(context, phones, smsText)

    // 2. Open the device's system Messages app directly with the pre-filled SOS text and recipients
    EmergencySmsManager.openEmergencyInMessagesApp(context, phones, smsText)
  }

  fun setVoiceNoteTranscription(transcription: String) {
    _emergencyState.update { current ->
      current.copy(
        voiceMessage = current.voiceMessage.copy(transcription = transcription)
      )
    }
    val currentEmergencyId = _emergencyState.value.emergencyId
    if (currentEmergencyId.isNotBlank()) {
      viewModelScope.launch {
        try {
          ensureRepository()?.updateVoiceMessage(
            emergencyId = currentEmergencyId,
            voiceUrl = _emergencyState.value.voiceMessage.audioUrl,
            localPath = _emergencyState.value.voiceMessage.localFilePath,
            transcription = transcription
          )
        } catch (_: Throwable) {}
      }
    }
  }

  fun startVoiceRecording() {
    if (voiceMessageManager == null) {
      voiceMessageManager = VoiceMessageManager()
    }
    val started = voiceMessageManager?.startRecording() ?: false
    if (started) {
      _emergencyState.update {
        it.copy(voiceMessage = VoiceMessageData(state = VoiceRecordingState.RECORDING))
      }
    } else {
      _emergencyState.update {
        it.copy(
          voiceMessage = VoiceMessageData(
            state = VoiceRecordingState.FAILED,
            errorMessage = "Microphone unavailable or permission denied"
          )
        )
      }
    }
  }

  fun stopVoiceRecording(transcription: String? = null) {
    val recordedData = voiceMessageManager?.stopRecording(transcription) ?: VoiceMessageData(state = VoiceRecordingState.FAILED)
    val isOnline = _networkOverride ?: EmergencyCommunicationHub.isNetworkAvailable(null)

    if (!isOnline) {
      // Offline mode: keep audio locally, set state to RECORDED, queue for sync
      val offlineData = recordedData.copy(
        state = VoiceRecordingState.RECORDED,
        isOfflineRecorded = true,
        isQueuedForSync = true
      )
      _emergencyState.update { it.copy(voiceMessage = offlineData) }

      // Persist local voice file and transcription into Room DB
      val currentEmergencyId = _emergencyState.value.emergencyId
      if (currentEmergencyId.isNotBlank()) {
        viewModelScope.launch {
          try {
            ensureRepository()?.updateVoiceMessage(
              emergencyId = currentEmergencyId,
              voiceUrl = null,
              localPath = offlineData.localFilePath,
              transcription = offlineData.transcription
            )
          } catch (_: Throwable) {}
        }
      }

      // If active emergency was already sent via SMS fallback, re-dispatch updated SMS with the transcription
      if (_emergencyState.value.isTriggered && _emergencyState.value.communicationMethod == CommunicationMethod.SMS_FALLBACK) {
        val session = _journeySession.value
        val activeContactIds = if (session.isMonitoring) session.selectedContactIds else _selectedSharingContacts.value
        val matchingContacts = _trustedContacts.value.filter { activeContactIds.contains(it.id) }
        val updatedPayload = EmergencyPayload(
          emergencyId = _emergencyState.value.emergencyId,
          status = "ACTIVE",
          userName = _userProfile.value.name.ifBlank { "Safe Companion User" },
          latitude = _emergencyState.value.latitude,
          longitude = _emergencyState.value.longitude,
          accuracyMeters = _emergencyState.value.accuracyMeters,
          timestamp = System.currentTimeMillis(),
          message = "EMERGENCY SOS UPDATE: Voice note attached.",
          locationAddress = _emergencyState.value.address,
          voiceTranscription = offlineData.transcription,
          localVoiceFilePath = offlineData.localFilePath,
          voiceDurationSeconds = offlineData.audioDurationSeconds,
          isOfflineFallback = true,
          targetContactIds = activeContactIds.toList()
        )
        dispatchEmergencyAlert(updatedPayload, matchingContacts, CommunicationMethod.SMS_FALLBACK)
      }
      return
    }

    _emergencyState.update { it.copy(voiceMessage = recordedData.copy(state = VoiceRecordingState.UPLOADING)) }

    viewModelScope.launch {
      try {
        val uploadedData = voiceMessageManager?.uploadVoiceMessage(recordedData)
          ?: recordedData.copy(state = VoiceRecordingState.UPLOADED, audioUrl = "https://safecompanion.app/storage/emergency_notes/note.m4a")
        _emergencyState.update { it.copy(voiceMessage = uploadedData) }

        // Update emergency voice recording in Room database
        val currentEmergencyId = _emergencyState.value.emergencyId
        if (currentEmergencyId.isNotBlank() && uploadedData.audioUrl != null) {
          try {
            val repo = ensureRepository()
            repo?.updateVoiceMessage(
              emergencyId = currentEmergencyId,
              voiceUrl = uploadedData.audioUrl,
              localPath = uploadedData.localFilePath,
              transcription = uploadedData.transcription
            )
          } catch (_: Throwable) {
            // Non-blocking persistence safety
          }
        }
      } catch (_: Throwable) {
        _emergencyState.update {
          it.copy(voiceMessage = recordedData.copy(state = VoiceRecordingState.FAILED, errorMessage = "Voice note upload failed"))
        }
      }
    }
  }

  fun cancelVoiceRecording() {
    voiceMessageManager?.cancelRecording()
    _emergencyState.update { it.copy(voiceMessage = VoiceMessageData(state = VoiceRecordingState.IDLE)) }
  }

  // Incoming trusted contact emergency alerts
  fun onIncomingEmergencyAlert(alert: IncomingEmergencyAlert) {
    _incomingAlert.value = alert
    _currentScreen.value = Screen.TrustedContactEmergency.route
  }

  fun acknowledgeAlarm() {
    EmergencyCommunicationHub.acknowledgeAndStopAlarm(null)
    _incomingAlert.update { it?.copy(isAlarmPlaying = false, isAcknowledged = true) }
  }

  fun dismissIncomingAlert() {
    EmergencyCommunicationHub.acknowledgeAndStopAlarm(null)
    _incomingAlert.value = null
    if (_journeySession.value.isMonitoring) {
      _currentScreen.value = Screen.ActiveJourney.route
    } else {
      _currentScreen.value = Screen.Dashboard.route
    }
  }

  fun cancelEmergency() {
    val previousEmergencyId = _emergencyState.value.emergencyId.ifBlank { activeEmergencyId ?: "" }
    _emergencyState.value = EmergencyAlertState(isTriggered = false, isResolved = true)
    if (_journeySession.value.isMonitoring) {
      _currentScreen.value = Screen.ActiveJourney.route
    } else {
      _currentScreen.value = Screen.Dashboard.route
    }

    if (previousEmergencyId.isNotBlank()) {
      viewModelScope.launch {
        try {
          val repo = ensureRepository()
          repo?.resolveEmergency(previousEmergencyId)
          syncManager?.syncEmergencyAsync(previousEmergencyId)
        } catch (_: Throwable) {
          // Non-blocking persistence safety
        }
      }
    }
    activeEmergencyId = null
  }

  fun resolveEmergency() {
    cancelEmergency()
  }

  // Hazards & Community
  fun setHazardFilter(status: HazardStatus?) {
    _hazardFilter.value = status
  }

  fun selectHazard(report: HazardReport) {
    _selectedHazard.value = report
    _currentScreen.value = Screen.ReportDetails.route
  }

  fun submitHazardReport(
    type: HazardType,
    address: String,
    description: String,
    photoUrl: String? = null,
    customId: String? = null
  ) {
    val resolvedType = type
    val loc = _userLocation.value
    val validatedAddress = if (address.isNotBlank()) {
      address.trim()
    } else if (loc.hasRealFix && loc.address.isNotBlank()) {
      loc.address
    } else {
      "Reported Location"
    }

    val validatedDescription = if (description.isNotBlank()) {
      description.trim()
    } else {
      "Community safety report: ${resolvedType.displayName}"
    }

    val lat = if (loc.hasRealFix && loc.latitude != 0.0) loc.latitude else 37.7749
    val lng = if (loc.hasRealFix && loc.longitude != 0.0) loc.longitude else -122.4194

    val hazardId = customId?.takeIf { it.isNotBlank() } ?: "hazard_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

    val entity = com.example.local.entity.CommunityHazardEntity(
      id = hazardId,
      title = resolvedType.displayName,
      hazardType = resolvedType.name,
      locationAddress = validatedAddress,
      reportedTime = "Just now",
      status = HazardStatus.REPORTED.name,
      description = validatedDescription,
      photoUrl = photoUrl,
      confirmationsCount = 1,
      userVoted = true,
      distanceAway = "100m away",
      latitude = lat,
      longitude = lng,
      isReportedByMe = true,
      syncStatus = SyncStatus.PENDING,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    val newReport = entity.toDomainModel()

    // 1. Immediately available locally even when offline
    _hazardReports.update { list ->
      listOf(newReport) + list.filter { it.id != hazardId }
    }
    _selectedHazard.value = newReport
    _currentScreen.value = Screen.ReportDetails.route

    val hazardNotif = SafetyNotification(
      id = "haz_${System.currentTimeMillis()}",
      title = "Safety Hazard Logged: ${resolvedType.displayName}",
      message = "Reported at $validatedAddress. Community notified.",
      timestamp = System.currentTimeMillis(),
      type = NotificationType.HAZARD,
      isRead = false,
      targetScreenRoute = Screen.ReportDetails.route
    )
    addNotification(hazardNotif)
    appContext?.let { ctx ->
      SafetyNotificationManager.showHazardAlertNotification(
        context = ctx,
        title = resolvedType.displayName,
        address = validatedAddress
      )
    }

    // 2. Persist to Room FIRST and trigger asynchronous synchronization
    viewModelScope.launch {
      try {
        val repo = ensureRepository()
        repo?.saveHazard(entity)
        syncManager?.syncHazardAsync(hazardId)
      } catch (e: Throwable) {
        android.util.Log.e("SafeCompanionVM", "Failed to save hazard report: ${e.message}")
      }
    }
  }

  fun deleteHazardReport(hazardId: String) {
    _hazardReports.update { list -> list.filter { it.id != hazardId } }
    if (_selectedHazard.value?.id == hazardId) {
      _selectedHazard.value = null
    }
    viewModelScope.launch {
      try {
        ensureRepository()?.deleteHazard(hazardId)
      } catch (_: Throwable) {}
    }
  }

  fun voteHazardPresence(reportId: String, stillPresent: Boolean) {
    _hazardReports.update { list ->
      list.map { item ->
        if (item.id == reportId) {
          val count = if (stillPresent && item.userVoted != true) item.confirmationsCount + 1 else item.confirmationsCount
          item.copy(confirmationsCount = count, userVoted = stillPresent)
        } else item
      }
    }
    _selectedHazard.update { current ->
      if (current?.id == reportId) {
        val count = if (stillPresent && current.userVoted != true) current.confirmationsCount + 1 else current.confirmationsCount
        current.copy(confirmationsCount = count, userVoted = stillPresent)
      } else current
    }

    viewModelScope.launch {
      try {
        val repo = ensureRepository() ?: return@launch
        val existing = repo.getHazardById(reportId)
        if (existing != null) {
          val count = if (stillPresent && existing.userVoted != true) existing.confirmationsCount + 1 else existing.confirmationsCount
          val updated = existing.copy(
            confirmationsCount = count,
            userVoted = stillPresent,
            updatedAt = System.currentTimeMillis()
          )
          repo.saveHazard(updated)
          syncManager?.syncHazardAsync(reportId)
        }
      } catch (e: Throwable) {
        android.util.Log.e("SafeCompanionVM", "Failed to update hazard vote: ${e.message}")
      }
    }
  }
}
