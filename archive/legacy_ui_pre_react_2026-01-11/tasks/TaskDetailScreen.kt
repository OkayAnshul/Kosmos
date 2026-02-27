package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.kosmos.core.models.*
import com.example.kosmos.features.tasks.components.CommitMessageDialog
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard
import java.text.SimpleDateFormat
import java.util.*

/**
 * Task Detail Screen - Redesigned to Match Reference
 *
 * Reference Design Features:
 * - Header: "Task Details" + more menu
 * - Offline banner: "Offline Mode • Changes pending sync"
 * - Task title (white, bold, large)
 * - Due date: "Due Oct 12, 2024"
 * - Status badge: "In Progress" (green)
 * - Priority badge: "High Priority" (red)
 * - Assign section: Avatar + name + "Assign" button
 * - Metadata: Due Date field, Add Tag button
 * - Description: Editable text area with bullet points, edit icon
 * - Subtasks: "2/3 completed", checkboxes, "Add" button
 * - Activity: User avatars, timestamp, action description, "View full history"
 * - Comment box: Input field + send button
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Card-based layout
 * - Blue accent for interactive elements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task?,
    assignedUser: User?,
    currentUserId: String,
    availableUsers: List<User>,
    subtasks: List<Task>,
    isLoading: Boolean,
    isUpdating: Boolean,
    isOffline: Boolean,
    error: String?,
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAssignUser: (User) -> Unit,
    onTagsUpdated: (List<String>) -> Unit,
    onEstimatedHoursChange: (Float?) -> Unit,
    onActualHoursChange: (Float?) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteTask: () -> Unit,
    onNavigateBack: () -> Unit,
    // Commit dialog parameters
    showCommitDialog: Boolean = false,
    pendingChanges: List<FieldChange> = emptyList(),
    onCommitConfirm: (String?) -> Unit = {},
    onCommitDismiss: () -> Unit = {},
    onDontAskAgain: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    ScreenScaffoldStandard(
        title = "Task Details",
        onNavigationClick = onNavigateBack,
        actions = {
            Box {
                IconButtonStandard(
                    icon = IconSet.Action.moreVert,
                    onClick = { showMoreMenu = true },
                    contentDescription = "More options"
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Task") },
                        onClick = {
                            showMoreMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                IconSet.Action.delete,
                                contentDescription = "",
                                tint = ColorTokens.Stitch.error
                            )
                        }
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.Stitch.backgroundPrimary)
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

                task != null -> {
                    TaskDetailContent(
                        task = task,
                        assignedUser = assignedUser,
                        currentUserId = currentUserId,
                        availableUsers = availableUsers,
                        subtasks = subtasks,
                        isUpdating = isUpdating,
                        isOffline = isOffline,
                        onStatusChange = onStatusChange,
                        onPriorityChange = onPriorityChange,
                        onDescriptionChange = onDescriptionChange,
                        onAssignUser = onAssignUser,
                        onTagsUpdated = onTagsUpdated,
                        onEstimatedHoursChange = onEstimatedHoursChange,
                        onActualHoursChange = onActualHoursChange,
                        onToggleSubtask = onToggleSubtask,
                        onAddComment = onAddComment
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Task?",
                    style = TypographyTokens.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorTokens.Stitch.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task? This action cannot be undone.",
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.Stitch.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.Stitch.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = ColorTokens.Stitch.textPrimary
                    )
                }
            },
            containerColor = ColorTokens.Stitch.surface
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
 * Task Detail Content
 */
