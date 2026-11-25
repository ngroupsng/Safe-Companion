package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.BottomTab
import com.example.model.JourneySession
import com.example.model.TrustedContact
import com.example.ui.components.OpenStreetMapView
import com.example.ui.components.SereneBottomNavBar
import com.example.ui.theme.*

@Composable
fun ActiveJourneyScreen(
  journeySession: JourneySession,
  trustedContacts: List<TrustedContact>,
  currentTab: BottomTab,
  userLocation: com.example.model.UserLocationState = com.example.model.UserLocationState(),
  hazardReports: List<com.example.model.HazardReport> = emptyList(),
  onTabSelected: (BottomTab) -> Unit,
  onBackClick: () -> Unit,
  onSosClick: () -> Unit,
  onCompleteJourney: () -> Unit,
  onStartNextDestination: () -> Unit = {},
  onArriveAtCurrentStop: () -> Unit = {},
  onAcknowledgeDeviation: () -> Unit = {},
  onAcknowledgeLongStop: () -> Unit = {},
  onTriggerDeviation: () -> Unit = {},
  onTriggerLongStop: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var userProgress by remember { mutableStateOf(0.42f) }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp),
        color = MaterialTheme.colorScheme.surface
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = onBackClick,
              modifier = Modifier.testTag("active_journey_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Primary
              )
            }
            Text(
              text = "Safe Companion",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
              )
            )
          }

          // Live status / Deviation / Long stop indicator
          val isDeviatedState = (journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED ||
                                journeySession.routeDeviationState == com.example.model.RouteDeviationState.WARNING) &&
                               !journeySession.isDeviationAcknowledgedByUser

          val isLongStopState = journeySession.longStopState == com.example.model.LongStopState.LONG_STOP_WARNING &&
                               !journeySession.isLongStopAcknowledged

          if (isLongStopState) {
            val stopMins = journeySession.stationaryDurationMinutes.coerceAtLeast(5)
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color(0xFFFFFBEB),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD97706))
                )
                Text(
                  text = "⏸️ Stationary (${stopMins}m)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.ExtraBold
                  )
                )
              }
            }
          } else if (isDeviatedState) {
            val isSevere = journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED
            val badgeColor = if (isSevere) Color(0xFFDC2626) else Color(0xFFD97706)
            val badgeBg = if (isSevere) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
            val badgeText = if (isSevere) "⚠️ Deviation Alert" else "⚠️ Route Warning"
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = badgeBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                )
                Text(
                  text = badgeText,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = badgeColor,
                    fontWeight = FontWeight.ExtraBold
                  )
                )
              }
            }
          } else {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = TertiaryContainer.copy(alpha = 0.15f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TertiaryFixed)
                )
                Text(
                  text = "Live GPS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = TertiaryContainer,
                    fontWeight = FontWeight.Bold
                  )
                )
              }
            }
          }
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
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
    ) {
      val isLongStopActive = journeySession.longStopState == com.example.model.LongStopState.LONG_STOP_WARNING &&
                            !journeySession.isLongStopAcknowledged

      // Free OpenStreetMap with real worldwide street tiles and dynamic journey coordinates
      OpenStreetMapView(
        modifier = Modifier.fillMaxSize(),
        startLat = journeySession.startLat,
        startLng = journeySession.startLng,
        destLat = journeySession.destLat,
        destLng = journeySession.destLng,
        userCurrentLat = userLocation.latitude,
        userCurrentLng = userLocation.longitude,
        accuracyMeters = userLocation.accuracyMeters,
        showRoute = true,
        userProgress = journeySession.progress,
        routePoints = journeySession.roadRoutePoints,
        hazardReports = hazardReports,
        isDeviated = (journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED ||
                      journeySession.routeDeviationState == com.example.model.RouteDeviationState.WARNING) &&
                     !journeySession.isDeviationAcknowledgedByUser,
        isStationary = isLongStopActive,
        stationaryMinutes = journeySession.stationaryDurationMinutes.coerceAtLeast(5)
      )

      // Overlay UI Controls
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Floating Card: Active Journey Summary
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          shadowElevation = 4.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (journeySession.isDestinationReached) TertiaryFixed else Primary)
                )
                Text(
                  text = when {
                    journeySession.isDestinationReached -> "Destination Reached"
                    journeySession.hasMultipleDestinations && journeySession.hasUncompletedStops ->
                      "Stop ${journeySession.currentStopIndex + 1} of ${journeySession.stops.size}"
                    journeySession.hasMultipleDestinations -> "Final Leg"
                    else -> "Active Journey"
                  },
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                  )
                )
              }
              Text(
                text = if (journeySession.hasMultipleDestinations) {
                  "-> ${journeySession.currentTargetName}"
                } else {
                  "${journeySession.startLocation} -> ${journeySession.destination}"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Primary
                ),
                maxLines = 1
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Stats: Distance & ETA (with dynamic Multi-Destination calculation)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              val totalDistanceDisplay = when {
                journeySession.isDestinationReached -> "0 m"
                journeySession.remainingKm < 1.0 -> "${(journeySession.remainingKm * 1000).toInt()} m"
                else -> String.format(java.util.Locale.US, "%.1f km", journeySession.remainingKm)
              }

              val legDistanceDisplay = when {
                journeySession.isDestinationReached -> "0 m"
                journeySession.currentLegDistanceKm < 1.0 -> "${(journeySession.currentLegDistanceKm * 1000).toInt()} m"
                else -> String.format(java.util.Locale.US, "%.1f km", journeySession.currentLegDistanceKm)
              }

              val etaDisplay = when {
                journeySession.isDestinationReached -> "0 min"
                journeySession.etaMinutes > 0 -> "~${journeySession.etaMinutes} min"
                else -> "< 1 min"
              }

              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = if (journeySession.hasMultipleDestinations && journeySession.hasUncompletedStops) "LEG DISTANCE" else "DISTANCE REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.sp,
                      color = OnSurfaceVariant,
                      fontWeight = FontWeight.Bold
                    )
                  )
                  Text(
                    text = if (journeySession.hasMultipleDestinations && journeySession.hasUncompletedStops) legDistanceDisplay else totalDistanceDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                      fontSize = 22.sp,
                      color = OnSurface
                    )
                  )
                  if (journeySession.hasMultipleDestinations && journeySession.hasUncompletedStops) {
                    Text(
                      text = "Total trip: $totalDistanceDisplay",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = OnSurfaceVariant
                      )
                    )
                  }
                }
              }

              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = if (journeySession.hasMultipleDestinations) "TOTAL TRIP ETA" else "ESTIMATED TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.sp,
                      color = OnSurfaceVariant,
                      fontWeight = FontWeight.Bold
                    )
                  )
                  Text(
                    text = etaDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                      fontSize = 22.sp,
                      color = Primary
                    )
                  )
                  if (journeySession.hasMultipleDestinations && journeySession.hasUncompletedStops) {
                    val remainingStays = journeySession.stops.drop(journeySession.currentStopIndex).sumOf { it.stayDurationMinutes }
                    Text(
                      text = "Leg ~${journeySession.currentLegEtaMinutes}m • Stays: ${remainingStays}m",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = OnSurfaceVariant
                      )
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Route Safety Score & Community Hazard Assessment
            val scoreColor = when {
              journeySession.safetyScore >= 80 -> Color(0xFF16A34A)
              journeySession.safetyScore >= 60 -> Color(0xFFD97706)
              else -> Color(0xFFDC2626)
            }
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("active_journey_safety_card"),
              shape = RoundedCornerShape(10.dp),
              color = if (journeySession.safetyScore >= 80) Color(0xFFF0FDF4) else SurfaceContainerLow,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (journeySession.safetyScore >= 80) Color(0xFF86EFAC) else OutlineVariant
              )
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(15.dp)
                  )
                  Text(
                    text = "Safety Score: ${journeySession.safetyScore}/100",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (journeySession.safetyScore >= 80) Color(0xFF166534) else OnSurface
                    )
                  )
                }
                Text(
                  text = if (journeySession.nearbyHazardsCount == 0) {
                    "✓ 0 hazards on corridor"
                  } else {
                    "⚠️ ${journeySession.nearbyHazardsCount} hazard(s) nearby"
                  },
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (journeySession.nearbyHazardsCount == 0) Color(0xFF16A34A) else Color(0xFFD97706),
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Background Monitoring Active pill indicator
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Background GPS active • Tracking continues with screen locked",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color(0xFF16A34A),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }
        }

        // Bottom Controls Overlay
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val isDeviatedAlertActive = (journeySession.routeDeviationState == com.example.model.RouteDeviationState.WARNING ||
                                       journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED) &&
                                      !journeySession.isDeviationAcknowledgedByUser

          val isLongStopAlertActive = journeySession.longStopState == com.example.model.LongStopState.LONG_STOP_WARNING &&
                                      !journeySession.isLongStopAcknowledged

          // Show Long Stop Alert Card ONLY when the real long-stop detection condition is active
          if (isLongStopAlertActive) {
            val stopMins = journeySession.stationaryDurationMinutes.coerceAtLeast(5)
            val accentColor = Color(0xFFD97706)
            val cardBg = Color(0xFFFFFBEB)

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = cardBg,
              shadowElevation = 8.dp,
              border = androidx.compose.foundation.BorderStroke(2.dp, accentColor),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("long_stop_warning_prompt_card")
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Stationary Alert",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Long Stop Alert!",
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = accentColor
                      )
                    )
                    Text(
                      text = "Stationary for ~$stopMins min • No movement",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                      )
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor
                  ) {
                    Text(
                      text = "INACTIVE",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = "You have remained stationary at this location for ~$stopMins minutes. Are you safe, or do you need assistance?",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1E293B),
                    lineHeight = 20.sp
                  )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Button(
                    onClick = onAcknowledgeLongStop,
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .testTag("button_long_stop_im_safe"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF16A34A),
                      contentColor = Color.White
                    )
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Yes, I'm Safe",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                  }

                  Button(
                    onClick = onSosClick,
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .testTag("button_long_stop_sos"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFFDC2626),
                      contentColor = Color.White
                    )
                  ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "I Need Help",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                  }
                }
              }
            }
          }

          // Show Route Deviation Card ONLY when the real route-deviation detection condition is active
          if (isDeviatedAlertActive) {
            val isSevere = journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED
            val cardBg = if (isSevere) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
            val accentColor = if (isSevere) Color(0xFFDC2626) else Color(0xFFD97706)
            val devMeters = journeySession.deviationDistanceMeters.toInt().coerceAtLeast(50)
            val titleText = if (isSevere) "Route Deviation Alert!" else "Route Deviation Warning"
            val badgeText = if (isSevere) "ALERT" else "WARNING"
            val bodyText = if (isSevere) {
              "You appear to have deviated ~${devMeters}m from your expected road route. Are you still safe, or did you make an unplanned detour?"
            } else {
              "You appear to have deviated ~${devMeters}m from your planned route. Are you still safe, or did you take a detour?"
            }

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = cardBg,
              shadowElevation = 8.dp,
              border = androidx.compose.foundation.BorderStroke(2.dp, accentColor),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("deviation_warning_prompt_card")
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = titleText,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = accentColor
                      )
                    )
                    Text(
                      text = "${devMeters}m off planned route",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                      )
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor
                  ) {
                    Text(
                      text = badgeText,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = bodyText,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1E293B),
                    lineHeight = 20.sp
                  )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Button(
                    onClick = onAcknowledgeDeviation,
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .testTag("button_im_safe"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF16A34A),
                      contentColor = Color.White
                    )
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Yes, I'm Safe",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                  }

                  Button(
                    onClick = onSosClick,
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .testTag("button_deviation_sos"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFFDC2626),
                      contentColor = Color.White
                    )
                  ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "I Need Help",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                  }
                }
              }
            }
          }

          // Multi-Destination Itinerary & Stop Advancement Card (shown if multiple stops exist)
          if (journeySession.hasMultipleDestinations) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = SurfaceContainerLowest,
              shadowElevation = 3.dp,
              border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("card_multi_destination_active_progress")
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Card Header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.AltRoute,
                      contentDescription = null,
                      tint = Primary,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = "MULTI-DESTINATION ITINERARY",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant,
                        letterSpacing = 0.5.sp
                      )
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (journeySession.hasUncompletedStops) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                  ) {
                    Text(
                      text = if (journeySession.hasUncompletedStops) "Stop ${journeySession.currentStopIndex + 1} of ${journeySession.stops.size}" else "All Stops Visited",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (journeySession.hasUncompletedStops) Color(0xFFB45309) else Color(0xFF15803D),
                        fontSize = 11.sp
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }

                // Stops horizontal sequence view
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  // Start point
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceContainerHigh
                  ) {
                    Text(
                      text = "Start",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }

                  Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = OnSurfaceVariant)

                  journeySession.stops.forEachIndexed { i, stop ->
                    val isPast = i < journeySession.currentStopIndex
                    val isCurrent = i == journeySession.currentStopIndex
                    val stopColor = when {
                      isPast -> Color(0xFFDCFCE7)
                      isCurrent -> Color(0xFFFEF3C7)
                      else -> SurfaceContainerHigh
                    }
                    val textColor = when {
                      isPast -> Color(0xFF15803D)
                      isCurrent -> Color(0xFFB45309)
                      else -> OnSurfaceVariant
                    }
                    val iconVector = when {
                      isPast -> Icons.Default.Check
                      isCurrent && journeySession.isAtStop -> Icons.Default.HourglassTop
                      isCurrent -> Icons.Default.DirectionsWalk
                      else -> Icons.Default.Place
                    }

                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = stopColor,
                      border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)) else null
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(iconVector, contentDescription = null, modifier = Modifier.size(12.dp), tint = textColor)
                        Text(
                          text = "${stop.name.ifBlank { "Stop ${i + 1}" }} (${stop.stayDurationMinutes}m)",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            fontSize = 11.sp
                          )
                        )
                      }
                    }

                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = OnSurfaceVariant)
                  }

                  // Final destination
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (!journeySession.hasUncompletedStops) Color(0xFFFEE2E2) else SurfaceContainerHigh
                  ) {
                    Text(
                      text = journeySession.destination,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (!journeySession.hasUncompletedStops) Error else OnSurfaceVariant,
                        fontSize = 11.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                // Stop Action Controls (if intermediate stops remain)
                if (journeySession.hasUncompletedStops) {
                  val currentStop = journeySession.stops[journeySession.currentStopIndex]
                  val nextTarget = journeySession.nextTargetNameAfterCurrent ?: journeySession.destination

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (journeySession.isAtStop) Color(0xFFFFFBEB) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (journeySession.isAtStop) Color(0xFFFDE68A) else Primary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Icon(
                          imageVector = if (journeySession.isAtStop) Icons.Default.HourglassBottom else Icons.Default.NearMe,
                          contentDescription = null,
                          tint = if (journeySession.isAtStop) Color(0xFFD97706) else Primary,
                          modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = if (journeySession.isAtStop) {
                              "At Stop: ${currentStop.name.ifBlank { "Stop ${journeySession.currentStopIndex + 1}" }}"
                            } else {
                              "Heading to: ${currentStop.name.ifBlank { "Stop ${journeySession.currentStopIndex + 1}" }}"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                          )
                          Text(
                            text = if (journeySession.isAtStop) {
                              "Planned stay: ${currentStop.stayDurationMinutes}m • Tap below when ready to depart."
                            } else {
                              "Leg: ~${journeySession.currentLegEtaMinutes}m • Planned stay on arrival: ${currentStop.stayDurationMinutes}m"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = OnSurfaceVariant)
                          )
                        }
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        if (!journeySession.isAtStop) {
                          OutlinedButton(
                            onClick = onArriveAtCurrentStop,
                            modifier = Modifier
                              .weight(1f)
                              .height(38.dp)
                              .testTag("button_arrive_at_stop"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                          ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("I've Arrived", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                          }
                        }

                        Button(
                          onClick = onStartNextDestination,
                          modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .testTag("button_start_next_destination"),
                          shape = RoundedCornerShape(8.dp),
                          colors = ButtonDefaults.buttonColors(
                            containerColor = if (journeySession.isAtStop) Color(0xFFD97706) else Primary
                          ),
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                          Text(
                            text = "Next: $nextTarget",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Safety Status Card
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            shadowElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val (statusDotColor, statusText, statusTextColor) = when {
                  journeySession.routeDeviationState == com.example.model.RouteDeviationState.DEVIATED -> Triple(
                    Error,
                    "Route Deviation (${journeySession.deviationDistanceMeters.toInt()}m off)",
                    Error
                  )
                  journeySession.longStopState == com.example.model.LongStopState.LONG_STOP_WARNING -> Triple(
                    Color(0xFFD97706),
                    "Long Stop (${journeySession.stationaryDurationMinutes}m stationary)",
                    Color(0xFFD97706)
                  )
                  journeySession.longStopState == com.example.model.LongStopState.STATIONARY && journeySession.stationaryDurationMinutes >= 1 -> Triple(
                    Color(0xFFF59E0B),
                    "Stationary (${journeySession.stationaryDurationMinutes}m)",
                    Color(0xFFD97706)
                  )
                  journeySession.routeDeviationState == com.example.model.RouteDeviationState.WARNING -> Triple(
                    Color(0xFFF59E0B),
                    "Unusual Route Change (${journeySession.deviationDistanceMeters.toInt()}m)",
                    Color(0xFFD97706)
                  )
                  else -> Triple(
                    TertiaryFixed,
                    "Monitoring Active • On Route",
                    Tertiary
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(statusDotColor)
                  )
                  Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = statusTextColor
                    )
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceContainerHigh
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(12.dp), tint = Primary)
                      Text("Online", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceContainerHigh
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(12.dp), tint = Tertiary)
                      Text("${journeySession.batteryPercent}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Notified circle contacts
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "Sharing with:",
                  style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                )
                if (trustedContacts.isEmpty()) {
                  Text(
                    text = "None (Private mode)",
                    style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                  )
                } else {
                  trustedContacts.take(2).forEach { contact ->
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = PrimaryContainer.copy(alpha = 0.1f)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                        Text(contact.name.split(" ").first(), style = MaterialTheme.typography.labelSmall.copy(color = Primary))
                      }
                    }
                  }
                  if (trustedContacts.size > 2) {
                    Text(
                      text = "+${trustedContacts.size - 2}",
                      style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
                    )
                  }
                }
              }
            }
          }

          // Safe Arrival Button & Emergency SOS Button Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Safe Arrival Complete Journey Button
            Button(
              onClick = onCompleteJourney,
              modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp))
                .testTag("button_complete_journey"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "I've Arrived Safely",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                )
              }
            }

            // Red Emergency SOS Button
            Button(
              onClick = onSosClick,
              modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
                .testTag("button_emergency_sos"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Error,
                contentColor = OnError
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(
                  text = "EMERGENCY SOS",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
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
