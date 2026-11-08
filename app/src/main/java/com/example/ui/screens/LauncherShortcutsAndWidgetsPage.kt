package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.UiState

/**
 * Secondary launcher page (Right Page / Page 2) dedicated to user widgets,
 * Focus Tasks, Smart Reminders, RAM Clean Booster, and battery tools.
 */
@Composable
fun LauncherShortcutsAndWidgetsPage(
    tasksUiState: UiState<List<GlassTask>>,
    selectedCategory: TaskCategory?,
    telemetry: SystemTelemetry,
    lastRamBoostSummary: String,
    onSelectCategory: (TaskCategory?) -> Unit,
    onToggleTaskComplete: (GlassTask) -> Unit,
    onToggleTaskReminder: (GlassTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onCommitReorderedTasks: (List<GlassTask>) -> Unit,
    onQuickRamClean: () -> Unit,
    onOpenAddTaskSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    val rawTasks = (tasksUiState as? UiState.Success)?.data.orEmpty()
    val filteredTasks = remember(rawTasks, selectedCategory) {
        if (selectedCategory == null) rawTasks
        else rawTasks.filter { it.category == selectedCategory.name }
    }

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

    val ramPct = (telemetry.ramUsageFraction * 100).toInt()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val isNarrow = maxWidth < 380.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
                .testTag("launcher_widgets_page"),
            contentPadding = PaddingValues(
                horizontal = if (isNarrow) 12.dp else 20.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isNarrow) 12.dp else 16.dp)
        ) {
            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Widgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "System Tools & Focus Tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.accentPrimary.copy(alpha = 0.18f))
                            .clickable(onClick = onOpenSettingsSheet)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accentPrimary
                        )
                    }
                }
            }

            // Quick RAM & Battery Telemetry Widget
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CustomGlassIcon(
                                    glyph = AeroIconGlyph.RAM_BOOST,
                                    size = 24.dp,
                                    tint = tokens.accentPrimary
                                )
                                Column {
                                    Text(
                                        text = "RAM Optimizer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.textPrimary
                                    )
                                    Text(
                                        text = "${telemetry.usedRamMb} MB / ${telemetry.totalRamMb} MB ($ramPct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.accentPrimary)
                                    .clickable(onClick = onQuickRamClean)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("quick_ram_clean_btn")
                            ) {
                                Text(
                                    text = "Boost RAM",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF041326)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { telemetry.ramUsageFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (ramPct > 80) RoseAlert else tokens.accentPrimary,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )

                        if (lastRamBoostSummary.isNotBlank()) {
                            Text(
                                text = lastRamBoostSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.accentSecondary
                            )
                        }
                    }
                }
            }

            // Task Management Header & Category Filter
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Focus Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            if (activeRemindersCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(tokens.accentPrimary.copy(alpha = 0.22f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$activeRemindersCount active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onOpenAddTaskSheet,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tokens.accentPrimary)
                                .testTag("add_task_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = Color(0xFF041326),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Category Pill Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryPill(
                            label = "All",
                            count = rawTasks.size,
                            isSelected = selectedCategory == null,
                            onClick = { onSelectCategory(null) }
                        )
                        TaskCategory.values().forEach { cat ->
                            val catCount = rawTasks.count { it.category == cat.name }
                            CategoryPill(
                                label = cat.label,
                                count = catCount,
                                isSelected = selectedCategory == cat,
                                onClick = { onSelectCategory(cat) }
                            )
                        }
                    }
                }
            }

            // Draggable Tasks List
            when (tasksUiState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = tokens.accentPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Text(
                                text = "Unable to load cloud tasks: ${tasksUiState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RoseAlert,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (draggableTasks.isEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomGlassIcon(
                                        glyph = AeroIconGlyph.DASHBOARD,
                                        size = 36.dp,
                                        tint = tokens.textSecondary
                                    )
                                    Text(
                                        text = "No focus tasks yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textSecondary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.accentPrimary.copy(alpha = 0.22f))
                                            .clickable(onClick = onOpenAddTaskSheet)
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "+ Create First Task",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.accentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = draggableTasks,
                            key = { _, task -> task.id }
                        ) { index, task ->
                            val isBeingDragged = draggedIndex == index
                            val itemOffsetY by animateFloatAsState(
                                targetValue = if (isBeingDragged) dragOffsetY else 0f,
                                label = "task_drag_offset"
                            )

                            DraggableGlassTaskCard(
                                task = task,
                                isDragged = isBeingDragged,
                                offsetY = itemOffsetY,
                                onToggleComplete = { onToggleTaskComplete(task) },
                                onToggleReminder = { onToggleTaskReminder(task) },
                                onDelete = { onDeleteTask(task.id) },
                                onMoveUp = {
                                    if (index > 0) {
                                        val mutableList = draggableTasks.toMutableList()
                                        val temp = mutableList[index]
                                        mutableList[index] = mutableList[index - 1]
                                        mutableList[index - 1] = temp
                                        onCommitReorderedTasks(mutableList)
                                    }
                                },
                                onMoveDown = {
                                    if (index < draggableTasks.lastIndex) {
                                        val mutableList = draggableTasks.toMutableList()
                                        val temp = mutableList[index]
                                        mutableList[index] = mutableList[index + 1]
                                        mutableList[index + 1] = temp
                                        onCommitReorderedTasks(mutableList)
                                    }
                                },
                                canMoveUp = index > 0,
                                canMoveDown = index < draggableTasks.lastIndex,
                                modifier = Modifier
                                    .zIndex(if (isBeingDragged) 10f else 1f)
                                    .pointerInput(task.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragOffsetY = 0f
                                                runCatching {
                                                    val vibrator = context.getSystemService(Context_Vibrator(context)) as? Vibrator
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator?.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator?.vibrate(30L)
                                                    }
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val threshold = 90f
                                                if (dragOffsetY > threshold && draggedIndex < draggableTasks.lastIndex) {
                                                    val currentIndex = draggedIndex
                                                    val nextIndex = currentIndex + 1
                                                    val item = draggableTasks.removeAt(currentIndex)
                                                    draggableTasks.add(nextIndex, item)
                                                    draggedIndex = nextIndex
                                                    dragOffsetY -= threshold
                                                } else if (dragOffsetY < -threshold && draggedIndex > 0) {
                                                    val currentIndex = draggedIndex
                                                    val prevIndex = currentIndex - 1
                                                    val item = draggableTasks.removeAt(currentIndex)
                                                    draggableTasks.add(prevIndex, item)
                                                    draggedIndex = prevIndex
                                                    dragOffsetY += threshold
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = -1
                                                dragOffsetY = 0f
                                                onCommitReorderedTasks(draggableTasks.toList())
                                            },
                                            onDragCancel = {
                                                draggedIndex = -1
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            // Bottom Spacing for fixed dock
            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

private fun Context_Vibrator(context: android.content.Context): String = android.content.Context.VIBRATOR_SERVICE

@Composable
private fun CategoryPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) tokens.accentPrimary.copy(alpha = 0.28f)
                else Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.45f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) tokens.accentPrimary else tokens.textSecondary
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) tokens.accentPrimary else tokens.textSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DraggableGlassTaskCard(
    task: GlassTask,
    isDragged: Boolean,
    offsetY: Float,
    onToggleComplete: () -> Unit,
    onToggleReminder: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val priority = runCatching { TaskPriority.valueOf(task.priority) }.getOrDefault(TaskPriority.MEDIUM)
    val priorityColor = when (priority) {
        TaskPriority.HIGH -> RoseAlert
        TaskPriority.MEDIUM -> AmberGlow
        TaskPriority.LOW -> EmeraldPulse
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetY
                scaleX = if (isDragged) 1.03f else 1f
                scaleY = if (isDragged) 1.03f else 1f
                alpha = if (task.completed) 0.65f else 1f
            },
        cornerRadius = 20.dp,
        elevated = isDragged
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Complete Checkbox Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.completed) EmeraldPulse
                        else Color.White.copy(alpha = 0.12f)
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title & Category
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Reminder toggle button
            IconButton(
                onClick = onToggleReminder,
                modifier = Modifier.size(32.dp)
            ) {
                CustomGlassIcon(
                    glyph = AeroIconGlyph.SMART_REMINDER,
                    size = 18.dp,
                    tint = if (task.reminderEnabled) tokens.accentPrimary else tokens.textSecondary.copy(alpha = 0.4f)
                )
            }

            // Manual reorder arrows
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (canMoveUp) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = tokens.textSecondary
                        )
                    }
                }
                if (canMoveDown) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = tokens.textSecondary
                        )
                    }
                }
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Task",
                    tint = RoseAlert.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
