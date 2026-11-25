package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrustedContact
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SosConfirmationScreen(
  trustedContacts: List<TrustedContact>,
  onConfirmSos: () -> Unit,
  onCancel: () -> Unit,
  countdownSeconds: Int = 10,
  voiceMessageData: com.example.model.VoiceMessageData = com.example.model.VoiceMessageData(),
  userProfile: com.example.model.UserProfile = com.example.model.UserProfile(),
  onStartVoiceRecording: () -> Unit = {},
  onStopVoiceRecording: () -> Unit = {},
  isPlayingVoiceNote: Boolean = false,
  onPlayVoiceNote: () -> Unit = {},
  onStopVoicePlayback: () -> Unit = {},
  onOpenMessagesApp: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onCancel)
  var remainingSeconds by remember { mutableIntStateOf(countdownSeconds) }
  var isAutoTriggered by remember { mutableStateOf(false) }

  // Automatic emergency dispatch timer: dispatches SOS if user doesn't cancel or confirm within time limit
  LaunchedEffect(countdownSeconds) {
    while (remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
    }
    if (!isAutoTriggered) {
      isAutoTriggered = true
      onConfirmSos()
    }
  }

  val handleConfirm = {
    if (!isAutoTriggered) {
      isAutoTriggered = true
      onConfirmSos()
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Scaffold(
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = handleConfirm,
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("button_confirm_send_sos"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Error,
              contentColor = OnError
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(24.dp))
              Text(
                text = if (remainingSeconds > 0) "Send SOS Now (${remainingSeconds}s)" else "Sending SOS Alert...",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }

          OutlinedButton(
            onClick = onOpenMessagesApp,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("button_open_messages_app_sos_confirm"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Open in Messages App (SMS)",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("button_cancel_sos_confirm"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceVariant)
          ) {
            Text(
              text = "Cancel Alert (I'm Safe)",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Pulsing Emergency Icon
      Box(
        modifier = Modifier
          .size(100.dp)
          .clip(CircleShape)
          .background(ErrorContainer)
          .border(3.dp, Error.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Emergency,
          contentDescription = "Emergency Alert",
          tint = Error,
          modifier = Modifier.size(52.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Auto-dispatch countdown warning card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = ErrorContainer.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Error.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_sos_countdown")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = Error,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = if (remainingSeconds > 0) "Auto-sending in ${remainingSeconds}s" else "Sending emergency alert...",
              style = MaterialTheme.typography.titleMedium.copy(
                color = Error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              ),
              modifier = Modifier.testTag("text_sos_auto_countdown")
            )
          }

          // Progress indicator draining as time ticks down
          LinearProgressIndicator(
            progress = { (remainingSeconds.toFloat() / countdownSeconds.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Error,
            trackColor = Error.copy(alpha = 0.2f),
          )

          Text(
            text = "If you are unable to tap the screen, SOS will be sent automatically to your journey's trusted contacts when time expires.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = OnSurfaceVariant,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Send an emergency alert?",
        style = MaterialTheme.typography.headlineLarge.copy(
          fontSize = 26.sp,
          color = OnSurface,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your trusted contacts and emergency services will be notified of your current location and safety status immediately.",
        style = MaterialTheme.typography.bodyLarge.copy(
          color = OnSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 22.sp
        )
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Location sharing active badge
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = TertiaryContainer.copy(alpha = 0.15f)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
          Text(
            text = "Location sharing active",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Tertiary,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Quick Voice Note Card for Emergency
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_sos_quick_voice_note")
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Mic, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp))
              Text(
                text = "VOICE NOTE (OFFLINE READY)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
              )
            }
            Text(
              text = "Optional",
              style = MaterialTheme.typography.labelSmall.copy(color = Primary)
            )
          }

          Text(
            text = "Record a quick voice note. Spoken words are transcribed into offline emergency SMS and audio is auto-synced upon reconnection.",
            style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 12.sp)
          )

          when (voiceMessageData.state) {
            com.example.model.VoiceRecordingState.IDLE, com.example.model.VoiceRecordingState.FAILED -> {
              if (userProfile.hasPreRecordedVoiceNote || userProfile.preRecordedVoiceTranscription.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = PrimaryContainer.copy(alpha = 0.35f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (userProfile.hasPreRecordedVoiceNote) "Pre-Recorded Emergency Audio Active" else "Safety SOS Voice Message Active",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                      )
                      Text(
                        text = "If you cannot speak or record right now, this pre-recorded emergency message will be dispatched automatically with your SOS.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = OnSurfaceVariant)
                      )
                    }
                  }
                }

                if (userProfile.hasPreRecordedVoiceNote) {
                  OutlinedButton(
                    onClick = {
                      if (isPlayingVoiceNote) onStopVoicePlayback() else onPlayVoiceNote()
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("button_sos_preview_pre_recorded_voice"),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(
                      imageVector = if (isPlayingVoiceNote) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = if (isPlayingVoiceNote) "Stop" else "Listen",
                      modifier = Modifier.size(16.dp),
                      tint = Primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isPlayingVoiceNote) "Stop Audio" else "Listen to Emergency Note (${userProfile.preRecordedVoiceDurationSeconds}s)",
                      style = MaterialTheme.typography.labelMedium
                    )
                  }
                }
              }
              OutlinedButton(
                onClick = onStartVoiceRecording,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("button_sos_quick_record"),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = Error)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (userProfile.hasPreRecordedVoiceNote) "Record Fresh Note (Overrides Pre-recorded)" else "Record Voice Note Now",
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
            com.example.model.VoiceRecordingState.RECORDING -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp))
                Text(
                  text = "Recording audio...",
                  style = MaterialTheme.typography.bodyMedium.copy(color = Error, fontWeight = FontWeight.Bold),
                  modifier = Modifier.weight(1f)
                )
                Button(
                  onClick = onStopVoiceRecording,
                  colors = ButtonDefaults.buttonColors(containerColor = Error),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.testTag("button_sos_quick_done")
                ) {
                  Text("Done", color = OnError)
                }
              }
            }
            com.example.model.VoiceRecordingState.RECORDED, com.example.model.VoiceRecordingState.UPLOADED, com.example.model.VoiceRecordingState.UPLOADING -> {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = TertiaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Fresh voice note attached (${voiceMessageData.audioDurationSeconds}s) - takes priority over pre-recorded message",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Tertiary)
                    )
                    if (!voiceMessageData.transcription.isNullOrBlank()) {
                      Text(
                        text = "SMS: \"${voiceMessageData.transcription}\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = OnSurface)
                      )
                    }
                  }
                }
              }

              OutlinedButton(
                onClick = {
                  if (isPlayingVoiceNote) onStopVoicePlayback() else onPlayVoiceNote()
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("button_sos_preview_fresh_voice"),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(
                  imageVector = if (isPlayingVoiceNote) Icons.Default.Stop else Icons.Default.PlayArrow,
                  contentDescription = if (isPlayingVoiceNote) "Stop" else "Listen",
                  modifier = Modifier.size(16.dp),
                  tint = Tertiary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isPlayingVoiceNote) "Stop Audio" else "Listen to Fresh Voice Note (${voiceMessageData.audioDurationSeconds}s)",
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Who will be notified section
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "WHO WILL BE NOTIFIED",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )

        if (trustedContacts.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "No contacts selected for this journey. Emergency alert status will be recorded locally.",
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
              )
            }
          }
        } else {
          trustedContacts.forEach { contact ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SurfaceContainerLowest,
              border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = contact.initials,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Primary
                      )
                    )
                  }

                  Column {
                    Text(
                      text = contact.name,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                      text = if (contact.isPrimary) "${contact.relationship} (Primary Contact)" else contact.relationship,
                      style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                    )
                  }
                }

                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected",
                  tint = Tertiary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
