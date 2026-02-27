package com.example.kosmos.features.tasks.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.components.StandardCard
import com.example.kosmos.shared.ui.components.PrimaryButton
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.components.IconButtonStandard
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    projectId: String,              // Primary parameter - tasks belong to projects
    chatRoomId: String? = null,     // Optional filter - for chat-scoped task view
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, chatRoomId) {
        if (chatRoomId != null) {
            taskViewModel.loadTasks(chatRoomId)        // Chat-scoped view
        } else {
            taskViewModel.loadTasksForProject(projectId)  // Project-wide view
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "To Do", "In Progress", "Done")
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message if any
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            taskViewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
            title = { Text("Task Board") },
            navigationIcon = {
                IconButtonStandard(
                    icon = IconSet.Navigation.back,
                    onClick = onNavigateBack,
                    contentDescription = "Back"
                )
            },
            actions = {
                IconButtonStandard(
                    icon = IconSet.Action.add,
                    onClick = { taskViewModel.showCreateTaskDialog() },
                    contentDescription = "Add Task"
                )
            }
        )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

        // Tab Row
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        // Update filter based on selected tab
                        when (index) {
                            0 -> taskViewModel.filterTasksByStatus(null) // All
                            1 -> taskViewModel.filterTasksByStatus(TaskStatus.TODO)
                            2 -> taskViewModel.filterTasksByStatus(TaskStatus.IN_PROGRESS)
                            3 -> taskViewModel.filterTasksByStatus(TaskStatus.DONE)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        // Filter chip row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            FilterChip(
                selected = uiState.showOnlyMyTasks,
                onClick = { taskViewModel.toggleMyTasksFilter() },
                label = { Text("My Tasks") },
                leadingIcon = if (uiState.showOnlyMyTasks) {
                    { Icon(IconSet.Status.success, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall)) }
                } else null
            )
        }

        // Task List with filters
        val filteredTasks = uiState.tasks
            .let { tasks ->
                // Filter by status (tab)
                when (selectedTab) {
                    1 -> tasks.filter { it.status == TaskStatus.TODO }
                    2 -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
                    3 -> tasks.filter { it.status == TaskStatus.DONE }
                    else -> tasks
                }
            }
            .let { tasks ->
                // Filter by "My Tasks"
                if (uiState.showOnlyMyTasks && uiState.currentUserId != null) {
                    tasks.filter { it.assignedToId == uiState.currentUserId }
                } else {
                    tasks
                }
            }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            items(filteredTasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { taskViewModel.showEditTaskDialog(task) },
                    onStatusChange = { newStatus ->
                        taskViewModel.updateTaskStatus(task.id, newStatus)
                    }
                )
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Task,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No tasks found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tasks will appear here when created",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
        }

        // Create Task Dialog
        if (uiState.showCreateTaskDialog) {
            CreateTaskDialog(
                title = uiState.createTaskTitle,
                description = uiState.createTaskDescription,
                priority = uiState.createTaskPriority,
                dueDate = uiState.createTaskDueDate,
                assignedTo = uiState.createTaskAssignedTo,
                tags = uiState.createTaskTags,
                estimatedHours = uiState.createTaskEstimatedHours,
                actualHours = uiState.createTaskActualHours,
                availableUsers = uiState.availableUsersForAssignment,
                onTitleChange = taskViewModel::updateCreateTaskTitle,
                onDescriptionChange = taskViewModel::updateCreateTaskDescription,
                onPriorityChange = taskViewModel::updateCreateTaskPriority,
                onDueDateChange = taskViewModel::updateCreateTaskDueDate,
                onAssignedToChange = taskViewModel::updateCreateTaskAssignedTo,
                onAddTag = taskViewModel::addCreateTaskTag,
                onRemoveTag = taskViewModel::removeCreateTaskTag,
                onEstimatedHoursChange = taskViewModel::updateCreateTaskEstimatedHours,
                onActualHoursChange = taskViewModel::updateCreateTaskActualHours,
                onCreateTask = {
                    // Require projectId from state - should be loaded by loadTasks()
                    val projectId = uiState.currentProjectId
                    if (projectId.isNullOrBlank()) {
                        // Show error to user via Snackbar (handled by LaunchedEffect above)
                        taskViewModel.setError("Cannot create task: Project context not found. Please try again.")
                        android.util.Log.e("TaskBoardScreen", "Cannot create task: projectId is null")
                        return@CreateTaskDialog
                    }

                    taskViewModel.createTask(
                        projectId = projectId,
                        chatRoomId = chatRoomId,
                        title = uiState.createTaskTitle,
                        description = uiState.createTaskDescription,
                        priority = uiState.createTaskPriority,
                        assignedToId = uiState.createTaskAssignedTo?.id,
                        dueDate = uiState.createTaskDueDate,
                        tags = uiState.createTaskTags,
                        estimatedHours = uiState.createTaskEstimatedHours,
                        actualHours = uiState.createTaskActualHours
                    )
                },
                onDismiss = taskViewModel::hideCreateTaskDialog
            )
        }

        // Edit Task Dialog
        if (uiState.showEditTaskDialog && uiState.editingTask != null) {
            uiState.editingTask?.let { editingTask ->
                EditTaskDialog(
                    task = editingTask,
                title = uiState.createTaskTitle,
                description = uiState.createTaskDescription,
                priority = uiState.createTaskPriority,
                status = uiState.createTaskStatus,
                dueDate = uiState.createTaskDueDate,
                assignedTo = uiState.createTaskAssignedTo,
                tags = uiState.createTaskTags,
                estimatedHours = uiState.createTaskEstimatedHours,
                actualHours = uiState.createTaskActualHours,
                parentTaskId = uiState.createTaskParentTaskId,
                availableUsers = uiState.availableUsersForAssignment,
                onTitleChange = taskViewModel::updateCreateTaskTitle,
                onDescriptionChange = taskViewModel::updateCreateTaskDescription,
                onPriorityChange = taskViewModel::updateCreateTaskPriority,
                onStatusChange = taskViewModel::updateCreateTaskStatus,
                onDueDateChange = taskViewModel::updateCreateTaskDueDate,
                onAssignedToChange = taskViewModel::updateCreateTaskAssignedTo,
                onAddTag = taskViewModel::addCreateTaskTag,
                onRemoveTag = taskViewModel::removeCreateTaskTag,
                onEstimatedHoursChange = taskViewModel::updateCreateTaskEstimatedHours,
                onActualHoursChange = taskViewModel::updateCreateTaskActualHours,
                onParentTaskIdChange = taskViewModel::updateCreateTaskParentTaskId,
                onSaveTask = {
                    taskViewModel.editTask(
                        taskId = editingTask.id,
                        title = uiState.createTaskTitle,
                        description = uiState.createTaskDescription,
                        priority = uiState.createTaskPriority,
                        status = uiState.createTaskStatus,
                        assignedToId = uiState.createTaskAssignedTo?.id,
                        dueDate = uiState.createTaskDueDate,
                        tags = uiState.createTaskTags,
                        estimatedHours = uiState.createTaskEstimatedHours,
                        actualHours = uiState.createTaskActualHours,
                        parentTaskId = uiState.createTaskParentTaskId
                    )
                },
                onDismiss = taskViewModel::hideEditTaskDialog,
                onDelete = {
                    taskViewModel.showDeleteConfirmation(editingTask)
                    taskViewModel.hideEditTaskDialog()
                },
                onAddComment = { content ->
                    taskViewModel.addComment(editingTask.id, content)
                }
            )
            }
        }

        // Delete confirmation dialog
        if (uiState.showDeleteConfirmation && uiState.taskToDelete != null) {
            uiState.taskToDelete?.let { taskToDelete ->
                AlertDialog(
                    onDismissRequest = { taskViewModel.hideDeleteConfirmation() },
                    title = { Text("Delete Task?") },
                    text = {
                        Text("Are you sure you want to delete \"${taskToDelete.title}\"? This cannot be undone.")
                    },
                confirmButton = {
                    TextButton(
                        onClick = { taskViewModel.confirmDeleteTask() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskViewModel.hideDeleteConfirmation() }) {
                        Text("Cancel")
                    }
                }
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    title: String,
    description: String,
    priority: TaskPriority,
    dueDate: Long?,
    assignedTo: com.example.kosmos.core.models.User?,
    tags: List<String>,
    estimatedHours: Float?,
    actualHours: Float?,
    availableUsers: List<com.example.kosmos.core.models.User>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onAssignedToChange: (com.example.kosmos.core.models.User?) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onEstimatedHoursChange: (Float?) -> Unit,
    onActualHoursChange: (Float?) -> Unit,
    onCreateTask: () -> Unit,
    onDismiss: () -> Unit
) {
    var showUserPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md)
                .padding(bottom = Tokens.Spacing.md)
        ) {
            // Title
            Text(
                text = "Create New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Tokens.Spacing.md)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                // Task Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                // Priority
                item {
                    Column {
                        Text(
                            "Priority",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TaskPriority.values().forEach { priorityOption ->
                                FilterChip(
                                    onClick = { onPriorityChange(priorityOption) },
                                    label = { Text(priorityOption.name) },
                                    selected = priority == priorityOption
                                )
                            }
                        }
                    }
                }

                // Assign To
                item {
                    Column {
                        Text(
                            "Assign To",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showUserPicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Tokens.Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (assignedTo != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                                    ) {
                                        Icon(IconSet.User.person, contentDescription = null)
                                        Text(assignedTo.displayName ?: assignedTo.email)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                                    ) {
                                        Icon(IconSet.User.person, contentDescription = null)
                                        Text("Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(IconSet.Direction.down, contentDescription = null)
                            }
                        }
                    }
                }

                // Due Date
                item {
                    Column {
                        Text(
                            "Due Date",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Tokens.Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                                ) {
                                    Icon(IconSet.Task.calendar, contentDescription = null)
                                    Text(
                                        text = dueDate?.let {
                                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                .format(java.util.Date(it))
                                        } ?: "No due date",
                                        color = if (dueDate == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (dueDate != null) {
                                    IconButtonStandard(
                                        icon = IconSet.Action.clear,
                                        onClick = { onDueDateChange(null) },
                                        contentDescription = "Clear date"
                                    )
                                }
                            }
                        }
                    }
                }

                // Tags
                item {
                    Column {
                        Text(
                            "Tags",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )

                        // Tag input
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Add tag") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (tagInput.isNotBlank()) {
                                    IconButtonStandard(
                                        icon = IconSet.Action.add,
                                        onClick = {
                                            onAddTag(tagInput)
                                            tagInput = ""
                                        },
                                        contentDescription = "Add tag"
                                    )
                                }
                            }
                        )

                        // Display tags as chips
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                            ) {
                                tags.forEach { tag ->
                                    AssistChip(
                                        onClick = { onRemoveTag(tag) },
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            Icon(
                                                IconSet.Navigation.close,
                                                contentDescription = "Remove tag",
                                                modifier = Modifier.size(Tokens.Size.iconSmall)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Time Tracking
                item {
                    Column {
                        Text(
                            "Time Tracking",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            // Estimated Hours
                            OutlinedTextField(
                                value = estimatedHours?.toString() ?: "",
                                onValueChange = { value ->
                                    onEstimatedHoursChange(value.toFloatOrNull())
                                },
                                label = { Text("Estimated (hrs)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(IconSet.Time.timer, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                                }
                            )
                            // Actual Hours
                            OutlinedTextField(
                                value = actualHours?.toString() ?: "",
                                onValueChange = { value ->
                                    onActualHoursChange(value.toFloatOrNull())
                                },
                                label = { Text("Actual (hrs)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(IconSet.Time.clock, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                                }
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Tokens.Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Create Task",
                    onClick = onCreateTask,
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank()
                )
            }
        }
    }

    // User Picker Bottom Sheet
    if (showUserPicker) {
        ModalBottomSheet(
            onDismissRequest = { showUserPicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Assign To",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Unassigned option
                ListItem(
                    headlineContent = { Text("Unassigned") },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onAssignedToChange(null)
                        showUserPicker = false
                    }
                )

                HorizontalDivider()

                // Available users
                LazyColumn {
                    items(availableUsers) { user ->
                        ListItem(
                            headlineContent = { Text(user.displayName ?: user.email) },
                            supportingContent = { Text(user.email) },
                            leadingContent = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                onAssignedToChange(user)
                                showUserPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Date Picker Dialog - Using Material DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDueDateChange(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    title: String,
    description: String,
    priority: TaskPriority,
    status: TaskStatus,
    dueDate: Long?,
    assignedTo: com.example.kosmos.core.models.User?,
    tags: List<String>,
    estimatedHours: Float?,
    actualHours: Float?,
    parentTaskId: String?,
    availableUsers: List<com.example.kosmos.core.models.User>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onAssignedToChange: (com.example.kosmos.core.models.User?) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onEstimatedHoursChange: (Float?) -> Unit,
    onActualHoursChange: (Float?) -> Unit,
    onParentTaskIdChange: (String?) -> Unit,
    onSaveTask: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAddComment: (String) -> Unit
) {
    // Reuse same logic as CreateTaskDialog but with "Save" button
    var showUserPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md)
                .padding(bottom = Tokens.Spacing.md)
        ) {
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Tokens.Spacing.md)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }

                item {
                    Column {
                        Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                            TaskPriority.values().forEach { p ->
                                FilterChip(
                                    onClick = { onPriorityChange(p) },
                                    label = { Text(p.name) },
                                    selected = priority == p
                                )
                            }
                        }
                    }
                }

                // Status dropdown
                item {
                    Column {
                        Text("Status", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                            TaskStatus.values().forEach { s ->
                                FilterChip(
                                    onClick = { onStatusChange(s) },
                                    label = {
                                        Text(when (s) {
                                            TaskStatus.TODO -> "To Do"
                                            TaskStatus.IN_PROGRESS -> "In Progress"
                                            TaskStatus.DONE -> "Done"
                                            TaskStatus.CANCELLED -> "Cancelled"
                                        })
                                    },
                                    selected = status == s,
                                    leadingIcon = if (status == s) {
                                        { Icon(IconSet.Action.check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Assign To", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showUserPicker = true }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(Tokens.Spacing.md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (assignedTo != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)) {
                                        Icon(IconSet.User.person, null)
                                        Text(assignedTo.displayName ?: assignedTo.email)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)) {
                                        Icon(IconSet.User.person, null)
                                        Text("Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(IconSet.Direction.down, null)
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Due Date", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showDatePicker = true }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(Tokens.Spacing.md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)) {
                                    Icon(IconSet.Task.calendar, null)
                                    Text(dueDate?.let { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "No due date", color = if (dueDate == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                }
                                if (dueDate != null) {
                                    IconButtonStandard(
                                        icon = IconSet.Action.clear,
                                        onClick = { onDueDateChange(null) },
                                        contentDescription = "Clear date"
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Tags", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Add tag") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (tagInput.isNotBlank()) {
                                    IconButtonStandard(
                                        icon = IconSet.Action.add,
                                        onClick = { onAddTag(tagInput); tagInput = "" },
                                        contentDescription = "Add tag"
                                    )
                                }
                            }
                        )
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                                tags.forEach { tag ->
                                    AssistChip(onClick = { onRemoveTag(tag) }, label = { Text(tag) }, trailingIcon = { Icon(IconSet.Navigation.close, "Remove", modifier = Modifier.size(Tokens.Size.iconSmall)) })
                                }
                            }
                        }
                    }
                }

                // Time Tracking
                item {
                    Column {
                        Text(
                            "Time Tracking",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            // Estimated Hours
                            OutlinedTextField(
                                value = estimatedHours?.toString() ?: "",
                                onValueChange = { value ->
                                    onEstimatedHoursChange(value.toFloatOrNull())
                                },
                                label = { Text("Estimated (hrs)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(IconSet.Time.timer, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                                }
                            )
                            // Actual Hours
                            OutlinedTextField(
                                value = actualHours?.toString() ?: "",
                                onValueChange = { value ->
                                    onActualHoursChange(value.toFloatOrNull())
                                },
                                label = { Text("Actual (hrs)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(IconSet.Time.clock, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                                }
                            )
                        }
                    }
                }

                // Parent Task (for subtasks)
                item {
                    var showParentTaskPicker by remember { mutableStateOf(false) }
                    val availableTasks = task.let { currentTask ->
                        // Don't show current task or its subtasks as potential parents (to avoid circular refs)
                        availableUsers.isEmpty().let {
                            // TODO: Get tasks from ViewModel - for now show placeholder
                            emptyList<Task>()
                        }
                    }

                    Column {
                        Text("Parent Task (Optional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = Tokens.Spacing.xs))
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showParentTaskPicker = true }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(Tokens.Spacing.md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)) {
                                    Icon(IconSet.Task.list, null)
                                    Text(
                                        if (parentTaskId != null) "Subtask of ${parentTaskId.take(8)}..." else "No parent task",
                                        color = if (parentTaskId == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (parentTaskId != null) {
                                    IconButtonStandard(
                                        icon = IconSet.Action.clear,
                                        onClick = { onParentTaskIdChange(null) },
                                        contentDescription = "Clear parent task"
                                    )
                                } else {
                                    Icon(IconSet.Direction.down, null)
                                }
                            }
                        }
                    }

                    // Parent task picker dialog (simplified - just clear for now)
                    if (showParentTaskPicker) {
                        AlertDialog(
                            onDismissRequest = { showParentTaskPicker = false },
                            title = { Text("Parent Task") },
                            text = { Text("Subtask feature is ready in backend. Task list UI coming soon!") },
                            confirmButton = {
                                TextButton(onClick = { showParentTaskPicker = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }

                // Comments section
                item {
                    var commentInput by remember { mutableStateOf("") }
                    var showComments by remember { mutableStateOf(false) }

                    Column {
                        OutlinedButton(
                            onClick = { showComments = !showComments },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                            Text(if (showComments) "Hide Comments (${task.comments.size})" else "Show Comments (${task.comments.size})")
                        }

                        if (showComments) {
                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

                            // Comments list
                            if (task.comments.isNotEmpty()) {
                                task.comments.sortedByDescending { it.timestamp }.forEach { comment ->
                                    StandardCard(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = Tokens.Spacing.xxs)
                                    ) {
                                        Column(modifier = Modifier.padding(Tokens.Spacing.sm)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = comment.authorName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                                        .format(java.util.Date(comment.timestamp)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))
                                            Text(
                                                text = comment.content,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No comments yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = Tokens.Spacing.xs)
                                )
                            }

                            // Add comment input
                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                label = { Text("Add a comment") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                trailingIcon = {
                                    if (commentInput.isNotBlank()) {
                                        IconButtonStandard(
                                            icon = IconSet.Message.send,
                                            onClick = {
                                                onAddComment(commentInput)
                                                commentInput = ""
                                            },
                                            contentDescription = "Send comment"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = Tokens.Spacing.md)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                    SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    PrimaryButton(text = "Save", onClick = onSaveTask, modifier = Modifier.weight(1f), enabled = title.isNotBlank())
                }
                Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(IconSet.Action.delete, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                    Text("Delete Task")
                }
            }
        }
    }

    if (showUserPicker) {
        ModalBottomSheet(onDismissRequest = { showUserPicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(Tokens.Spacing.md)) {
                Text("Assign To", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = Tokens.Spacing.md))
                ListItem(headlineContent = { Text("Unassigned") }, leadingContent = { Icon(IconSet.User.person, null) }, modifier = Modifier.clickable { onAssignedToChange(null); showUserPicker = false })
                HorizontalDivider()
                LazyColumn {
                    items(availableUsers) { user ->
                        ListItem(headlineContent = { Text(user.displayName ?: user.email) }, supportingContent = { Text(user.email) }, leadingContent = { Icon(IconSet.User.person, null) }, modifier = Modifier.clickable { onAssignedToChange(user); showUserPicker = false })
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { onDueDateChange(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate due date status
    val now = System.currentTimeMillis()
    val isOverdue = task.dueDate != null && task.dueDate!! < now && task.status != TaskStatus.DONE
    val isDueSoon = task.dueDate != null && task.dueDate!! > now && (task.dueDate!! - now) < TimeUnit.HOURS.toMillis(24)

    StandardCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Completion checkbox
                Checkbox(
                    checked = task.status == TaskStatus.DONE,
                    onCheckedChange = { isChecked ->
                        val newStatus = if (isChecked) TaskStatus.DONE else TaskStatus.TODO
                        onStatusChange(newStatus)
                    },
                    modifier = Modifier.padding(end = Tokens.Spacing.xs)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                        Text(
                            text = task.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Priority badge with icon and color
                Surface(
                    color = when (task.priority) {
                        TaskPriority.LOW -> Color(0xFF2196F3).copy(alpha = 0.15f) // Blue
                        TaskPriority.MEDIUM -> Color(0xFFFFC107).copy(alpha = 0.15f) // Amber
                        TaskPriority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.15f) // Orange
                        TaskPriority.URGENT -> Color(0xFFF44336).copy(alpha = 0.15f) // Red
                    },
                    shape = RoundedCornerShape(Tokens.CornerRadius.xs),
                    modifier = Modifier.padding(start = Tokens.Spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Tokens.Spacing.xs, vertical = Tokens.Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                    ) {
                        Text(
                            text = when (task.priority) {
                                TaskPriority.LOW -> "🔵"
                                TaskPriority.MEDIUM -> "🟡"
                                TaskPriority.HIGH -> "🟠"
                                TaskPriority.URGENT -> "🔴"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (task.priority) {
                                TaskPriority.LOW -> Color(0xFF2196F3)
                                TaskPriority.MEDIUM -> Color(0xFFFFC107)
                                TaskPriority.HIGH -> Color(0xFFFF9800)
                                TaskPriority.URGENT -> Color(0xFFF44336)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip with color coding
                Surface(
                    color = when (task.status) {
                        TaskStatus.TODO -> Color(0xFF2196F3).copy(alpha = 0.15f) // Blue
                        TaskStatus.IN_PROGRESS -> Color(0xFFFFC107).copy(alpha = 0.15f) // Amber
                        TaskStatus.DONE -> Color(0xFF4CAF50).copy(alpha = 0.15f) // Green
                        TaskStatus.CANCELLED -> Color(0xFF9E9E9E).copy(alpha = 0.15f) // Gray
                    },
                    shape = RoundedCornerShape(Tokens.CornerRadius.md)
                ) {
                    Text(
                        text = when (task.status) {
                            TaskStatus.TODO -> "To Do"
                            TaskStatus.IN_PROGRESS -> "In Progress"
                            TaskStatus.DONE -> "Done"
                            TaskStatus.CANCELLED -> "Cancelled"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (task.status) {
                            TaskStatus.TODO -> Color(0xFF2196F3)
                            TaskStatus.IN_PROGRESS -> Color(0xFFFFC107)
                            TaskStatus.DONE -> Color(0xFF4CAF50)
                            TaskStatus.CANCELLED -> Color(0xFF9E9E9E)
                        },
                        modifier = Modifier.padding(horizontal = Tokens.Spacing.sm, vertical = Tokens.Spacing.xs)
                    )
                }

                // Assignee with icon
                if (task.assignedToName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                    ) {
                        Icon(
                            IconSet.User.person,
                            contentDescription = null,
                            modifier = Modifier.size(Tokens.Size.iconSmall),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = task.assignedToName ?: "Unassigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Due date and tags with warnings
            if (task.dueDate != null || task.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due date with color warnings
                    if (task.dueDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                        ) {
                            Icon(
                                IconSet.Time.schedule,
                                contentDescription = null,
                                modifier = Modifier.size(Tokens.Size.iconSmall),
                                tint = when {
                                    isOverdue -> Color(0xFFF44336) // Red
                                    isDueSoon -> Color(0xFFFF9800) // Amber
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                            Text(
                                text = task.dueDate?.let { dueDate ->
                                    val dateStr = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                        .format(java.util.Date(dueDate))
                                    when {
                                        isOverdue -> "⚠️ OVERDUE: $dateStr"
                                        isDueSoon -> "⏰ Due Soon: $dateStr"
                                        else -> dateStr
                                    }
                                } ?: "No due date",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isOverdue || isDueSoon) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isOverdue -> Color(0xFFF44336) // Red
                                    isDueSoon -> Color(0xFFFF9800) // Amber
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }

                    // Tags with color-coded chips
                    if (task.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            task.tags.take(2).forEach { tag ->
                                Surface(
                                    color = Color(0xFF9C27B0).copy(alpha = 0.15f), // Purple
                                    shape = RoundedCornerShape(Tokens.CornerRadius.xs)
                                ) {
                                    Text(
                                        text = "🏷️ $tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF9C27B0),
                                        modifier = Modifier.padding(horizontal = Tokens.Spacing.xs, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (task.tags.size > 2) {
                                Text(
                                    text = "+${task.tags.size - 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9C27B0)
                                )
                            }
                        }
                    }
                }
            }

            // Time tracking display
            if (task.estimatedHours != null || task.actualHours != null) {
                Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.estimatedHours != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                        ) {
                            Icon(
                                IconSet.Time.timer,
                                contentDescription = null,
                                modifier = Modifier.size(Tokens.Size.iconSmall),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Est: ${task.estimatedHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (task.actualHours != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                        ) {
                            Icon(
                                IconSet.Time.clock,
                                contentDescription = null,
                                modifier = Modifier.size(Tokens.Size.iconSmall),
                                tint = if (task.estimatedHours != null && task.actualHours!! > task.estimatedHours!!) {
                                    Color(0xFFF44336) // Red if over estimate
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                            Text(
                                text = "Actual: ${task.actualHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.estimatedHours != null && task.actualHours!! > task.estimatedHours!!) {
                                    Color(0xFFF44336) // Red if over estimate
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                },
                                fontWeight = if (task.estimatedHours != null && task.actualHours!! > task.estimatedHours!!) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}