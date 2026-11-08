package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomIconPack
import com.example.ui.theme.LocalGlassTokens
import kotlin.math.cos
import kotlin.math.sin

enum class AeroIconGlyph(val contentDesc: String) {
    DASHBOARD("Dashboard home"),
    TASKS("Tasks manager"),
    DISCOVER_NEWS("Discover news and weather"),
    SYSTEM_OPTIMIZER("RAM and battery optimizer"),
    QUICK_SETTINGS("Quick settings and customization"),
    RAM_BOOST("Clean RAM boost"),
    BATTERY_PULSE("Battery health and thermal optimizer"),
    WEATHER_SUN_CLOUD("Weather condition"),
    SMART_REMINDER("Smart reminder bell"),
    CLOCK_3D("3D spatial clock"),
    DRAG_HANDLE("Drag to reorder task"),
    CHECK_COMPLETE("Task completed"),
    WIFI("Wi-Fi toggle"),
    BLUETOOTH("Bluetooth toggle"),
    DND_FOCUS("Distraction-free focus mode"),
    FLASHLIGHT("Torch flashlight toggle"),
    AUTO_BRIGHTNESS("Adaptive brightness"),
    PARALLAX_3D("3D parallax motion sensor")
}

@Composable
fun CustomGlassIcon(
    glyph: AeroIconGlyph,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalGlassTokens.current.accentPrimary,
    secondaryTint: Color = LocalGlassTokens.current.accentSecondary,
    iconPack: CustomIconPack = LocalGlassTokens.current.iconPack
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = glyph.contentDesc }
    ) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = when (iconPack) {
            CustomIconPack.MINIMAL_LINE -> w * 0.075f
            CustomIconPack.LIQUID_PRISM -> w * 0.095f
            CustomIconPack.NEON_GLYPH -> w * 0.105f
        }

        // Icon Pack Background Halo / Prism Layer
        when (iconPack) {
            CustomIconPack.LIQUID_PRISM -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.24f),
                            secondaryTint.copy(alpha = 0.10f)
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    ),
                    topLeft = Offset(w * 0.06f, h * 0.06f),
                    size = Size(w * 0.88f, h * 0.88f),
                    cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
                )
            }
            CustomIconPack.NEON_GLYPH -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.5f,
                    center = Offset(w / 2f, h / 2f)
                )
            }
            CustomIconPack.MINIMAL_LINE -> Unit
        }

        drawGlyphVector(
            glyph = glyph,
            tint = tint,
            secondaryTint = secondaryTint,
            strokeWidth = strokeWidth,
            isFilledAccent = iconPack == CustomIconPack.LIQUID_PRISM
        )
    }
}

