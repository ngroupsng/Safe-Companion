package com.example

import com.example.firebase.auth.AuthState
import com.example.firebase.auth.AuthUser
import com.example.firebase.model.*
import com.example.firebase.storage.FirebaseStorageHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseBackendFoundationTest {

  @Test
  fun test1_authState_transitionsAndProperties() {
    val unauth: Any = AuthState.Unauthenticated
    assertTrue(unauth is AuthState)

    val loading: Any = AuthState.Loading
    assertTrue(loading is AuthState)

    val authUser = AuthUser(
      uid = "usr_test_123",
      email = "user@safecompanion.app",
      displayName = "Safe Companion User",
      isAnonymous = false
    )
    val authenticated = AuthState.Authenticated(
      uid = authUser.uid,
      email = authUser.email,
      displayName = authUser.displayName,
      isAnonymous = authUser.isAnonymous
    )

    assertEquals("usr_test_123", authenticated.uid)
    assertEquals("user@safecompanion.app", authenticated.email)
    assertEquals("Safe Companion User", authenticated.displayName)
    assertFalse(authenticated.isAnonymous)

    val errorState = AuthState.Error("Network failure")
    assertEquals("Network failure", errorState.message)
  }

  @Test
  fun test2_firestoreUserModel_convertsToMapAccurately() {
    val user = FirestoreUser(
      userId = "u_999",
      displayName = "Jane Doe",
      email = "jane@example.com",
      phoneNumber = "+1 (555) 123-4567"
    )

    val map = user.toMap()
    assertEquals("u_999", map["userId"])
    assertEquals("Jane Doe", map["displayName"])
    assertEquals("jane@example.com", map["email"])
    assertEquals("+1 (555) 123-4567", map["phoneNumber"])
    assertNotNull(map["createdAt"])
    assertNotNull(map["updatedAt"])
  }

  @Test
  fun test3_firestoreTrustedContactModel_convertsToMapAccurately() {
    val contact = FirestoreTrustedContact(
      contactId = "c_001",
      name = "Sarah Connor",
      phoneNumber = "+1 (555) 999-8888",
      relationship = "Sister",
      isPrimary = true
    )

    val map = contact.toMap()
    assertEquals("c_001", map["contactId"])
    assertEquals("Sarah Connor", map["name"])
    assertEquals("+1 (555) 999-8888", map["phoneNumber"])
    assertEquals("Sister", map["relationship"])
    assertEquals(true, map["isPrimary"])
    assertNotNull(map["createdAt"])
    assertNotNull(map["updatedAt"])
  }

  @Test
  fun test4_firestoreJourneyAndLocationModel_convertsToMapAccurately() {
    val journey = FirestoreJourney(
      journeyId = "j_1001",
      userId = "u_999",
      destination = "Downtown Library",
      startLocation = "Home",
      startTimestamp = 1700000000000L,
      status = "ACTIVE",
      selectedContactIds = listOf("c_001", "c_002")
    )

    val jMap = journey.toMap()
    assertEquals("j_1001", jMap["journeyId"])
    assertEquals("u_999", jMap["userId"])
    assertEquals("Downtown Library", jMap["destination"])
    assertEquals("ACTIVE", jMap["status"])
    assertEquals(listOf("c_001", "c_002"), jMap["selectedContactIds"])

    val loc = FirestoreJourneyLocation(
      locationId = "loc_01",
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 4.5f,
      timestamp = 1700000010000L
    )
    val locMap = loc.toMap()
    assertEquals("loc_01", locMap["locationId"])
    assertEquals(37.7749, locMap["latitude"])
    assertEquals(-122.4194, locMap["longitude"])
    assertEquals(4.5f, locMap["accuracyMeters"])
    assertEquals(1700000010000L, locMap["timestamp"])
  }

  @Test
  fun test5_firestoreEmergencyModel_convertsToMapAccurately() {
    val emergency = FirestoreEmergency(
      emergencyId = "em_777",
      journeyId = "j_1001",
      userId = "u_999",
      status = "ACTIVE",
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 3.0f,
      selectedContactIds = listOf("c_001"),
      voiceMessageUrl = "https://safecompanion.storage/voice.m4a"
    )

    val emMap = emergency.toMap()
    assertEquals("em_777", emMap["emergencyId"])
    assertEquals("j_1001", emMap["journeyId"])
    assertEquals("u_999", emMap["userId"])
    assertEquals("ACTIVE", emMap["status"])
    assertEquals(37.7749, emMap["latitude"])
    assertEquals(-122.4194, emMap["longitude"])
    assertEquals(listOf("c_001"), emMap["selectedContactIds"])
    assertEquals("https://safecompanion.storage/voice.m4a", emMap["voiceMessageUrl"])
  }

  @Test
  fun test6_firestoreDeviceTokenModel_convertsToMapAccurately() {
    val device = FirestoreDeviceToken(
      deviceId = "device_pixel_8",
      fcmToken = "fcm_token_sample_abc123",
      platform = "ANDROID"
    )

    val devMap = device.toMap()
    assertEquals("device_pixel_8", devMap["deviceId"])
    assertEquals("fcm_token_sample_abc123", devMap["fcmToken"])
    assertEquals("ANDROID", devMap["platform"])
    assertNotNull(devMap["updatedAt"])
  }

  @Test
  fun test7_firestoreCommunityHazardModel_convertsToMapAccurately() {
    val hazard = FirestoreCommunityHazard(
      hazardId = "h_500",
      hazardType = "BROKEN_STREETLIGHT",
      latitude = 37.7800,
      longitude = -122.4100,
      description = "Light out at 5th and Market",
      reportedBy = "u_999",
      status = "REPORTED"
    )

    val hMap = hazard.toMap()
    assertEquals("h_500", hMap["hazardId"])
    assertEquals("BROKEN_STREETLIGHT", hMap["hazardType"])
    assertEquals(37.7800, hMap["latitude"])
    assertEquals("u_999", hMap["reportedBy"])
    assertEquals("REPORTED", hMap["status"])
  }

  @Test
  fun test8_storagePathGeneration_validatesAndBuildsConformingPaths() {
    val userId = "usr_abc"
    val emergencyId = "em_xyz"
    val filename = "voice_note_1.m4a"

    val path = FirebaseStorageHelper.buildEmergencyVoiceStoragePath(userId, emergencyId, filename)
    assertEquals("emergency_voice/usr_abc/em_xyz/voice_note_1.m4a", path)

    assertTrue(FirebaseStorageHelper.isValidEmergencyVoicePath(path))
    assertTrue(FirebaseStorageHelper.isValidEmergencyVoicePath(path, expectedUserId = "usr_abc"))
    assertFalse(FirebaseStorageHelper.isValidEmergencyVoicePath(path, expectedUserId = "different_user"))

    // Invalid path checks
    assertFalse(FirebaseStorageHelper.isValidEmergencyVoicePath("other_bucket/file.m4a"))
    assertFalse(FirebaseStorageHelper.isValidEmergencyVoicePath("emergency_voice/usr_abc/voice.m4a"))
  }
}
