package com.example

import com.example.model.RouteDeviationState
import com.example.util.LocationTracker
import com.example.viewmodel.SafeCompanionViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RouteDeviationTest {

  @Test
  fun testDistanceToSegmentCalculation() {
    // Point on segment: (0, 0) to (0, 0.01) -> distance to (0, 0.005) should be ~0 meters
    val distOn = LocationTracker.calculateDistanceToSegmentMeters(
      pLat = 0.0, pLng = 0.005,
      aLat = 0.0, aLng = 0.0,
      bLat = 0.0, bLng = 0.01
    )
    assertTrue("Distance on line should be very close to 0m", distOn < 1.0f)

    // Point 100 meters off segment
    // At equator, 0.001 deg latitude is approx 110.5 meters
    val distOff = LocationTracker.calculateDistanceToSegmentMeters(
      pLat = 0.001, pLng = 0.005,
      aLat = 0.0, aLng = 0.0,
      bLat = 0.0, bLng = 0.01
    )
    assertTrue("Distance off line should be approx 110m, was $distOff", distOff in 100f..125f)
  }

  @Test
  fun testRouteDeviationTransitions() {
    val viewModel = SafeCompanionViewModel()
    // Start journey
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value
    assertEquals(RouteDeviationState.NORMAL, session.routeDeviationState)

    // 1. Normal point directly on start
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 1.4f, accuracyMeters = 5f)
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(0, viewModel.journeySession.value.consecutiveDeviationReadings)

    // 2. Reading far away (0.05 degrees ~ 5.5km off path) -> Reading 1 (WARNING)
    val deviatedLat = session.startLat + 0.05
    val deviatedLng = session.startLng + 0.05
    viewModel.updateJourneyMetrics(deviatedLat, deviatedLng, speedMps = 1.4f, accuracyMeters = 5f)
    assertEquals(RouteDeviationState.WARNING, viewModel.journeySession.value.routeDeviationState)
    assertEquals(1, viewModel.journeySession.value.consecutiveDeviationReadings)

    // 3. Reading 2 far away -> Still WARNING
    viewModel.updateJourneyMetrics(deviatedLat, deviatedLng, speedMps = 1.4f, accuracyMeters = 5f)
    assertEquals(RouteDeviationState.WARNING, viewModel.journeySession.value.routeDeviationState)
    assertEquals(2, viewModel.journeySession.value.consecutiveDeviationReadings)

    // 4. Reading 3 far away -> DEVIATED (Persistent deviation confirmed)
    viewModel.updateJourneyMetrics(deviatedLat, deviatedLng, speedMps = 1.4f, accuracyMeters = 5f)
    assertEquals(RouteDeviationState.DEVIATED, viewModel.journeySession.value.routeDeviationState)
    assertEquals(3, viewModel.journeySession.value.consecutiveDeviationReadings)

    // 5. User acknowledges "Yes, I'm safe"
    viewModel.acknowledgeDeviation()
    assertTrue(viewModel.journeySession.value.isDeviationAcknowledgedByUser)

    // 6. User returns to route -> NORMAL and reset acknowledgment
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 1.4f, accuracyMeters = 5f)
    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(0, viewModel.journeySession.value.consecutiveDeviationReadings)
    assertFalse(viewModel.journeySession.value.isDeviationAcknowledgedByUser)
  }

  @Test
  fun testPoorGpsAccuracyPreventsFalseDeviations() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    // Reading with poor accuracy (80 meters) should NOT escalate deviation count
    val deviatedLat = session.startLat + 0.05
    val deviatedLng = session.startLng + 0.05
    viewModel.updateJourneyMetrics(deviatedLat, deviatedLng, speedMps = 1.4f, accuracyMeters = 80f)

    assertEquals(RouteDeviationState.NORMAL, viewModel.journeySession.value.routeDeviationState)
    assertEquals(0, viewModel.journeySession.value.consecutiveDeviationReadings)
  }

  @Test
  fun testJourneyCompletionStopsDeviationMonitoring() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Dest", "")
    viewModel.completeJourney()

    val session = viewModel.journeySession.value
    assertFalse(session.isMonitoring)
    assertEquals(RouteDeviationState.NORMAL, session.routeDeviationState)
  }
}
