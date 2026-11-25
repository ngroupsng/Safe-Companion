package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPermissions
import com.example.model.TrustedContact
import com.example.model.UserProfile
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
  step: Int,
  userProfile: UserProfile,
  permissions: AppPermissions,
  trustedContacts: List<TrustedContact>,
  onRequestLocationPermission: () -> Unit = {},
  onRequestPermission: (String) -> Unit = {},
  onNext: () -> Unit,
  onPrev: () -> Unit,
  onStepSelected: (Int) -> Unit,
  onUpdateProfile: (String, String, String) -> Unit,
  onSaveProfileDetails: (String, String, String) -> Unit = { _, _, _ -> },
  onLogin: (String, String, String) -> Unit = { _, _, _ -> },
  onTogglePermission: (String) -> Unit,
  onAddContact: (String, String, String, Boolean) -> Unit,
  onRemoveContact: (String) -> Unit = {},
  onComplete: () -> Unit,
  isRecordingVoice: Boolean = false,
  isPlayingVoice: Boolean = false,
  onStartRecordVoice: () -> Unit = {},
  onStopRecordVoice: (String?) -> Unit = {},
  onPlayVoice: () -> Unit = {},
  onStopPlayVoice: () -> Unit = {},
  onUpdateVoiceTranscription: (String) -> Unit = {},
  onClearVoice: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showLoginDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .statusBarsPadding()
      ) {
        // App Title in Header
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
          contentAlignment = Alignment.Center
        ) {
          if (step > 1) {
            IconButton(
              onClick = onPrev,
              modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag("onboarding_back_button")
            ) {
              Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Primary
              )
            }
          }

          Text(
            text = "Safe Companion",
            style = MaterialTheme.typography.headlineMedium.copy(
              color = Primary,
              fontWeight = FontWeight.Bold
            )
          )

          if (step < 8) {
            TextButton(
              onClick = onComplete,
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("onboarding_skip_button")
            ) {
              Text(
                text = "Skip",
                style = MaterialTheme.typography.labelMedium.copy(color = OnSurfaceVariant)
              )
            }
          }
        }

        // Progress Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          for (i in 1..8) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                  if (i <= step) Primary else SurfaceVariant
                )
            )
          }
        }
      }
    },
    bottomBar = {
      // Bottom sticky Action
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Button(
            onClick = {
              if (step < 8) onNext() else onComplete()
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("onboarding_next_button"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Primary,
              contentColor = OnPrimary
            )
          ) {
            Text(
              text = when (step) {
                1 -> "Get Started"
                3 -> "Save Details & Continue"
                8 -> "Go to Dashboard"
                else -> "Continue"
              },
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          if (step == 1) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = { showLoginDialog = true },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_login_button"),
              shape = RoundedCornerShape(999.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Already registered? Log In",
                style = MaterialTheme.typography.labelLarge.copy(color = Primary, fontWeight = FontWeight.SemiBold)
              )
            }
          }

          if (step == 4) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "You can change these anytime in Safety Settings.",
              style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(innerPadding)
    ) {
      when (step) {
        1 -> WelcomeStepView()
        2 -> WhySafeCompanionStepView()
        3 -> ProfileSetupStepView(
          userProfile = userProfile,
          onUpdateProfile = onUpdateProfile,
          onSaveProfileDetails = onSaveProfileDetails,
          onOpenLogin = { showLoginDialog = true }
        )
        4 -> PermissionsStepView(
          permissions = permissions,
          onTogglePermission = onTogglePermission,
          onRequestPermission = onRequestPermission
        )
        5 -> TrustedContactsStepView(
          trustedContacts = trustedContacts,
          onAddContact = onAddContact,
          onRemoveContact = onRemoveContact
        )
        6 -> PreRecordedVoiceNoteStepView(
          userProfile = userProfile,
          isRecording = isRecordingVoice,
          isPlaying = isPlayingVoice,
          onStartRecording = onStartRecordVoice,
          onStopRecording = onStopRecordVoice,
          onPlayVoice = onPlayVoice,
          onStopPlayVoice = onStopPlayVoice,
          onUpdateTranscription = onUpdateVoiceTranscription,
          onClearVoice = onClearVoice
        )
        7 -> PrivacySafetyStepView()
        8 -> SetupCompleteStepView(userName = userProfile.name)
      }
    }
  }

  if (showLoginDialog) {
    LoginDialog(
      onDismiss = { showLoginDialog = false },
      onLoginSuccess = { name, email, phone ->
        showLoginDialog = false
        onLogin(name, email, phone)
      }
    )
  }
}

