package com.example.sync

import android.util.Log
import com.example.firebase.repository.FirestoreRepository
import com.example.local.entity.EmergencyEntity
import com.example.local.entity.JourneyEntity
import com.example.local.entity.JourneyLocationEntity
import com.example.local.entity.SyncStatus
import com.example.local.entity.TrustedContactEntity
import com.example.local.entity.CommunityHazardEntity
import com.example.local.mapper.ModelMappers.toEntity
import com.example.local.mapper.ModelMappers.toFirestoreEmergency
import com.example.local.mapper.ModelMappers.toFirestoreHazard
import com.example.local.mapper.ModelMappers.toFirestoreJourney
import com.example.local.mapper.ModelMappers.toFirestoreLocation
import com.example.local.mapper.ModelMappers.toFirestoreTrustedContact
import com.example.local.repository.SafeCompanionLocalRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Summary of sync operation execution.
 */
data class SyncSummary(
  val journeysSynced: Int = 0,
  val locationsSynced: Int = 0,
  val emergenciesSynced: Int = 0,
  val contactsSynced: Int = 0,
  val hazardsSynced: Int = 0,
  val errorsCount: Int = 0
)

/**
 * Clean synchronization orchestrator for Safe Companion.
 * Responsible for synchronizing Room-backed pending records to Firebase Firestore.
 * Room remains the primary source of truth.
 *
 * Responsibilities:
 * - Room: Local persistence
 * - Sync Layer: Pending record processing, retry logic, duplicate prevention
 * - FirestoreRepository: Firebase operations
 */
