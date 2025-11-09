package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kosmos.features.tasks.components.TaskPriority
import com.example.kosmos.features.tasks.components.TaskStatus
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Quick Task Creation Sheet
 *
 * Bottom sheet for rapid task creation with:
 * - Title and description
 * - Status selection
 * - Priority selection
 * - Due date picker
 * - Project selection (optional)
 * - Assignee selection (optional)
 * - Smart defaults
 * - Quick save
 *
 * Power user features:
 * - Keyboard navigation
 * - Smart suggestions
 * - Template support (future)
 */

/**
 * Quick Task Creation Sheet
 *
 * @param onDismiss Dismiss handler
 * @param onCreate Task creation handler
 * @param modifier Modifier
 * @param initialProjectId Initial project ID (if creating from project context)
 * @param initialProjectName Initial project name
 * @param availableProjects List of available projects
 * @param availableAssignees List of available assignees
 * @param isCreating Loading state from ViewModel - controls button loading indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTaskCreationSheet(
    onDismiss: () -> Unit,
    onCreate: (QuickTaskData) -> Unit,
    modifier: Modifier = Modifier,
    initialProjectId: String? = null,
    initialProjectName: String? = null,
    availableProjects: List<ProjectOption> = emptyList(),
    availableAssignees: List<AssigneeOption> = emptyList(),
    isCreating: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(TaskStatus.TODO) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedProjectId by remember { mutableStateOf(initialProjectId) }
    var selectedProjectName by remember { mutableStateOf(initialProjectName) }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var selectedAssignees by remember { mutableStateOf<List<String>>(emptyList()) }

    var showProjectPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAssigneePicker by remember { mutableStateOf(false) }

    val canCreate = title.isNotBlank() && !isCreating

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Task",
                    style = MaterialTheme.typography.titleLarge
                )

                IconButtonStandard(
                    icon = IconSet.Navigation.close,
                    onClick = onDismiss,
                    contentDescription = "Close"
                )
            }

            // Title input
            TextFieldStandard(
                value = title,
                onValueChange = { title = it },
                label = "Task Title",
                placeholder = "What needs to be done?",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            // Description input
            TextFieldMultiline(
                value = description,
                onValueChange = { description = it },
                label = "Description (optional)",
                placeholder = "Add more details...",
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Status selection
            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = "Status",
                    style = TypographyTokens.Custom.inputLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ChipGroup(
                    chips = TaskStatus.values().map {
                        it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }
                    },
                    selectedChips = setOf(
                        selectedStatus.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }
                    ),
                    onChipClick = { statusName ->
                        val status = TaskStatus.values().find {
                            it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } == statusName
                        }
                        status?.let { selectedStatus = it }
                    },
                    multiSelect = false
                )
            }

            // Priority selection
            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = "Priority",
                    style = TypographyTokens.Custom.inputLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    TaskPriority.values().forEach { priority ->
                        PriorityChip(
                            priority = priority,
                            isSelected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Optional fields
            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                Text(
                    text = "Optional",
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Project selection
                if (availableProjects.isNotEmpty()) {
                    SelectableListItem(
                        icon = IconSet.Navigation.projects,
                        label = "Project",
                        value = selectedProjectName ?: "Select project",
                        onClick = { showProjectPicker = true },
                        isValueSet = selectedProjectName != null
                    )
                }

                // Due date
                SelectableListItem(
                    icon = IconSet.Time.calendar,
                    label = "Due Date",
                    value = dueDate ?: "Set due date",
                    onClick = { showDatePicker = true },
                    isValueSet = dueDate != null
                )

                // Assignees
                if (availableAssignees.isNotEmpty()) {
                    SelectableListItem(
                        icon = IconSet.User.profile,
                        label = "Assignees",
                        value = when {
                            selectedAssignees.isEmpty() -> "Assign to..."
                            selectedAssignees.size == 1 -> availableAssignees.find { it.id == selectedAssignees.first() }?.name ?: "1 assignee"
                            else -> "${selectedAssignees.size} assignees"
                        },
                        onClick = { showAssigneePicker = true },
                        isValueSet = selectedAssignees.isNotEmpty()
                    )
                }
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.md))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isCreating
                )

                LoadingButton(
                    text = "Create Task",
                    onClick = {
                        onCreate(
                            QuickTaskData(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                status = selectedStatus,
                                priority = selectedPriority,
                                projectId = selectedProjectId,
                                dueDate = dueDate,
                                assigneeIds = selectedAssignees
                            )
                        )
                    },
                    isLoading = isCreating,
                    modifier = Modifier.weight(1f),
                    enabled = canCreate
                )
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.md))
        }
    }

    // Project picker dialog
    if (showProjectPicker) {
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            title = { Text("Select Project") },
            text = {
                LazyColumn {
                    items(availableProjects.size) { index ->
                        val project = availableProjects[index]
                        TextButton(
                            onClick = {
                                selectedProjectId = project.id
                                selectedProjectName = project.name
                                showProjectPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(project.name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                dueDate = date?.toString()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Assignee picker dialog
    if (showAssigneePicker) {
        var tempSelectedAssignees by remember { mutableStateOf(selectedAssignees) }

        AlertDialog(
            onDismissRequest = { showAssigneePicker = false },
            title = { Text("Assign To") },
            text = {
                LazyColumn {
                    items(availableAssignees.size) { index ->
                        val assignee = availableAssignees[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tempSelectedAssignees.contains(assignee.id),
                                onCheckedChange = { checked ->
                                    tempSelectedAssignees = if (checked) {
                                        tempSelectedAssignees + assignee.id
                                    } else {
                                        tempSelectedAssignees - assignee.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(assignee.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedAssignees = tempSelectedAssignees
                    showAssigneePicker = false
                }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssigneePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Priority Chip for Selection
 */
@Composable
private fun PriorityChip(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (priority) {
        TaskPriority.URGENT -> ColorTokens.Priority.urgent to "Urgent"
        TaskPriority.HIGH -> ColorTokens.Priority.high to "High"
        TaskPriority.MEDIUM -> ColorTokens.Priority.medium to "Medium"
        TaskPriority.LOW -> ColorTokens.Priority.low to "Low"
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, color)
        else null
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}

            Text(
                text = label,
                style = TypographyTokens.Custom.caption,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Selectable List Item
 */
@Composable
private fun SelectableListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isValueSet: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isValueSet)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isValueSet)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = IconSet.Direction.right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Tokens.Size.iconSmall)
            )
        }
    }
}

/**
 * Quick Task Data
 */
data class QuickTaskData(
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val projectId: String?,
    val dueDate: String?,
    val assigneeIds: List<String>
)

/**
 * Project Option
 */
data class ProjectOption(
    val id: String,
    val name: String
)

/**
 * Assignee Option
 */
data class AssigneeOption(
    val id: String,
    val name: String,
    val avatarUrl: String? = null
)
