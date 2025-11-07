package com.example.kosmos.features.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Bottom sheet for selecting a parent task when creating subtasks
 * Stitch design: Navy background, search bar, task cards with status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPickerBottomSheet(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter tasks: only tasks without parents (can't nest infinitely)
    val availableTasks = remember(tasks) {
        tasks.filter { it.parentTaskId == null }
    }

    // Apply search filter
    val filteredTasks = remember(availableTasks, searchQuery) {
        if (searchQuery.isBlank()) {
            availableTasks
        } else {
            val query = searchQuery.trim().lowercase()
            availableTasks.filter { task ->
                task.title.lowercase().contains(query) ||
                (task.description?.lowercase()?.contains(query) == true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = ColorTokens.ReactTheme.card,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Title
            Text(
                text = "Select Parent Task",
                style = MaterialTheme.typography.titleLarge,
                color = ColorTokens.ReactTheme.foreground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search tasks...",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ColorTokens.ReactTheme.foreground,
                    unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                    focusedBorderColor = ColorTokens.ReactTheme.primary,
                    unfocusedBorderColor = ColorTokens.ReactTheme.border,
                    cursorColor = ColorTokens.ReactTheme.primary,
                    focusedContainerColor = ColorTokens.ReactTheme.background,
                    unfocusedContainerColor = ColorTokens.ReactTheme.background
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task list
            if (filteredTasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No tasks available" else "No tasks found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskPickerItem(
                            task = task,
                            onClick = { onTaskSelected(task) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single task item in the picker
 */
@Composable
private fun TaskPickerItem(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Task title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorTokens.ReactTheme.foreground,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Status and priority badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    StatusChip(status = task.status)

                    // Priority badge
                    PriorityChip(priority = task.priority)
                }
            }
        }
    }
}

/**
 * Status chip for task
 */
@Composable
private fun StatusChip(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        TaskStatus.TODO -> Pair(ColorTokens.ReactTheme.mutedForeground, "To Do")
        TaskStatus.IN_PROGRESS -> Pair(ColorTokens.ReactTheme.primary, "In Progress")
        TaskStatus.DONE -> Pair(ColorTokens.Status.online, "Done")
        TaskStatus.CANCELLED -> Pair(ColorTokens.ReactTheme.mutedForeground, "Cancelled")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Priority chip for task
 */
@Composable
private fun PriorityChip(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (priority) {
        TaskPriority.URGENT -> Pair(Color(0xFFD32F2F), "URGENT")
        TaskPriority.HIGH -> Pair(ColorTokens.ReactTheme.destructive, "HIGH")
        TaskPriority.MEDIUM -> Pair(ColorTokens.Priority.medium, "MEDIUM")
        TaskPriority.LOW -> Pair(ColorTokens.ReactTheme.primary, "LOW")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
