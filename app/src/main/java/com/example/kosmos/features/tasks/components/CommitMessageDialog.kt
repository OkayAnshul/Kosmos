package com.example.kosmos.features.tasks.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kosmos.core.models.FieldChange
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Commit Message Dialog
 *
 * Modal dialog shown after important task changes (status, assignment, due date).
 * Allows users to optionally provide a commit message explaining the change.
 *
 * Features:
 * - Summary of what changed (field-level diffs)
 * - Optional commit message text field
 * - "Don't ask again this session" checkbox
 * - Confirm/Cancel actions
 *
 * Pattern: Git-style commit workflow for task changes
 */
@Composable
fun CommitMessageDialog(
    isVisible: Boolean,
    changes: List<FieldChange>,
    onConfirm: (commitMessage: String?) -> Unit,
    onDismiss: () -> Unit,
    onDontAskAgain: (Boolean) -> Unit = {},
    standaloneMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var commitMessage by remember { mutableStateOf("") }
    var dontAskAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = KosmosDialogDefaults.shape,
            color = ColorTokens.ReactTheme.card,
            tonalElevation = KosmosDialogDefaults.elevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (standaloneMode) "Add Journal Entry" else "Describe this change",
                        style = TypographyTokens.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = IconSet.Navigation.close,
                            contentDescription = "Close",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }

                // Changes summary - hide in standalone mode
                if (!standaloneMode) {
                    ChangesSummarySection(changes = changes)
                }

                // Commit message input
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = {
                        Text(
                            text = if (standaloneMode) "Journal entry" else "Commit message (optional)",
                            style = TypographyTokens.typography.bodyMedium
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (standaloneMode) "What did you work on?" else "Explain why you made this change...",
                            style = TypographyTokens.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = KosmosDialogDefaults.textFieldColors(),
                    shape = RoundedCornerShape(Tokens.CornerRadius.md)
                )

                // "Don't ask again" checkbox - hide in standalone mode
                if (!standaloneMode) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Tokens.Spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ColorTokens.ReactTheme.primary,
                            uncheckedColor = ColorTokens.ReactTheme.border,
                            checkmarkColor = ColorTokens.ReactTheme.primaryForeground
                        )
                    )

                    Text(
                        text = "Don't ask again this session",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorTokens.ReactTheme.mutedForeground
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = TypographyTokens.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.width(Tokens.Spacing.sm))

                    Button(
                        onClick = {
                            if (dontAskAgain) {
                                onDontAskAgain(true)
                            }
                            val message = commitMessage.trim().ifBlank { null }
                            onConfirm(message)
                        },
                        enabled = !standaloneMode || commitMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.ReactTheme.primary,
                            contentColor = ColorTokens.ReactTheme.primaryForeground
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = IconSet.Action.done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                        Text(
                            text = "Confirm",
                            style = TypographyTokens.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Changes Summary Section
 *
 * Displays what changed in a compact, readable format
 */
@Composable
private fun ChangesSummarySection(
    changes: List<FieldChange>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.card,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IconSet.Status.info,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Changes",
                    style = TypographyTokens.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Changes list
            changes.forEach { change ->
                ChangeItem(change = change)
            }
        }
    }
}

/**
 * Individual Change Item
 *
 * Shows field name and before → after values
 */
@Composable
private fun ChangeItem(
    change: FieldChange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Field name
        Text(
            text = "${formatFieldName(change.field)}:",
            style = TypographyTokens.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.width(80.dp)
        )

        // From value (with strikethrough effect)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f)
        ) {
            Text(
                text = change.getFormattedFromValue(),
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.8f),
                modifier = Modifier.padding(
                    horizontal = Tokens.Spacing.xs,
                    vertical = Tokens.Spacing.xxs
                )
            )
        }

        // Arrow
        Icon(
            imageVector = IconSet.Direction.arrowForward,
            contentDescription = "changed to",
            tint = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.size(16.dp)
        )

        // To value (highlighted)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = ColorTokens.Status.online.copy(alpha = 0.1f)
        ) {
            Text(
                text = change.getFormattedToValue(),
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.Status.online.copy(alpha = 0.8f),
                modifier = Modifier.padding(
                    horizontal = Tokens.Spacing.xs,
                    vertical = Tokens.Spacing.xxs
                )
            )
        }
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Format field name for display
 */
private fun formatFieldName(field: String): String {
    return when (field) {
        "status" -> "Status"
        "priority" -> "Priority"
        "assignedTo" -> "Assigned"
        "title" -> "Title"
        "description" -> "Description"
        "dueDate" -> "Due Date"
        "tags" -> "Tags"
        "estimatedHours" -> "Estimate"
        "actualHours" -> "Actual"
        else -> field.replaceFirstChar { it.uppercase() }
    }
}
