package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.HazardReport
import com.example.ui.theme.Primary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OpenStreetMapView(
  modifier: Modifier = Modifier,
  startLat: Double = 37.7749,
  startLng: Double = -122.4194,
  destLat: Double = 37.7833,
  destLng: Double = -122.4064,
  userCurrentLat: Double? = null,
  userCurrentLng: Double? = null,
  accuracyMeters: Float = 0f,
  showRoute: Boolean = true,
  userProgress: Float = 0.5f,
  routePoints: List<Pair<Double, Double>> = emptyList(),
  isDeviated: Boolean = false,
  isStationary: Boolean = false,
  stationaryMinutes: Int = 0,
  hazardReports: List<HazardReport> = emptyList(),
  hazardPoints: List<Pair<String, Pair<Double, Double>>> = emptyList(),
  onHazardClick: ((HazardReport) -> Unit)? = null,
  onRecenterClicked: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Ensure osmdroid configuration and user agent are set before MapView is created
  remember(context) {
    try {
      Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
      Configuration.getInstance().userAgentValue = context.packageName
    } catch (_: Exception) {}
  }

  val startPoint = remember(startLat, startLng) { GeoPoint(startLat, startLng) }
  val destPoint = remember(destLat, destLng) { GeoPoint(destLat, destLng) }

  val currentPoint = remember(startLat, startLng, destLat, destLng, userProgress, userCurrentLat, userCurrentLng, routePoints, hazardReports) {
    if (userCurrentLat != null && userCurrentLng != null) {
      GeoPoint(userCurrentLat, userCurrentLng)
    } else if (showRoute) {
      if (routePoints.size >= 2) {
        val targetIndexFloat = (routePoints.size - 1) * userProgress.coerceIn(0f, 1f)
        val lowerIdx = targetIndexFloat.toInt().coerceIn(0, routePoints.size - 1)
        val upperIdx = (lowerIdx + 1).coerceAtMost(routePoints.size - 1)
        val frac = targetIndexFloat - lowerIdx

        val (lat1, lng1) = routePoints[lowerIdx]
        val (lat2, lng2) = routePoints[upperIdx]
        val interpLat = lat1 + (lat2 - lat1) * frac
        val interpLng = lng1 + (lng2 - lng1) * frac
        GeoPoint(interpLat, interpLng)
      } else {
        val lat = startLat + (destLat - startLat) * userProgress
        val lng = startLng + (destLng - startLng) * userProgress
        GeoPoint(lat, lng)
      }
    } else if (hazardReports.isNotEmpty()) {
      GeoPoint(hazardReports.first().latitude, hazardReports.first().longitude)
    } else {
      GeoPoint(startLat, startLng)
    }
  }

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }

  // Manage MapView lifecycle to prevent memory leaks and keep tile caching smooth
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
        Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapViewRef?.onDetach()
    }
  }

  // Smoothly center the map when user's live position updates
  LaunchedEffect(currentPoint, mapViewRef) {
    mapViewRef?.controller?.animateTo(currentPoint)
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        try {
          val osmdroidConfig = Configuration.getInstance()
          if (osmdroidConfig.userAgentValue.isNullOrBlank()) {
            osmdroidConfig.load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            osmdroidConfig.userAgentValue = ctx.packageName
          }
        } catch (_: Exception) {}
        MapView(ctx).apply {
          setTileSource(TileSourceFactory.MAPNIK)
          setMultiTouchControls(true)
          zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
          controller.setZoom(if (showRoute) 16.5 else 15.5)
          controller.setCenter(currentPoint)
          mapViewRef = this
        }
      },
      update = { mapView ->
        mapView.overlays.clear()

        if (showRoute) {
          // 1. Draw Google Maps style Road Polyline
          val finalGeoPoints: List<GeoPoint> = if (routePoints.isNotEmpty()) {
            routePoints.map { (lat, lng) -> GeoPoint(lat, lng) }
          } else {
            listOf(
              startPoint,
              GeoPoint(startPoint.latitude + (destPoint.latitude - startPoint.latitude) * 0.35, startPoint.longitude + (destPoint.longitude - startPoint.longitude) * 0.05),
              GeoPoint(startPoint.latitude + (destPoint.latitude - startPoint.latitude) * 0.40, startPoint.longitude + (destPoint.longitude - startPoint.longitude) * 0.65),
              GeoPoint(startPoint.latitude + (destPoint.latitude - startPoint.latitude) * 0.80, startPoint.longitude + (destPoint.longitude - startPoint.longitude) * 0.70),
              GeoPoint(startPoint.latitude + (destPoint.latitude - startPoint.latitude) * 0.85, startPoint.longitude + (destPoint.longitude - startPoint.longitude) * 0.95),
              destPoint
            )
          }

          // Outer border casing (Google Maps navy border for road clarity)
          val routeCasing = Polyline(mapView).apply {
            outlinePaint.color = 0xFF1D4ED8.toInt() // Darker royal blue border
            outlinePaint.strokeWidth = 18f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(finalGeoPoints)
          }
          mapView.overlays.add(routeCasing)

          // Inner bright road polyline (Google Maps vibrant blue path)
          val routeForeground = Polyline(mapView).apply {
            outlinePaint.color = 0xFF3B82F6.toInt() // Vivid navigation blue
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(finalGeoPoints)
          }
          mapView.overlays.add(routeForeground)

          // 2. Start Marker
          val startMarker = Marker(mapView).apply {
            position = startPoint
            title = "Start Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createCircleMarkerDrawable(context, 0xFF64748B.toInt(), "S")
          }
          mapView.overlays.add(startMarker)

          // 3. Destination Marker
          val destMarker = Marker(mapView).apply {
            position = destPoint
            title = "Destination"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createCircleMarkerDrawable(context, 0xFF22C55E.toInt(), "D")
          }
          mapView.overlays.add(destMarker)

          // 4. Route Deviation Connector (if off planned road path)
          if (isDeviated) {
            var closestPt = finalGeoPoints.firstOrNull() ?: startPoint
            var minDistance = Float.MAX_VALUE
            for (pt in finalGeoPoints) {
              val d = com.example.util.LocationTracker.calculateDistanceMeters(
                pt.latitude, pt.longitude,
                currentPoint.latitude, currentPoint.longitude
              )
              if (d < minDistance) {
                minDistance = d
                closestPt = pt
              }
            }
            val deviationLine = Polyline(mapView).apply {
              outlinePaint.color = 0xFFDC2626.toInt() // Vibrant red alert line
              outlinePaint.strokeWidth = 6f
              outlinePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
              outlinePaint.strokeCap = Paint.Cap.ROUND
              setPoints(listOf(closestPt, currentPoint))
            }
            mapView.overlays.add(deviationLine)
          }

          // 5. Current User Marker
          val userMarker = Marker(mapView).apply {
            position = currentPoint
            title = when {
              isDeviated -> "Route Deviation Detected!"
              isStationary -> "Stationary Alert (~$stationaryMinutes mins)"
              accuracyMeters > 0 -> "Current GPS Position (±${accuracyMeters.toInt()}m)"
              else -> "Current GPS Position"
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createUserPositionMarkerDrawable(context, isDeviated, isStationary)
          }
          mapView.overlays.add(userMarker)
        } else {
          // In Community Map: show user position if available
          if (userCurrentLat != null && userCurrentLng != null) {
            val userMarker = Marker(mapView).apply {
              position = GeoPoint(userCurrentLat, userCurrentLng)
              title = if (accuracyMeters > 0) "Your Location (±${accuracyMeters.toInt()}m)" else "Your Current Location"
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = createUserPositionMarkerDrawable(context, false, false)
            }
            mapView.overlays.add(userMarker)
          }
        }

        // 5. Community Hazard Reports
        hazardReports.forEach { report ->
          val hazardMarker = Marker(mapView).apply {
            position = GeoPoint(report.latitude, report.longitude)
            title = "${report.type.displayName} • ${report.status.label}"
            snippet = "${report.description}\n${report.confirmationsCount} votes • ${report.locationAddress}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createHazardMarkerDrawable(context)
            setOnMarkerClickListener { marker, _ ->
              marker.showInfoWindow()
              onHazardClick?.invoke(report)
              true
            }
          }
          mapView.overlays.add(hazardMarker)
        }

        // Legacy hazard points (if any passed)
        hazardPoints.forEach { (title, coords) ->
          val (hLat, hLng) = coords
          val hazardMarker = Marker(mapView).apply {
            position = GeoPoint(hLat, hLng)
            this.title = "⚠️ $title"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createHazardMarkerDrawable(context)
          }
          mapView.overlays.add(hazardMarker)
        }

        mapView.invalidate()
      }
    )

    // Floating Map Controls (Zoom In / Out and Recenter)
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
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
            onClick = {
              mapViewRef?.controller?.zoomIn()
            },
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
            onClick = {
              mapViewRef?.controller?.zoomOut()
            },
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

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.size(44.dp)
      ) {
        IconButton(
          onClick = {
            onRecenterClicked?.invoke()
            mapViewRef?.controller?.animateTo(currentPoint, 16.5, 600L)
          },
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Recenter",
            tint = Primary,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }
}

