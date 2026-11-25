package com.example

import com.example.model.BottomTab
import com.example.model.LongStopState
import com.example.model.RouteDeviationState
import com.example.viewmodel.SafeCompanionViewModel
import com.example.viewmodel.Screen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JourneyLifecycleTest {

  @Test
  fun test1_reachDestination_showsJourneyCompletedUI() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "Commute")

    // Active journey started
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isMonitoring)
    assertFalse(viewModel.journeySession.value.isDestinationReached)

    val session = viewModel.journeySession.value
    // Simulate user reaching destination (coordinates matching destLat, destLng)
    viewModel.updateJourneyMetrics(
      currentLat = session.destLat,
      currentLng = session.destLng,
      speedMps = 1.2f,
      accuracyMeters = 5f
    )

    // Verify Journey Completed UI appears
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isDestinationReached)
    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertEquals(0.0, viewModel.journeySession.value.remainingKm, 0.01)
    assertEquals(0, viewModel.journeySession.value.etaMinutes)
    assertEquals(1.0f, viewModel.journeySession.value.progress, 0.01f)
  }

  @Test
  fun test2_afterCompletion_gpsMonitoringStops() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "")
    val session = viewModel.journeySession.value

    // Reach destination to complete journey
    viewModel.updateJourneyMetrics(session.destLat, session.destLng, speedMps = 1.0f, accuracyMeters = 5f)
    assertFalse("Monitoring must be stopped", viewModel.journeySession.value.isMonitoring)

    // Further location updates must be ignored
    val completedRemaining = viewModel.journeySession.value.remainingKm
    val completedProgress = viewModel.journeySession.value.progress
    viewModel.updateJourneyMetrics(
      currentLat = session.startLat,
      currentLng = session.startLng,
      speedMps = 2.0f,
      accuracyMeters = 5f
    )

    assertEquals(completedRemaining, viewModel.journeySession.value.remainingKm, 0.01)
    assertEquals(completedProgress, viewModel.journeySession.value.progress, 0.01f)
    assertFalse(viewModel.journeySession.value.isMonitoring)
  }

  @Test
  fun test3_afterCompletion_routeDeviationMonitoringStops() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "")
    viewModel.completeJourney()

    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)

    // Simulate location far off path
    viewModel.updateJourneyMetrics(
      currentLat = 0.0,
      currentLng = 0.0,
      speedMps = 1.4f,
      accuracyMeters = 5f
    )

    // Route deviation state must remain NORMAL and readings count at 0
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(0, viewModel.journeySession.value.consecutiveDeviationReadings)
  }

  @Test
  fun test4_afterCompletion_longStopMonitoringStops() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "")
    viewModel.completeJourney()

    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)

    // Simulate stationary location with 15 minutes elapsed
    val baseTime = 2000000L
    viewModel.updateJourneyMetrics(37.77, -122.41, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime + (15 * 60 * 1000L))

    // Long stop state must remain NORMAL and duration at 0
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
    assertEquals(0, viewModel.journeySession.value.stationaryDurationMinutes)
  }

  @Test
  fun test5_secondJourney_hasCleanMonitoringState() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start1", "Dest1", "First Trip")
    val session1 = viewModel.journeySession.value

    // Trigger deviation on first journey
    val devLat = session1.startLat + 0.05
    val devLng = session1.startLng + 0.05
    repeat(3) {
      viewModel.updateJourneyMetrics(devLat, devLng, speedMps = 1.4f, accuracyMeters = 5f)
    }
    assertEquals(RouteDeviationState.DEVIATED, viewModel.journeySession.value.routeDeviationState)

    // Complete first journey
    viewModel.completeJourney()
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)

    // Start a second journey
    viewModel.startJourney("Start2", "Dest2", "Second Trip")
    val session2 = viewModel.journeySession.value

    assertEquals("Start2", session2.startLocation)
    assertEquals("Dest2", session2.destination)
    assertTrue("New journey must be actively monitoring", session2.isMonitoring)
    assertFalse("Destination must not be reached yet", session2.isDestinationReached)
    assertEquals(0.0f, session2.progress, 0.01f)

    // Verify all safety monitoring states are reset cleanly without carry-over
    assertEquals(RouteDeviationState.NORMAL, session2.routeDeviationState)
    assertEquals(0, session2.consecutiveDeviationReadings)
    assertEquals(0f, session2.deviationDistanceMeters, 0.01f)
    assertFalse(session2.isDeviationAcknowledgedByUser)

    assertEquals(LongStopState.NORMAL, session2.longStopState)
    assertEquals(0L, session2.stationarySinceTimestamp)
    assertEquals(0, session2.stationaryDurationMinutes)
    assertFalse(session2.isLongStopAcknowledged)
    assertEquals(0, session2.warningsCount)
  }

  @Test
  fun test6_activeWarning_clearedWhenDestinationReached() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Home", "Library", "")
    val session = viewModel.journeySession.value

    // Escalate route deviation warning
    val devLat = session.startLat + 0.05
    val devLng = session.startLng + 0.05
    repeat(3) {
      viewModel.updateJourneyMetrics(devLat, devLng, speedMps = 1.4f, accuracyMeters = 5f)
    }
    assertEquals(RouteDeviationState.DEVIATED, viewModel.journeySession.value.routeDeviationState)

    // User now reaches destination within proximity threshold
    viewModel.updateJourneyMetrics(
      currentLat = session.destLat,
      currentLng = session.destLng,
      speedMps = 1.2f,
      accuracyMeters = 5f
    )

    // Journey must complete cleanly with no stale warning state
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertTrue(viewModel.journeySession.value.isDestinationReached)
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(0, viewModel.journeySession.value.consecutiveDeviationReadings)
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
    assertFalse(viewModel.journeySession.value.isDeviationAcknowledgedByUser)
    assertFalse(viewModel.journeySession.value.isLongStopAcknowledged)
  }

  @Test
  fun test7_returnToNormalAppStateFromCompletionScreen() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Grandma's House", "Family visit")
    val session = viewModel.journeySession.value

    // Complete journey by reaching destination
    viewModel.updateJourneyMetrics(session.destLat, session.destLng, speedMps = 1.0f, accuracyMeters = 5f)
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)

    // Preserve final journey summary information
    assertEquals("Start Point", viewModel.journeySession.value.startLocation)
    assertEquals("Grandma's House", viewModel.journeySession.value.destination)
    assertTrue("Total distance must be positive", viewModel.journeySession.value.totalKm > 0)
    assertTrue("Duration minutes must be positive", viewModel.journeySession.value.durationMinutes > 0)

    // Return to dashboard
    viewModel.navigateTo(Screen.Dashboard.route)
    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)
    assertEquals(BottomTab.HOME, viewModel.currentTab.value)
  }

  @Test
  fun test8_safeArrivalButton_terminatesJourneyCleanly() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Work", "Home", "")
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isMonitoring)

    // User taps "I've Arrived Safely" button
    viewModel.completeJourney()

    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertTrue(viewModel.journeySession.value.isDestinationReached)
    assertEquals(0.0, viewModel.journeySession.value.remainingKm, 0.01)
    assertEquals(1.0f, viewModel.journeySession.value.progress, 0.01f)
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
  }

  @Test
  fun test9_backgroundLocationService_feedsJourneyMetricsAndStopsOnCompletion() = kotlinx.coroutines.test.runTest {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val viewModel = SafeCompanionViewModel()

    val targetDestLat = 37.7850
    val targetDestLng = -122.4050

    viewModel.startJourney(
      startLoc = "Work",
      dest = "Home",
      note = "Testing background service",
      context = context,
      destLat = targetDestLat,
      destLng = targetDestLng
    )
    assertTrue("Journey must be actively monitoring", viewModel.journeySession.value.isMonitoring)
    assertTrue("Background JourneyTrackingService must be active", com.example.service.JourneyTrackingService.isTrackingActive)

    // Simulate location update received from background service matching destination coordinates
    val mockLocation = android.location.Location("gps").apply {
      latitude = targetDestLat
      longitude = targetDestLng
      speed = 1.2f
      accuracy = 5f
      time = System.currentTimeMillis()
    }
    viewModel.handleLocationUpdate(mockLocation, context)

    // Allow ViewModel coroutines (viewModelScope) to run
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    testScheduler.advanceUntilIdle()

    // Reached destination -> Journey finishes and background service stops
    assertFalse("Monitoring must stop after arriving at destination", viewModel.journeySession.value.isMonitoring)
    assertTrue("Destination reached flag must be true", viewModel.journeySession.value.isDestinationReached)
    assertFalse("Background service must be stopped on journey completion", com.example.service.JourneyTrackingService.isTrackingActive)
  }

  @Test
  fun test10_journeyHistory_containsPreviousJourneyDetailsAndRecords() {
    val viewModel = SafeCompanionViewModel()
    val history = viewModel.journeyHistory.value

    // Verify history contains previous journey records
    assertTrue("Journey history records must not be empty", history.isNotEmpty())
    val firstRecord = history.first()
    assertNotNull("First record must have destination", firstRecord.destination)
    assertTrue("First record distance must be positive", firstRecord.distanceKm > 0)
    assertTrue("First record duration must be positive", firstRecord.durationMinutes > 0)
    assertTrue("First record safety score must be high", firstRecord.safetyScore >= 90)

    // Select a record to inspect and verify details load into selectedHistoricalJourney
    val targetRecord = history.last()
    viewModel.selectHistoricalJourney(targetRecord)

    val selected = viewModel.selectedHistoricalJourney.value
    assertNotNull("Selected historical journey must be set", selected)
    assertEquals(targetRecord.id, selected?.id)
    assertEquals(targetRecord.destination, selected?.destination)
    assertEquals(targetRecord.distanceKm, selected?.distanceKm ?: 0.0, 0.01)
  }

  @Test
  fun test11_bottomTab_journey_navigatesToJourneyRecordsScreen() {
    val viewModel = SafeCompanionViewModel()

    // When not monitoring, selecting JOURNEY tab opens JourneyMap screen
    viewModel.selectBottomTab(BottomTab.JOURNEY)
    assertEquals(Screen.JourneyMap.route, viewModel.currentScreen.value)
    assertEquals(BottomTab.JOURNEY, viewModel.currentTab.value)

    // When monitoring is active, selecting JOURNEY tab returns to ActiveJourney HUD
    viewModel.startJourney("Start", "Dest", "")
    viewModel.selectBottomTab(BottomTab.HOME)
    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)

    viewModel.selectBottomTab(BottomTab.JOURNEY)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
  }
}

