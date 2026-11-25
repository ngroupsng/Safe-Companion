package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

/**
 * Dedicated Emergency Audio Player.
 * - Plays local emergency confirmation tone on the sender's device when SOS is triggered.
 * - Plays high-priority siren/alarm sound on the recipient device when an emergency alert is received.
 * - Manages starting, looping, and stopping the siren, with vibration support.
 */
class EmergencyAudioPlayer(private val context: Context? = null) {

  private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private var sirenJob: Job? = null
  private var isSirenActive = false
  private var mediaPlayer: android.media.MediaPlayer? = null
  private var isPlayingAudioFile = false

  private var textToSpeech: android.speech.tts.TextToSpeech? = null
  private var isTtsReady = false

  init {
    // Initialize TTS asynchronously so it never stalls main UI thread / SurfaceSyncGroup during startup
    coroutineScope.launch(Dispatchers.IO) {
      ensureTtsInitialized()
    }
  }

  @Synchronized
  fun ensureTtsInitialized() {
    if (textToSpeech != null) return
    context?.let { ctx ->
      try {
        textToSpeech = android.speech.tts.TextToSpeech(ctx.applicationContext) { status ->
          if (status == android.speech.tts.TextToSpeech.SUCCESS) {
            try {
              val loc = java.util.Locale.getDefault()
              if ((textToSpeech?.isLanguageAvailable(loc) ?: -1) >= android.speech.tts.TextToSpeech.LANG_AVAILABLE) {
                textToSpeech?.language = loc
              } else {
                textToSpeech?.language = java.util.Locale.US
              }
            } catch (_: Throwable) {
              try { textToSpeech?.language = java.util.Locale.US } catch (_: Throwable) {}
            }
            isTtsReady = true
          }
        }
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "TextToSpeech init warning: ${e.message}")
      }
    }
  }

  fun isSirenPlaying(): Boolean = isSirenActive
  fun isPlayingAudio(): Boolean = isPlayingAudioFile

  /**
   * Plays a recorded voice note from local file path or speaks the voice transcription.
   * Ensures stream volume is unmuted, uses proper AudioAttributes, and falls back to
   * Text-To-Speech or acoustic chimes if file playback cannot proceed.
   */
  fun playAudioFile(
    filePath: String,
    transcription: String? = null,
    onFinished: (() -> Unit)? = null
  ) {
    stopAudioPlayback()

    // Unmute or raise STREAM_MUSIC volume if currently silent
    context?.let { ctx ->
      try {
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (currentVol == 0 && maxVol > 0) {
          audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75).toInt().coerceAtLeast(1), 0)
        }
      } catch (_: Throwable) {}
    }

    val file = if (filePath.isNotBlank()) java.io.File(filePath) else null
    val hasValidFile = file != null && file.exists() && file.length() > 200L

    if (hasValidFile) {
      try {
        val player = android.media.MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .build()
          )
          setVolume(1.0f, 1.0f)
          setDataSource(filePath)
          setOnCompletionListener {
            isPlayingAudioFile = false
            try { it.release() } catch (_: Throwable) {}
            mediaPlayer = null
            onFinished?.invoke()
          }
          setOnErrorListener { _, what, extra ->
            Log.w("EmergencyAudioPlayer", "MediaPlayer playback error: $what/$extra, falling back to TTS/tone")
            isPlayingAudioFile = false
            try { release() } catch (_: Throwable) {}
            mediaPlayer = null
            playTtsOrToneFallback(transcription, onFinished)
            true
          }
          prepare()
          start()
        }
        mediaPlayer = player
        isPlayingAudioFile = true
        return
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "MediaPlayer init error: ${e.message}, falling back to speech synthesis")
        isPlayingAudioFile = false
      }
    }

    // Fallback to TTS or synthesized emergency tone
    playTtsOrToneFallback(transcription, onFinished)
  }

  private fun playTtsOrToneFallback(transcription: String?, onFinished: (() -> Unit)? = null) {
    if (textToSpeech == null) {
      ensureTtsInitialized()
    }
    val cleanText = transcription?.trim()
    if (!cleanText.isNullOrBlank() && textToSpeech != null && isTtsReady) {
      try {
        val utteranceId = "emergency_tts_${System.currentTimeMillis()}"
        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
          override fun onStart(id: String?) {
            isPlayingAudioFile = true
          }
          override fun onDone(id: String?) {
            isPlayingAudioFile = false
            coroutineScope.launch(Dispatchers.Main) {
              onFinished?.invoke()
            }
          }
          override fun onError(id: String?) {
            isPlayingAudioFile = false
            coroutineScope.launch(Dispatchers.Main) {
              playToneChimeFallback(onFinished)
            }
          }
        })

        val params = android.os.Bundle().apply {
          putInt(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
          putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val res = textToSpeech?.speak(cleanText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (res == android.speech.tts.TextToSpeech.SUCCESS) {
          isPlayingAudioFile = true
          return
        }
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "TTS speak exception: ${e.message}")
      }
    }

    playToneChimeFallback(onFinished)
  }

  private fun playToneChimeFallback(onFinished: (() -> Unit)? = null) {
    isPlayingAudioFile = true
    coroutineScope.launch {
      try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        delay(350)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        delay(450)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
        delay(550)
        toneGen.release()
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "Tone fallback error: ${e.message}")
      } finally {
        isPlayingAudioFile = false
        withContext(Dispatchers.Main) {
          onFinished?.invoke()
        }
      }
    }
  }

  /**
   * Stops recorded audio file playback.
   */
  fun stopAudioPlayback() {
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (_: Throwable) {}
    mediaPlayer = null

    try {
      textToSpeech?.stop()
    } catch (_: Throwable) {}

    isPlayingAudioFile = false
  }

  /**
   * Plays a local emergency confirmation sound on the sender's phone
   * so the user receives immediate audible confirmation that the SOS action was activated.
   */
  fun playSosConfirmationBeep() {
    coroutineScope.launch {
      try {
        val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
        delay(450)
        toneGen.release()
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "ToneGenerator failed (expected in test/headless): ${e.message}")
        // Fallback or silent in test environment
      }

      // Short haptic confirmation
      triggerHaptic(longArrayOf(0, 100, 50, 150))
    }
  }

  /**
   * Starts dedicated emergency alarm siren.
   * Alternates frequencies to create a loud, distinctive emergency siren pattern.
   * Plays continuously until stopSiren() is called.
   */
  fun startSiren() {
    if (isSirenActive) return
    isSirenActive = true

    sirenJob?.cancel()
    sirenJob = coroutineScope.launch {
      try {
        // Continuous siren vibration pattern
        triggerVibrationPattern(longArrayOf(0, 500, 200, 500, 200, 800))

        while (isActive && isSirenActive) {
          try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            // Warbling alternating high-urgency siren pattern
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 800)
            delay(850)
            if (!isSirenActive) {
              toneGen.release()
              break
            }
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            delay(650)
            toneGen.release()
          } catch (e: Throwable) {
            Log.w("EmergencyAudioPlayer", "Siren loop tone exception: ${e.message}")
            delay(1000)
          }
        }
      } catch (e: CancellationException) {
        // Normal cancellation
      } catch (e: Throwable) {
        Log.w("EmergencyAudioPlayer", "Siren playback failed: ${e.message}")
      } finally {
        stopVibration()
      }
    }
  }

  /**
   * Stops the dedicated emergency alarm siren immediately and cancels vibration.
   */
  fun stopSiren() {
    isSirenActive = false
    sirenJob?.cancel()
    sirenJob = null
    stopVibration()
  }

  private fun triggerHaptic(timings: LongArray) {
    try {
      context?.let { ctx ->
        val vibrator = getVibrator(ctx)
        if (vibrator != null && vibrator.hasVibrator()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
          } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
          }
        }
      }
    } catch (_: Throwable) {}
  }

  private fun triggerVibrationPattern(pattern: LongArray) {
    try {
      context?.let { ctx ->
        val vibrator = getVibrator(ctx)
        if (vibrator != null && vibrator.hasVibrator()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // repeat index 0
          } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
          }
        }
      }
    } catch (_: Throwable) {}
  }

  private fun stopVibration() {
    try {
      context?.let { ctx ->
        val vibrator = getVibrator(ctx)
        vibrator?.cancel()
      }
    } catch (_: Throwable) {}
  }

  private fun getVibrator(context: Context): Vibrator? {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (_: Throwable) {
      null
    }
  }

  fun release() {
    stopSiren()
    stopAudioPlayback()
    try {
      textToSpeech?.shutdown()
    } catch (_: Throwable) {}
    textToSpeech = null
    coroutineScope.cancel()
  }
}