@Composable
fun LoginDialog(
  onDismiss: () -> Unit,
  onLoginSuccess: (name: String, email: String, phone: String) -> Unit
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Primary)
        Text(
          text = "Log In to Safe Companion",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Log in or sign in with your details to access your safety network and skip onboarding.",
          style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
        )

        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            errorMessage = null
          },
          label = { Text("Email Address") },
          placeholder = { Text("alex@example.com") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Primary) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_email")
        )

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            errorMessage = null
          },
          label = { Text("Full Name (Optional)") },
          placeholder = { Text("Alex Morgan") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Primary) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_name")
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number (Optional)") },
          placeholder = { Text("+1 (555) 019-2834") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Primary) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_phone")
        )

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Primary) },
          visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_password")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
          )
        }

        Divider(color = OutlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

        FilledTonalButton(
          onClick = {
            onLoginSuccess("Alex Morgan", "alex.morgan@safecompanion.app", "+1 (555) 019-2834")
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("login_btn_quick_demo"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("One-Tap Demo Login (Alex Morgan)")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (email.isBlank() && name.isBlank()) {
            errorMessage = "Please enter an email or full name to continue"
          } else {
            val finalName = if (name.isNotBlank()) name else email.substringBefore("@").replace(".", " ")
            onLoginSuccess(finalName, email, phone)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("onboarding_dialog_login_submit")
      ) {
        Text("Log In & Continue")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("onboarding_dialog_login_cancel")
      ) {
        Text("Cancel")
      }
    }
  )
}

// -------------------------------------------------------------
// STEP 1: WELCOME
// -------------------------------------------------------------
@Composable
private fun WelcomeStepView() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(100.dp)
        .clip(CircleShape)
        .background(SurfaceContainer)
        .border(2.dp, PrimaryFixedDim, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = "Safe Companion Logo",
        tint = Primary,
        modifier = Modifier.size(54.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Safe Companion",
      style = MaterialTheme.typography.displayLarge.copy(
        color = OnSurface,
        textAlign = TextAlign.Center
      )
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "“Getting there safely, not just getting there.”",
      style = MaterialTheme.typography.headlineMedium.copy(
        color = Primary,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
      )
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Your intelligent, personal safety companion designed to provide seamless journey monitoring, instant emergency alerts to your trusted circle, and real-time community hazard awareness.",
      style = MaterialTheme.typography.bodyLarge.copy(
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 26.sp
      )
    )

    Spacer(modifier = Modifier.height(32.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceContainerLowest,
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = Tertiary,
          modifier = Modifier.size(28.dp)
        )
        Text(
          text = "Safe-by-Design: Privacy-first tracking active only when you initiate a journey.",
          style = MaterialTheme.typography.bodyMedium.copy(color = OnSurface)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 2: WHY SAFE COMPANION
// -------------------------------------------------------------
@Composable
private fun WhySafeCompanionStepView() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
  ) {
    Text(
      text = "Why Safe Companion?",
      style = MaterialTheme.typography.headlineLarge.copy(color = OnSurface)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Designed to give you and your loved ones complete peace of mind on every commute.",
      style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
    )

    Spacer(modifier = Modifier.height(20.dp))

    WhyFeatureCard(
      icon = Icons.Default.DirectionsWalk,
      iconBg = PrimaryContainer,
      iconTint = OnPrimaryContainer,
      title = "Journey Monitoring",
      description = "Real-time route tracking that detects significant deviations or unexpected stops along your path."
    )

    Spacer(modifier = Modifier.height(12.dp))

    WhyFeatureCard(
      icon = Icons.Default.Group,
      iconBg = SecondaryContainer,
      iconTint = OnSecondaryContainer,
      title = "Trusted Contacts",
      description = "Instant sharing with your selected inner circle who receive automatic safe arrival notifications."
    )

    Spacer(modifier = Modifier.height(12.dp))

    WhyFeatureCard(
      icon = Icons.Default.Emergency,
      iconBg = ErrorContainer,
      iconTint = OnErrorContainer,
      title = "Emergency Support",
      description = "One-touch SOS triggers immediate location broadcasts and coordinates emergency response."
    )

    Spacer(modifier = Modifier.height(12.dp))

    WhyFeatureCard(
      icon = Icons.Default.WifiOff,
      iconBg = SurfaceContainerHigh,
      iconTint = Primary,
      title = "Offline Resiliency",
      description = "Prepared with alternative emergency fallback states even when cellular data is weak."
    )
  }
}

@Composable
private fun WhyFeatureCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBg: Color,
  iconTint: Color,
  title: String,
  description: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 3: PROFILE SETUP
// -------------------------------------------------------------
@Composable
private fun ProfileSetupStepView(
  userProfile: UserProfile,
  onUpdateProfile: (String, String, String) -> Unit,
  onSaveProfileDetails: (String, String, String) -> Unit = { _, _, _ -> },
  onOpenLogin: () -> Unit = {}
) {
  var name by remember { mutableStateOf(userProfile.name) }
  var phone by remember { mutableStateOf(userProfile.phone) }
  var email by remember { mutableStateOf(userProfile.email) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
  ) {
    Text(
      text = "Profile Setup",
      style = MaterialTheme.typography.headlineLarge.copy(color = OnSurface)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Please verify your personal details so emergency contacts recognize your alerts.",
      style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Name Input
    Text(
      text = "FULL NAME",
      style = MaterialTheme.typography.labelMedium.copy(
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Bold
      )
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = name,
      onValueChange = {
        name = it
        onUpdateProfile(it, phone, email)
      },
      placeholder = {
        Text(
          text = "Enter your full name",
          style = MaterialTheme.typography.bodyLarge.copy(color = OnSurfaceVariant.copy(alpha = 0.5f))
        )
      },
      leadingIcon = {
        Icon(Icons.Default.Person, contentDescription = null, tint = Primary)
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_profile_name"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceContainerLowest,
        unfocusedContainerColor = SurfaceContainerLowest,
        focusedBorderColor = Primary,
        unfocusedBorderColor = OutlineVariant
      )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Phone Input
    Text(
      text = "PHONE NUMBER",
      style = MaterialTheme.typography.labelMedium.copy(
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Bold
      )
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = phone,
      onValueChange = {
        phone = it
        onUpdateProfile(name, it, email)
      },
      placeholder = {
        Text(
          text = "Enter your phone number",
          style = MaterialTheme.typography.bodyLarge.copy(color = OnSurfaceVariant.copy(alpha = 0.5f))
        )
      },
      leadingIcon = {
        Icon(Icons.Default.Phone, contentDescription = null, tint = Primary)
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_profile_phone"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceContainerLowest,
        unfocusedContainerColor = SurfaceContainerLowest,
        focusedBorderColor = Primary,
        unfocusedBorderColor = OutlineVariant
      )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Email Input
    Text(
      text = "EMAIL (OPTIONAL)",
      style = MaterialTheme.typography.labelMedium.copy(
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Bold
      )
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = email,
      onValueChange = {
        email = it
        onUpdateProfile(name, phone, it)
      },
      placeholder = {
        Text(
          text = "Enter your email address",
          style = MaterialTheme.typography.bodyLarge.copy(color = OnSurfaceVariant.copy(alpha = 0.5f))
        )
      },
      leadingIcon = {
        Icon(Icons.Default.Email, contentDescription = null, tint = Primary)
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_profile_email"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceContainerLowest,
        unfocusedContainerColor = SurfaceContainerLowest,
        focusedBorderColor = Primary,
        unfocusedBorderColor = OutlineVariant
      )
    )

    Spacer(modifier = Modifier.height(24.dp))

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = SurfaceContainerLow,
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Tertiary, modifier = Modifier.size(20.dp))
        Text(
          text = "Your contact info is encrypted and shared only during activated journeys.",
          style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Direct "Save Details & Finish Setup" button
    Button(
      onClick = {
        onUpdateProfile(name, phone, email)
        onSaveProfileDetails(name, phone, email)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("btn_save_profile_details"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Tertiary,
        contentColor = OnTertiary
      )
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Save Details & Go to Dashboard",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
      onClick = onOpenLogin,
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag("profile_step_login_button"),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
    ) {
      Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Already have an account? Log In",
        style = MaterialTheme.typography.labelMedium.copy(color = Primary, fontWeight = FontWeight.SemiBold)
      )
    }
  }
}

// -------------------------------------------------------------
// STEP 4: PERMISSIONS
// -------------------------------------------------------------
private data class PermissionDialogInfo(
  val typeKey: String,
  val title: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val badgeText: String,
  val iconTint: Color,
  val iconBg: Color,
  val headline: String,
  val reason: String,
  val features: List<String>
)

@Composable
private fun PermissionsStepView(
  permissions: AppPermissions,
  onTogglePermission: (String) -> Unit,
  onRequestPermission: (String) -> Unit = {}
) {
  var activePermissionDialog by remember { mutableStateOf<PermissionDialogInfo?>(null) }

  val locationInfo = remember {
    PermissionDialogInfo(
      typeKey = "location",
      title = "Location Permission",
      icon = Icons.Default.LocationOn,
      badgeText = "Required",
      iconTint = Primary,
      iconBg = PrimaryContainer,
      headline = "Enable Real-Time Journey Tracking",
      reason = "Safe Companion requires access to your device's location to monitor your active journey, guide you along well-lit paths, and detect abnormal stationary delays or route deviations.",
      features = listOf(
        "Accurate live route tracking and progress updates",
        "Instant alerts if you deviate from your safe path",
        "Live GPS coordinates dispatched in emergency SOS"
      )
    )
  }

  val notificationsInfo = remember {
    PermissionDialogInfo(
      typeKey = "notifications",
      title = "Notifications Permission",
      icon = Icons.Default.Notifications,
      badgeText = "Recommended",
      iconTint = Secondary,
      iconBg = SecondaryContainer,
      headline = "Stay Informed with Timely Alerts",
      reason = "Allow notifications so Safe Companion can immediately send you route safety updates, overdue journey warnings, and responses from your trusted contacts.",
      features = listOf(
        "Safety check-in reminders during your commute",
        "Instant alerts when trusted contacts acknowledge your SOS",
        "Community hazard warnings reported nearby"
      )
    )
  }

  val backgroundInfo = remember {
    PermissionDialogInfo(
      typeKey = "background",
      title = "Background Location",
      icon = Icons.Default.Map,
      badgeText = "Full Protection",
      iconTint = Primary,
      iconBg = SurfaceContainerHigh,
      headline = "Protect When Phone is in Pocket",
      reason = "Background location enables Safe Companion to continue protecting you seamlessly even when your screen is locked or you are using other apps.",
      features = listOf(
        "Continuous safety tracking when screen is turned off",
        "Battery-efficient background GPS tracking",
        "Guaranteed location accuracy during sudden emergencies"
      )
    )
  }

  val smsInfo = remember {
    PermissionDialogInfo(
      typeKey = "sms",
      title = "Emergency SMS Fallback",
      icon = Icons.Default.Sms,
      badgeText = "Offline Safety",
      iconTint = Primary,
      iconBg = SurfaceContainerHigh,
      headline = "Direct Emergency Communication",
      reason = "When cellular data or internet connection is unavailable, Safe Companion uses direct SMS to send your exact GPS distress coordinates to your trusted circle.",
      features = listOf(
        "Automated SOS dispatch to trusted contacts via SMS",
        "Works even without active mobile data or Wi-Fi",
        "Used strictly and exclusively for emergency SOS triggers"
      )
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    Text(
      text = "Stay safe and connected",
      style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 28.sp,
        color = OnSurface,
        textAlign = TextAlign.Center
      ),
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Safe Companion needs a few permissions to keep you protected during your journeys.",
      style = MaterialTheme.typography.bodyLarge.copy(
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center
      ),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 1. Location
    PermissionItemCard(
      icon = Icons.Default.LocationOn,
      iconBg = PrimaryContainer,
      iconTint = OnPrimaryContainer,
      title = "Location",
      tagLabel = "Required",
      tagColor = ErrorContainer,
      tagTextColor = OnErrorContainer,
      description = "Used to monitor your journey and provide location-based safety features.",
      isGranted = permissions.location,
      onActionClick = {
        if (!permissions.location) {
          activePermissionDialog = locationInfo
        } else {
          onTogglePermission("location")
        }
      },
      actionLabel = if (permissions.location) "Allowed" else "Set up",
      testTag = "permission_location"
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Notifications
    PermissionItemCard(
      icon = Icons.Default.Notifications,
      iconBg = SecondaryContainer,
      iconTint = OnSecondaryContainer,
      title = "Notifications",
      tagLabel = "Recommended",
      tagColor = SurfaceVariant,
      tagTextColor = OnSurfaceVariant,
      description = "Used for journey warnings, status updates, and emergency information.",
      isGranted = permissions.notifications,
      onActionClick = {
        if (!permissions.notifications) {
          activePermissionDialog = notificationsInfo
        } else {
          onTogglePermission("notifications")
        }
      },
      actionLabel = if (permissions.notifications) "Allowed" else "Set up",
      testTag = "permission_notifications"
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 3. Background Location
    PermissionItemCard(
      icon = Icons.Default.Map,
      iconBg = SurfaceContainerHigh,
      iconTint = Primary,
      title = "Background Location",
      tagLabel = "Required for full safety",
      tagColor = SurfaceVariant,
      tagTextColor = OnSurfaceVariant,
      description = "Allows journey monitoring to continue when the app is not actively open.",
      isGranted = permissions.backgroundLocation,
      onActionClick = {
        if (!permissions.backgroundLocation) {
          activePermissionDialog = backgroundInfo
        } else {
          onTogglePermission("background")
        }
      },
      actionLabel = if (permissions.backgroundLocation) "Configured" else "Set up",
      testTag = "permission_background"
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 4. SMS / Emergency
    PermissionItemCard(
      icon = Icons.Default.Sms,
      iconBg = SurfaceContainerHigh,
      iconTint = Primary,
      title = "SMS / Emergency",
      tagLabel = "Optional",
      tagColor = SurfaceContainerHighest,
      tagTextColor = OnSurfaceVariant,
      description = "Used as a fallback emergency communication method when internet connectivity is unavailable.",
      isGranted = permissions.smsFallback,
      onActionClick = {
        if (!permissions.smsFallback) {
          activePermissionDialog = smsInfo
        } else {
          onTogglePermission("sms")
        }
      },
      actionLabel = if (permissions.smsFallback) "Configured" else "Set up",
      testTag = "permission_sms"
    )
  }

  // Permission Setup Dialog
  activePermissionDialog?.let { dialogInfo ->
    PermissionSetupDialog(
      info = dialogInfo,
      onDismiss = { activePermissionDialog = null },
      onConfirm = {
        val type = dialogInfo.typeKey
        activePermissionDialog = null
        onRequestPermission(type)
      }
    )
  }
}

@Composable
private fun PermissionSetupDialog(
  info: PermissionDialogInfo,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(info.iconBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = info.icon,
          contentDescription = null,
          tint = info.iconTint,
          modifier = Modifier.size(28.dp)
        )
      }
    },
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = info.iconBg,
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Text(
            text = info.badgeText,
            style = MaterialTheme.typography.labelSmall.copy(
              color = info.iconTint,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
          )
        }
        Text(
          text = info.title,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = info.reason,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = OnSurfaceVariant,
            lineHeight = 20.sp
          )
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SurfaceContainerLow,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            info.features.forEach { feature ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Tertiary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = feature,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = OnSurface,
                    fontWeight = FontWeight.Medium
                  )
                )
              }
            }
          }
        }

        Text(
          text = "Tap 'Set up Permission' to view and allow the system access prompt.",
          style = MaterialTheme.typography.labelSmall.copy(
            color = OnSurfaceVariant.copy(alpha = 0.8f),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.testTag("dialog_confirm_permission_${info.typeKey}")
      ) {
        Text("Set up Permission", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dialog_cancel_permission_${info.typeKey}")
      ) {
        Text("Not Now", color = OnSurfaceVariant)
      }
    }
  )
}

@Composable
private fun PermissionItemCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBg: Color,
  iconTint: Color,
  title: String,
  tagLabel: String,
  tagColor: Color,
  tagTextColor: Color,
  description: String,
  isGranted: Boolean,
  onActionClick: () -> Unit,
  actionLabel: String,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(iconBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurface
            )
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = tagColor
          ) {
            Text(
              text = tagLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = tagTextColor,
                fontWeight = FontWeight.SemiBold
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = onActionClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .testTag(testTag),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isGranted) TertiaryContainer else SurfaceContainerHigh,
          contentColor = if (isGranted) OnTertiaryContainer else Primary
        )
      ) {
        if (isGranted) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 5: TRUSTED CONTACTS SETUP
// -------------------------------------------------------------
@Composable
private fun TrustedContactsStepView(
  trustedContacts: List<TrustedContact>,
  onAddContact: (String, String, String, Boolean) -> Unit,
  onRemoveContact: (String) -> Unit = {}
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
  ) {
    Text(
      text = "Trusted Contacts",
      style = MaterialTheme.typography.headlineLarge.copy(color = OnSurface)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Add the people who should be notified when you start a journey or trigger an emergency.",
      style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (trustedContacts.isEmpty()) {
      // Empty state guidance banner
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(PrimaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              tint = OnPrimaryContainer,
              modifier = Modifier.size(28.dp)
            )
          }
          Text(
            text = "No Trusted Contacts Added",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = OnSurface,
              textAlign = TextAlign.Center
            )
          )
          Text(
            text = "Please add at least one person you trust (such as family, a partner, or a friend) who should receive your live route and emergency SOS alerts.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = OnSurfaceVariant,
              textAlign = TextAlign.Center,
              lineHeight = 20.sp
            )
          )
        }
      }
    } else {
      // List of added contacts
      trustedContacts.forEach { contact ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceContainerLowest,
          shadowElevation = 1.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(if (contact.isPrimary) PrimaryFixedDim else SurfaceContainerHigh),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = contact.initials,
                  style = MaterialTheme.typography.labelLarge.copy(
                    color = if (contact.isPrimary) OnPrimaryFixed else Primary,
                    fontWeight = FontWeight.Bold
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
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                      ) {
                        Icon(
                          Icons.Default.Star,
                          contentDescription = null,
                          tint = OnPrimaryContainer,
                          modifier = Modifier.size(11.dp)
                        )
                        Text(
                          text = "Primary",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = OnPrimaryContainer,
                            fontWeight = FontWeight.Bold
                          )
                        )
                      }
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
              modifier = Modifier.testTag("button_remove_contact_${contact.id}")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Remove Contact",
                tint = Error
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Add New Contact Button
    if (trustedContacts.isEmpty()) {
      Button(
        onClick = { showAddDialog = true },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("button_add_contact_dialog"),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
      ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Add Your First Contact",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    } else {
      OutlinedButton(
        onClick = { showAddDialog = true },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("button_add_contact_dialog"),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Add Another Contact",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
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
fun AddContactDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Boolean) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf("Family") }
  var phone by remember { mutableStateOf("") }
  var isPrimary by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Add Trusted Contact",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Contact Name") },
          placeholder = { Text("e.g. Sarah Johnson") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_input_contact_name")
        )
        OutlinedTextField(
          value = relationship,
          onValueChange = { relationship = it },
          label = { Text("Relationship") },
          placeholder = { Text("e.g. Mother, Partner, Friend") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          placeholder = { Text("e.g. +1 555-0199") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialog_input_contact_phone")
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { isPrimary = !isPrimary }
        ) {
          Checkbox(
            checked = isPrimary,
            onCheckedChange = { isPrimary = it },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
          )
          Text(text = "Set as Primary Emergency Contact", style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onConfirm(
              name.trim(),
              relationship.trim().ifBlank { "Contact" },
              phone.trim(),
              isPrimary
            )
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
      ) {
        Text("Add Contact")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = OnSurfaceVariant)
      }
    }
  )
}

