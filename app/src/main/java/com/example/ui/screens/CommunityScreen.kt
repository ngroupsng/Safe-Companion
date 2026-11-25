package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.OpenStreetMapView
import com.example.ui.components.SereneBottomNavBar
import com.example.ui.components.SereneTopAppBar
import com.example.ui.theme.*

enum class CommunitySubTab {
  FEED,
  MY_REPORTING
}

@Composable
fun CommunityScreen(
  userProfile: UserProfile,
  hazardReports: List<HazardReport>,
  currentFilter: HazardStatus?,
  currentMode: AppMode,
  currentTab: BottomTab,
  userLocation: UserLocationState = UserLocationState(),
  initialSubTab: CommunitySubTab = CommunitySubTab.FEED,
  onModeToggle: (AppMode) -> Unit,
  onTabSelected: (BottomTab) -> Unit,
  onFilterSelected: (HazardStatus?) -> Unit,
  onHazardClick: (HazardReport) -> Unit,
  onReportHazardClick: () -> Unit,
  onNotificationClick: () -> Unit,
  unreadNotificationsCount: Int = 0,
  onDeleteHazardReport: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler {
    onTabSelected(BottomTab.HOME)
  }
  var activeSubTab by remember { mutableStateOf(initialSubTab) }
  var myReportFilter by remember { mutableStateOf<HazardStatus?>(null) }
  var hazardToDelete by remember { mutableStateOf<HazardReport?>(null) }

  val myReports = remember(hazardReports) {
    hazardReports.filter { it.isReportedByMe }
  }

  val filteredFeedList = remember(hazardReports, currentFilter) {
    if (currentFilter == null) hazardReports else hazardReports.filter { it.status == currentFilter }
  }

  val filteredMyReports = remember(myReports, myReportFilter) {
    if (myReportFilter == null) myReports else myReports.filter { it.status == myReportFilter }
  }

  val centerLat = if (userLocation.hasRealFix) userLocation.latitude else 37.7749
  val centerLng = if (userLocation.hasRealFix) userLocation.longitude else -122.4194

  // Delete confirmation dialog
  if (hazardToDelete != null) {
    val target = hazardToDelete!!
    AlertDialog(
      onDismissRequest = { hazardToDelete = null },
      icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Error) },
      title = { Text("Withdraw Hazard Report?") },
      text = {
        Text("Are you sure you want to withdraw \"${target.title}\"? It will be removed from your reporting history and the active community map.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteHazardReport?.invoke(target.id)
            hazardToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Error)
        ) {
          Text("Withdraw Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { hazardToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  Scaffold(
    topBar = {
      SereneTopAppBar(
        title = "Community",
        avatarUrl = userProfile.avatarUrl,
        currentMode = currentMode,
        showModeToggle = true,
        onModeToggle = onModeToggle,
        onNotificationClick = onNotificationClick,
        unreadNotificationsCount = unreadNotificationsCount,
        onAvatarClick = { onTabSelected(BottomTab.PROFILE) }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onReportHazardClick,
        containerColor = Primary,
        contentColor = OnPrimary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .padding(bottom = 16.dp)
          .testTag("fab_report_hazard")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(24.dp))
          Text(
            text = "Report Hazard",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    },
    bottomBar = {
      SereneBottomNavBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Segmented Switcher: Community Feed vs My Reporting
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("community_sub_tab_row")
      ) {
        Row(
          modifier = Modifier.padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Tab 1: Community Feed
          Surface(
            onClick = { activeSubTab = CommunitySubTab.FEED },
            shape = RoundedCornerShape(10.dp),
            color = if (activeSubTab == CommunitySubTab.FEED) Primary else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("tab_community_feed")
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Public,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (activeSubTab == CommunitySubTab.FEED) OnPrimary else OnSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Community (${hazardReports.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (activeSubTab == CommunitySubTab.FEED) OnPrimary else OnSurfaceVariant
                )
              )
            }
          }

          // Tab 2: My Reporting
          Surface(
            onClick = { activeSubTab = CommunitySubTab.MY_REPORTING },
            shape = RoundedCornerShape(10.dp),
            color = if (activeSubTab == CommunitySubTab.MY_REPORTING) Primary else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("tab_my_reporting")
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (activeSubTab == CommunitySubTab.MY_REPORTING) OnPrimary else OnSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "My Reporting (${myReports.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (activeSubTab == CommunitySubTab.MY_REPORTING) OnPrimary else OnSurfaceVariant
                )
              )
            }
          }
        }
      }

      // Render based on selected sub-tab
      if (activeSubTab == CommunitySubTab.FEED) {
        // --- COMMUNITY FEED VIEW ---

        // Community Safety Summary Header Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          shadowElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "LOCAL AREA SAFETY",
              style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                letterSpacing = 0.5.sp
              )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(text = "${hazardReports.size}", style = MaterialTheme.typography.headlineLarge.copy(color = Primary, fontSize = 24.sp))
                  Text(text = "Total Reports", style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant))
                }
              }
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  val activeCount = hazardReports.count { it.status != HazardStatus.RESOLVED }
                  Text(text = "$activeCount", style = MaterialTheme.typography.headlineLarge.copy(color = Error, fontSize = 24.sp))
                  Text(text = "Requiring Attention", style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant))
                }
              }
            }
          }
        }

        // Community Hazard Map Preview Card (OpenStreetMap)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          shadowElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            OpenStreetMapView(
              modifier = Modifier.fillMaxSize(),
              startLat = centerLat,
              startLng = centerLng,
              destLat = centerLat + 0.005,
              destLng = centerLng + 0.005,
              userCurrentLat = if (userLocation.isPermissionGranted && userLocation.hasRealFix) userLocation.latitude else null,
              userCurrentLng = if (userLocation.isPermissionGranted && userLocation.hasRealFix) userLocation.longitude else null,
              accuracyMeters = if (userLocation.isPermissionGranted) userLocation.accuracyMeters else 0f,
              showRoute = false,
              userProgress = 0f,
              hazardReports = filteredFeedList,
              onHazardClick = onHazardClick
            )
            Surface(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
              shape = RoundedCornerShape(999.dp),
              color = SurfaceContainerLowest.copy(alpha = 0.95f),
              shadowElevation = 2.dp
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Icon(
                  Icons.Default.Place,
                  contentDescription = null,
                  tint = if (userLocation.isPermissionGranted) Primary else Error,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = if (userLocation.isPermissionGranted) {
                    "${filteredFeedList.size} Hazards nearby • GPS Active"
                  } else {
                    "${filteredFeedList.size} Hazards nearby • Location Disabled"
                  },
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                  )
                )
              }
            }
          }
        }

        // Filter Chips Row
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "NEARBY HAZARDS",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = currentFilter == null,
              onClick = { onFilterSelected(null) },
              label = { Text("All (${hazardReports.size})") },
              shape = RoundedCornerShape(999.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary,
                selectedLabelColor = OnPrimary
              )
            )
            HazardStatus.values().forEach { status ->
              val count = hazardReports.count { it.status == status }
              FilterChip(
                selected = currentFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text("${status.label} ($count)") },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Primary,
                  selectedLabelColor = OnPrimary
                )
              )
            }
          }
        }

        // Hazard Cards List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          filteredFeedList.forEach { hazard ->
            HazardFeedCard(
              hazard = hazard,
              onClick = { onHazardClick(hazard) }
            )
          }
        }

      } else {
        // --- MY REPORTING VIEW ---

        // My Safety Impact Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          shadowElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("card_my_reporting_summary")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "MY SAFETY CONTRIBUTIONS",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                  )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Your Reported Hazards",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                  )
                )
              }
              Surface(
                shape = CircleShape,
                color = Primary.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Tiles
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "${myReports.size}",
                    style = MaterialTheme.typography.titleLarge.copy(color = Primary, fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "Total Filed",
                    style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 11.sp)
                  )
                }
              }
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  val inReviewCount = myReports.count { it.status == HazardStatus.REPORTED || it.status == HazardStatus.UNDER_REVIEW }
                  Text(
                    text = "$inReviewCount",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "In Review",
                    style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 11.sp)
                  )
                }
              }
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  val resolvedCount = myReports.count { it.status == HazardStatus.VERIFIED || it.status == HazardStatus.RESOLVED }
                  Text(
                    text = "$resolvedCount",
                    style = MaterialTheme.typography.titleLarge.copy(color = Tertiary, fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "Verified/Fixed",
                    style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 11.sp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Reports you file directly alert nearby pedestrians during safe route planning and inform municipal safety teams.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            )
          }
        }

        // Quick Action: Report Button
        Button(
          onClick = onReportHazardClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Primary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("button_my_reporting_add")
        ) {
          Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Report a New Hazard",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }

        // Filter chips for user's reports
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "MY REPORTS STATUS",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = myReportFilter == null,
              onClick = { myReportFilter = null },
              label = { Text("All (${myReports.size})") },
              shape = RoundedCornerShape(999.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary,
                selectedLabelColor = OnPrimary
              )
            )
            HazardStatus.values().forEach { status ->
              val count = myReports.count { it.status == status }
              FilterChip(
                selected = myReportFilter == status,
                onClick = { myReportFilter = if (myReportFilter == status) null else status },
                label = { Text("${status.label} ($count)") },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Primary,
                  selectedLabelColor = OnPrimary
                )
              )
            }
          }
        }

        // User reports list or empty state
        if (filteredMyReports.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp)
          ) {
            Column(
              modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Outlined.AssignmentLate,
                  contentDescription = null,
                  tint = Primary,
                  modifier = Modifier.size(28.dp)
                )
              }
              Text(
                text = if (myReports.isEmpty()) "No Hazards Reported Yet" else "No Reports for this Filter",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = if (myReports.isEmpty()) {
                  "You have not reported any community hazards yet. Spotted an unlit road, pothole, or obstruction? File a report to alert others!"
                } else {
                  "There are no reports submitted by you matching the selected status."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
              if (myReports.isEmpty()) {
                Button(
                  onClick = onReportHazardClick,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                  Text("Report Your First Hazard")
                }
              } else {
                OutlinedButton(
                  onClick = { myReportFilter = null },
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Clear Status Filter")
                }
              }
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            filteredMyReports.forEach { report ->
              MyReportCard(
                report = report,
                onClick = { onHazardClick(report) },
                onWithdraw = { hazardToDelete = report }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(48.dp))
    }
  }
}

