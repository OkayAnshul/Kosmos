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

        // Task List
        val filteredTasks = when (selectedTab) {
            1 -> uiState.tasks.filter { it.status == TaskStatus.TODO }
            2 -> uiState.tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            3 -> uiState.tasks.filter { it.status == TaskStatus.DONE }
            else -> uiState.tasks
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTasks) { task ->
                TaskCard(
                    task = task,
                    onClick = { /* TODO: Navigate to task details */ }
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
                onTitleChange = taskViewModel::updateCreateTaskTitle,
                onDescriptionChange = taskViewModel::updateCreateTaskDescription,
                onPriorityChange = taskViewModel::updateCreateTaskPriority,
                onCreateTask = {
                    taskViewModel.createTask(
                        chatRoomId = chatRoomId,
                        title = uiState.createTaskTitle,
                        description = uiState.createTaskDescription,
                        priority = uiState.createTaskPriority
                    )
                },
                onDismiss = taskViewModel::hideCreateTaskDialog
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
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onCreateTask: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Priority:", style = MaterialTheme.typography.labelMedium)
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
        },
        confirmButton = {
            TextButton(
                onClick = onCreateTask,
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
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
        }
    }
}