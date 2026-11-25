package com.example.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.local.entity.SyncStatus
import com.example.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: TrustedContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContacts(contacts: List<TrustedContactEntity>)

  @Update
  suspend fun updateContact(contact: TrustedContactEntity)

  @Query("SELECT * FROM trusted_contacts WHERE id = :contactId LIMIT 1")
  suspend fun getContactById(contactId: String): TrustedContactEntity?

  @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, name ASC")
  fun getAllContacts(): Flow<List<TrustedContactEntity>>

  @Query("SELECT * FROM trusted_contacts ORDER BY isPrimary DESC, name ASC")
  suspend fun getAllContactsList(): List<TrustedContactEntity>

  @Query("SELECT * FROM trusted_contacts WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
  suspend fun getPendingContacts(): List<TrustedContactEntity>

  @Query("DELETE FROM trusted_contacts WHERE id = :contactId")
  suspend fun deleteContactById(contactId: String)

  @Query("UPDATE trusted_contacts SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :contactId")
  suspend fun updateSyncStatus(contactId: String, syncStatus: SyncStatus, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM trusted_contacts")
  suspend fun clearAllContacts()
}
