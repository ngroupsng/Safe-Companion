package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.local.SafeCompanionDatabase
import com.example.local.entity.*
import com.example.local.mapper.ModelMappers.toDomainModel
import com.example.local.mapper.ModelMappers.toEntity
import com.example.model.JourneySession
import com.example.model.TrustedContact
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafeCompanionRoomDatabaseTest {

  private lateinit var context: Context
  private lateinit var db: SafeCompanionDatabase

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = SafeCompanionDatabase.buildInMemoryDatabase(context)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun test1_journey_canBeInsertedAndRetrieved(): Unit = runBlocking {
    val journeyDao = db.journeyDao()

    val journey = JourneyEntity(
      journeyId = "j_test_001",
      userId = "u_alice",
      startLocation = "Work Office",
      destination = "Home Sweet Home",
      note = "Late night walk",
      startLat = 37.7749,
      startLng = -122.4194,
      destLat = 37.7833,
      destLng = -122.4064,
      initialDistanceKm = 3.5,
      remainingKm = 3.5,
      etaMinutes = 20,
      progress = 0f,
      isDestinationReached = false,
      isMonitoring = true,
      status = "ACTIVE",
      startTimestamp = 1700000000000L,
      selectedContactIdsCsv = "c1,c2",
      syncStatus = SyncStatus.PENDING
    )

    journeyDao.insertJourney(journey)

    val retrieved = journeyDao.getJourneyById("j_test_001")
    assertNotNull(retrieved)
    assertEquals("j_test_001", retrieved?.journeyId)
    assertEquals("Work Office", retrieved?.startLocation)
    assertEquals("Home Sweet Home", retrieved?.destination)
    assertEquals("Late night walk", retrieved?.note)
    assertEquals(37.7749, retrieved?.startLat ?: 0.0, 0.0001)
    assertEquals(-122.4194, retrieved?.startLng ?: 0.0, 0.0001)
    assertEquals(3.5, retrieved?.initialDistanceKm ?: 0.0, 0.01)
    assertEquals(20, retrieved?.etaMinutes)
    assertTrue(retrieved?.isMonitoring == true)
    assertEquals("c1,c2", retrieved?.selectedContactIdsCsv)
    assertEquals(SyncStatus.PENDING, retrieved?.syncStatus)

    // Verify mapping back to domain model JourneySession
    val session = retrieved!!.toDomainModel()
    assertEquals("Work Office", session.startLocation)
    assertEquals("Home Sweet Home", session.destination)
    assertEquals(setOf("c1", "c2"), session.selectedContactIds)
  }

  @Test
  fun test2_journeyLocations_insertedAndRetrievedInTimestampOrder(): Unit = runBlocking {
    val journeyDao = db.journeyDao()
    val locationDao = db.journeyLocationDao()

    val journeyId = "j_test_track_01"
    journeyDao.insertJourney(
      JourneyEntity(
        journeyId = journeyId,
        startLocation = "Point A",
        destination = "Point B",
        startLat = 37.7749,
        startLng = -122.4194,
        destLat = 37.7800,
        destLng = -122.4100,
        initialDistanceKm = 1.0,
        remainingKm = 1.0,
        etaMinutes = 10,
        startTimestamp = 1700000000000L
      )
    )

    // Insert out of chronological order
    val loc3 = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7760,
      longitude = -122.4180,
      accuracyMeters = 4.0f,
      timestamp = 1700000030000L, // 30s
      speedKmh = 4.8
    )
    val loc1 = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 3.5f,
      timestamp = 1700000010000L, // 10s
      speedKmh = 0.0
    )
    val loc2 = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7755,
      longitude = -122.4188,
      accuracyMeters = 3.8f,
      timestamp = 1700000020000L, // 20s
      speedKmh = 5.1
    )

    locationDao.insertLocation(loc3)
    locationDao.insertLocation(loc1)
    locationDao.insertLocation(loc2)

    val locations = locationDao.getLocationsForJourney(journeyId)
    assertEquals(3, locations.size)

    // Assert strictly ordered by timestamp ascending
    assertEquals(1700000010000L, locations[0].timestamp)
    assertEquals(37.7749, locations[0].latitude, 0.0001)
    assertEquals(1700000020000L, locations[1].timestamp)
    assertEquals(37.7755, locations[1].latitude, 0.0001)
    assertEquals(1700000030000L, locations[2].timestamp)
    assertEquals(37.7760, locations[2].latitude, 0.0001)
  }

  @Test
  fun test3_emergencyRecords_storedAndRetrieved(): Unit = runBlocking {
    val emergencyDao = db.emergencyDao()

    val emergency = EmergencyEntity(
      emergencyId = "em_sos_999",
      journeyId = "j_test_001",
      userId = "u_alice",
      status = "ACTIVE",
      latitude = 37.7750,
      longitude = -122.4185,
      accuracyMeters = 3,
      timestamp = 1700000050000L,
      message = "EMERGENCY SOS TRIGGERED",
      locationAddress = "5th & Market St",
      notifiedContactsCsv = "Sarah Connor, John Connor",
      communicationMethod = "ONLINE_FCM",
      voiceMessageUrl = "https://safecompanion.storage/voice/999.m4a",
      localVoiceFilePath = "/data/user/0/emergency_voice.m4a",
      isResolved = false,
      syncStatus = SyncStatus.PENDING
    )

    emergencyDao.insertEmergency(emergency)

    val retrieved = emergencyDao.getEmergencyById("em_sos_999")
    assertNotNull(retrieved)
    assertEquals("em_sos_999", retrieved?.emergencyId)
    assertEquals("j_test_001", retrieved?.journeyId)
    assertEquals("ACTIVE", retrieved?.status)
    assertEquals(37.7750, retrieved?.latitude ?: 0.0, 0.0001)
    assertEquals(-122.4185, retrieved?.longitude ?: 0.0, 0.0001)
    assertEquals(3, retrieved?.accuracyMeters)
    assertEquals("5th & Market St", retrieved?.locationAddress)
    assertEquals("Sarah Connor, John Connor", retrieved?.notifiedContactsCsv)
    assertEquals("ONLINE_FCM", retrieved?.communicationMethod)
    assertEquals("https://safecompanion.storage/voice/999.m4a", retrieved?.voiceMessageUrl)
    assertFalse(retrieved?.isResolved ?: true)
    assertEquals(SyncStatus.PENDING, retrieved?.syncStatus)

    // Resolve emergency
    emergencyDao.resolveEmergency("em_sos_999")
    val resolved = emergencyDao.getEmergencyById("em_sos_999")
    assertTrue(resolved?.isResolved == true)
    assertEquals("RESOLVED", resolved?.status)
  }

  @Test
  fun test4_dataSurvivesReopeningRoomDatabase(): Unit = runBlocking {
    // Test persistence by writing to a file-backed Room database, closing it,
    // and reopening it to verify everything survived cleanly.
    val dbFile = File(context.filesDir, "test_persistent_reopen.db")
    if (dbFile.exists()) dbFile.delete()

    val fileDb1 = Room.databaseBuilder(
      context,
      SafeCompanionDatabase::class.java,
      dbFile.absolutePath
    ).allowMainThreadQueries().build()

    // 1. Write records across entities
    val journey = JourneyEntity(
      journeyId = "j_survive_1",
      startLocation = "Origin Station",
      destination = "Destination Hub",
      startLat = 37.77,
      startLng = -122.41,
      destLat = 37.78,
      destLng = -122.42,
      initialDistanceKm = 2.4,
      remainingKm = 0.0,
      etaMinutes = 0,
      status = "COMPLETED",
      startTimestamp = 1700000000000L,
      endTimestamp = 1700001200000L,
      totalKm = 2.5,
      durationMinutes = 20,
      selectedContactIdsCsv = "c_mother,c_friend"
    )
    fileDb1.journeyDao().insertJourney(journey)

    val location = JourneyLocationEntity(
      journeyId = "j_survive_1",
      latitude = 37.7712,
      longitude = -122.4123,
      accuracyMeters = 5.0f,
      speedKmh = 4.5,
      timestamp = 1700000600000L,
      address = "Midway Blvd"
    )
    fileDb1.journeyLocationDao().insertLocation(location)

    val emergency = EmergencyEntity(
      emergencyId = "em_survive_1",
      journeyId = "j_survive_1",
      status = "RESOLVED",
      latitude = 37.7712,
      longitude = -122.4123,
      accuracyMeters = 5,
      timestamp = 1700000600000L,
      message = "False alarm - safe",
      isResolved = true
    )
    fileDb1.emergencyDao().insertEmergency(emergency)

    val contact = TrustedContact(
      id = "c_mother",
      name = "Martha Wayne",
      relationship = "Mother",
      phone = "+1 (555) 019-2831",
      isPrimary = true
    ).toEntity()
    fileDb1.trustedContactDao().insertContact(contact)

    val hazard = CommunityHazardEntity(
      id = "h_survive_1",
      title = "Icy Walkway",
      hazardType = "OTHER",
      locationAddress = "Midway Blvd & 3rd",
      reportedTime = "10 mins ago",
      status = "REPORTED",
      description = "Slippery path due to freezing weather",
      latitude = 37.7715,
      longitude = -122.4120
    )
    fileDb1.communityHazardDao().insertHazard(hazard)

    // Close the first database handle
    fileDb1.close()

    // 2. Reopen the database from the same file
    val fileDb2 = Room.databaseBuilder(
      context,
      SafeCompanionDatabase::class.java,
      dbFile.absolutePath
    ).allowMainThreadQueries().build()

    // 3. Verify all data survived the close and reopen
    val loadedJourney = fileDb2.journeyDao().getJourneyById("j_survive_1")
    assertNotNull(loadedJourney)
    assertEquals("Origin Station", loadedJourney?.startLocation)
    assertEquals("Destination Hub", loadedJourney?.destination)
    assertEquals(20, loadedJourney?.durationMinutes)
    assertEquals(2.5, loadedJourney?.totalKm ?: 0.0, 0.01)
    assertEquals("c_mother,c_friend", loadedJourney?.selectedContactIdsCsv)

    val loadedLocations = fileDb2.journeyLocationDao().getLocationsForJourney("j_survive_1")
    assertEquals(1, loadedLocations.size)
    assertEquals(37.7712, loadedLocations[0].latitude, 0.0001)
    assertEquals("Midway Blvd", loadedLocations[0].address)

    val loadedEmergency = fileDb2.emergencyDao().getEmergencyById("em_survive_1")
    assertNotNull(loadedEmergency)
    assertEquals("False alarm - safe", loadedEmergency?.message)
    assertTrue(loadedEmergency?.isResolved == true)

    val loadedContact = fileDb2.trustedContactDao().getContactById("c_mother")
    assertNotNull(loadedContact)
    assertEquals("Martha Wayne", loadedContact?.name)
    assertEquals("Mother", loadedContact?.relationship)
    assertTrue(loadedContact?.isPrimary == true)

    val loadedHazard = fileDb2.communityHazardDao().getHazardById("h_survive_1")
    assertNotNull(loadedHazard)
    assertEquals("Icy Walkway", loadedHazard?.title)
    assertEquals("OTHER", loadedHazard?.hazardType)

    fileDb2.close()
    dbFile.delete()
    Unit
  }

  @Test
  fun test5_trustedContactAndHazardFlows_emitReactivity(): Unit = runBlocking {
    val contactDao = db.trustedContactDao()
    val hazardDao = db.communityHazardDao()

    val c1 = TrustedContactEntity(
      id = "c_001",
      name = "Alice",
      relationship = "Sister",
      phone = "+15551111",
      isPrimary = true
    )
    val c2 = TrustedContactEntity(
      id = "c_002",
      name = "Bob",
      relationship = "Friend",
      phone = "+15552222",
      isPrimary = false
    )

    contactDao.insertContacts(listOf(c1, c2))

    val contacts = contactDao.getAllContacts().first()
    assertEquals(2, contacts.size)
    assertEquals("Alice", contacts[0].name) // isPrimary first
    assertTrue(contacts[0].isPrimary)

    val h1 = CommunityHazardEntity(
      id = "h_001",
      title = "Pothole",
      hazardType = "POTHOLE",
      locationAddress = "Main St",
      reportedTime = "Now",
      status = "REPORTED",
      description = "Deep hole",
      latitude = 37.77,
      longitude = -122.41
    )
    hazardDao.insertHazard(h1)

    val hazards = hazardDao.getAllHazards().first()
    assertEquals(1, hazards.size)
    assertEquals("Pothole", hazards[0].title)
  }
}
