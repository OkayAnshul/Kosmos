package com.example.kosmos.features.tasks.components
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.sync.ConflictChoice
import com.example.kosmos.core.sync.FieldConflict
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Conflict Resolution Dialog
 *
 * Shows field-level conflicts when multiple users edit the same task.
 * User can choose which version to keep for each field.
 *
 * Usage:
 * ```kotlin
 * ConflictResolutionDialog(
 *     isVisible = showConflictDialog,
 *     conflicts = conflicts,
 *     onResolve = { choices ->
 *         val merged = conflictResolver.applyUserChoices(baseTask, choices)
 *         viewModel.updateTask(merged)
 *     },
 *     onDismiss = { /* dismiss */ }
 * )
 * ```
 */
@Composable
fun ConflictResolutionDialog(
    isVisible: Boolean,
    conflicts: List<FieldConflict>,
    onResolve: (Map<String, ConflictChoice>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track user choices for each conflict
    val choices = remember(conflicts) {
        mutableStateMapOf<String, ConflictChoice>().apply {
            // Default to keeping remote (newer)
            conflicts.forEach { conflict ->
                this[conflict.fieldName] = ConflictChoice.KeepRemote(conflict.remoteValue)
            }
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
                        .padding(Tokens.Spacing.md)
                ) {
                    // Header
                    ConflictDialogHeader()

                    Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                    // Conflicts list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        items(items = conflicts, key = { it.fieldName }) { conflict ->
                            ConflictItem(
                                conflict = conflict,
                                selectedChoice = choices[conflict.fieldName]!!,
                                onChoiceChange = { newChoice ->
                                    choices[conflict.fieldName] = newChoice
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                    // Action buttons
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
                                onResolve(choices.toMap())
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorTokens.ReactTheme.primary
                            )
                        ) {
                            Text("Resolve")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog header with warning icon
 */
@Composable
private fun ConflictDialogHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Conflict warning",
            tint = ColorTokens.Priority.medium,
            modifier = Modifier.size(32.dp)
        )

        Column {
            Text(
                text = "Resolve Conflicts",
                style = TypographyTokens.typography.titleLarge,
                color = ColorTokens.ReactTheme.foreground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose which version to keep for each field",
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

/**
 * Individual conflict item
 */
@Composable
private fun ConflictItem(
    conflict: FieldConflict,
    selectedChoice: ConflictChoice,
    onChoiceChange: (ConflictChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.mutedForeground,
                shape = MaterialTheme.shapes.medium
            )
            .padding(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Field name
        Text(
            text = formatFieldName(conflict.fieldName),
            style = TypographyTokens.typography.titleSmall,
            color = ColorTokens.ReactTheme.foreground,
            fontWeight = FontWeight.Bold
        )

        // Your version (local)
        ConflictChoiceCard(
            label = "Your version",
            value = formatFieldValue(conflict.fieldName, conflict.localValue),
            timestamp = conflict.localTimestamp,
            isSelected = selectedChoice is ConflictChoice.KeepLocal,
            onClick = {
                onChoiceChange(ConflictChoice.KeepLocal(conflict.localValue))
            }
        )

        // Other user's version (remote)
        ConflictChoiceCard(
            label = "Other user's version",
            value = formatFieldValue(conflict.fieldName, conflict.remoteValue),
            timestamp = conflict.remoteTimestamp,
            isSelected = selectedChoice is ConflictChoice.KeepRemote,
            onClick = {
                onChoiceChange(ConflictChoice.KeepRemote(conflict.remoteValue))
            }
        )
    }
}

/**
 * Choice card (clickable)
 */
@Composable
private fun ConflictChoiceCard(
    label: String,
    value: String,
    timestamp: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            ColorTokens.ReactTheme.primary
        } else {
            ColorTokens.ReactTheme.card
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                ColorTokens.ReactTheme.primary
            } else {
                ColorTokens.ReactTheme.mutedForeground
            }
        ),
        tonalElevation = if (isSelected) 2.dp else 0.dp
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
                Text(
                    text = label,
                    style = TypographyTokens.typography.labelMedium,
                    color = if (isSelected) {
                        Color.White
                    } else {
                        ColorTokens.ReactTheme.mutedForeground
                    },
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = value,
                    style = TypographyTokens.typography.bodyMedium,
                    color = if (isSelected) {
                        Color.White
                    } else {
                        ColorTokens.ReactTheme.foreground
                    }
                )

                Text(
                    text = formatTimestamp(timestamp),
                    style = TypographyTokens.typography.labelSmall,
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        ColorTokens.ReactTheme.mutedForeground
                    }
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Format field name for display
 */
private fun formatFieldName(fieldName: String): String {
    return when (fieldName) {
        "title" -> "Title"
        "description" -> "Description"
        "status" -> "Status"
        "priority" -> "Priority"
        "assignedToId" -> "Assignee"
        "dueDate" -> "Due Date"
        "tags" -> "Tags"
        "estimatedHours" -> "Estimated Hours"
        "actualHours" -> "Actual Hours"
        else -> fieldName.capitalize()
    }
}

/**
 * Format field value for display
 */
private fun formatFieldValue(fieldName: String, value: Any?): String {
    if (value == null) return "Not set"

    return when (fieldName) {
        "status" -> (value as? TaskStatus)?.name?.replace("_", " ") ?: value.toString()
        "priority" -> (value as? TaskPriority)?.name ?: value.toString()
        "dueDate" -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            dateFormat.format(Date(value as Long))
        }
        "tags" -> (value as? List<*>)?.joinToString(", ") ?: value.toString()
        "estimatedHours", "actualHours" -> "${value}h"
        else -> value.toString()
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd 'at' hh:mm a", Locale.getDefault())
    return "Updated ${dateFormat.format(Date(timestamp))}"
}