@Composable
private fun TaskDetailContent(
    task: Task,
    assignedUser: User?,
    currentUserId: String,
    availableUsers: List<User>,
    subtasks: List<Task>,
    isUpdating: Boolean,
    isOffline: Boolean,
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAssignUser: (User) -> Unit,
    onTagsUpdated: (List<String>) -> Unit,
    onEstimatedHoursChange: (Float?) -> Unit,
    onActualHoursChange: (Float?) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingDescription by remember { mutableStateOf(false) }
    var descriptionText by remember(task.description) { mutableStateOf(task.description ?: "") }
    var commentText by remember { mutableStateOf("") }
    var showUserPicker by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Capture currentUserId for use in LazyColumn items
    val currentUserIdCaptured = currentUserId

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Tokens.Spacing.md)
    ) {
        // Offline Banner
        if (isOffline) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorTokens.Stitch.warning.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IconSet.Status.offline,
                            contentDescription = "",
                            tint = ColorTokens.Stitch.warning,
                            modifier = Modifier.size(Tokens.Size.iconSmall)
                        )
                        Text(
                            text = "Offline Mode • Changes pending sync",
                            style = TypographyTokens.typography.bodySmall,
                            color = ColorTokens.Stitch.warning
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
            }
        }

        // Task Title
        item {
            Text(
                text = task.title,
                style = TypographyTokens.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.Stitch.textPrimary
            )
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
        }

        // Due Date
        task.dueDate?.let { dueDate ->
            item {
                Text(
                    text = "Due ${formatDate(dueDate)}",
                    style = TypographyTokens.typography.bodyMedium,
                    color = if (dueDate < System.currentTimeMillis()) {
                        ColorTokens.Stitch.error
                    } else {
                        ColorTokens.Stitch.textSecondary
                    }
                )
                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
            }
        }

        // Status and Priority Badges - Interactive
        item {
            var showStatusMenu by remember { mutableStateOf(false) }
            var showPriorityMenu by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                // Interactive Status Badge with Dropdown - PROMINENT
                Box {
                    Surface(
                        onClick = { if (!isUpdating) showStatusMenu = true },
                        shape = RoundedCornerShape(Tokens.CornerRadius.md)
                    ) {
                        StatusBadge(status = task.status)
                    }

                    // Status Dropdown Menu with Permission Check
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        TaskStatus.values().forEach { status ->
                            // Permission check: Only assigned user can mark DONE
                            val canSelectStatus = when {
                                status == TaskStatus.DONE && task.assignedToId != null -> {
                                    // Only assigned user can mark as DONE
                                    task.assignedToId == currentUserIdCaptured
                                }
                                else -> true // All other statuses allowed for everyone
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Status color indicator
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(getStatusColor(status), CircleShape)
                                        )
                                        Text(
                                            text = getStatusLabel(status),
                                            style = TypographyTokens.typography.bodyMedium
                                        )
                                        // Checkmark for current status
                                        if (status == task.status) {
                                            Spacer(Modifier.weight(1f))
                                            Icon(
                                                IconSet.Action.check,
                                                contentDescription = "Current status",
                                                modifier = Modifier.size(16.dp),
                                                tint = ColorTokens.Stitch.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (canSelectStatus) {
                                        onStatusChange(status)
                                        showStatusMenu = false
                                    }
                                },
                                enabled = !isUpdating && canSelectStatus
                            )
                        }
                    }
                }

                // Priority Badge with SUBTLE edit icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PriorityBadge(priority = task.priority)

                    // Small edit icon - SUBTLE, not loud
                    IconButton(
                        onClick = { showPriorityMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            IconSet.Action.edit,
                            contentDescription = "Edit priority",
                            modifier = Modifier.size(14.dp),
                            tint = ColorTokens.Stitch.textSecondary.copy(alpha = 0.6f)
                        )
                    }

                    // Priority Dropdown Menu
                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false }
                    ) {
                        TaskPriority.values().forEach { priority ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(getPriorityColor(priority), CircleShape)
                                        )
                                        Text(getPriorityLabel(priority))
                                        if (priority == task.priority) {
                                            Spacer(Modifier.weight(1f))
                                            Icon(
                                                IconSet.Action.check,
                                                contentDescription = "Current priority",
                                                modifier = Modifier.size(16.dp),
                                                tint = ColorTokens.Stitch.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onPriorityChange(priority)
                                    showPriorityMenu = false
                                },
                                enabled = !isUpdating
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
        }

        // Last Updated Info
        item {
            Text(
                text = "Last updated by ${task.createdByName} on ${formatDateTime(task.updatedAt)}",
                style = TypographyTokens.typography.bodySmall,
                color = ColorTokens.Stitch.textSecondary
            )
            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Assign Section
        item {
            SectionHeader(title = "Assign")
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ColorTokens.Stitch.cardBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (assignedUser != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            UserAvatar(
                                photoUrl = assignedUser.photoUrl,
                                displayName = assignedUser.displayName,
                                isOnline = assignedUser.isOnline,
                                size = 40.dp
                            )
                            Text(
                                text = assignedUser.displayName,
                                style = TypographyTokens.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = ColorTokens.Stitch.textPrimary
                            )
                        }
                    } else {
                        Text(
                            text = "Unassigned",
                            style = TypographyTokens.typography.bodyLarge,
                            color = ColorTokens.Stitch.textSecondary
                        )
                    }

                    Button(
                        onClick = { showUserPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.Stitch.primary
                        )
                    ) {
                        Text(if (assignedUser != null) "Reassign" else "Assign")
                    }
                }
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Time Tracking Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Tokens.CornerRadius.lg),
                color = ColorTokens.Stitch.cardBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.lg)
                ) {
                    // Section Header
                    Text(
                        text = "Time Tracking",
                        style = TypographyTokens.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ColorTokens.Stitch.textPrimary
                    )

                    Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                    // Input Fields Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                    ) {
                        // Estimated Hours
                        TimeInputField(
                            label = "Estimated Hours",
                            value = task.estimatedHours,
                            onValueChange = onEstimatedHoursChange,
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating
                        )

                        // Actual Hours
                        TimeInputField(
                            label = "Actual Hours",
                            value = task.actualHours,
                            onValueChange = onActualHoursChange,
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating
                        )
                    }

                    // Progress Bar (show if both values set)
                    if (task.estimatedHours != null && task.actualHours != null) {
                        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                        val progress = (task.actualHours!! / task.estimatedHours!!).coerceIn(0f, 1.2f)
                        val progressPercent = (progress * 100).toInt()
                        val isOverBudget = progress > 1f

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progress",
                                    style = TypographyTokens.typography.bodySmall,
                                    color = ColorTokens.Stitch.textSecondary
                                )
                                Text(
                                    text = if (isOverBudget) {
                                        "$progressPercent% (over budget)"
                                    } else {
                                        "$progressPercent% complete"
                                    },
                                    style = TypographyTokens.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = when {
                                        progress > 1f -> ColorTokens.Stitch.error
                                        progress >= 0.8f -> ColorTokens.Priority.medium
                                        else -> ColorTokens.Status.online
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    progress > 1f -> ColorTokens.Stitch.error
                                    progress >= 0.8f -> ColorTokens.Priority.medium
                                    else -> ColorTokens.Status.online
                                },
                                trackColor = ColorTokens.Stitch.surface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Tags Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Tokens.CornerRadius.lg),
                color = ColorTokens.Stitch.cardBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tags",
                            style = TypographyTokens.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ColorTokens.Stitch.textPrimary
                        )

                        IconButton(onClick = { showTagDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Manage tags",
                                tint = ColorTokens.Stitch.primary
                            )
                        }
                    }

                    if (task.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Tokens.Spacing.md))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            task.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                                    color = ColorTokens.Stitch.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = tag,
                                        style = TypographyTokens.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = ColorTokens.Stitch.primary,
                                        modifier = Modifier.padding(
                                            horizontal = Tokens.Spacing.md,
                                            vertical = Tokens.Spacing.sm
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No tags added",
                            style = TypographyTokens.typography.bodyMedium,
                            color = ColorTokens.Stitch.textSecondary,
                            modifier = Modifier.padding(top = Tokens.Spacing.sm)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Description Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Description")
                if (!isEditingDescription) {
                    IconButtonStandard(
                        icon = IconSet.Action.edit,
                        onClick = { isEditingDescription = true },
                        contentDescription = "Edit description"
                    )
                }
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

            if (isEditingDescription) {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a description...") },
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ColorTokens.Stitch.textPrimary,
                        unfocusedTextColor = ColorTokens.Stitch.textPrimary,
                        focusedBorderColor = ColorTokens.Stitch.primary,
                        unfocusedBorderColor = ColorTokens.Stitch.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    TextButton(onClick = {
                        isEditingDescription = false
                        descriptionText = task.description ?: ""
                    }) {
                        Text("Cancel", color = ColorTokens.Stitch.textPrimary)
                    }
                    Button(
                        onClick = {
                            onDescriptionChange(descriptionText)
                            isEditingDescription = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.Stitch.primary
                        )
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Text(
                    text = task.description?.takeIf { it.isNotBlank() } ?: "No description",
                    style = TypographyTokens.typography.bodyMedium,
                    color = if (task.description.isNullOrBlank()) {
                        ColorTokens.Stitch.textSecondary
                    } else {
                        ColorTokens.Stitch.textPrimary
                    }
                )
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Subtasks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val completedCount = subtasks.count { it.status == TaskStatus.DONE }
                SectionHeader(title = "Subtasks ($completedCount/${subtasks.size} completed)")

                IconButtonStandard(
                    icon = IconSet.Action.add,
                    onClick = { /* TODO: Add subtask */ },
                    contentDescription = "Add subtask"
                )
            }
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
        }

        items(subtasks) { subtask ->
            SubtaskItem(
                subtask = subtask,
                onToggle = { onToggleSubtask(subtask.id) }
            )
        }

        if (subtasks.isEmpty()) {
            item {
                Text(
                    text = "No subtasks yet",
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.Stitch.textSecondary,
                    modifier = Modifier.padding(vertical = Tokens.Spacing.md)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(Tokens.Spacing.lg)) }

        // Comment Box
        item {
            SectionHeader(title = "Comments")
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ColorTokens.Stitch.textPrimary,
                        unfocusedTextColor = ColorTokens.Stitch.textPrimary,
                        focusedBorderColor = ColorTokens.Stitch.primary,
                        unfocusedBorderColor = ColorTokens.Stitch.textSecondary
                    )
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        imageVector = IconSet.Message.send,
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank()) {
                            ColorTokens.Stitch.primary
                        } else {
                            ColorTokens.Stitch.textSecondary
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(Tokens.Spacing.xxl)) }
    }

    // User Picker Dialog
    if (showUserPicker) {
        UserPickerDialog(
            users = availableUsers,
            title = "Assign Task",
            showRole = true,
            onUserSelected = { user ->
                onAssignUser(user)
                showUserPicker = false
            },
            onDismiss = { showUserPicker = false }
        )
    }

    // Tag Input Dialog
    if (showTagDialog) {
        TagInputDialog(
            currentTags = task.tags,
            title = "Manage Tags",
            maxTags = 10,
            onTagsUpdated = { newTags ->
                onTagsUpdated(newTags)
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }
}

/**
 * Section Header
 */
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
        color = ColorTokens.Stitch.textPrimary,
        modifier = modifier
    )
}

