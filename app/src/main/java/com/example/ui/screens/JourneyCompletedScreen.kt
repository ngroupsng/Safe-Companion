package com.example.ui.screens

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
import androidx.compose.runtime.Composable
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
import com.example.model.JourneySession
import com.example.ui.theme.*

@Composable
fun JourneyCompletedScreen(
  journeySession: JourneySession,
  onBackToDashboard: () -> Unit,
  onViewDetails: () -> Unit,
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onBackToDashboard)
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
            onClick = onBackToDashboard,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("button_back_to_dashboard_completed"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Primary,
              contentColor = OnPrimary
            )
          ) {
            Text(
              text = "Back to Dashboard",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("button_view_journey_map_details"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
          ) {
            Text(
              text = "View Journey Map Details",
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
      // Blue shield icon with checkmark
      Box(
        modifier = Modifier
          .size(100.dp)
          .clip(CircleShape)
          .background(PrimaryContainer.copy(alpha = 0.15f))
          .border(2.dp, PrimaryFixedDim, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Verified,
          contentDescription = "Safe Arrival",
          tint = Primary,
          modifier = Modifier.size(54.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Journey Completed",
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = 28.sp,
          color = OnSurface,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "You have arrived safely at your destination.",
        style = MaterialTheme.typography.bodyLarge.copy(
          color = OnSurfaceVariant,
          textAlign = TextAlign.Center
        )
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Route summary card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "FROM", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant))
            Text(text = journeySession.startLocation, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp, color = OnSurface))
          }
          Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
          Column(horizontalAlignment = Alignment.End) {
            Text(text = "TO", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant))
            Text(text = journeySession.destination, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp, color = Primary))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bento 4-card statistics
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Duration
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          color = SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "DURATION", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${journeySession.durationMinutes} min", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp, color = OnSurface))
          }
        }

        // Distance
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          color = SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "DISTANCE", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${journeySession.totalKm} km", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp, color = OnSurface))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Warnings
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          color = SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "WARNINGS", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
              Text(text = "0 warnings", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Tertiary))
            }
          }
        }

        // Emergency alerts
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          color = SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "EMERGENCY ALERTS", style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
              Text(text = "None", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Tertiary))
            }
          }
        }
      }
    }
  }
}
