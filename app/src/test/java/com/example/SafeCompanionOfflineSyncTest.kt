package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.model.*
import com.example.firebase.repository.FirestoreRepository
import com.example.local.SafeCompanionDatabase
import com.example.local.entity.EmergencyEntity
import com.example.local.entity.JourneyEntity
import com.example.local.entity.JourneyLocationEntity
import com.example.local.entity.SyncStatus
import com.example.local.repository.SafeCompanionLocalRepository
import com.example.model.*
import com.example.sync.ConnectivityObserver
import com.example.sync.SafeCompanionSyncManager
import com.example.viewmodel.SafeCompanionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Fake ConnectivityObserver for testing offline/online transitions.
 */
class FakeConnectivityObserver(initialOnline: Boolean = true) : ConnectivityObserver {
  private val _isOnline = MutableStateFlow(initialOnline)
  override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
  override fun isConnected(): Boolean = _isOnline.value

  fun setOnline(online: Boolean) {
    _isOnline.value = online
  }
}

/**
 * Fake FirestoreRepository for offline/online sync verification and error simulation.
 */
class FakeFirestoreRepository(var shouldFail: Boolean = false) : FirestoreRepository(null) {
  val createdJourneys = mutableListOf<FirestoreJourney>()
  val updatedJourneys = mutableMapOf<String, String>()
  val addedLocations = mutableListOf<Pair<String, FirestoreJourneyLocation>>()
  val createdEmergencies = mutableListOf<FirestoreEmergency>()
  val updatedEmergencies = mutableMapOf<String, String>()
  val savedContacts = mutableListOf<Pair<String, FirestoreTrustedContact>>()
  val reportedHazards = mutableListOf<FirestoreCommunityHazard>()

