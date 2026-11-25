package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BottomTab
import com.example.model.JourneyRecord
import com.example.model.JourneySession
import com.example.model.UserLocationState
import com.example.ui.components.OpenStreetMapView
import com.example.ui.components.SereneBottomNavBar
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.theme.*
import java.util.Locale

/**
 * Screen presenting the complete record of user journeys and interactive map.
 * Allows users to review previous journey details, track distance/duration/safety scores,
 * and view route paths on OpenStreetMap.
 */
@Composable
fun JourneyMapScreen(
  journeySession: JourneySession,
  journeyHistory: List<JourneyRecord> = emptyList(),
  selectedJourneyRecord: JourneyRecord? = null,
  userLocation: UserLocationState = UserLocationState(),
  currentTab: BottomTab = BottomTab.JOURNEY,
  onTabSelected: (BottomTab) -> Unit = {},
  onSelectJourneyRecord: (JourneyRecord) -> Unit = {},
  onStartNewJourneyClick: () -> Unit = {},
  onNavigateToActiveJourney: () -> Unit = {},
  onBackClick: () -> Unit = {},
  onEmergencyClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler {
    onTabSelected(BottomTab.HOME)
  }
  // 0 = Records & History, 1 = Route Map
  var selectedTab by remember { mutableIntStateOf(0) }

  val activeRecord = selectedJourneyRecord ?: journeyHistory.firstOrNull()

  // Calculate summary metrics
  val totalTrips = journeyHistory.size
  val totalKm = journeyHistory.sumOf { it.distanceKm }
  val safeTripsRate = if (totalTrips > 0) {
    (journeyHistory.count { it.status == "COMPLETED" || it.safetyScore >= 90 } * 100) / totalTrips
  } else 100

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
      ) {
        SereneDetailTopBar(
          title = "Journey & Records",
          onBackClick = onBackClick
        )

        // Segmented Tab Switcher (Records vs Route Map)
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color.White,
          contentColor = Color(0xFF2563EB),
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              height = 3.dp,
              color = Color(0xFF2563EB)
            )
          },
          divider = {
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
          }
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            modifier = Modifier.testTag("tab_journey_records"),
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Records & History",
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 14.sp
                )
                Surface(
                  shape = CircleShape,
                  color = if (selectedTab == 0) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                ) {
                  Text(
                    text = totalTrips.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (selectedTab == 0) Color(0xFF2563EB) else Color(0xFF64748B)
                    )
                  )
                }
              }
            }
          )

          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            modifier = Modifier.testTag("tab_route_map"),
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Map,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Route Map",
                  fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 14.sp
                )
              }
            }
          )
        }
      }
    },
    bottomBar = {
      SereneBottomNavBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFFF8FAFC))
        .padding(innerPadding)
    ) {
      if (selectedTab == 0) {
        // RECORDS & HISTORY LIST VIEW
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
          // 1. Overall Summary Stats Card
          item {
            JourneySummaryMetricsCard(
              totalTrips = totalTrips,
              totalKm = totalKm,
              safePercentage = safeTripsRate
            )
          }

          // 2. Active Monitoring Journey Banner (if ongoing)
          if (journeySession.isMonitoring) {
            item {
              ActiveMonitoringBanner(
                journeySession = journeySession,
                onNavigateToActive = onNavigateToActiveJourney
              )
            }
          }

          // 3. Start New Journey Action Button
          item {
            Button(
              onClick = onStartNewJourneyClick,
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("button_start_new_journey"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
              ),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.DirectionsWalk,
                  contentDescription = null,
                  modifier = Modifier.size(24.dp)
                )
                Text(
                  text = "Plan New Safe Journey",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                )
              }
            }
          }

          // 4. Section Header
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "PREVIOUS JOURNEY RECORDS",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp,
                    fontSize = 12.sp
                  )
                )
                Text(
                  text = "Recorded routes, distance, and safety logs",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                  )
                )
              }
            }
          }

          // 5. List of Historical Journey Record Cards
          if (journeyHistory.isEmpty()) {
            item {
              EmptyJourneyRecordsView(onStartJourney = onStartNewJourneyClick)
            }
          } else {
            items(journeyHistory, key = { it.id }) { record ->
              JourneyRecordCard(
                record = record,
                isSelected = record.id == activeRecord?.id,
                onViewOnMap = {
                  onSelectJourneyRecord(record)
                  selectedTab = 1
                },
                onClick = {
                  onSelectJourneyRecord(record)
                }
              )
            }
          }
        }
      } else {
        // ROUTE MAP VIEW (Displays OpenStreetMap + Record Details)
        val displayStartLat = activeRecord?.startLat ?: journeySession.startLat
        val displayStartLng = activeRecord?.startLng ?: journeySession.startLng
        val displayDestLat = activeRecord?.destLat ?: journeySession.destLat
        val displayDestLng = activeRecord?.destLng ?: journeySession.destLng
        val displayPoints = if (activeRecord?.roadRoutePoints?.isNotEmpty() == true) {
          activeRecord.roadRoutePoints
        } else {
          journeySession.roadRoutePoints
        }

        Box(modifier = Modifier.fillMaxSize()) {
          // OpenStreetMapView with real worldwide street tiles
          OpenStreetMapView(
            modifier = Modifier.fillMaxSize(),
            startLat = displayStartLat,
            startLng = displayStartLng,
            destLat = displayDestLat,
            destLng = displayDestLng,
            userCurrentLat = userLocation.latitude,
            userCurrentLng = userLocation.longitude,
            accuracyMeters = userLocation.accuracyMeters,
            showRoute = true,
            userProgress = if (activeRecord?.status == "COMPLETED") 1.0f else journeySession.progress,
            routePoints = displayPoints,
            hazardPoints = listOf(
              "Poor Lighting" to (displayStartLat + 0.0041 to displayStartLng + 0.0064)
            )
          )

          // Floating Top Pill: Journey Record Quick Indicator
          Surface(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (activeRecord?.status == "COMPLETED") Color(0xFF10B981) else Color(0xFF2563EB))
              )
              Text(
                text = "Viewing: ${activeRecord?.destination ?: journeySession.destination}",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1E293B)
                )
              )
            }
          }

          // Bottom floating card with full journey details
          Surface(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = activeRecord?.destination ?: journeySession.destination,
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF0F172A)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "From: ${activeRecord?.startLocation ?: journeySession.startLocation}",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = Color(0xFF64748B),
                      fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Safety Score Badge
                val score = activeRecord?.safetyScore ?: 98
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = Color(0xFFECFDF5),
                  border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Shield,
                      contentDescription = null,
                      tint = Color(0xFF059669),
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "Score: $score/100",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold
                      )
                    )
                  }
                }
              }

              // Metrics row: Distance, Duration, Status
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "Distance",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                  )
                  val dist = activeRecord?.distanceKm ?: journeySession.totalKm
                  Text(
                    text = String.format(Locale.US, "%.1f km", dist),
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF1E293B)
                    )
                  )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))

                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                  )
                  val dur = activeRecord?.durationMinutes ?: journeySession.durationMinutes
                  Text(
                    text = "$dur mins",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF1E293B)
                    )
                  )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))

                // Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                  )
                  val status = activeRecord?.status ?: if (journeySession.isMonitoring) "ACTIVE" else "COMPLETED"
                  Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (status == "COMPLETED") Color(0xFF059669) else Color(0xFF2563EB)
                    )
                  )
                }
              }

              // Action buttons row: "View All Records" + "Emergency SOS"
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = { selectedTab = 0 },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("button_back_to_records"),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.FormatListBulleted,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                      tint = Color(0xFF475569)
                    )
                    Text(
                      text = "Records List",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                      )
                    )
                  }
                }

                Button(
                  onClick = onEmergencyClick,
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("button_map_emergency"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                  )
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Emergency,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                      tint = Color.White
                    )
                    Text(
                      text = "SOS Alert",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
}

