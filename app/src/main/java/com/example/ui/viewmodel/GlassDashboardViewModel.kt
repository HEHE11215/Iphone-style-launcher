package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassTask
import com.example.data.model.GlassThemeMode
import com.example.data.model.NewsArticle
import com.example.data.model.SystemTelemetry
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.UserPreferences
import com.example.data.model.WeatherForecast
import com.example.data.repository.GlassDashboardRepository
import com.example.util.DeviceTelemetryHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class QuickSettingsState(
    val wifiEnabled: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val focusDndEnabled: Boolean = false,
    val torchEnabled: Boolean = false,
    val adaptiveBrightness: Boolean = true,
    val powerProfile: String = "Adaptive 90Hz+ Balanced",
    val reclaimedRamBonusMb: Long = 0L,
    val lastRamBoostSummary: String = "Ready • Ultra-low memory footprint"
)

class GlassDashboardViewModel(
    private val repository: GlassDashboardRepository,
    private val currentUserId: String
) : ViewModel() {

    val tasksState: StateFlow<UiState<List<GlassTask>>> = repository.observeTasks(currentUserId)
        .map<List<GlassTask>, UiState<List<GlassTask>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val preferencesState: StateFlow<UserPreferences> = repository.observePreferences(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing preferences", firestoreError ?: error)
            emit(UserPreferences(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences(userId = currentUserId)
        )

    private val _weatherState = MutableStateFlow<WeatherForecast?>(null)
    val weatherState: StateFlow<WeatherForecast?> = _weatherState.asStateFlow()

    private val _newsState = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsState: StateFlow<List<NewsArticle>> = _newsState.asStateFlow()

    private val _quickSettings = MutableStateFlow(QuickSettingsState())
    val quickSettings: StateFlow<QuickSettingsState> = _quickSettings.asStateFlow()

    private val _telemetryState = MutableStateFlow(
        SystemTelemetry(
            usedRamMb = 3120L,
            totalRamMb = 8192L,
            availableRamMb = 5072L,
            ramUsageFraction = 0.38f,
            batteryPercent = 88,
            isCharging = true,
            batteryTempCelsius = 28.4f,
            batteryVoltageMv = 4180,
            displayRefreshRateHz = 120,
            estimatedHoursRemaining = "16h 40m",
            powerProfile = "Adaptive 90Hz+ Balanced"
        )
    )
    val telemetryState: StateFlow<SystemTelemetry> = _telemetryState.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TaskCategory?>(null)
    val selectedCategoryFilter: StateFlow<TaskCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterTasksIfEmpty()
        }
        refreshWeatherAndNews("San Francisco")
    }

    fun refreshTelemetry(context: Context) {
        val qs = _quickSettings.value
        _telemetryState.value = DeviceTelemetryHelper.readSystemTelemetry(
            context = context,
            powerProfile = qs.powerProfile,
            reclaimedMbBonus = qs.reclaimedRamBonusMb
        )
    }

    fun refreshWeatherAndNews(city: String = preferencesState.value.city) {
        viewModelScope.launch {
            _weatherState.value = repository.fetchLiveWeather(city)
        }
        viewModelScope.launch {
            _newsState.value = repository.fetchDiscoverNews()
        }
    }

    fun setCategoryFilter(category: TaskCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun addTask(
        context: Context,
        title: String,
        notes: String,
        category: TaskCategory,
        priority: TaskPriority,
        reminderLabel: String,
        reminderEnabled: Boolean
    ) {
        if (title.isBlank()) return
        val currentList = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val nextOrder = (currentList.maxOfOrNull { it.sortOrder.toInt() } ?: -1) + 1
        viewModelScope.launch {
            val result = repository.createTask(
                title = title,
                notes = notes,
                category = category,
                priority = priority,
                sortOrder = nextOrder,
                reminderLabel = reminderLabel,
                reminderEnabled = reminderEnabled
            )
            if (result.isSuccess && reminderEnabled && reminderLabel.isNotBlank()) {
                DeviceTelemetryHelper.postSmartReminderNotification(context, title, reminderLabel)
                _statusBannerMessage.value = "Smart Reminder armed for $reminderLabel"
            }
        }
    }

    fun toggleTaskCompleted(task: GlassTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = !task.completed))
        }
    }

    fun toggleTaskReminder(context: Context, task: GlassTask) {
        val nextEnabled = !task.reminderEnabled
        val label = task.reminderLabel.ifBlank { "09:00 AM • Smart Focus" }
        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    reminderEnabled = nextEnabled,
                    reminderLabel = label
                )
            )
            if (nextEnabled) {
                DeviceTelemetryHelper.postSmartReminderNotification(context, task.title, label)
                _statusBannerMessage.value = "Smart Reminder active: $label"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun commitReorderedTasks(reorderedList: List<GlassTask>) {
        viewModelScope.launch {
            repository.updateTaskOrderBatch(reorderedList)
        }
    }

    fun updateBlurIntensity(newBlurPercent: Int) {
        val prefs = preferencesState.value
        viewModelScope.launch {
            repository.savePreferences(
                blurIntensity = newBlurPercent.coerceIn(0, 100),
                themeMode = runCatching { GlassThemeMode.valueOf(prefs.themeMode) }.getOrDefault(GlassThemeMode.DARK_GLASS),
                iconPack = runCatching { CustomIconPack.valueOf(prefs.iconPack) }.getOrDefault(CustomIconPack.LIQUID_PRISM),
                parallaxEnabled = prefs.parallaxEnabled,
                city = prefs.city
            )
        }
    }

    fun updateThemeMode(themeMode: GlassThemeMode) {
        val prefs = preferencesState.value
        viewModelScope.launch {
            repository.savePreferences(
                blurIntensity = prefs.blurIntensity.toInt(),
                themeMode = themeMode,
                iconPack = runCatching { CustomIconPack.valueOf(prefs.iconPack) }.getOrDefault(CustomIconPack.LIQUID_PRISM),
                parallaxEnabled = prefs.parallaxEnabled,
                city = prefs.city
            )
        }
    }

    fun updateIconPack(iconPack: CustomIconPack) {
        val prefs = preferencesState.value
        viewModelScope.launch {
            repository.savePreferences(
                blurIntensity = prefs.blurIntensity.toInt(),
                themeMode = runCatching { GlassThemeMode.valueOf(prefs.themeMode) }.getOrDefault(GlassThemeMode.DARK_GLASS),
                iconPack = iconPack,
                parallaxEnabled = prefs.parallaxEnabled,
                city = prefs.city
            )
        }
    }

    fun toggleParallax(enabled: Boolean) {
        val prefs = preferencesState.value
        viewModelScope.launch {
            repository.savePreferences(
                blurIntensity = prefs.blurIntensity.toInt(),
                themeMode = runCatching { GlassThemeMode.valueOf(prefs.themeMode) }.getOrDefault(GlassThemeMode.DARK_GLASS),
                iconPack = runCatching { CustomIconPack.valueOf(prefs.iconPack) }.getOrDefault(CustomIconPack.LIQUID_PRISM),
                parallaxEnabled = enabled,
                city = prefs.city
            )
        }
    }

    fun updateWeatherCity(city: String) {
        val prefs = preferencesState.value
        viewModelScope.launch {
            repository.savePreferences(
                blurIntensity = prefs.blurIntensity.toInt(),
                themeMode = runCatching { GlassThemeMode.valueOf(prefs.themeMode) }.getOrDefault(GlassThemeMode.DARK_GLASS),
                iconPack = runCatching { CustomIconPack.valueOf(prefs.iconPack) }.getOrDefault(CustomIconPack.LIQUID_PRISM),
                parallaxEnabled = prefs.parallaxEnabled,
                city = city
            )
            _weatherState.value = repository.fetchLiveWeather(city)
        }
    }

    fun performRamCleanBoost(context: Context) {
        val reclaimed = DeviceTelemetryHelper.performLightweightRamBoost(context)
        _quickSettings.update { current ->
            val updatedBonus = (current.reclaimedRamBonusMb + reclaimed).coerceAtMost(1450L)
            current.copy(
                reclaimedRamBonusMb = updatedBonus,
                lastRamBoostSummary = "Freed ${reclaimed}MB • 90Hz+ Frame Pacing Locked"
            )
        }
        refreshTelemetry(context)
        _statusBannerMessage.value = "RAM Clean complete: Freed ${reclaimed}MB cache for 90fps+ glass smoothness"
    }

    fun cyclePowerProfile(context: Context) {
        val profiles = listOf(
            "Adaptive 90Hz+ Balanced",
            "Ultra Endurance Eco Saver",
            "120Hz Peak Glass Fluidity"
        )
        _quickSettings.update { current ->
            val idx = profiles.indexOf(current.powerProfile)
            val next = profiles[(idx + 1) % profiles.size]
            current.copy(powerProfile = next)
        }
        refreshTelemetry(context)
    }

    fun toggleQuickSetting(tile: String) {
        _quickSettings.update { s ->
            when (tile) {
                "wifi" -> s.copy(wifiEnabled = !s.wifiEnabled)
                "bt" -> s.copy(bluetoothEnabled = !s.bluetoothEnabled)
                "dnd" -> s.copy(focusDndEnabled = !s.focusDndEnabled)
                "torch" -> s.copy(torchEnabled = !s.torchEnabled)
                "brightness" -> s.copy(adaptiveBrightness = !s.adaptiveBrightness)
                else -> s
            }
        }
    }

    private companion object {
        const val TAG = "GlassDashboardVM"
    }
}
