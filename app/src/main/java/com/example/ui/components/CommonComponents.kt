package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppMode
import com.example.model.BottomTab
import com.example.ui.theme.*

@Composable
fun SereneTopAppBar(
  title: String = "Hello",
  avatarUrl: String = "",
  currentMode: AppMode = AppMode.PERSONAL,
  showModeToggle: Boolean = true,
  onModeToggle: (AppMode) -> Unit = {},
  onNotificationClick: () -> Unit = {},
  unreadNotificationsCount: Int = 0,
  onAvatarClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 1.dp, shape = RoundedCornerShape(0.dp)),
    color = Color.White
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .height(58.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Avatar + Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable { onAvatarClick() }
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFFE2E8F0), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (avatarUrl.isNotBlank()) {
            AsyncImage(
              model = avatarUrl,
              contentDescription = "User Avatar",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Avatar Placeholder",
              tint = Color(0xFF2563EB),
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(
            color = Color(0xFF1E293B),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          )
        )
      }

      // Right: Mode Toggle Pill + Notification
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (showModeToggle) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Row(
              modifier = Modifier.padding(3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Personal Pill
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (currentMode == AppMode.PERSONAL) Color(0xFF2563EB) else Color.Transparent,
                shadowElevation = if (currentMode == AppMode.PERSONAL) 1.dp else 0.dp,
                modifier = Modifier
                  .clickable { onModeToggle(AppMode.PERSONAL) }
                  .testTag("toggle_personal_mode")
              ) {
                Text(
                  text = "Personal",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (currentMode == AppMode.PERSONAL) Color.White else Color(0xFF64748B),
                    fontWeight = if (currentMode == AppMode.PERSONAL) FontWeight.Bold else FontWeight.Medium
                  ),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }

              // Community Pill
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (currentMode == AppMode.COMMUNITY) Color(0xFF2563EB) else Color.Transparent,
                shadowElevation = if (currentMode == AppMode.COMMUNITY) 1.dp else 0.dp,
                modifier = Modifier
                  .clickable { onModeToggle(AppMode.COMMUNITY) }
                  .testTag("toggle_community_mode")
              ) {
                Text(
                  text = "Community",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (currentMode == AppMode.COMMUNITY) Color.White else Color(0xFF64748B),
                    fontWeight = if (currentMode == AppMode.COMMUNITY) FontWeight.Bold else FontWeight.Medium
                  ),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        Box(contentAlignment = Alignment.Center) {
          IconButton(
            onClick = onNotificationClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("notifications_button")
          ) {
            Icon(
              imageVector = if (unreadNotificationsCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = if (unreadNotificationsCount > 0) Color(0xFF2563EB) else Color(0xFF64748B)
            )
          }
          if (unreadNotificationsCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFDC2626)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SereneDetailTopBar(
  title: String,
  onBackClick: () -> Unit,
  trailingIcon: @Composable (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(1.dp),
    color = Color.White
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier
            .size(48.dp)
            .testTag("back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color(0xFF2563EB)
          )
        }
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(
            color = Color(0xFF1E293B),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          )
        )
      }

      if (trailingIcon != null) {
        trailingIcon()
      } else {
        Spacer(modifier = Modifier.size(48.dp))
      }
    }
  }
}

@Composable
fun SereneBottomNavBar(
  currentTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 8.dp),
    color = Color.White,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .height(62.dp)
        .padding(horizontal = 8.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Home
      BottomNavItem(
        label = "Home",
        selected = currentTab == BottomTab.HOME,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        onClick = { onTabSelected(BottomTab.HOME) },
        testTag = "tab_home"
      )

      // Journey
      BottomNavItem(
        label = "Journey",
        selected = currentTab == BottomTab.JOURNEY,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        onClick = { onTabSelected(BottomTab.JOURNEY) },
        testTag = "tab_journey"
      )

      // Community
      BottomNavItem(
        label = "Community",
        selected = currentTab == BottomTab.COMMUNITY,
        selectedIcon = Icons.Filled.Group,
        unselectedIcon = Icons.Outlined.Group,
        onClick = { onTabSelected(BottomTab.COMMUNITY) },
        testTag = "tab_community"
      )

      // Profile / Settings
      BottomNavItem(
        label = "Profile",
        selected = currentTab == BottomTab.PROFILE,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        onClick = { onTabSelected(BottomTab.PROFILE) },
        testTag = "tab_profile"
      )
    }
  }
}

