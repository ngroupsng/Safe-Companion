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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.model.JourneyStop
import com.example.model.TrustedContact
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.theme.*

@Composable
fun JourneySetupScreen(
  trustedContacts: List<TrustedContact>,
  selectedContactIds: Set<String>,
  userLocation: com.example.model.UserLocationState = com.example.model.UserLocationState(),
  routeOptions: List<com.example.model.ScoredRouteOption> = emptyList(),
  selectedRouteOption: com.example.model.ScoredRouteOption? = null,
  isLoadingRoutes: Boolean = false,
  savedPlaces: List<com.example.model.SavedPlace> = emptyList(),
  onRequestLocationPermission: () -> Unit = {},
  onRefreshLocation: () -> Unit = {},
  onBackClick: () -> Unit,
  onStartJourney: (String, String, String, List<JourneyStop>) -> Unit,
  onToggleContactSelection: (String) -> Unit,
  onSelectRouteOption: (com.example.model.ScoredRouteOption) -> Unit = {},
  onDestinationChanged: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var startLocation by remember(userLocation.address, userLocation.hasRealFix) {
    mutableStateOf(
      if (userLocation.hasRealFix && userLocation.address.isNotBlank()) userLocation.address else "Current Location (GPS)"
    )
  }
  var destination by remember { mutableStateOf("Home") }
  var note by remember { mutableStateOf("Taking the evening bus route #42") }
  var stops by remember { mutableStateOf(listOf<JourneyStop>()) }

  // Map Picker State: for startLocation, destination, or an intermediate stop index
  var showMapPickerFor by remember { mutableStateOf<String?>(null) } // "start", "destination", "stop_{index}"
  var mapPickerTargetIndex by remember { mutableIntStateOf(-1) }

  // Dynamic ETA calculation including stops stay durations
  val baseTravelMinutes = selectedRouteOption?.etaMinutes ?: 18
  val additionalTravelPerStop = 7 // estimated transit travel between stops
  val totalTravelMinutes = baseTravelMinutes + (stops.size * additionalTravelPerStop)
  val totalStaysMinutes = stops.sumOf { it.stayDurationMinutes }
  val totalTripEtaMinutes = totalTravelMinutes + totalStaysMinutes

  LaunchedEffect(destination) {
    if (destination.isNotBlank()) {
      onDestinationChanged(destination)
    }
  }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Plan your journey",
        onBackClick = onBackClick
      )
    },
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
          Button(
            onClick = {
              if (!userLocation.isPermissionGranted) {
                onRequestLocationPermission()
              }
              onStartJourney(startLocation, destination, note, stops)
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("button_start_journey_confirm"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Primary,
              contentColor = OnPrimary
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = if (stops.isNotEmpty()) "Start Journey (${stops.size + 1} Stops • ~${totalTripEtaMinutes}m)" else "Start Journey",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
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
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Accurate Location Status Banner / Permission Prompt
      if (!userLocation.isPermissionGranted) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFFEFF6FF),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFDBEAFE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Accurate Location Needed",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1E293B)
                )
              )
              Text(
                text = "Grant GPS permission so your companion map & contacts track your real-time path accurately.",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = Color(0xFF64748B),
                  fontSize = 12.sp
                )
              )
            }
            Button(
              onClick = onRequestLocationPermission,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
              )
            ) {
              Text("Allow", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      } else {
        // High accuracy GPS badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFF0FDF4),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = if (userLocation.isLocating) "Acquiring GPS fix..." else "Accurate GPS Active",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF15803D)
                )
              )
              if (userLocation.accuracyMeters > 0) {
                Text(
                  text = "±${userLocation.accuracyMeters.toInt()}m",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF16A34A),
                    fontSize = 11.sp
                  )
                )
              }
            }

            TextButton(
              onClick = onRefreshLocation,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Text(
                text = "Refresh",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Primary,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }
        }
      }

      // Location Input Group Card (with Multi-Destination & Intermediate Stops support)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Starting Point
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Primary)
            )
            OutlinedTextField(
              value = startLocation,
              onValueChange = { startLocation = it },
              label = { Text("Starting point") },
              trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      showMapPickerFor = "start"
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Map,
                      contentDescription = "Pick Starting Point on Map",
                      tint = Primary
                    )
                  }
                  IconButton(
                    onClick = {
                      if (!userLocation.isPermissionGranted) {
                        onRequestLocationPermission()
                      } else {
                        onRefreshLocation()
                        if (userLocation.hasRealFix && userLocation.address.isNotBlank()) {
                          startLocation = userLocation.address
                        }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.MyLocation,
                      contentDescription = "Fetch Current GPS Location",
                      tint = Primary
                    )
                  }
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_start_location"),
              shape = RoundedCornerShape(12.dp)
            )
          }

          // Connector line
          Box(
            modifier = Modifier
              .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
              .width(2.dp)
              .height(18.dp)
              .background(OutlineVariant)
          )

          // Intermediate Stops (if any)
          stops.forEachIndexed { index, stop ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SurfaceContainerLow,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("card_stop_$index")
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Stop Number Badge
                  Surface(
                    shape = CircleShape,
                    color = Color(0xFFD97706),
                    modifier = Modifier.size(24.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = Color.White,
                          fontWeight = FontWeight.Bold
                        )
                      )
                    }
                  }

                  // Stop Destination Name
                  OutlinedTextField(
                    value = stop.name,
                    onValueChange = { newName ->
                      stops = stops.toMutableList().also {
                        it[index] = it[index].copy(name = newName)
                      }
                    },
                    label = { Text("Stop ${index + 1} destination") },
                    placeholder = { Text("e.g. Coffee Shop, Grocery") },
                    trailingIcon = {
                      IconButton(
                        onClick = {
                          mapPickerTargetIndex = index
                          showMapPickerFor = "stop_$index"
                        }
                      ) {
                        Icon(
                          imageVector = Icons.Default.Map,
                          contentDescription = "Pick Stop ${index + 1} on Map",
                          tint = Primary
                        )
                      }
                    },
                    singleLine = true,
                    modifier = Modifier
                      .weight(1f)
                      .testTag("input_stop_name_$index"),
                    shape = RoundedCornerShape(10.dp)
                  )

                  // Remove Stop Button
                  IconButton(
                    onClick = {
                      stops = stops.toMutableList().also { it.removeAt(index) }
                    },
                    modifier = Modifier.testTag("button_remove_stop_$index")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Remove Stop ${index + 1}",
                      tint = OnSurfaceVariant,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stay Duration Input & Quick Chips
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "Stay time:",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = OnSurfaceVariant
                    )
                  )

                  OutlinedTextField(
                    value = if (stop.stayDurationMinutes > 0) stop.stayDurationMinutes.toString() else "",
                    onValueChange = { input ->
                      val parsed = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                      stops = stops.toMutableList().also {
                        it[index] = it[index].copy(stayDurationMinutes = parsed.coerceIn(0, 480))
                      }
                    },
                    suffix = { Text("min", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                      .width(96.dp)
                      .testTag("input_stop_stay_minutes_$index"),
                    shape = RoundedCornerShape(8.dp)
                  )

                  // Quick duration chips for stop stay
                  Row(
                    modifier = Modifier
                      .weight(1f)
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    listOf(5, 10, 15, 30, 45).forEach { mins ->
                      FilterChip(
                        selected = (stop.stayDurationMinutes == mins),
                        onClick = {
                          stops = stops.toMutableList().also {
                            it[index] = it[index].copy(stayDurationMinutes = mins)
                          }
                        },
                        label = { Text("${mins}m", fontSize = 11.sp) },
                        shape = RoundedCornerShape(999.dp)
                      )
                    }
                  }
                }
              }
            }

            // Connector line between stops
            Box(
              modifier = Modifier
                .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                .width(2.dp)
                .height(14.dp)
                .background(OutlineVariant)
            )
          }

          // Add Stop Button & Quick Destination Suggestions
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                stops = stops + JourneyStop(name = "", stayDurationMinutes = 15)
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("button_add_destination_stop"),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AddLocationAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "+ Add Stop / Waypoint",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
          }

          // Connector line
          Box(
            modifier = Modifier
              .padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
              .width(2.dp)
              .height(18.dp)
              .background(OutlineVariant)
          )

          // Final Destination
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Final Destination",
              tint = Error,
              modifier = Modifier.size(22.dp)
            )
            OutlinedTextField(
              value = destination,
              onValueChange = { destination = it },
              label = { Text(if (stops.isNotEmpty()) "Final destination" else "Where are you going?") },
              trailingIcon = {
                IconButton(
                  onClick = {
                    showMapPickerFor = "destination"
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Pick Final Destination on Map",
                    tint = Primary
                  )
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_destination"),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }
      }

      // Quick Stop & Destination presets
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = if (stops.isNotEmpty()) "Quick add intermediate stop:" else "Quick destinations:",
          style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (stops.isNotEmpty()) {
            listOf("Coffee Shop", "Grocery Store", "Pharmacy", "Library", "Friend's Place", "ATM / Bank", "Gym").forEach { stopPreset ->
              SuggestionChip(
                onClick = {
                  stops = stops + JourneyStop(name = stopPreset, stayDurationMinutes = 15)
                },
                label = { Text("+ $stopPreset") },
                icon = {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFD97706)
                  )
                },
                shape = RoundedCornerShape(999.dp)
              )
            }
          } else {
            // Dynamically combine saved places (Home, Work, etc.) with default labels
            val dynamicPresets = mutableListOf<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>()
            dynamicPresets.add("Current Location" to Icons.Default.MyLocation)

            if (savedPlaces.isNotEmpty()) {
              savedPlaces.forEach { place ->
                val icon = when (place.category) {
                  com.example.model.PlaceCategory.HOME -> Icons.Default.Home
                  com.example.model.PlaceCategory.WORK -> Icons.Default.Work
                  com.example.model.PlaceCategory.COLLEGE -> Icons.Default.School
                  com.example.model.PlaceCategory.GYM -> Icons.Default.FitnessCenter
                  else -> Icons.Default.Place
                }
                val label = place.title.ifBlank { place.category.displayName }
                dynamicPresets.add(label to icon)
              }
            } else {
              dynamicPresets.add("Home" to Icons.Default.Home)
              dynamicPresets.add("Work" to Icons.Default.Work)
              dynamicPresets.add("University Library" to Icons.Default.School)
              dynamicPresets.add("Gym" to Icons.Default.FitnessCenter)
            }

            dynamicPresets.forEach { (label, icon) ->
              SuggestionChip(
                onClick = {
                  if (label == "Current Location") {
                    startLocation = label
                  } else {
                    val matchingPlace = savedPlaces.firstOrNull { it.title.equals(label, ignoreCase = true) || (it.isHome && label == "Home") || (it.isWork && label == "Work") }
                    destination = matchingPlace?.address?.takeIf { it.isNotBlank() } ?: label
                  }
                },
                label = { Text(label) },
                icon = {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Primary
                  )
                },
                shape = RoundedCornerShape(999.dp)
              )
            }
          }
        }
      }

      // Multi-Destination Dynamic ETA & Route Overview Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (stops.isNotEmpty()) Color(0xFFF8FAFC) else SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (stops.isNotEmpty()) Primary.copy(alpha = 0.4f) else OutlineVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_multi_destination_summary")
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = if (stops.isNotEmpty()) "MULTI-DESTINATION TRIP & ETA" else "JOURNEY ETA ESTIMATE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  color = OnSurfaceVariant
                )
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (stops.isNotEmpty()) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh
            ) {
              Text(
                text = if (stops.isNotEmpty()) "${stops.size} intermediate stop${if (stops.size > 1) "s" else ""}" else "Direct route",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (stops.isNotEmpty()) Primary else OnSurfaceVariant,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          // Total ETA and Breakdown
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Column {
              Text(
                text = "~$totalTripEtaMinutes min",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  color = Primary
                ),
                modifier = Modifier.testTag("text_total_eta_breakdown")
              )
              Text(
                text = if (stops.isNotEmpty()) {
                  "Travel: ~$totalTravelMinutes min • Planned stays: $totalStaysMinutes min"
                } else {
                  "Direct travel estimate based on safe route"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                  color = OnSurfaceVariant,
                  fontSize = 12.sp
                )
              )
            }

            if (stops.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                  Text(
                    text = "${totalStaysMinutes}m at stops",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFB45309),
                      fontSize = 11.sp
                    )
                  )
                }
              }
            }
          }

          // Stop Sequence Preview ribbon
          if (stops.isNotEmpty()) {
            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Start",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Primary)
              )
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = OnSurfaceVariant)

              stops.forEachIndexed { i, s ->
                val label = s.name.ifBlank { "Stop ${i + 1}" }
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = Color(0xFFFEF3C7)
                ) {
                  Text(
                    text = "$label (${s.stayDurationMinutes}m)",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = Color(0xFF92400E),
                      fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = OnSurfaceVariant)
              }

              Text(
                text = destination.ifBlank { "End" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Error)
              )
            }
          }
        }
      }

      // Community Route Safety & Recommendation Section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("section_community_route_safety"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = Primary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "COMMUNITY ROUTE SAFETY",
              style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                letterSpacing = 0.5.sp
              )
            )
          }
          if (isLoadingRoutes) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Primary
              )
              Text(
                text = "Scoring...",
                style = MaterialTheme.typography.labelSmall.copy(color = Primary)
              )
            }
          }
        }

        if (routeOptions.isNotEmpty()) {
          // Recommended Route Highlight Card
          val recommendedRoute = routeOptions.firstOrNull { it.isRecommended } ?: routeOptions.first()
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (recommendedRoute.safetyScore >= 80) Color(0xFFF0FDF4) else SurfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
              width = 1.dp,
              color = if (recommendedRoute.safetyScore >= 80) Color(0xFF86EFAC) else OutlineVariant
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("card_route_recommendation")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (recommendedRoute.safetyScore >= 80) Icons.Default.VerifiedUser else Icons.Default.Info,
                contentDescription = null,
                tint = if (recommendedRoute.safetyScore >= 80) Color(0xFF16A34A) else Primary,
                modifier = Modifier.size(22.dp)
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = if (recommendedRoute.isRecommended) "Safer route based on community reports" else "Route safety assessment",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (recommendedRoute.safetyScore >= 80) Color(0xFF166534) else OnSurface
                  )
                )
                Text(
                  text = recommendedRoute.recommendationReason.ifBlank { "Evaluated against active verified community hazards." },
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = OnSurfaceVariant,
                    fontSize = 12.sp
                  )
                )
              }
            }
          }

          // Route Candidates List
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            routeOptions.forEach { option ->
              val isSelected = selectedRouteOption?.id == option.id || (selectedRouteOption == null && option.isRecommended)
              val scoreColor = when {
                option.safetyScore >= 80 -> Color(0xFF16A34A)
                option.safetyScore >= 60 -> Color(0xFFD97706)
                else -> Color(0xFFDC2626)
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PrimaryContainer.copy(alpha = 0.12f) else SurfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectRouteOption(option) }
                  .testTag("route_option_${option.id}")
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
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(
                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) Primary else OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                      )
                      Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = OnSurface
                        )
                      )
                      if (option.isRecommended) {
                        Surface(
                          shape = RoundedCornerShape(999.dp),
                          color = Color(0xFFDCFCE7)
                        ) {
                          Text(
                            text = "RECOMMENDED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 10.sp,
                              fontWeight = FontWeight.ExtraBold,
                              color = Color(0xFF166534)
                            )
                          )
                        }
                      }
                    }

                    // Safety Score Badge
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = scoreColor.copy(alpha = 0.12f),
                      border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f))
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Shield,
                          contentDescription = null,
                          tint = scoreColor,
                          modifier = Modifier.size(13.dp)
                        )
                        Text(
                          text = "Score: ${option.safetyScore}/100",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            fontSize = 11.sp
                          )
                        )
                      }
                    }
                  }

                  // Metrics Row: Distance, Duration, Hazard Count
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.DirectionsWalk,
                          contentDescription = null,
                          tint = OnSurfaceVariant,
                          modifier = Modifier.size(15.dp)
                        )
                        Text(
                          text = "${option.distanceKm} km",
                          style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface
                          )
                        )
                      }
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.AccessTime,
                          contentDescription = null,
                          tint = OnSurfaceVariant,
                          modifier = Modifier.size(15.dp)
                        )
                        Text(
                          text = "~${option.etaMinutes} min",
                          style = MaterialTheme.typography.bodySmall.copy(
                            color = OnSurfaceVariant
                          )
                        )
                      }
                    }

                    // Hazard Status Text
                    Text(
                      text = if (option.hazardCountNearby == 0) {
                        "✓ 0 hazards nearby"
                      } else {
                        "⚠️ ${option.hazardCountNearby} hazard(s) nearby"
                      },
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = if (option.hazardCountNearby == 0) Color(0xFF16A34A) else Color(0xFFD97706),
                        fontWeight = FontWeight.Medium
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Sharing With Section
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "SHARING WITH",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurfaceVariant,
              letterSpacing = 0.5.sp
            )
          )
          Text(
            text = "Tap to toggle",
            style = MaterialTheme.typography.labelSmall.copy(color = Primary)
          )
        }

        // Contact chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          trustedContacts.forEach { contact ->
            val isSelected = selectedContactIds.contains(contact.id)
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerLowest,
              border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isSelected) Primary else OutlineVariant
              ),
              modifier = Modifier
                .clickable { onToggleContactSelection(contact.id) }
                .testTag("chip_contact_${contact.id}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                  )
                } else {
                  Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Text(
                  text = "${contact.name} (${contact.relationship})",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Primary else OnSurface
                  )
                )
              }
            }
          }
        }
      }

      // Note Field
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "ADD A NOTE (OPTIONAL)",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          placeholder = { Text("e.g. Taking the bus, walking with classmates") },
          modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .testTag("input_journey_note"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainerLowest,
            unfocusedContainerColor = SurfaceContainerLowest
          )
        )
      }

      // Safety Info notice
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = Tertiary,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Your contacts will receive an SMS/link once monitoring starts, and a notification upon your safe arrival.",
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.GpsFixed,
              contentDescription = null,
              tint = Primary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Continuous background tracking keeps you protected even if you lock your screen or switch apps.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Primary,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }
      }
    }
  }

  // Interactive Map Picker Dialog for Start, Destination, or Stops
  if (showMapPickerFor != null) {
    val mode = showMapPickerFor!!
    val (title, initAddr) = when {
      mode == "start" -> "Pick Starting Point" to startLocation
      mode == "destination" -> "Pick Destination" to destination
      mode.startsWith("stop_") -> {
        val idx = mapPickerTargetIndex
        val stopName = stops.getOrNull(idx)?.name ?: ""
        "Pick Stop ${idx + 1}" to stopName
      }
      else -> "Pick Location" to ""
    }

    com.example.ui.components.LocationMapPickerDialog(
      initialTitle = title,
      initialAddress = initAddr,
      initialLat = userLocation.latitude,
      initialLng = userLocation.longitude,
      onDismiss = {
        showMapPickerFor = null
        mapPickerTargetIndex = -1
      },
      onLocationSelected = { address, lat, lng ->
        when {
          mode == "start" -> {
            startLocation = address
          }
          mode == "destination" -> {
            destination = address
          }
          mode.startsWith("stop_") -> {
            val idx = mapPickerTargetIndex
            if (idx in stops.indices) {
              stops = stops.toMutableList().also {
                it[idx] = it[idx].copy(name = address, lat = lat, lng = lng)
              }
            }
          }
        }
        showMapPickerFor = null
        mapPickerTargetIndex = -1
      }
    )
  }
}