/**
 * Status Badge
 */
@Composable
private fun StatusBadge(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, label) = when (status) {
        TaskStatus.TODO -> ColorTokens.Stitch.textSecondary to "To Do"
        TaskStatus.IN_PROGRESS -> ColorTokens.Stitch.primary to "In Progress"
        TaskStatus.DONE -> ColorTokens.Stitch.success to "Done"
        TaskStatus.CANCELLED -> ColorTokens.Stitch.error to "Cancelled"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            style = TypographyTokens.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = backgroundColor,
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.sm,
                vertical = Tokens.Spacing.xxs
            )
        )
    }
}

/**
 * Priority Badge
 */
@Composable
private fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, label) = when (priority) {
        TaskPriority.LOW -> ColorTokens.Stitch.success to "Low Priority"
        TaskPriority.MEDIUM -> ColorTokens.Stitch.warning to "Medium Priority"
        TaskPriority.HIGH -> ColorTokens.Stitch.error to "High Priority"
        TaskPriority.URGENT -> ColorTokens.Stitch.error to "Urgent"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            style = TypographyTokens.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = backgroundColor,
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.sm,
                vertical = Tokens.Spacing.xxs
            )
        )
    }
}

/**
 * Subtask Item
 */
@Composable
private fun SubtaskItem(
    subtask: Task,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.xxs),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.Stitch.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            Checkbox(
                checked = subtask.status == TaskStatus.DONE,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorTokens.Stitch.primary,
                    uncheckedColor = ColorTokens.Stitch.textSecondary
                )
            )

            Text(
                text = subtask.title,
                style = TypographyTokens.typography.bodyMedium,
                color = if (subtask.status == TaskStatus.DONE) {
                    ColorTokens.Stitch.textSecondary
                } else {
                    ColorTokens.Stitch.textPrimary
                }
            )
        }
    }
}

