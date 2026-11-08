package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.ui.theme.LocalGlassTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskModalSheet(
    onDismiss: () -> Unit,
    onCreateTask: (
        title: String,
        notes: String,
        category: TaskCategory,
        priority: TaskPriority,
        reminderLabel: String,
        reminderEnabled: Boolean
    ) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.FOCUS) }
    var priority by remember { mutableStateOf(TaskPriority.HIGH) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderLabel by remember { mutableStateOf("09:30 AM • Smart Focus") }

    val presetTimes = listOf(
        "09:30 AM • Smart Focus",
        "12:00 PM • Midday Sprint",
        "03:30 PM • Deep Work",
        "07:00 PM • Evening Wrap"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.bgGradientEnd,
        contentColor = tokens.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Create Liquid Glass Task",
                style = MaterialTheme.typography.headlineMedium,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                placeholder = { Text("e.g., Ship 120Hz Glass Widget") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.accentPrimary,
                    unfocusedBorderColor = tokens.borderHighlight,
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    focusedLabelColor = tokens.accentPrimary,
                    unfocusedLabelColor = tokens.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Context (Optional)") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.accentPrimary,
                    unfocusedBorderColor = tokens.borderHighlight,
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    focusedLabelColor = tokens.accentPrimary,
                    unfocusedLabelColor = tokens.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_task_notes_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.accentPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskCategory.entries.forEach { cat ->
                    val selected = cat == category
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) tokens.accentPrimary else tokens.cardFillTop)
                            .clickable { category = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color(0xFF041326) else tokens.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "PRIORITY",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.accentPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { prio ->
                    val selected = prio == priority
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) tokens.accentSecondary else tokens.cardFillTop)
                            .clickable { priority = prio }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prio.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else tokens.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Built-in Smart Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Schedule distraction-free notification alert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = tokens.accentPrimary,
                        checkedTrackColor = tokens.accentPrimary.copy(alpha = 0.35f)
                    )
                )
            }

            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTimes.forEach { preset ->
                        val selected = reminderLabel == preset
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) tokens.accentPrimary else tokens.cardFillTop)
                                .clickable { reminderLabel = preset }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color(0xFF041326) else tokens.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateTask(
                            title,
                            notes,
                            category,
                            priority,
                            if (reminderEnabled) reminderLabel else "",
                            reminderEnabled
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accentPrimary,
                    contentColor = Color(0xFF041326)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_create_task_button")
            ) {
                Text(
                    text = "Add to Glass Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
