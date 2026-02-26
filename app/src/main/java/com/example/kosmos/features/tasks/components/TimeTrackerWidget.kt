package com.example.kosmos.features.tasks.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TimeEntry
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Time Tracker Widget
 *
 * Complete time tracking widget with:
 * - Active timer display with live countdown
 * - Time summary cards (tracked, estimated, remaining)
 * - Time entries list (last 5)
 * - Start/Stop button
 * - Add manual entry button
 *
 * Usage:
 * ```kotlin
 * TimeTrackerWidget(
 *     task = task,
 *     runningTimer = runningTimer,
 *     timeEntries = timeEntries,
 *     onStartTimer = { viewModel.startTimer() },
 *     onStopTimer = { viewModel.stopTimer() },
 *     onAddManualEntry = { viewModel.showManualEntryDialog() },
 *     onViewAllEntries = { navController.navigate(...) }
 * )
 * ```
 */
@Composable
fun TimeTrackerWidget(
    task: Task,
    runningTimer: TimeEntry?,
    timeEntries: List<TimeEntry>,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onAddManualEntry: () -> Unit,
    onViewAllEntries: () -> Unit,
    onDeleteEntry: (TimeEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ColorTokens.ReactTheme.card.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.mutedForeground,
                shape = MaterialTheme.shapes.large
            )
            .padding(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        // Header
        TimeTrackerHeader()

        // Active Timer Display (if running)
        AnimatedVisibility(visible = runningTimer != null) {
            if (runningTimer != null) {
                ActiveTimerDisplay(timer = runningTimer)
            }
        }

        // Time Summary Cards
        TimeSummaryCards(
            trackedHours = task.actualHours ?: 0f,
            estimatedHours = task.estimatedHours ?: 0f
        )

        // Controls
        TimeTrackerControls(
            isRunning = runningTimer != null,
            onStartTimer = onStartTimer,
            onStopTimer = onStopTimer,
            onAddManualEntry = onAddManualEntry
        )

        // Time Entries List
        if (timeEntries.isNotEmpty()) {
            TimeEntriesList(
                entries = timeEntries.take(5),
                onViewAll = onViewAllEntries,
                onDeleteEntry = onDeleteEntry,
                hasMore = timeEntries.size > 5
            )
        }
    }
}

/**
 * Header with icon and title
 */
@Composable
private fun TimeTrackerHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Time tracking",
            tint = ColorTokens.ReactTheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = "Time Tracking",
            style = TypographyTokens.typography.titleMedium,
            color = ColorTokens.ReactTheme.foreground,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Active Timer Display with live countdown
 */
@Composable
private fun ActiveTimerDisplay(timer: TimeEntry) {
    // Live duration update every second
    var currentDuration by remember { mutableStateOf(timer.calculateDuration()) }

    LaunchedEffect(timer.id) {
        while (true) {
            delay(1000)
            currentDuration = timer.calculateDuration()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.primary,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing dot
                PulsingTimerDot()

                Column {
                    Text(
                        text = "Timer Running",
                        style = TypographyTokens.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    if (timer.description != null) {
                        Text(
                            text = timer.description,
                            style = TypographyTokens.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Live duration
            Text(
                text = formatDuration(currentDuration),
                style = TypographyTokens.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Pulsing dot indicator
 */
@Composable
private fun PulsingTimerDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(ColorTokens.ReactTheme.primary.copy(alpha = alpha))
    )
}

/**
 * Time Summary Cards (Tracked, Estimated, Remaining)
 */
@Composable
private fun TimeSummaryCards(
    trackedHours: Float,
    estimatedHours: Float
) {
    val remainingHours = estimatedHours - trackedHours

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Tracked Time
        TimeSummaryCard(
            label = "Tracked",
            hours = trackedHours,
            icon = Icons.Default.CheckCircle,
            color = ColorTokens.ReactTheme.primary,
            modifier = Modifier.weight(1f)
        )

        // Estimated Time
        TimeSummaryCard(
            label = "Estimated",
            hours = estimatedHours,
            icon = Icons.Default.Schedule,
            color = ColorTokens.Priority.medium,
            modifier = Modifier.weight(1f)
        )

        // Remaining Time
        TimeSummaryCard(
            label = "Remaining",
            hours = remainingHours,
            icon = Icons.Default.HourglassEmpty,
            color = if (remainingHours < 0) ColorTokens.ReactTheme.destructive else ColorTokens.Status.online,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single summary card
 */
@Composable
private fun TimeSummaryCard(
    label: String,
    hours: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = ColorTokens.ReactTheme.card,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.mutedForeground)
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = String.format("%.1fh", hours),
                style = TypographyTokens.typography.titleMedium,
                color = ColorTokens.ReactTheme.foreground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = TypographyTokens.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

/**
 * Controls (Start/Stop, Add Manual Entry)
 */
@Composable
private fun TimeTrackerControls(
    isRunning: Boolean,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onAddManualEntry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Start/Stop Button
        if (isRunning) {
            Button(
                onClick = onStopTimer,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.destructive
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop timer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                Text("Stop Timer")
            }
        } else {
            Button(
                onClick = onStartTimer,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start timer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                Text("Start Timer")
            }
        }

        // Add Manual Entry
        OutlinedButton(
            onClick = onAddManualEntry,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add manual entry",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
            Text("Add Entry")
        }
    }
}

/**
 * Time Entries List (last 5)
 */
@Composable
private fun TimeEntriesList(
    entries: List<TimeEntry>,
    onViewAll: () -> Unit,
    onDeleteEntry: (TimeEntry) -> Unit,
    hasMore: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Entries",
                style = TypographyTokens.typography.titleSmall,
                color = ColorTokens.ReactTheme.foreground,
                fontWeight = FontWeight.Bold
            )

            if (hasMore) {
                TextButton(onClick = onViewAll) {
                    Text("View All")
                }
            }
        }

        // Entries
        entries.forEach { entry ->
            TimeEntryListItem(
                entry = entry,
                onDelete = { onDeleteEntry(entry) }
            )
        }
    }
}

/**
 * Single time entry list item
 */
@Composable
private fun TimeEntryListItem(
    entry: TimeEntry,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = ColorTokens.ReactTheme.card,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.mutedForeground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                // Duration
                Text(
                    text = entry.formatDurationHumanReadable(),
                    style = TypographyTokens.typography.titleSmall,
                    color = ColorTokens.ReactTheme.foreground,
                    fontWeight = FontWeight.Bold
                )

                // Time range
                Text(
                    text = formatTimeRange(entry.startTime, entry.endTime),
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                // Description
                if (entry.description != null) {
                    Text(
                        text = entry.description,
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }

                // Billable indicator
                if (entry.isBillable) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Billable",
                            tint = ColorTokens.Status.online,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Billable",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.Status.online
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = ColorTokens.ReactTheme.destructive
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Time Entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = ColorTokens.ReactTheme.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Format duration in seconds to HH:MM:SS
 */
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

/**
 * Format time range
 */
private fun formatTimeRange(startTime: Long, endTime: Long?): String {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val start = dateFormat.format(Date(startTime))

    return if (endTime != null) {
        val end = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))
        "$start - $end"
    } else {
        "$start - Running"
    }
}
