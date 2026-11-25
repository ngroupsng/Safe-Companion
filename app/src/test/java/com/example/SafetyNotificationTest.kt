package com.example

import com.example.model.NotificationType
import com.example.model.SafetyNotification
import com.example.viewmodel.SafeCompanionViewModel
import org.junit.Assert.*
import org.junit.Test

class SafetyNotificationTest {

  @Test
  fun safetyNotification_relativeTimeCalculation() {
    val now = System.currentTimeMillis()
    val justNowNotif = SafetyNotification(
      title = "Alert",
      message = "Just happened",
      timestamp = now - 5000L,
      type = NotificationType.SYSTEM
    )
    assertEquals("Just now", justNowNotif.getRelativeTime())

    val tenMinNotif = SafetyNotification(
      title = "Alert",
      message = "10 mins ago",
      timestamp = now - 10 * 60 * 1000L,
      type = NotificationType.SAFE_ARRIVAL
    )
    assertEquals("10m ago", tenMinNotif.getRelativeTime())
  }

  @Test
  fun viewModel_notificationStateManagement() {
    val vm = SafeCompanionViewModel()
    val initialList = vm.notifications.value
    assertTrue("Default notifications should not be empty", initialList.isNotEmpty())

    val newNotif = SafetyNotification(
      id = "test_1",
      title = "Custom Alert",
      message = "Testing message",
      type = NotificationType.JOURNEY_ALERT,
      isRead = false
    )
    vm.addNotification(newNotif)
    assertTrue("Newly added notification should be present", vm.notifications.value.any { it.id == "test_1" })

    vm.markNotificationAsRead("test_1")
    val found = vm.notifications.value.first { it.id == "test_1" }
    assertTrue("Notification should be marked as read", found.isRead)

    vm.deleteNotification("test_1")
    assertFalse("Notification should be deleted", vm.notifications.value.any { it.id == "test_1" })

    vm.clearAllNotifications()
    assertTrue("All notifications should be cleared", vm.notifications.value.isEmpty())
  }
}
