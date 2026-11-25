package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.firebase.model.FirestoreCommunityHazard
import com.example.firebase.repository.FirestoreRepository
import com.example.local.SafeCompanionDatabase
import com.example.local.entity.CommunityHazardEntity
import com.example.local.entity.SyncStatus
import com.example.local.repository.SafeCompanionLocalRepository
import com.example.model.HazardStatus
import com.example.model.HazardType
import com.example.sync.ConnectivityObserver
import com.example.sync.SafeCompanionSyncManager
import com.example.viewmodel.SafeCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import org.robolectric.annotation.Config
import java.io.IOException

private class CommunityTestConnectivityObserver(initialState: Boolean = true) : ConnectivityObserver {
  private val _isOnline = MutableStateFlow(initialState)
  override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
  override fun isConnected(): Boolean = _isOnline.value

  fun setOnline(online: Boolean) {
    _isOnline.value = online
  }
}

private class CommunityTestFirestoreRepository : FirestoreRepository(null) {
  var shouldFail: Boolean = false
  val reportedHazards = mutableMapOf<String, FirestoreCommunityHazard>()

  override suspend fun reportHazard(hazard: FirestoreCommunityHazard): Result<Unit> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    reportedHazards[hazard.hazardId] = hazard
    return Result.success(Unit)
  }

  override suspend fun getCommunityHazards(): Result<List<FirestoreCommunityHazard>> {
    if (shouldFail) return Result.failure(Exception("Simulated Firestore failure"))
    return Result.success(reportedHazards.values.toList())
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommunitySafetySyncTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private lateinit var db: SafeCompanionDatabase
  private lateinit var localRepository: SafeCompanionLocalRepository
  private lateinit var fakeFirestore: CommunityTestFirestoreRepository
  private lateinit var connectivityObserver: CommunityTestConnectivityObserver
  private lateinit var syncManager: SafeCompanionSyncManager

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SafeCompanionDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    localRepository = SafeCompanionLocalRepository(db)
    fakeFirestore = CommunityTestFirestoreRepository()
    connectivityObserver = CommunityTestConnectivityObserver(initialState = true)

    syncManager = SafeCompanionSyncManager(
      localRepository = localRepository,
      connectivityObserver = connectivityObserver,
      firestoreRepository = fakeFirestore,
      scope = testScope
    )
  }

  @After
  @Throws(IOException::class)
  fun tearDown() {
    db.close()
    Dispatchers.resetMain()
  }

  @Test
  fun test1_hazardReportedAndSavedLocally() = testScope.runTest {
    val viewModel = SafeCompanionViewModel(localRepo = localRepository, syncMgr = syncManager)
    advanceUntilIdle()

    viewModel.submitHazardReport(
      type = HazardType.POTHOLE,
      address = "Market St & 4th",
      description = "Deep dangerous pothole in right lane",
      customId = "hazard_c4_1"
    )
    advanceUntilIdle()

    val savedEntity = localRepository.getHazardById("hazard_c4_1")
    assertNotNull("Hazard entity should be persisted to Room", savedEntity)
    assertEquals("hazard_c4_1", savedEntity?.id)
    assertEquals("Pothole", savedEntity?.title)
    assertEquals("POTHOLE", savedEntity?.hazardType)
    assertEquals("Market St & 4th", savedEntity?.locationAddress)
    assertEquals("Deep dangerous pothole in right lane", savedEntity?.description)
  }

  @Test
  fun test2_hazardImmediatelyAvailableWhileOffline() = testScope.runTest {
    connectivityObserver.setOnline(false)
    val viewModel = SafeCompanionViewModel(localRepo = localRepository, syncMgr = syncManager)
    advanceUntilIdle()

    viewModel.submitHazardReport(
      type = HazardType.BROKEN_STREETLIGHT,
      address = "Mission St & 8th",
      description = "Streetlight completely dark",
      customId = "hazard_offline_immediate"
    )

    // User sees the report immediately without network
    val reports = viewModel.hazardReports.value
    val foundReport = reports.find { it.id == "hazard_offline_immediate" }
    assertNotNull("Hazard should be immediately present in UI state", foundReport)
    assertEquals("Broken Streetlight", foundReport?.title)
    assertEquals("hazard_offline_immediate", viewModel.selectedHazard.value?.id)

    // Room database also persists it
    advanceUntilIdle()
    val dbEntity = localRepository.getHazardById("hazard_offline_immediate")
    assertNotNull("Hazard must be in local Room database even offline", dbEntity)
  }

  @Test
  fun test3_newHazardStartsAsPending() = testScope.runTest {
    connectivityObserver.setOnline(false)
    val viewModel = SafeCompanionViewModel(localRepo = localRepository, syncMgr = syncManager)
    advanceUntilIdle()

    viewModel.submitHazardReport(
      type = HazardType.BLOCKED_ROAD,
      address = "Broadway & Pine",
      description = "Construction barrier blocking road",
      customId = "hazard_pending_check"
    )
    advanceUntilIdle()

    val entity = localRepository.getHazardById("hazard_pending_check")
    assertNotNull(entity)
    assertEquals(SyncStatus.PENDING, entity?.syncStatus)

    val pendingHazards = localRepository.getPendingHazards()
    assertTrue(pendingHazards.any { it.id == "hazard_pending_check" })
  }

  @Test
  fun test4_onlineHazardSynchronizesToFirebase() = testScope.runTest {
    connectivityObserver.setOnline(true)

    val hazard = CommunityHazardEntity(
      id = "hazard_online_sync",
      title = "Pothole",
      hazardType = "POTHOLE",
      locationAddress = "Folsom St",
      reportedTime = "Just now",
      status = "REPORTED",
      description = "Large road crater",
      latitude = 37.7790,
      longitude = -122.4080,
      syncStatus = SyncStatus.PENDING
    )
    localRepository.saveHazard(hazard)

    val syncSuccess = syncManager.syncHazard(hazard)
    assertTrue("syncHazard should return true online", syncSuccess)

    val firestoreRecord = fakeFirestore.reportedHazards["hazard_online_sync"]
    assertNotNull("Record should be uploaded to Firebase", firestoreRecord)
    assertEquals("hazard_online_sync", firestoreRecord?.hazardId)
    assertEquals("POTHOLE", firestoreRecord?.hazardType)
    assertEquals("Large road crater", firestoreRecord?.description)
    assertEquals(37.7790, firestoreRecord?.latitude ?: 0.0, 0.0001)
    assertEquals(-122.4080, firestoreRecord?.longitude ?: 0.0, 0.0001)
  }

  @Test
  fun test5_successfulSynchronizationChangesStatusToSynced() = testScope.runTest {
    connectivityObserver.setOnline(true)

    val hazard = CommunityHazardEntity(
      id = "hazard_status_transition",
      title = "Broken Streetlight",
      hazardType = "BROKEN_STREETLIGHT",
      locationAddress = "7th and Howard",
      reportedTime = "Just now",
      status = "REPORTED",
      description = "Light flickering dangerously",
      latitude = 37.7750,
      longitude = -122.4100,
      syncStatus = SyncStatus.PENDING
    )
    localRepository.saveHazard(hazard)

    syncManager.syncHazard(hazard)

    val updatedEntity = localRepository.getHazardById("hazard_status_transition")
    assertNotNull(updatedEntity)
    assertEquals(SyncStatus.SYNCED, updatedEntity?.syncStatus)
    assertTrue(localRepository.getPendingHazards().none { it.id == "hazard_status_transition" })
  }

  @Test
  fun test6_offlineHazardsSynchronizeAfterConnectivityReturns() = testScope.runTest {
    connectivityObserver.setOnline(false)

    val h1 = CommunityHazardEntity(
      id = "h_queue_1",
      title = "Pothole",
      hazardType = "POTHOLE",
      locationAddress = "1st St",
      reportedTime = "10m ago",
      status = "REPORTED",
      description = "Deep hole",
      latitude = 37.7749,
      longitude = -122.4194,
      syncStatus = SyncStatus.PENDING
    )
    val h2 = CommunityHazardEntity(
      id = "h_queue_2",
      title = "Blocked Road",
      hazardType = "BLOCKED_ROAD",
      locationAddress = "2nd St",
      reportedTime = "5m ago",
      status = "REPORTED",
      description = "Tree down",
      latitude = 37.7750,
      longitude = -122.4200,
      syncStatus = SyncStatus.PENDING
    )
    localRepository.saveHazards(listOf(h1, h2))

    assertEquals(2, localRepository.getPendingHazards().size)
    assertEquals(0, fakeFirestore.reportedHazards.size)

    // Connectivity returns
    connectivityObserver.setOnline(true)
    val summaryResult = syncManager.syncPendingData()
    assertTrue(summaryResult.isSuccess)

    // Both hazards now synced
    assertEquals(2, fakeFirestore.reportedHazards.size)
    assertNotNull(fakeFirestore.reportedHazards["h_queue_1"])
    assertNotNull(fakeFirestore.reportedHazards["h_queue_2"])

    assertEquals(SyncStatus.SYNCED, localRepository.getHazardById("h_queue_1")?.syncStatus)
    assertEquals(SyncStatus.SYNCED, localRepository.getHazardById("h_queue_2")?.syncStatus)
    assertEquals(0, localRepository.getPendingHazards().size)
  }

  @Test
  fun test7_repeatedSynchronizationDoesNotCreateDuplicateHazards() = testScope.runTest {
    connectivityObserver.setOnline(true)

    val h = CommunityHazardEntity(
      id = "h_idempotent",
      title = "Blocked Road",
      hazardType = "BLOCKED_ROAD",
      locationAddress = "Grove St",
      reportedTime = "Now",
      status = "REPORTED",
      description = "Accident blocking intersection",
      latitude = 37.7760,
      longitude = -122.4210,
      syncStatus = SyncStatus.PENDING
    )
    localRepository.saveHazard(h)

    // First sync
    syncManager.syncPendingData()
    assertEquals(1, fakeFirestore.reportedHazards.size)

    // Second sync: should be idempotent and not re-upload or duplicate
    syncManager.syncPendingData()
    assertEquals(1, fakeFirestore.reportedHazards.size)

    val allHazards = localRepository.getAllHazardsList()
    assertEquals(1, allHazards.filter { it.id == "h_idempotent" }.size)
  }

  @Test
  fun test8_retrievedFirebaseHazardsCachedLocally() = testScope.runTest {
    connectivityObserver.setOnline(true)

    // Simulate hazard reported by another user in Firebase
    val remoteHazard = FirestoreCommunityHazard(
      hazardId = "remote_h_999",
      hazardType = "UNSAFE_AREA",
      latitude = 37.7800,
      longitude = -122.4150,
      description = "Poorly lit construction zone at night",
      reportedBy = "user_remote_42",
      status = "REPORTED",
      title = "Unsafe Area",
      locationAddress = "Ellis & Taylor St",
      confirmationsCount = 4
    )
    fakeFirestore.reportedHazards[remoteHazard.hazardId] = remoteHazard

    // Local DB does not have it yet
    assertNull(localRepository.getHazardById("remote_h_999"))

    // Fetch and cache remote hazards
    val fetchResult = syncManager.fetchAndCacheRemoteHazards()
    assertTrue(fetchResult.isSuccess)
    assertEquals(1, fetchResult.getOrNull())

    // It is now cached in Room as SYNCED
    val cachedEntity = localRepository.getHazardById("remote_h_999")
    assertNotNull("Remote hazard must be cached in local Room DB", cachedEntity)
    assertEquals("remote_h_999", cachedEntity?.id)
    assertEquals("Unsafe Area", cachedEntity?.title)
    assertEquals("UNSAFE_AREA", cachedEntity?.hazardType)
    assertEquals("Ellis & Taylor St", cachedEntity?.locationAddress)
    assertEquals(4, cachedEntity?.confirmationsCount)
    assertEquals(SyncStatus.SYNCED, cachedEntity?.syncStatus)

    // Re-fetching should not duplicate the hazard
    val repeatFetch = syncManager.fetchAndCacheRemoteHazards()
    assertTrue(repeatFetch.isSuccess)
    val listAfterRepeat = localRepository.getAllHazardsList()
    assertEquals(1, listAfterRepeat.filter { it.id == "remote_h_999" }.size)
  }

  @Test
  fun test9_hazardVotingUpdatesRoomAndSyncs() = testScope.runTest {
    connectivityObserver.setOnline(true)
    val viewModel = SafeCompanionViewModel(localRepo = localRepository, syncMgr = syncManager)
    advanceUntilIdle()

    viewModel.submitHazardReport(
      type = HazardType.POTHOLE,
      address = "Hayes & Laguna",
      description = "Medium pothole",
      customId = "hazard_vote_test"
    )
    advanceUntilIdle()

    // Upvote/confirm hazard
    viewModel.voteHazardPresence("hazard_vote_test", true)
    advanceUntilIdle()

    val votedEntity = localRepository.getHazardById("hazard_vote_test")
    assertNotNull(votedEntity)
    assertTrue("Confirmations count should reflect vote", (votedEntity?.confirmationsCount ?: 0) >= 1)
    assertTrue("userVoted should be true", votedEntity?.userVoted == true)
  }
}
