package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.GlassTask
import com.example.data.model.SystemTelemetry
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.WeatherForecast
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.components.Spatial3DParallaxClockCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.UiState

@Composable
fun DashboardHomePage(
    tasksUiState: UiState<List<GlassTask>>,
    selectedCategory: TaskCategory?,
    weather: WeatherForecast?,
    telemetry: SystemTelemetry,
    parallaxEnabled: Boolean,
    lastRamBoostSummary: String,
    onSelectCategory: (TaskCategory?) -> Unit,
    onToggleTaskComplete: (GlassTask) -> Unit,
    onToggleTaskReminder: (GlassTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onCommitReorderedTasks: (List<GlassTask>) -> Unit,
    onQuickRamClean: () -> Unit,
    onOpenDiscoverFeed: () -> Unit,
    onOpenOptimizerPage: () -> Unit,
    onOpenAddTaskSheet: () -> Unit,
    onOpenAppDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    val rawTasks = (tasksUiState as? UiState.Success)?.data.orEmpty()
    val filteredTasks = remember(rawTasks, selectedCategory) {
        if (selectedCategory == null) rawTasks
        else rawTasks.filter { it.category == selectedCategory.name }
    }

    // Local mutable snapshot list for zero-latency 90fps drag-and-drop reordering
    val draggableTasks = remember { mutableStateListOf<GlassTask>() }
    LaunchedEffect(filteredTasks) {
        draggableTasks.clear()
        draggableTasks.addAll(filteredTasks)
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val activeRemindersCount = remember(rawTasks) {
        rawTasks.count { it.reminderEnabled && !it.completed }
    }

    var installedAppsPreview by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isDefaultHome by remember { mutableStateOf(isAeroGlassDefaultLauncher(context)) }

    LaunchedEffect(Unit) {
        installedAppsPreview = fetchInstalledApps(context).take(8)
        isDefaultHome = isAeroGlassDefaultLauncher(context)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val isNarrow = maxWidth < 380.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
                .testTag("dashboard_home_page"),
            contentPadding = PaddingValues(
                horizontal = if (isNarrow) 12.dp else 20.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isNarrow) 12.dp else 16.dp)
        ) {
        // Default Home Launcher Banner
        if (!isDefaultHome) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDefaultHomeSettings(context) }
                        .testTag("home_default_launcher_chip"),
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.HOME_LAUNCHER,
                                size = 22.dp,
                                tint = tokens.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AeroGlass Home Launcher",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = "Tap to set as your default Android home",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.textSecondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.accentPrimary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "SET HOME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF041326)
                            )
                        }
                    }
                }
            }
        }

        // 1. Main Home 3D Spatial Parallax Clock
        item {
            Spatial3DParallaxClockCard(
                parallaxEnabled = parallaxEnabled,
                refreshRateHz = telemetry.displayRefreshRateHz,
                activeRemindersCount = activeRemindersCount,
                weatherTempCelsius = weather?.temperatureCelsius,
                weatherCondition = weather?.conditionText,
                modifier = Modifier.testTag("spatial_3d_clock_card")
            )
        }

        // 2. Launcher Quick Apps Bar (Phone, Messages, Camera, All Apps, Settings)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_quick_apps_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK APPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentPrimary
                        )
                        Text(
                            text = "All Apps (${installedAppsPreview.count()}+) →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accentSecondary,
                            modifier = Modifier.clickable(onClick = onOpenAppDrawer)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HomeAppLauncherShortcut(
                            glyph = AeroIconGlyph.PHONE,
                            label = "Phone",
                            tint = EmeraldPulse,
                            onClick = { launchPhone(context) }
                        )
                        HomeAppLauncherShortcut(
                            glyph = AeroIconGlyph.MESSAGES,
                            label = "Messages",
                            tint = tokens.accentPrimary,
                            onClick = { launchMessages(context) }
                        )
                        HomeAppLauncherShortcut(
                            glyph = AeroIconGlyph.CAMERA,
                            label = "Camera",
                            tint = AmberGlow,
                            onClick = { launchCamera(context) }
                        )
                        HomeAppLauncherShortcut(
                            glyph = AeroIconGlyph.APPS_DRAWER,
                            label = "All Apps",
                            tint = tokens.accentSecondary,
                            onClick = onOpenAppDrawer
                        )
                        HomeAppLauncherShortcut(
                            glyph = AeroIconGlyph.SETTINGS,
                            label = "Settings",
                            tint = tokens.textPrimary,
                            onClick = { openDefaultHomeSettings(context) }
                        )
                    }

                    if (installedAppsPreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            for (app in installedAppsPreview) {
                                HomeInstalledAppMiniBadge(
                                    app = app,
                                    onClick = { launchApp(context, app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Dual Glass Widgets Row: Home Weather Widget + 1-Tap 90fps RAM & Battery Widget
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Weather Mini Widget (opens left Discover feed on tap)
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_weather_widget"),
                    cornerRadius = 24.dp,
                    onClick = onOpenDiscoverFeed
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.WEATHER_SUN_CLOUD,
                                size = 24.dp,
                                tint = tokens.accentPrimary,
                                secondaryTint = tokens.accentSecondary
                            )
                            Text(
                                text = "PIXEL SKY",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.accentPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${weather?.temperatureCelsius ?: 21}°C",
                            style = MaterialTheme.typography.headlineLarge,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = weather?.city ?: "San Francisco",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = weather?.conditionText ?: "Clear Sky • Swipe Left",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 1-Tap RAM Clean & Battery Health Widget
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_ram_clean_widget"),
                    cornerRadius = 24.dp,
                    onClick = {
                        triggerSubtleHaptic(context)
                        onQuickRamClean()
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomGlassIcon(
                                glyph = AeroIconGlyph.RAM_BOOST,
                                size = 24.dp,
                                tint = EmeraldPulse,
                                secondaryTint = tokens.accentPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmeraldPulse.copy(alpha = 0.20f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "TAP BOOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPulse
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(telemetry.ramUsageFraction * 100).toInt()}% RAM",
                            style = MaterialTheme.typography.headlineLarge,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Battery ${telemetry.batteryPercent}% • ${telemetry.batteryTempCelsius}°C",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { telemetry.ramUsageFraction },
                            color = EmeraldPulse,
                            trackColor = tokens.cardFillBottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        // 3. Built-in Smart Reminder System Status Bar
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_reminder_summary_card"),
                cornerRadius = 22.dp,
                onClick = onOpenOptimizerPage
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.SMART_REMINDER,
                            size = 26.dp,
                            tint = tokens.accentPrimary,
                            secondaryTint = tokens.accentSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Reminder & Frame Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = lastRamBoostSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.accentPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${telemetry.estimatedHoursRemaining} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accentPrimary
                        )
                    }
                }
            }
        }

        // 4. Task Header + Category Filter Chips + Add Task Button
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Priority Glass Tasks",
                            style = MaterialTheme.typography.headlineMedium,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Long-press & drag handle or tap arrows to reorder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }

                    GlassCard(
                        cornerRadius = 18.dp,
                        elevated = true,
                        onClick = onOpenAddTaskSheet,
                        modifier = Modifier.testTag("add_task_header_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add new task",
                                tint = tokens.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Task",
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipPill(
                        label = "All (${rawTasks.size})",
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        testTag = "filter_chip_all"
                    )
                    TaskCategory.entries.forEach { category ->
                        FilterChipPill(
                            label = category.label,
                            selected = selectedCategory == category,
                            onClick = { onSelectCategory(category) },
                            testTag = "filter_chip_${category.name.lowercase()}"
                        )
                    }
                }
            }
        }

        // 5. Smooth Drag-and-Drop Task Cards
        when (tasksUiState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = tokens.accentPrimary)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = tasksUiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseAlert,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (draggableTasks.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_tasks_card"),
                            cornerRadius = 26.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.TASKS,
                                    size = 42.dp,
                                    tint = tokens.accentPrimary,
                                    secondaryTint = tokens.accentSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Crystal Clear Focus Queue",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tokens.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'New Task' to add a glass task card with smart reminders.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = draggableTasks,
                        key = { _, item -> item.id.ifBlank { item.title } }
                    ) { index, task ->
                        val isDragging = index == draggedIndex
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            label = "dragScale"
                        )

                        DraggableGlassTaskItem(
                            task = task,
                            index = index,
                            totalCount = draggableTasks.size,
                            isDragging = isDragging,
                            dragTranslationY = if (isDragging) dragOffsetY else 0f,
                            scale = scale,
                            onDragStart = {
                                triggerSubtleHaptic(context)
                                draggedIndex = index
                                dragOffsetY = 0f
                            },
                            onDragDelta = { deltaY ->
                                dragOffsetY += deltaY
                                val thresholdPx = 135f
                                if (dragOffsetY > thresholdPx && draggedIndex < draggableTasks.lastIndex) {
                                    val targetIdx = draggedIndex + 1
                                    val moved = draggableTasks.removeAt(draggedIndex)
                                    draggableTasks.add(targetIdx, moved)
                                    draggedIndex = targetIdx
                                    dragOffsetY = 0f
                                    triggerSubtleHaptic(context)
                                } else if (dragOffsetY < -thresholdPx && draggedIndex > 0) {
                                    val targetIdx = draggedIndex - 1
                                    val moved = draggableTasks.removeAt(draggedIndex)
                                    draggableTasks.add(targetIdx, moved)
                                    draggedIndex = targetIdx
                                    dragOffsetY = 0f
                                    triggerSubtleHaptic(context)
                                }
                            },
                            onDragEnd = {
                                if (draggedIndex != -1) {
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                    onCommitReorderedTasks(draggableTasks.toList())
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val moved = draggableTasks.removeAt(index)
                                    draggableTasks.add(index - 1, moved)
                                    triggerSubtleHaptic(context)
                                    onCommitReorderedTasks(draggableTasks.toList())
                                }
                            },
                            onMoveDown = {
                                if (index < draggableTasks.lastIndex) {
                                    val moved = draggableTasks.removeAt(index)
                                    draggableTasks.add(index + 1, moved)
                                    triggerSubtleHaptic(context)
                                    onCommitReorderedTasks(draggableTasks.toList())
                                }
                            },
                            onToggleComplete = { onToggleTaskComplete(task) },
                            onToggleReminder = { onToggleTaskReminder(task) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) tokens.accentPrimary else tokens.cardFillTop)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF041326) else tokens.textPrimary
        )
    }
}

