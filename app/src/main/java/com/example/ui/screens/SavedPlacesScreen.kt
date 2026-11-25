package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.model.PlaceCategory
import com.example.model.SavedPlace
import com.example.ui.components.LocationMapPickerDialog
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun SavedPlacesScreen(
  savedPlaces: List<SavedPlace>,
  onBackClick: () -> Unit,
  onAddOrUpdatePlace: (SavedPlace) -> Unit,
  onDeletePlace: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showEditDialog by remember { mutableStateOf(false) }
  var placeToEdit by remember { mutableStateOf<SavedPlace?>(null) }
  var showMapPicker by remember { mutableStateOf(false) }
  var targetPlaceForMap by remember { mutableStateOf<SavedPlace?>(null) }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Saved Places & Addresses",
        onBackClick = onBackClick
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          placeToEdit = SavedPlace(
            title = "",
            address = "",
            latitude = 37.7749,
            longitude = -122.4194,
            category = PlaceCategory.OTHER,
            isHome = false,
            isWork = false
          )
          showEditDialog = true
        },
        containerColor = Primary,
        contentColor = OnPrimary,
        shape = CircleShape,
        modifier = Modifier.testTag("fab_add_saved_place")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add New Address")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = PrimaryContainer.copy(alpha = 0.2f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = Primary,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Configure your Home address and other frequent destinations. Tap the map button on any address to position it visually on the interactive map.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = OnSurfaceVariant,
                lineHeight = 18.sp
              )
            )
          }
        }
      }

      // Home & Work Primary Addresses Section
      val homePlace = savedPlaces.firstOrNull { it.isHome } ?: SavedPlace(
        id = "place_home",
        title = "Home",
        address = "742 Evergreen Terrace, San Francisco, CA",
        latitude = 37.7869,
        longitude = -122.4049,
        category = PlaceCategory.HOME,
        isHome = true
      )

      val workPlace = savedPlaces.firstOrNull { it.isWork } ?: SavedPlace(
        id = "place_work",
        title = "Work Office",
        address = "Market St & 4th St, San Francisco, CA",
        latitude = 37.7749,
        longitude = -122.4194,
        category = PlaceCategory.WORK,
        isWork = true
      )

      val otherPlaces = savedPlaces.filter { !it.isHome && !it.isWork }

      item {
        Text(
          text = "PRIMARY ADDRESSES",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )
      }

      item {
        SavedPlaceCard(
          place = homePlace,
          isPrimaryType = true,
          onEditClick = {
            placeToEdit = homePlace
            showEditDialog = true
          },
          onLocateOnMapClick = {
            targetPlaceForMap = homePlace
            showMapPicker = true
          },
          onDeleteClick = null
        )
      }

      item {
        SavedPlaceCard(
          place = workPlace,
          isPrimaryType = true,
          onEditClick = {
            placeToEdit = workPlace
            showEditDialog = true
          },
          onLocateOnMapClick = {
            targetPlaceForMap = workPlace
            showMapPicker = true
          },
          onDeleteClick = null
        )
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "OTHER SAVED DESTINATIONS",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 0.5.sp
          )
        )
      }

      if (otherPlaces.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AddLocationAlt,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(36.dp)
              )
              Text(
                text = "No other addresses added yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Add places like College/University, Gym, or favorite hangouts for quick 1-tap route selection.",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = OnSurfaceVariant,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              )
            }
          }
        }
      } else {
        items(otherPlaces, key = { it.id }) { place ->
          SavedPlaceCard(
            place = place,
            isPrimaryType = false,
            onEditClick = {
              placeToEdit = place
              showEditDialog = true
            },
            onLocateOnMapClick = {
              targetPlaceForMap = place
              showMapPicker = true
            },
            onDeleteClick = {
              onDeletePlace(place.id)
            }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }

  // Edit / Add Place Dialog
  if (showEditDialog && placeToEdit != null) {
    EditPlaceDialog(
      place = placeToEdit!!,
      onDismiss = {
        showEditDialog = false
        placeToEdit = null
      },
      onSave = { updated ->
        onAddOrUpdatePlace(updated)
        showEditDialog = false
        placeToEdit = null
      },
      onLocateOnMap = { currentPlace ->
        targetPlaceForMap = currentPlace
        showMapPicker = true
      }
    )
  }

  // Interactive Map Location Picker
  if (showMapPicker && targetPlaceForMap != null) {
    val place = targetPlaceForMap!!
    LocationMapPickerDialog(
      initialTitle = "Locate \"${place.title}\"",
      initialAddress = place.address,
      initialLat = place.latitude,
      initialLng = place.longitude,
      onDismiss = {
        showMapPicker = false
        targetPlaceForMap = null
      },
      onLocationSelected = { newAddress, newLat, newLng ->
        val updated = place.copy(
          address = newAddress,
          latitude = newLat,
          longitude = newLng
        )
        onAddOrUpdatePlace(updated)
        // If edit dialog is currently open with same place, update it too
        if (showEditDialog && placeToEdit?.id == place.id) {
          placeToEdit = updated
        }
        showMapPicker = false
        targetPlaceForMap = null
      }
    )
  }
}

@Composable
private fun SavedPlaceCard(
  place: SavedPlace,
  isPrimaryType: Boolean,
  onEditClick: () -> Unit,
  onLocateOnMapClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_saved_place_${place.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = when {
              place.isHome -> Primary.copy(alpha = 0.12f)
              place.isWork -> Color(0xFFD97706).copy(alpha = 0.12f)
              else -> PrimaryFixedDim.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = when (place.category) {
                  PlaceCategory.HOME -> Icons.Default.Home
                  PlaceCategory.WORK -> Icons.Default.Work
                  PlaceCategory.COLLEGE -> Icons.Default.School
                  PlaceCategory.GYM -> Icons.Default.FitnessCenter
                  PlaceCategory.FAVORITE -> Icons.Default.Star
                  PlaceCategory.OTHER -> Icons.Default.Place
                },
                contentDescription = null,
                tint = when {
                  place.isHome -> Primary
                  place.isWork -> Color(0xFFD97706)
                  else -> Primary
                },
                modifier = Modifier.size(22.dp)
              )
            }
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = place.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = OnSurface
                )
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 2.dp)
              ) {
                Text(
                  text = place.category.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = OnSurfaceVariant
                  ),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = if (place.address.isNotBlank()) place.address else "No address set",
              style = MaterialTheme.typography.bodySmall.copy(
                color = OnSurfaceVariant,
                fontSize = 13.sp
              ),
              maxLines = 2
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          // Manual Map Locator Button
          IconButton(
            onClick = onLocateOnMapClick,
            modifier = Modifier.testTag("button_locate_map_${place.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Map,
              contentDescription = "Locate on Map",
              tint = Primary
            )
          }

          // Edit Button
          IconButton(
            onClick = onEditClick,
            modifier = Modifier.testTag("button_edit_place_${place.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Details",
              tint = OnSurfaceVariant
            )
          }

          // Delete (only for non-primary)
          if (!isPrimaryType && onDeleteClick != null) {
            IconButton(
              onClick = onDeleteClick,
              modifier = Modifier.testTag("button_delete_place_${place.id}")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Address",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Coordinates info bar
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = String.format(Locale.US, "GPS: %.4f, %.4f", place.latitude, place.longitude),
            style = MaterialTheme.typography.labelSmall.copy(
              color = OnSurfaceVariant,
              fontSize = 11.sp
            )
          )

          Text(
            text = "Tap 🗺️ to locate on map",
            style = MaterialTheme.typography.labelSmall.copy(
              color = Primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlaceDialog(
  place: SavedPlace,
  onDismiss: () -> Unit,
  onSave: (SavedPlace) -> Unit,
  onLocateOnMap: (SavedPlace) -> Unit
) {
  var title by remember { mutableStateOf(place.title) }
  var address by remember { mutableStateOf(place.address) }
  var category by remember { mutableStateOf(place.category) }
  var latitude by remember { mutableDoubleStateOf(place.latitude) }
  var longitude by remember { mutableDoubleStateOf(place.longitude) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (place.title.isNotBlank()) "Edit Address" else "Add New Address",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Place Name / Title") },
          placeholder = { Text("e.g. Home, Work, University") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_input_place_title")
        )

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Street Address") },
          placeholder = { Text("e.g. 123 Main St, San Francisco") },
          trailingIcon = {
            IconButton(
              onClick = {
                val current = place.copy(
                  title = title,
                  address = address,
                  category = category,
                  latitude = latitude,
                  longitude = longitude
                )
                onLocateOnMap(current)
              }
            ) {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Pick on Map",
                tint = Primary
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_input_place_address")
        )

        // Manual Locate On Map Button
        OutlinedButton(
          onClick = {
            val current = place.copy(
              title = title,
              address = address,
              category = category,
              latitude = latitude,
              longitude = longitude
            )
            onLocateOnMap(current)
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Pick / Adjust on Map", fontWeight = FontWeight.Bold)
        }

        // Category selection
        Text(
          text = "CATEGORY",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant
          )
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(PlaceCategory.HOME, PlaceCategory.WORK, PlaceCategory.COLLEGE).forEach { cat ->
            FilterChip(
              selected = (category == cat),
              onClick = { category = cat },
              label = { Text(cat.displayName.split(" ").first(), fontSize = 11.sp) }
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(PlaceCategory.GYM, PlaceCategory.FAVORITE, PlaceCategory.OTHER).forEach { cat ->
            FilterChip(
              selected = (category == cat),
              onClick = { category = cat },
              label = { Text(cat.displayName.split(" ").first(), fontSize = 11.sp) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalTitle = title.ifBlank { category.displayName }
          val finalAddress = address.ifBlank { "Location (${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)})" }
          onSave(
            place.copy(
              title = finalTitle,
              address = finalAddress,
              category = category,
              latitude = latitude,
              longitude = longitude
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
        modifier = Modifier.testTag("dialog_button_save_place")
      ) {
        Text("Save Address")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
