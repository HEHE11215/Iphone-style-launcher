package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ultra-lightweight iOS 18 / visionOS Liquid Glass container.
 * Uses hardware-accelerated multi-stop linear & specular rim brushes without expensive
 * multi-pass RenderEffect offscreen buffers so it runs at 90fps-120fps on every screen.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseMod = modifier.glassmorphism(
        cornerRadius = cornerRadius,
        elevated = elevated
    )

    val clickableMod = if (onClick != null) {
        baseMod.clickable(onClick = onClick)
    } else {
        baseMod
    }

    Box(
        modifier = clickableMod,
        content = content
    )
}

/**
 * Real-time 3D Parallax Sensor + Touch Tilt State for the iPhone-style 3D Depth Clock & Hero Cards.
 */
@Composable
fun rememberParallaxTilt(enabled: Boolean): Pair<Float, Float> {
    val context = LocalContext.current
    var rawPitch by remember { mutableFloatStateOf(0f) }
    var rawRoll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(enabled) {
        if (!enabled) {
            rawPitch = 0f
            rawRoll = 0f
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    // Normalize accelerometer gravity (-9.8..9.8) to subtle -1f..1f tilt
                    rawRoll = (values[0] / 7.5f).coerceIn(-1f, 1f)
                    rawPitch = ((values[1] - 5.0f) / 7.5f).coerceIn(-1f, 1f)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accelSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val smoothPitch by animateFloatAsState(
        targetValue = if (enabled) rawPitch else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "parallaxPitch"
    )
    val smoothRoll by animateFloatAsState(
        targetValue = if (enabled) rawRoll else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "parallaxRoll"
    )

    return smoothPitch to smoothRoll
}

/**
 * iPhone 18 / StandBy-inspired 3D Spatial Parallax Clock Widget with interactive touch-tilt,
 * real hardware accelerometer depth layers, dynamic specular glass glare, and adaptive screen scaling.
 */
@Composable
fun Spatial3DParallaxClockCard(
    parallaxEnabled: Boolean,
    refreshRateHz: Int,
    activeRemindersCount: Int,
    modifier: Modifier = Modifier,
    weatherTempCelsius: Int? = null,
    weatherCondition: String? = null,
    onToggleParallax: (() -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    val (sensorPitch, sensorRoll) = rememberParallaxTilt(parallaxEnabled)

    var touchTiltX by remember { mutableFloatStateOf(0f) }
    var touchTiltY by remember { mutableFloatStateOf(0f) }

    val combinedTiltX by animateFloatAsState(
        targetValue = if (parallaxEnabled) (sensorRoll + touchTiltX).coerceIn(-1.3f, 1.3f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "clockTiltX"
    )
    val combinedTiltY by animateFloatAsState(
        targetValue = if (parallaxEnabled) (sensorPitch + touchTiltY).coerceIn(-1.3f, 1.3f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "clockTiltY"
    )

    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val secFormat = remember { SimpleDateFormat("ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    val nowDate = Date(currentMillis)
    val timeText = timeFormat.format(nowDate)
    val secText = secFormat.format(nowDate)
    val dateText = dateFormat.format(nowDate).uppercase(Locale.getDefault())

    val currentSecondsInt = remember(secText) { secText.toIntOrNull() ?: 0 }
    val secondsFraction = (currentSecondsInt / 60f).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 360.dp
        val isTablet = maxWidth > 560.dp

        val clockFontSize = if (isCompact) 56.sp else if (isTablet) 84.sp else 70.sp
        val secFontSize = if (isCompact) 18.sp else if (isTablet) 26.sp else 22.sp
        val cardPaddingH = if (isCompact) 16.dp else if (isTablet) 28.dp else 22.dp
        val cardPaddingV = if (isCompact) 16.dp else if (isTablet) 24.dp else 20.dp

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = combinedTiltX * 10f
                    rotationX = -combinedTiltY * 10f
                    cameraDistance = 16f * density
                }
                .pointerInput(parallaxEnabled) {
                    if (!parallaxEnabled) return@pointerInput
                    detectDragGestures(
                        onDragEnd = {
                            touchTiltX = 0f
                            touchTiltY = 0f
                        },
                        onDragCancel = {
                            touchTiltX = 0f
                            touchTiltY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchTiltX = (touchTiltX + dragAmount.x / 160f).coerceIn(-1.2f, 1.2f)
                            touchTiltY = (touchTiltY + dragAmount.y / 160f).coerceIn(-1.2f, 1.2f)
                        }
                    )
                },
            cornerRadius = 32.dp,
            elevated = true
        ) {
            // Layer 0: Multi-depth ambient lighting + sweeping orbital seconds ring
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .offset {
                        IntOffset(
                            x = (-combinedTiltX * 24f).roundToInt(),
                            y = (-combinedTiltY * 20f).roundToInt()
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Ambient Radial Aura Orbs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tokens.accentPrimary.copy(alpha = 0.32f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.28f + combinedTiltX * 0.1f), h * 0.45f),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(w * (0.28f + combinedTiltX * 0.1f), h * 0.45f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tokens.accentSecondary.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(w * (0.75f - combinedTiltX * 0.1f), h * 0.55f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * (0.75f - combinedTiltX * 0.1f), h * 0.55f)
                )

                // 60-second Spatial Orbital Progress Arc behind the clock core
                val arcCenter = Offset(w * 0.5f, h * 0.54f)
                val arcRadius = w * 0.42f
                val sweepAngle = secondsFraction * 360f

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            tokens.accentPrimary.copy(alpha = 0.10f),
                            tokens.accentPrimary.copy(alpha = 0.45f),
                            tokens.accentSecondary.copy(alpha = 0.70f)
                        ),
                        center = arcCenter
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius * 0.46f),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 0.92f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Specular Glass Glare Sheen (translates across surface based on 3D tilt angle)
                val sheenX = w * (0.5f + combinedTiltX * 0.4f)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = Offset(sheenX - 60f, 0f),
                        end = Offset(sheenX + 60f, h)
                    ),
                    start = Offset(sheenX - 60f, 0f),
                    end = Offset(sheenX + 60f, h),
                    strokeWidth = 40f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPaddingH, vertical = cardPaddingV),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Pill Row (Adaptive and interactive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.accentPrimary.copy(alpha = 0.18f))
                            .clickable(enabled = onToggleParallax != null) { onToggleParallax?.invoke() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.PARALLAX_3D,
                            size = 14.dp,
                            tint = tokens.accentPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (parallaxEnabled) "3D PARALLAX" else "STATIC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (weatherTempCelsius != null) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(tokens.accentSecondary.copy(alpha = 0.18f))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "$weatherTempCelsius°C ${weatherCondition.orEmpty()}".trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tokens.textPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.cardFillTop)
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${refreshRateHz}Hz FLUID",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Extruded Time Display with Chromatic Prism Dispersion Layers
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Layer 1: Extruded Violet Deep Shadow Layer
                    Text(
                        text = timeText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = clockFontSize,
                        letterSpacing = (-2).sp,
                        color = tokens.accentSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (combinedTiltX * 18f + 4f).roundToInt(),
                                y = (combinedTiltY * 16f + 6f).roundToInt()
                            )
                        }
                    )

                    // Layer 2: Mid Cyan Prismatic Specular Dispersion
                    Text(
                        text = timeText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = clockFontSize,
                        letterSpacing = (-2).sp,
                        color = tokens.accentPrimary.copy(alpha = 0.50f),
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (-combinedTiltX * 8f - 2f).roundToInt(),
                                y = (-combinedTiltY * 8f - 2f).roundToInt()
                            )
                        }
                    )

                    // Layer 3: Crisp Foreground Glass Numerals + Floating Seconds Badge
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (combinedTiltX * 5f).roundToInt(),
                                y = (combinedTiltY * 5f).roundToInt()
                            )
                        }
                    ) {
                        Text(
                            text = timeText,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = clockFontSize,
                            letterSpacing = (-2).sp,
                            color = tokens.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = secText,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = secFontSize,
                            color = tokens.accentPrimary,
                            modifier = Modifier.padding(bottom = if (isCompact) 8.dp else 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Metadata Chips: Live Date & Active Smart Alerts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(tokens.accentPrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.SMART_REMINDER,
                        size = 14.dp,
                        tint = tokens.accentPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$activeRemindersCount Smart Alerts",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.accentPrimary
                    )
                }
            }
        }
    }
}
