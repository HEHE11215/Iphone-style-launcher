package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.RoseAlert

/**
 * Clean Modal Bottom Sheet for all launcher settings, system optimization,
 * theme controls, blur slider, 3D parallax toggle, and default launcher setup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsModalSheet(
    preferences: UserPreferences,
    telemetry: SystemTelemetry,
    isGuestSession: Boolean,
    onDismiss: () -> Unit,
    onRunRamCleanBoost: () -> Unit,
    onCyclePowerProfile: () -> Unit,
    onBlurIntensityChanged: (Float) -> Unit,
    onSelectThemeMode: (GlassThemeMode) -> Unit,
    onSelectIconPack: (CustomIconPack) -> Unit,
    onToggleParallax: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentBlurSlider by remember(preferences.blurIntensity) {
        mutableFloatStateOf(preferences.blurIntensity.toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (tokens.isDark) Color(0xFF13141C) else Color(0xFFF4F6F8),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Launcher Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "AeroGlass • Real iPhone Glass Experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGuestSession) tokens.accentSecondary.copy(alpha = 0.2f) else tokens.accentPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGuestSession) "Guest Mode" else "Cloud Synced",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isGuestSession) tokens.accentSecondary else tokens.accentPrimary
                    )
                }
            }

            // Set As Default Launcher Button
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Default Launcher",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Set AeroGlass as primary home app",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.accentPrimary)
                            .clickable { promptSetDefaultLauncher(context) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("set_default_launcher_btn")
                    ) {
                        Text(
                            text = "Set Default",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF041326)
                        )
                    }
                }
            }

            // RAM Cleaner & Performance Booster
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val ramPct = (telemetry.ramUsageFraction * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.RAM_BOOST,
                                size = 22.dp,
                                tint = tokens.accentPrimary
                            )
                            Text(
                                text = "RAM Optimizer & 90FPS Cache",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.accentPrimary)
                                .clickable(onClick = onRunRamCleanBoost)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Clean RAM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF041326)
                            )
                        }
                    }

                    Text(
                        text = "${telemetry.usedRamMb} MB used / ${telemetry.totalRamMb} MB total ($ramPct%). Clears background cache for silky responsiveness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
            }

            // Adjustable Blur Intensity Slider
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frosted Blur Intensity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "${currentBlurSlider.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPrimary
                        )
                    }

                    Slider(
                        value = currentBlurSlider,
                        onValueChange = {
                            currentBlurSlider = it
                            onBlurIntensityChanged(it)
                        },
                        valueRange = 10f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accentPrimary,
                            activeTrackColor = tokens.accentPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("blur_intensity_slider")
                    )

                    Text(
                        text = "Adjusts real-time optical translucency & specular highlight intensity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary
                    )
                }
            }

            // Real iPhone Glass Theme Selector
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Liquid Glass Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            label = "Dark Glass",
                            selected = preferences.themeMode == GlassThemeMode.DARK_GLASS.name,
                            accentColor = Color(0xFF0A84FF),
                            onClick = { onSelectThemeMode(GlassThemeMode.DARK_GLASS) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            label = "Light Frost",
                            selected = preferences.themeMode == GlassThemeMode.LIGHT_FROST.name,
                            accentColor = Color(0xFF007AFF),
                            onClick = { onSelectThemeMode(GlassThemeMode.LIGHT_FROST) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            label = "Aurora",
                            selected = preferences.themeMode == GlassThemeMode.AURORA_DYNAMIC.name,
                            accentColor = Color(0xFF38BDF8),
                            onClick = { onSelectThemeMode(GlassThemeMode.AURORA_DYNAMIC) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3D Parallax Gyroscope Effect Toggle
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "3D Parallax Clock Motion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Uses gyroscope sensor for tilt depth & light reflection",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }

                    Switch(
                        checked = preferences.parallaxEnabled,
                        onCheckedChange = onToggleParallax,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF041326),
                            checkedTrackColor = tokens.accentPrimary,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("parallax_toggle_switch")
                    )
                }
            }

            // Power / Battery Profile
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Power & Battery Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "${telemetry.powerProfile} (${telemetry.batteryPercent}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.accentPrimary.copy(alpha = 0.2f))
                            .clickable(onClick = onCyclePowerProfile)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Cycle Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPrimary
                        )
                    }
                }
            }

            // Account & Sign Out / Guest Mode
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGuestSession) "Guest Mode Active" else "Google Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = if (isGuestSession) "Tasks & settings saved locally only" else "Synced securely via Firebase Cloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoseAlert.copy(alpha = 0.15f))
                            .clickable(onClick = onSignOut)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("sign_out_btn")
                    ) {
                        Text(
                            text = if (isGuestSession) "Sign In" else "Sign Out",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoseAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeOptionCard(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.28f)
                else Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.45f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) tokens.textPrimary else tokens.textSecondary
            )
        }
    }
}
