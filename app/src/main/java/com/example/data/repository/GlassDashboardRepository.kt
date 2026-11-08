package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassTask
import com.example.data.model.GlassThemeMode
import com.example.data.model.HourlyWeatherItem
import com.example.data.model.NewsArticle
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.UserPreferences
import com.example.data.model.WeatherForecast
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GlassDashboardRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTasks(userId: String = requireUserId()): Flow<List<GlassTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GlassTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<GlassTask> { it.completed }
                            .thenBy { it.sortOrder }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePreferences(userId: String = requireUserId()): Flow<UserPreferences> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPreferences::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: UserPreferences(userId = userId)
                    } else {
                        UserPreferences(userId = userId)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserTasks(userId: String = requireUserId()): Result<List<GlassTask>> = runCatching {
        val path = "users/$userId/tasks"
        try {
            val snapshot = db.collection("users").document(userId).collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(
                GlassTask::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.sortOrder }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getTaskById(ownerUserId: String, taskId: String): Result<GlassTask?> = runCatching {
        val docRef = db.collection("users").document(ownerUserId).collection("tasks").document(taskId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(
                GlassTask::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createTask(
        title: String,
        notes: String = "",
        category: TaskCategory = TaskCategory.FOCUS,
        priority: TaskPriority = TaskPriority.MEDIUM,
        sortOrder: Int = 0,
        reminderLabel: String = "",
        reminderEnabled: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(140),
            "notes" to notes.trim().take(500),
            "category" to category.name,
            "priority" to priority.name,
            "completed" to false,
            "sortOrder" to sortOrder.coerceIn(0, 1000000),
            "reminderLabel" to reminderLabel.trim().take(80),
            "reminderEnabled" to reminderEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(task: GlassTask): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        val payload = mapOf(
            "title" to task.title.trim().take(140),
            "notes" to task.notes.trim().take(500),
            "category" to task.category,
            "priority" to task.priority,
            "completed" to task.completed,
            "sortOrder" to task.sortOrder.coerceIn(0L, 1000000L),
            "reminderLabel" to task.reminderLabel.trim().take(80),
            "reminderEnabled" to task.reminderEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTaskOrderBatch(orderedTasks: List<GlassTask>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val batch = db.batch()
        orderedTasks.forEachIndexed { index, task ->
            if (task.id.isNotBlank()) {
                val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
                batch.update(
                    docRef,
                    mapOf(
                        "sortOrder" to index.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/tasks")
            throw e
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun savePreferences(
        blurIntensity: Int,
        themeMode: GlassThemeMode,
        iconPack: CustomIconPack,
        parallaxEnabled: Boolean,
        city: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "blurIntensity" to blurIntensity.coerceIn(0, 100),
            "themeMode" to themeMode.name,
            "iconPack" to iconPack.name,
            "parallaxEnabled" to parallaxEnabled,
            "city" to city.trim().ifBlank { "San Francisco" }.take(80),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun seedStarterTasksIfEmpty(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val existing = getUserTasks(uid).getOrDefault(emptyList())
        if (existing.isNotEmpty()) return@runCatching

        val starterTasks = listOf(
            Triple(
                "Calibrate Liquid Glass Blur & 3D Parallax Clock",
                "Adjust the real-time frosted refraction slider and tilt the 3D spatial clock on the home screen.",
                TaskCategory.FOCUS to TaskPriority.HIGH
            ) to ("09:30 AM • Smart Focus" to true),
            Triple(
                "Long-Press & Drag to Reorder Priority Sprints",
                "Hold any glass task card to elevate and smoothly reorder your daily workflow at 90Hz+.",
                TaskCategory.WORK to TaskPriority.HIGH
            ) to ("11:00 AM • Sprint Sync" to true),
            Triple(
                "Run 90Hz+ RAM Boost & Thermal Battery Optimizer",
                "Open the System Care glass widget to purge background cache and inspect live battery voltage.",
                TaskCategory.SYSTEM to TaskPriority.MEDIUM
            ) to ("02:00 PM • Device Care" to true),
            Triple(
                "Swipe Left to Pixel Discover & Live Weather Feed",
                "Browse personalized tech briefings and hourly atmospheric forecasts in the left feed.",
                TaskCategory.PERSONAL to TaskPriority.LOW
            ) to ("05:15 PM • Evening Digest" to false)
        )

        starterTasks.forEachIndexed { index, (taskInfo, reminderInfo) ->
            createTask(
                title = taskInfo.first,
                notes = taskInfo.second,
                category = taskInfo.third.first,
                priority = taskInfo.third.second,
                sortOrder = index,
                reminderLabel = reminderInfo.first,
                reminderEnabled = reminderInfo.second
            )
        }
    }

    suspend fun fetchLiveWeather(city: String): WeatherForecast = withContext(Dispatchers.IO) {
        val coordinates = cityCoordinates(city)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${coordinates.first}&longitude=${coordinates.second}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&hourly=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        try {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body?.string() ?: error("Empty body")
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val temp = current.optDouble("temperature_2m", 21.0).toInt()
                val humidity = current.optInt("relative_humidity_2m", 54)
                val wind = current.optDouble("wind_speed_10m", 12.0).toInt()
                val code = current.optInt("weather_code", 1)

                val daily = json.getJSONObject("daily")
                val maxArr = daily.getJSONArray("temperature_2m_max")
                val minArr = daily.getJSONArray("temperature_2m_min")
                val high = if (maxArr.length() > 0) maxArr.getDouble(0).toInt() else temp + 4
                val low = if (minArr.length() > 0) minArr.getDouble(0).toInt() else temp - 5

                val hourlyObj = json.getJSONObject("hourly")
                val times = hourlyObj.getJSONArray("time")
                val temps = hourlyObj.getJSONArray("temperature_2m")
                val codes = hourlyObj.getJSONArray("weather_code")
                val hourlyItems = mutableListOf<HourlyWeatherItem>()
                val step = 3
                var idx = 6
                while (idx < times.length() && hourlyItems.size < 6) {
                    val rawTime = times.optString(idx, "12:00")
                    val hourPart = rawTime.substringAfter("T", "12:00").take(5)
                    hourlyItems.add(
                        HourlyWeatherItem(
                            hourLabel = hourPart,
                            tempCelsius = temps.optDouble(idx, temp.toDouble()).toInt(),
                            conditionCode = codes.optInt(idx, code)
                        )
                    )
                    idx += step
                }

                WeatherForecast(
                    city = city,
                    temperatureCelsius = temp,
                    highCelsius = high,
                    lowCelsius = low,
                    conditionText = weatherCodeToLabel(code),
                    windKmh = wind,
                    humidityPercent = humidity,
                    aqiLabel = "AQI 24 • Pristine",
                    hourly = hourlyItems,
                    isLiveNetwork = true
                )
            }
        } catch (e: Exception) {
            WeatherForecast(
                city = city,
                temperatureCelsius = 22,
                highCelsius = 25,
                lowCelsius = 16,
                conditionText = "Crystal Clear Sky",
                windKmh = 11,
                humidityPercent = 48,
                aqiLabel = "AQI 21 • Pristine",
                hourly = listOf(
                    HourlyWeatherItem("09:00", 19, 0),
                    HourlyWeatherItem("12:00", 23, 1),
                    HourlyWeatherItem("15:00", 25, 1),
                    HourlyWeatherItem("18:00", 21, 2),
                    HourlyWeatherItem("21:00", 18, 0)
                ),
                isLiveNetwork = false
            )
        }
    }

    suspend fun fetchDiscoverNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val topIdsReq = Request.Builder()
                .url("https://hacker-news.firebaseio.com/v0/topstories.json")
                .get()
                .build()
            val ids = httpClient.newCall(topIdsReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Failed")
                val arr = JSONArray(resp.body?.string() ?: "[]")
                (0 until minOf(5, arr.length())).map { arr.getLong(it) }
            }
            val articles = ids.mapIndexedNotNull { idx, id ->
                val itemReq = Request.Builder()
                    .url("https://hacker-news.firebaseio.com/v0/item/$id.json")
                    .get()
                    .build()
                httpClient.newCall(itemReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val obj = JSONObject(resp.body?.string() ?: return@use null)
                    val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return@use null
                    val by = obj.optString("by", "Tech Desk")
                    val score = obj.optInt("score", 120)
                    val url = obj.optString("url", "https://news.ycombinator.com/item?id=$id")
                    val domain = url.substringAfter("://").substringBefore("/").removePrefix("www.")
                    NewsArticle(
                        id = id.toString(),
                        title = title,
                        source = domain.ifBlank { "Pixel Pulse" },
                        category = when (idx % 3) {
                            0 -> "AI & Silicon"
                            1 -> "Spatial UI"
                            else -> "Mobile Systems"
                        },
                        summary = "Trending across developer & hardware feeds • $score pts by @$by with real-time community analysis.",
                        timeAgo = "${(idx + 1) * 14}m ago",
                        readMinutes = 3 + (idx % 4),
                        url = url
                    )
                }
            }
            if (articles.isNotEmpty()) articles else defaultCuratedNews()
        } catch (e: Exception) {
            defaultCuratedNews()
        }
    }

    private fun cityCoordinates(city: String): Pair<Double, Double> {
        return when (city.lowercase().trim()) {
            "new york" -> 40.7128 to -74.0060
            "tokyo" -> 35.6762 to 139.6503
            "london" -> 51.5072 to -0.1276
            "seoul" -> 37.5665 to 126.9780
            "singapore" -> 1.3521 to 103.8198
            "paris" -> 48.8566 to 2.3522
            else -> 37.7749 to -122.4194 // San Francisco default
        }
    }

    private fun weatherCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear Prismatic Sky"
        1, 2 -> "Partly Frosted Clouds"
        3 -> "Overcast Glass Layer"
        45, 48 -> "Mist & Vapor Haze"
        51, 53, 55, 61, 63, 65 -> "Liquid Rain Showers"
        71, 73, 75 -> "Crystalline Snowfall"
        95, 96, 99 -> "Electric Storm Front"
        else -> "Crisp Breeze"
    }

    private fun defaultCuratedNews(): List<NewsArticle> = listOf(
        NewsArticle(
            id = "curated_1",
            title = "Next-Gen Spatial Glass Interfaces Bring 120Hz Micro-Optics to Mobile Launchers",
            source = "DesignSystems Weekly",
            category = "Spatial UI",
            summary = "Ultra-lightweight GPU shader pipelines now render real-time specular borders and parallax depth with near-zero RAM overhead.",
            timeAgo = "12m ago",
            readMinutes = 4
        ),
        NewsArticle(
            id = "curated_2",
            title = "Adaptive Frame Pacing & Thermal Governor Tuning Extends OLED Battery Endurance by 28%",
            source = "Android Silicon Lab",
            category = "Hardware & Power",
            summary = "Smart scheduling aligns touch-boost intervals with display VSYNC to sustain 90fps-120fps fluidity while lowering millivolt draw.",
            timeAgo = "34m ago",
            readMinutes = 3
        ),
        NewsArticle(
            id = "curated_3",
            title = "Context-Aware Smart Reminders Replace Notification Clutter with Ambient Focus Windows",
            source = "Productivity Pulse",
            category = "Focus Science",
            summary = "Grouping high-cognitive tasks into 45-minute distraction-free glass sprints boosts daily completion rates by 41%.",
            timeAgo = "1h ago",
            readMinutes = 5
        ),
        NewsArticle(
            id = "curated_4",
            title = "Zero-Allocation Vector Icon Engines Outperform Bitmap Packs Across All Screen Densities",
            source = "Compose Craft",
            category = "Graphics",
            summary = "Procedural canvas and vector path rendering ensures razor-sharp iconography from compact phones to foldable tablets.",
            timeAgo = "2h ago",
            readMinutes = 3
        )
    )
}
