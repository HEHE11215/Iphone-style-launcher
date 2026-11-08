package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassThemeMode

@Immutable
data class GlassVisualTokens(
    val isDark: Boolean,
    val blurIntensity: Float, // 0f..1f
    val themeMode: GlassThemeMode,
    val iconPack: CustomIconPack,
    val parallaxEnabled: Boolean,
    val cardFillTop: Color,
    val cardFillBottom: Color,
    val borderHighlight: Color,
    val borderShadow: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val bgGradientStart: Color,
    val bgGradientEnd: Color
)

val LocalGlassTokens = staticCompositionLocalOf {
    GlassVisualTokens(
        isDark = true,
        blurIntensity = 0.68f,
        themeMode = GlassThemeMode.DARK_GLASS,
        iconPack = CustomIconPack.LIQUID_PRISM,
        parallaxEnabled = true,
        cardFillTop = Color(0x38FFFFFF),
        cardFillBottom = Color(0x14FFFFFF),
        borderHighlight = Color(0x88FFFFFF),
        borderShadow = Color(0x18FFFFFF),
        accentPrimary = CyanSpecular,
        accentSecondary = VioletAurora,
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFB4C6E7),
        bgGradientStart = ObsidianBgStart,
        bgGradientEnd = ObsidianBgEnd
    )
}

private val DarkGlassColorScheme = darkColorScheme(
    primary = CyanSpecular,
    secondary = VioletAurora,
    tertiary = EmeraldPulse,
    background = ObsidianBgStart,
    surface = ObsidianBgEnd,
    onPrimary = Color(0xFF041326),
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val LightFrostColorScheme = lightColorScheme(
    primary = QuartzPrimary,
    secondary = QuartzSecondary,
    tertiary = Color(0xFF059669),
    background = QuartzBgStart,
    surface = Color(0xFFF8FAFC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = QuartzTextPrimary,
    onSurface = QuartzTextPrimary
)

@Composable
fun AeroGlassTheme(
    themeMode: GlassThemeMode = GlassThemeMode.DARK_GLASS,
    blurIntensityPercent: Int = 68,
    iconPack: CustomIconPack = CustomIconPack.LIQUID_PRISM,
    parallaxEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        GlassThemeMode.DARK_GLASS -> true
        GlassThemeMode.LIGHT_FROST -> false
        GlassThemeMode.AURORA_DYNAMIC -> true
    }

    val blurFraction = (blurIntensityPercent.coerceIn(0, 100) / 100f)

    // Ultra-lightweight frosted glass alpha calculation that adapts to blurIntensity
    val topAlpha = if (isDark) (0.10f + blurFraction * 0.18f) else (0.52f + blurFraction * 0.34f)
    val bottomAlpha = if (isDark) (0.04f + blurFraction * 0.10f) else (0.32f + blurFraction * 0.26f)
    val borderTopAlpha = if (isDark) (0.28f + blurFraction * 0.42f) else (0.65f + blurFraction * 0.30f)

    val tokens = when (themeMode) {
        GlassThemeMode.DARK_GLASS -> GlassVisualTokens(
            isDark = true,
            blurIntensity = blurFraction,
            themeMode = themeMode,
            iconPack = iconPack,
            parallaxEnabled = parallaxEnabled,
            cardFillTop = Color.White.copy(alpha = topAlpha),
            cardFillBottom = Color(0xFF38BDF8).copy(alpha = bottomAlpha),
            borderHighlight = Color.White.copy(alpha = borderTopAlpha),
            borderShadow = Color(0xFF38BDF8).copy(alpha = 0.12f),
            accentPrimary = CyanSpecular,
            accentSecondary = VioletAurora,
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFB8C7E0),
            bgGradientStart = ObsidianBgStart,
            bgGradientEnd = ObsidianBgEnd
        )
        GlassThemeMode.LIGHT_FROST -> GlassVisualTokens(
            isDark = false,
            blurIntensity = blurFraction,
            themeMode = themeMode,
            iconPack = iconPack,
            parallaxEnabled = parallaxEnabled,
            cardFillTop = Color.White.copy(alpha = topAlpha),
            cardFillBottom = Color(0xFFE0F2FE).copy(alpha = bottomAlpha),
            borderHighlight = Color.White.copy(alpha = borderTopAlpha),
            borderShadow = Color(0xFF0284C7).copy(alpha = 0.16f),
            accentPrimary = QuartzPrimary,
            accentSecondary = QuartzSecondary,
            textPrimary = QuartzTextPrimary,
            textSecondary = QuartzTextSecondary,
            bgGradientStart = QuartzBgStart,
            bgGradientEnd = QuartzBgEnd
        )
        GlassThemeMode.AURORA_DYNAMIC -> GlassVisualTokens(
            isDark = true,
            blurIntensity = blurFraction,
            themeMode = themeMode,
            iconPack = iconPack,
            parallaxEnabled = parallaxEnabled,
            cardFillTop = Color(0xFF2DD4BF).copy(alpha = topAlpha * 0.85f),
            cardFillBottom = Color(0xFFC084FC).copy(alpha = bottomAlpha * 1.15f),
            borderHighlight = Color(0xFF99F6E4).copy(alpha = borderTopAlpha),
            borderShadow = Color(0xFFC084FC).copy(alpha = 0.18f),
            accentPrimary = AuroraPrimary,
            accentSecondary = AuroraSecondary,
            textPrimary = Color(0xFFF0FDFA),
            textSecondary = Color(0xFFCCFBF1),
            bgGradientStart = AuroraBgStart,
            bgGradientEnd = AuroraBgEnd
        )
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeMode == GlassThemeMode.AURORA_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (systemDark) dynamicDarkColorScheme(context) else DarkGlassColorScheme
        }
        isDark -> DarkGlassColorScheme
        else -> LightFrostColorScheme
    }

    CompositionLocalProvider(LocalGlassTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    AeroGlassTheme(content = content)
}
