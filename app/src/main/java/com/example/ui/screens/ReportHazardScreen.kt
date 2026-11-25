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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HazardType
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.theme.*

@Composable
fun ReportHazardScreen(
  userLocation: com.example.model.UserLocationState = com.example.model.UserLocationState(),
  onRequestLocationPermission: () -> Unit = {},
  onBackClick: () -> Unit,
  onSubmitReport: (HazardType, String, String, String?) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedType by remember { mutableStateOf(HazardType.BROKEN_STREETLIGHT) }
  var address by remember(userLocation.address, userLocation.hasRealFix) {
    mutableStateOf(if (userLocation.hasRealFix && userLocation.address.isNotBlank()) userLocation.address else "Current GPS Location")
  }
  var description by remember { mutableStateOf("") }
  var hasPhoto by remember { mutableStateOf(false) }

  var hazardLat by remember(userLocation.latitude) { mutableDoubleStateOf(if (userLocation.latitude != 0.0) userLocation.latitude else 37.7749) }
  var hazardLng by remember(userLocation.longitude) { mutableDoubleStateOf(if (userLocation.longitude != 0.0) userLocation.longitude else -122.4194) }
  var showMapPicker by remember { mutableStateOf(false) }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Report a Hazard",
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
            .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
          Button(
            onClick = {
              val photoUrl = if (hasPhoto) "https://lh3.googleusercontent.com/aida-public/AB6AXuCvYrexAljV0X1HRwL7sB0Gy8YApUEOX8_s8zco9iyobfilapZxU4wS8B4Ek4YhDoVl-W9aEoeqSwBnEsEUo0lEunFvYlfNCXJ90V1NAR87oc8cemYzKVo_sHqm8kSW-obst06xC0a4MBSRkAh2KdeXILWwEbmoWotMtDuOFZUoWdPObTRGQUlixBVh0qx8zq1FefrO0aEyPC2BF1XniKhWKLmiwGtDbwKaO-q_J6xK-RUgJg2MD9_aWA" else null
              onSubmitReport(selectedType, address, description, photoUrl)
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("button_submit_hazard_report"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Primary,
              contentColor = OnPrimary
            )
          ) {
            Text(
              text = "Submit Report",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      // Hazard Type Selector Grid
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "HAZARD TYPE",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(HazardType.BROKEN_STREETLIGHT, HazardType.POTHOLE).forEach { type ->
            val isSelected = selectedType == type
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { selectedType = type }
                .testTag("type_${type.name}"),
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerLowest,
              border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.4f)
              )
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = when (type) {
                    HazardType.BROKEN_STREETLIGHT -> Icons.Default.Lightbulb
                    else -> Icons.Default.Warning
                  },
                  contentDescription = null,
                  tint = if (isSelected) Primary else OnSurfaceVariant,
                  modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = type.displayName,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Primary else OnSurface
                  )
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(HazardType.BLOCKED_ROAD, HazardType.UNSAFE_AREA, HazardType.OTHER).forEach { type ->
            val isSelected = selectedType == type
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { selectedType = type }
                .testTag("type_${type.name}"),
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerLowest,
              border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.4f)
              )
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = when (type) {
                    HazardType.BLOCKED_ROAD -> Icons.Default.Block
                    HazardType.UNSAFE_AREA -> Icons.Default.Security
                    else -> Icons.Default.MoreHoriz
                  },
                  contentDescription = null,
                  tint = if (isSelected) Primary else OnSurfaceVariant,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = type.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Primary else OnSurface
                  ),
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      // Location Field
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "LOCATION",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          leadingIcon = {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary)
          },
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  showMapPicker = true
                }
              ) {
                Icon(Icons.Default.Map, contentDescription = "Pick on Map", tint = Primary)
              }
              IconButton(
                onClick = {
                  if (!userLocation.isPermissionGranted) {
                    onRequestLocationPermission()
                  } else if (userLocation.hasRealFix && userLocation.address.isNotBlank()) {
                    address = userLocation.address
                    hazardLat = userLocation.latitude
                    hazardLng = userLocation.longitude
                  } else {
                    address = "GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", userLocation.latitude, userLocation.longitude)}"
                    hazardLat = userLocation.latitude
                    hazardLng = userLocation.longitude
                  }
                }
              ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Use GPS", tint = Primary)
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_hazard_address"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainerLowest,
            unfocusedContainerColor = SurfaceContainerLowest
          )
        )

        // Locate / Adjust Pin on Map Button
        OutlinedButton(
          onClick = { showMapPicker = true },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("button_locate_hazard_on_map")
        ) {
          Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Locate or Adjust on Map", fontWeight = FontWeight.Bold)
        }
      }

      // Description Field
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "DESCRIPTION (OPTIONAL)",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          placeholder = { Text("Describe the hazard, e.g. streetlight is completely unlit, dark alley...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("input_hazard_description"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainerLowest,
            unfocusedContainerColor = SurfaceContainerLowest
          )
        )
      }

      // Photo upload placeholder
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "PHOTO (OPTIONAL)",
          style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (hasPhoto) PrimaryContainer.copy(alpha = 0.1f) else SurfaceContainerLowest,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { hasPhoto = !hasPhoto }
            .testTag("button_toggle_photo_upload")
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
                .background(SurfaceContainerHigh),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (hasPhoto) Icons.Default.Check else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (hasPhoto) "Photo attached (streetlight_corner.jpg)" else "Add photo proof",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = if (hasPhoto) Primary else OnSurface
                )
              )
              Text(
                text = if (hasPhoto) "Tap to remove" else "Helps community members verify faster",
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
              )
            }
          }
        }
      }
    }
  }

  // Interactive Map Picker Dialog for hazard pin
  if (showMapPicker) {
    com.example.ui.components.LocationMapPickerDialog(
      initialTitle = "Locate Hazard on Map",
      initialAddress = address,
      initialLat = hazardLat,
      initialLng = hazardLng,
      onDismiss = { showMapPicker = false },
      onLocationSelected = { pickedAddress, lat, lng ->
        address = pickedAddress
        hazardLat = lat
        hazardLng = lng
        showMapPicker = false
      }
    )
  }
}
