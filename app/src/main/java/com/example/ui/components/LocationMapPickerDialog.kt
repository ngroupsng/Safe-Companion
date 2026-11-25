package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.SurfaceContainerLowest
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * Fullscreen / modal interactive map picker dialog.
 * Allows user to visually pan, zoom, and tap on the map or drag the center pin
 * to pick precise latitude and longitude, with editable reverse-geocoded address label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationMapPickerDialog(
  initialTitle: String = "Pick Location",
  initialAddress: String = "",
  initialLat: Double = 37.7749,
  initialLng: Double = -122.4194,
  onDismiss: () -> Unit,
  onLocationSelected: (address: String, lat: Double, lng: Double) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Ensure osmdroid configuration is loaded safely
  remember(context) {
    try {
      Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
      Configuration.getInstance().userAgentValue = context.packageName
    } catch (_: Exception) {}
  }

  var selectedLat by remember { mutableDoubleStateOf(if (initialLat != 0.0) initialLat else 37.7749) }
  var selectedLng by remember { mutableDoubleStateOf(if (initialLng != 0.0) initialLng else -122.4194) }
  var addressInput by remember {
    mutableStateOf(
      if (initialAddress.isNotBlank()) initialAddress
      else String.format(Locale.US, "Location (%.4f, %.4f)", selectedLat, selectedLng)
    )
  }

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
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
              .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("map_picker_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = initialTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              )
              Text(
                text = "Tap or pan map to locate exact point",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
              )
            }
          }
        }
      },
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
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Selected Address textfield (editable)
            OutlinedTextField(
              value = addressInput,
              onValueChange = { addressInput = it },
              label = { Text("Address or Place Name") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Place,
                  contentDescription = null,
                  tint = Primary
                )
              },
              trailingIcon = {
                if (addressInput.isNotBlank()) {
                  IconButton(onClick = { addressInput = "" }) {
                    Icon(
                      imageVector = Icons.Default.Clear,
                      contentDescription = "Clear address",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_picker_address")
            )

            // Coordinates badge & Confirm button
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
              ) {
                Text(
                  text = String.format(Locale.US, "GPS: %.5f, %.5f", selectedLat, selectedLng),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }

              Button(
                onClick = {
                  val finalAddress = if (addressInput.isNotBlank()) {
                    addressInput.trim()
                  } else {
                    String.format(Locale.US, "Location (%.4f, %.4f)", selectedLat, selectedLng)
                  }
                  onLocationSelected(finalAddress, selectedLat, selectedLng)
                  onDismiss()
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Primary,
                  contentColor = OnPrimary
                ),
                modifier = Modifier
                  .height(46.dp)
                  .testTag("button_confirm_picked_location")
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Confirm Location",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Interactive OSM Map
        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            when (event) {
              Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
              Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
              Lifecycle.Event.ON_DESTROY -> {
                mapViewRef?.onDetach()
                mapViewRef = null
              }
              else -> {}
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
            mapViewRef = null
          }
        }

        AndroidView(
          modifier = Modifier.fillMaxSize(),
          factory = { ctx ->
            MapView(ctx).apply {
              setTileSource(TileSourceFactory.MAPNIK)
              setMultiTouchControls(true)
              isTilesScaledToDpi = true
              minZoomLevel = 4.0
              maxZoomLevel = 20.0
              controller.setZoom(16.5)
              controller.setCenter(GeoPoint(selectedLat, selectedLng))

              // Map click overlay to pick new point
              val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                  selectedLat = p.latitude
                  selectedLng = p.longitude
                  if (addressInput.startsWith("Location (") || addressInput.startsWith("GPS:") || addressInput.isBlank()) {
                    addressInput = String.format(Locale.US, "Location (%.4f, %.4f)", p.latitude, p.longitude)
                  }
                  controller.animateTo(p)
                  return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                  selectedLat = p.latitude
                  selectedLng = p.longitude
                  addressInput = String.format(Locale.US, "Pinned Location (%.4f, %.4f)", p.latitude, p.longitude)
                  controller.animateTo(p)
                  return true
                }
              })
              overlays.add(eventsOverlay)
              mapViewRef = this
            }
          },
          update = { mv ->
            // Clear prior dynamic marker overlays, keep event overlay
            val eventOverlay = mv.overlays.firstOrNull { it is MapEventsOverlay }
            mv.overlays.clear()
            eventOverlay?.let { mv.overlays.add(it) }

            // Add marker for current selected location
            val pin = Marker(mv).apply {
              position = GeoPoint(selectedLat, selectedLng)
              title = "Selected Location"
              snippet = addressInput
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
              isDraggable = true
              setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {}
                override fun onMarkerDragStart(marker: Marker?) {}
                override fun onMarkerDragEnd(marker: Marker?) {
                  marker?.position?.let { pt ->
                    selectedLat = pt.latitude
                    selectedLng = pt.longitude
                    if (addressInput.startsWith("Location (") || addressInput.startsWith("GPS:")) {
                      addressInput = String.format(Locale.US, "Location (%.4f, %.4f)", pt.latitude, pt.longitude)
                    }
                  }
                }
              })
            }
            mv.overlays.add(pin)
            mv.invalidate()
          }
        )

        // Center crosshair helper indicator
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .size(36.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Primary)
          )
        }

        // Floating Map Controls: Zoom in/out and Recenter
        Column(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 16.dp)
            .width(44.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.width(44.dp)
          ) {
            Column(
              modifier = Modifier.width(44.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              IconButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                modifier = Modifier.size(44.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Zoom In",
                  tint = Color(0xFF1E293B),
                  modifier = Modifier.size(20.dp)
                )
              }
              HorizontalDivider(
                modifier = Modifier.width(32.dp),
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
              )
              IconButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                modifier = Modifier.size(44.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Remove,
                  contentDescription = "Zoom Out",
                  tint = Color(0xFF1E293B),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          // Recenter on current selected point
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.size(44.dp)
          ) {
            IconButton(
              onClick = {
                mapViewRef?.controller?.animateTo(GeoPoint(selectedLat, selectedLng), 16.5, 500L)
              },
              modifier = Modifier.size(44.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter on Pin",
                tint = Primary,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }
    }
  }
}
