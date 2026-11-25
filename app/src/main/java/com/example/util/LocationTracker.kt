package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationTracker(private val context: Context) {

  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  private val locationManager: android.location.LocationManager? =
    context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager

  private var locationCallback: LocationCallback? = null
  private var systemLocationListener: android.location.LocationListener? = null

  fun hasLocationPermission(): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocation = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineLocation || coarseLocation
  }

  @SuppressLint("MissingPermission")
  fun getCurrentLocation(
    onLocationReceived: (Location) -> Unit,
    onError: (Exception) -> Unit = {}
  ) {
    if (!hasLocationPermission()) {
      onError(SecurityException("Location permission not granted"))
      return
    }

    var fixDelivered = false

    // 1. Check system LocationManager last known locations first for instant fix
    try {
      val gpsLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
      val netLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
      val passLoc = locationManager?.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)

      val bestSystemLoc = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
      if (bestSystemLoc != null) {
        fixDelivered = true
        onLocationReceived(bestSystemLoc)
      }
    } catch (_: Exception) {}

    // 2. Query Google Play Services Fused Location Client
    try {
      fusedLocationClient.lastLocation
        .addOnSuccessListener { lastLocation ->
          if (lastLocation != null) {
            fixDelivered = true
            onLocationReceived(lastLocation)
          }
        }

      val priority = Priority.PRIORITY_HIGH_ACCURACY
      fusedLocationClient.getCurrentLocation(priority, null)
        .addOnSuccessListener { location ->
          if (location != null) {
            fixDelivered = true
            onLocationReceived(location)
          } else if (!fixDelivered) {
            // If still no fix, try single-shot system provider
            requestSingleSystemFix(onLocationReceived, onError)
          }
        }
        .addOnFailureListener { exception ->
          if (!fixDelivered) {
            requestSingleSystemFix(onLocationReceived, onError)
          }
        }
    } catch (e: Exception) {
      if (!fixDelivered) {
        requestSingleSystemFix(onLocationReceived, onError)
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun requestSingleSystemFix(
    onLocationReceived: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {
    if (!hasLocationPermission() || locationManager == null) {
      onError(SecurityException("Unable to acquire location"))
      return
    }

    try {
      val provider = when {
        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ->
          android.location.LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ->
          android.location.LocationManager.NETWORK_PROVIDER
        else -> null
      }

      if (provider != null) {
        val listener = object : android.location.LocationListener {
          override fun onLocationChanged(location: Location) {
            try {
              locationManager.removeUpdates(this)
            } catch (_: Exception) {}
            onLocationReceived(location)
          }
          @Deprecated("Deprecated in Java")
          override fun onStatusChanged(p: String?, status: Int, extras: android.os.Bundle?) {}
          override fun onProviderEnabled(p: String) {}
          override fun onProviderDisabled(p: String) {}
        }
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
      } else {
        onError(IllegalStateException("No location provider available"))
      }
    } catch (e: Exception) {
      onError(e)
    }
  }

  @SuppressLint("MissingPermission")
  fun startLocationUpdates(
    intervalMs: Long = 4000L,
    onLocationUpdate: (Location) -> Unit
  ) {
    if (!hasLocationPermission()) return

    stopLocationUpdates()

    // 1. Google Play Services Fused Location updates
    val locationRequest = LocationRequest.Builder(
      Priority.PRIORITY_HIGH_ACCURACY,
      intervalMs
    ).apply {
      setMinUpdateIntervalMillis(1500L)
      setMinUpdateDistanceMeters(1f)
      setWaitForAccurateLocation(false)
    }.build()

    locationCallback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let { location ->
          onLocationUpdate(location)
        }
      }
    }

    try {
      fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback!!,
        Looper.getMainLooper()
      )
    } catch (_: Exception) {}

    // 2. Dual fallback: System LocationManager updates (for emulators & non-GMS devices)
    try {
      val lm = locationManager
      if (lm != null) {
        val listener = object : android.location.LocationListener {
          override fun onLocationChanged(location: Location) {
            onLocationUpdate(location)
          }
          @Deprecated("Deprecated in Java")
          override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
          override fun onProviderEnabled(provider: String) {}
          override fun onProviderDisabled(provider: String) {}
        }
        systemLocationListener = listener

        if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
          lm.requestLocationUpdates(
            android.location.LocationManager.GPS_PROVIDER,
            intervalMs,
            1f,
            listener,
            Looper.getMainLooper()
          )
        } else if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
          lm.requestLocationUpdates(
            android.location.LocationManager.NETWORK_PROVIDER,
            intervalMs,
            1f,
            listener,
            Looper.getMainLooper()
          )
        }
      }
    } catch (_: Exception) {}
  }

  fun stopLocationUpdates() {
    locationCallback?.let {
      try {
        fusedLocationClient.removeLocationUpdates(it)
      } catch (_: Exception) {}
      locationCallback = null
    }

    systemLocationListener?.let {
      try {
        locationManager?.removeUpdates(it)
      } catch (_: Exception) {}
      systemLocationListener = null
    }
  }

  suspend fun reverseGeocode(latitude: Double, longitude: Double): String =
    withContext(Dispatchers.IO) {
      try {
        if (!Geocoder.isPresent()) {
          return@withContext formatCoordinates(latitude, longitude)
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val firstAddress = addresses?.firstOrNull()
        if (firstAddress != null) {
          val formatted = formatAddress(firstAddress)
          if (!formatted.isNullOrBlank()) {
            return@withContext formatted
          }
        }
        formatCoordinates(latitude, longitude)
      } catch (e: Exception) {
        formatCoordinates(latitude, longitude)
      }
    }

  suspend fun geocodeAddress(addressName: String): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
      if (addressName.isBlank()) return@withContext null
      try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocationName(addressName, 1)
        if (!results.isNullOrEmpty()) {
          val location = results[0]
          return@withContext Pair(location.latitude, location.longitude)
        }
        null
      } catch (e: Exception) {
        null
      }
    }

  companion object {
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
      val results = FloatArray(1)
      Location.distanceBetween(lat1, lon1, lat2, lon2, results)
      return results[0]
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      return calculateDistanceMeters(lat1, lon1, lat2, lon2) / 1000.0
    }

    /**
     * Calculates the minimum perpendicular distance in meters from point P to line segment AB.
     */
    fun calculateDistanceToSegmentMeters(
      pLat: Double, pLng: Double,
      aLat: Double, aLng: Double,
      bLat: Double, bLng: Double
    ): Float {
      val midLat = Math.toRadians((aLat + bLat) / 2.0)
      val cosLat = Math.cos(midLat)

      val ax = 0.0
      val ay = 0.0
      val bx = (bLng - aLng) * (Math.PI / 180.0) * 6371000.0 * cosLat
      val by = (bLat - aLat) * (Math.PI / 180.0) * 6371000.0

      val px = (pLng - aLng) * (Math.PI / 180.0) * 6371000.0 * cosLat
      val py = (pLat - aLat) * (Math.PI / 180.0) * 6371000.0

      val dx = bx - ax
      val dy = by - ay
      val lenSq = dx * dx + dy * dy

      if (lenSq < 1e-6) {
        return calculateDistanceMeters(pLat, pLng, aLat, aLng)
      }

      val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
      val clampedT = t.coerceIn(0.0, 1.0)

      val projX = ax + clampedT * dx
      val projY = ay + clampedT * dy

      val distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY)
      return Math.sqrt(distSq).toFloat()
    }

    /**
     * Calculates minimum distance in meters from point P to an ordered list of waypoints (polyline path).
     */
    fun calculateDistanceToPathMeters(
      pLat: Double, pLng: Double,
      pathPoints: List<Pair<Double, Double>>
    ): Float {
      if (pathPoints.isEmpty()) return 0f
      if (pathPoints.size == 1) {
        val (lat, lng) = pathPoints[0]
        return calculateDistanceMeters(pLat, pLng, lat, lng)
      }

      var minDistance = Float.MAX_VALUE
      for (i in 0 until pathPoints.size - 1) {
        val (aLat, aLng) = pathPoints[i]
        val (bLat, bLng) = pathPoints[i + 1]
        val segDist = calculateDistanceToSegmentMeters(pLat, pLng, aLat, aLng, bLat, bLng)
        if (segDist < minDistance) {
          minDistance = segDist
        }
      }
      return minDistance
    }
  }

  private fun formatAddress(address: Address?): String? {
    if (address == null) return null
    val street = address.thoroughfare ?: address.featureName
    val locality = address.locality ?: address.subLocality ?: address.adminArea
    return when {
      street != null && locality != null -> "$street, $locality"
      street != null -> street
      locality != null -> locality
      address.maxAddressLineIndex >= 0 -> address.getAddressLine(0)
      else -> null
    }
  }

  private fun formatCoordinates(latitude: Double, longitude: Double): String {
    val latDir = if (latitude >= 0) "N" else "S"
    val lngDir = if (longitude >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f° %s, %.4f° %s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir)
  }
}