@Composable
private fun DraggableGlassTaskItem(
    task: GlassTask,
    index: Int,
    totalCount: Int,
    isDragging: Boolean,
    dragTranslationY: Float,
    scale: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH.name -> RoseAlert
        TaskPriority.MEDIUM.name -> AmberGlow
        else -> EmeraldPulse
    }
    val categoryLabel = runCatching { TaskCategory.valueOf(task.category).label }.getOrDefault(task.category)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragTranslationY
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    }
                )
            }
            .testTag("task_item_card_${task.id}"),
        cornerRadius = 24.dp,
        elevated = isDragging
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag Handle Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tokens.cardFillBottom),
                    contentAlignment = Alignment.Center
                ) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.DRAG_HANDLE,
                        size = 22.dp,
                        tint = tokens.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Checkbox Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleComplete)
                        .testTag("task_complete_button_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.CHECK_COMPLETE,
                            size = 28.dp,
                            tint = EmeraldPulse,
                            secondaryTint = tokens.accentPrimary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tokens.accentPrimary.copy(alpha = 0.22f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(priorityColor.copy(alpha = 0.20f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            )
                        }
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accentPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.completed) tokens.textSecondary else tokens.textPrimary,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )

                    AnimatedVisibility(visible = task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Bar: Smart Reminder Toggle + Up/Down Reorder + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (task.reminderEnabled) tokens.accentPrimary.copy(alpha = 0.18f)
                            else tokens.cardFillBottom
                        )
                        .clickable(onClick = onToggleReminder)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("task_reminder_toggle_${task.id}")
                ) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.SMART_REMINDER,
                        size = 16.dp,
                        tint = if (task.reminderEnabled) tokens.accentPrimary else tokens.textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (task.reminderEnabled) {
                            task.reminderLabel.ifBlank { "Smart Alert Armed" }
                        } else {
                            "Arm Smart Reminder"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.reminderEnabled) tokens.accentPrimary else tokens.textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("task_move_up_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move task up",
                            tint = if (index > 0) tokens.textPrimary else tokens.textSecondary.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("task_move_down_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move task down",
                            tint = if (index < totalCount - 1) tokens.textPrimary else tokens.textSecondary.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("task_delete_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = RoseAlert.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

private fun triggerSubtleHaptic(context: android.content.Context) {
    try {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18L)
        }
    } catch (ignored: Exception) {
    }
}

@Composable
private fun HomeAppLauncherShortcut(
    glyph: AeroIconGlyph,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            CustomGlassIcon(
                glyph = glyph,
                size = 24.dp,
                tint = tint,
                secondaryTint = tokens.accentSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textPrimary
        )
    }
}

@Composable
private fun HomeInstalledAppMiniBadge(
    app: InstalledAppItem,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val appBitmap = remember(app.packageName) { app.icon?.toBitmapOrNull(100, 100) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = if (tokens.isDark) 0.10f else 0.30f))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (appBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = appBitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
