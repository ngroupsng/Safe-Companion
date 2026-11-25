package com.example

import com.example.model.*
import com.example.util.EmergencyAudioPlayer
import com.example.util.EmergencyCommunicationHub
import com.example.util.EmergencySmsManager
import com.example.viewmodel.SafeCompanionViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyCommunicationTest {

  private lateinit var viewModel: SafeCompanionViewModel

  @Before
  fun setUp() {
    viewModel = SafeCompanionViewModel()
    viewModel.setNetworkConnectedForTesting(true)
  }

  @Test
  fun test1_sosCapturesLatestLocationAndTimestamp() = runTest {
    viewModel.startJourney("Start Location", "Destination Point", "Commute")

    val latestLat = 37.7891
    val latestLng = -122.4014
    val latestAccuracy = 6f
    viewModel.updateJourneyMetrics(
      currentLat = latestLat,
      currentLng = latestLng,
      speedMps = 1.4f,
      accuracyMeters = latestAccuracy
    )

    viewModel.triggerEmergency()

    val state = viewModel.emergencyState.value
    assertTrue("Emergency state must be triggered", state.isTriggered)
    assertEquals(latestLat, state.latitude, 0.0001)
    assertEquals(latestLng, state.longitude, 0.0001)
    assertEquals(6, state.accuracyMeters)
    assertTrue("Timestamp must be valid and recent", state.timestamp > 0L)
    assertTrue(state.isLocationAvailable)
  }

  @Test
  fun test2_correctJourneyContactsReceiveEmergencyEvent() = runTest {
    val contacts = viewModel.trustedContacts.value
    val contact1 = contacts[0]
    val contact2 = contacts[1]

    // Select only contact1 for this journey
    viewModel.toggleContactSelectionForJourney(contact2.id)
    viewModel.startJourney("Home", "Work", "")

    viewModel.triggerEmergency()

    val state = viewModel.emergencyState.value
    assertEquals(listOf(contact1.name), state.notifiedContacts)
    assertEquals(1, state.contactStatuses.size)
    assertEquals(contact1.id, state.contactStatuses[0].contactId)
  }

  @Test
  fun test3_contactsFromAnotherJourneyCannotReceiveEmergencyEvent() = runTest {
    val contacts = viewModel.trustedContacts.value
    val contact1 = contacts[0]
    val contact2 = contacts[1]

    // Journey A only includes contact1
    viewModel.toggleContactSelectionForJourney(contact2.id)
    viewModel.startJourney("Home", "Office", "")
    assertEquals(setOf(contact1.id), viewModel.journeySession.value.selectedContactIds)

    // Modify global settings to include contact2 only
    viewModel.toggleContactSelectionForJourney(contact1.id)
    viewModel.toggleContactSelectionForJourney(contact2.id)

    // Trigger SOS during active journey
    viewModel.triggerEmergency()

    val state = viewModel.emergencyState.value
    assertTrue(state.notifiedContacts.contains(contact1.name))
    assertFalse("Contact from another journey/global selection must not receive emergency", state.notifiedContacts.contains(contact2.name))
  }

  @Test
  fun test4_onlineEmergencyNotificationStateWorks() = runTest {
    viewModel.setNetworkConnectedForTesting(true)
    viewModel.startJourney("Home", "Office", "")

    viewModel.triggerEmergency()

    val state = viewModel.emergencyState.value
    assertEquals(CommunicationMethod.ONLINE_FCM, state.communicationMethod)
    assertTrue(state.internetAvailable)
    assertFalse(state.communicationFailed)
  }

  @Test
  fun test5_offlineSmsFallbackIsSelectedWhenNoInternet() = runTest {
    viewModel.setNetworkConnectedForTesting(false)
    viewModel.startJourney("Home", "Office", "")

    viewModel.triggerEmergency()

    val state = viewModel.emergencyState.value
    assertEquals(CommunicationMethod.SMS_FALLBACK, state.communicationMethod)
    assertFalse(state.internetAvailable)
    assertFalse(state.communicationFailed)
    assertTrue(state.contactStatuses.all { it.method == CommunicationMethod.SMS_FALLBACK })
  }

  @Test
  fun test6_smsContainsLocationAndTimestamp() {
    val payload = EmergencyPayload(
      emergencyId = "test-emergency-999",
      userName = "Alex Rivera",
      latitude = 37.77492,
      longitude = -122.41941,
      accuracyMeters = 8,
      timestamp = 1700000000000L,
      message = "Help! Need assistance.",
      journeyDestination = "Central Park"
    )

    val smsText = EmergencySmsManager.buildEmergencySms(payload)

    assertTrue(smsText.contains("[SAFE COMPANION EMERGENCY]"))
    assertTrue(smsText.contains("Alex Rivera"))
    assertTrue(smsText.contains("37.77492"))
    assertTrue(smsText.contains("-122.41941"))
    assertTrue(smsText.contains("±8m"))
    assertTrue(smsText.contains("maps.google.com/?q=37.77492,-122.41941"))
    assertTrue(smsText.contains("Central Park"))
    assertTrue(smsText.contains("test-emergency-999"))
  }

  @Test
  fun test7_emergencySmsParsingAndValidation() {
    val incomingText = """
      🚨 [SAFE COMPANION EMERGENCY]
      SOS from: Emma Watson
      Status: ACTIVE
      Location: 40.71280, -74.00600 (±5m)
      Maps: https://maps.google.com/?q=40.71280,-74.00600
      Time: Nov 14, 8:30 PM
      Destination: Broadway Theater
      ID: emergency-abc-123
    """.trimIndent()

    val alert = EmergencySmsManager.parseEmergencySms(incomingText, "+15551234567")

    assertNotNull("Must parse valid emergency SMS", alert)
    assertEquals("Emma Watson", alert!!.senderName)
    assertEquals("+15551234567", alert.senderPhone)
    assertEquals(40.71280, alert.latitude, 0.0001)
    assertEquals(-74.00600, alert.longitude, 0.0001)
    assertEquals(5, alert.accuracyMeters)
    assertEquals("Broadway Theater", alert.journeyDestination)
    assertEquals("emergency-abc-123", alert.emergencyId)
    assertTrue(alert.receivedViaSms)
    assertTrue(alert.isAlarmPlaying)

    // Irrelevant regular SMS must return null
    val regularSms = "Hey, are you free for dinner tonight?"
    assertNull(EmergencySmsManager.parseEmergencySms(regularSms, "+15551234567"))
  }

  @Test
  fun test8_voiceMessageIsOptional_andDoesNotBlockSos() = runTest {
    viewModel.startJourney("Start", "Dest", "")

    // SOS is triggered immediately without voice message
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)
    assertEquals(VoiceRecordingState.IDLE, viewModel.emergencyState.value.voiceMessage.state)

    // User subsequently records voice note
    viewModel.startVoiceRecording()
    assertEquals(VoiceRecordingState.RECORDING, viewModel.emergencyState.value.voiceMessage.state)
    assertTrue("SOS remains triggered while voice note records", viewModel.emergencyState.value.isTriggered)

    viewModel.stopVoiceRecording()
    // State transitions to UPLOADING then UPLOADED
    assertNotNull(viewModel.emergencyState.value.voiceMessage)
  }

  @Test
  fun test9_alarmStateActivatesOnTrustedContactEmergencyReception() {
    val incomingAlert = IncomingEmergencyAlert(
      emergencyId = "alert-100",
      senderName = "Marcus Ray",
      senderPhone = "+15559876543",
      relationship = "Partner",
      latitude = 34.0522,
      longitude = -118.2437,
      accuracyMeters = 4,
      timestamp = System.currentTimeMillis(),
      message = "Emergency alert received.",
      journeyDestination = "Downtown LA",
      isAlarmPlaying = true,
      isAcknowledged = false
    )

    // Dispatch to communication hub
    EmergencyCommunicationHub.onEmergencyAlertReceived(incomingAlert, null)

    assertEquals(Screen.TrustedContactEmergency.route, viewModel.currentScreen.value)
    assertNotNull(viewModel.incomingAlert.value)
    assertEquals("Marcus Ray", viewModel.incomingAlert.value?.senderName)
    assertTrue(viewModel.incomingAlert.value?.isAlarmPlaying == true)
    assertFalse(viewModel.incomingAlert.value?.isAcknowledged == true)
  }

  @Test
  fun test10_alarmCanBeAcknowledgedAndStopped() {
    val incomingAlert = IncomingEmergencyAlert(
      emergencyId = "alert-200",
      senderName = "Marcus Ray",
      latitude = 34.0522,
      longitude = -118.2437,
      accuracyMeters = 4,
      timestamp = System.currentTimeMillis(),
      message = "Emergency alert received.",
      isAlarmPlaying = true,
      isAcknowledged = false
    )

    viewModel.onIncomingEmergencyAlert(incomingAlert)
    assertTrue(viewModel.incomingAlert.value?.isAlarmPlaying == true)

    // Contact acknowledges alarm
    viewModel.acknowledgeAlarm()

    assertFalse("Siren/alarm must be stopped", viewModel.incomingAlert.value?.isAlarmPlaying == true)
    assertTrue("Alert must be marked acknowledged", viewModel.incomingAlert.value?.isAcknowledged == true)
  }

  @Test
  fun test11_communicationFailureDoesNotClearSos() = runTest {
    viewModel.startJourney("Start", "Dest", "")
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)

    // Trigger retry or failure state simulation
    viewModel.retryEmergencyDispatch()

    // SOS state must remain active even under network failure
    assertTrue("Communication failure must never deactivate SOS state", viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.EmergencyStatus.route, viewModel.currentScreen.value)
  }

  @Test
  fun test12_resolvingSosClearsEmergencyStateCorrectly() = runTest {
    viewModel.startJourney("Start", "Dest", "")
    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)

    viewModel.resolveEmergency()

    assertFalse("Emergency state must be cleared", viewModel.emergencyState.value.isTriggered)
    assertTrue(viewModel.emergencyState.value.isResolved)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
  }

  @Test
  fun test13_existingJourneyCompletionStillWorksAfterSosResolution() = runTest {
    viewModel.startJourney("Start", "Dest", "")
    val session = viewModel.journeySession.value

    viewModel.triggerEmergency()
    assertTrue(viewModel.emergencyState.value.isTriggered)

    viewModel.resolveEmergency()
    assertFalse(viewModel.emergencyState.value.isTriggered)
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isMonitoring)

    // Continue journey to arrival
    viewModel.updateJourneyMetrics(
      currentLat = session.destLat,
      currentLng = session.destLng,
      speedMps = 1.0f,
      accuracyMeters = 5f
    )

    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
    assertTrue(viewModel.journeySession.value.isDestinationReached)
    assertFalse(viewModel.journeySession.value.isMonitoring)
  }

  @Test
  fun test14_emergencyAudioPlayerSirenControls() {
    val audioPlayer = EmergencyAudioPlayer(null)
    assertFalse(audioPlayer.isSirenPlaying())

    audioPlayer.startSiren()
    assertTrue(audioPlayer.isSirenPlaying())

    audioPlayer.stopSiren()
    assertFalse(audioPlayer.isSirenPlaying())

    audioPlayer.playSosConfirmationBeep()
    audioPlayer.release()
  }
}
