package com.example

import com.example.viewmodel.SafeCompanionViewModel
import com.example.viewmodel.Screen
import org.junit.Assert.*
import org.junit.Test

class UserPreferencesTest {

  @Test
  fun `loginAndSaveDetails updates profile and routes directly to Dashboard`() {
    val viewModel = SafeCompanionViewModel()
    
    // Initial state is Onboarding
    assertEquals(Screen.Onboarding.route, viewModel.currentScreen.value)
    assertFalse(viewModel.isOnboardingCompleted.value)

    // User logs in and saves details
    viewModel.loginAndSaveDetails(
      name = "Alex Morgan",
      email = "alex.morgan@safecompanion.app",
      phone = "+1 (555) 019-2834"
    )

    // Verify screen immediately routes to Dashboard and onboarding is marked completed
    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)
    assertTrue(viewModel.isOnboardingCompleted.value)
    assertEquals("Alex Morgan", viewModel.userProfile.value.name)
    assertEquals("alex.morgan@safecompanion.app", viewModel.userProfile.value.email)
    assertEquals("+1 (555) 019-2834", viewModel.userProfile.value.phone)
  }

  @Test
  fun `saveProfileAndComplete saves details and prevents onboarding from showing`() {
    val viewModel = SafeCompanionViewModel()

    viewModel.saveProfileAndComplete(
      name = "Jane Doe",
      phone = "+1 (555) 987-6543",
      email = "jane.doe@example.com"
    )

    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)
    assertTrue(viewModel.isOnboardingCompleted.value)
    assertEquals("Jane Doe", viewModel.userProfile.value.name)
    assertEquals("+1 (555) 987-6543", viewModel.userProfile.value.phone)
    assertEquals("jane.doe@example.com", viewModel.userProfile.value.email)
  }

  @Test
  fun `completeOnboarding transitions to Dashboard`() {
    val viewModel = SafeCompanionViewModel()
    assertFalse(viewModel.isOnboardingCompleted.value)

    viewModel.completeOnboarding()

    assertTrue(viewModel.isOnboardingCompleted.value)
    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)
  }

  @Test
  fun `logoutUser resets session back to onboarding`() {
    val viewModel = SafeCompanionViewModel()
    viewModel.loginAndSaveDetails("Alex Morgan", "alex@example.com", "555-1234")
    assertEquals(Screen.Dashboard.route, viewModel.currentScreen.value)

    viewModel.logoutUser()

    assertFalse(viewModel.isOnboardingCompleted.value)
    assertEquals(Screen.Onboarding.route, viewModel.currentScreen.value)
  }
}
