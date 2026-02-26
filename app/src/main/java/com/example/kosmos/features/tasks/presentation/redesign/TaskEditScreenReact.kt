package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Task Edit Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/tasks/TaskEditScreen.tsx
 *
 * Features:
 * - Form for creating/editing tasks
 * - Top app bar with back button + title + save button
 * - Input fields: Title, Description, Status, Priority, Due Date, Project, Assignee
 * - Status selector: 3-button grid (TODO/IN_PROGRESS/DONE)
 * - Priority selector: 3-button grid (LOW/MEDIUM/HIGH)
 * - Delete button (only for existing tasks)
 * - All inputs with card bg, border, rounded-xl, focus ring
 *
 * All styling matches React design exactly:
 * - Colors from ColorTokens.ReactTheme.*
 * - Input fields: card bg, border, rounded-xl (12dp), focus ring
 * - Button grids: 3 columns with selected state (primary bg)
 * - Labels: 14sp, medium weight, muted color
 * - Input text: 15sp
 * - NO backend wiring (mock data only)
 */

enum class TaskStatusEdit {
    TODO, IN_PROGRESS, DONE
}

enum class TaskPriorityEdit {
    LOW, MEDIUM, HIGH
}

data class TaskEditFormData(
    val title: String = "",
    val description: String = "",
    val status: TaskStatusEdit = TaskStatusEdit.TODO,
    val priority: TaskPriorityEdit = TaskPriorityEdit.MEDIUM,
    val dueDate: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val parentTaskId: String? = null,
    val parentTaskTitle: String? = null,
    val tags: List<String> = emptyList(),
    val estimatedHours: Float? = null
)

