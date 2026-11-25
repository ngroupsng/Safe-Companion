package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun EmergencyStatusScreen(
  emergencyState: EmergencyAlertState,
  onCancelEmergency: () -> Unit,
  onStartVoiceRecording: () -> Unit = {},
  onStopVoiceRecording: () -> Unit = {},
  onCancelVoiceRecording: () -> Unit = {},
  onRetryDispatch: () -> Unit = {},
  isPlayingVoiceNote: Boolean = false,
  onPlayVoiceNote: () -> Unit = {},
  onStopVoicePlayback: () -> Unit = {},
  onShareVoiceNoteViaMms: () -> Unit = {},
  onOpenMessagesApp: () -> Unit = {},
  onEditTranscription: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onCancelEmergency)
  val infiniteTransition = rememberInfiniteTransition(label = "sos_ring")
  val ringScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "ring_scale"
  )
  val ringAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "ring_alpha"
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
          Button(
            onClick = onCancelEmergency,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("button_cancel_sos_emergency"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = SurfaceContainerHigh,
              contentColor = Error
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp), tint = Error)
              Text(
                text = "Cancel SOS Alert (I am safe)",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = Error
                )
              )
            }
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
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Glowing pulsating emergency icon
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(110.dp)
      ) {
        // Outer glowing ring
        Box(
          modifier = Modifier
            .size(90.dp * ringScale)
            .clip(CircleShape)
            .background(Error.copy(alpha = ringAlpha))
        )

        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Error),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Emergency,
            contentDescription = "Emergency Active",
            tint = OnError,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "SOS Activated",
        style = MaterialTheme.typography.headlineLarge.copy(
          fontSize = 28.sp,
          color = Error,
          fontWeight = FontWeight.Bold
        )
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Help is on the way. Stay calm.",
        style = MaterialTheme.typography.bodyLarge.copy(
          color = OnSurface,
          fontWeight = FontWeight.Medium
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Failure & Status Banners
      if (!emergencyState.isLocationAvailable) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SurfaceContainerHigh,
          border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_location_unavailable")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.LocationOff, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
            Text(
              text = "Live GPS Location Unavailable: Last known coordinates dispatched.",
              style = MaterialTheme.typography.bodySmall.copy(color = Error, fontWeight = FontWeight.SemiBold)
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      if (emergencyState.communicationFailed) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ErrorContainer,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_communication_failed")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Communication Alert",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OnErrorContainer)
              )
              Text(
                text = emergencyState.failureReason ?: "Network and SMS dispatch encountered an error. SOS remains active.",
                style = MaterialTheme.typography.bodySmall.copy(color = OnErrorContainer)
              )
            }
            TextButton(onClick = onRetryDispatch) {
              Text("Retry", color = OnErrorContainer, fontWeight = FontWeight.Bold)
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Communication Method Banner
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) SecondaryContainer else SurfaceContainerLow,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_communication_method")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) Icons.Default.Sms else Icons.Default.Wifi,
            contentDescription = null,
            tint = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) OnSecondaryContainer else Primary,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = when (emergencyState.communicationMethod) {
                CommunicationMethod.ONLINE_FCM -> "Online Emergency Dispatch (Cloud / FCM)"
                CommunicationMethod.SMS_FALLBACK -> "Offline SMS Fallback Activated"
                else -> "Emergency Broadcast Active"
              },
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) OnSecondaryContainer else Primary
              )
            )
            Text(
              text = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) {
                "No internet detected. Immediate emergency SMS with live GPS and Maps link sent to all journey contacts."
              } else {
                "High-priority push alert sent with live location to all selected journey contacts."
              },
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurface)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Optional Voice Message Card (Non-blocking)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_voice_message_recorder")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "VOICE NOTE (OPTIONAL)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
              )
            )
            Text(
              text = "Does not block SOS",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Primary,
                fontWeight = FontWeight.Medium
              )
            )
          }

          when (emergencyState.voiceMessage.state) {
            VoiceRecordingState.IDLE -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = onStartVoiceRecording,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_record_voice_message"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp), tint = Error)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Record 10s Voice Note for Contacts", color = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
            VoiceRecordingState.RECORDING -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                Text(
                  text = "Recording voice message...",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Error),
                  modifier = Modifier.weight(1f)
                )
                Button(
                  onClick = onStopVoiceRecording,
                  colors = ButtonDefaults.buttonColors(containerColor = Error),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("button_stop_voice_recording")
                ) {
                  Text("Done", color = OnError)
                }
              }
            }
            VoiceRecordingState.RECORDED -> {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (emergencyState.voiceMessage.isOfflineRecorded) Tertiary else Primary,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = if (emergencyState.voiceMessage.isOfflineRecorded) {
                      "Voice note recorded offline (${emergencyState.voiceMessage.audioDurationSeconds}s)"
                    } else {
                      "Voice note recorded (${emergencyState.voiceMessage.audioDurationSeconds}s)"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = OnSurface
                    ),
                    modifier = Modifier.weight(1f)
                  )
                }

                // Transcription box
                if (!emergencyState.voiceMessage.transcription.isNullOrBlank()) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Text(
                        text = "Transcribed in Emergency SMS:",
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = OnSurfaceVariant,
                          fontWeight = FontWeight.Bold
                        )
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "\"${emergencyState.voiceMessage.transcription}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                          color = OnSurface
                        )
                      )
                    }
                  }
                }

                // Offline sync status note
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TertiaryContainer.copy(alpha = 0.15f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      Icons.Default.CloudSync,
                      contentDescription = null,
                      tint = Tertiary,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = "Audio saved on device. Auto-uploads to cloud when connection is restored.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Tertiary)
                    )
                  }
                }

                // Audio Playback & MMS Share Action Buttons
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      if (isPlayingVoiceNote) onStopVoicePlayback() else onPlayVoiceNote()
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("button_play_voice_note"),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(
                      imageVector = if (isPlayingVoiceNote) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlayingVoiceNote) "Stop" else "Listen")
                  }

                  Button(
                    onClick = onShareVoiceNoteViaMms,
                    modifier = Modifier
                      .weight(1.2f)
                      .testTag("button_share_voice_mms"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                  ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send via MMS", style = MaterialTheme.typography.labelMedium)
                  }
                }

                OutlinedButton(
                  onClick = onOpenMessagesApp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_open_messages_app_emergency_voice"),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Open SOS in Messages App (SMS)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
            }
            VoiceRecordingState.UPLOADING -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                  text = "Uploading voice message securely...",
                  style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                  modifier = Modifier.weight(1f)
                )
              }
            }
            VoiceRecordingState.UPLOADED -> {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(20.dp))
                  Text(
                    text = "Voice note attached to emergency (${emergencyState.voiceMessage.audioDurationSeconds}s)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Tertiary),
                    modifier = Modifier.weight(1f)
                  )
                  if (!emergencyState.voiceMessage.localFilePath.isNullOrBlank()) {
                    IconButton(
                      onClick = {
                        if (isPlayingVoiceNote) onStopVoicePlayback() else onPlayVoiceNote()
                      },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(
                        imageVector = if (isPlayingVoiceNote) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play audio",
                        tint = Primary
                      )
                    }
                  }
                }
                if (!emergencyState.voiceMessage.transcription.isNullOrBlank()) {
                  Text(
                    text = "\"${emergencyState.voiceMessage.transcription}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                      color = OnSurfaceVariant
                    )
                  )
                }
              }
            }
            VoiceRecordingState.FAILED -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.MicOff, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(
                  text = emergencyState.voiceMessage.errorMessage ?: "Voice note unavailable.",
                  style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant),
                  modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onStartVoiceRecording) {
                  Text("Retry")
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Direct Messages App Quick Access Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = PrimaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_open_messages_sms")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMessagesApp() }
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Chat, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
            }
            Column {
              Text(
                text = "Check in Messages App",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
              )
              Text(
                text = "View & send emergency SOS SMS with live GPS and Maps link",
                style = MaterialTheme.typography.bodySmall.copy(color = OnPrimaryContainer.copy(alpha = 0.8f))
              )
            }
          }
          Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Emergency status timeline card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "ALERT STATUS",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )

          // Step 1: Emergency Created
          TimelineStepItem(
            title = "Emergency created",
            subtitle = "SOS Triggered with journey contacts",
            isDone = true,
            isActive = false,
            isLast = false
          )

          // Step 2: Location captured
          TimelineStepItem(
            title = "Location captured",
            subtitle = if (emergencyState.isLocationAvailable) {
              "Accuracy: ±${emergencyState.accuracyMeters}m (GPS Locked)"
            } else {
              "Location unavailable (last known sent)"
            },
            isDone = true,
            isActive = false,
            isLast = false
          )

          // Step 3: Alert sent
          TimelineStepItem(
            title = if (emergencyState.communicationMethod == CommunicationMethod.SMS_FALLBACK) "SMS fallback sent" else "Cloud alert sent",
            subtitle = if (emergencyState.stepIndex >= 4) "Dispatched to contacts" else "Broadcasting emergency packet...",
            isDone = emergencyState.stepIndex >= 4,
            isActive = emergencyState.stepIndex == 3,
            isLast = false
          )

          // Step 4: Delivery
          TimelineStepItem(
            title = "Delivery confirmed",
            subtitle = if (emergencyState.stepIndex >= 5) "Delivered to contact devices" else "Awaiting carrier/cloud delivery",
            isDone = emergencyState.stepIndex >= 5,
            isActive = emergencyState.stepIndex == 4,
            isLast = false
          )

          // Step 5: Acknowledgement
          TimelineStepItem(
            title = "Contact acknowledgement",
            subtitle = if (emergencyState.stepIndex >= 5) "Contact viewed alert" else "Waiting for contact response",
            isDone = emergencyState.stepIndex >= 5,
            isActive = emergencyState.stepIndex == 5,
            isLast = true
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Notified Contacts List Card
      if (emergencyState.contactStatuses.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "JOURNEY CONTACTS NOTIFIED (${emergencyState.contactStatuses.size})",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
              )
            )

            emergencyState.contactStatuses.forEach { contactStatus ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = when (contactStatus.status) {
                      DeliveryStatus.DELIVERED, DeliveryStatus.ACKNOWLEDGED -> Icons.Default.CheckCircle
                      DeliveryStatus.SENT -> Icons.Default.Schedule
                      DeliveryStatus.FAILED -> Icons.Default.Error
                      DeliveryStatus.PENDING -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when (contactStatus.status) {
                      DeliveryStatus.DELIVERED, DeliveryStatus.ACKNOWLEDGED -> Tertiary
                      DeliveryStatus.SENT -> Primary
                      DeliveryStatus.FAILED -> Error
                      DeliveryStatus.PENDING -> OnSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                  )
                  Column {
                    Text(
                      text = contactStatus.contactName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                      text = "${contactStatus.method.displayName} • ${contactStatus.phone}",
                      style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = when (contactStatus.status) {
                    DeliveryStatus.DELIVERED, DeliveryStatus.ACKNOWLEDGED -> TertiaryContainer
                    DeliveryStatus.SENT -> PrimaryContainer
                    DeliveryStatus.FAILED -> ErrorContainer
                    DeliveryStatus.PENDING -> SurfaceVariant
                  }
                ) {
                  Text(
                    text = contactStatus.status.label,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = when (contactStatus.status) {
                        DeliveryStatus.DELIVERED, DeliveryStatus.ACKNOWLEDGED -> OnTertiaryContainer
                        DeliveryStatus.SENT -> OnPrimaryContainer
                        DeliveryStatus.FAILED -> OnErrorContainer
                        DeliveryStatus.PENDING -> OnSurfaceVariant
                      }
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TimelineStepItem(
  title: String,
  subtitle: String,
  isDone: Boolean,
  isActive: Boolean,
  isLast: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.Top
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(
            when {
              isDone -> TertiaryContainer
              isActive -> PrimaryContainer
              else -> SurfaceVariant
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isDone) {
          Icon(Icons.Default.Check, contentDescription = null, tint = OnTertiaryContainer, modifier = Modifier.size(16.dp))
        } else if (isActive) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(OnPrimaryContainer)
          )
        }
      }

      if (!isLast) {
        Box(
          modifier = Modifier
            .width(2.dp)
            .height(24.dp)
            .background(if (isDone) TertiaryContainer.copy(alpha = 0.5f) else OutlineVariant.copy(alpha = 0.4f))
        )
      }
    }

    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Medium,
          color = if (isActive || isDone) OnSurface else OnSurfaceVariant
        )
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
      )
    }
  }
}
