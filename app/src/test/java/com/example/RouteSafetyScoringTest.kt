package com.example

import com.example.model.HazardReport
import com.example.model.HazardStatus
import com.example.model.HazardType
import com.example.model.RouteSafetyScoringConfig
import com.example.model.ScoredRouteOption
import com.example.util.RouteSafetyScorer
import org.junit.Assert.*
import org.junit.Test

class RouteSafetyScoringTest {

  // Test polyline corridor from San Francisco Market St to Union Square
  // Segment from (37.7749, -122.4194) to (37.7870, -122.4070)
  private val testRoutePoints = listOf(
    Pair(37.7749, -122.4194),
    Pair(37.7780, -122.4160),
    Pair(37.7820, -122.4110),
    Pair(37.7870, -122.4070)
  )

  @Test
  fun `zero hazards yields perfect safety score of 100`() {
    val emptyHazards = emptyList<HazardReport>()
    val evaluation = RouteSafetyScorer.evaluateRouteHazards(emptyHazards)

    assertEquals(100.0, evaluation.rawScore, 0.001)
    assertEquals(100, evaluation.clampedScore)
    assertTrue(evaluation.nearbyHazards.isEmpty())
    assertTrue(evaluation.deduplicatedHazardIds.isEmpty())
  }

  @Test
  fun `nearby hazard within 80m threshold is detected and applies category penalty`() {
    // Point directly on segment: (37.7780, -122.4160)
    val onRouteHazard = HazardReport(
      id = "h1",
      title = "Dark Streetlight",
      type = HazardType.BROKEN_STREETLIGHT,
      locationAddress = "Market & 7th",
      reportedTime = "10 mins ago",
      status = HazardStatus.UNDER_REVIEW,
      description = "Streetlight is completely out",
      latitude = 37.7780,
      longitude = -122.4160,
      confirmationsCount = 0
    )

    val matched = RouteSafetyScorer.findNearbyHazards(testRoutePoints, listOf(onRouteHazard))
    assertEquals(1, matched.size)
    assertEquals("h1", matched[0].id)

    val evaluation = RouteSafetyScorer.evaluateRouteHazards(matched)
    // Base penalty: 18.0, status multiplier (UNDER_REVIEW): 1.0, confirmations: 0 -> penalty = 18.0
    // Score = 100 - 18 = 82
    assertEquals(82.0, evaluation.rawScore, 0.01)
    assertEquals(82, evaluation.clampedScore)
  }

  @Test
  fun `hazard far away from route corridor does not affect score`() {
    // Hazard located 10 km away in Oakland (37.8044, -122.2711)
    val distantHazard = HazardReport(
      id = "h_distant",
      title = "Distant Blocked Road",
      type = HazardType.BLOCKED_ROAD,
      locationAddress = "Oakland",
      reportedTime = "1 hour ago",
      status = HazardStatus.VERIFIED,
      description = "Distant roadblock",
      latitude = 37.8044,
      longitude = -122.2711,
      confirmationsCount = 5
    )

    val matched = RouteSafetyScorer.findNearbyHazards(testRoutePoints, listOf(distantHazard))
    assertTrue("Distant hazard must not match route corridor", matched.isEmpty())

    val evaluation = RouteSafetyScorer.evaluateRouteHazards(matched)
    assertEquals(100, evaluation.clampedScore)
  }

  @Test
  fun `same hazard near multiple route waypoints counts only once (strict deduplication)`() {
    // Hazard located midway between waypoints
    val midpointHazard = HazardReport(
      id = "h_dup",
      title = "Midpoint Pothole",
      type = HazardType.POTHOLE,
      locationAddress = "Central St",
      reportedTime = "2 hours ago",
      status = HazardStatus.UNDER_REVIEW,
      description = "Road flaw",
      latitude = 37.778005,
      longitude = -122.416005,
      confirmationsCount = 0
    )

    // Pass multiple duplicates in the hazard input list
    val matched = RouteSafetyScorer.findNearbyHazards(
      testRoutePoints,
      listOf(midpointHazard, midpointHazard.copy(), midpointHazard.copy())
    )

    assertEquals("Hazard must be deduplicated by id", 1, matched.size)
    val evaluation = RouteSafetyScorer.evaluateRouteHazards(matched)
    assertEquals(1, evaluation.deduplicatedHazardIds.size)
    assertEquals(1, evaluation.hazardBreakdown[HazardType.POTHOLE])
  }