/**
 * Summary metrics card highlighting total journeys, total kilometers walked, and safety score.
 */
@Composable
private fun JourneySummaryMetricsCard(
  totalTrips: Int,
  totalKm: Double,
  safePercentage: Int
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
    shadowElevation = 1.dp,
    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("journey_summary_metrics_card")
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Route,
              contentDescription = null,
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(20.dp)
            )
          }
          Text(
            text = "Your Journey Summary",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E293B)
            )
          )
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xFFF1F5F9)
        ) {
          Text(
            text = "Lifetime Log",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color(0xFF64748B),
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Metric 1: Total Trips
        MetricItem(
          value = "$totalTrips",
          unit = "trips",
          label = "Completed",
          icon = Icons.Default.CheckCircle,
          accentColor = Color(0xFF2563EB),
          modifier = Modifier.weight(1f)
        )

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .align(Alignment.CenterVertically)
            .background(Color(0xFFF1F5F9))
        )

        // Metric 2: Distance
        MetricItem(
          value = String.format(Locale.US, "%.1f", totalKm),
          unit = "km",
          label = "Distance",
          icon = Icons.Default.DirectionsWalk,
          accentColor = Color(0xFF0D9488),
          modifier = Modifier.weight(1f)
        )

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .align(Alignment.CenterVertically)
            .background(Color(0xFFF1F5F9))
        )

        // Metric 3: Safety Percentage
        MetricItem(
          value = "$safePercentage%",
          unit = "safe",
          label = "Safety Score",
          icon = Icons.Default.Shield,
          accentColor = Color(0xFF059669),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun MetricItem(
  value: String,
  unit: String,
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = Color(0xFF0F172A)
        )
      )
      Text(
        text = unit,
        style = MaterialTheme.typography.labelSmall.copy(
          color = Color(0xFF64748B),
          fontSize = 11.sp
        ),
        modifier = Modifier.padding(bottom = 2.dp)
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(12.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(
          color = Color(0xFF64748B),
          fontSize = 12.sp
        )
      )
    }
  }
}

