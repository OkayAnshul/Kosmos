package com.example.kosmos.features.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.dao.ProjectDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.kosmos.shared.ui.designsystem.ColorTokens
/**
 * Quick Data Check Screen
 *
 * Shows RAW database counts without any filtering
 * This helps identify if the problem is:
 * 1. No data in database → Sync issue
 * 2. Data exists but not showing → Filtering issue
 */

data class DataCheckState(
    val isLoading: Boolean = true,
    val currentUserId: String? = null,

    // Raw counts (no filtering)
    val totalProjects: Int = 0,
    val totalChatRooms: Int = 0,
    val totalTasks: Int = 0,

    // Sample data (first 5 items)
    val sampleProjects: List<String> = emptyList(),
    val sampleChats: List<String> = emptyList(),
    val sampleTasks: List<String> = emptyList(),

    val error: String? = null
)

@HiltViewModel
class QuickDataCheckViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectDao: ProjectDao,
    private val chatRoomDao: ChatRoomDao,
    private val taskDao: TaskDao
) : ViewModel() {

    private val _state = MutableStateFlow(DataCheckState())
    val state: StateFlow<DataCheckState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                val currentUser = authRepository.getCurrentUser()

                // Get raw counts (NO FILTERING!)
                val allProjects = projectDao.getAllProjectsFlow().first()
                val allChats = chatRoomDao.getAllChatRoomsFlow().first()
                val allTasks = taskDao.getAllTasksFlow().first()

                // Sample data (first 5 with details)
                val sampleProjects = allProjects.take(5).map {
                    "${it.name} (id=${it.id.take(8)}, chats=${it.chatCount}, tasks=${it.taskCount})"
                }
                val sampleChats = allChats.take(5).map {
                    "${it.name} (project=${it.projectId.take(8)}, isPrivate=${it.isPrivate}, participants=${it.participantIds.size})"
                }
                val sampleTasks = allTasks.take(5).map {
                    "${it.title} (project=${it.projectId.take(8)}, assigned=${it.assignedToName ?: "none"})"
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    currentUserId = currentUser?.id,
                    totalProjects = allProjects.size,
                    totalChatRooms = allChats.size,
                    totalTasks = allTasks.size,
                    sampleProjects = sampleProjects,
                    sampleChats = sampleChats,
                    sampleTasks = sampleTasks
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}

@Composable
fun QuickDataCheckScreen(
    onBack: () -> Unit,
    viewModel: QuickDataCheckViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Data Check") },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Close")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.secondary)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📊 RAW DATABASE COUNTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This shows UNFILTERED data from Room database.\nIf counts are 0 → Sync issue\nIf counts are >0 but UI empty → Filtering issue",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // User info
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current User ID:")
                        Text(
                            state.currentUserId ?: "Not logged in",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error
            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.destructive)) {
                        Text(
                            state.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = ColorTokens.ReactTheme.destructiveForeground
                        )
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Counts
            if (!state.isLoading) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "DATABASE COUNTS (RAW)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DataRow("Projects:", state.totalProjects)
                            DataRow("Chat Rooms:", state.totalChatRooms)
                            DataRow("Tasks:", state.totalTasks)
                        }
                    }
                }

                // Diagnosis
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                state.totalProjects == 0 -> ColorTokens.ReactTheme.destructive
                                state.totalChatRooms == 0 || state.totalTasks == 0 -> ColorTokens.ReactTheme.destructive
                                else -> ColorTokens.ReactTheme.accent
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "DIAGNOSIS:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            when {
                                state.totalProjects == 0 -> {
                                    Text("❌ NO PROJECTS IN DATABASE")
                                    Text("→ Sync never ran OR sync failed", style = MaterialTheme.typography.bodySmall)
                                    Text("→ Check InitialSyncManager logs", style = MaterialTheme.typography.bodySmall)
                                }
                                state.totalChatRooms == 0 && state.totalTasks == 0 -> {
                                    Text("⚠️ PROJECTS EXIST BUT NO CHATS/TASKS")
                                    Text("→ Project sync succeeded", style = MaterialTheme.typography.bodySmall)
                                    Text("→ Chat/task sync failed", style = MaterialTheme.typography.bodySmall)
                                    Text("→ Check sync logs for errors", style = MaterialTheme.typography.bodySmall)
                                }
                                state.totalChatRooms > 0 && state.totalTasks > 0 -> {
                                    Text("✅ DATA EXISTS IN DATABASE")
                                    Text("→ Problem is FILTERING logic", style = MaterialTheme.typography.bodySmall)
                                    Text("→ Check ChatRepository.getChatRoomsForProject()", style = MaterialTheme.typography.bodySmall)
                                    Text("→ Check isPrivate, participantIds fields", style = MaterialTheme.typography.bodySmall)
                                }
                                else -> {
                                    Text("⚠️ PARTIAL DATA")
                                    Text("Some data exists, check specific tables", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Sample Projects
                if (state.sampleProjects.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Sample Projects (first 5):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                state.sampleProjects.forEach { project ->
                                    Text(
                                        "• $project",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Sample Chats
                if (state.sampleChats.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Sample Chat Rooms (first 5):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                state.sampleChats.forEach { chat ->
                                    Text(
                                        "• $chat",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else if (!state.isLoading && state.totalChatRooms == 0) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.destructive)) {
                            Text(
                                "❌ No chat rooms in database\n→ Check sync logs for chat sync errors",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Sample Tasks
                if (state.sampleTasks.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Sample Tasks (first 5):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                state.sampleTasks.forEach { task ->
                                    Text(
                                        "• $task",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else if (!state.isLoading && state.totalTasks == 0) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.destructive)) {
                            Text(
                                "❌ No tasks in database\n→ Check sync logs for task sync errors",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Reload button
                item {
                    Button(
                        onClick = { viewModel.loadData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reload Data")
                    }
                }
            }
        }
    }
}

@Composable
fun DataRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.destructive
        )
    }
}