@Composable
fun TaskEditScreenReact(
    taskId: String? = null,
    initialData: TaskEditFormData = TaskEditFormData(),
    projects: List<Pair<String, String>> = emptyList(), // List of (id, name) pairs
    assignees: List<Pair<String, String>> = emptyList(), // List of (id, name) pairs
    availableTasks: List<Pair<String, String>> = emptyList(), // List of (id, title) pairs for parent task picker
    onBack: () -> Unit = {},
    onSave: (TaskEditFormData) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isNewTask = taskId == null

    // State for form fields
    var title by remember { mutableStateOf(initialData.title) }
    var description by remember { mutableStateOf(initialData.description) }
    var status by remember { mutableStateOf(initialData.status) }
    var priority by remember { mutableStateOf(initialData.priority) }
    var dueDate by remember { mutableStateOf(initialData.dueDate) }
    var selectedProjectId by remember { mutableStateOf(initialData.projectId) }
    var selectedProjectName by remember { mutableStateOf(initialData.projectName) }
    var selectedAssigneeId by remember { mutableStateOf(initialData.assigneeId) }
    var selectedAssigneeName by remember { mutableStateOf(initialData.assigneeName) }
    var selectedParentTaskId by remember { mutableStateOf(initialData.parentTaskId) }
    var selectedParentTaskTitle by remember { mutableStateOf(initialData.parentTaskTitle) }
    var tags by remember { mutableStateOf(initialData.tags) }
    var tagInput by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf(initialData.estimatedHours?.toString() ?: "") }

    // Extract names for dropdowns
    val projectNames = projects.map { it.second }
    val assigneeNames = assignees.map { it.second }
    val taskTitles = availableTasks.map { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = if (isNewTask) "New Task" else "Edit Task",
            onBack = onBack,
            onSave = {
                // Package form data and pass to callback
                onSave(
                    TaskEditFormData(
                        title = title,
                        description = description,
                        status = status,
                        priority = priority,
                        dueDate = dueDate,
                        projectId = selectedProjectId,
                        projectName = selectedProjectName,
                        assigneeId = selectedAssigneeId,
                        assigneeName = selectedAssigneeName,
                        parentTaskId = selectedParentTaskId,
                        parentTaskTitle = selectedParentTaskTitle,
                        tags = tags,
                        estimatedHours = estimatedHours.toFloatOrNull()
                    )
                )
            }
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Title field
            item {
                InputField(
                    label = "Title",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Enter task title"
                )
            }

            // Description field
            item {
                TextAreaField(
                    label = "Description",
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Enter task description",
                    rows = 4
                )
            }

            // Status selector
            item {
                StatusSelector(
                    label = "Status",
                    selectedStatus = status,
                    onStatusChange = { status = it }
                )
            }

            // Priority selector
            item {
                PrioritySelector(
                    label = "Priority",
                    selectedPriority = priority,
                    onPriorityChange = { priority = it }
                )
            }

            // Due Date field
            item {
                DateField(
                    label = "Due Date",
                    value = dueDate,
                    onValueChange = { dueDate = it }
                )
            }

            // Project dropdown
            item {
                DropdownField(
                    label = "Project",
                    selectedValue = selectedProjectName,
                    options = projectNames,
                    onValueChange = { name ->
                        selectedProjectName = name
                        // Find and set the ID
                        selectedProjectId = projects.find { it.second == name }?.first ?: ""
                    }
                )
            }

            // Assignee dropdown
            item {
                DropdownField(
                    label = "Assignee",
                    selectedValue = selectedAssigneeName ?: "Unassigned",
                    options = listOf("Unassigned") + assigneeNames,
                    onValueChange = { name ->
                        if (name == "Unassigned") {
                            selectedAssigneeName = null
                            selectedAssigneeId = null
                        } else {
                            selectedAssigneeName = name
                            // Find and set the ID
                            selectedAssigneeId = assignees.find { it.second == name }?.first
                        }
                    }
                )
            }

            // Parent Task dropdown (for subtasks)
            if (availableTasks.isNotEmpty()) {
                item {
                    DropdownField(
                        label = "Parent Task (Optional)",
                        selectedValue = selectedParentTaskTitle ?: "None",
                        options = listOf("None") + taskTitles,
                        onValueChange = { title ->
                            if (title == "None") {
                                selectedParentTaskTitle = null
                                selectedParentTaskId = null
                            } else {
                                selectedParentTaskTitle = title
                                // Find and set the ID
                                selectedParentTaskId = availableTasks.find { it.second == title }?.first
                            }
                        }
                    )
                }
            }

            // Tags field
            item {
                TagsField(
                    tags = tags,
                    tagInput = tagInput,
                    onTagInputChange = { tagInput = it },
                    onAddTag = {
                        val trimmed = tagInput.trim()
                        if (trimmed.isNotEmpty() && trimmed !in tags) {
                            tags = tags + trimmed
                            tagInput = ""
                        }
                    },
                    onRemoveTag = { tag -> tags = tags - tag }
                )
            }

            // Estimated Hours field
            item {
                InputField(
                    label = "Estimated Hours",
                    value = estimatedHours,
                    onValueChange = { value ->
                        // Only allow numeric input with optional decimal
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            estimatedHours = value
                        }
                    },
                    placeholder = "e.g. 4.5"
                )
            }

            // Delete button (only for existing tasks)
            if (!isNewTask) {
                item {
                    DeleteButton(onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun TopAppBar(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button + title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Right: Save button
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary,
                    contentColor = ColorTokens.ReactTheme.primaryForeground
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    HorizontalDivider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 15.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                color = ColorTokens.ReactTheme.foreground
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorTokens.ReactTheme.card,
                unfocusedContainerColor = ColorTokens.ReactTheme.card,
                focusedBorderColor = ColorTokens.ReactTheme.primary,
                unfocusedBorderColor = ColorTokens.ReactTheme.border,
                cursorColor = ColorTokens.ReactTheme.primary,
                focusedTextColor = ColorTokens.ReactTheme.foreground,
                unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                focusedLabelColor = ColorTokens.ReactTheme.primary,
                unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
            ),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            singleLine = true
        )
    }
}

@Composable
private fun TextAreaField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    rows: Int = 4
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height((rows * 24 + 48).dp),  // Approximate height for rows
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 15.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                color = ColorTokens.ReactTheme.foreground
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorTokens.ReactTheme.card,
                unfocusedContainerColor = ColorTokens.ReactTheme.card,
                focusedBorderColor = ColorTokens.ReactTheme.primary,
                unfocusedBorderColor = ColorTokens.ReactTheme.border,
                cursorColor = ColorTokens.ReactTheme.primary,
                focusedTextColor = ColorTokens.ReactTheme.foreground,
                unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                focusedLabelColor = ColorTokens.ReactTheme.primary,
                unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
            ),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            minLines = rows,
            maxLines = rows
        )
    }
}

