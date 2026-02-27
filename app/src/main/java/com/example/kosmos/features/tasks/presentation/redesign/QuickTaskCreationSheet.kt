package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.kosmos.shared.utils.ValidationUtils

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
 *
 * P1-07: Now includes proper validation using ValidationUtils
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
    var estimatedHoursInput by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }

    var showProjectPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAssigneePicker by remember { mutableStateOf(false) }

    // P1-07: Validation errors
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val canCreate = title.isNotBlank() && !isCreating && titleError == null && descriptionError == null

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
                onValueChange = {
                    title = it
                    // Validate on change
                    titleError = ValidationUtils.validateTaskTitle(it)
                },
                label = "Task Title",
                placeholder = "What needs to be done?",
                supportingText = titleError ?: if (title.isNotBlank()) "${title.length}/200 characters" else null,
                isError = titleError != null,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            // Description input
            TextFieldMultiline(
                value = description,
                onValueChange = {
                    description = it
                    // Validate on change
                    descriptionError = ValidationUtils.validateTaskDescription(it)
                },
                label = "Description (optional)",
                placeholder = "Add more details...",
                supportingText = descriptionError ?: if (description.isNotBlank()) "${description.length}/2000 characters" else null,
                isError = descriptionError != null,
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
                    color = ColorTokens.ReactTheme.mutedForeground
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
                    color = ColorTokens.ReactTheme.mutedForeground
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
                    color = ColorTokens.ReactTheme.mutedForeground
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

                // Assignee (single select)
                if (availableAssignees.isNotEmpty()) {
                    SelectableListItem(
                        icon = IconSet.User.profile,
                        label = "Assignee",
                        value = when {
                            selectedAssignees.isEmpty() -> "Assign to..."
                            else -> availableAssignees.find { it.id == selectedAssignees.first() }?.name ?: "1 assignee"
                        },
                        onClick = { showAssigneePicker = true },
                        isValueSet = selectedAssignees.isNotEmpty()
                    )
                }

                // Estimated hours
                TextFieldStandard(
                    value = estimatedHoursInput,
                    onValueChange = { v ->
                        // Allow only numeric input with up to one decimal
                        if (v.isEmpty() || v.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?\$"))) {
                            estimatedHoursInput = v
                        }
                    },
                    label = "Estimated Hours",
                    placeholder = "e.g. 2.5",
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags input
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    TextFieldStandard(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = "Tags",
                        placeholder = "Type tag and press Enter",
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            val trimmed = tagInput.trim()
                            if (trimmed.isNotBlank() && !tags.contains(trimmed)) {
                                tags = tags + trimmed
                                tagInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tags.isNotEmpty()) {
                        ChipGroup(
                            chips = tags,
                            selectedChips = tags.toSet(),
                            onChipClick = { tag -> tags = tags.filter { it != tag } },
                            multiSelect = true
                        )
                    }
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
                        // P1-07: Validate all fields before creating
                        val titleValidation = ValidationUtils.validateTaskTitle(title)
                        val descValidation = ValidationUtils.validateTaskDescription(description)

                        titleError = titleValidation
                        descriptionError = descValidation

                        // Only create if all validations pass
                        if (titleValidation == null && descValidation == null) {
                            onCreate(
                                QuickTaskData(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    status = selectedStatus,
                                    priority = selectedPriority,
                                    projectId = selectedProjectId,
                                    dueDate = dueDate,
                                    assigneeIds = selectedAssignees,
                                    tags = tags,
                                    estimatedHours = estimatedHoursInput.toFloatOrNull()
                                )
                            )
                        }
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
        KosmosDialogSurface(
            onDismissRequest = { showProjectPicker = false }
        ) {
            Text(
                text = "Select Project",
                style = KosmosDialogDefaults.titleStyle,
                color = KosmosDialogDefaults.titleColor
            )

            HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                availableProjects.forEach { project ->
                    Surface(
                        onClick = {
                            selectedProjectId = project.id
                            selectedProjectName = project.name
                            showProjectPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md),
                        color = if (selectedProjectId == project.id)
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                        else
                            ColorTokens.ReactTheme.secondary
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.padding(Tokens.Spacing.md)
                        )
                    }
                }
            }
        }
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
        var tempSelected by remember { mutableStateOf(selectedAssignees.firstOrNull()) }

        KosmosDialogSurface(
            onDismissRequest = { showAssigneePicker = false }
        ) {
            Text(
                text = "Assign To",
                style = KosmosDialogDefaults.titleStyle,
                color = KosmosDialogDefaults.titleColor
            )

            HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                availableAssignees.forEach { assignee ->
                    Surface(
                        onClick = {
                            tempSelected = if (tempSelected == assignee.id) null else assignee.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md),
                        color = if (tempSelected == assignee.id)
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                        else
                            ColorTokens.ReactTheme.secondary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Tokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            RadioButton(
                                selected = tempSelected == assignee.id,
                                onClick = {
                                    tempSelected = if (tempSelected == assignee.id) null else assignee.id
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ColorTokens.ReactTheme.primary,
                                    unselectedColor = ColorTokens.ReactTheme.border
                                )
                            )
                            Text(
                                text = assignee.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = { showAssigneePicker = false },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Done",
                    onClick = {
                        selectedAssignees = listOfNotNull(tempSelected)
                        showAssigneePicker = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
        color = if (isSelected) color.copy(alpha = 0.2f) else ColorTokens.ReactTheme.secondary,
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
                color = if (isSelected) color else ColorTokens.ReactTheme.mutedForeground
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
        color = ColorTokens.ReactTheme.secondary
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
                contentDescription = "",
                tint = if (isValueSet)
                    ColorTokens.ReactTheme.primary
                else
                    ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = TypographyTokens.Custom.caption,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isValueSet)
                        ColorTokens.ReactTheme.foreground
                    else
                        ColorTokens.ReactTheme.mutedForeground
                )
            }

            Icon(
                imageVector = IconSet.Direction.right,
                contentDescription = "",
                tint = ColorTokens.ReactTheme.mutedForeground,
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
    val assigneeIds: List<String>,
    val tags: List<String> = emptyList(),
    val estimatedHours: Float? = null
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
