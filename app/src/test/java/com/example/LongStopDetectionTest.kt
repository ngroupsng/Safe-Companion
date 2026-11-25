package com.example

import com.example.model.LongStopState
import com.example.model.RouteDeviationState
import com.example.viewmodel.SafeCompanionViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LongStopDetectionTest {

  @Test
  fun test1_normalMovement() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    var currentLat = session.startLat
    var currentLng = session.startLng
    var time = 1000000L

    // Simulate moving step-by-step
    for (i in 1..5) {
      currentLat += 0.001 // ~110m movement each step
      time += 30000L // 30 sec interval
      viewModel.updateJourneyMetrics(currentLat, currentLng, speedMps = 1.4f, accuracyMeters = 5f, currentTimeMillis = time)
      assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
      assertFalse(viewModel.journeySession.value.isLongStopAcknowledged)
    }
  }

  @Test
  fun test2_shortStop() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    // Stop at location for 2 minutes (less than 5 min threshold)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)

    val after2Min = baseTime + (2 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after2Min)
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)
    assertEquals(2, viewModel.journeySession.value.stationaryDurationMinutes)
  }

  @Test
  fun test3_longStopExceedingThreshold() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    // Start stationary
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)

    // After 5 minutes -> Escalates to LONG_STOP_WARNING
    val after5Min = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)
    assertEquals(5, viewModel.journeySession.value.stationaryDurationMinutes)
    assertFalse(viewModel.journeySession.value.isLongStopAcknowledged)
  }

  @Test
  fun test4_gpsNoiseDoesNotResetStationaryTimer() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)

    // Jitter coordinates by 3 meters (approx 0.00003 degrees)
    val jitterLat = session.startLat + 0.00003
    val jitterLng = session.startLng + 0.00003
    val after6Min = baseTime + (6 * 60 * 1000L)

    viewModel.updateJourneyMetrics(jitterLat, jitterLng, speedMps = 0.1f, accuracyMeters = 6f, currentTimeMillis = after6Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)
    assertEquals(6, viewModel.journeySession.value.stationaryDurationMinutes)
  }

  @Test
  fun test5_poorGpsAccuracyHandled() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    // Initial stationary fix
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)

    // Reading with poor accuracy (80m > 65m threshold) after 10 mins
    val after10Min = baseTime + (10 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 80f, currentTimeMillis = after10Min)

    // Should remain in previous state without escalating
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)
  }

  @Test
  fun test6_movementResumes() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    // Escalate to warning
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    val after5Min = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)

    // User resumes moving (moved 50 meters away)
    val movedLat = session.startLat + 0.0005
    val movedLng = session.startLng + 0.0005
    val after6Min = baseTime + (6 * 60 * 1000L)
    viewModel.updateJourneyMetrics(movedLat, movedLng, speedMps = 1.4f, accuracyMeters = 5f, currentTimeMillis = after6Min)

    // State returns to NORMAL and stationary timer resets
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
    assertEquals(0, viewModel.journeySession.value.stationaryDurationMinutes)
    assertEquals(0L, viewModel.journeySession.value.stationarySinceTimestamp)
  }

  @Test
  fun test7_userAcknowledgement() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    val after5Min = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)
    assertFalse(viewModel.journeySession.value.isLongStopAcknowledged)

    // User acknowledges "Yes, I'm Safe"
    viewModel.acknowledgeLongStop()
    assertTrue(viewModel.journeySession.value.isLongStopAcknowledged)
    assertTrue("Journey must still be monitoring", viewModel.journeySession.value.isMonitoring)
  }

  @Test
  fun test8_journeyCompletionClearsState() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    val after5Min = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)

    // Complete journey
    viewModel.completeJourney()
    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)
    assertEquals(0L, viewModel.journeySession.value.stationarySinceTimestamp)
    assertEquals(0, viewModel.journeySession.value.stationaryDurationMinutes)
  }

  @Test
  fun test9_newJourneyResetsState() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start1", "Dest1", "")
    val baseTime = 1000000L
    viewModel.updateJourneyMetrics(37.77, -122.41, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    viewModel.completeJourney()

    // Start a new journey
    viewModel.startJourney("Start2", "Dest2", "")
    val session = viewModel.journeySession.value
    assertTrue(session.isMonitoring)
    assertEquals(LongStopState.NORMAL, session.longStopState)
    assertEquals(0L, session.stationarySinceTimestamp)
    assertEquals(0, session.stationaryDurationMinutes)
    assertFalse(session.isLongStopAcknowledged)
  }

  @Test
  fun test10_routeDeviationAndLongStopOperateIndependently() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    val baseTime = 1000000L
    // Off path (0.05 deg off ~ 5.5km) AND stationary for 5 min
    val devLat = session.startLat + 0.05
    val devLng = session.startLng + 0.05

    // Reading 1: User moves off-route to devLat, devLng -> meaningful movement updates anchor, longStopState is NORMAL
    viewModel.updateJourneyMetrics(devLat, devLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    assertEquals(RouteDeviationState.WARNING, viewModel.journeySession.value.routeDeviationState)
    assertEquals(LongStopState.NORMAL, viewModel.journeySession.value.longStopState)

    // Reading 2: User remains at devLat, devLng -> now stationary at the new anchor
    viewModel.updateJourneyMetrics(devLat, devLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime + 10000L)
    assertEquals(RouteDeviationState.WARNING, viewModel.journeySession.value.routeDeviationState)
    assertEquals(LongStopState.STATIONARY, viewModel.journeySession.value.longStopState)

    // Reading 3: Confirmed deviation (3rd reading) and stationary >= 5 min
    val after5Min = baseTime + 10000L + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(devLat, devLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(RouteDeviationState.DEVIATED, viewModel.journeySession.value.routeDeviationState)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)
  }

  @Test
  fun test11_sosFlowRemainsIndependent() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")

    // Stationary warning active
    val baseTime = 1000000L
    val after5Min = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(37.7749, -122.4194, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    viewModel.updateJourneyMetrics(37.7749, -122.4194, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = after5Min)
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)

    // SOS not automatically triggered by long-stop
    assertFalse(viewModel.emergencyState.value.isTriggered)

    // User can trigger SOS independently
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)
  }
}
