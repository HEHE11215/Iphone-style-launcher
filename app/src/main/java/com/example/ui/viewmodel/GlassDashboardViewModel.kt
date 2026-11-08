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
import com.google.firebase.Timestamp
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    private val currentUserId: String,
    private val isGuestSession: Boolean = false,
    private val appContext: Context? = null
) : ViewModel() {

    private val guestPrefsStore by lazy {
        appContext?.applicationContext?.getSharedPreferences("aeroglass_guest_store", Context.MODE_PRIVATE)
    }

    private val _guestTasksFlow = MutableStateFlow<List<GlassTask>>(emptyList())
    private val _guestPreferencesFlow = MutableStateFlow(UserPreferences(userId = "guest_local"))

    val tasksState: StateFlow<UiState<List<GlassTask>>> = (
        if (isGuestSession) {
            _guestTasksFlow.map<List<GlassTask>, UiState<List<GlassTask>>> { list ->
                UiState.Success(
                    list.sortedWith(
                        compareBy<GlassTask> { it.completed }
                            .thenBy { it.sortOrder }
                    )
                )
            }
        } else {
            repository.observeTasks(currentUserId)
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
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    val preferencesState: StateFlow<UserPreferences> = (
        if (isGuestSession) {
            _guestPreferencesFlow
        } else {
            repository.observePreferences(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing preferences", firestoreError ?: error)
                    emit(UserPreferences(userId = currentUserId))
                }
        }
    ).stateIn(
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
        if (isGuestSession) {
            loadGuestDataFromDisk()
        } else {
            viewModelScope.launch {
                repository.seedStarterTasksIfEmpty()
            }
        }
        refreshWeatherAndNews(_guestPreferencesFlow.value.city)
    }

    private fun loadGuestDataFromDisk() {
        val prefs = guestPrefsStore
        val loadedPrefs = UserPreferences(
            userId = "guest_local",
            blurIntensity = prefs?.getLong("blurIntensity", 68L) ?: 68L,
            themeMode = prefs?.getString("themeMode", GlassThemeMode.DARK_GLASS.name) ?: GlassThemeMode.DARK_GLASS.name,
            iconPack = prefs?.getString("iconPack", CustomIconPack.LIQUID_PRISM.name) ?: CustomIconPack.LIQUID_PRISM.name,
            parallaxEnabled = prefs?.getBoolean("parallaxEnabled", true) ?: true,
            city = prefs?.getString("city", "San Francisco") ?: "San Francisco"
        )
        _guestPreferencesFlow.value = loadedPrefs

        val rawTasksJson = prefs?.getString("guest_tasks_json", null)
        if (rawTasksJson.isNullOrBlank()) {
            val starter = defaultGuestStarterTasks()
            _guestTasksFlow.value = starter
            saveGuestTasksToDisk(starter)
        } else {
            val parsed = runCatching {
                val arr = JSONArray(rawTasksJson)
                (0 until arr.length()).map { idx ->
                    val obj = arr.getJSONObject(idx)
                    GlassTask(
                        id = obj.optString("id", "guest_$idx"),
                        userId = "guest_local",
                        title = obj.optString("title", ""),
                        notes = obj.optString("notes", ""),
                        category = obj.optString("category", TaskCategory.FOCUS.name),
                        priority = obj.optString("priority", TaskPriority.MEDIUM.name),
                        completed = obj.optBoolean("completed", false),
                        sortOrder = obj.optLong("sortOrder", idx.toLong()),
                        reminderLabel = obj.optString("reminderLabel", ""),
                        reminderEnabled = obj.optBoolean("reminderEnabled", false),
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )
                }
            }.getOrElse { defaultGuestStarterTasks() }
            _guestTasksFlow.value = parsed
        }
    }

    private fun saveGuestTasksToDisk(tasks: List<GlassTask>) {
        val prefs = guestPrefsStore ?: return
        val arr = JSONArray()
        tasks.forEach { task ->
            arr.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("notes", task.notes)
                    put("category", task.category)
                    put("priority", task.priority)
                    put("completed", task.completed)
                    put("sortOrder", task.sortOrder)
                    put("reminderLabel", task.reminderLabel)
                    put("reminderEnabled", task.reminderEnabled)
                }
            )
        }
        prefs.edit().putString("guest_tasks_json", arr.toString()).apply()
    }

    private fun saveGuestPreferencesToDisk(userPrefs: UserPreferences) {
        val prefs = guestPrefsStore ?: return
        prefs.edit()
            .putLong("blurIntensity", userPrefs.blurIntensity)
            .putString("themeMode", userPrefs.themeMode)
            .putString("iconPack", userPrefs.iconPack)
            .putBoolean("parallaxEnabled", userPrefs.parallaxEnabled)
            .putString("city", userPrefs.city)
            .apply()
    }

    private fun defaultGuestStarterTasks(): List<GlassTask> = listOf(
        GlassTask(
            id = "guest_1",
            userId = "guest_local",
            title = "Calibrate Liquid Glass Blur & 3D Parallax Clock",
            notes = "Adjust the real-time frosted refraction slider and tilt the 3D spatial clock on the home screen.",
            category = TaskCategory.FOCUS.name,
            priority = TaskPriority.HIGH.name,
            completed = false,
            sortOrder = 0L,
            reminderLabel = "09:30 AM • Smart Focus",
            reminderEnabled = true,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        ),
        GlassTask(
            id = "guest_2",
            userId = "guest_local",
            title = "Long-Press & Drag to Reorder Priority Sprints",
            notes = "Hold any glass task card to elevate and smoothly reorder your daily workflow at 90Hz+.",
            category = TaskCategory.WORK.name,
            priority = TaskPriority.HIGH.name,
            completed = false,
            sortOrder = 1L,
            reminderLabel = "11:00 AM • Sprint Sync",
            reminderEnabled = true,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        ),
        GlassTask(
            id = "guest_3",
            userId = "guest_local",
            title = "Run 90Hz+ RAM Boost & Thermal Battery Optimizer",
            notes = "Open the System Care glass widget to purge background cache and inspect live battery voltage.",
            category = TaskCategory.SYSTEM.name,
            priority = TaskPriority.MEDIUM.name,
            completed = false,
            sortOrder = 2L,
            reminderLabel = "02:00 PM • Device Care",
            reminderEnabled = true,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        ),
        GlassTask(
            id = "guest_4",
            userId = "guest_local",
            title = "Swipe Left to Pixel Discover & Live Weather Feed",
            notes = "Browse personalized tech briefings and hourly atmospheric forecasts in the left feed.",
            category = TaskCategory.PERSONAL.name,
            priority = TaskPriority.LOW.name,
            completed = false,
            sortOrder = 3L,
            reminderLabel = "05:15 PM • Evening Digest",
            reminderEnabled = false,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    )

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

        if (isGuestSession) {
            val newTask = GlassTask(
                id = "guest_${UUID.randomUUID().toString().take(8)}",
                userId = "guest_local",
                title = title.trim().take(140),
                notes = notes.trim().take(500),
                category = category.name,
                priority = priority.name,
                completed = false,
                sortOrder = nextOrder.toLong(),
                reminderLabel = reminderLabel.trim().take(80),
                reminderEnabled = reminderEnabled,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val updated = _guestTasksFlow.value + newTask
            _guestTasksFlow.value = updated
            saveGuestTasksToDisk(updated)
            if (reminderEnabled && reminderLabel.isNotBlank()) {
                DeviceTelemetryHelper.postSmartReminderNotification(context, title, reminderLabel)
                _statusBannerMessage.value = "Smart Reminder armed for $reminderLabel"
            }
            return
        }

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
        if (isGuestSession) {
            val updated = _guestTasksFlow.value.map {
                if (it.id == task.id) it.copy(completed = !it.completed) else it
            }
            _guestTasksFlow.value = updated
            saveGuestTasksToDisk(updated)
            return
        }
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = !task.completed))
        }
    }

    fun toggleTaskReminder(context: Context, task: GlassTask) {
        val nextEnabled = !task.reminderEnabled
        val label = task.reminderLabel.ifBlank { "09:00 AM • Smart Focus" }
        if (isGuestSession) {
            val updated = _guestTasksFlow.value.map {
                if (it.id == task.id) it.copy(reminderEnabled = nextEnabled, reminderLabel = label) else it
            }
            _guestTasksFlow.value = updated
            saveGuestTasksToDisk(updated)
            if (nextEnabled) {
                DeviceTelemetryHelper.postSmartReminderNotification(context, task.title, label)
                _statusBannerMessage.value = "Smart Reminder active: $label"
            }
            return
        }
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
        if (isGuestSession) {
            val updated = _guestTasksFlow.value.filterNot { it.id == taskId }
            _guestTasksFlow.value = updated
            saveGuestTasksToDisk(updated)
            return
        }
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun commitReorderedTasks(reorderedList: List<GlassTask>) {
        if (isGuestSession) {
            val indexed = reorderedList.mapIndexed { idx, item -> item.copy(sortOrder = idx.toLong()) }
            _guestTasksFlow.value = indexed
            saveGuestTasksToDisk(indexed)
            return
        }
        viewModelScope.launch {
            repository.updateTaskOrderBatch(reorderedList)
        }
    }

    fun updateBlurIntensity(newBlurPercent: Int) {
        val prefs = preferencesState.value
        if (isGuestSession) {
            val updated = prefs.copy(blurIntensity = newBlurPercent.coerceIn(0, 100).toLong())
            _guestPreferencesFlow.value = updated
            saveGuestPreferencesToDisk(updated)
            return
        }
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
        if (isGuestSession) {
            val updated = prefs.copy(themeMode = themeMode.name)
            _guestPreferencesFlow.value = updated
            saveGuestPreferencesToDisk(updated)
            return
        }
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
        if (isGuestSession) {
            val updated = prefs.copy(iconPack = iconPack.name)
            _guestPreferencesFlow.value = updated
            saveGuestPreferencesToDisk(updated)
            return
        }
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
        if (isGuestSession) {
            val updated = prefs.copy(parallaxEnabled = enabled)
            _guestPreferencesFlow.value = updated
            saveGuestPreferencesToDisk(updated)
            return
        }
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
        if (isGuestSession) {
            val updated = prefs.copy(city = city)
            _guestPreferencesFlow.value = updated
            saveGuestPreferencesToDisk(updated)
            viewModelScope.launch {
                _weatherState.value = repository.fetchLiveWeather(city)
            }
            return
        }
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