@Composable
private fun HazardFeedCard(
  hazard: HazardReport,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("hazard_card_${hazard.id}")
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(
            when (hazard.type) {
              HazardType.BROKEN_STREETLIGHT -> SurfaceContainerHighest
              HazardType.POTHOLE -> ErrorContainer
              HazardType.BLOCKED_ROAD -> SecondaryContainer
              else -> SurfaceContainerHigh
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (hazard.type) {
            HazardType.BROKEN_STREETLIGHT -> Icons.Default.Lightbulb
            HazardType.POTHOLE -> Icons.Default.Warning
            HazardType.BLOCKED_ROAD -> Icons.Default.Block
            HazardType.UNSAFE_AREA -> Icons.Default.Security
            HazardType.OTHER -> Icons.Default.ReportProblem
          },
          contentDescription = null,
          tint = when (hazard.type) {
            HazardType.POTHOLE -> Error
            HazardType.BROKEN_STREETLIGHT -> Primary
            else -> OnSurfaceVariant
          },
          modifier = Modifier.size(24.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Text(
              text = hazard.title,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
              )
            )
            if (hazard.isReportedByMe) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Primary.copy(alpha = 0.12f)
              ) {
                Text(
                  text = "You",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          // Status Badge
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = when (hazard.status) {
              HazardStatus.REPORTED -> ErrorContainer
              HazardStatus.UNDER_REVIEW -> PrimaryContainer.copy(alpha = 0.15f)
              HazardStatus.VERIFIED -> PrimaryContainer.copy(alpha = 0.25f)
              HazardStatus.RESOLVED -> TertiaryContainer.copy(alpha = 0.15f)
            }
          ) {
            Text(
              text = hazard.status.label,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when (hazard.status) {
                  HazardStatus.REPORTED -> Error
                  HazardStatus.UNDER_REVIEW -> Primary
                  HazardStatus.VERIFIED -> Primary
                  HazardStatus.RESOLVED -> Tertiary
                }
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${hazard.distanceAway} · ${hazard.locationAddress}",
          style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
          Text(
            text = "${hazard.confirmationsCount} community confirmations",
            style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.Medium)
          )
        }
      }
    }
  }
}

