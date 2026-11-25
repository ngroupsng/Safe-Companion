package com.example

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
class SosFlowTest {

  @Test
  fun test1_activeJourney_tapSos_showsConfirmationUI() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "Commute")

    // Initially in Active Journey
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isMonitoring)
    assertFalse(viewModel.emergencyState.value.isTriggered)

    // User taps SOS button on active journey
    viewModel.openSosConfirmation()

    // Confirmation screen is displayed; emergency is NOT yet activated
    assertEquals(Screen.SosConfirmation.route, viewModel.currentScreen.value)
    assertFalse("Tapping SOS must not immediately activate emergency", viewModel.emergencyState.value.isTriggered)
    assertTrue("Journey monitoring must remain active during confirmation dialog", viewModel.journeySession.value.isMonitoring)
  }

  @Test
  fun test2_cancelSos_returnsToActiveJourney_monitoringContinues() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start Point", "Office", "Commute")
    viewModel.openSosConfirmation()
    assertEquals(Screen.SosConfirmation.route, viewModel.currentScreen.value)

    // User cancels the SOS confirmation
    viewModel.cancelSosConfirmation()

    // Returned to Active Journey
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertFalse("Emergency must not be triggered after cancel", viewModel.emergencyState.value.isTriggered)
    assertTrue("Journey monitoring must continue", viewModel.journeySession.value.isMonitoring)

    // GPS tracking, route deviation and long stop monitoring continue normally
    val session = viewModel.journeySession.value
    viewModel.updateJourneyMetrics(
      currentLat = session.startLat + 0.001,
      currentLng = session.startLng + 0.001,
      speedMps = 1.4f,
      accuracyMeters = 5f
    )
    assertTrue("Remaining distance must update", viewModel.journeySession.value.remainingKm < session.initialDistanceKm)
  }

  @Test
  fun test3_confirmSos_showsSosActivatedUI() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Home", "Gym", "")
    viewModel.openSosConfirmation()

    // User explicitly confirms sending SOS
    viewModel.triggerEmergency()

    // Transitions to SOS Activated / Emergency Status screen
    assertEquals(Screen.EmergencyStatus.route, viewModel.currentScreen.value)
    assertTrue("Emergency state must be active/triggered", viewModel.emergencyState.value.isTriggered)
  }

  @Test
  fun test4_sosActivation_capturesLatestGpsLocation() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Home", "Gym", "")

    // Simulate location updates during the journey
    val expectedLat = 37.7812
    val expectedLng = -122.4110
    val expectedAccuracy = 8f
    viewModel.updateJourneyMetrics(
      currentLat = expectedLat,
      currentLng = expectedLng,
      speedMps = 1.3f,
      accuracyMeters = expectedAccuracy
    )

    // Trigger emergency
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()

    // Verify latest GPS coordinates and accuracy are captured in the emergency state
    assertEquals(expectedLat, viewModel.emergencyState.value.latitude, 0.0001)
    assertEquals(expectedLng, viewModel.emergencyState.value.longitude, 0.0001)
    assertEquals(expectedAccuracy.toInt(), viewModel.emergencyState.value.accuracyMeters)
    assertTrue(viewModel.emergencyState.value.timestamp > 0L)
  }

  @Test
  fun test5_sosActivation_journeyMonitoringRemainsActive() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Home", "Downtown", "Evening walk")
    val initialDistance = viewModel.journeySession.value.remainingKm

    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()

    // Journey monitoring must NOT terminate on SOS activation
    assertTrue("Journey monitoring must stay active", viewModel.journeySession.value.isMonitoring)
    assertFalse("Destination must not be marked reached", viewModel.journeySession.value.isDestinationReached)
    assertEquals("Home", viewModel.journeySession.value.startLocation)
    assertEquals("Downtown", viewModel.journeySession.value.destination)
    assertEquals(initialDistance, viewModel.journeySession.value.remainingKm, 0.01)
    assertTrue(viewModel.journeySession.value.emergencyAlertsCount >= 1)
  }

  @Test
  fun test6_routeDeviation_doesNotActivateSos() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Destination", "")
    val session = viewModel.journeySession.value

    // Simulate 3 consecutive deviation readings far off path
    val offPathLat = session.startLat + 0.05
    val offPathLng = session.startLng + 0.05
    repeat(3) {
      viewModel.updateJourneyMetrics(offPathLat, offPathLng, speedMps = 1.4f, accuracyMeters = 5f)
    }

    // Deviation warning is active, but SOS must NEVER be activated automatically
    assertEquals(RouteDeviationState.DEVIATED, viewModel.journeySession.value.routeDeviationState)
    assertFalse("Route deviation must never automatically activate SOS", viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
  }

  @Test
  fun test7_longStop_doesNotActivateSos() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Destination", "")
    val session = viewModel.journeySession.value

    // Simulate stationary for 5+ minutes
    val baseTime = 10000000L
    val fiveMinLater = baseTime + (5 * 60 * 1000L)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = baseTime)
    viewModel.updateJourneyMetrics(session.startLat, session.startLng, speedMps = 0.0f, accuracyMeters = 5f, currentTimeMillis = fiveMinLater)

    // Long stop warning is active, but SOS must NEVER be activated automatically
    assertEquals(LongStopState.LONG_STOP_WARNING, viewModel.journeySession.value.longStopState)
    assertFalse("Long stop must never automatically activate SOS", viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
  }

  @Test
  fun test8_resolveOrCancelSos_clearsEmergencyStateCorrectly() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Destination", "")

    // Confirm and activate SOS
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.EmergencyStatus.route, viewModel.currentScreen.value)

    // User cancels/resolves SOS (I am safe)
    viewModel.cancelEmergency()

    // Emergency state must be cleared
    assertFalse("Emergency state must be cleared", viewModel.emergencyState.value.isTriggered)
    // Returns user to the active journey
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertTrue("Journey monitoring must still be active", viewModel.journeySession.value.isMonitoring)
  }

  @Test
  fun test9_completeJourneyAfterSosResolved_normalCompletedFlowWorks() {
    val viewModel = SafeCompanionViewModel()
    viewModel.startJourney("Start", "Destination", "")
    val session = viewModel.journeySession.value

    // Activate and resolve SOS during journey
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)
    viewModel.resolveEmergency()
    assertFalse(viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)

    // User now arrives at destination
    viewModel.updateJourneyMetrics(
      currentLat = session.destLat,
      currentLng = session.destLng,
      speedMps = 1.0f,
      accuracyMeters = 5f
    )

    // Normal Journey Completed screen is displayed
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isDestinationReached)
    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertEquals(0.0, viewModel.journeySession.value.remainingKm, 0.01)
    assertEquals(1.0f, viewModel.journeySession.value.progress, 0.01f)
  }

  @Test
  fun test10_trustedContacts_selectedBeforeJourney_carriedIntoActiveJourney_and_retrievedInSos() {
    val viewModel = SafeCompanionViewModel()
    val allContacts = viewModel.trustedContacts.value
    assertTrue("At least 2 contacts exist in default list", allContacts.size >= 2)

    val firstContact = allContacts[0]
    val secondContact = allContacts[1]

    // Set selection before starting journey: toggle off second contact
    viewModel.toggleContactSelectionForJourney(secondContact.id)
    assertEquals(setOf(firstContact.id), viewModel.selectedSharingContacts.value)

    // Start journey
    viewModel.startJourney("Start", "Destination", "Note")

    // Verify session carries the selected contacts
    assertEquals(setOf(firstContact.id), viewModel.journeySession.value.selectedContactIds)

    // User triggers SOS during active journey
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()

    // Emergency alert state should only retrieve the contacts selected for this journey
    assertTrue(viewModel.emergencyState.value.isTriggered)
    assertEquals(listOf(firstContact.name), viewModel.emergencyState.value.notifiedContacts)
  }

  @Test
  fun test11_changingGlobalContactsAfterJourneyStart_doesNotAffectActiveJourneyOrSos() {
    val viewModel = SafeCompanionViewModel()
    val allContacts = viewModel.trustedContacts.value
    val contact1 = allContacts[0]
    val contact2 = allContacts[1]

    // Initially select only contact1
    viewModel.toggleContactSelectionForJourney(contact2.id)
    assertEquals(setOf(contact1.id), viewModel.selectedSharingContacts.value)

    // Start journey
    viewModel.startJourney("Home", "Office", "")
    assertEquals(setOf(contact1.id), viewModel.journeySession.value.selectedContactIds)

    // Now change the global contact selection while journey is ongoing (e.g. from background or settings)
    viewModel.toggleContactSelectionForJourney(contact2.id)
    viewModel.toggleContactSelectionForJourney(contact1.id)
    assertEquals(setOf(contact2.id), viewModel.selectedSharingContacts.value)

    // Verify active journey's selectedContactIds remained isolated
    assertEquals(setOf(contact1.id), viewModel.journeySession.value.selectedContactIds)

    // Trigger SOS from active journey
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()

    // Notified contacts in SOS alert MUST be from the active journey's isolated contacts (contact1), NOT the modified global list (contact2)
    assertEquals(listOf(contact1.name), viewModel.emergencyState.value.notifiedContacts)
  }

  @Test
  fun test12_addNewTrustedContact_and_selectForJourney() {
    val viewModel = SafeCompanionViewModel()
    val initialCount = viewModel.trustedContacts.value.size

    // Add a new trusted contact
    viewModel.addTrustedContact(
      name = "Officer Davis",
      relationship = "Campus Security",
      phone = "+1 (555) 999-0000",
      isPrimary = false
    )

    assertEquals(initialCount + 1, viewModel.trustedContacts.value.size)
    val newContact = viewModel.trustedContacts.value.find { it.name == "Officer Davis" }
    assertNotNull(newContact)

    // When newly added, it is auto-selected in sharing contacts
    assertTrue(viewModel.selectedSharingContacts.value.contains(newContact!!.id))

    // Start journey with this contact included
    viewModel.startJourney("Campus Library", "Dormitory", "Late walk")
    assertTrue(viewModel.journeySession.value.selectedContactIds.contains(newContact.id))

    // Trigger SOS
    viewModel.openSosConfirmation()
    viewModel.triggerEmergency()

    // Alert notified contacts includes newly created contact
    assertTrue(viewModel.emergencyState.value.notifiedContacts.contains("Officer Davis"))
  }

  @Test
  fun test13_sosConfirmation_dispatchesAlertToConfiguredJourneyContacts() {
    val viewModel = SafeCompanionViewModel()
    val allContacts = viewModel.trustedContacts.value
    assertTrue("At least 2 contacts exist in default list", allContacts.size >= 2)

    val chosenContact = allContacts[0]
    val unselectedContact = allContacts[1]

    // Configure journey with only chosenContact
    viewModel.toggleContactSelectionForJourney(unselectedContact.id)
    viewModel.startJourney(
      startLoc = "Metro Station",
      dest = "Home",
      note = "Night commute",
      startLat = 37.7749,
      startLng = -122.4194,
      contactIds = setOf(chosenContact.id)
    )

    assertEquals(setOf(chosenContact.id), viewModel.journeySession.value.selectedContactIds)

    // User taps SOS on map -> opens confirmation screen
    viewModel.openSosConfirmation()
    assertEquals(Screen.SosConfirmation.route, viewModel.currentScreen.value)
    assertFalse(viewModel.emergencyState.value.isTriggered)

    // If user cannot click (e.g. emergency incapacitation), time limit expires and triggerEmergency executes
    viewModel.triggerEmergency()

    // Verified: Emergency is active and dispatched to the exact contact kept in the journey session configuration
    val emergencyState = viewModel.emergencyState.value
    assertTrue(emergencyState.isTriggered)
    assertEquals(listOf(chosenContact.name), emergencyState.notifiedContacts)
    assertFalse(emergencyState.notifiedContacts.contains(unselectedContact.name))
    assertEquals(Screen.EmergencyStatus.route, viewModel.currentScreen.value)
  }
}