@Composable
private fun StatusSelector(
    label: String,
    selectedStatus: TaskStatusEdit,
    onStatusChange: (TaskStatusEdit) -> Unit
) {
    val statuses = listOf(
        TaskStatusEdit.TODO to "To Do",
        TaskStatusEdit.IN_PROGRESS to "In Progress",
        TaskStatusEdit.DONE to "Done"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (status, displayName) ->
                SelectableButton(
                    text = displayName,
                    selected = selectedStatus == status,
                    onClick = { onStatusChange(status) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrioritySelector(
    label: String,
    selectedPriority: TaskPriorityEdit,
    onPriorityChange: (TaskPriorityEdit) -> Unit
) {
    val priorities = listOf(
        TaskPriorityEdit.LOW to "Low",
        TaskPriorityEdit.MEDIUM to "Medium",
        TaskPriorityEdit.HIGH to "High"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            priorities.forEach { (priority, displayName) ->
                SelectableButton(
                    text = displayName,
                    selected = selectedPriority == priority,
                    onClick = { onPriorityChange(priority) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SelectableButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = if (selected) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.card,
                shape = RoundedCornerShape(Tokens.CornerRadius.md)
            )
            .border(
                width = 1.dp,
                color = if (selected) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.border,
                shape = RoundedCornerShape(Tokens.CornerRadius.md)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) ColorTokens.ReactTheme.primaryForeground else ColorTokens.ReactTheme.foreground
        )
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                color = ColorTokens.ReactTheme.foreground
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorTokens.ReactTheme.card,
                unfocusedContainerColor = ColorTokens.ReactTheme.card,
                focusedBorderColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
                unfocusedBorderColor = ColorTokens.ReactTheme.border,
                cursorColor = ColorTokens.ReactTheme.primary
            ),
            shape = RoundedCornerShape(Tokens.CornerRadius.md),
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select date",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Box {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = ColorTokens.ReactTheme.foreground
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ColorTokens.ReactTheme.card,
                    unfocusedContainerColor = ColorTokens.ReactTheme.card,
                    focusedBorderColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
                    unfocusedBorderColor = ColorTokens.ReactTheme.border,
                    disabledContainerColor = ColorTokens.ReactTheme.card,
                    disabledBorderColor = ColorTokens.ReactTheme.border,
                    disabledTextColor = ColorTokens.ReactTheme.foreground
                ),
                shape = RoundedCornerShape(Tokens.CornerRadius.md),
                singleLine = true,
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorTokens.ReactTheme.card)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = ColorTokens.ReactTheme.foreground
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsField(
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tags",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        // Tag chips
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag, fontSize = 13.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = ColorTokens.ReactTheme.secondary,
                            labelColor = ColorTokens.ReactTheme.foreground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = InputChipDefaults.inputChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = ColorTokens.ReactTheme.border
                        )
                    )
                }
            }
        }

        // Tag input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        ColorTokens.ReactTheme.card,
                        RoundedCornerShape(Tokens.CornerRadius.md)
                    )
                    .border(
                        1.dp,
                        ColorTokens.ReactTheme.border,
                        RoundedCornerShape(Tokens.CornerRadius.md)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = tagInput,
                    onValueChange = onTagInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = ColorTokens.ReactTheme.foreground,
                        fontSize = 15.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(ColorTokens.ReactTheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (tagInput.isEmpty()) {
                            Text(
                                "Add tag...",
                                color = ColorTokens.ReactTheme.mutedForeground,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            IconButton(
                onClick = onAddTag,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        ColorTokens.ReactTheme.primary,
                        RoundedCornerShape(Tokens.CornerRadius.md)
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add tag",
                    tint = ColorTokens.ReactTheme.primaryForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f),
            contentColor = ColorTokens.ReactTheme.destructive
        ),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.destructive.copy(alpha = 0.3f))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Delete Task",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
