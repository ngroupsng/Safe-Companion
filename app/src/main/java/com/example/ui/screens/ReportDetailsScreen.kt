package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.HazardReport
import com.example.model.HazardStatus
import com.example.model.HazardType
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.components.SereneMapCanvas
import com.example.ui.theme.*

@Composable
fun ReportDetailsScreen(
  report: HazardReport?,
  onBackClick: () -> Unit,
  onVoteHazard: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  if (report == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No report selected.")
    }
    return
  }

  var votedPresent by remember(report.id) { mutableStateOf(report.userVoted) }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Hazard Details",
        onBackClick = onBackClick
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Header Card
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
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(
                when (report.type) {
                  HazardType.BROKEN_STREETLIGHT -> SurfaceContainerHighest
                  HazardType.POTHOLE -> ErrorContainer
                  else -> SecondaryContainer
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (report.type) {
                HazardType.BROKEN_STREETLIGHT -> Icons.Default.Lightbulb
                HazardType.POTHOLE -> Icons.Default.Warning
                HazardType.BLOCKED_ROAD -> Icons.Default.Block
                HazardType.UNSAFE_AREA -> Icons.Default.Security
                HazardType.OTHER -> Icons.Default.ReportProblem
              },
              contentDescription = null,
              tint = if (report.type == HazardType.POTHOLE) Error else Primary,
              modifier = Modifier.size(28.dp)
            )
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = report.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = OnSurface
                )
              )
              if (report.isReportedByMe) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = Primary.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "Reported by You",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Primary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Reported ${report.reportedTime}",
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
            )
          }
        }
      }

      // Status Progress Tracker
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "STATUS TRACKER",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatusStepNode(label = "Reported", isCompleted = true, isActive = false)
            StatusStepDivider(isCompleted = report.status != HazardStatus.REPORTED)
            StatusStepNode(label = "Review", isCompleted = report.status != HazardStatus.REPORTED, isActive = report.status == HazardStatus.UNDER_REVIEW)
            StatusStepDivider(isCompleted = report.status == HazardStatus.VERIFIED || report.status == HazardStatus.RESOLVED)
            StatusStepNode(label = "Verified", isCompleted = report.status == HazardStatus.VERIFIED || report.status == HazardStatus.RESOLVED, isActive = report.status == HazardStatus.VERIFIED)
            StatusStepDivider(isCompleted = report.status == HazardStatus.RESOLVED)
            StatusStepNode(label = "Resolved", isCompleted = report.status == HazardStatus.RESOLVED, isActive = report.status == HazardStatus.RESOLVED)
          }
        }
      }

      // Location & Mini Map Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
          ) {
            SereneMapCanvas(
              modifier = Modifier.fillMaxSize(),
              showRoute = false,
              showControls = false,
              hazardLocations = listOf("h" to (0.5f to 0.45f))
            )
          }

          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Column {
              Text(
                text = report.locationAddress,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = OnSurface)
              )
              Text(
                text = "Approx. ${report.distanceAway}",
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
              )
            }
          }
        }
      }

      // Description & Photo
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "DESCRIPTION",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )
          Text(
            text = report.description,
            style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface, lineHeight = 22.sp)
          )

          if (report.photoUrl != null) {
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
              model = report.photoUrl,
              contentDescription = "Hazard Photo",
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
          }
        }
      }

      // Community Confirmation & Voting
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Text(
              text = "${report.confirmationsCount} community members confirmed this hazard",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = OnSurface)
            )
          }

          Divider(color = OutlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

          Text(
            text = "Is this hazard still present?",
            style = MaterialTheme.typography.labelMedium.copy(color = OnSurfaceVariant)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                votedPresent = true
                onVoteHazard(report.id, true)
              },
              modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("button_vote_still_present"),
              shape = RoundedCornerShape(999.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (votedPresent == true) Primary else SurfaceContainerHigh,
                contentColor = if (votedPresent == true) OnPrimary else OnSurface
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Yes, still here", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
            }

            Button(
              onClick = {
                votedPresent = false
                onVoteHazard(report.id, false)
              },
              modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("button_vote_resolved"),
              shape = RoundedCornerShape(999.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (votedPresent == false) TertiaryContainer else SurfaceContainerHigh,
                contentColor = if (votedPresent == false) OnTertiaryContainer else OnSurface
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "No, it's fixed", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusStepNode(label: String, isCompleted: Boolean, isActive: Boolean) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(
          when {
            isCompleted -> TertiaryContainer
            isActive -> PrimaryContainer
            else -> SurfaceVariant
          }
        ),
      contentAlignment = Alignment.Center
    ) {
      if (isCompleted) {
        Icon(Icons.Default.Check, contentDescription = null, tint = OnTertiaryContainer, modifier = Modifier.size(14.dp))
      } else if (isActive) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OnPrimaryContainer))
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
        color = if (isActive || isCompleted) OnSurface else OnSurfaceVariant
      )
    )
  }
}

@Composable
private fun StatusStepDivider(isCompleted: Boolean) {
  Box(
    modifier = Modifier
      .width(28.dp)
      .height(2.dp)
      .background(if (isCompleted) TertiaryContainer else OutlineVariant.copy(alpha = 0.4f))
  )
}
