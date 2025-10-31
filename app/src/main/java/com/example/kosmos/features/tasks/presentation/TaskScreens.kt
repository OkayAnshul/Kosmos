package com.example.kosmos.features.tasks.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    chatRoomId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(chatRoomId) {
        taskViewModel.loadTasks(chatRoomId)
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "To Do", "In Progress", "Done")

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Task Board") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { taskViewModel.showCreateTaskDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        )

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.showOnlyMyTasks,
                onClick = { taskViewModel.toggleMyTasksFilter() },
                label = { Text("My Tasks") },
                leadingIcon = if (uiState.showOnlyMyTasks) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { taskViewModel.showEditTaskDialog(task) }
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

        // Show error message if any
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // TODO: Show snackbar with error
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
                availableUsers = uiState.availableUsersForAssignment,
                onTitleChange = taskViewModel::updateCreateTaskTitle,
                onDescriptionChange = taskViewModel::updateCreateTaskDescription,
                onPriorityChange = taskViewModel::updateCreateTaskPriority,
                onDueDateChange = taskViewModel::updateCreateTaskDueDate,
                onAssignedToChange = taskViewModel::updateCreateTaskAssignedTo,
                onAddTag = taskViewModel::addCreateTaskTag,
                onRemoveTag = taskViewModel::removeCreateTaskTag,
                onCreateTask = {
                    taskViewModel.createTask(
                        chatRoomId = chatRoomId,
                        title = uiState.createTaskTitle,
                        description = uiState.createTaskDescription,
                        priority = uiState.createTaskPriority,
                        assignedToId = uiState.createTaskAssignedTo?.id,
                        dueDate = uiState.createTaskDueDate,
                        tags = uiState.createTaskTags
                    )
                },
                onDismiss = taskViewModel::hideCreateTaskDialog
            )
        }

        // Edit Task Dialog
        if (uiState.showEditTaskDialog && uiState.editingTask != null) {
            EditTaskDialog(
                task = uiState.editingTask!!,
                title = uiState.createTaskTitle,
                description = uiState.createTaskDescription,
                priority = uiState.createTaskPriority,
                dueDate = uiState.createTaskDueDate,
                assignedTo = uiState.createTaskAssignedTo,
                tags = uiState.createTaskTags,
                availableUsers = uiState.availableUsersForAssignment,
                onTitleChange = taskViewModel::updateCreateTaskTitle,
                onDescriptionChange = taskViewModel::updateCreateTaskDescription,
                onPriorityChange = taskViewModel::updateCreateTaskPriority,
                onDueDateChange = taskViewModel::updateCreateTaskDueDate,
                onAssignedToChange = taskViewModel::updateCreateTaskAssignedTo,
                onAddTag = taskViewModel::addCreateTaskTag,
                onRemoveTag = taskViewModel::removeCreateTaskTag,
                onSaveTask = {
                    taskViewModel.editTask(
                        taskId = uiState.editingTask!!.id,
                        title = uiState.createTaskTitle,
                        description = uiState.createTaskDescription,
                        priority = uiState.createTaskPriority,
                        assignedToId = uiState.createTaskAssignedTo?.id,
                        dueDate = uiState.createTaskDueDate,
                        tags = uiState.createTaskTags
                    )
                },
                onDismiss = taskViewModel::hideEditTaskDialog,
                onDelete = {
                    taskViewModel.showDeleteConfirmation(uiState.editingTask!!)
                    taskViewModel.hideEditTaskDialog()
                },
                onAddComment = { content ->
                    taskViewModel.addComment(uiState.editingTask!!.id, content)
                }
            )
        }

        // Delete confirmation dialog
        if (uiState.showDeleteConfirmation && uiState.taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { taskViewModel.hideDeleteConfirmation() },
                title = { Text("Delete Task?") },
                text = {
                    Text("Are you sure you want to delete \"${uiState.taskToDelete!!.title}\"? This cannot be undone.")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    title: String,
    description: String,
    priority: TaskPriority,
    dueDate: Long?,
    assignedTo: com.example.kosmos.core.models.User?,
    tags: List<String>,
    availableUsers: List<com.example.kosmos.core.models.User>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onAssignedToChange: (com.example.kosmos.core.models.User?) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Title
            Text(
                text = "Create New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showUserPicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (assignedTo != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                        Text(assignedTo.displayName ?: assignedTo.email)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                        Text("Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                    Text(
                                        text = dueDate?.let {
                                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                                .format(java.util.Date(it))
                                        } ?: "No due date",
                                        color = if (dueDate == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (dueDate != null) {
                                    IconButton(onClick = { onDueDateChange(null) }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear date")
                                    }
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
                            modifier = Modifier.padding(bottom = 8.dp)
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
                                    IconButton(onClick = {
                                        onAddTag(tagInput)
                                        tagInput = ""
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                                    }
                                }
                            }
                        )

                        // Display tags as chips
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEach { tag ->
                                    AssistChip(
                                        onClick = { onRemoveTag(tag) },
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove tag",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onCreateTask,
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank()
                ) {
                    Text("Create Task")
                }
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

                Divider()

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
    dueDate: Long?,
    assignedTo: com.example.kosmos.core.models.User?,
    tags: List<String>,
    availableUsers: List<com.example.kosmos.core.models.User>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onAssignedToChange: (com.example.kosmos.core.models.User?) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                item {
                    Column {
                        Text("Assign To", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showUserPicker = true }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (assignedTo != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Person, null)
                                        Text(assignedTo.displayName ?: assignedTo.email)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Person, null)
                                        Text("Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Due Date", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showDatePicker = true }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.DateRange, null)
                                    Text(dueDate?.let { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "No due date", color = if (dueDate == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                }
                                if (dueDate != null) {
                                    IconButton(onClick = { onDueDateChange(null) }) {
                                        Icon(Icons.Default.Clear, "Clear date")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Tags", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Add tag") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (tagInput.isNotBlank()) {
                                    IconButton(onClick = { onAddTag(tagInput); tagInput = "" }) {
                                        Icon(Icons.Default.Add, "Add tag")
                                    }
                                }
                            }
                        )
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tags.forEach { tag ->
                                    AssistChip(onClick = { onRemoveTag(tag) }, label = { Text(tag) }, trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(18.dp)) })
                                }
                            }
                        }
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
                            Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showComments) "Hide Comments (${task.comments.size})" else "Show Comments (${task.comments.size})")
                        }

                        if (showComments) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Comments list
                            if (task.comments.isNotEmpty()) {
                                task.comments.sortedByDescending { it.timestamp }.forEach { comment ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
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
                                            Spacer(modifier = Modifier.height(4.dp))
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
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Add comment input
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                label = { Text("Add a comment") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                trailingIcon = {
                                    if (commentInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            onAddComment(commentInput)
                                            commentInput = ""
                                        }) {
                                            Icon(Icons.Default.Send, "Send comment")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = onSaveTask, modifier = Modifier.weight(1f), enabled = title.isNotBlank()) { Text("Save") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Task")
                }
            }
        }
    }

    if (showUserPicker) {
        ModalBottomSheet(onDismissRequest = { showUserPicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Assign To", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                ListItem(headlineContent = { Text("Unassigned") }, leadingContent = { Icon(Icons.Default.Person, null) }, modifier = Modifier.clickable { onAssignedToChange(null); showUserPicker = false })
                Divider()
                LazyColumn {
                    items(availableUsers) { user ->
                        ListItem(headlineContent = { Text(user.displayName ?: user.email) }, supportingContent = { Text(user.email) }, leadingContent = { Icon(Icons.Default.Person, null) }, modifier = Modifier.clickable { onAssignedToChange(user); showUserPicker = false })
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Priority indicator
                Surface(
                    color = when (task.priority) {
                        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.secondaryContainer
                        TaskPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = task.priority.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip
                Surface(
                    color = when (task.status) {
                        TaskStatus.TODO -> MaterialTheme.colorScheme.surfaceVariant
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                        TaskStatus.DONE -> MaterialTheme.colorScheme.tertiaryContainer
                        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when (task.status) {
                            TaskStatus.TODO -> "To Do"
                            TaskStatus.IN_PROGRESS -> "In Progress"
                            TaskStatus.DONE -> "Done"
                            TaskStatus.CANCELLED -> "Cancelled"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Assignee
                if (task.assignedToName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.assignedToName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Due date and tags
            if (task.dueDate != null || task.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due date
                    if (task.dueDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                                    .format(java.util.Date(task.dueDate!!)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Tags
                    if (task.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            task.tags.take(2).forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (task.tags.size > 2) {
                                Text(
                                    text = "+${task.tags.size - 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}