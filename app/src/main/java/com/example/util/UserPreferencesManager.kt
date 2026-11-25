package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.PlaceCategory
import com.example.model.SavedPlace
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage of user preferences, authentication status,
 * profile details, and onboarding completion state using SharedPreferences.
 */
class UserPreferencesManager(context: Context) {

  private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
  )

  companion object {
    private const val PREFS_NAME = "safe_companion_user_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
    private const val KEY_DETAILS_SAVED = "key_details_saved"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_PHONE = "key_user_phone"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_EMERGENCY_NOTES = "key_emergency_notes"
    private const val KEY_VOICE_NOTE_PATH = "key_voice_note_path"
    private const val KEY_VOICE_NOTE_TRANSCRIPTION = "key_voice_note_transcription"
    private const val KEY_VOICE_NOTE_DURATION = "key_voice_note_duration"
    private const val KEY_HAS_VOICE_NOTE = "key_has_voice_note"
    private const val KEY_SAVED_PLACES = "key_saved_places"
  }

  /**
   * Returns true if the user has completed onboarding, logged in,
   * or previously saved their profile details.
   */
  fun isOnboardingCompleted(): Boolean {
    val completed = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    val detailsSaved = isDetailsSaved()
    val loggedIn = isLoggedIn()
    return completed || (loggedIn && detailsSaved)
  }

  fun setOnboardingCompleted(completed: Boolean) {
    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
  }

  fun isDetailsSaved(): Boolean {
    val flag = prefs.getBoolean(KEY_DETAILS_SAVED, false)
    val name = prefs.getString(KEY_USER_NAME, "")
    val phone = prefs.getString(KEY_USER_PHONE, "")
    return flag || !name.isNullOrBlank() || !phone.isNullOrBlank()
  }

  fun setDetailsSaved(saved: Boolean) {
    prefs.edit().putBoolean(KEY_DETAILS_SAVED, saved).apply()
  }

  fun isLoggedIn(): Boolean {
    return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
  }

  fun setLoggedIn(loggedIn: Boolean) {
    prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
  }

  fun saveUserProfile(profile: UserProfile) {
    val editor = prefs.edit()
    editor.putString(KEY_USER_NAME, profile.name)
    editor.putString(KEY_USER_PHONE, profile.phone)
    editor.putString(KEY_USER_EMAIL, profile.email)
    editor.putString(KEY_EMERGENCY_NOTES, profile.emergencyNotes)
    if (profile.preRecordedVoicePath != null) {
      editor.putString(KEY_VOICE_NOTE_PATH, profile.preRecordedVoicePath)
    }
    editor.putString(KEY_VOICE_NOTE_TRANSCRIPTION, profile.preRecordedVoiceTranscription)
    editor.putInt(KEY_VOICE_NOTE_DURATION, profile.preRecordedVoiceDurationSeconds)
    editor.putBoolean(KEY_HAS_VOICE_NOTE, profile.hasPreRecordedVoiceNote)
    if (profile.name.isNotBlank() || profile.phone.isNotBlank() || profile.email.isNotBlank()) {
      editor.putBoolean(KEY_DETAILS_SAVED, true)
    }
    editor.apply()
  }

  fun getUserProfile(): UserProfile {
    val name = prefs.getString(KEY_USER_NAME, "") ?: ""
    val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
    val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    val notes = prefs.getString(KEY_EMERGENCY_NOTES, "No known allergies.") ?: "No known allergies."
    val voicePath = prefs.getString(KEY_VOICE_NOTE_PATH, null)
    val transcription = prefs.getString(
      KEY_VOICE_NOTE_TRANSCRIPTION,
      "EMERGENCY SOS: I have had an accident and cannot speak. Please send immediate help to my current GPS coordinates!"
    ) ?: "EMERGENCY SOS: I have had an accident and cannot speak. Please send immediate help to my current GPS coordinates!"
    val duration = prefs.getInt(KEY_VOICE_NOTE_DURATION, 0)
    val hasVoice = prefs.getBoolean(KEY_HAS_VOICE_NOTE, false)
    val places = getSavedPlaces()

    return UserProfile(
      name = name,
      phone = phone,
      email = email,
      emergencyNotes = notes,
      preRecordedVoicePath = voicePath,
      preRecordedVoiceTranscription = transcription,
      preRecordedVoiceDurationSeconds = duration,
      hasPreRecordedVoiceNote = hasVoice,
      savedPlaces = places
    )
  }

  fun getSavedPlaces(): List<SavedPlace> {
    val rawJson = prefs.getString(KEY_SAVED_PLACES, null)
    if (rawJson.isNullOrBlank()) {
      val defaultPlaces = listOf(
        SavedPlace(
          id = "place_home",
          title = "Home",
          address = "742 Evergreen Terrace, San Francisco, CA",
          latitude = 37.7869,
          longitude = -122.4049,
          category = PlaceCategory.HOME,
          isHome = true,
          isWork = false
        ),
        SavedPlace(
          id = "place_work",
          title = "Work Office",
          address = "Market St & 4th St, San Francisco, CA",
          latitude = 37.7749,
          longitude = -122.4194,
          category = PlaceCategory.WORK,
          isHome = false,
          isWork = true
        )
      )
      saveSavedPlaces(defaultPlaces)
      return defaultPlaces
    }

    return try {
      val jsonArray = JSONArray(rawJson)
      val result = mutableListOf<SavedPlace>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val catName = obj.optString("category", PlaceCategory.OTHER.name)
        val category = try {
          PlaceCategory.valueOf(catName)
        } catch (_: Exception) {
          PlaceCategory.OTHER
        }
        result.add(
          SavedPlace(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            title = obj.optString("title", "Saved Place"),
            address = obj.optString("address", ""),
            latitude = obj.optDouble("latitude", 37.7749),
            longitude = obj.optDouble("longitude", -122.4194),
            category = category,
            isHome = obj.optBoolean("isHome", false),
            isWork = obj.optBoolean("isWork", false)
          )
        )
      }
      result
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveSavedPlaces(places: List<SavedPlace>) {
    try {
      val jsonArray = JSONArray()
      for (place in places) {
        val obj = JSONObject().apply {
          put("id", place.id)
          put("title", place.title)
          put("address", place.address)
          put("latitude", place.latitude)
          put("longitude", place.longitude)
          put("category", place.category.name)
          put("isHome", place.isHome)
          put("isWork", place.isWork)
        }
        jsonArray.put(obj)
      }
      prefs.edit().putString(KEY_SAVED_PLACES, jsonArray.toString()).apply()
    } catch (_: Exception) {}
  }

  fun clearPreferences() {
    prefs.edit().clear().apply()
  }
}
