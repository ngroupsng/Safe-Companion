package com.example.local.repository

import com.example.local.SafeCompanionDatabase
import com.example.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Clean repository abstracting Room operations for Safe Companion.
 */
class SafeCompanionLocalRepository(
  private val database: SafeCompanionDatabase
) {

  private val journeyDao = database.journeyDao()
  private val journeyLocationDao = database.journeyLocationDao()
  private val emergencyDao = database.emergencyDao()
  private val trustedContactDao = database.trustedContactDao()
  private val communityHazardDao = database.communityHazardDao()

  // --- Journeys ---

  suspend fun saveJourney(journey: JourneyEntity) = journeyDao.insertJourney(journey)

  suspend fun getJourneyById(journeyId: String): JourneyEntity? = journeyDao.getJourneyById(journeyId)

  fun observeJourneyById(journeyId: String): Flow<JourneyEntity?> = journeyDao.observeJourneyById(journeyId)

  fun getAllJourneys(): Flow<List<JourneyEntity>> = journeyDao.getAllJourneys()

  suspend fun getAllJourneysList(): List<JourneyEntity> = journeyDao.getAllJourneysList()

  suspend fun completeJourney(
    journeyId: String,
    status: String = "COMPLETED",
    isReached: Boolean = true,
    endTime: Long = System.currentTimeMillis(),
    totalKm: Double = 0.0,
    durationMinutes: Int = 0
  ) = journeyDao.completeJourney(journeyId, status, isReached, endTime, totalKm, durationMinutes)

  suspend fun deleteJourney(journeyId: String) = journeyDao.deleteJourneyById(journeyId)

  // --- Locations ---

  suspend fun saveLocation(location: JourneyLocationEntity): Long = journeyLocationDao.insertLocation(location)

  suspend fun saveLocations(locations: List<JourneyLocationEntity>): List<Long> =
    journeyLocationDao.insertLocations(locations)

  suspend fun getLocationsForJourney(journeyId: String): List<JourneyLocationEntity> =
    journeyLocationDao.getLocationsForJourney(journeyId)

  fun observeLocationsForJourney(journeyId: String): Flow<List<JourneyLocationEntity>> =
    journeyLocationDao.observeLocationsForJourney(journeyId)

  suspend fun getLocationCountForJourney(journeyId: String): Int =
    journeyLocationDao.getLocationCountForJourney(journeyId)

  // --- Emergencies ---

  suspend fun saveEmergency(emergency: EmergencyEntity) = emergencyDao.insertEmergency(emergency)

  suspend fun getEmergencyById(emergencyId: String): EmergencyEntity? = emergencyDao.getEmergencyById(emergencyId)

  fun observeEmergencyById(emergencyId: String): Flow<EmergencyEntity?> = emergencyDao.observeEmergencyById(emergencyId)

  fun getAllEmergencies(): Flow<List<EmergencyEntity>> = emergencyDao.getAllEmergencies()

  suspend fun getAllEmergenciesList(): List<EmergencyEntity> = emergencyDao.getAllEmergenciesList()

  suspend fun resolveEmergency(emergencyId: String) = emergencyDao.resolveEmergency(emergencyId)

  suspend fun updateVoiceMessage(emergencyId: String, voiceUrl: String?, localPath: String?, transcription: String? = null) =
    emergencyDao.updateVoiceMessage(emergencyId, voiceUrl, localPath, transcription)

  // --- Trusted Contacts ---

  suspend fun saveTrustedContact(contact: TrustedContactEntity) = trustedContactDao.insertContact(contact)

  suspend fun saveTrustedContacts(contacts: List<TrustedContactEntity>) = trustedContactDao.insertContacts(contacts)

  suspend fun getTrustedContactById(contactId: String): TrustedContactEntity? = trustedContactDao.getContactById(contactId)

  fun getAllTrustedContacts(): Flow<List<TrustedContactEntity>> = trustedContactDao.getAllContacts()

  suspend fun getAllTrustedContactsList(): List<TrustedContactEntity> = trustedContactDao.getAllContactsList()

  suspend fun deleteTrustedContact(contactId: String) = trustedContactDao.deleteContactById(contactId)

  // --- Community Hazards ---

  suspend fun saveHazard(hazard: CommunityHazardEntity) = communityHazardDao.insertHazard(hazard)

  suspend fun saveHazards(hazards: List<CommunityHazardEntity>) = communityHazardDao.insertHazards(hazards)

  suspend fun updateHazard(hazard: CommunityHazardEntity) = communityHazardDao.updateHazard(hazard)

  suspend fun getHazardById(hazardId: String): CommunityHazardEntity? = communityHazardDao.getHazardById(hazardId)

  fun getAllHazards(): Flow<List<CommunityHazardEntity>> = communityHazardDao.getAllHazards()

  suspend fun getAllHazardsList(): List<CommunityHazardEntity> = communityHazardDao.getAllHazardsList()

  suspend fun deleteHazard(hazardId: String) = communityHazardDao.deleteHazardById(hazardId)

  // --- Sync Operations ---

  suspend fun getPendingJourneys(): List<JourneyEntity> = journeyDao.getPendingJourneys()
  suspend fun updateJourneySyncStatus(journeyId: String, status: SyncStatus) =
    journeyDao.updateSyncStatus(journeyId, status)

  suspend fun getPendingLocations(): List<JourneyLocationEntity> = journeyLocationDao.getPendingLocations()
  suspend fun getPendingLocationsForJourney(journeyId: String): List<JourneyLocationEntity> =
    journeyLocationDao.getPendingLocationsForJourney(journeyId)
  suspend fun getLocationById(id: Long): JourneyLocationEntity? = journeyLocationDao.getLocationById(id)
  suspend fun updateLocationSyncStatus(id: Long, status: SyncStatus) =
    journeyLocationDao.updateLocationSyncStatus(id, status)
  suspend fun updateLocationsSyncStatus(ids: List<Long>, status: SyncStatus) =
    journeyLocationDao.updateLocationsSyncStatus(ids, status)

  suspend fun getPendingEmergencies(): List<EmergencyEntity> = emergencyDao.getPendingEmergencies()
  suspend fun updateEmergencySyncStatus(emergencyId: String, status: SyncStatus) =
    emergencyDao.updateSyncStatus(emergencyId, status)

  suspend fun getPendingContacts(): List<TrustedContactEntity> = trustedContactDao.getPendingContacts()
  suspend fun updateContactSyncStatus(contactId: String, status: SyncStatus) =
    trustedContactDao.updateSyncStatus(contactId, status)

  suspend fun getPendingHazards(): List<CommunityHazardEntity> = communityHazardDao.getPendingHazards()
  suspend fun updateHazardSyncStatus(hazardId: String, status: SyncStatus) =
    communityHazardDao.updateSyncStatus(hazardId, status)
}
