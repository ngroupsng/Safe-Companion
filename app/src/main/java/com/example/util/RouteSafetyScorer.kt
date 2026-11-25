package com.example.util

import com.example.model.HazardReport
import com.example.model.HazardStatus
import com.example.model.HazardType
import com.example.model.RouteSafetyEvaluation
import com.example.model.RouteSafetyScoringConfig
import com.example.model.ScoredRouteOption
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic community hazard matcher and route safety scoring engine.
 * Pure mathematical implementation with zero external framework dependencies
 * to ensure high-performance, explainability, and full testability across JVM and Android.
 */
object RouteSafetyScorer {

  private const val EARTH_RADIUS_METERS = 6371000.0

  /**
   * Calculates Haversine great-circle distance between two geographic coordinates in meters.
   */
  fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
  }

  /**
   * Calculates minimum perpendicular distance in meters from point P to line segment AB
   * using equirectangular projection centered at segment mid-latitude.
   */
  fun distanceToSegmentMeters(
    pLat: Double, pLng: Double,
    aLat: Double, aLng: Double,
    bLat: Double, bLng: Double
  ): Double {
    val midLat = Math.toRadians((aLat + bLat) / 2.0)
    val cosLat = cos(midLat)

    val ax = 0.0
    val ay = 0.0
    val bx = (bLng - aLng) * (Math.PI / 180.0) * EARTH_RADIUS_METERS * cosLat
    val by = (bLat - aLat) * (Math.PI / 180.0) * EARTH_RADIUS_METERS

    val px = (pLng - aLng) * (Math.PI / 180.0) * EARTH_RADIUS_METERS * cosLat
    val py = (pLat - aLat) * (Math.PI / 180.0) * EARTH_RADIUS_METERS

    val dx = bx - ax
    val dy = by - ay
    val lenSq = dx * dx + dy * dy

    if (lenSq < 1e-6) {
      return haversineDistanceMeters(pLat, pLng, aLat, aLng)
    }

    val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
    val clampedT = t.coerceIn(0.0, 1.0)

    val projX = ax + clampedT * dx
    val projY = ay + clampedT * dy

    val distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY)
    return sqrt(distSq)
  }

  /**
   * Calculates minimum distance in meters from point (pLat, pLng) to a route polyline.
   */
  fun distanceToPathMeters(
    pLat: Double, pLng: Double,
    pathPoints: List<Pair<Double, Double>>
  ): Double {
    if (pathPoints.isEmpty()) return Double.MAX_VALUE
    if (pathPoints.size == 1) {
      val (lat, lng) = pathPoints[0]
      return haversineDistanceMeters(pLat, pLng, lat, lng)
    }

    var minDistance = Double.MAX_VALUE
    for (i in 0 until pathPoints.size - 1) {
      val (aLat, aLng) = pathPoints[i]
      val (bLat, bLng) = pathPoints[i + 1]
      val segDist = distanceToSegmentMeters(pLat, pLng, aLat, aLng, bLat, bLng)
      if (segDist < minDistance) {
        minDistance = segDist
      }
    }
    return minDistance
  }

  /**
   * Validates whether geographic coordinates are valid and non-placeholder.
   */
  fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    if (latitude.isNaN() || longitude.isNaN()) return false
    if (latitude.isInfinite() || longitude.isInfinite()) return false
    if (abs(latitude) > 90.0 || abs(longitude) > 180.0) return false
    // Ignore origin (0.0, 0.0) placeholder coordinates
    if (latitude == 0.0 && longitude == 0.0) return false
    return true
  }

  /**
   * Identifies community hazards located within the configured proximity threshold of the route.
   *
   * DEDUPLICATION:
   * Hazards are evaluated against the polyline as a whole. Each hazard is tracked by its unique ID
   * and added at most once to the resulting list, strictly preventing duplicate penalties even if
   * multiple route waypoints or segments intersect the hazard's proximity circle.
   */
  fun findNearbyHazards(
    routePoints: List<Pair<Double, Double>>,
    hazards: List<HazardReport>,
    proximityMeters: Double = RouteSafetyScoringConfig.HAZARD_PROXIMITY_METERS
  ): List<HazardReport> {
    if (routePoints.isEmpty() || hazards.isEmpty()) return emptyList()

    val nearby = mutableListOf<HazardReport>()
    val visitedIds = mutableSetOf<String>()

    for (hazard in hazards) {
      if (!isValidCoordinate(hazard.latitude, hazard.longitude)) continue
      if (visitedIds.contains(hazard.id)) continue

      val dist = distanceToPathMeters(hazard.latitude, hazard.longitude, routePoints)
      if (dist <= proximityMeters) {
        visitedIds.add(hazard.id)
        nearby.add(hazard)
      }
    }

    return nearby
  }

  /**
   * Calculates the deterministic route safety score based on matched community hazards.
   *
   * Formula:
   * Base Score = 100.0
   * Penalty per hazard = BasePenalty(hazard.type) * StatusMultiplier(hazard.status) * ConfirmationMultiplier(hazard.confirmationsCount)
   * Total Risk = Sum(Penalties)
   * Score = (100.0 - Total Risk).coerceIn(0.0, 100.0)
   */
  fun evaluateRouteHazards(nearbyHazards: List<HazardReport>): RouteSafetyEvaluation {
    var totalRisk = 0.0
    val breakdown = mutableMapOf<HazardType, Int>()
    val deduplicatedIds = mutableSetOf<String>()

    for (hazard in nearbyHazards) {
      deduplicatedIds.add(hazard.id)
      breakdown[hazard.type] = (breakdown[hazard.type] ?: 0) + 1

      // 1. Base penalty by hazard category
      val basePenalty = when (hazard.type) {
        HazardType.UNSAFE_AREA -> RouteSafetyScoringConfig.UNSAFE_AREA_PENALTY
        HazardType.BROKEN_STREETLIGHT -> RouteSafetyScoringConfig.BROKEN_STREETLIGHT_PENALTY
        HazardType.BLOCKED_ROAD -> RouteSafetyScoringConfig.BLOCKED_ROAD_PENALTY
        HazardType.OTHER -> RouteSafetyScoringConfig.OTHER_PENALTY
        HazardType.POTHOLE -> RouteSafetyScoringConfig.POTHOLE_PENALTY
      }

      // 2. Status multiplier (Resolved hazards incur 0 penalty)
      val statusMultiplier = when (hazard.status) {
        HazardStatus.RESOLVED -> RouteSafetyScoringConfig.STATUS_RESOLVED_MULTIPLIER
        HazardStatus.VERIFIED -> RouteSafetyScoringConfig.STATUS_VERIFIED_MULTIPLIER
        HazardStatus.UNDER_REVIEW -> RouteSafetyScoringConfig.STATUS_UNDER_REVIEW_MULTIPLIER
        HazardStatus.REPORTED -> RouteSafetyScoringConfig.STATUS_REPORTED_MULTIPLIER
      }

      // 3. Confirmation weighting (more confirmations increase confidence up to 1.5x)
      val confirmationBonus = hazard.confirmationsCount.coerceAtLeast(0) * RouteSafetyScoringConfig.CONFIRMATION_BONUS_PER_VOTE
      val confirmationMultiplier = min(1.0 + confirmationBonus, RouteSafetyScoringConfig.MAX_CONFIRMATION_MULTIPLIER)

      val hazardPenalty = basePenalty * statusMultiplier * confirmationMultiplier
      totalRisk += hazardPenalty
    }

    val rawScore = RouteSafetyScoringConfig.BASE_SCORE - totalRisk
    val clampedScore = round(rawScore.coerceIn(RouteSafetyScoringConfig.MIN_SCORE, RouteSafetyScoringConfig.MAX_SCORE)).toInt()

    return RouteSafetyEvaluation(
      rawScore = rawScore,
      clampedScore = clampedScore,
      nearbyHazards = nearbyHazards,
      deduplicatedHazardIds = deduplicatedIds,
      hazardBreakdown = breakdown
    )
  }

  /**
   * Scores an individual candidate route against community hazards.
   */
  fun scoreCandidateRoute(
    id: String,
    title: String,
    subtitle: String,
    points: List<Pair<Double, Double>>,
    distanceMeters: Double,
    durationSeconds: Double,
    hazards: List<HazardReport>,
    proximityMeters: Double = RouteSafetyScoringConfig.HAZARD_PROXIMITY_METERS
  ): ScoredRouteOption {
    val nearbyHazards = findNearbyHazards(points, hazards, proximityMeters)
    val evaluation = evaluateRouteHazards(nearbyHazards)

    return ScoredRouteOption(
      id = id,
      title = title,
      subtitle = subtitle,
      points = points,
      distanceMeters = distanceMeters,
      durationSeconds = durationSeconds,
      safetyScore = evaluation.clampedScore,
      hazardCountNearby = nearbyHazards.size,
      hazardsOnRoute = nearbyHazards,
      isRecommended = false,
      recommendationReason = ""
    )
  }

  /**
   * Evaluates candidate routes and recommends the safer route while balancing route practicality.
   *
   * Recommendation Logic:
   * 1. Single candidate: Evaluates safety condition and informs user accordingly.
   * 2. Multiple candidates:
   *    - Identifies shortest route and safest route (highest safety score).
   *    - If shortest route is equally safe or safer, recommend shortest.
   *    - If safest route has higher safety score by at least MIN_SAFETY_ADVANTAGE_POINTS:
   *      - Checks detour ratio: (safest.distance / shortest.distance).
   *      - If detour <= MAX_ACCEPTABLE_DETOUR_RATIO (e.g. +35% max extra distance):
   *        Recommends safer route with clear reason (e.g., "Safer route recommended — slightly longer (+0.4 km), but avoids 2 reported hazards.")
   *      - If detour > MAX_ACCEPTABLE_DETOUR_RATIO:
   *        Recommends shortest route with advisory (detour is impractical).
   *    - Otherwise (score advantage < 5 points), recommends shortest route.
   */
  fun evaluateAndRecommendRoutes(
    candidates: List<ScoredRouteOption>
  ): List<ScoredRouteOption> {
    if (candidates.isEmpty()) return emptyList()

    if (candidates.size == 1) {
      val single = candidates[0]
      val reason = when {
        single.hazardCountNearby == 0 -> "Clear route — no active community hazards reported along this corridor."
        single.safetyScore >= 85 -> "Good safety rating based on community hazard reports."
        single.safetyScore >= 60 -> "Moderate safety — ${single.hazardCountNearby} reported hazard(s) nearby."
        else -> "Caution advised — ${single.hazardCountNearby} hazard(s) reported along this route."
      }
      return listOf(single.copy(isRecommended = true, recommendationReason = reason))
    }

    val shortest = candidates.minByOrNull { it.distanceMeters } ?: candidates.first()
    val safest = candidates.maxByOrNull { it.safetyScore } ?: candidates.first()

    val recommendedId: String
    val updatedCandidates = candidates.map { candidate ->
      candidate
    }.toMutableList()

    if (safest.id == shortest.id) {
      // Direct route is also the safest route!
      recommendedId = shortest.id
      val reason = if (shortest.hazardCountNearby == 0) {
        "Direct route recommended — clear corridor with zero reported hazards."
      } else {
        "Direct route recommended — lowest hazard exposure and shortest travel time."
      }
      return updatedCandidates.map { c ->
        c.copy(
          isRecommended = (c.id == recommendedId),
          recommendationReason = if (c.id == recommendedId) reason else c.subtitle
        )
      }
    }

    val detourRatio = safest.distanceMeters / shortest.distanceMeters.coerceAtLeast(100.0)
    val scoreAdvantage = safest.safetyScore - shortest.safetyScore
    val hazardDifference = shortest.hazardCountNearby - safest.hazardCountNearby

    if (detourRatio <= RouteSafetyScoringConfig.MAX_ACCEPTABLE_DETOUR_RATIO &&
      scoreAdvantage >= RouteSafetyScoringConfig.MIN_SAFETY_ADVANTAGE_POINTS
    ) {
      // Recommend the safer route!
      recommendedId = safest.id
      val extraDistanceKm = (safest.distanceMeters - shortest.distanceMeters) / 1000.0
      val distText = if (extraDistanceKm >= 0.1) {
        String.format(Locale.US, "+%.1f km", extraDistanceKm)
      } else {
        String.format(Locale.US, "+%d m", (safest.distanceMeters - shortest.distanceMeters).toInt())
      }

      val reason = if (hazardDifference > 0) {
        "Safer route recommended — slightly longer ($distText), but avoids $hazardDifference reported hazard(s)."
      } else {
        "Safer route recommended — higher safety score ($scoreAdvantage pts higher) with minimal detour ($distText)."
      }

      return updatedCandidates.map { c ->
        c.copy(
          isRecommended = (c.id == recommendedId),
          recommendationReason = if (c.id == recommendedId) reason else "Direct path has ${c.hazardCountNearby} nearby hazard(s)."
        )
      }
    } else if (detourRatio > RouteSafetyScoringConfig.MAX_ACCEPTABLE_DETOUR_RATIO) {
      // Practicality threshold exceeded
      recommendedId = shortest.id
      val reason = "Direct route recommended — alternative avoids hazards but adds an excessive detour (+${String.format(Locale.US, "%.1f", (safest.distanceMeters - shortest.distanceMeters) / 1000.0)} km)."
      return updatedCandidates.map { c ->
        c.copy(
          isRecommended = (c.id == recommendedId),
          recommendationReason = if (c.id == recommendedId) reason else "Safer corridor adds too much walking distance."
        )
      }
    } else {
      // Safety advantage is negligible (< 5 points)
      recommendedId = shortest.id
      val reason = "Direct route recommended — comparable safety with minimal travel distance."
      return updatedCandidates.map { c ->
        c.copy(
          isRecommended = (c.id == recommendedId),
          recommendationReason = if (c.id == recommendedId) reason else c.subtitle
        )
      }
    }
  }
}
