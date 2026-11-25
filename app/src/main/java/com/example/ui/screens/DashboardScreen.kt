package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppMode
import com.example.model.BottomTab
import com.example.model.TrustedContact
import com.example.model.UserProfile
import com.example.ui.components.SereneBottomNavBar
import com.example.ui.components.SereneTopAppBar
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
  userProfile: UserProfile,
  trustedContacts: List<TrustedContact>,
  currentMode: AppMode,
  currentTab: BottomTab,
  userLocation: com.example.model.UserLocationState = com.example.model.UserLocationState(),
  onRequestLocationPermission: () -> Unit = {},
  onRefreshLocation: () -> Unit = {},
  onModeToggle: (AppMode) -> Unit,
  onTabSelected: (BottomTab) -> Unit,
  onStartJourneyClick: () -> Unit,
  onManageContactsClick: () -> Unit,
  onAddContactClick: () -> Unit,
  onRecentJourneyClick: () -> Unit,
  onNotificationClick: () -> Unit,
  unreadNotificationsCount: Int = 0,
  onSosClick: () -> Unit = {},
  onOpenMessagesApp: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      SereneTopAppBar(
        title = "Safe Companion",
        avatarUrl = userProfile.avatarUrl,
        currentMode = currentMode,
        showModeToggle = true,
        onModeToggle = onModeToggle,
        onNotificationClick = onNotificationClick,
        unreadNotificationsCount = unreadNotificationsCount,
        onAvatarClick = { onTabSelected(BottomTab.PROFILE) }
      )
    },
    bottomBar = {
      SereneBottomNavBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFFF8FAFC))
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header Greeting
      Column(modifier = Modifier.padding(top = 4.dp)) {
        val greetingTarget = if (userProfile.name.isNotBlank()) userProfile.name else "Friend"
        Text(
          text = "Good morning, $greetingTarget",
          style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "You're ready for a safe journey.",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF64748B),
            fontSize = 15.sp
          )
        )
      }

      // Safety Status Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("status_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = "Shield",
                  tint = Color(0xFF2563EB),
                  modifier = Modifier.size(24.dp)
                )
              }
              Column {
                Text(
                  text = "System Status",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                  )
                )
                Text(
                  text = if (!userLocation.isPermissionGranted) "Location permission required"
                  else if (userLocation.isLocating) "Acquiring accurate GPS..."
                  else "GPS Active · High Accuracy",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (!userLocation.isPermissionGranted) Color(0xFFEA580C) else Color(0xFF64748B),
                    fontSize = 13.sp
                  )
                )
              }
            }

            if (!userLocation.isPermissionGranted) {
              Button(
                onClick = onRequestLocationPermission,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF2563EB),
                  contentColor = Color.White
                )
              ) {
                Text(
                  text = "Enable GPS",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
              }
            } else {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFDCFCE7)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF22C55E))
                  )
                  Text(
                    text = if (userLocation.accuracyMeters > 0) "±${userLocation.accuracyMeters.toInt()}m" else "Ready",
                    style = MaterialTheme.typography.labelMedium.copy(
                      color = Color(0xFF15803D),
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  )
                }
              }
            }
          }

          if (userLocation.isPermissionGranted && userLocation.hasRealFix) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFF8FAFC),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = userLocation.address,
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color(0xFF1E293B),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                  )
                }
                IconButton(
                  onClick = onRefreshLocation,
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh GPS",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Primary Action: Start a Safe Journey (80dp height)
      Button(
        onClick = onStartJourneyClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(76.dp)
          .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
          .testTag("start_safe_journey_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF2563EB),
          contentColor = Color.White
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsWalk,
            contentDescription = "Walk",
            modifier = Modifier.size(28.dp),
            tint = Color.White
          )
          Text(
            text = "Start a Safe Journey",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
          )
        }
      }

      // Emergency SOS & SMS Fallback Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEF2F2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_sos_card")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Emergency SOS & SMS",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF991B1B)
                )
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFFEE2E2)
            ) {
              Text(
                text = "SMS Ready",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color(0xFFDC2626),
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }

          Text(
            text = "Instantly notify trusted contacts with live GPS coordinates, Google Maps link, and emergency audio note via SMS.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFF7F1D1D),
              lineHeight = 16.sp
            )
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onSosClick,
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("button_dashboard_sos"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
              Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Trigger SOS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
              onClick = onOpenMessagesApp,
              modifier = Modifier
                .weight(1.3f)
                .height(44.dp)
                .testTag("button_dashboard_open_messages"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF991B1B)),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
              Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Check in Messages", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }

      // Recent Journey Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onRecentJourneyClick() }
          .testTag("recent_journey_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "RECENT JOURNEY",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Visual timeline dots
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFCBD5E1))
              )
              Box(
                modifier = Modifier
                  .width(2.dp)
                  .height(24.dp)
                  .background(Color(0xFFE2E8F0))
              )
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .border(2.dp, Color(0xFF2563EB), CircleShape)
                  .background(Color.White)
              )
            }

            // Text Info
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "College Campus",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                  )
                )
                Text(
                  text = "Yesterday · 6:42 PM",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                  )
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Home",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                  )
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "Safe Arrival",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = Color(0xFF15803D),
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
            }
          }
        }
      }

      // Trusted Contacts Section
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TRUSTED CONTACTS",
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          )
          Row(
            modifier = Modifier
              .clickable { onManageContactsClick() }
              .padding(4.dp)
              .testTag("manage_contacts_button"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Manage",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            )
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // Horizontal Contacts Carousel
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          trustedContacts.forEach { contact ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .width(68.dp)
                .clickable { onManageContactsClick() }
            ) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .border(
                    width = if (contact.isPrimary) 2.dp else 1.dp,
                    color = if (contact.isPrimary) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                    shape = CircleShape
                  )
                  .padding(2.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
              ) {
                if (contact.avatarUrl != null) {
                  AsyncImage(
                    model = contact.avatarUrl,
                    contentDescription = contact.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                  )
                } else {
                  Text(
                    text = contact.initials,
                    style = MaterialTheme.typography.labelLarge.copy(
                      color = Color(0xFF2563EB),
                      fontWeight = FontWeight.Bold
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = contact.name.split(" ").first(),
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color(0xFF1E293B),
                  fontWeight = FontWeight.Medium
                ),
                maxLines = 1
              )
            }
          }

          // Add Contact Item
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .width(68.dp)
              .clickable { onAddContactClick() }
              .testTag("dashboard_add_contact_button")
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color(0xFFCBD5E1), CircleShape)
                .background(Color.White),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Contact",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Add",
              style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
              )
            )
          }
        }
      }

      // Status Footer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (userLocation.hasRealFix) Icons.Default.GpsFixed else Icons.Default.Wifi,
          contentDescription = null,
          tint = if (userLocation.hasRealFix) Color(0xFF16A34A) else Color(0xFF64748B),
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (userLocation.hasRealFix) "GPS: ${userLocation.address} · Accurate" else if (userLocation.isPermissionGranted) "Acquiring GPS location..." else "Location permission disabled",
          style = MaterialTheme.typography.labelSmall.copy(
            color = Color(0xFF64748B),
            fontSize = 12.sp
          ),
          maxLines = 1
        )
      }
    }
  }
}

