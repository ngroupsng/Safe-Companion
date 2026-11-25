package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IncomingEmergencyAlert
import com.example.util.EmergencyAudioPlayer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrustedContactEmergencyScreen(
  alert: IncomingEmergencyAlert,
  onAcknowledgeAlarm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val infiniteTransition = rememberInfiniteTransition(label = "siren_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "siren_scale"
  )

  var isVoicePlaying by remember { mutableStateOf(false) }

  val formattedTime = remember(alert.timestamp) {
    try {
      SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(alert.timestamp))
    } catch (_: Throwable) {
      "Just now"
    }
  }

  Scaffold(
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (alert.isAlarmPlaying && !alert.isAcknowledged) {
            Button(
              onClick = onAcknowledgeAlarm,
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("button_acknowledge_alarm"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Error,
                contentColor = OnError
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                  text = "STOP ALARM & ACKNOWLEDGE",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          } else {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("button_dismiss_alert_screen"),
              shape = RoundedCornerShape(16.dp)
            ) {
              Text(
                text = "Close Alert Details",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      // Siren / Emergency Pulsing Header
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(110.dp)
      ) {
        if (alert.isAlarmPlaying && !alert.isAcknowledged) {
          Box(
            modifier = Modifier
              .size(95.dp * pulseScale)
              .clip(CircleShape)
              .background(Error.copy(alpha = 0.25f))
          )
        }
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Error),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Emergency Alarm",
            tint = OnError,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "🚨 EMERGENCY ALERT",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          color = Error,
          letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center
      )

      Text(
        text = "${alert.senderName} triggered SOS",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (alert.receivedViaSms) SecondaryContainer else TertiaryContainer
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = if (alert.receivedViaSms) Icons.Default.Sms else Icons.Default.CloudSync,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (alert.receivedViaSms) OnSecondaryContainer else OnTertiaryContainer
          )
          Text(
            text = if (alert.receivedViaSms) "Received via Emergency SMS Fallback" else "Received via High-Priority Cloud Alert",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (alert.receivedViaSms) OnSecondaryContainer else OnTertiaryContainer
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // User & Relationship Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
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
            Column {
              Text(
                text = "CALLING COMPANION",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = OnSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
              )
              Text(
                text = alert.senderName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = PrimaryContainer
            ) {
              Text(
                text = alert.relationship,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(color = OnPrimaryContainer, fontWeight = FontWeight.SemiBold)
              )
            }
          }

          if (alert.senderPhone.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
              Text(
                text = alert.senderPhone,
                style = MaterialTheme.typography.bodyMedium.copy(color = Primary, fontWeight = FontWeight.Medium)
              )
            }
          }

          if (!alert.journeyDestination.isNullOrBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
              Text(
                text = "Destination: ${alert.journeyDestination}",
                style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface)
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
              text = formattedTime,
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Location Details Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
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
              text = "LATEST CAPTURED LOCATION",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
              )
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = TertiaryContainer.copy(alpha = 0.5f)
            ) {
              Text(
                text = "±${alert.accuracyMeters}m GPS accuracy",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = OnTertiaryContainer,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }

          Text(
            text = "Lat: ${"%.5f".format(alert.latitude)}, Lng: ${"%.5f".format(alert.longitude)}",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          )

          if (alert.address.isNotBlank()) {
            Text(
              text = alert.address,
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
            )
          }

          // Open in Maps Action
          OutlinedButton(
            onClick = {
              val geoUri = Uri.parse("geo:${alert.latitude},${alert.longitude}?q=${alert.latitude},${alert.longitude}(${Uri.encode(alert.senderName + " SOS")})")
              val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
              mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
              try {
                context.startActivity(mapIntent)
              } catch (_: Throwable) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${alert.latitude},${alert.longitude}"))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("button_open_maps_alert"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Google Maps")
          }
        }
      }

      val audioPlayer = remember { EmergencyAudioPlayer(context) }
      DisposableEffect(Unit) {
        onDispose {
          audioPlayer.stopAudioPlayback()
        }
      }

      // Voice Message Section if available
      if (!alert.voiceMessageUrl.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.2f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .padding(16.dp)
              .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            IconButton(
              onClick = {
                if (isVoicePlaying) {
                  audioPlayer.stopAudioPlayback()
                  isVoicePlaying = false
                } else {
                  isVoicePlaying = true
                  val audioUrl = alert.voiceMessageUrl ?: ""
                  val text = "Emergency voice message from ${alert.senderName}. I have triggered an emergency alert. Please assist me at my location."
                  audioPlayer.playAudioFile(audioUrl, text) {
                    isVoicePlaying = false
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .background(Primary, CircleShape)
                .testTag("button_play_voice_message")
            ) {
              Icon(
                imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isVoicePlaying) "Pause voice note" else "Play voice note",
                tint = OnPrimary
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Voice Message Attached",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = if (isVoicePlaying) "Playing audio note..." else "Tap to listen to recorded audio note",
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Direct Phone Call Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (alert.senderPhone.isNotBlank()) {
          Button(
            onClick = {
              try {
                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${alert.senderPhone}"))
                callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(callIntent)
              } catch (_: Throwable) {}
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = OnPrimaryContainer),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Call Companion")
          }
        }

        Button(
          onClick = {
            try {
              val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
              callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
              context.startActivity(callIntent)
            } catch (_: Throwable) {}
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = OnError),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Call 911")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Important disclaimer regarding Android limits
      Text(
        text = "Notice: Siren playback and delivery respect device volume and system notification permissions.",
        style = MaterialTheme.typography.labelSmall.copy(
          color = OnSurfaceVariant.copy(alpha = 0.8f),
          textAlign = TextAlign.Center
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
