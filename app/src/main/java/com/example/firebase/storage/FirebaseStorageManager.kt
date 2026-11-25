package com.example.firebase.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Manages Firebase Storage operations for emergency audio recordings.
 * Uses paths conforming to:
 * emergency_voice/{userId}/{emergencyId}/{voiceFile}
 */
class FirebaseStorageManager(
  private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

  /**
   * Uploads an emergency audio recording file to Firebase Storage and returns its download URL.
   */
  suspend fun uploadEmergencyVoiceNote(
    userId: String,
    emergencyId: String,
    audioFile: File
  ): Result<String> = runCatching {
    require(userId.isNotBlank()) { "userId must not be blank" }
    require(emergencyId.isNotBlank()) { "emergencyId must not be blank" }
    require(audioFile.exists()) { "audio file does not exist: ${audioFile.absolutePath}" }

    val storagePath = FirebaseStorageHelper.buildEmergencyVoiceStoragePath(
      userId = userId,
      emergencyId = emergencyId,
      filename = audioFile.name
    )

    val storageRef = storage.reference.child(storagePath)
    val uploadTask = storageRef.putFile(Uri.fromFile(audioFile)).await()
    val downloadUrl = storageRef.downloadUrl.await()

    downloadUrl.toString()
  }

  /**
   * Deletes an emergency audio file from storage.
   */
  suspend fun deleteEmergencyVoiceNote(
    userId: String,
    emergencyId: String,
    filename: String
  ): Result<Unit> = runCatching {
    val storagePath = FirebaseStorageHelper.buildEmergencyVoiceStoragePath(
      userId = userId,
      emergencyId = emergencyId,
      filename = filename
    )
    storage.reference.child(storagePath).delete().await()
  }
}
