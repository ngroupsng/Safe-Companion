package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.local.SafeCompanionDatabase
import com.example.local.repository.SafeCompanionLocalRepository
import com.example.model.*
import com.example.viewmodel.SafeCompanionViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafeCompanionRoomIntegrationTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var db: SafeCompanionDatabase
  private lateinit var repository: SafeCompanionLocalRepository
  private lateinit var context: Context

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, SafeCompanionDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = SafeCompanionLocalRepository(db)
  }

  @After
  @Throws(IOException::class)
  fun teardown() {
    db.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testStartJourney_persistsJourneyToRoom() = runTest {
    val viewModel = SafeCompanionViewModel(localRepo = repository)

    viewModel.startJourney(
      startLoc = "Downtown Station",
      dest = "Home",
      note = "Late night walk",
      startLat = 37.7749,
      startLng = -122.4194,
      destLat = 37.7850,
      destLng = -122.4050,
      contactIds = setOf("contact_1", "contact_2")
    )

    var journeys = repository.getAllJourneysList()
    var attempts = 0
    while (journeys.isEmpty() && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      journeys = repository.getAllJourneysList()
      attempts++
    }
    assertEquals(1, journeys.size)

    val savedJourney = journeys[0]
    assertEquals("Downtown Station", savedJourney.startLocation)
    assertEquals("Home", savedJourney.destination)
    assertEquals("Late night walk", savedJourney.note)
    assertEquals(37.7749, savedJourney.startLat, 0.001)
    assertEquals(-122.4194, savedJourney.startLng, 0.001)
    assertEquals(37.7850, savedJourney.destLat, 0.001)
    assertEquals(-122.4050, savedJourney.destLng, 0.001)
    assertEquals("ACTIVE", savedJourney.status)
    assertTrue(savedJourney.selectedContactIdsCsv.contains("contact_1"))
    assertTrue(savedJourney.selectedContactIdsCsv.contains("contact_2"))
    assertEquals(Screen.ActiveJourney.route, viewModel.currentScreen.value)
  }

  @Test
  fun testLocationUpdate_persistsBreadcrumbsToRoomDuringActiveJourney() = runTest {
    val viewModel = SafeCompanionViewModel(localRepo = repository)

    viewModel.startJourney(
      startLoc = "Office",
      dest = "Subway",
      note = "",
      startLat = 37.7700,
      startLng = -122.4100,
      destLat = 37.7900,
      destLng = -122.3900
    )
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    var journeys = repository.getAllJourneysList()
    var attempts = 0
    while (journeys.isEmpty() && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      journeys = repository.getAllJourneysList()
      attempts++
    }
    val savedJourney = journeys.first()
    val journeyId = savedJourney.journeyId

    // Simulate location updates
    val loc1 = android.location.Location("gps").apply {
      latitude = 37.7710
      longitude = -122.4110
      accuracy = 10f
      speed = 1.5f
    }
    viewModel.handleLocationUpdate(loc1)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    val loc2 = android.location.Location("gps").apply {
      latitude = 37.7720
      longitude = -122.4120
      accuracy = 8f
      speed = 1.4f
    }
    viewModel.handleLocationUpdate(loc2)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    var locations = repository.getLocationsForJourney(journeyId)
    attempts = 0
    while (locations.size < 2 && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      locations = repository.getLocationsForJourney(journeyId)
      attempts++
    }
    assertEquals("Locations in DB for $journeyId were: $locations", 2, locations.size)
    assertEquals(37.7710, locations[0].latitude, 0.0001)
    assertEquals(-122.4110, locations[0].longitude, 0.0001)
    assertEquals(10f, locations[0].accuracyMeters, 0.1f)

    assertEquals(37.7720, locations[1].latitude, 0.0001)
    assertEquals(-122.4120, locations[1].longitude, 0.0001)
  }

  @Test
  fun testCompleteJourney_updatesJourneyEntityInRoom() = runTest {
    val viewModel = SafeCompanionViewModel(localRepo = repository)

    viewModel.startJourney(
      startLoc = "Market",
      dest = "Home",
      note = "Walking home"
    )
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    var journeys = repository.getAllJourneysList()
    var attempts = 0
    while (journeys.isEmpty() && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      journeys = repository.getAllJourneysList()
      attempts++
    }
    val savedJourney = journeys.first()
    assertEquals("ACTIVE", savedJourney.status)
    assertFalse(savedJourney.isDestinationReached)

    viewModel.completeJourney()
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    val updated = repository.getJourneyById(savedJourney.journeyId)
    assertNotNull(updated)
    assertEquals("COMPLETED", updated?.status)
    assertTrue(updated?.isDestinationReached == true)
    assertTrue((updated?.endTimestamp ?: 0L) > 0L)
    assertEquals(Screen.JourneyCompleted.route, viewModel.currentScreen.value)
  }

  @Test
  fun testEmergencySos_persistsEmergencyEntityInRoomAndResolves() = runTest {
    val viewModel = SafeCompanionViewModel(localRepo = repository)

    viewModel.startJourney("Start Point", "Home", "")
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    viewModel.triggerEmergency()
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    var emergencies = repository.getAllEmergenciesList()
    var attempts = 0
    while (emergencies.isEmpty() && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      emergencies = repository.getAllEmergenciesList()
      attempts++
    }
    assertEquals(1, emergencies.size)

    val emergency = emergencies[0]
    assertEquals("ACTIVE", emergency.status)
    assertFalse(emergency.isResolved)
    assertEquals(Screen.EmergencyStatus.route, viewModel.currentScreen.value)

    // Now resolve emergency
    viewModel.cancelEmergency()
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    var resolved = repository.getEmergencyById(emergency.emergencyId)
    attempts = 0
    while (resolved?.isResolved != true && attempts < 20) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      resolved = repository.getEmergencyById(emergency.emergencyId)
      attempts++
    }
    assertNotNull(resolved)
    assertEquals("RESOLVED", resolved?.status)
    assertTrue(resolved?.isResolved == true)
  }
}
