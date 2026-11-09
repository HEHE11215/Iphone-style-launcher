package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGlassTokens

/**
 * Reusable Glassmorphism Modifier for surfaces, widgets, and launcher cards.
 * Combines:
 * 1. Optical blur-responsive semi-transparent multi-stop frosted background
 * 2. Soft top-left directional light border with subtle bottom-right shadow rim
 * 3. Inner specular top-edge light sheen (refractive glass highlight)
 */
@Composable
fun Modifier.glassmorphism(
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    tintColor: Color = Color.Unspecified,
    backgroundAlphaBoost: Float = 1.0f,
    borderWidth: Dp = if (elevated) 1.3.dp else 1.dp,
    softLightBorderAlpha: Float = 1.0f
): Modifier = composed {
    val tokens = LocalGlassTokens.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val effectiveBlur = tokens.blurIntensity.coerceIn(0.1f, 1.0f)
    val elevationFactor = if (elevated) 1.25f else 1.0f

    val topFillAlpha = (tokens.cardFillTop.alpha * elevationFactor * backgroundAlphaBoost)
        .coerceIn(0.06f, 0.92f)
    val bottomFillAlpha = (tokens.cardFillBottom.alpha * elevationFactor * backgroundAlphaBoost)
        .coerceIn(0.03f, 0.85f)

    val topColor = if (tintColor != Color.Unspecified) {
        Color.White.copy(alpha = topFillAlpha * 0.75f)
    } else {
        tokens.cardFillTop.copy(alpha = topFillAlpha)
    }

    val midTintColor = if (tintColor != Color.Unspecified) {
        tintColor.copy(alpha = (0.10f + 0.12f * effectiveBlur).coerceIn(0.05f, 0.30f))
    } else {
        tokens.cardFillTop.copy(alpha = (topFillAlpha + bottomFillAlpha) * 0.5f)
    }

    val bottomColor = tokens.cardFillBottom.copy(alpha = bottomFillAlpha)

    val fillBrush = remember(topColor, midTintColor, bottomColor) {
        Brush.linearGradient(
            colors = listOf(topColor, midTintColor, bottomColor),
            start = Offset(0f, 0f),
            end = Offset(650f, 650f)
        )
    }

    val borderTopLight = tokens.borderHighlight.copy(
        alpha = (tokens.borderHighlight.alpha * softLightBorderAlpha).coerceIn(0.15f, 0.95f)
    )
    val borderMidSoft = Color.White.copy(
        alpha = (0.14f * effectiveBlur * softLightBorderAlpha).coerceIn(0.05f, 0.45f)
    )
    val borderBottomShadow = tokens.borderShadow

    val borderBrush = remember(borderTopLight, borderMidSoft, borderBottomShadow) {
        Brush.linearGradient(
            colors = listOf(
                borderTopLight,
                borderMidSoft,
                borderBottomShadow,
                borderTopLight.copy(alpha = borderTopLight.alpha * 0.38f)
            ),
            start = Offset(0f, 0f),
            end = Offset(550f, 550f)
        )
    }

    this
        .clip(shape)
        .background(brush = fillBrush, shape = shape)
        .drawBehind {
            val rPx = cornerRadius.toPx()
            // Soft top-edge refractive glass light sheen
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f * effectiveBlur * softLightBorderAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.42f
                ),
                cornerRadius = CornerRadius(rPx, rPx)
            )
        }
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Specialized Glassmorphism Modifier for Task Cards.
 * Applies frosted blur translucency, soft light borders, priority aura tinting,
 * and interactive drag elevation.
 */
@Composable
fun Modifier.glassTaskCard(
    cornerRadius: Dp = 20.dp,
    isDragged: Boolean = false,
    completed: Boolean = false,
    priorityAccent: Color = Color.Unspecified
): Modifier = composed {
    val tokens = LocalGlassTokens.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val effectiveBlur = tokens.blurIntensity.coerceIn(0.15f, 1.0f)

    val baseAlphaScale = when {
        isDragged -> 1.35f
        completed -> 0.68f
        else -> 1.0f
    }

    val topAlpha = (tokens.cardFillTop.alpha * baseAlphaScale).coerceIn(0.08f, 0.90f)
    val bottomAlpha = (tokens.cardFillBottom.alpha * baseAlphaScale).coerceIn(0.04f, 0.80f)

    val fillBrush = remember(tokens, isDragged, completed, priorityAccent) {
        val accentTint = if (priorityAccent != Color.Unspecified && !completed) {
            priorityAccent.copy(alpha = if (tokens.isDark) 0.12f else 0.16f)
        } else {
            tokens.cardFillTop.copy(alpha = (topAlpha + bottomAlpha) * 0.5f)
        }
        Brush.linearGradient(
            colors = listOf(
                tokens.cardFillTop.copy(alpha = topAlpha),
                accentTint,
                tokens.cardFillBottom.copy(alpha = bottomAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(520f, 320f)
        )
    }

    val softLightBorder = remember(tokens, isDragged, priorityAccent) {
        val highlightColor = if (isDragged) {
            tokens.accentPrimary.copy(alpha = 0.85f)
        } else {
            tokens.borderHighlight.copy(alpha = (tokens.borderHighlight.alpha * 1.05f).coerceIn(0.22f, 0.90f))
        }
        val secondaryRim = if (priorityAccent != Color.Unspecified) {
            priorityAccent.copy(alpha = 0.38f)
        } else {
            Color.White.copy(alpha = 0.16f * effectiveBlur)
        }
        Brush.linearGradient(
            colors = listOf(
                highlightColor,
                secondaryRim,
                tokens.borderShadow,
                highlightColor.copy(alpha = 0.32f)
            ),
            start = Offset(0f, 0f),
            end = Offset(480f, 260f)
        )
    }

    this
        .clip(shape)
        .background(brush = fillBrush, shape = shape)
        .drawBehind {
            val rPx = cornerRadius.toPx()
            // Top soft light refraction band
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDragged) 0.30f else 0.18f * effectiveBlur),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f
                ),
                cornerRadius = CornerRadius(rPx, rPx)
            )
            // Subtle inner glow rim
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f * effectiveBlur),
                cornerRadius = CornerRadius(rPx, rPx),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .border(
            width = if (isDragged) 1.5.dp else 1.dp,
            brush = softLightBorder,
            shape = shape
        )
}

/**
 * Reusable Glassmorphism Pill Modifier for Stock Android Search Bar, Filter Chips, and At-a-Glance Capsules.
 */
@Composable
fun Modifier.glassPill(
    cornerRadius: Dp = 999.dp,
    selected: Boolean = false,
    accentColor: Color = Color.Unspecified
): Modifier = composed {
    val tokens = LocalGlassTokens.current
    val resolvedAccent = if (accentColor != Color.Unspecified) accentColor else tokens.accentPrimary
    this.glassmorphism(
        cornerRadius = cornerRadius,
        elevated = selected,
        tintColor = if (selected) resolvedAccent else Color.Unspecified,
        backgroundAlphaBoost = if (selected) 1.25f else 0.95f,
        borderWidth = if (selected) 1.3.dp else 1.dp,
        softLightBorderAlpha = if (selected) 1.15f else 0.85f
    )
}

/**
 * Stock Android / Pixel Launcher Frosted Dock Shelf Modifier.
 */
@Composable
fun Modifier.glassDockShelf(
    cornerRadius: Dp = 30.dp
): Modifier = composed {
    this.glassmorphism(
        cornerRadius = cornerRadius,
        elevated = true,
        backgroundAlphaBoost = 1.15f,
        borderWidth = 1.1.dp,
        softLightBorderAlpha = 1.0f
    )
}