private fun createUserPositionMarkerDrawable(context: Context, isDeviated: Boolean = false, isStationary: Boolean = false): Drawable {
  val sizePx = 64
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  val pulseColor = when {
    isDeviated -> 0x44EF4444.toInt()
    isStationary -> 0x44F59E0B.toInt()
    else -> 0x332563EB.toInt()
  }
  val innerColor = when {
    isDeviated -> 0xFFDC2626.toInt()
    isStationary -> 0xFFD97706.toInt()
    else -> 0xFF2563EB.toInt()
  }

  // Outer pulse circle
  val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = pulseColor
    style = Paint.Style.FILL
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, outerPaint)

  // Inner solid circle
  val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = innerColor
    style = Paint.Style.FILL
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, 16f, innerPaint)

  // White border
  val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = 4f
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, 16f, strokePaint)

  return BitmapDrawable(context.resources, bitmap)
}

private fun createCircleMarkerDrawable(context: Context, color: Int, label: String): Drawable {
  val sizePx = 72
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    style = Paint.Style.FILL
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.5f, circlePaint)

  val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = android.graphics.Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = 4f
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.5f, borderPaint)

  val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = android.graphics.Color.WHITE
    textSize = 28f
    textAlign = Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
  }
  val yPos = (canvas.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
  canvas.drawText(label, sizePx / 2f, yPos, textPaint)

  return BitmapDrawable(context.resources, bitmap)
}

private fun createHazardMarkerDrawable(context: Context): Drawable {
  val sizePx = 60
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFFF59E0B.toInt()
    style = Paint.Style.FILL
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, 22f, paint)

  val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = 3f
  }
  canvas.drawCircle(sizePx / 2f, sizePx / 2f, 22f, borderPaint)

  val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textSize = 26f
    textAlign = Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
  }
  val yPos = (canvas.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
  canvas.drawText("!", sizePx / 2f, yPos, textPaint)

  return BitmapDrawable(context.resources, bitmap)
}
