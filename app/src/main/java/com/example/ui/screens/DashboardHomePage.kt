package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherForecast
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.Spatial3DParallaxClockCard
import com.example.ui.components.glassPill
import com.example.ui.components.glassmorphism
import com.example.ui.theme.LocalGlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class HomeGridShortcut(
    val id: String,
    val label: String,
    val glyph: AeroIconGlyph,
    val brushColors: List<Color>,
    val installedApp: InstalledAppItem? = null,
    val onClick: () -> Unit
)

/**
 * Authentic Stock Android (Pixel / AOSP Launcher) Home Screen with Glassmorphism:
 * - Top: Stock Android "At a Glance" (Date, Live Weather, Upcoming Focus Tasks chip)
 * - Middle: 3D Parallax Glass Clock Widget
 * - Lower Grid: Stock Android 4x2 Home Screen App Shortcuts Grid (real installed apps + launcher tools)
 * - Long-press on empty space opens Stock Android Home Context Menu (Home settings, Widgets, RAM Boost)
 * - Swipe up anywhere opens the blurred All Apps Drawer overlay
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardHomePage(
    weather: WeatherForecast?,
    telemetry: SystemTelemetry,
    parallaxEnabled: Boolean,
    activeRemindersCount: Int = 0,
    pendingTasksCount: Int = 0,
    onOpenAppDrawer: () -> Unit,
    onOpenDiscoverFeed: () -> Unit,
    onOpenWidgetsPage: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onQuickRamClean: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    var isDefaultHome by remember { mutableStateOf(isAeroGlassDefaultLauncher(context)) }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var showHomeLongPressMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isDefaultHome = isAeroGlassDefaultLauncher(context)
        installedApps = fetchInstalledApps(context).take(8)
    }

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    val dateLabel = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    // Build 8 shortcuts for the 2x4 Stock Android Home Grid (mixing real installed apps + essential shortcuts)
    val homeShortcuts = remember(installedApps, tokens) {
        val list = mutableListOf<HomeGridShortcut>()
        // First add real installed apps up to 4
        installedApps.take(4).forEach { app ->
            list.add(
                HomeGridShortcut(
                    id = app.packageName,
                    label = app.label,
                    glyph = AeroIconGlyph.APPS_DRAWER,
                    brushColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                    installedApp = app,
                    onClick = { launchApp(context, app.packageName) }
                )
            )
        }
        // Add built-in stock & glass shortcuts so the 8-slot grid is always rich and functional
        list.add(
            HomeGridShortcut(
                id = "shortcut_discover",
                label = "Google News",
                glyph = AeroIconGlyph.DISCOVER_NEWS,
                brushColors = listOf(Color(0xFF4285F4), Color(0xFF1D4ED8)),
                onClick = onOpenDiscoverFeed
            )
        )
        list.add(
            HomeGridShortcut(
                id = "shortcut_tasks",
                label = "Focus Tasks",
                glyph = AeroIconGlyph.TASKS,
                brushColors = listOf(Color(0xFF0A84FF), Color(0xFF0284C7)),
                onClick = onOpenWidgetsPage
            )
        )
        list.add(
            HomeGridShortcut(
                id = "shortcut_ram_boost",
                label = "Boost RAM",
                glyph = AeroIconGlyph.RAM_BOOST,
                brushColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                onClick = onQuickRamClean
            )
        )
        list.add(
            HomeGridShortcut(
                id = "shortcut_system_settings",
                label = "System",
                glyph = AeroIconGlyph.SETTINGS,
                brushColors = listOf(Color(0xFF64748B), Color(0xFF334155)),
                onClick = {
                    runCatching {
                        val intent = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        )
        // Fill remaining slots up to 8 with additional installed apps or launcher tools
        installedApps.drop(4).forEach { app ->
            if (list.size < 8) {
                list.add(
                    HomeGridShortcut(
                        id = app.packageName,
                        label = app.label,
                        glyph = AeroIconGlyph.APPS_DRAWER,
                        brushColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                        installedApp = app,
                        onClick = { launchApp(context, app.packageName) }
                    )
                )
            }
        }
        if (list.size < 8) {
            list.add(
                HomeGridShortcut(
                    id = "shortcut_home_settings",
                    label = "Glass Style",
                    glyph = AeroIconGlyph.QUICK_SETTINGS,
                    brushColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                    onClick = onOpenSettingsSheet
                )
            )
        }
        if (list.size < 8) {
            list.add(
                HomeGridShortcut(
                    id = "shortcut_all_apps",
                    label = "All Apps",
                    glyph = AeroIconGlyph.APPS_DRAWER,
                    brushColors = listOf(Color(0xFF38BDF8), Color(0xFF0369A1)),
                    onClick = onOpenAppDrawer
                )
            )
        }
        list.take(8)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = { showHomeLongPressMenu = true }
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDragAccumulator += dragAmount
                        if (verticalDragAccumulator < -38f) {
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

        // Stock Android Long-Press Home Context Menu
        Box(modifier = Modifier.align(Alignment.Center)) {
            DropdownMenu(
                expanded = showHomeLongPressMenu,
                onDismissRequest = { showHomeLongPressMenu = false },
                modifier = Modifier.background(
                    if (tokens.isDark) Color(0xFF181924) else Color(0xFFF8FAFC)
                )
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Home settings & Glass blur",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.SETTINGS,
                            size = 20.dp,
                            tint = tokens.accentPrimary
                        )
                    },
                    onClick = {
                        showHomeLongPressMenu = false
                        onOpenSettingsSheet()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Widgets & Focus Tasks",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.TASKS,
                            size = 20.dp,
                            tint = tokens.accentSecondary
                        )
                    },
                    onClick = {
                        showHomeLongPressMenu = false
                        onOpenWidgetsPage()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Clean RAM (${telemetry.usedRamMb} MB used)",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.RAM_BOOST,
                            size = 20.dp,
                            tint = tokens.accentPrimary
                        )
                    },
                    onClick = {
                        showHomeLongPressMenu = false
                        onQuickRamClean()
                    }
                )
                if (!isDefaultHome) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Set as Default Launcher",
                                color = tokens.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.HOME_LAUNCHER,
                                size = 20.dp,
                                tint = tokens.accentPrimary
                            )
                        },
                        onClick = {
                            showHomeLongPressMenu = false
                            promptSetDefaultLauncher(context)
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter)
                .padding(
                    horizontal = if (isNarrow) 14.dp else 20.dp,
                    vertical = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Stock Android "At a Glance" + 3D Parallax Glass Clock
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stock Android "At a Glance" Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenDiscoverFeed)
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.WEATHER_SUN_CLOUD,
                                size = 16.dp,
                                tint = tokens.accentPrimary
                            )
                            Text(
                                text = if (weather != null) {
                                    "${weather.temperatureCelsius}°C • ${weather.conditionText}"
                                } else {
                                    "22°C • Clear Sky"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = tokens.textSecondary
                            )
                        }
                    }

                    // Right-side Quick Pills: Tasks count & Default Home prompt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .glassPill(selected = pendingTasksCount > 0)
                                .clickable(onClick = onOpenWidgetsPage)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("at_a_glance_tasks_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.TASKS,
                                    size = 14.dp,
                                    tint = tokens.accentPrimary
                                )
                                Text(
                                    text = if (pendingTasksCount > 0) "$pendingTasksCount Tasks" else "Tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.textPrimary
                                )
                            }
                        }

                        if (!isDefaultHome) {
                            Box(
                                modifier = Modifier
                                    .glassPill(selected = true)
                                    .clickable {
                                        promptSetDefaultLauncher(context)
                                        isDefaultHome = isAeroGlassDefaultLauncher(context)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Set Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.accentPrimary
                                )
                            }
                        }
                    }
                }

                // 3D Parallax Glass Clock Card
                Spatial3DParallaxClockCard(
                    parallaxEnabled = parallaxEnabled,
                    refreshRateHz = telemetry.displayRefreshRateHz,
                    activeRemindersCount = activeRemindersCount,
                    weatherTempCelsius = weather?.temperatureCelsius,
                    weatherCondition = weather?.conditionText,
                    onToggleParallax = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // LOWER WORKSPACE SECTION: Stock Android 2x4 Home Screen App Grid (Right above the Dock)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val rows = homeShortcuts.chunked(4)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { item ->
                            StockHomeAppIconItem(
                                shortcut = item,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StockHomeAppIconItem(
    shortcut: HomeGridShortcut,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    var showAppMenu by remember { mutableStateOf(false) }

    val appBitmap = remember(shortcut.installedApp) {
        shortcut.installedApp?.icon?.toBitmapOrNull(144, 144)?.asImageBitmap()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .defaultMinSize(minWidth = 64.dp, minHeight = 76.dp)
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = shortcut.onClick,
                    onLongClick = { showAppMenu = true }
                )
                .padding(vertical = 4.dp, horizontal = 4.dp)
                .testTag("home_shortcut_${shortcut.id}")
        ) {
            if (appBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .glassmorphism(
                            cornerRadius = 18.dp,
                            elevated = true,
                            backgroundAlphaBoost = 0.85f
                        )
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = appBitmap,
                        contentDescription = shortcut.label,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(shortcut.brushColors))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CustomGlassIcon(
                        glyph = shortcut.glyph,
                        size = 25.dp,
                        tint = Color.White,
                        secondaryTint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = showAppMenu,
            onDismissRequest = { showAppMenu = false },
            modifier = Modifier.background(
                if (tokens.isDark) Color(0xFF181924) else Color(0xFFF8FAFC)
            )
        ) {
            DropdownMenuItem(
                text = { Text("Open ${shortcut.label}", color = tokens.textPrimary) },
                onClick = {
                    showAppMenu = false
                    shortcut.onClick()
                }
            )
            if (shortcut.installedApp != null) {
                DropdownMenuItem(
                    text = { Text("App info", color = tokens.textPrimary) },
                    onClick = {
                        showAppMenu = false
                        openAppInfo(context, shortcut.installedApp.packageName)
                    }
                )
            }
        }
    }
}
