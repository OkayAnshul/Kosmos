package com.example.kosmos.features.tasks.components
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Add Manual Time Entry Dialog
 *
 * Allows users to manually add a time entry with:
 * - Start/end date and time pickers
 * - Automatic duration calculation
 * - Description field
 * - Billable toggle
 * - Hourly rate input
 *
 * Usage:
 * ```kotlin
 * AddManualTimeEntryDialog(
 *     isVisible = showDialog,
 *     onConfirm = { startTime, endTime, description, isBillable, hourlyRate ->
 *         viewModel.addManualEntry(...)
 *     },
 *     onDismiss = { /* close */ }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualTimeEntryDialog(
    isVisible: Boolean,
    onConfirm: (
        startTime: Long,
        endTime: Long,
        description: String?,
        isBillable: Boolean,
        hourlyRate: Float?
    ) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis() + 3600_000) } // +1 hour
    var description by remember { mutableStateOf("") }
    var isBillable by remember { mutableStateOf(true) }
    var hourlyRate by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Calculate duration
    val durationSeconds = remember(startDate, endDate) {
        if (endDate > startDate) {
            ((endDate - startDate) / 1000).toInt()
        } else {
            0
        }
    }

    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = KosmosDialogDefaults.shape,
                color = ColorTokens.ReactTheme.card,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                ) {
                    // Header
                    DialogHeader()

                    // Start Date/Time
                    DateTimeSelector(
                        label = "Start Time",
                        timestamp = startDate,
                        onTimestampChange = { startDate = it }
                    )

                    // End Date/Time
                    DateTimeSelector(
                        label = "End Time",
                        timestamp = endDate,
                        onTimestampChange = { endDate = it }
                    )

                    // Duration Display
                    DurationDisplay(durationSeconds = durationSeconds)

                    // Error message
                    if (showError && durationSeconds <= 0) {
                        Text(
                            text = "End time must be after start time",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.ReactTheme.destructive
                        )
                    }

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = KosmosDialogDefaults.textFieldColors(),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md)
                    )

                    // Billable Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Billable",
                            style = TypographyTokens.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.foreground
                        )

                        Switch(
                            checked = isBillable,
                            onCheckedChange = { isBillable = it }
                        )
                    }

                    // Hourly Rate (if billable)
                    if (isBillable) {
                        OutlinedTextField(
                            value = hourlyRate,
                            onValueChange = { hourlyRate = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Hourly Rate (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("$") },
                            singleLine = true,
                            colors = KosmosDialogDefaults.textFieldColors(),
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (durationSeconds > 0) {
                                    val parsedHourlyRate = hourlyRate.toFloatOrNull()
                                    onConfirm(
                                        startDate,
                                        endDate,
                                        description.takeIf { it.isNotBlank() },
                                        isBillable,
                                        parsedHourlyRate
                                    )
                                    onDismiss()
                                } else {
                                    showError = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorTokens.ReactTheme.primary
                            )
                        ) {
                            Text("Add Entry")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog Header
 */
@Composable
private fun DialogHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Add manual entry",
            tint = ColorTokens.ReactTheme.primary,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = "Add Manual Time Entry",
            style = TypographyTokens.typography.titleLarge,
            color = ColorTokens.ReactTheme.foreground,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Date/Time Selector
 * Note: This is a simplified version using timestamp display
 * In production, would use proper DatePicker and TimePicker dialogs
 */
@Composable
private fun DateTimeSelector(
    label: String,
    timestamp: Long,
    onTimestampChange: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    OutlinedButton(
        onClick = {
            // In production, would show DatePickerDialog + TimePickerDialog
            // For now, this is a placeholder
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(Tokens.Spacing.sm))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = TypographyTokens.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )

            Text(
                text = dateFormat.format(Date(timestamp)),
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.foreground
            )
        }
    }
}

/**
 * Duration Display
 */
@Composable
private fun DurationDisplay(durationSeconds: Int) {
    if (durationSeconds > 0) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = ColorTokens.ReactTheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration:",
                    style = TypographyTokens.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatDuration(durationSeconds),
                    style = TypographyTokens.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Format duration
 */
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m")
    }.trim().ifEmpty { "${seconds}s" }
}