class SafeCompanionSyncManager(
  private val localRepository: SafeCompanionLocalRepository,
  private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
  private val connectivityObserver: ConnectivityObserver? = null,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

  private val syncMutex = Mutex()
  private var monitoringJob: Job? = null

  /**
   * Starts observing network connectivity.
   * When connectivity transitions to online, automatically triggers sync of pending/failed records.
   */
  fun startMonitoring() {
    if (connectivityObserver == null) return
    monitoringJob?.cancel()
    monitoringJob = scope.launch {
      connectivityObserver.isOnline.collectLatest { isOnline ->
        if (isOnline) {
          Log.i("SyncManager", "Network connection restored. Triggering pending data sync.")
          syncPendingData()
        }
      }
    }
  }

  fun stopMonitoring() {
    monitoringJob?.cancel()
    monitoringJob = null
  }

  fun isConnected(): Boolean {
    return connectivityObserver?.isConnected() ?: true
  }

  /**
   * Synchronizes all pending/failed records from Room to Firebase.
   * Runs asynchronously, thread-safe with Mutex to prevent duplicate sync passes.
   */
  suspend fun syncPendingData(): Result<SyncSummary> = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      Log.d("SyncManager", "Network offline; skipping remote synchronization.")
      return@withContext Result.success(SyncSummary())
    }

    syncMutex.withLock {
      var jCount = 0
      var lCount = 0
      var eCount = 0
      var cCount = 0
      var hCount = 0
      var errCount = 0

      // 1. Sync Journeys
      try {
        val pendingJourneys = localRepository.getPendingJourneys()
        for (journey in pendingJourneys) {
          val success = syncJourneyInternal(journey)
          if (success) jCount++ else errCount++
        }
      } catch (e: Throwable) {
        Log.e("SyncManager", "Error querying pending journeys: ${e.message}")
        errCount++
      }

      // 2. Sync Journey Locations (ordered chronologically)
      try {
        val pendingLocations = localRepository.getPendingLocations()
        for (location in pendingLocations) {
          val success = syncLocationInternal(location)
          if (success) lCount++ else errCount++
        }
      } catch (e: Throwable) {
        Log.e("SyncManager", "Error querying pending locations: ${e.message}")
        errCount++
      }

      // 3. Sync Emergencies
      try {
        val pendingEmergencies = localRepository.getPendingEmergencies()
        for (emergency in pendingEmergencies) {
          val success = syncEmergencyInternal(emergency)
          if (success) eCount++ else errCount++
        }
      } catch (e: Throwable) {
        Log.e("SyncManager", "Error querying pending emergencies: ${e.message}")
        errCount++
      }

      // 4. Sync Trusted Contacts
      try {
        val pendingContacts = localRepository.getPendingContacts()
        for (contact in pendingContacts) {
          val success = syncContactInternal(contact)
          if (success) cCount++ else errCount++
        }
      } catch (e: Throwable) {
        Log.e("SyncManager", "Error querying pending contacts: ${e.message}")
        errCount++
      }

      // 5. Sync Hazards
      try {
        val pendingHazards = localRepository.getPendingHazards()
        for (hazard in pendingHazards) {
          val success = syncHazardInternal(hazard)
          if (success) hCount++ else errCount++
        }
      } catch (e: Throwable) {
        Log.e("SyncManager", "Error querying pending hazards: ${e.message}")
        errCount++
      }

      // 6. Download and cache remote hazards from Firebase
      try {
        fetchAndCacheRemoteHazards()
      } catch (e: Throwable) {
        Log.w("SyncManager", "Could not fetch remote hazards during sync: ${e.message}")
      }

      val summary = SyncSummary(
        journeysSynced = jCount,
        locationsSynced = lCount,
        emergenciesSynced = eCount,
        contactsSynced = cCount,
        hazardsSynced = hCount,
        errorsCount = errCount
      )
      Result.success(summary)
    }
  }

  // --- Asynchronous triggers for individual records ---

  fun syncJourneyAsync(journeyId: String) {
    scope.launch {
      val journey = localRepository.getJourneyById(journeyId) ?: return@launch
      syncJourney(journey)
    }
  }

  fun syncLocationAsync(location: JourneyLocationEntity) {
    scope.launch {
      syncLocation(location)
    }
  }

  fun syncEmergencyAsync(emergencyId: String) {
    scope.launch {
      val emergency = localRepository.getEmergencyById(emergencyId) ?: return@launch
      syncEmergency(emergency)
    }
  }

  fun syncHazardAsync(hazardId: String) {
    scope.launch {
      val hazard = localRepository.getHazardById(hazardId) ?: return@launch
      syncHazard(hazard)
    }
  }

  // --- Individual Sync Operations with Idempotent Writes ---

  suspend fun syncJourney(journey: JourneyEntity): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext false
    }
    syncJourneyInternal(journey)
  }

  private suspend fun syncJourneyInternal(journey: JourneyEntity): Boolean {
    // Transition to SYNCING in Room
    localRepository.updateJourneySyncStatus(journey.journeyId, SyncStatus.SYNCING)

    val firestoreModel = journey.toFirestoreJourney()
    val result = firestoreRepository.createJourney(firestoreModel)

    return if (result.isSuccess) {
      localRepository.updateJourneySyncStatus(journey.journeyId, SyncStatus.SYNCED)
      true
    } else {
      Log.w("SyncManager", "Failed syncing journey ${journey.journeyId}: ${result.exceptionOrNull()?.message}")
      localRepository.updateJourneySyncStatus(journey.journeyId, SyncStatus.FAILED)
      false
    }
  }

  suspend fun syncLocation(location: JourneyLocationEntity): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext false
    }
    syncLocationInternal(location)
  }

  private suspend fun syncLocationInternal(location: JourneyLocationEntity): Boolean {
    // Transition to SYNCING in Room
    localRepository.updateLocationSyncStatus(location.id, SyncStatus.SYNCING)

    val firestoreModel = location.toFirestoreLocation()
    val result = firestoreRepository.addJourneyLocation(location.journeyId, firestoreModel)

    return if (result.isSuccess) {
      localRepository.updateLocationSyncStatus(location.id, SyncStatus.SYNCED)
      true
    } else {
      Log.w("SyncManager", "Failed syncing location ${location.id}: ${result.exceptionOrNull()?.message}")
      localRepository.updateLocationSyncStatus(location.id, SyncStatus.FAILED)
      false
    }
  }

  suspend fun syncEmergency(emergency: EmergencyEntity): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext false
    }
    syncEmergencyInternal(emergency)
  }

  private suspend fun syncEmergencyInternal(emergency: EmergencyEntity): Boolean {
    // Transition to SYNCING in Room
    localRepository.updateEmergencySyncStatus(emergency.emergencyId, SyncStatus.SYNCING)

    var updatedEmergency = emergency
    // If local voice recording exists but has not yet been uploaded to cloud storage, upload now
    if (!emergency.localVoiceFilePath.isNullOrBlank() && emergency.voiceMessageUrl.isNullOrBlank()) {
      try {
        val voiceManager = com.example.util.VoiceMessageManager()
        val uploaded = voiceManager.uploadVoiceMessage(
          com.example.model.VoiceMessageData(
            localFilePath = emergency.localVoiceFilePath,
            transcription = emergency.voiceTranscription,
            state = com.example.model.VoiceRecordingState.RECORDED
          )
        )
        if (uploaded.audioUrl != null) {
          localRepository.updateVoiceMessage(
            emergencyId = emergency.emergencyId,
            voiceUrl = uploaded.audioUrl,
            localPath = emergency.localVoiceFilePath,
            transcription = emergency.voiceTranscription
          )
          updatedEmergency = emergency.copy(voiceMessageUrl = uploaded.audioUrl)
        }
      } catch (e: Throwable) {
        Log.w("SyncManager", "Deferred voice note upload failed during emergency sync: ${e.message}")
      }
    }

    val firestoreModel = updatedEmergency.toFirestoreEmergency()
    val result = firestoreRepository.createEmergency(firestoreModel)

    return if (result.isSuccess) {
      localRepository.updateEmergencySyncStatus(emergency.emergencyId, SyncStatus.SYNCED)
      true
    } else {
      Log.w("SyncManager", "Failed syncing emergency ${emergency.emergencyId}: ${result.exceptionOrNull()?.message}")
      localRepository.updateEmergencySyncStatus(emergency.emergencyId, SyncStatus.FAILED)
      false
    }
  }

  suspend fun syncContact(contact: TrustedContactEntity): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext false
    }
    syncContactInternal(contact)
  }

  private suspend fun syncContactInternal(contact: TrustedContactEntity): Boolean {
    localRepository.updateContactSyncStatus(contact.id, SyncStatus.SYNCING)

    val firestoreModel = contact.toFirestoreTrustedContact()
    val result = firestoreRepository.saveTrustedContact("user_default", firestoreModel)

    return if (result.isSuccess) {
      localRepository.updateContactSyncStatus(contact.id, SyncStatus.SYNCED)
      true
    } else {
      localRepository.updateContactSyncStatus(contact.id, SyncStatus.FAILED)
      false
    }
  }

  suspend fun syncHazard(hazard: CommunityHazardEntity): Boolean = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext false
    }
    syncHazardInternal(hazard)
  }

  private suspend fun syncHazardInternal(hazard: CommunityHazardEntity): Boolean {
    localRepository.updateHazardSyncStatus(hazard.id, SyncStatus.SYNCING)

    val firestoreModel = hazard.toFirestoreHazard()
    val result = firestoreRepository.reportHazard(firestoreModel)

    return if (result.isSuccess) {
      localRepository.updateHazardSyncStatus(hazard.id, SyncStatus.SYNCED)
      true
    } else {
      localRepository.updateHazardSyncStatus(hazard.id, SyncStatus.FAILED)
      false
    }
  }

  suspend fun fetchAndCacheRemoteHazards(): Result<Int> = withContext(Dispatchers.IO) {
    if (!isConnected()) {
      return@withContext Result.failure(IllegalStateException("Cannot fetch hazards while offline"))
    }
    try {
      val remoteResult = firestoreRepository.getCommunityHazards()
      if (remoteResult.isFailure) {
        return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to fetch remote hazards"))
      }
      val remoteHazards = remoteResult.getOrNull() ?: emptyList()
      var cachedCount = 0

      for (remote in remoteHazards) {
        if (remote.hazardId.isBlank()) continue

        val existing = localRepository.getHazardById(remote.hazardId)
        if (existing == null) {
          // New hazard from remote, cache it locally as SYNCED
          localRepository.saveHazard(remote.toEntity(syncStatus = SyncStatus.SYNCED))
          cachedCount++
        } else {
          // Existing hazard. Avoid overwriting local user changes that are PENDING or SYNCING
          if (existing.syncStatus == SyncStatus.SYNCED) {
            val updated = existing.copy(
              title = if (remote.title.isNotBlank()) remote.title else existing.title,
              hazardType = remote.hazardType,
              locationAddress = if (remote.locationAddress.isNotBlank()) remote.locationAddress else existing.locationAddress,
              description = if (remote.description.isNotBlank()) remote.description else existing.description,
              status = remote.status,
              confirmationsCount = if (remote.confirmationsCount > 0) remote.confirmationsCount else existing.confirmationsCount,
              latitude = if (remote.latitude != 0.0) remote.latitude else existing.latitude,
              longitude = if (remote.longitude != 0.0) remote.longitude else existing.longitude,
              syncStatus = SyncStatus.SYNCED,
              updatedAt = System.currentTimeMillis()
            )
            localRepository.saveHazard(updated)
            cachedCount++
          }
        }
      }
      Result.success(cachedCount)
    } catch (e: Throwable) {
      Log.e("SyncManager", "Error fetching/caching remote hazards: ${e.message}")
      Result.failure(e)
    }
  }

  fun fetchAndCacheRemoteHazardsAsync() {
    scope.launch {
      fetchAndCacheRemoteHazards()
    }
  }
}