@Composable
private fun MyReportCard(
  report: HazardReport,
  onClick: () -> Unit,
  onWithdraw: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("my_report_card_${report.id}")
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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(
                when (report.type) {
                  HazardType.BROKEN_STREETLIGHT -> SurfaceContainerHighest
                  HazardType.POTHOLE -> ErrorContainer
                  HazardType.BLOCKED_ROAD -> SecondaryContainer
                  else -> SurfaceContainerHigh
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
              tint = when (report.type) {
                HazardType.POTHOLE -> Error
                HazardType.BROKEN_STREETLIGHT -> Primary
                else -> OnSurfaceVariant
              },
              modifier = Modifier.size(22.dp)
            )
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Primary.copy(alpha = 0.12f)
              ) {
                Text(
                  text = "Your Report",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Filed ${report.reportedTime}",
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 11.sp)
            )
          }
        }

        // Status badge
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = when (report.status) {
            HazardStatus.REPORTED -> ErrorContainer
            HazardStatus.UNDER_REVIEW -> PrimaryContainer.copy(alpha = 0.2f)
            HazardStatus.VERIFIED -> PrimaryContainer.copy(alpha = 0.35f)
            HazardStatus.RESOLVED -> TertiaryContainer.copy(alpha = 0.2f)
          }
        ) {
          Text(
            text = report.status.label,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = when (report.status) {
                HazardStatus.REPORTED -> Error
                HazardStatus.UNDER_REVIEW -> Primary
                HazardStatus.VERIFIED -> Primary
                HazardStatus.RESOLVED -> Tertiary
              }
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
          )
        }
      }

      // Address Row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          Icons.Default.Place,
          contentDescription = null,
          tint = OnSurfaceVariant,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = report.locationAddress,
          style = MaterialTheme.typography.bodySmall.copy(
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
        )
      }

      // Description
      if (report.description.isNotBlank()) {
        Text(
          text = report.description,
          style = MaterialTheme.typography.bodySmall.copy(color = OnSurface, lineHeight = 18.sp),
          maxLines = 2,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }

      // Divider & Footer Actions
      HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
          Text(
            text = "${report.confirmationsCount} community upvotes",
            style = MaterialTheme.typography.labelSmall.copy(color = Primary, fontWeight = FontWeight.SemiBold)
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onWithdraw,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Error)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Withdraw", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
          }

          OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Text("Details", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }
  }
}
