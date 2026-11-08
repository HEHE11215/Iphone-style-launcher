package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherForecast
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.components.Spatial3DParallaxClockCard
import com.example.ui.theme.LocalGlassTokens

/**
 * Minimalist, distraction-free Launcher Home Screen (Page 1):
 * - Real iPhone liquid glass aesthetic
 * - 3D Parallax Glass Clock & Live Weather widget
 * - Google Search Bar Pill (web search & voice launch)
 * - Swipe up gesture detection that smoothly triggers the slide-up App Drawer overlay
 */
@Composable
fun DashboardHomePage(
    weather: WeatherForecast?,
    telemetry: SystemTelemetry,
    parallaxEnabled: Boolean,
    activeRemindersCount: Int = 0,
    onOpenAppDrawer: () -> Unit,
    onOpenDiscoverFeed: () -> Unit,
    onOpenWidgetsPage: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    var isDefaultHome by remember { mutableStateOf(isAeroGlassDefaultLauncher(context)) }

    LaunchedEffect(Unit) {
        isDefaultHome = isAeroGlassDefaultLauncher(context)
    }

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDragAccumulator += dragAmount
                        if (verticalDragAccumulator < -40f) {
                            // Swiped UP with sufficient velocity
                            verticalDragAccumulator = 0f
                            onOpenAppDrawer()
                        }
                    },
                    onDragEnd = { verticalDragAccumulator = 0f },
                    onDragCancel = { verticalDragAccumulator = 0f }
                )
            }
            .testTag("dashboard_home_page")
    ) {
        val isNarrow = maxWidth < 380.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter)
                .padding(
                    horizontal = if (isNarrow) 14.dp else 22.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Default Home Launcher Banner (dismissible, only if not already default)
                if (!isDefaultHome) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.DASHBOARD,
                                    size = 20.dp,
                                    tint = tokens.accentPrimary
                                )
                                Text(
                                    text = "Set AeroGlass as default launcher",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.accentPrimary)
                                    .clickable {
                                        promptSetDefaultLauncher(context)
                                        isDefaultHome = isAeroGlassDefaultLauncher(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Enable",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF041326)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Core 3D Parallax Glass Clock & Live Weather Card
                Spatial3DParallaxClockCard(
                    parallaxEnabled = parallaxEnabled,
                    refreshRateHz = telemetry.displayRefreshRateHz,
                    activeRemindersCount = activeRemindersCount,
                    weatherTempCelsius = weather?.temperatureCelsius,
                    weatherCondition = weather?.conditionText,
                    onToggleParallax = null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Google Search Bar Pill with Voice & Lens
                GoogleGlassSearchBar(
                    onClick = { launchBrowser(context) },
                    onVoiceSearch = { launchBrowser(context) }
                )

                // Quick Launcher Action Chips (Discover on left, Widgets on right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Discover Feed Shortcut Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = if (tokens.isDark) 0.10f else 0.40f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(onClick = onOpenDiscoverFeed)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.DISCOVER_NEWS,
                                size = 16.dp,
                                tint = tokens.accentPrimary
                            )
                            Text(
                                text = "◀ Google News",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary
                            )
                        }
                    }

                    // Right: Widgets & Tools Shortcut Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = if (tokens.isDark) 0.10f else 0.40f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(onClick = onOpenWidgetsPage)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Smart Widgets ▶",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary
                            )
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.RAM_BOOST,
                                size = 16.dp,
                                tint = tokens.accentSecondary
                            )
                        }
                    }
                }
            }

            // Bottom Section: Swipe Up Indicator above fixed dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.45f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onOpenAppDrawer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("swipe_up_apps_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.APPS_DRAWER,
                            size = 16.dp,
                            tint = tokens.accentPrimary
                        )
                        Text(
                            text = "▲ Swipe up for apps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Google Search Bar pill in Real iPhone Glass style
 */
@Composable
private fun GoogleGlassSearchBar(
    onClick: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("google_search_bar_card"),
        cornerRadius = 28.dp,
        elevated = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Colored "G" logo simulation
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4285F4),
                                    Color(0xFFEA4335),
                                    Color(0xFFFBBC05),
                                    Color(0xFF34A853)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Search web or type URL...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textSecondary
                )
            }

            // Voice Search Microphone Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.accentPrimary.copy(alpha = 0.15f))
                    .clickable(onClick = onVoiceSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = tokens.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