// -------------------------------------------------------------
// STEP 6: PRIVACY & SAFETY
// -------------------------------------------------------------
@Composable
private fun PrivacySafetyStepView() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
  ) {
    Text(
      text = "Privacy & Safety",
      style = MaterialTheme.typography.headlineLarge.copy(color = OnSurface)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "You maintain full control of your location and data at all times.",
      style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
    )

    Spacer(modifier = Modifier.height(20.dp))

    PrivacyBullet(
      icon = Icons.Default.ShareLocation,
      title = "Trusted-Circle Sharing",
      body = "Your live location is only broadcast to trusted contacts you explicitly select when starting a journey."
    )

    Spacer(modifier = Modifier.height(14.dp))

    PrivacyBullet(
      icon = Icons.Default.VisibilityOff,
      title = "No Passive Background Tracking",
      body = "Safe Companion does not harvest or sell continuous location trails. Tracking begins and ends with your journey."
    )

    Spacer(modifier = Modifier.height(14.dp))

    PrivacyBullet(
      icon = Icons.Default.Security,
      title = "End-to-End Encryption",
      body = "Emergency dispatch packets and location coordinates are protected with strict transport security."
    )

    Spacer(modifier = Modifier.height(14.dp))

    PrivacyBullet(
      icon = Icons.Default.CheckCircle,
      title = "What Information is Shared",
      body = "During active SOS: live GPS coordinate, battery level, route start/destination, and network status."
    )
  }
}