/**
 * Live journey monitoring banner if a session is currently active.
 */
@Composable
private fun ActiveMonitoringBanner(
  journeySession: JourneySession,
  onNavigateToActive: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFFEFF6FF),
    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onNavigateToActive() }
      .testTag("banner_active_journey")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF2563EB))
        )
        Column {
          Text(
            text = "LIVE JOURNEY IN PROGRESS",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color(0xFF1D4ED8),
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          )
          Text(
            text = "En route to ${journeySession.destination}",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF1E293B)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF2563EB)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "View Live",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }
  }
}

/**
 * Individual Historical Journey Record Card displaying route origins, destinations,
 * timestamps, distance, duration, safety score badge, and "View on Map" action.
 */
@Composable
private fun JourneyRecordCard(
  record: JourneyRecord,
  isSelected: Boolean,
  onViewOnMap: () -> Unit,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White,
    shadowElevation = if (isSelected) 3.dp else 1.dp,
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("journey_record_card_${record.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Destination Header & Status Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = record.destination,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = Color(0xFF0F172A)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Status Badge
        val statusBg = when (record.status) {
          "COMPLETED" -> Color(0xFFDCFCE7)
          "ACTIVE" -> Color(0xFFDBEAFE)
          else -> Color(0xFFFEF3C7)
        }
        val statusTextColor = when (record.status) {
          "COMPLETED" -> Color(0xFF166534)
          "ACTIVE" -> Color(0xFF1E40AF)
          else -> Color(0xFF92400E)
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = statusBg
        ) {
          Text(
            text = record.status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              color = statusTextColor,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            )
          )
        }
      }

      // 2. Timeline visual (Start location -> Destination)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Color(0xFF94A3B8))
          )
          Box(
            modifier = Modifier
              .width(2.dp)
              .height(18.dp)
              .background(Color(0xFFE2E8F0))
          )
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .border(1.5.dp, Color(0xFF2563EB), CircleShape)
              .background(Color.White)
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "From: ${record.startLocation}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFF64748B),
              fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "To: ${record.destination}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFF1E293B),
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // 3. Date & Time row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CalendarToday,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(13.dp)
        )
        Text(
          text = "${record.getRelativeDate()} • ${record.getFormattedDate()}",
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color(0xFF64748B),
            fontSize = 12.sp
          )
        )
      }

      // 4. Metrics row with Distance, Duration, Safety Score
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Distance
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsWalk,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = String.format(Locale.US, "%.1f km", record.distanceKm),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E293B)
            )
          )
        }

        // Duration
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "${record.durationMinutes} min",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E293B)
            )
          )
        }

        // Safety Score
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color(0xFF059669),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "${record.safetyScore}/100 Safe",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color(0xFF065F46)
            )
          )
        }
      }

      // 5. Action: View Route on Map button
      OutlinedButton(
        onClick = onViewOnMap,
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp)
          .testTag("button_view_map_${record.id}"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = Color(0xFF2563EB)
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "View Route on Map",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyJourneyRecordsView(
  onStartJourney: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White,
    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(Color(0xFFEFF6FF)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DirectionsWalk,
          contentDescription = null,
          tint = Color(0xFF2563EB),
          modifier = Modifier.size(32.dp)
        )
      }

      Text(
        text = "No Previous Journeys Yet",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = Color(0xFF1E293B)
        )
      )

      Text(
        text = "When you start and complete safe journeys, detailed records, distance metrics, and route maps will be preserved here.",
        style = MaterialTheme.typography.bodySmall.copy(
          color = Color(0xFF64748B),
          lineHeight = 18.sp
        ),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Button(
        onClick = onStartJourney,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF2563EB),
          contentColor = Color.White
        )
      ) {
        Text("Start First Journey")
      }
    }
  }
}
