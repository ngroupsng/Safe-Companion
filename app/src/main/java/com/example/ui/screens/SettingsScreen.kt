package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.AppPermissions
import com.example.model.BottomTab
import com.example.model.TrustedContact
import com.example.model.UserProfile
import com.example.ui.components.SereneBottomNavBar
import com.example.ui.components.SereneDetailTopBar
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  userProfile: UserProfile,
  trustedContacts: List<TrustedContact>,
  permissions: AppPermissions,
  currentTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
  onOpenSavedPlaces: () -> Unit = {},
  onOpenTrustedContacts: () -> Unit,
  onOpenPermissions: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenNotifications: () -> Unit = {},
  onRestartOnboarding: () -> Unit,
  onLogout: () -> Unit = {},
  onUpdateProfile: (String, String, String) -> Unit = { _, _, _ -> },
  onAddContact: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
  onRemoveContact: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showAddContactDialog by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }

  androidx.activity.compose.BackHandler {
    onTabSelected(BottomTab.HOME)
  }

  Scaffold(
    topBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(1.dp),
        color = MaterialTheme.colorScheme.surface
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .padding(horizontal = 20.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = "Settings & Profile",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurface
            )
          )
        }
      }
    },
    bottomBar = {
      SereneBottomNavBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // User Profile Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(SurfaceContainerHigh)
              .border(2.dp, PrimaryFixedDim, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            if (userProfile.avatarUrl.isNotBlank()) {
              AsyncImage(
                model = userProfile.avatarUrl,
                contentDescription = userProfile.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (userProfile.name.isNotBlank()) userProfile.name else "Set up your profile",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
              )
            )
            Text(
              text = if (userProfile.phone.isNotBlank()) userProfile.phone else "Add phone number",
              style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
            )
            Text(
              text = if (userProfile.email.isNotBlank()) userProfile.email else "Add email address",
              style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
            )
          }

          IconButton(
            onClick = { showEditProfileDialog = true },
            modifier = Modifier.testTag("button_edit_profile")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Primary)
          }
        }
      }

      // Places & Addresses Section
      SettingsSectionHeader("LOCATIONS & PLACES")

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          val homeAddress = userProfile.savedPlaces.firstOrNull { it.isHome }?.address ?: "742 Evergreen Terrace, San Francisco, CA"
          SettingsRowItem(
            icon = Icons.Default.Home,
            title = "Home & Saved Addresses",
            subtitle = "Home: $homeAddress",
            onClick = onOpenSavedPlaces,
            testTag = "settings_saved_places"
          )
        }
      }

      // Safety Section
      SettingsSectionHeader("SAFETY")

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Manage Contacts Header / Nav row
          SettingsRowItem(
            icon = Icons.Default.Group,
            title = "Trusted Contacts",
            subtitle = if (trustedContacts.isEmpty()) "No contacts added yet — tap to add" else "${trustedContacts.size} contact${if (trustedContacts.size == 1) "" else "s"} configured",
            onClick = onOpenTrustedContacts,
            testTag = "settings_trusted_contacts"
          )

          if (trustedContacts.isEmpty()) {
            Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "No emergency contacts configured. Add close family or friends who should receive your live route updates and SOS alerts.",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = OnSurfaceVariant,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                  lineHeight = 18.sp
                )
              )
              Button(
                onClick = { showAddContactDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("settings_button_add_contact_empty")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Contact", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
              }
            }
          } else {
            Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)

            // Direct contacts list inside settings
            trustedContacts.forEachIndexed { index, contact ->
              if (index > 0) {
                Divider(color = OutlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(if (contact.isPrimary) PrimaryFixedDim else SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = contact.initials,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (contact.isPrimary) OnPrimaryFixed else Primary
                      )
                    )
                  }

                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = OnSurface
                        )
                      )
                      if (contact.isPrimary) {
                        Surface(
                          shape = RoundedCornerShape(999.dp),
                          color = PrimaryContainer
                        ) {
                          Text(
                            text = "Primary",
                            style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 9.sp,
                              color = OnPrimaryContainer,
                              fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                          )
                        }
                      }
                    }
                    Text(
                      text = if (contact.phone.isNotBlank()) "${contact.relationship} · ${contact.phone}" else contact.relationship,
                      style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
                    )
                  }
                }

                IconButton(
                  onClick = { onRemoveContact(contact.id) },
                  modifier = Modifier.size(32.dp).testTag("settings_remove_contact_${contact.id}")
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Contact",
                    tint = Error.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = { showAddContactDialog = true },
                modifier = Modifier.testTag("settings_button_add_contact")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Contact", color = Primary, fontWeight = FontWeight.Bold)
              }

              TextButton(
                onClick = onOpenTrustedContacts,
                modifier = Modifier.testTag("settings_button_manage_all_contacts")
              ) {
                Text("Manage All", color = OnSurfaceVariant, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = OnSurfaceVariant)
              }
            }
          }

          Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
          SettingsRowItem(
            icon = Icons.Default.Security,
            title = "Permissions",
            subtitle = "Location, Notifications, Background",
            onClick = onOpenPermissions,
            testTag = "settings_permissions"
          )
          Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
          SettingsRowItem(
            icon = Icons.Default.Notifications,
            title = "Safety Notifications",
            subtitle = "Alert logs, push diagnostics & arrival alerts",
            onClick = onOpenNotifications,
            testTag = "settings_notifications"
          )
        }
      }

      // Privacy Section
      SettingsSectionHeader("PRIVACY & DATA")

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          SettingsRowItem(
            icon = Icons.Default.Lock,
            title = "Privacy & Safety Policy",
            subtitle = "Encrypted trusted-circle sharing rules",
            onClick = onOpenPrivacy,
            testTag = "settings_privacy"
          )
        }
      }

      // App Experience Section
      SettingsSectionHeader("APP EXPERIENCE")

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          SettingsRowItem(
            icon = Icons.Default.PlayCircle,
            title = "Replay Onboarding Flow",
            subtitle = "Review the 7-step welcome guide",
            onClick = onRestartOnboarding,
            testTag = "settings_restart_onboarding"
          )
          Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
          SettingsRowItem(
            icon = Icons.Default.AccountCircle,
            title = "Log Out / Switch User",
            subtitle = "Clear session and return to onboarding",
            onClick = { showLogoutDialog = true },
            testTag = "settings_logout_button",
            showChevron = false
          )
          Divider(color = OutlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
          SettingsRowItem(
            icon = Icons.Default.Info,
            title = "About Safe Companion",
            subtitle = "Version 1.0.0 · Safe-by-Design Architecture",
            onClick = {},
            showChevron = false
          )
        }
      }

      Spacer(modifier = Modifier.height(48.dp))
    }
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Log Out of Safe Companion") },
      text = { Text("Are you sure you want to log out? This will clear your saved session details on this device.") },
      confirmButton = {
        Button(
          onClick = {
            showLogoutDialog = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showEditProfileDialog) {
    EditProfileDialog(
      currentProfile = userProfile,
      onUpdateProfile = onUpdateProfile,
      onDismiss = { showEditProfileDialog = false }
    )
  }

  if (showAddContactDialog) {
    AddContactDialog(
      onDismiss = { showAddContactDialog = false },
      onConfirm = { name, rel, phone, isPrimary ->
        onAddContact(name, rel, phone, isPrimary)
        showAddContactDialog = false
      }
    )
  }
}

@Composable
fun TrustedContactsManagerScreen(
  trustedContacts: List<TrustedContact>,
  onBackClick: () -> Unit,
  onAddContact: (String, String, String, Boolean) -> Unit,
  onRemoveContact: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onBackClick)
  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Trusted Contacts",
        onBackClick = onBackClick,
        trailingIcon = {
          IconButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("button_add_contact_topbar")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = Primary)
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add Contact") },
        containerColor = Primary,
        contentColor = OnPrimary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("fab_add_contact")
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (trustedContacts.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(PrimaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.GroupAdd,
                  contentDescription = null,
                  tint = OnPrimaryContainer,
                  modifier = Modifier.size(32.dp)
                )
              }
              Text(
                text = "No Trusted Contacts Added",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Bold,
                  color = OnSurface,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
              )
              Text(
                text = "Add close family members, partners, or friends who should receive your live trip progress and immediate notifications during emergency SOS alerts.",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = OnSurfaceVariant,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                  lineHeight = 20.sp
                )
              )
              Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                  .padding(top = 4.dp)
                  .testTag("button_manager_add_contact_empty")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Contact", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        items(trustedContacts) { contact ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (contact.isPrimary) PrimaryFixedDim else SurfaceContainerHigh),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = contact.initials,
                    style = MaterialTheme.typography.labelLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (contact.isPrimary) OnPrimaryFixed else Primary
                    )
                  )
                }

                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = contact.name,
                      style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                      )
                    )
                    if (contact.isPrimary) {
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PrimaryContainer
                      ) {
                        Text(
                          text = "Primary",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = OnPrimaryContainer,
                            fontWeight = FontWeight.Bold
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                  Text(
                    text = if (contact.phone.isNotBlank()) "${contact.relationship} · ${contact.phone}" else contact.relationship,
                    style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                  )
                }
              }

              IconButton(
                onClick = { onRemoveContact(contact.id) },
                modifier = Modifier.testTag("button_delete_contact_${contact.id}")
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Error)
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddContactDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { name, rel, phone, isPrimary ->
        onAddContact(name, rel, phone, isPrimary)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun PermissionsSettingsScreen(
  permissions: AppPermissions,
  onTogglePermission: (String) -> Unit,
  onRequestPermission: (String) -> Unit = {},
  onTestSmsInMessagesApp: () -> Unit = {},
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onBackClick)
  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Permissions",
        onBackClick = onBackClick
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      PermissionToggleCard(
        title = "Location Access",
        subtitle = "Required for real-time journey monitoring & map navigation",
        isChecked = permissions.location,
        onToggle = {
          if (!permissions.location) onRequestPermission("location")
          onTogglePermission("location")
        }
      )

      PermissionToggleCard(
        title = "Notifications",
        subtitle = "Emergency alerts, route updates, and safe arrival notices",
        isChecked = permissions.notifications,
        onToggle = {
          if (!permissions.notifications) onRequestPermission("notifications")
          onTogglePermission("notifications")
        }
      )

      PermissionToggleCard(
        title = "Background Location",
        subtitle = "Allows journey safety tracking even when your screen is locked",
        isChecked = permissions.backgroundLocation,
        onToggle = {
          if (!permissions.backgroundLocation) onRequestPermission("background")
          onTogglePermission("background")
        }
      )

      PermissionToggleCard(
        title = "SMS Emergency Fallback",
        subtitle = "Sends SMS packet if data connectivity drops during SOS",
        isChecked = permissions.smsFallback,
        onToggle = {
          if (!permissions.smsFallback) onRequestPermission("sms")
          onTogglePermission("sms")
        }
      )

      OutlinedButton(
        onClick = onTestSmsInMessagesApp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .testTag("button_test_sms_messages_app"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
      ) {
        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Send Test SOS to Messages App", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
fun PrivacySettingsScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  androidx.activity.compose.BackHandler(onBack = onBackClick)
  Scaffold(
    topBar = {
      SereneDetailTopBar(
        title = "Privacy & Safety",
        onBackClick = onBackClick
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(text = "Safe-by-Design Principles", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
          Text(
            text = "Safe Companion is built from the ground up with strict privacy guarantees:",
            style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
          )
          Text(
            text = "• Location tracking is strictly ephemeral and tied to active user-initiated journeys.\n• Zero sale or advertisement use of user geolocation logs.\n• Trusted contact sharing requires affirmative opt-in per session.\n• All emergency telemetry is transmitted over encrypted channels.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelMedium.copy(
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = OnSurfaceVariant,
      letterSpacing = 0.5.sp
    ),
    modifier = Modifier.padding(start = 4.dp)
  )
}

@Composable
private fun SettingsRowItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  showChevron: Boolean = true,
  testTag: String = ""
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(SurfaceContainerHigh),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
      }
      Column {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = OnSurface))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant))
      }
    }

    if (showChevron) {
      Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
private fun PermissionToggleCard(
  title: String,
  subtitle: String,
  isChecked: Boolean,
  onToggle: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant))
      }
      Switch(
        checked = isChecked,
        onCheckedChange = { onToggle() },
        colors = SwitchDefaults.colors(
          checkedThumbColor = OnPrimary,
          checkedTrackColor = Primary
        )
      )
    }
  }
}

@Composable
private fun EditProfileDialog(
  currentProfile: UserProfile,
  onUpdateProfile: (String, String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf(currentProfile.name) }
  var phone by remember { mutableStateOf(currentProfile.phone) }
  var email by remember { mutableStateOf(currentProfile.email) }
  var notes by remember { mutableStateOf(currentProfile.emergencyNotes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Profile Details", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          placeholder = { Text("Enter your name") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          placeholder = { Text("Enter your phone number") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          placeholder = { Text("Enter your email address") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Emergency / Medical Notes") },
          placeholder = { Text("e.g. Allergies or special notes") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onUpdateProfile(name, phone, email)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
