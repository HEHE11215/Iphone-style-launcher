package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassThemeMode
import com.example.data.model.SystemTelemetry
import com.example.data.model.UserPreferences
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.QuickSettingsState

/**
 * Right-pane System Care (RAM Clean + Battery Optimizer) & Quick Settings Control Center.
 */
@Composable
fun SystemOptimizerAndSettingsPage(
    telemetry: SystemTelemetry,
    preferences: UserPreferences,
    quickSettings: QuickSettingsState,
    onRunRamCleanBoost: () -> Unit,
    onCyclePowerProfile: () -> Unit,
    onToggleQuickSetting: (String) -> Unit,
    onBlurIntensityChanged: (Int) -> Unit,
    onSelectThemeMode: (GlassThemeMode) -> Unit,
    onSelectIconPack: (CustomIconPack) -> Unit,
    onToggleParallax: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val currentTheme = runCatching { GlassThemeMode.valueOf(preferences.themeMode) }
        .getOrDefault(GlassThemeMode.DARK_GLASS)
    val currentIconPack = runCatching { CustomIconPack.valueOf(preferences.iconPack) }
        .getOrDefault(CustomIconPack.LIQUID_PRISM)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("system_optimizer_page"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.SYSTEM_OPTIMIZER,
                        size = 28.dp,
                        tint = tokens.accentPrimary,
                        secondaryTint = tokens.accentSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Glass Control & Silicon Care",
                            style = MaterialTheme.typography.headlineMedium,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "90Hz+ Frame Pacing • RAM & Thermal Optimizer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }
                }
            }
        }

        // 1. Lightweight RAM Cleaner & 90fps+ Hardware Boost Widget
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.RAM_BOOST,
                                size = 26.dp,
                                tint = EmeraldPulse,
                                secondaryTint = tokens.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ultra-Light RAM Engine",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = "${telemetry.usedRamMb} MB used of ${telemetry.totalRamMb} MB (${telemetry.availableRamMb} MB free)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPulse.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${telemetry.displayRefreshRateHz}Hz LOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPulse
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { telemetry.ramUsageFraction },
                        color = EmeraldPulse,
                        trackColor = tokens.cardFillBottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = quickSettings.lastRamBoostSummary,
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRunRamCleanBoost,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPulse,
                            contentColor = Color(0xFF032218)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ram_clean_boost_button")
                    ) {
                        Text(
                            text = "Purge Standby Cache & Lock ${telemetry.displayRefreshRateHz}fps Smoothness",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Advanced Battery Optimization Tools
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.BATTERY_PULSE,
                                size = 26.dp,
                                tint = AmberGlow,
                                secondaryTint = tokens.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Battery & Thermal Governor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = if (telemetry.isCharging) "Charging • ${telemetry.estimatedHoursRemaining} projected"
                                    else "Discharging • ${telemetry.estimatedHoursRemaining} remaining",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                        }

                        Text(
                            text = "${telemetry.batteryPercent}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AmberGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricMiniTile(
                            label = "THERMAL",
                            value = "${telemetry.batteryTempCelsius}°C",
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "VOLTAGE",
                            value = "${telemetry.batteryVoltageMv} mV",
                            modifier = Modifier.weight(1f)
                        )
                        MetricMiniTile(
                            label = "DISPLAY",
                            value = "${telemetry.displayRefreshRateHz} Hz",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.cardFillBottom)
                            .clickable(onClick = onCyclePowerProfile)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("cycle_power_profile_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE SILICON POWER PROFILE",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textSecondary
                            )
                            Text(
                                text = telemetry.powerProfile,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.accentPrimary
                            )
                        }
                        Text(
                            text = "Switch →",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.textPrimary
                        )
                    }
                }
            }
        }

        // 3. Quick Settings Control Center Panel
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "QUICK SETTINGS CONTROL CENTER",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToggleTile(
                            glyph = AeroIconGlyph.WIFI,
                            label = "Wi-Fi 7",
                            active = quickSettings.wifiEnabled,
                            onClick = { onToggleQuickSetting("wifi") },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_wifi"
                        )
                        QuickToggleTile(
                            glyph = AeroIconGlyph.BLUETOOTH,
                            label = "Bluetooth",
                            active = quickSettings.bluetoothEnabled,
                            onClick = { onToggleQuickSetting("bt") },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_bt"
                        )
                        QuickToggleTile(
                            glyph = AeroIconGlyph.DND_FOCUS,
                            label = "Focus DND",
                            active = quickSettings.focusDndEnabled,
                            onClick = { onToggleQuickSetting("dnd") },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_dnd"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToggleTile(
                            glyph = AeroIconGlyph.FLASHLIGHT,
                            label = "Torch",
                            active = quickSettings.torchEnabled,
                            onClick = { onToggleQuickSetting("torch") },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_torch"
                        )
                        QuickToggleTile(
                            glyph = AeroIconGlyph.AUTO_BRIGHTNESS,
                            label = "Auto Lux",
                            active = quickSettings.adaptiveBrightness,
                            onClick = { onToggleQuickSetting("brightness") },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_brightness"
                        )
                        QuickToggleTile(
                            glyph = AeroIconGlyph.PARALLAX_3D,
                            label = "3D Parallax",
                            active = preferences.parallaxEnabled,
                            onClick = { onToggleParallax(!preferences.parallaxEnabled) },
                            modifier = Modifier.weight(1f),
                            testTag = "qs_tile_parallax"
                        )
                    }
                }
            }
        }

        // 4. Adjustable Glass Blur Intensity & Theme Personalization
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIQUID GLASS BLUR INTENSITY",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.accentPrimary
                        )
                        Text(
                            text = "${preferences.blurIntensity}% Frosted Refraction",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = preferences.blurIntensity.toFloat(),
                        onValueChange = { onBlurIntensityChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accentPrimary,
                            activeTrackColor = tokens.accentPrimary,
                            inactiveTrackColor = tokens.cardFillBottom
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blur_intensity_slider")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DYNAMIC GLASS THEME MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentTheme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) tokens.accentPrimary.copy(alpha = 0.20f)
                                    else tokens.cardFillBottom
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) tokens.accentPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectThemeMode(mode) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("theme_mode_${mode.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.accentPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CUSTOM VECTOR ICON PACK",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CustomIconPack.entries.forEach { pack ->
                        val isSelected = pack == currentIconPack
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) tokens.accentSecondary.copy(alpha = 0.20f)
                                    else tokens.cardFillBottom
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) tokens.accentSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectIconPack(pack) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("icon_pack_${pack.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.DASHBOARD,
                                    size = 28.dp,
                                    tint = tokens.accentPrimary,
                                    secondaryTint = tokens.accentSecondary,
                                    iconPack = pack
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = pack.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.textPrimary
                                    )
                                    Text(
                                        text = pack.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onSelectIconPack(pack) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = tokens.accentPrimary,
                                    checkedTrackColor = tokens.accentPrimary.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Account Sign Out Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_card"),
                cornerRadius = 22.dp,
                onClick = onSignOut
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cloud Synced Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Sign out of Google Workspace session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = tokens.accentPrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun MetricMiniTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.cardFillBottom)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = tokens.textPrimary
        )
    }
}

@Composable
private fun QuickToggleTile(
    glyph: AeroIconGlyph,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) tokens.accentPrimary.copy(alpha = 0.24f)
                else tokens.cardFillBottom
            )
            .border(
                width = 1.dp,
                color = if (active) tokens.accentPrimary else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomGlassIcon(
            glyph = glyph,
            size = 24.dp,
            tint = if (active) tokens.accentPrimary else tokens.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) tokens.textPrimary else tokens.textSecondary
        )
    }
}