/**
 * Format date for display
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Format date and time for display
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Status color mapping
 */
private fun getStatusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.TODO -> ColorTokens.Stitch.textSecondary
    TaskStatus.IN_PROGRESS -> ColorTokens.Stitch.primary
    TaskStatus.DONE -> ColorTokens.Status.online
    TaskStatus.CANCELLED -> ColorTokens.Stitch.error
}

/**
 * Status label mapping
 */
private fun getStatusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.TODO -> "To Do"
    TaskStatus.IN_PROGRESS -> "In Progress"
    TaskStatus.DONE -> "Done"
    TaskStatus.CANCELLED -> "Cancelled"
}

/**
 * Priority color mapping
 */
private fun getPriorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.URGENT -> Color(0xFFD32F2F)  // Dark red
    TaskPriority.HIGH -> ColorTokens.Priority.high
    TaskPriority.MEDIUM -> ColorTokens.Priority.medium
    TaskPriority.LOW -> ColorTokens.Priority.low
}

/**
 * Priority label mapping
 */
private fun getPriorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.URGENT -> "Urgent"
    TaskPriority.HIGH -> "High"
    TaskPriority.MEDIUM -> "Medium"
    TaskPriority.LOW -> "Low"
}

/**
 * Time Input Field Component
 */
@Composable
private fun TimeInputField(
    label: String,
    value: Float?,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var textValue by remember(value) {
        mutableStateOf(value?.toString() ?: "")
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = TypographyTokens.typography.labelSmall,
            color = ColorTokens.Stitch.textSecondary
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                // Validate and parse
                val parsed = newValue.toFloatOrNull()
                when {
                    parsed != null && parsed >= 0 -> onValueChange(parsed)
                    newValue.isEmpty() -> onValueChange(null)
                    // Invalid input - keep text but don't update value
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            suffix = {
                Text(
                    text = "hrs",
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.Stitch.textSecondary
                )
            },
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorTokens.Stitch.textPrimary,
                unfocusedTextColor = ColorTokens.Stitch.textPrimary,
                focusedBorderColor = ColorTokens.Stitch.primary,
                unfocusedBorderColor = ColorTokens.Stitch.textSecondary.copy(alpha = 0.5f)
            )
        )
    }
}