  @Test
  fun `hazard categories produce correct relative penalties`() {
    // Category penalties: UNSAFE_AREA (30) > BROKEN_STREETLIGHT (18) > BLOCKED_ROAD (14) > OTHER (8) > POTHOLE (6)
    val unsafeArea = HazardReport(id = "h1", title = "Area", type = HazardType.UNSAFE_AREA, locationAddress = "", reportedTime = "", status = HazardStatus.UNDER_REVIEW, description = "", latitude = 37.778, longitude = -122.416, confirmationsCount = 0)
    val streetlight = HazardReport(id = "h2", title = "Light", type = HazardType.BROKEN_STREETLIGHT, locationAddress = "", reportedTime = "", status = HazardStatus.UNDER_REVIEW, description = "", latitude = 37.778, longitude = -122.416, confirmationsCount = 0)
    val blockedRoad = HazardReport(id = "h3", title = "Block", type = HazardType.BLOCKED_ROAD, locationAddress = "", reportedTime = "", status = HazardStatus.UNDER_REVIEW, description = "", latitude = 37.778, longitude = -122.416, confirmationsCount = 0)
    val other = HazardReport(id = "h4", title = "Other", type = HazardType.OTHER, locationAddress = "", reportedTime = "", status = HazardStatus.UNDER_REVIEW, description = "", latitude = 37.778, longitude = -122.416, confirmationsCount = 0)
    val pothole = HazardReport(id = "h5", title = "Hole", type = HazardType.POTHOLE, locationAddress = "", reportedTime = "", status = HazardStatus.UNDER_REVIEW, description = "", latitude = 37.778, longitude = -122.416, confirmationsCount = 0)

    val scoreUnsafe = RouteSafetyScorer.evaluateRouteHazards(listOf(unsafeArea)).clampedScore
    val scoreStreetlight = RouteSafetyScorer.evaluateRouteHazards(listOf(streetlight)).clampedScore
    val scoreBlocked = RouteSafetyScorer.evaluateRouteHazards(listOf(blockedRoad)).clampedScore
    val scoreOther = RouteSafetyScorer.evaluateRouteHazards(listOf(other)).clampedScore
    val scorePothole = RouteSafetyScorer.evaluateRouteHazards(listOf(pothole)).clampedScore

    // Higher penalty results in lower score
    assertTrue("Unsafe Area score ($scoreUnsafe) < Streetlight score ($scoreStreetlight)", scoreUnsafe < scoreStreetlight)
    assertTrue("Streetlight score ($scoreStreetlight) < Blocked road score ($scoreBlocked)", scoreStreetlight < scoreBlocked)
    assertTrue("Blocked road score ($scoreBlocked) < Other score ($scoreOther)", scoreBlocked < scoreOther)
    assertTrue("Other score ($scoreOther) < Pothole score ($scorePothole)", scoreOther < scorePothole)
  }

  @Test
  fun `resolved hazards incur zero penalty`() {
    val resolvedHazard = HazardReport(
      id = "h_resolved",
      title = "Fixed Streetlight",
      type = HazardType.UNSAFE_AREA,
      locationAddress = "Market St",
      reportedTime = "Yesterday",
      status = HazardStatus.RESOLVED,
      description = "Resolved threat",
      latitude = 37.7780,
      longitude = -122.4160,
      confirmationsCount = 10
    )

    val matched = RouteSafetyScorer.findNearbyHazards(testRoutePoints, listOf(resolvedHazard))
    val evaluation = RouteSafetyScorer.evaluateRouteHazards(matched)

    assertEquals("Resolved hazards must have 0 penalty", 100, evaluation.clampedScore)
  }

  @Test
  fun `verified status and community votes increase penalty multiplier`() {
    val unverifiedReport = HazardReport(
      id = "h_rep",
      title = "Reported Light",
      type = HazardType.BROKEN_STREETLIGHT,
      locationAddress = "",
      reportedTime = "",
      status = HazardStatus.REPORTED,
      description = "",
      latitude = 37.778,
      longitude = -122.416,
      confirmationsCount = 0
    )

    val verifiedReport = HazardReport(
      id = "h_ver",
      title = "Verified Light",
      type = HazardType.BROKEN_STREETLIGHT,
      locationAddress = "",
      reportedTime = "",
      status = HazardStatus.VERIFIED,
      description = "",
      latitude = 37.778,
      longitude = -122.416,
      confirmationsCount = 4
    )

    val scoreRep = RouteSafetyScorer.evaluateRouteHazards(listOf(unverifiedReport)).clampedScore
    val scoreVer = RouteSafetyScorer.evaluateRouteHazards(listOf(verifiedReport)).clampedScore

    assertTrue("Verified hazard with community confirmations must produce greater penalty (lower score)", scoreVer < scoreRep)
  }

  @Test
  fun `score clamps correctly between 0 and 100 under massive hazard accumulation`() {
    // Generate 10 major unsafe areas along route
    val massiveHazards = (1..10).map { i ->
      HazardReport(
        id = "massive_$i",
        title = "Severe Hazard $i",
        type = HazardType.UNSAFE_AREA,
        locationAddress = "",
        reportedTime = "",
        status = HazardStatus.VERIFIED,
        description = "Dangerous zone $i",
        latitude = 37.7749 + (i * 0.001),
        longitude = -122.4194 + (i * 0.001),
        confirmationsCount = 5
      )
    }

    val matched = RouteSafetyScorer.findNearbyHazards(testRoutePoints, massiveHazards)
    val evaluation = RouteSafetyScorer.evaluateRouteHazards(matched)

    assertTrue("Raw score may be negative", evaluation.rawScore < 0)
    assertEquals("Clamped score must not drop below 0", 0, evaluation.clampedScore)
  }