@Composable
private fun PrivacyBullet(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  body: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceContainerLowest,
    shadowElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(SurfaceContainerHigh),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
      }
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceVariant)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 7: SETUP COMPLETE
// -------------------------------------------------------------
@Composable
private fun SetupCompleteStepView(userName: String) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(90.dp)
        .clip(CircleShape)
        .background(TertiaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Success",
        tint = OnTertiaryContainer,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "You're all set!",
      style = MaterialTheme.typography.displayLarge.copy(
        color = OnSurface,
        textAlign = TextAlign.Center
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    val welcomeMsg = if (userName.isNotBlank()) {
      "Welcome to Safe Companion, $userName. Your safety network is active and ready to accompany you wherever you go."
    } else {
      "Welcome to Safe Companion! Your safety network is active and ready to accompany you wherever you go."
    }

    Text(
      text = welcomeMsg,
      style = MaterialTheme.typography.bodyLarge.copy(
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 26.sp
      )
    )

    Spacer(modifier = Modifier.height(28.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceContainerLowest,
      shadowElevation = 1.dp,
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "YOUR ACTIVE SETUP",
          style = MaterialTheme.typography.labelMedium.copy(
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Bold
          )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
          Text("GPS & Anomaly Monitoring Ready", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
          Text("Emergency Contacts Linked", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
          Text("Community Hazard Radar Connected", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
          Text("Pre-Recorded Emergency Voice Note Armed", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// STEP 6: PRE-RECORDED EMERGENCY VOICE NOTE
// -------------------------------------------------------------
@Composable
private fun PreRecordedVoiceNoteStepView(
  userProfile: UserProfile,
  isRecording: Boolean,
  isPlaying: Boolean,
  onStartRecording: () -> Unit,
  onStopRecording: (String?) -> Unit,
  onPlayVoice: () -> Unit,
  onStopPlayVoice: () -> Unit,
  onUpdateTranscription: (String) -> Unit,
  onClearVoice: () -> Unit
) {
  var transcriptionText by remember {
    mutableStateOf(
      userProfile.preRecordedVoiceTranscription.ifBlank {
        "EMERGENCY SOS: I have had an accident and cannot speak. Please dispatch emergency help immediately to my current GPS coordinates!"
      }
    )
  }

  val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "rec_scale"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header Badge
    Surface(
      shape = RoundedCornerShape(999.dp),
      color = PrimaryContainer.copy(alpha = 0.5f),
      border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = null,
          tint = Primary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "EMERGENCY SAFETY BACKUP",
          style = MaterialTheme.typography.labelSmall.copy(
            color = Primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Emergency Voice Note",
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Record your voice now in advance. If an accident or crisis happens and you are unable to speak or tap the screen, Safe Companion will automatically send this voice message and its text transcription with your SOS!",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
      )
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Accident safety reassurance banner
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = SurfaceContainerLowest,
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(SecondaryContainer.copy(alpha = 0.4f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Automatic Fallback Protection",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "• In an accident: Sent automatically if you cannot interact.\n• Fresh recording: If you record a new note during an incident, it will take priority instead.",
            style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, lineHeight = 18.sp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Interactive Voice Recording Card
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = SurfaceContainerLowest,
      shadowElevation = 2.dp,
      border = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = if (isRecording) Error else if (userProfile.hasPreRecordedVoiceNote) Tertiary.copy(alpha = 0.5f) else OutlineVariant.copy(alpha = 0.3f)
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (isRecording) {
          // Recording in Progress State
          Box(
            modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(Error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size((72 * pulseScale).dp)
                .clip(CircleShape)
                .background(Error.copy(alpha = 0.3f))
            )
            IconButton(
              onClick = { onStopRecording(transcriptionText) },
              modifier = Modifier
                .size(60.dp)
                .background(Error, CircleShape)
                .testTag("button_stop_pre_recording")
            ) {
              Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop Recording",
                tint = OnError,
                modifier = Modifier.size(32.dp)
              )
            }
          }

          Text(
            text = "Recording in progress...",
            style = MaterialTheme.typography.titleMedium.copy(color = Error, fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Speak clearly: 'This is my emergency voice alert. Please send help to my location.'",
            style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, textAlign = TextAlign.Center)
          )

          Button(
            onClick = { onStopRecording(transcriptionText) },
            colors = ButtonDefaults.buttonColors(containerColor = Error),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("button_save_recording_done")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Done Recording", style = MaterialTheme.typography.labelLarge)
          }
        } else if (userProfile.hasPreRecordedVoiceNote || !userProfile.preRecordedVoicePath.isNullOrBlank()) {
          // Saved State
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = TertiaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Tertiary, modifier = Modifier.size(24.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Emergency Voice Note Saved",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Tertiary)
                )
                Text(
                  text = "Duration: ${userProfile.preRecordedVoiceDurationSeconds}s • Ready for auto-dispatch",
                  style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant)
                )
              }
            }
          }

          // Playback & Management Controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                if (isPlaying) onStopPlayVoice() else onPlayVoice()
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("button_playback_pre_recorded_voice"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Error else Primary
              )
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isPlaying) "Stop Audio" else "Listen (${userProfile.preRecordedVoiceDurationSeconds}s)",
                style = MaterialTheme.typography.labelMedium
              )
            }

            OutlinedButton(
              onClick = onStartRecording,
              modifier = Modifier
                .height(48.dp)
                .testTag("button_rerecord_pre_voice"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Re-record", style = MaterialTheme.typography.labelMedium)
            }

            IconButton(
              onClick = onClearVoice,
              modifier = Modifier
                .size(48.dp)
                .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .testTag("button_delete_pre_voice")
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Voice Note", tint = Error)
            }
          }
        } else {
          // Idle / Not Recorded Yet State
          IconButton(
            onClick = onStartRecording,
            modifier = Modifier
              .size(76.dp)
              .background(Primary.copy(alpha = 0.1f), CircleShape)
              .border(2.dp, Primary, CircleShape)
              .testTag("button_start_pre_recording")
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Start Voice Note Recording",
              tint = Primary,
              modifier = Modifier.size(36.dp)
            )
          }

          Text(
            text = "Tap microphone to record",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Say your name, medical conditions, or a general emergency statement.",
            style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, textAlign = TextAlign.Center)
          )

          Button(
            onClick = onStartRecording,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("button_record_voice_note_main")
          ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Record Voice Note Now", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Editable Text Message (Emergency SMS fallback)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceContainerLowest,
      border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "EMERGENCY SMS TEXT TRANSCRIPTION",
            style = MaterialTheme.typography.labelSmall.copy(
              color = OnSurfaceVariant,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          )
          Text(
            text = "Included in SMS",
            style = MaterialTheme.typography.labelSmall.copy(color = Primary)
          )
        }

        Text(
          text = "This transcription will be included in the SOS text sent to your contacts, even if cellular data is offline:",
          style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, fontSize = 12.sp)
        )

        OutlinedTextField(
          value = transcriptionText,
          onValueChange = {
            transcriptionText = it
            onUpdateTranscription(it)
          },
          placeholder = { Text("Emergency message to include in SMS...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_emergency_transcription"),
          minLines = 3,
          maxLines = 5,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineVariant
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "You can update or re-record this safety message anytime in Settings.",
      style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceVariant, textAlign = TextAlign.Center)
    )
  }
}

