package com.example.firebase.storage

/**
 * Generates and validates standard Firebase Storage paths for Safe Companion.
 * Adheres strictly to the specification:
 * emergency_voice/{userId}/{emergencyId}/{voiceFile}
 */
object FirebaseStorageHelper {

  const val EMERGENCY_VOICE_ROOT = "emergency_voice"

  /**
   * Generates the storage reference path for an emergency audio recording.
   */
  fun buildEmergencyVoiceStoragePath(
    userId: String,
    emergencyId: String,
    filename: String = "voice_note_${System.currentTimeMillis()}.m4a"
  ): String {
    require(userId.isNotBlank()) { "userId must not be blank" }
    require(emergencyId.isNotBlank()) { "emergencyId must not be blank" }
    require(filename.isNotBlank()) { "filename must not be blank" }

    val cleanFilename = filename.substringAfterLast("/")
    return "$EMERGENCY_VOICE_ROOT/$userId/$emergencyId/$cleanFilename"
  }

  /**
   * Validates whether a storage path complies with the emergency_voice convention.
   */
  fun isValidEmergencyVoicePath(path: String, expectedUserId: String? = null): Boolean {
    val segments = path.split("/").filter { it.isNotBlank() }
    if (segments.size != 4) return false
    if (segments[0] != EMERGENCY_VOICE_ROOT) return false
    if (expectedUserId != null && segments[1] != expectedUserId) return false
    return segments[2].isNotBlank() && segments[3].isNotBlank()
  }
}
