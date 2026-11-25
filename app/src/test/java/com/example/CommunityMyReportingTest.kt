package com.example

import com.example.model.HazardReport
import com.example.model.HazardStatus
import com.example.model.HazardType
import com.example.viewmodel.SafeCompanionViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommunityMyReportingTest {

  private lateinit var viewModel: SafeCompanionViewModel

  @Before
  fun setUp() {
    viewModel = SafeCompanionViewModel()
  }

  @Test
  fun test1_initialHazardReportsContainUserReportedItems() {
    val allReports = viewModel.hazardReports.value
    assertTrue("Should have initial hazard reports", allReports.isNotEmpty())

    val myReports = allReports.filter { it.isReportedByMe }
    assertTrue("Initial reports should contain items reported by the user", myReports.isNotEmpty())

    // Verify properties of user reports
    val firstUserReport = myReports.first()
    assertEquals(HazardType.BROKEN_STREETLIGHT, firstUserReport.type)
    assertTrue(firstUserReport.isReportedByMe)
  }

  @Test
  fun test2_submittingNewHazardSetsIsReportedByMeToTrue() {
    viewModel.submitHazardReport(
      type = HazardType.POTHOLE,
      address = "Market & 4th Crosswalk",
      description = "Large pothole in the crosswalk",
      photoUrl = null,
      customId = "test_hazard_my_report_1"
    )

    val allReports = viewModel.hazardReports.value
    val newReport = allReports.find { it.id == "test_hazard_my_report_1" }

    assertNotNull("New report should be present in hazard reports", newReport)
    assertEquals(true, newReport?.isReportedByMe)
    assertEquals("Market & 4th Crosswalk", newReport?.locationAddress)
    assertEquals(HazardStatus.REPORTED, newReport?.status)
  }

  @Test
  fun test3_deletingHazardReportRemovesItFromMyReporting() {
    viewModel.submitHazardReport(
      type = HazardType.BLOCKED_ROAD,
      address = "7th & Folsom St",
      description = "Fallen construction scaffolding",
      photoUrl = null,
      customId = "test_hazard_delete_me"
    )

    assertTrue(viewModel.hazardReports.value.any { it.id == "test_hazard_delete_me" })

    viewModel.deleteHazardReport("test_hazard_delete_me")

    assertFalse(
      "Deleted hazard report should no longer exist",
      viewModel.hazardReports.value.any { it.id == "test_hazard_delete_me" }
    )
  }

  @Test
  fun test4_filteringMyReportsByStatus() {
    val allReports = viewModel.hazardReports.value
    val myReports = allReports.filter { it.isReportedByMe }

    val underReviewReports = myReports.filter { it.status == HazardStatus.UNDER_REVIEW }
    val verifiedReports = myReports.filter { it.status == HazardStatus.VERIFIED }

    assertTrue("Should find at least one user report under review", underReviewReports.isNotEmpty())
    assertTrue("Should find at least one verified user report", verifiedReports.isNotEmpty())
  }
}
