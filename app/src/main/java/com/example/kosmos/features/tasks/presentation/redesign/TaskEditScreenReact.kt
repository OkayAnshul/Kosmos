package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.example.kosmos.shared.ui.components.CharacterCount
import com.example.kosmos.shared.ui.components.LoadingButton
import com.example.kosmos.shared.ui.components.SectionCard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Task Edit Screen - React Design Implementation (SectionCard redesign)
 *
 * Design Reference: documents/Kosmos/src/app/components/tasks/TaskEditScreen.tsx
 *
 * Features:
 * - TopAppBar: back arrow + "Edit Task" title + Delete icon (destructive)
 * - LazyColumn content with 16dp horizontal padding, 24dp between cards
 * - SectionCard("BASICS"): title + description with char count
 * - SectionCard("STATUS & PRIORITY"): status chips + priority chips
 * - SectionCard("DETAILS"): project, assignee, due date, estimated hours
 * - SectionCard("TIME TRACKING"): actualHours read-only display
 * - SectionCard("TAGS"): tag input + tag chips
 * - SectionCard("DEPENDENCIES"): dependency list or "(none)"
 * - Sticky bottom bar: full-width "Save Changes" LoadingButton
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
    projects: List<Pair<String, String>> = emptyList(),       // List of (id, name) pairs
    assignees: List<Pair<String, String>> = emptyList(),      // List of (id, name) pairs
    availableTasks: List<Pair<String, String>> = emptyList(), // List of (id, title) pairs for parent task picker
    actualHours: Float? = null,                               // Read-only, source of truth = time tracker
    dependencies: List<String> = emptyList(),                 // Dependency task titles (read-only display)
    onBack: () -> Unit = {},
    onSave: (TaskEditFormData) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isNewTask = taskId == null

    // Form field state
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

    // Helper: build form data snapshot
    fun buildFormData() = TaskEditFormData(
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

    // Formatted actualHours display (show minutes/seconds for small durations)
    val actualHoursText = when {
        actualHours == null -> "—"
        actualHours < 1f / 60f -> { // Less than 1 minute → show seconds
            val totalSeconds = (actualHours * 3600).toInt()
            "${totalSeconds}s"
        }
        actualHours < 1f -> { // Less than 1 hour → show minutes
            val totalMinutes = (actualHours * 60).toInt()
            val remainingSeconds = ((actualHours * 3600) % 60).toInt()
            if (remainingSeconds > 0) "${totalMinutes}m ${remainingSeconds}s" else "${totalMinutes}m"
        }
        actualHours == actualHours.toLong().toFloat() -> "${actualHours.toLong()}h"
        else -> {
            val hours = actualHours.toInt()
            val minutes = ((actualHours - hours) * 60).toInt()
            if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // ── Top App Bar ──────────────────────────────────────────────────────
        EditTopAppBar(
            title = if (isNewTask) "New Task" else "Edit Task",
            onBack = onBack,
            onDelete = if (!isNewTask) onDelete else null
        )

        // ── Scrollable content + sticky bottom bar ───────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp) // 96dp bottom pad for sticky bar
            ) {

                // ── BASICS ───────────────────────────────────────────────────
                item {
                    SectionCard(title = "BASICS") {
                        val MAX_DESCRIPTION = 500

                        // Title
                        InputField(
                            label = "Title",
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Enter task title"
                        )

                        // Description + char count
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextAreaField(
                                label = "Description",
                                value = description,
                                onValueChange = { if (it.length <= MAX_DESCRIPTION) description = it },
                                placeholder = "Enter task description",
                                rows = 4
                            )
                            CharacterCount(
                                current = description.length,
                                max = MAX_DESCRIPTION,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.End)
                                    .padding(end = 4.dp)
                            )
                        }
                    }
                }

                // ── STATUS & PRIORITY ────────────────────────────────────────
                item {
                    SectionCard(title = "STATUS & PRIORITY") {
                        StatusSelector(
                            label = "Status",
                            selectedStatus = status,
                            onStatusChange = { status = it }
                        )
                        PrioritySelector(
                            label = "Priority",
                            selectedPriority = priority,
                            onPriorityChange = { priority = it }
                        )
                    }
                }

                // ── DETAILS ──────────────────────────────────────────────────
                item {
                    SectionCard(title = "DETAILS") {
                        // Project
                        DropdownField(
                            label = "Project",
                            selectedValue = selectedProjectName,
                            options = projectNames,
                            onValueChange = { name ->
                                selectedProjectName = name
                                selectedProjectId = projects.find { it.second == name }?.first ?: ""
                            }
                        )

                        // Assignee
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
                                    selectedAssigneeId = assignees.find { it.second == name }?.first
                                }
                            }
                        )

                        // Due Date
                        var showDatePicker by remember { mutableStateOf(false) }
                        DateField(
                            label = "Due Date",
                            value = dueDate,
                            onCalendarClick = { showDatePicker = true }
                        )
                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        .parse(dueDate)?.time
                                } catch (_: Exception) { null }
                            )
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            dueDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                .format(java.util.Date(millis))
                                        }
                                        showDatePicker = false
                                    }) { Text("OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        // Estimated Hours
                        InputField(
                            label = "Estimated Hours",
                            value = estimatedHours,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    estimatedHours = value
                                }
                            },
                            placeholder = "e.g. 4.5"
                        )

                        // Parent Task (only if there are available tasks)
                        if (availableTasks.isNotEmpty()) {
                            DropdownField(
                                label = "Parent Task (Optional)",
                                selectedValue = selectedParentTaskTitle ?: "None",
                                options = listOf("None") + taskTitles,
                                onValueChange = { taskTitle ->
                                    if (taskTitle == "None") {
                                        selectedParentTaskTitle = null
                                        selectedParentTaskId = null
                                    } else {
                                        selectedParentTaskTitle = taskTitle
                                        selectedParentTaskId =
                                            availableTasks.find { it.second == taskTitle }?.first
                                    }
                                }
                            )
                        }
                    }
                }

                // ── TIME TRACKING ────────────────────────────────────────────
                item {
                    SectionCard(title = "TIME TRACKING") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Actual Hours",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                                Text(
                                    text = "Managed by time tracker",
                                    fontSize = 12.sp,
                                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = actualHoursText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }
                }

                // ── TAGS ─────────────────────────────────────────────────────
                item {
                    SectionCard(title = "TAGS") {
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
                }

                // ── DEPENDENCIES ─────────────────────────────────────────────
                item {
                    SectionCard(title = "DEPENDENCIES") {
                        if (dependencies.isEmpty()) {
                            Text(
                                text = "(none)",
                                fontSize = 14.sp,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            dependencies.forEach { dep ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountTree,
                                        contentDescription = null,
                                        tint = ColorTokens.ReactTheme.mutedForeground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = dep,
                                        fontSize = 14.sp,
                                        color = ColorTokens.ReactTheme.foreground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Sticky bottom bar ────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = ColorTokens.ReactTheme.background,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Column {
                    HorizontalDivider(
                        color = ColorTokens.ReactTheme.border,
                        thickness = 1.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        LoadingButton(
                            text = "Save Changes",
                            onClick = { onSave(buildFormData()) },
                            isLoading = false,
                            fullWidth = true
                        )
                    }
                }
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun EditTopAppBar(
    title: String,
    onBack: () -> Unit,
    onDelete: (() -> Unit)?
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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: back arrow + title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
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

            // Right: delete icon (only for existing tasks)
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = ColorTokens.ReactTheme.destructive,
                        modifier = Modifier.size(22.dp)
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
                .height((rows * 24 + 48).dp),
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
            statuses.forEach { (s, displayName) ->
                SelectableButton(
                    text = displayName,
                    selected = selectedStatus == s,
                    onClick = { onStatusChange(s) },
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
            priorities.forEach { (p, displayName) ->
                SelectableButton(
                    text = displayName,
                    selected = selectedPriority == p,
                    onClick = { onPriorityChange(p) },
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
    onCalendarClick: () -> Unit
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
            onValueChange = {},
            readOnly = true,
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
                IconButton(onClick = onCalendarClick) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select date",
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

        // Tag input row
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
