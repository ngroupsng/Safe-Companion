package com.example.model

/**
 * Centralized configuration for community-aware route safety scoring.
 * All penalty weights, multipliers, and proximity thresholds are defined here
 * for explainability and tuning.
 */
object RouteSafetyScoringConfig {
  const val BASE_SCORE = 100.0
  const val MIN_SCORE = 0.0
  const val MAX_SCORE = 100.0

  /**
   * Proximity threshold in meters.
   * A hazard within 80 meters of the polyline corridor is considered directly affecting the route.
   * 80 meters covers the roadway, sidewalks, pedestrian crossings, and immediate alleyways
   * without falsely matching hazards on distant parallel avenues.
   */
  const val HAZARD_PROXIMITY_METERS = 80.0

  /**
   * Risk penalties by hazard category.
   * - UNSAFE_AREA: 30.0 - Highest risk to personal safety in a walking companion app.
   * - BROKEN_STREETLIGHT: 18.0 - Critical vulnerability for night pedestrians due to poor visibility.
   * - BLOCKED_ROAD: 14.0 - Forces pedestrians into vehicle traffic or unexpected detours.
   * - OTHER: 8.0 - Generic uncategorized safety concern.
   * - POTHOLE: 6.0 - Physical trip hazard or road surface flaw.
   */
  const val UNSAFE_AREA_PENALTY = 30.0
  const val BROKEN_STREETLIGHT_PENALTY = 18.0
  const val BLOCKED_ROAD_PENALTY = 14.0
  const val OTHER_PENALTY = 8.0
  const val POTHOLE_PENALTY = 6.0

  /**
   * Credibility multipliers by hazard verification status:
   * - RESOLVED: 0.0x (Fixed/cleared hazards pose no active threat)
   * - VERIFIED: 1.25x (Confirmed by community consensus or city authorities)
   * - UNDER_REVIEW: 1.0x (Standard active review)
   * - REPORTED: 0.9x (Initial unverified report)
   */
  const val STATUS_RESOLVED_MULTIPLIER = 0.0
  const val STATUS_VERIFIED_MULTIPLIER = 1.25
  const val STATUS_UNDER_REVIEW_MULTIPLIER = 1.0
  const val STATUS_REPORTED_MULTIPLIER = 0.9

  /**
   * Community confirmation weighting:
   * Each positive confirmation adds 5% weight up to a maximum multiplier of 1.5x.
   */
  const val CONFIRMATION_BONUS_PER_VOTE = 0.05
  const val MAX_CONFIRMATION_MULTIPLIER = 1.5

  /**
   * Route practicality balance:
   * A safer route will be recommended over a direct route if its detour ratio is
   * within 1.35x (at most 35% longer) and it offers at least 5 points of safety score advantage.
   */
  const val MAX_ACCEPTABLE_DETOUR_RATIO = 1.35
  const val MIN_SAFETY_ADVANTAGE_POINTS = 5.0
}

/**
 * Represents a scored route option presented to the user.
 */
data class ScoredRouteOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val points: List<Pair<Double, Double>>,
  val distanceMeters: Double,
  val durationSeconds: Double,
  val safetyScore: Int,
  val hazardCountNearby: Int,
  val hazardsOnRoute: List<HazardReport>,
  val isRecommended: Boolean,
  val recommendationReason: String
) {
  val distanceKm: Double
    get() = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0

  val etaMinutes: Int
    get() = Math.round(durationSeconds / 60.0).toInt().coerceAtLeast(1)
}

/**
 * Result of scoring a single route path against community hazards.
 */
data class RouteSafetyEvaluation(
  val rawScore: Double,
  val clampedScore: Int,
  val nearbyHazards: List<HazardReport>,
  val deduplicatedHazardIds: Set<String>,
  val hazardBreakdown: Map<HazardType, Int>
)
