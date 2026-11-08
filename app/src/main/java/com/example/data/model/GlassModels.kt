package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskCategory(val label: String) {
    FOCUS("Deep Focus"),
    WORK("Work & Sprint"),
    PERSONAL("Personal"),
    HEALTH("Vitality"),
    SYSTEM("Device Care")
}

enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

enum class GlassThemeMode(val label: String, val subtitle: String) {
    DARK_GLASS("iOS 18 Obsidian Glass", "Deep cosmos indigo with specular cyan highlights"),
    LIGHT_FROST("Daylight Quartz Frost", "Airy alabaster glass with subtle prismatic refraction"),
    AURORA_DYNAMIC("Vision Aurora Dynamic", "Adaptive iridescent violet & teal liquid glass")
}

enum class CustomIconPack(val label: String, val description: String) {
    LIQUID_PRISM("Liquid Prism", "Multi-layered translucent geometric custom vectors"),
    MINIMAL_LINE("Precision Hairline", "Ultra-crisp 1.5px architectural line glyphs"),
    NEON_GLYPH("Luminous Cyber", "High-contrast glowing core badges with halo rings")
}

@IgnoreExtraProperties
data class GlassTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val category: String = TaskCategory.FOCUS.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val completed: Boolean = false,
    val sortOrder: Long = 0L,
    val reminderLabel: String = "",
    val reminderEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserPreferences(
    val userId: String = "",
    val blurIntensity: Long = 68L,
    val themeMode: String = GlassThemeMode.DARK_GLASS.name,
    val iconPack: String = CustomIconPack.LIQUID_PRISM.name,
    val parallaxEnabled: Boolean = true,
    val city: String = "San Francisco",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class HourlyWeatherItem(
    val hourLabel: String,
    val tempCelsius: Int,
    val conditionCode: Int
)

data class WeatherForecast(
    val city: String,
    val temperatureCelsius: Int,
    val highCelsius: Int,
    val lowCelsius: Int,
    val conditionText: String,
    val windKmh: Int,
    val humidityPercent: Int,
    val aqiLabel: String,
    val hourly: List<HourlyWeatherItem>,
    val isLiveNetwork: Boolean = true
)

data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val category: String,
    val summary: String,
    val timeAgo: String,
    val readMinutes: Int,
    val url: String = ""
)

data class SystemTelemetry(
    val usedRamMb: Long,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val ramUsageFraction: Float,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTempCelsius: Float,
    val batteryVoltageMv: Int,
    val displayRefreshRateHz: Int,
    val estimatedHoursRemaining: String,
    val powerProfile: String
)