@Composable
private fun BottomNavItem(
  label: String,
  selected: Boolean,
  selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable { onClick() }
      .testTag(testTag)
      .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 4.dp)
      .background(
        color = if (selected) Color(0xFFEFF6FF) else Color.Transparent,
        shape = RoundedCornerShape(999.dp)
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = if (selected) selectedIcon else unselectedIcon,
        contentDescription = label,
        tint = if (selected) Color(0xFF2563EB) else Color(0xFF64748B),
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          color = if (selected) Color(0xFF2563EB) else Color(0xFF64748B)
        )
      )
    }
  }
}

/**
 * High-fidelity Interactive Canvas Map simulating real urban layout with
 * streets, parks, water, route polyline, pulsing GPS marker, destination pin and hazards.
 * Fully responsive to pan and zoom gestures.
 */
@Composable
fun SereneMapCanvas(
  modifier: Modifier = Modifier,
  showRoute: Boolean = true,
  showControls: Boolean = true,
  onRecenter: () -> Unit = {},
  hazardLocations: List<Pair<String, Pair<Float, Float>>> = emptyList(),
  userProgress: Float = 0.5f
) {
  var zoomScale by remember { mutableStateOf(1.0f) }
  var offsetPoint by remember { mutableStateOf(Offset(0f, 0f)) }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 12f,
    targetValue = 36f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_radius"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_alpha"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SurfaceLowContainer)
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          zoomScale = (zoomScale * zoom).coerceIn(0.7f, 3.5f)
          offsetPoint = Offset(offsetPoint.x + pan.x, offsetPoint.y + pan.y)
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // Background subtle city tint
      drawRect(color = Color(0xFFF3F6FD), size = size)

      // Green parks & open spaces
      drawRoundRect(
        color = Color(0xFFE2F3E7),
        topLeft = Offset(canvasWidth * 0.05f + offsetPoint.x, canvasHeight * 0.12f + offsetPoint.y),
        size = Size(canvasWidth * 0.35f * zoomScale, canvasHeight * 0.18f * zoomScale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
      )
      drawRoundRect(
        color = Color(0xFFE2F3E7),
        topLeft = Offset(canvasWidth * 0.65f + offsetPoint.x, canvasHeight * 0.68f + offsetPoint.y),
        size = Size(canvasWidth * 0.3f * zoomScale, canvasHeight * 0.22f * zoomScale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
      )

      // Water body (Lake / Bay on side)
      val waterPath = Path().apply {
        moveTo(0f, canvasHeight * 0.75f + offsetPoint.y)
        cubicTo(
          canvasWidth * 0.2f, canvasHeight * 0.7f + offsetPoint.y,
          canvasWidth * 0.25f, canvasHeight * 0.95f + offsetPoint.y,
          canvasWidth * 0.45f, canvasHeight + offsetPoint.y
        )
        lineTo(0f, canvasHeight + offsetPoint.y)
        close()
      }
      drawPath(path = waterPath, color = Color(0xFFDCEAFB))

      // Street Grid lines (Major Avenues & Secondary Streets)
      val streetColor = Color(0xFFFFFFFF)
      val streetBorderColor = Color(0xFFD6DFEC)

      // Grid Roads - Horizontal
      for (i in 1..8) {
        val y = (canvasHeight * (i * 0.12f)) * zoomScale + offsetPoint.y
        drawLine(
          color = streetBorderColor,
          start = Offset(0f, y - 6f),
          end = Offset(canvasWidth, y - 6f),
          strokeWidth = 14f
        )
        drawLine(
          color = streetColor,
          start = Offset(0f, y - 6f),
          end = Offset(canvasWidth, y - 6f),
          strokeWidth = 10f
        )
      }

      // Grid Roads - Vertical
      for (i in 1..6) {
        val x = (canvasWidth * (i * 0.18f)) * zoomScale + offsetPoint.x
        drawLine(
          color = streetBorderColor,
          start = Offset(x, 0f),
          end = Offset(x, canvasHeight),
          strokeWidth = 14f
        )
        drawLine(
          color = streetColor,
          start = Offset(x, 0f),
          end = Offset(x, canvasHeight),
          strokeWidth = 10f
        )
      }

      // Diagonal Arterial Boulevard
      drawLine(
        color = Color(0xFFCBD6E8),
        start = Offset(0f, canvasHeight * 0.85f + offsetPoint.y),
        end = Offset(canvasWidth * 1.1f, canvasHeight * 0.15f + offsetPoint.y),
        strokeWidth = 22f
      )
      drawLine(
        color = Color.White,
        start = Offset(0f, canvasHeight * 0.85f + offsetPoint.y),
        end = Offset(canvasWidth * 1.1f, canvasHeight * 0.15f + offsetPoint.y),
        strokeWidth = 16f
      )

      if (showRoute) {
        // Safe Route Path
        val startX = canvasWidth * 0.36f + offsetPoint.x
        val startY = canvasHeight * 0.65f + offsetPoint.y
        val midX = canvasWidth * 0.48f + offsetPoint.x
        val midY = canvasHeight * 0.45f + offsetPoint.y
        val endX = canvasWidth * 0.72f + offsetPoint.x
        val endY = canvasHeight * 0.28f + offsetPoint.y

        val routePath = Path().apply {
          moveTo(startX, startY)
          lineTo(startX, midY)
          lineTo(midX, midY)
          lineTo(endX, midY)
          lineTo(endX, endY)
        }

        // Route glow outer
        drawPath(
          path = routePath,
          color = Primary.copy(alpha = 0.25f),
          style = Stroke(width = 16f)
        )
        // Route main solid line
        drawPath(
          path = routePath,
          color = PrimaryContainer,
          style = Stroke(
            width = 7f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
          )
        )

        // Start marker (Origin circle)
        drawCircle(
          color = Color.White,
          radius = 10f,
          center = Offset(startX, startY)
        )
        drawCircle(
          color = Secondary,
          radius = 6f,
          center = Offset(startX, startY)
        )

        // Destination Marker (Pin)
        drawCircle(
          color = Primary,
          radius = 16f,
          center = Offset(endX, endY - 14f)
        )
        drawCircle(
          color = Color.White,
          radius = 7f,
          center = Offset(endX, endY - 14f)
        )

        // User Current GPS Location (pulsing dot along route)
        val currentGpsX = startX + (endX - startX) * userProgress
        val currentGpsY = startY + (endY - startY) * userProgress

        // Pulse wave
        drawCircle(
          color = Primary.copy(alpha = pulseAlpha),
          radius = pulseRadius,
          center = Offset(currentGpsX, currentGpsY)
        )
        // Solid GPS dot
        drawCircle(
          color = Color.White,
          radius = 12f,
          center = Offset(currentGpsX, currentGpsY)
        )
        drawCircle(
          color = Primary,
          radius = 8f,
          center = Offset(currentGpsX, currentGpsY)
        )
      }

      // Draw hazards if any
      hazardLocations.forEach { (_, pos) ->
        val hX = canvasWidth * pos.first + offsetPoint.x
        val hY = canvasHeight * pos.second + offsetPoint.y
        drawCircle(
          color = ErrorContainer,
          radius = 14f,
          center = Offset(hX, hY)
        )
        drawCircle(
          color = Error,
          radius = 7f,
          center = Offset(hX, hY)
        )
      }
    }

    // Map Controls on Right Side
    if (showControls) {
      Column(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
          Column {
            IconButton(
              onClick = { zoomScale = (zoomScale + 0.3f).coerceAtMost(3.5f) },
              modifier = Modifier.size(44.dp)
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = OnSurface)
            }
            Divider(color = OutlineVariant.copy(alpha = 0.3f), thickness = 1.dp)
            IconButton(
              onClick = { zoomScale = (zoomScale - 0.3f).coerceAtLeast(0.7f) },
              modifier = Modifier.size(44.dp)
            ) {
              Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = OnSurface)
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
          modifier = Modifier.clickable {
            zoomScale = 1.0f
            offsetPoint = Offset.Zero
            onRecenter()
          }
        ) {
          Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "Recenter",
              tint = Primary,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}

val SurfaceLowContainer = Color(0xFFF0F3FF)
