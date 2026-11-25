package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationType
import com.example.model.SafetyNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  notifications: List<SafetyNotification>,
  isNotificationPermissionGranted: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onSendTestNotification: () -> Unit,
  onMarkAsRead: (String) -> Unit,
  onMarkAllAsRead: () -> Unit,
  onDeleteNotification: (String) -> Unit,
  onClearAll: () -> Unit,
  onNavigateToScreen: (String) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
  var testNotificationSentMessage by remember { mutableStateOf<String?>(null) }

  val filteredNotifications = remember(notifications, selectedFilter) {
    if (selectedFilter == null) notifications
    else notifications.filter { it.type == selectedFilter }
  }

  val unreadCount = remember(notifications) {
    notifications.count { !it.isRead }
  }

  androidx.activity.compose.BackHandler(onBack = onBackClick)

  Scaffold(
    topBar = {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = onBackClick,
              modifier = Modifier.testTag("notifications_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1E293B)
              )
            }
            Text(
              text = "Safety Notifications",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1E293B)
              )
            )
            if (unreadCount > 0) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF2563EB),
                modifier = Modifier.padding(start = 6.dp)
              ) {
                Text(
                  text = "$unreadCount new",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }

          if (notifications.isNotEmpty()) {
            IconButton(
              onClick = onClearAll,
              modifier = Modifier.testTag("button_clear_all_notifications")
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = "Clear All",
                tint = Color(0xFF64748B)
              )
            }
          }
        }
      }
    },
    containerColor = Color(0xFFF8FAFC)
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFFF8FAFC))
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

      // Notification Permission Banner (if Android 13+ permission not granted)
      if (!isNotificationPermissionGranted) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("permission_banner_notifications")
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsOff,
                  contentDescription = null,
                  tint = Color(0xFFD97706),
                  modifier = Modifier.size(22.dp)
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "System Notifications Disabled",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                  )
                )
                Text(
                  text = "Enable notifications so you never miss SOS emergency dispatches or arrival alerts.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB45309),
                    fontSize = 12.sp
                  )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = onRequestNotificationPermission,
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("button_enable_system_notifications")
                ) {
                  Text("Enable Notifications", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
            }
          }
        }
      }

      // Quick Actions: Test System Notification & Mark All Read
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.White,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  onSendTestNotification()
                  testNotificationSentMessage = "Test alert sent to device tray!"
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier
                  .weight(1.3f)
                  .height(44.dp)
                  .testTag("button_send_test_notification")
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Send Test Alert",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }

              if (unreadCount > 0) {
                OutlinedButton(
                  onClick = onMarkAllAsRead,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp)
                    .testTag("button_mark_all_read"),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                  border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                  Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Mark All Read",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                  )
                }
              }
            }

            AnimatedVisibility(visible = testNotificationSentMessage != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Text(
                      text = testNotificationSentMessage ?: "",
                      style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                    )
                  }
                  IconButton(
                    onClick = { testNotificationSentMessage = null },
                    modifier = Modifier.size(20.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }
      }

      // Filter Chips
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FilterChip(
              selected = selectedFilter == null,
              onClick = { selectedFilter = null },
              label = { Text("All (${notifications.size})") },
              leadingIcon = if (selectedFilter == null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF2563EB),
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("filter_all_notifications")
            )
          }

          items(NotificationType.values()) { type ->
            val count = notifications.count { it.type == type }
            val labelText = when (type) {
              NotificationType.EMERGENCY -> "🚨 Emergencies ($count)"
              NotificationType.SAFE_ARRIVAL -> "🎉 Arrivals ($count)"
              NotificationType.JOURNEY_ALERT -> "⚠️ Trips ($count)"
              NotificationType.HAZARD -> "🛡️ Hazards ($count)"
              NotificationType.SYSTEM -> "⚙️ System ($count)"
            }
            FilterChip(
              selected = selectedFilter == type,
              onClick = { selectedFilter = if (selectedFilter == type) null else type },
              label = { Text(labelText) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF2563EB),
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("filter_${type.name.lowercase()}")
            )
          }
        }
      }

      // Notification Items
      if (filteredNotifications.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 16.dp)
              .testTag("empty_notifications_card")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.Notifications,
                  contentDescription = null,
                  tint = Color(0xFF2563EB),
                  modifier = Modifier.size(32.dp)
                )
              }
              Text(
                text = "No Notifications",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1E293B)
                )
              )
              Text(
                text = if (selectedFilter != null) {
                  "No notifications found in this category."
                } else {
                  "You're all caught up! Real-time alerts, arrival confirmations, and community updates will appear here."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                  color = Color(0xFF64748B),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              )
              Button(
                onClick = {
                  onSendTestNotification()
                  testNotificationSentMessage = "Test alert sent to device tray!"
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.padding(top = 8.dp)
              ) {
                Text("Send a Test Alert")
              }
            }
          }
        }
      } else {
        items(filteredNotifications, key = { it.id }) { notif ->
          NotificationCard(
            notification = notif,
            onCardClick = {
              onMarkAsRead(notif.id)
              notif.targetScreenRoute?.let { onNavigateToScreen(it) }
            },
            onDelete = { onDeleteNotification(notif.id) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun NotificationCard(
  notification: SafetyNotification,
  onCardClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (badgeBg, iconColor, icon) = when (notification.type) {
    NotificationType.EMERGENCY -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.Emergency)
    NotificationType.SAFE_ARRIVAL -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Icons.Default.CheckCircle)
    NotificationType.JOURNEY_ALERT -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.DirectionsWalk)
    NotificationType.HAZARD -> Triple(Color(0xFFFFEDD5), Color(0xFFEA580C), Icons.Default.WarningAmber)
    NotificationType.SYSTEM -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Icons.Default.Shield)
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (!notification.isRead) Color(0xFFF1F5F9) else Color.White,
    border = BorderStroke(
      1.dp,
      if (!notification.isRead) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
    ),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onCardClick() }
      .testTag("notification_card_${notification.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(badgeBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = notification.title,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                color = Color(0xFF1E293B)
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (!notification.isRead) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF2563EB))
              )
            }
          }

          Text(
            text = notification.getRelativeTime(),
            style = MaterialTheme.typography.labelSmall.copy(
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = notification.message,
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color(0xFF475569),
            lineHeight = 18.sp
          )
        )

        if (notification.targetScreenRoute != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Tap to view details",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold
              )
            )
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(28.dp)
          .testTag("button_delete_notification_${notification.id}")
      ) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = "Dismiss",
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
