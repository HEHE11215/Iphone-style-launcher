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
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(cornerRadius)

    val fillBrush = remember(tokens, elevated) {
        val boost = if (elevated) 1.25f else 1.0f
        Brush.linearGradient(
            colors = listOf(
                tokens.cardFillTop.copy(alpha = (tokens.cardFillTop.alpha * boost).coerceIn(0f, 0.92f)),
                tokens.cardFillBottom.copy(alpha = (tokens.cardFillBottom.alpha * boost).coerceIn(0f, 0.85f))
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 700f)
        )
    }

    val borderBrush = remember(tokens, elevated) {
        Brush.linearGradient(
            colors = listOf(
                tokens.borderHighlight,
                tokens.borderShadow,
                tokens.borderHighlight.copy(alpha = tokens.borderHighlight.alpha * 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 600f)
        )
    }

    val baseMod = modifier
        .clip(shape)
        .background(brush = fillBrush, shape = shape)
        .drawBehind {
            // Top specular glass highlight line (iOS 18 refractive edge)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.borderHighlight.copy(alpha = 0.25f * tokens.blurIntensity),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.38f
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )
        }
        .border(
            width = if (elevated) 1.4.dp else 1.dp,
            brush = borderBrush,
            shape = shape
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
 * iPhone 18 / StandBy-inspired 3D Spatial Parallax Clock Widget with interactive touch-tilt
 * and real hardware accelerometer depth layers.
 */
@Composable
fun Spatial3DParallaxClockCard(
    parallaxEnabled: Boolean,
    refreshRateHz: Int,
    activeRemindersCount: Int,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val (sensorPitch, sensorRoll) = rememberParallaxTilt(parallaxEnabled)

    var touchTiltX by remember { mutableFloatStateOf(0f) }
    var touchTiltY by remember { mutableFloatStateOf(0f) }

    val combinedTiltX by animateFloatAsState(
        targetValue = if (parallaxEnabled) (sensorRoll + touchTiltX).coerceIn(-1.2f, 1.2f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "clockTiltX"
    )
    val combinedTiltY by animateFloatAsState(
        targetValue = if (parallaxEnabled) (sensorPitch + touchTiltY).coerceIn(-1.2f, 1.2f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = combinedTiltX * 9f
                rotationX = -combinedTiltY * 9f
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
                        touchTiltX = (touchTiltX + dragAmount.x / 180f).coerceIn(-1f, 1f)
                        touchTiltY = (touchTiltY + dragAmount.y / 180f).coerceIn(-1f, 1f)
                    }
                )
            },
        cornerRadius = 32.dp,
        elevated = true
    ) {
        // Deep 3D Ambient Orb Layer (shifts opposite to foreground for depth perception)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .offset {
                    IntOffset(
                        x = (-combinedTiltX * 22f).roundToInt(),
                        y = (-combinedTiltY * 18f).roundToInt()
                    )
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentPrimary.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.45f),
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.25f, size.height * 0.45f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tokens.accentSecondary.copy(alpha = 0.26f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.55f),
                    radius = size.width * 0.44f
                ),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.78f, size.height * 0.55f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.accentPrimary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.PARALLAX_3D,
                        size = 14.dp,
                        tint = tokens.accentPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (parallaxEnabled) "3D PARALLAX ACTIVE" else "STATIC GLASS",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.accentSecondary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${refreshRateHz}Hz ADAPTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3D Extruded Time Display with Parallax Shadow Layer + Specular Foreground Layer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Extruded 3D Shadow Layer
                Text(
                    text = timeText,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    letterSpacing = (-2).sp,
                    color = tokens.accentSecondary.copy(alpha = 0.30f),
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (combinedTiltX * 16f + 4f).roundToInt(),
                            y = (combinedTiltY * 14f + 6f).roundToInt()
                        )
                    }
                )

                // Mid Cyan Prismatic Layer
                Text(
                    text = timeText,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    letterSpacing = (-2).sp,
                    color = tokens.accentPrimary.copy(alpha = 0.45f),
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (-combinedTiltX * 8f - 2f).roundToInt(),
                            y = (-combinedTiltY * 6f - 2f).roundToInt()
                        )
                    }
                )

                // Crisp Foreground Glass Numerals + Floating Seconds Badge
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (combinedTiltX * 6f).roundToInt(),
                            y = (combinedTiltY * 6f).roundToInt()
                        )
                    }
                ) {
                    Text(
                        text = timeText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 72.sp,
                        letterSpacing = (-2).sp,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = secText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = tokens.accentPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.textSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(tokens.accentPrimary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                CustomGlassIcon(
                    glyph = AeroIconGlyph.SMART_REMINDER,
                    size = 15.dp,
                    tint = tokens.accentPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$activeRemindersCount Smart Alerts",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.accentPrimary
                )
            }
        }
    }
}
