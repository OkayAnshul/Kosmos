package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.components.task.TaskFormatUtils
import com.example.kosmos.features.tasks.components.CommitMessageDialog
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * Task Edit Screen (Comprehensive Editing)
 *
 * Priority 2 screen providing comprehensive editing for all 21 task fields.
 * Features inline editing, validation, and unsaved changes warning.
 *
 * Editable Fields:
 * 1. Title (required, max 200 chars)
 * 2. Description (optional, max 5000 chars)
 * 3. Status (dropdown)
 * 4. Priority (dropdown)
 * 5. Assigned User (user picker)
 * 6. Due Date (date picker)
 * 7. Estimated Hours (decimal input)
 * 8. Actual Hours (decimal input)
 * 9. Tags (tag input, max 10)
 * 10. Subtasks (TODO: Future implementation)
 * 11. Comments (view + add new)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    originalTask: Task?,
    draftTask: Task?,
    validationErrors: Map<String, String>,
    availableUsers: List<User>,
    isSaving: Boolean,
    isLoading: Boolean,
    error: String?,
    showUnsavedWarning: Boolean,
    hasUnsavedChanges: Boolean,
    onFieldChange: (field: String, value: Any?) -> Unit,
    onSave: () -> Unit,
    onDiscardChanges: () -> Unit,
    onShowUnsavedWarning: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    // Commit dialog parameters
    showCommitDialog: Boolean = false,
    pendingChanges: List<FieldChange> = emptyList(),
    onCommitConfirm: (String?) -> Unit = {},
    onCommitDismiss: () -> Unit = {},
    onDontAskAgain: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showStatusPicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showUserPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Handle back press with unsaved changes warning
    BackHandler(enabled = hasUnsavedChanges && !showUnsavedWarning) {
        onShowUnsavedWarning(true)
    }

    ScreenScaffoldStandard(
        title = "Edit Task",
        onNavigationClick = {
            if (hasUnsavedChanges) {
                onShowUnsavedWarning(true)
            } else {
                onNavigateBack()
            }
        },
        actions = {
            // Save Button
            Button(
                onClick = onSave,
                enabled = !isSaving && hasUnsavedChanges && validationErrors.isEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary,
                    disabledContainerColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ColorTokens.ReactTheme.primaryForeground
                    )
                } else {
                    Text("Save")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(
                        message = "Loading task...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    ErrorState(
                        title = "Failed to load task",
                        message = error,
                        onRetry = { /* Retry handled by wrapper */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                draftTask != null -> {
                    TaskEditForm(
                        task = draftTask,
                        validationErrors = validationErrors,
                        availableUsers = availableUsers,
                        onFieldChange = onFieldChange,
                        onShowStatusPicker = { showStatusPicker = true },
                        onShowPriorityPicker = { showPriorityPicker = true },
                        onShowUserPicker = { showUserPicker = true },
                        onShowDatePicker = { showDatePicker = true },
                        onShowTagDialog = { showTagDialog = true }
                    )
                }
            }
        }
    }

    // Status Picker Dialog
    if (showStatusPicker && draftTask != null) {
        StatusPickerDialog(
            currentStatus = draftTask.status,
            onStatusSelected = { status ->
                onFieldChange("status", status)
                showStatusPicker = false
            },
            onDismiss = { showStatusPicker = false }
        )
    }

    // Priority Picker Dialog
    if (showPriorityPicker && draftTask != null) {
        PriorityPickerDialog(
            currentPriority = draftTask.priority,
            onPrioritySelected = { priority ->
                onFieldChange("priority", priority)
                showPriorityPicker = false
            },
            onDismiss = { showPriorityPicker = false }
        )
    }

    // User Picker Dialog
    if (showUserPicker && draftTask != null) {
        UserPickerDialog(
            users = availableUsers,
            title = "Assign Task",
            onUserSelected = { user ->
                onFieldChange("assignedToId", user)
                showUserPicker = false
            },
            onDismiss = { showUserPicker = false }
        )
    }

    // Date Picker Dialog
    if (showDatePicker && draftTask != null) {
        DatePickerDialog(
            currentDate = draftTask.dueDate,
            onDateSelected = { date ->
                onFieldChange("dueDate", date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Tag Input Dialog
    if (showTagDialog && draftTask != null) {
        TagInputDialog(
            currentTags = draftTask.tags,
            title = "Edit Tags",
            maxTags = 10,
            onTagsUpdated = { tags ->
                onFieldChange("tags", tags)
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }

    // Unsaved Changes Warning Dialog
    if (showUnsavedWarning) {
        UnsavedChangesDialog(
            onSave = {
                onSave()
                onShowUnsavedWarning(false)
            },
            onDiscard = {
                onDiscardChanges()
                onShowUnsavedWarning(false)
                onNavigateBack()
            },
            onKeepEditing = {
                onShowUnsavedWarning(false)
            }
        )
    }

    // Commit Message Dialog
    if (showCommitDialog) {
        CommitMessageDialog(
            isVisible = true,
            changes = pendingChanges,
            onConfirm = { message ->
                onCommitConfirm(message)
            },
            onDismiss = {
                onCommitDismiss()
            },
            onDontAskAgain = { dontAsk ->
                onDontAskAgain(dontAsk)
            }
        )
    }
}

/**
 * Task Edit Form
 *
 * Scrollable form with all editable fields
 */
@Composable
private fun TaskEditForm(
    task: Task,
    validationErrors: Map<String, String>,
    availableUsers: List<User>,
    onFieldChange: (field: String, value: Any?) -> Unit,
    onShowStatusPicker: () -> Unit,
    onShowPriorityPicker: () -> Unit,
    onShowUserPicker: () -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTagDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        // Section: Basic Information
        item {
            SectionHeader(title = "Basic Information")
        }

        // Field 1: Title
        item {
            TitleField(
                value = task.title,
                error = validationErrors["title"],
                onValueChange = { onFieldChange("title", it) }
            )
        }

        // Field 2: Description
        item {
            DescriptionField(
                value = task.description,
                error = validationErrors["description"],
                onValueChange = { onFieldChange("description", it) }
            )
        }

        // Section: Status & Priority
        item {
            SectionHeader(title = "Status & Priority")
        }

        // Field 3: Status & Field 4: Priority (Side by side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                StatusPickerField(
                    status = task.status,
                    onClick = onShowStatusPicker,
                    modifier = Modifier.weight(1f)
                )
                PriorityPickerField(
                    priority = task.priority,
                    onClick = onShowPriorityPicker,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Assignment & Due Date
        item {
            SectionHeader(title = "Assignment & Deadline")
        }

        // Field 5: Assigned User
        item {
            AssignedUserField(
                assignedUserName = task.assignedToName,
                onClick = onShowUserPicker
            )
        }

        // Field 6: Due Date
        item {
            DueDateField(
                dueDate = task.dueDate,
                onClick = onShowDatePicker
            )
        }

        // Section: Time Tracking
        item {
            SectionHeader(title = "Time Tracking")
        }

        // Field 7 & 8: Estimated and Actual Hours (Side by side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                EstimatedHoursField(
                    value = task.estimatedHours,
                    error = validationErrors["estimatedHours"],
                    onValueChange = { onFieldChange("estimatedHours", it) },
                    modifier = Modifier.weight(1f)
                )
                ActualHoursField(
                    value = task.actualHours,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Progress Indicator (if both hours set)
        if (task.estimatedHours != null && task.actualHours != null) {
            item {
                TimeProgressIndicator(
                    estimatedHours = task.estimatedHours,
                    actualHours = task.actualHours
                )
            }
        }

        // Section: Tags
        item {
            SectionHeader(title = "Tags")
        }

        // Field 9: Tags
        item {
            TagsField(
                tags = task.tags,
                error = validationErrors["tags"],
                onClick = onShowTagDialog
            )
        }

        // Section: Comments
        item {
            SectionHeader(title = "Comments")
        }

        // Field 11: Comments (View existing)
        if (task.comments.isNotEmpty()) {
            items(task.comments) { comment ->
                CommentItem(comment = comment)
            }
        } else {
            item {
                Text(
                    text = "No comments yet",
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================================
// Form Field Components
// ============================================================================

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = TypographyTokens.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = ColorTokens.ReactTheme.foreground,
        modifier = modifier
    )
}

@Composable
private fun TitleField(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title *") },
            placeholder = { Text("Enter task title") },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorTokens.ReactTheme.foreground,
                unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                focusedBorderColor = ColorTokens.ReactTheme.primary,
                unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
                errorBorderColor = ColorTokens.ReactTheme.destructive,
                focusedLabelColor = ColorTokens.ReactTheme.primary,
                unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
            )
        )

        if (error != null) {
            Text(
                text = error,
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive,
                modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
            )
        }

        Text(
            text = "${value.length}/200 characters",
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
        )
    }
}

@Composable
private fun DescriptionField(
    value: String?,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            label = { Text("Description") },
            placeholder = { Text("Enter task description...") },
            isError = error != null,
            minLines = 4,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default
            ),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorTokens.ReactTheme.foreground,
                unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                focusedBorderColor = ColorTokens.ReactTheme.primary,
                unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
                errorBorderColor = ColorTokens.ReactTheme.destructive,
                focusedLabelColor = ColorTokens.ReactTheme.primary,
                unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
            )
        )

        if (error != null) {
            Text(
                text = error,
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive,
                modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
            )
        }

        val charCount = value?.length ?: 0
        Text(
            text = "$charCount/5000 characters",
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
        )
    }
}

@Composable
private fun StatusPickerField(
    status: TaskStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Status",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = TaskFormatUtils.getStatusColor(status).copy(alpha = 0.1f),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TaskFormatUtils.getStatusLabel(status),
                    style = TypographyTokens.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = TaskFormatUtils.getStatusColor(status)
                )
                Icon(
                    imageVector = IconSet.Direction.expandMore,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PriorityPickerField(
    priority: TaskPriority,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Priority",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = TaskFormatUtils.getPriorityColor(priority).copy(alpha = 0.1f),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TaskFormatUtils.getPriorityShortLabel(priority),
                    style = TypographyTokens.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = TaskFormatUtils.getPriorityColor(priority)
                )
                Icon(
                    imageVector = IconSet.Direction.expandMore,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AssignedUserField(
    assignedUserName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Assigned To",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = ColorTokens.ReactTheme.card,
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    Icon(
                        imageVector = IconSet.User.person,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = assignedUserName ?: "Not assigned",
                        style = TypographyTokens.typography.bodyMedium,
                        color = if (assignedUserName != null) {
                            ColorTokens.ReactTheme.foreground
                        } else {
                            ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                        }
                    )
                }
                Icon(
                    imageVector = IconSet.Direction.chevronRight,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DueDateField(
    dueDate: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Due Date",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = ColorTokens.ReactTheme.card,
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    Icon(
                        imageVector = IconSet.Time.schedule,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (dueDate != null) {
                            TaskFormatUtils.formatDate(dueDate)
                        } else {
                            "Set due date"
                        },
                        style = TypographyTokens.typography.bodyMedium,
                        color = if (dueDate != null) {
                            ColorTokens.ReactTheme.foreground
                        } else {
                            ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                        }
                    )
                }
                Icon(
                    imageVector = IconSet.Direction.chevronRight,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EstimatedHoursField(
    value: Float?,
    error: String?,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) {
        mutableStateOf(value?.toString() ?: "")
    }

    Column(modifier = modifier) {
        Text(
            text = "Estimated",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                val parsed = newValue.toFloatOrNull()
                when {
                    parsed != null && parsed >= 0 -> onValueChange(parsed)
                    newValue.isEmpty() -> onValueChange(null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            suffix = {
                Text(
                    text = "hrs",
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorTokens.ReactTheme.foreground,
                unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                focusedBorderColor = ColorTokens.ReactTheme.primary,
                unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
                errorBorderColor = ColorTokens.ReactTheme.destructive
            )
        )

        if (error != null) {
            Text(
                text = error,
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive,
                modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
            )
        }
    }
}

@Composable
// [FUTURE] actualHours is read-only here — it is the authoritative sum from the time tracker.
// To edit it, use the time tracker widget on the Task Detail screen.
private fun ActualHoursField(
    value: Float?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Actual",
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        OutlinedTextField(
            value = if (value != null) "$value hrs" else "—",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorTokens.ReactTheme.mutedForeground,
                unfocusedTextColor = ColorTokens.ReactTheme.mutedForeground,
                focusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                disabledTextColor = ColorTokens.ReactTheme.mutedForeground
            )
        )

        Text(
            text = "Set by time tracker",
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
        )
    }
}

@Composable
private fun TimeProgressIndicator(
    estimatedHours: Float,
    actualHours: Float,
    modifier: Modifier = Modifier
) {
    val progressData = TaskFormatUtils.calculateTimeProgress(estimatedHours, actualHours)

    if (progressData != null) {
        val (progress, progressText, isOverBudget) = progressData
        val progressColor = TaskFormatUtils.getProgressColor(progress)

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = ColorTokens.ReactTheme.card
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = TypographyTokens.typography.labelSmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = progressText,
                        style = TypographyTokens.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = progressColor
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor,
                    trackColor = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun TagsField(
    tags: List<String>,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            color = ColorTokens.ReactTheme.card,
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tags.isEmpty()) "Add tags" else "${tags.size} tags",
                        style = TypographyTokens.typography.bodyMedium,
                        color = if (tags.isEmpty()) {
                            ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                        } else {
                            ColorTokens.ReactTheme.foreground
                        }
                    )
                    Icon(
                        imageVector = IconSet.Direction.chevronRight,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.take(5).forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = tag,
                                    style = TypographyTokens.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.padding(
                                        horizontal = Tokens.Spacing.sm,
                                        vertical = Tokens.Spacing.xxs
                                    )
                                )
                            }
                        }
                        if (tags.size > 5) {
                            Text(
                                text = "+${tags.size - 5}",
                                style = TypographyTokens.typography.labelSmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive,
                modifier = Modifier.padding(start = Tokens.Spacing.md, top = Tokens.Spacing.xxs)
            )
        }
    }
}

@Composable
private fun CommentItem(
    comment: TaskComment,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        color = ColorTokens.ReactTheme.card
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    style = TypographyTokens.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "•",
                    style = TypographyTokens.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                )
                Text(
                    text = TaskFormatUtils.formatDateTime(comment.timestamp),
                    style = TypographyTokens.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                )
            }
            Text(
                text = comment.content,
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

// ============================================================================
// Dialog Components
// ============================================================================

@Composable
private fun StatusPickerDialog(
    currentStatus: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Status",
                style = TypographyTokens.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                TaskStatus.values().forEach { status ->
                    Surface(
                        onClick = { onStatusSelected(status) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (status == currentStatus) {
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                        } else {
                            ColorTokens.ReactTheme.card
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Tokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TaskFormatUtils.getStatusLabel(status),
                                style = TypographyTokens.typography.bodyMedium,
                                color = TaskFormatUtils.getStatusColor(status)
                            )
                            if (status == currentStatus) {
                                Icon(
                                    imageVector = IconSet.Action.check,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = ColorTokens.ReactTheme.card
    )
}

@Composable
private fun PriorityPickerDialog(
    currentPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Priority",
                style = TypographyTokens.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                TaskPriority.values().forEach { priority ->
                    Surface(
                        onClick = { onPrioritySelected(priority) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (priority == currentPriority) {
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                        } else {
                            ColorTokens.ReactTheme.card
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Tokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TaskFormatUtils.getPriorityLabel(priority),
                                style = TypographyTokens.typography.bodyMedium,
                                color = TaskFormatUtils.getPriorityColor(priority)
                            )
                            if (priority == currentPriority) {
                                Icon(
                                    imageVector = IconSet.Action.check,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = ColorTokens.ReactTheme.card
    )
}

@Composable
private fun DatePickerDialog(
    currentDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement Material 3 DatePicker when stable
    // For now, use a simple placeholder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Due Date") },
        text = {
            Text(
                "Date picker coming soon. Current: ${TaskFormatUtils.formatDate(currentDate)}",
                color = ColorTokens.ReactTheme.mutedForeground
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        containerColor = ColorTokens.ReactTheme.card
    )
}

@Composable
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = {
            Text(
                text = "Unsaved Changes",
                style = TypographyTokens.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground
            )
        },
        text = {
            Text(
                text = "You have unsaved changes. What would you like to do?",
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)) {
                TextButton(onClick = onDiscard) {
                    Text("Discard", color = ColorTokens.ReactTheme.destructive)
                }
                TextButton(onClick = onKeepEditing) {
                    Text("Keep Editing")
                }
            }
        },
        containerColor = ColorTokens.ReactTheme.card
    )
}

// BackHandler composable for handling back press
@Composable
private fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