  override suspend fun createJourney(journey: FirestoreJourney): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    createdJourneys.add(journey)
    return Result.success(Unit)
  }

  override suspend fun updateJourneyStatus(
    journeyId: String,
    status: String,
    endTimestamp: Long?,
    finalDistanceKm: Double?,
    finalDurationMinutes: Int?
  ): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    updatedJourneys[journeyId] = status
    return Result.success(Unit)
  }

  override suspend fun addJourneyLocation(
    journeyId: String,
    location: FirestoreJourneyLocation
  ): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    addedLocations.add(journeyId to location)
    return Result.success(Unit)
  }

  override suspend fun createEmergency(emergency: FirestoreEmergency): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    createdEmergencies.add(emergency)
    return Result.success(Unit)
  }

  override suspend fun updateEmergencyStatus(
    emergencyId: String,
    status: String,
    voiceMessageUrl: String?
  ): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    updatedEmergencies[emergencyId] = status
    return Result.success(Unit)
  }

  override suspend fun saveTrustedContact(
    userId: String,
    contact: FirestoreTrustedContact
  ): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    savedContacts.add(userId to contact)
    return Result.success(Unit)
  }

  override suspend fun reportHazard(hazard: FirestoreCommunityHazard): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    reportedHazards.add(hazard)
    return Result.success(Unit)
  }

  override suspend fun getCommunityHazards(): Result<List<FirestoreCommunityHazard>> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    return Result.success(reportedHazards.toList())
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafeCompanionOfflineSyncTest {

  private val testDispatcher = Dispatchers.Unconfined
  private lateinit var database: SafeCompanionDatabase
  private lateinit var repository: SafeCompanionLocalRepository
  private lateinit var fakeFirestore: FakeFirestoreRepository
  private lateinit var fakeConnectivity: FakeConnectivityObserver
  private lateinit var syncManager: SafeCompanionSyncManager

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, SafeCompanionDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = SafeCompanionLocalRepository(database)
    fakeFirestore = FakeFirestoreRepository()
    fakeConnectivity = FakeConnectivityObserver(initialOnline = true)
    syncManager = SafeCompanionSyncManager(
      localRepository = repository,
      firestoreRepository = fakeFirestore,
      connectivityObserver = fakeConnectivity
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    syncManager.stopMonitoring()
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testOfflineRecording_journeyAndLocationsSavedLocallyAsPending() = runBlocking {
    fakeConnectivity.setOnline(false)

    val journeyId = "journey_offline_1"
    val journeyEntity = JourneyEntity(
      journeyId = journeyId,
      userId = "test_user",
      destination = "789 Pine St",
      startLocation = "123 Main St",
      startTimestamp = 1000L,
      status = "ACTIVE",
      isMonitoring = true,
      syncStatus = SyncStatus.PENDING
    )
    repository.saveJourney(journeyEntity)

    val loc1 = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 5.0f,
      speedKmh = 15.0,
      timestamp = 1001L,
      syncStatus = SyncStatus.PENDING
    )
    val loc2 = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7750,
      longitude = -122.4195,
      accuracyMeters = 4.5f,
      speedKmh = 16.0,
      timestamp = 1002L,
      syncStatus = SyncStatus.PENDING
    )
    val id1 = repository.saveLocation(loc1)
    val id2 = repository.saveLocation(loc2)

    assertTrue(id1 > 0)
    assertTrue(id2 > 0)

    // Verify stored in Room as PENDING
    val pendingJourneys = repository.getPendingJourneys()
    assertEquals(1, pendingJourneys.size)
    assertEquals(SyncStatus.PENDING, pendingJourneys[0].syncStatus)

    val pendingLocations = repository.getPendingLocations()
    assertEquals(2, pendingLocations.size)
    assertEquals(SyncStatus.PENDING, pendingLocations[0].syncStatus)
    assertEquals(SyncStatus.PENDING, pendingLocations[1].syncStatus)

    // Verify Firestore was NOT contacted while offline
    assertEquals(0, fakeFirestore.createdJourneys.size)
    assertEquals(0, fakeFirestore.addedLocations.size)
  }

  @Test
  fun testNetworkRestored_syncsPendingDataToFirestoreAndMarksSynced() = runBlocking {
    // 1. Save data while offline
    fakeConnectivity.setOnline(false)
    val journeyId = "journey_offline_2"
    repository.saveJourney(
      JourneyEntity(
        journeyId = journeyId,
        userId = "user_abc",
        destination = "Market St",
        startLocation = "Mission St",
        startTimestamp = 2000L,
        status = "ACTIVE",
        isMonitoring = true,
        syncStatus = SyncStatus.PENDING
      )
    )
    val loc = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.7833,
      longitude = -122.4167,
      accuracyMeters = 3.0f,
      timestamp = 2005L,
      syncStatus = SyncStatus.PENDING
    )
    repository.saveLocation(loc)

    // 2. Network returns
    fakeConnectivity.setOnline(true)

    // 3. Trigger sync
    val result = syncManager.syncPendingData()
    assertTrue(result.isSuccess)
    val summary = result.getOrThrow()
    assertEquals(1, summary.journeysSynced)
    assertEquals(1, summary.locationsSynced)
    assertEquals(0, summary.errorsCount)

    // 4. Verify Firestore documents written
    assertEquals(1, fakeFirestore.createdJourneys.size)
    assertEquals(journeyId, fakeFirestore.createdJourneys[0].journeyId)
    assertEquals(1, fakeFirestore.addedLocations.size)
    assertEquals(journeyId, fakeFirestore.addedLocations[0].first)

    // 5. Verify Room records updated to SYNCED
    val pendingJourneysAfter = repository.getPendingJourneys()
    assertEquals(0, pendingJourneysAfter.size)

    val pendingLocationsAfter = repository.getPendingLocations()
    assertEquals(0, pendingLocationsAfter.size)

    val savedJourney = repository.getJourneyById(journeyId)
    assertNotNull(savedJourney)
    assertEquals(SyncStatus.SYNCED, savedJourney!!.syncStatus)
  }

  @Test
  fun testNetworkFailure_recordsMarkedFailedAndRetriedSuccessfullyLater() = runBlocking {
    val journeyId = "journey_fail_test"
    repository.saveJourney(
      JourneyEntity(
        journeyId = journeyId,
        userId = "user_fail",
        destination = "Test Dest",
        startLocation = "Test Start",
        startTimestamp = 3000L,
        status = "ACTIVE",
        syncStatus = SyncStatus.PENDING
      )
    )

    // Simulate network error during Firebase write
    fakeFirestore.shouldFail = true
    fakeConnectivity.setOnline(true)

    val result = syncManager.syncPendingData()
    assertTrue(result.isSuccess)
    val summary = result.getOrThrow()
    assertEquals(0, summary.journeysSynced)
    assertTrue(summary.errorsCount > 0)

    // Verify Room record is marked FAILED (never SYNCED on error)
    val journeyAfterFailure = repository.getJourneyById(journeyId)
    assertNotNull(journeyAfterFailure)
    assertEquals(SyncStatus.FAILED, journeyAfterFailure!!.syncStatus)

    // Still in pending query because syncStatus != 'SYNCED'
    val pending = repository.getPendingJourneys()
    assertEquals(1, pending.size)

    // Now restore Firebase and retry sync
    fakeFirestore.shouldFail = false
    val retryResult = syncManager.syncPendingData()
    assertTrue(retryResult.isSuccess)
    assertEquals(1, retryResult.getOrThrow().journeysSynced)

    val journeyAfterSuccess = repository.getJourneyById(journeyId)
    assertNotNull(journeyAfterSuccess)
    assertEquals(SyncStatus.SYNCED, journeyAfterSuccess!!.syncStatus)
  }

  @Test
  fun testIdempotentWrites_retriesTargetSameFirestoreDocumentWithoutDuplicates() = runBlocking {
    val journeyId = "journey_idempotent"
    val journey = JourneyEntity(
      journeyId = journeyId,
      userId = "user_idempotent",
      destination = "Pier 39",
      startLocation = "Union Square",
      startTimestamp = 4000L,
      status = "ACTIVE",
      syncStatus = SyncStatus.PENDING
    )
    repository.saveJourney(journey)

    val loc = JourneyLocationEntity(
      journeyId = journeyId,
      latitude = 37.8080,
      longitude = -122.4177,
      accuracyMeters = 6.0f,
      timestamp = 4001L,
      syncStatus = SyncStatus.PENDING
    )
    val locId = repository.saveLocation(loc)
    val savedLoc = loc.copy(id = locId)

    // First sync
    syncManager.syncLocation(savedLoc)
    assertEquals(1, fakeFirestore.addedLocations.size)
    val locationId1 = fakeFirestore.addedLocations[0].second.locationId
    assertEquals("loc_$locId", locationId1)

    // Second sync of same record
    syncManager.syncLocation(savedLoc)
    assertEquals(2, fakeFirestore.addedLocations.size)
    val locationId2 = fakeFirestore.addedLocations[1].second.locationId
    // Document ID matches exactly: no random duplicate IDs
    assertEquals(locationId1, locationId2)
  }

  @Test
  fun testOfflineEmergency_activatesImmediatelyAndSyncsWhenOnline() = runBlocking {
    fakeConnectivity.setOnline(false)

    val emergencyId = "emergency_offline_sos"
    val emergency = EmergencyEntity(
      emergencyId = emergencyId,
      journeyId = "journey_123",
      userId = "user_sos",
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 5,
      timestamp = 5000L,
      status = "ACTIVE",
      isResolved = false,
      syncStatus = SyncStatus.PENDING
    )
    repository.saveEmergency(emergency)

    // Check saved in Room immediately
    val savedInRoom = repository.getEmergencyById(emergencyId)
    assertNotNull(savedInRoom)
    assertEquals("ACTIVE", savedInRoom!!.status)
    assertEquals(SyncStatus.PENDING, savedInRoom.syncStatus)
    assertEquals(0, fakeFirestore.createdEmergencies.size)

    // Come back online
    fakeConnectivity.setOnline(true)
    syncManager.syncPendingData()

    // Verified synced to Firestore
    assertEquals(1, fakeFirestore.createdEmergencies.size)
    assertEquals(emergencyId, fakeFirestore.createdEmergencies[0].emergencyId)

    val syncedInRoom = repository.getEmergencyById(emergencyId)
    assertNotNull(syncedInRoom)
    assertEquals(SyncStatus.SYNCED, syncedInRoom!!.syncStatus)
  }

  @Test
  fun testViewModelOfflineTracking_persistsBreadcrumbsAndMetricsSeamlessly() = runBlocking {
    fakeConnectivity.setOnline(false)
    val viewModel = SafeCompanionViewModel(
      localRepo = repository,
      syncMgr = syncManager
    )

    // Set permission and start journey
    viewModel.onLocationPermissionChanged(true, ApplicationProvider.getApplicationContext())
    viewModel.startJourney(
      dest = "Coit Tower",
      destLat = 37.8024,
      destLng = -122.4058,
      context = ApplicationProvider.getApplicationContext()
    )
    shadowOf(Looper.getMainLooper()).idle()

    // Feed location updates while offline
    val mockLocation1 = android.location.Location("gps").apply {
      latitude = 37.7900
      longitude = -122.4000
      speed = 1.4f
      accuracy = 4.0f
      time = 10000L
    }
    viewModel.handleLocationUpdate(mockLocation1)
    shadowOf(Looper.getMainLooper()).idle()

    val mockLocation2 = android.location.Location("gps").apply {
      latitude = 37.7910
      longitude = -122.4010
      speed = 1.5f
      accuracy = 3.5f
      time = 15000L
    }
    viewModel.handleLocationUpdate(mockLocation2)
    shadowOf(Looper.getMainLooper()).idle()

    // Verify journey state is active and tracking metrics update
    assertTrue(viewModel.journeySession.value.isMonitoring)
    assertTrue(viewModel.journeySession.value.currentSpeedKmh > 0.0)

    // Verify locations persisted in Room as PENDING
    var pendingLocs = repository.getPendingLocations()
    var retryCount = 0
    while (pendingLocs.size < 2 && retryCount < 30) {
      kotlinx.coroutines.delay(50)
      shadowOf(Looper.getMainLooper()).idle()
      pendingLocs = repository.getPendingLocations()
      retryCount++
    }
    assertTrue(pendingLocs.isNotEmpty())
    assertEquals(SyncStatus.PENDING, pendingLocs[0].syncStatus)

    // End journey
    viewModel.completeJourney()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.journeySession.value.isMonitoring)
    assertTrue(viewModel.journeySession.value.isDestinationReached)

    // Restore network and sync
    fakeConnectivity.setOnline(true)
    val syncResult = syncManager.syncPendingData()
    assertTrue(syncResult.isSuccess)
    assertTrue(syncResult.getOrThrow().locationsSynced >= 2)
  }
}
