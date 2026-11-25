package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppMode
import com.example.model.BottomTab
import com.example.ui.screens.*
import com.example.ui.theme.SafeCompanionTheme
import com.example.util.LocationTracker
import com.example.viewmodel.SafeCompanionViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Offload SDK/configuration initialization so main thread frame synchronization completes immediately
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      try {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
      } catch (_: Throwable) {}

      try {
        Configuration.getInstance().load(
          applicationContext,
          applicationContext.getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
      } catch (_: Exception) {}
    }

    setContent {
      SafeCompanionTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          SafeCompanionApp()
        }
      }
    }
  }
}

@Composable
fun SafeCompanionApp(
  viewModel: SafeCompanionViewModel = viewModel()
) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val onboardingStep by viewModel.onboardingStep.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val trustedContacts by viewModel.trustedContacts.collectAsStateWithLifecycle()
  val selectedSharingContacts by viewModel.selectedSharingContacts.collectAsStateWithLifecycle()
  val permissions by viewModel.permissions.collectAsStateWithLifecycle()
  val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
  val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val journeySession by viewModel.journeySession.collectAsStateWithLifecycle()
  val emergencyState by viewModel.emergencyState.collectAsStateWithLifecycle()
  val hazardReports by viewModel.hazardReports.collectAsStateWithLifecycle()
  val selectedHazard by viewModel.selectedHazard.collectAsStateWithLifecycle()
  val hazardFilter by viewModel.hazardFilter.collectAsStateWithLifecycle()
  val incomingAlert by viewModel.incomingAlert.collectAsStateWithLifecycle()
  val routeOptions by viewModel.routeOptions.collectAsStateWithLifecycle()
  val selectedRouteOption by viewModel.selectedRouteOption.collectAsStateWithLifecycle()
  val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsStateWithLifecycle()
  val journeyHistory by viewModel.journeyHistory.collectAsStateWithLifecycle()
  val selectedHistoricalJourney by viewModel.selectedHistoricalJourney.collectAsStateWithLifecycle()
  val isPlayingVoiceNote by viewModel.isPlayingVoiceNote.collectAsStateWithLifecycle()
  val isOnboardingRecordingVoice by viewModel.isOnboardingRecordingVoice.collectAsStateWithLifecycle()
  val isOnboardingPlayingVoice by viewModel.isOnboardingPlayingVoice.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

  // Initialize context and background services without blocking UI frame drawing
  LaunchedEffect(Unit) {
    viewModel.setAppContext(context)

    // Offload Room database initialization, sync manager, and channels creation off the UI frame thread
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val db = com.example.local.SafeCompanionDatabase.getInstance(context)
      val repo = com.example.local.repository.SafeCompanionLocalRepository(db)
      val connectivityMonitor = com.example.sync.NetworkConnectivityMonitor(context)
      val syncManager = com.example.sync.SafeCompanionSyncManager(
        localRepository = repo,
        connectivityObserver = connectivityMonitor
      )

      com.example.util.SafetyNotificationManager.createNotificationChannels(context)
      com.example.util.EmergencyNotificationHelper.createNotificationChannel(context)

      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        viewModel.setLocalRepository(repo)
        viewModel.setSyncManager(syncManager)
        syncManager.startMonitoring()
        syncManager.syncPendingData()
      }
    }

    // Handle incoming notification navigation intent if user tapped an Android notification
    val activity = context as? ComponentActivity
    val targetRoute = activity?.intent?.getStringExtra(com.example.util.SafetyNotificationManager.EXTRA_TARGET_ROUTE)
    if (!targetRoute.isNullOrBlank()) {
      viewModel.navigateTo(targetRoute)
    }
  }

  // Location Permission Launcher for accurate GPS
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissionsMap ->
    val fineGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    val isGranted = fineGranted || coarseGranted
    viewModel.onLocationPermissionChanged(isGranted, context)
    viewModel.setPermissionGranted("location", isGranted, context)
  }

  // Notification Permission Launcher
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setPermissionGranted("notifications", isGranted, context)
  }

  // Background Location Permission Launcher
  val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setPermissionGranted("background", isGranted, context)
  }

  // SMS Permission Launcher
  val smsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissionsMap ->
    val sendGranted = permissionsMap[Manifest.permission.SEND_SMS] == true
    val receiveGranted = permissionsMap[Manifest.permission.RECEIVE_SMS] == true
    val isGranted = sendGranted || receiveGranted
    viewModel.setPermissionGranted("sms", isGranted, context)
  }

  // Audio Recording Permission Launcher
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setPermissionGranted("audio", isGranted, context)
  }

  val requestLocationPermission = {
    locationPermissionLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
  }

  val onRequestPermission: (String) -> Unit = { type ->
    when (type) {
      "location" -> {
        locationPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
          )
        )
      }
      "notifications" -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          viewModel.setPermissionGranted("notifications", true, context)
        }
      }
      "background" -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
          viewModel.setPermissionGranted("background", true, context)
        }
      }
      "sms" -> {
        smsPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
          )
        )
      }
      "audio" -> {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      }
    }
  }

  // Initial check on app startup (checks status silently without unprompted dialogs)
  LaunchedEffect(Unit) {
    val tracker = LocationTracker(context)
    val hasLocation = tracker.hasLocationPermission()
    if (hasLocation) {
      viewModel.onLocationPermissionChanged(true, context)
      viewModel.setPermissionGranted("location", true, context)
    }

    val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    if (hasNotifications) {
      viewModel.setPermissionGranted("notifications", true, context)
    }

    val hasSms = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
    if (hasSms) {
      viewModel.setPermissionGranted("sms", true, context)
    }

    val hasBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      hasLocation
    }
    if (hasBg) {
      viewModel.setPermissionGranted("background", true, context)
    }
  }

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
      when (screen) {
        Screen.Onboarding.route -> {
          OnboardingScreen(
            step = onboardingStep,
            userProfile = userProfile,
            permissions = permissions,
            trustedContacts = trustedContacts,
            onRequestLocationPermission = requestLocationPermission,
            onRequestPermission = onRequestPermission,
            onNext = { viewModel.nextOnboardingStep() },
            onPrev = { viewModel.prevOnboardingStep() },
            onStepSelected = { viewModel.setOnboardingStep(it) },
            onUpdateProfile = { name, phone, email -> viewModel.updateProfile(name, phone, email) },
            onSaveProfileDetails = { name, phone, email -> viewModel.saveProfileAndComplete(name, phone, email) },
            onLogin = { name, email, phone -> viewModel.loginAndSaveDetails(name, email, phone) },
            onTogglePermission = { viewModel.togglePermission(it, context) },
            onAddContact = { name, rel, phone, isPrimary -> viewModel.addTrustedContact(name, rel, phone, isPrimary) },
            onRemoveContact = { viewModel.removeTrustedContact(it) },
            onComplete = { viewModel.completeOnboarding() },
            isRecordingVoice = isOnboardingRecordingVoice,
            isPlayingVoice = isOnboardingPlayingVoice,
            onStartRecordVoice = { viewModel.startPreRecordingVoice() },
            onStopRecordVoice = { transcription -> viewModel.stopPreRecordingVoice(transcription) },
            onPlayVoice = { viewModel.playPreRecordedVoice() },
            onStopPlayVoice = { viewModel.stopPreRecordedVoicePlayback() },
            onUpdateVoiceTranscription = { viewModel.updatePreRecordedTranscription(it) },
            onClearVoice = { viewModel.clearPreRecordedVoiceNote() }
          )
        }

        Screen.Dashboard.route -> {
          DashboardScreen(
            userProfile = userProfile,
            trustedContacts = trustedContacts,
            currentMode = currentMode,
            currentTab = currentTab,
            userLocation = userLocation,
            onRequestLocationPermission = requestLocationPermission,
            onRefreshLocation = { viewModel.refreshLocation(context) },
            onModeToggle = { viewModel.switchMode(it) },
            onTabSelected = { viewModel.selectBottomTab(it) },
            onStartJourneyClick = { viewModel.navigateTo(Screen.JourneySetup.route) },
            onManageContactsClick = { viewModel.navigateTo(Screen.TrustedContacts.route) },
            onAddContactClick = { viewModel.navigateTo(Screen.TrustedContacts.route) },
            onRecentJourneyClick = { viewModel.selectBottomTab(BottomTab.JOURNEY) },
            onNotificationClick = { viewModel.navigateTo(Screen.Notifications.route) },
            unreadNotificationsCount = unreadNotificationsCount,
            onSosClick = { viewModel.openSosConfirmation() },
            onOpenMessagesApp = { viewModel.sendSosToMessagesApp(context) }
          )
        }

        Screen.JourneySetup.route -> {
          JourneySetupScreen(
            trustedContacts = trustedContacts,
            selectedContactIds = selectedSharingContacts,
            userLocation = userLocation,
            routeOptions = routeOptions,
            selectedRouteOption = selectedRouteOption,
            isLoadingRoutes = isLoadingRoutes,
            savedPlaces = userProfile.savedPlaces,
            onRequestLocationPermission = requestLocationPermission,
            onRefreshLocation = { viewModel.refreshLocation(context) },
            onBackClick = { viewModel.navigateTo(Screen.Dashboard.route) },
            onStartJourney = { startLoc, dest, note, stops -> viewModel.startJourney(startLoc, dest, note, context, stops = stops) },
            onToggleContactSelection = { viewModel.toggleContactSelectionForJourney(it) },
            onSelectRouteOption = { viewModel.selectRouteOption(it) },
            onDestinationChanged = { dest -> viewModel.calculateRouteRecommendations(dest = dest) }
          )
        }

        Screen.ActiveJourney.route -> {
          ActiveJourneyScreen(
            journeySession = journeySession,
            trustedContacts = trustedContacts.filter { journeySession.selectedContactIds.contains(it.id) },
            currentTab = currentTab,
            userLocation = userLocation,
            hazardReports = hazardReports,
            onTabSelected = { viewModel.selectBottomTab(it) },
            onBackClick = { viewModel.navigateTo(Screen.Dashboard.route) },
            onSosClick = { viewModel.openSosConfirmation() },
            onCompleteJourney = { viewModel.completeJourney() },
            onStartNextDestination = { viewModel.startNextDestination() },
            onArriveAtCurrentStop = { viewModel.arriveAtCurrentStop() },
            onAcknowledgeDeviation = { viewModel.acknowledgeDeviation() },
            onAcknowledgeLongStop = { viewModel.acknowledgeLongStop() },
            onTriggerDeviation = { viewModel.triggerRouteDeviation() },
            onTriggerLongStop = { viewModel.triggerLongStopAlert() }
          )
        }

        Screen.SosConfirmation.route -> {
          val contactsForSos = if (journeySession.isMonitoring) {
            trustedContacts.filter { journeySession.selectedContactIds.contains(it.id) }
          } else {
            trustedContacts.filter { selectedSharingContacts.contains(it.id) }
          }
          SosConfirmationScreen(
            trustedContacts = contactsForSos,
            onConfirmSos = { viewModel.triggerEmergency() },
            onCancel = { viewModel.cancelSosConfirmation() },
            voiceMessageData = emergencyState.voiceMessage,
            userProfile = userProfile,
            onStartVoiceRecording = {
              val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
              ) == android.content.pm.PackageManager.PERMISSION_GRANTED
              if (!hasAudio) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
              viewModel.startVoiceRecording()
            },
            onStopVoiceRecording = { viewModel.stopVoiceRecording() },
            isPlayingVoiceNote = isPlayingVoiceNote,
            onPlayVoiceNote = { viewModel.playVoiceNote() },
            onStopVoicePlayback = { viewModel.stopVoiceNotePlayback() },
            onOpenMessagesApp = { viewModel.sendSosToMessagesApp(context) }
          )
        }

        Screen.EmergencyStatus.route -> {
          EmergencyStatusScreen(
            emergencyState = emergencyState,
            isPlayingVoiceNote = isPlayingVoiceNote,
            onCancelEmergency = { viewModel.cancelEmergency() },
            onStartVoiceRecording = { viewModel.startVoiceRecording() },
            onStopVoiceRecording = { viewModel.stopVoiceRecording() },
            onCancelVoiceRecording = { viewModel.cancelVoiceRecording() },
            onRetryDispatch = { viewModel.retryEmergencyDispatch() },
            onPlayVoiceNote = { viewModel.playVoiceNote() },
            onStopVoicePlayback = { viewModel.stopVoiceNotePlayback() },
            onShareVoiceNoteViaMms = { viewModel.shareVoiceNoteViaMms(context) },
            onOpenMessagesApp = { viewModel.sendSosToMessagesApp(context) },
            onEditTranscription = { viewModel.setVoiceNoteTranscription(it) }
          )
        }

        Screen.TrustedContactEmergency.route -> {
          val alert = incomingAlert ?: com.example.model.IncomingEmergencyAlert(
            emergencyId = emergencyState.emergencyId.ifBlank { "demo_alert" },
            senderName = "Companion in Distress",
            senderPhone = "+1 (555) 019-2834",
            relationship = "Trusted Contact",
            latitude = emergencyState.latitude,
            longitude = emergencyState.longitude,
            accuracyMeters = emergencyState.accuracyMeters,
            timestamp = emergencyState.timestamp,
            message = "Emergency SOS Alert triggered.",
            journeyDestination = journeySession.destination.ifBlank { "Home" }
          )
          TrustedContactEmergencyScreen(
            alert = alert,
            onAcknowledgeAlarm = { viewModel.acknowledgeAlarm() },
            onDismiss = { viewModel.dismissIncomingAlert() }
          )
        }

        Screen.JourneyCompleted.route -> {
          JourneyCompletedScreen(
            journeySession = journeySession,
            onBackToDashboard = { viewModel.navigateTo(Screen.Dashboard.route) },
            onViewDetails = { viewModel.navigateTo(Screen.JourneyMap.route) }
          )
        }

        Screen.JourneyMap.route -> {
          JourneyMapScreen(
            journeySession = journeySession,
            journeyHistory = journeyHistory,
            selectedJourneyRecord = selectedHistoricalJourney,
            userLocation = userLocation,
            currentTab = currentTab,
            onTabSelected = { viewModel.selectBottomTab(it) },
            onSelectJourneyRecord = { viewModel.selectHistoricalJourney(it) },
            onStartNewJourneyClick = { viewModel.navigateTo(Screen.JourneySetup.route) },
            onNavigateToActiveJourney = { viewModel.navigateTo(Screen.ActiveJourney.route) },
            onBackClick = { viewModel.navigateTo(Screen.Dashboard.route) },
            onEmergencyClick = { viewModel.openSosConfirmation() }
          )
        }

        Screen.CommunityDashboard.route, Screen.CommunityReports.route -> {
          CommunityScreen(
            userProfile = userProfile,
            hazardReports = hazardReports,
            currentFilter = hazardFilter,
            currentMode = currentMode,
            currentTab = currentTab,
            userLocation = userLocation,
            initialSubTab = if (currentScreen == Screen.CommunityReports.route) {
              com.example.ui.screens.CommunitySubTab.MY_REPORTING
            } else {
              com.example.ui.screens.CommunitySubTab.FEED
            },
            onModeToggle = { viewModel.switchMode(it) },
            onTabSelected = { viewModel.selectBottomTab(it) },
            onFilterSelected = { viewModel.setHazardFilter(it) },
            onHazardClick = { viewModel.selectHazard(it) },
            onReportHazardClick = { viewModel.navigateTo(Screen.ReportHazard.route) },
            onNotificationClick = { viewModel.navigateTo(Screen.Notifications.route) },
            unreadNotificationsCount = unreadNotificationsCount,
            onDeleteHazardReport = { viewModel.deleteHazardReport(it) }
          )
        }

        Screen.ReportHazard.route -> {
          ReportHazardScreen(
            userLocation = userLocation,
            onRequestLocationPermission = requestLocationPermission,
            onBackClick = { viewModel.navigateTo(Screen.CommunityDashboard.route) },
            onSubmitReport = { type, address, desc, photoUrl ->
              viewModel.submitHazardReport(type, address, desc, photoUrl)
            }
          )
        }

        Screen.ReportDetails.route -> {
          ReportDetailsScreen(
            report = selectedHazard,
            onBackClick = { viewModel.navigateTo(Screen.CommunityDashboard.route) },
            onVoteHazard = { reportId, stillPresent ->
              viewModel.voteHazardPresence(reportId, stillPresent)
            }
          )
        }

        Screen.Settings.route -> {
          SettingsScreen(
            userProfile = userProfile,
            trustedContacts = trustedContacts,
            permissions = permissions,
            currentTab = currentTab,
            onTabSelected = { viewModel.selectBottomTab(it) },
            onOpenSavedPlaces = { viewModel.navigateTo(Screen.SavedPlaces.route) },
            onOpenTrustedContacts = { viewModel.navigateTo(Screen.TrustedContacts.route) },
            onOpenPermissions = { viewModel.navigateTo(Screen.PermissionsSettings.route) },
            onOpenPrivacy = { viewModel.navigateTo(Screen.PrivacySettings.route) },
            onOpenNotifications = { viewModel.navigateTo(Screen.Notifications.route) },
            onRestartOnboarding = { viewModel.restartOnboarding() },
            onLogout = { viewModel.logoutUser() },
            onUpdateProfile = { name, phone, email -> viewModel.updateProfile(name, phone, email) },
            onAddContact = { name, rel, phone, isPrimary -> viewModel.addTrustedContact(name, rel, phone, isPrimary) },
            onRemoveContact = { viewModel.removeTrustedContact(it) }
          )
        }

        Screen.SavedPlaces.route -> {
          com.example.ui.screens.SavedPlacesScreen(
            savedPlaces = userProfile.savedPlaces,
            onBackClick = { viewModel.navigateTo(Screen.Settings.route) },
            onAddOrUpdatePlace = { viewModel.addOrUpdateSavedPlace(it) },
            onDeletePlace = { viewModel.deleteSavedPlace(it) }
          )
        }

        Screen.TrustedContacts.route -> {
          TrustedContactsManagerScreen(
            trustedContacts = trustedContacts,
            onBackClick = { viewModel.navigateTo(Screen.Settings.route) },
            onAddContact = { name, rel, phone, isPrimary ->
              viewModel.addTrustedContact(name, rel, phone, isPrimary)
            },
            onRemoveContact = { viewModel.removeTrustedContact(it) }
          )
        }

        Screen.PermissionsSettings.route -> {
          PermissionsSettingsScreen(
            permissions = permissions,
            onTogglePermission = { viewModel.togglePermission(it, context) },
            onRequestPermission = onRequestPermission,
            onTestSmsInMessagesApp = { viewModel.sendSosToMessagesApp(context) },
            onBackClick = { viewModel.navigateTo(Screen.Settings.route) }
          )
        }

        Screen.PrivacySettings.route -> {
          PrivacySettingsScreen(
            onBackClick = { viewModel.navigateTo(Screen.Settings.route) }
          )
        }

        Screen.Notifications.route -> {
          NotificationsScreen(
            notifications = notifications,
            isNotificationPermissionGranted = permissions.notifications,
            onRequestNotificationPermission = { onRequestPermission("notifications") },
            onSendTestNotification = { viewModel.sendTestNotification(context) },
            onMarkAsRead = { viewModel.markNotificationAsRead(it) },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
            onDeleteNotification = { viewModel.deleteNotification(it) },
            onClearAll = { viewModel.clearAllNotifications() },
            onNavigateToScreen = { route -> viewModel.navigateTo(route) },
            onBackClick = { viewModel.navigateTo(Screen.Dashboard.route) }
          )
        }
      }
    }
  }
}