private fun DrawScope.drawGlyphVector(
    glyph: AeroIconGlyph,
    tint: Color,
    secondaryTint: Color,
    strokeWidth: Float,
    isFilledAccent: Boolean
) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (glyph) {
        AeroIconGlyph.DASHBOARD -> {
            val pad = w * 0.20f
            val box = w * 0.24f
            val gap = w * 0.12f
            drawRoundRect(
                color = tint,
                topLeft = Offset(pad, pad),
                size = Size(box, box),
                cornerRadius = CornerRadius(w * 0.07f),
                style = stroke
            )
            drawRoundRect(
                color = secondaryTint,
                topLeft = Offset(pad + box + gap, pad),
                size = Size(box, box),
                cornerRadius = CornerRadius(w * 0.07f),
                style = if (isFilledAccent) Stroke(strokeWidth * 1.1f) else stroke
            )
            drawRoundRect(
                color = secondaryTint,
                topLeft = Offset(pad, pad + box + gap),
                size = Size(box, box),
                cornerRadius = CornerRadius(w * 0.07f),
                style = stroke
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(pad + box + gap, pad + box + gap),
                size = Size(box, box),
                cornerRadius = CornerRadius(w * 0.07f),
                style = stroke
            )
        }

        AeroIconGlyph.TASKS -> {
            val path = Path().apply {
                moveTo(w * 0.24f, h * 0.52f)
                lineTo(w * 0.43f, h * 0.70f)
                lineTo(w * 0.78f, h * 0.32f)
            }
            drawRoundRect(
                color = secondaryTint.copy(alpha = 0.65f),
                topLeft = Offset(w * 0.16f, h * 0.16f),
                size = Size(w * 0.68f, h * 0.68f),
                cornerRadius = CornerRadius(w * 0.20f),
                style = stroke
            )
            drawPath(path = path, color = tint, style = stroke)
        }

        AeroIconGlyph.DISCOVER_NEWS -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.18f, h * 0.20f),
                size = Size(w * 0.64f, h * 0.60f),
                cornerRadius = CornerRadius(w * 0.15f),
                style = stroke
            )
            drawLine(
                color = secondaryTint,
                start = Offset(w * 0.30f, h * 0.38f),
                end = Offset(w * 0.70f, h * 0.38f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.30f, h * 0.53f),
                end = Offset(w * 0.62f, h * 0.53f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.75f),
                start = Offset(w * 0.30f, h * 0.66f),
                end = Offset(w * 0.52f, h * 0.66f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        AeroIconGlyph.SYSTEM_OPTIMIZER, AeroIconGlyph.RAM_BOOST -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.26f, h * 0.26f),
                size = Size(w * 0.48f, h * 0.48f),
                cornerRadius = CornerRadius(w * 0.10f),
                style = stroke
            )
            if (isFilledAccent) {
                drawRoundRect(
                    color = secondaryTint.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.38f, h * 0.38f),
                    size = Size(w * 0.24f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.05f)
                )
            }
            val pins = listOf(0.38f, 0.62f)
            pins.forEach { p ->
                drawLine(tint, Offset(w * p, h * 0.14f), Offset(w * p, h * 0.26f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(w * p, h * 0.74f), Offset(w * p, h * 0.86f), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.14f, h * p), Offset(w * 0.26f, h * p), strokeWidth, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.74f, h * p), Offset(w * 0.86f, h * p), strokeWidth, StrokeCap.Round)
            }
        }

        AeroIconGlyph.QUICK_SETTINGS -> {
            drawLine(tint, Offset(w * 0.22f, h * 0.32f), Offset(w * 0.78f, h * 0.32f), strokeWidth, StrokeCap.Round)
            drawCircle(secondaryTint, radius = w * 0.09f, center = Offset(w * 0.38f, h * 0.32f))
            drawLine(tint, Offset(w * 0.22f, h * 0.52f), Offset(w * 0.78f, h * 0.52f), strokeWidth, StrokeCap.Round)
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.64f, h * 0.52f))
            drawLine(tint, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.78f, h * 0.72f), strokeWidth, StrokeCap.Round)
            drawCircle(secondaryTint, radius = w * 0.09f, center = Offset(w * 0.46f, h * 0.72f))
        }

        AeroIconGlyph.BATTERY_PULSE -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.18f, h * 0.28f),
                size = Size(w * 0.58f, h * 0.44f),
                cornerRadius = CornerRadius(w * 0.10f),
                style = stroke
            )
            drawRoundRect(
                color = secondaryTint,
                topLeft = Offset(w * 0.78f, h * 0.40f),
                size = Size(w * 0.06f, h * 0.20f),
                cornerRadius = CornerRadius(w * 0.03f)
            )
            val bolt = Path().apply {
                moveTo(w * 0.48f, h * 0.34f)
                lineTo(w * 0.38f, h * 0.51f)
                lineTo(w * 0.50f, h * 0.51f)
                lineTo(w * 0.44f, h * 0.66f)
            }
            drawPath(bolt, color = secondaryTint, style = stroke)
        }

        AeroIconGlyph.WEATHER_SUN_CLOUD -> {
            drawCircle(
                color = secondaryTint,
                radius = w * 0.16f,
                center = Offset(w * 0.38f, h * 0.38f),
                style = stroke
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.28f, h * 0.46f),
                size = Size(w * 0.52f, h * 0.28f),
                cornerRadius = CornerRadius(w * 0.14f),
                style = stroke
            )
        }

        AeroIconGlyph.SMART_REMINDER -> {
            val bell = Path().apply {
                moveTo(w * 0.30f, h * 0.64f)
                lineTo(w * 0.70f, h * 0.64f)
                lineTo(w * 0.64f, h * 0.42f)
                cubicTo(w * 0.64f, h * 0.26f, w * 0.36f, h * 0.26f, w * 0.36f, h * 0.42f)
                close()
            }
            drawPath(bell, color = tint, style = stroke)
            drawCircle(color = secondaryTint, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.75f))
        }

        AeroIconGlyph.CLOCK_3D -> {
            drawCircle(
                color = tint,
                radius = w * 0.34f,
                center = Offset(w * 0.50f, h * 0.50f),
                style = stroke
            )
            drawLine(
                color = secondaryTint,
                start = Offset(w * 0.50f, h * 0.50f),
                end = Offset(w * 0.50f, h * 0.30f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.50f, h * 0.50f),
                end = Offset(w * 0.66f, h * 0.56f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        AeroIconGlyph.DRAG_HANDLE -> {
            val rows = listOf(0.32f, 0.50f, 0.68f)
            val cols = listOf(0.38f, 0.62f)
            rows.forEach { r ->
                cols.forEach { c ->
                    drawCircle(
                        color = tint.copy(alpha = 0.85f),
                        radius = w * 0.055f,
                        center = Offset(w * c, h * r)
                    )
                }
            }
        }

        AeroIconGlyph.CHECK_COMPLETE -> {
            drawCircle(
                color = tint.copy(alpha = 0.25f),
                radius = w * 0.36f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
            val check = Path().apply {
                moveTo(w * 0.30f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.66f)
                lineTo(w * 0.72f, h * 0.36f)
            }
            drawPath(check, color = tint, style = stroke)
        }

        AeroIconGlyph.WIFI -> {
            drawArc(
                color = tint,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(w * 0.16f, h * 0.24f),
                size = Size(w * 0.68f, h * 0.68f),
                style = stroke
            )
            drawArc(
                color = secondaryTint,
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.40f),
                size = Size(w * 0.44f, h * 0.44f),
                style = stroke
            )
            drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.72f))
        }

        AeroIconGlyph.BLUETOOTH -> {
            val bt = Path().apply {
                moveTo(w * 0.34f, h * 0.34f)
                lineTo(w * 0.64f, h * 0.64f)
                lineTo(w * 0.48f, h * 0.78f)
                lineTo(w * 0.48f, h * 0.22f)
                lineTo(w * 0.64f, h * 0.36f)
                lineTo(w * 0.34f, h * 0.66f)
            }
            drawPath(bt, color = tint, style = stroke)
        }

        AeroIconGlyph.DND_FOCUS -> {
            drawCircle(
                color = tint,
                radius = w * 0.32f,
                center = Offset(w * 0.50f, h * 0.50f),
                style = stroke
            )
            drawLine(
                color = secondaryTint,
                start = Offset(w * 0.34f, h * 0.50f),
                end = Offset(w * 0.66f, h * 0.50f),
                strokeWidth = strokeWidth * 1.15f,
                cap = StrokeCap.Round
            )
        }

        AeroIconGlyph.FLASHLIGHT -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.36f, h * 0.24f),
                size = Size(w * 0.28f, h * 0.54f),
                cornerRadius = CornerRadius(w * 0.08f),
                style = stroke
            )
            drawLine(
                color = secondaryTint,
                start = Offset(w * 0.50f, h * 0.12f),
                end = Offset(w * 0.50f, h * 0.18f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        AeroIconGlyph.AUTO_BRIGHTNESS -> {
            drawCircle(
                color = tint,
                radius = w * 0.18f,
                center = Offset(w * 0.50f, h * 0.50f),
                style = stroke
            )
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val r1 = w * 0.27f
                val r2 = w * 0.36f
                drawLine(
                    color = secondaryTint,
                    start = Offset((w / 2f + cos(angle) * r1).toFloat(), (h / 2f + sin(angle) * r1).toFloat()),
                    end = Offset((w / 2f + cos(angle) * r2).toFloat(), (h / 2f + sin(angle) * r2).toFloat()),
                    strokeWidth = strokeWidth * 0.85f,
                    cap = StrokeCap.Round
                )
            }
        }

        AeroIconGlyph.PARALLAX_3D -> {
            drawRoundRect(
                color = secondaryTint.copy(alpha = 0.65f),
                topLeft = Offset(w * 0.28f, h * 0.18f),
                size = Size(w * 0.50f, h * 0.50f),
                cornerRadius = CornerRadius(w * 0.12f),
                style = stroke
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.20f, h * 0.30f),
                size = Size(w * 0.50f, h * 0.50f),
                cornerRadius = CornerRadius(w * 0.12f),
                style = stroke
            )
        }
    }
}