  @Test
  fun `safer route is recommended over direct route when detour is practical`() {
    // Route 1 (Direct): 1000m, but has 2 unsafe area hazards -> score ~46
    val directRoute = ScoredRouteOption(
      id = "direct",
      title = "Direct Route",
      subtitle = "Shortest path",
      points = testRoutePoints,
      distanceMeters = 1000.0,
      durationSeconds = 740.0,
      safetyScore = 46,
      hazardCountNearby = 2,
      hazardsOnRoute = emptyList(),
      isRecommended = false,
      recommendationReason = ""
    )

    // Route 2 (Alternative): 1150m (+15% detour <= 35% limit), but clean (zero hazards) -> score 100
    val alternativeRoute = ScoredRouteOption(
      id = "safer_corridor",
      title = "Safer Avenue",
      subtitle = "Well-lit avenue corridor",
      points = testRoutePoints,
      distanceMeters = 1150.0,
      durationSeconds = 850.0,
      safetyScore = 100,
      hazardCountNearby = 0,
      hazardsOnRoute = emptyList(),
      isRecommended = false,
      recommendationReason = ""
    )

    val evaluated = RouteSafetyScorer.evaluateAndRecommendRoutes(listOf(directRoute, alternativeRoute))
    val recommended = evaluated.first { it.isRecommended }

    assertEquals("Safer corridor must be recommended", "safer_corridor", recommended.id)
    assertTrue("Recommendation reason must highlight safety benefit", recommended.recommendationReason.contains("Safer route recommended"))
  }

  @Test
  fun `excessive detour is NOT recommended even if safer (practicality threshold)`() {
    // Route 1 (Direct): 1000m, score 75
    val directRoute = ScoredRouteOption(
      id = "direct",
      title = "Direct Route",
      subtitle = "Shortest path",
      points = testRoutePoints,
      distanceMeters = 1000.0,
      durationSeconds = 740.0,
      safetyScore = 75,
      hazardCountNearby = 1,
      hazardsOnRoute = emptyList(),
      isRecommended = false,
      recommendationReason = ""
    )

    // Route 2 (Alternative): 2500m (2.5x detour > 1.35 limit), score 100
    val extremeDetourRoute = ScoredRouteOption(
      id = "extreme_detour",
      title = "Distant Bypass",
      subtitle = "Far perimeter",
      points = testRoutePoints,
      distanceMeters = 2500.0,
      durationSeconds = 1850.0,
      safetyScore = 100,
      hazardCountNearby = 0,
      hazardsOnRoute = emptyList(),
      isRecommended = false,
      recommendationReason = ""
    )

    val evaluated = RouteSafetyScorer.evaluateAndRecommendRoutes(listOf(directRoute, extremeDetourRoute))
    val recommended = evaluated.first { it.isRecommended }

    assertEquals("Direct route must be recommended when alternative is an impractical detour", "direct", recommended.id)
  }

  @Test
  fun `single candidate route evaluation provides clean advisory`() {
    val single = ScoredRouteOption(
      id = "only_route",
      title = "Single Corridor",
      subtitle = "Main street",
      points = testRoutePoints,
      distanceMeters = 1200.0,
      durationSeconds = 900.0,
      safetyScore = 100,
      hazardCountNearby = 0,
      hazardsOnRoute = emptyList(),
      isRecommended = false,
      recommendationReason = ""
    )

    val evaluated = RouteSafetyScorer.evaluateAndRecommendRoutes(listOf(single))
    assertEquals(1, evaluated.size)
    assertTrue(evaluated[0].isRecommended)
    assertTrue(evaluated[0].recommendationReason.contains("Clear route"))
  }

  @Test
  fun `invalid and corrupted coordinates are safely filtered`() {
    assertFalse(RouteSafetyScorer.isValidCoordinate(Double.NaN, 0.0))
    assertFalse(RouteSafetyScorer.isValidCoordinate(0.0, Double.POSITIVE_INFINITY))
    assertFalse(RouteSafetyScorer.isValidCoordinate(95.0, -122.0)) // Lat > 90
    assertFalse(RouteSafetyScorer.isValidCoordinate(37.0, 190.0)) // Lng > 180
    assertFalse(RouteSafetyScorer.isValidCoordinate(0.0, 0.0)) // Placeholder (0,0)
    assertTrue(RouteSafetyScorer.isValidCoordinate(37.7749, -122.4194))
  }
}
