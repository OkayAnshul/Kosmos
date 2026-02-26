package com.example.kosmos.features.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.sync.InitialSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.kosmos.shared.ui.designsystem.ColorTokens
/**
 * Sync Diagnostics Screen
 *
 * Use this screen to diagnose why badges show counts but UI lists are empty.
 *
 * This screen shows:
 * - Current user info
 * - Projects with metadata counts from projects table
 * - Actual counts from chat_rooms and tasks tables
 * - Comparison to identify sync issues
 * - Manual sync trigger button
 */

data class ProjectDiagnostic(
    val projectId: String,
    val projectName: String,
    val metadataChatCount: Int,  // From projects.chatCount
    val actualChatCount: Int,     // From COUNT(*) in chat_rooms
    val metadataTaskCount: Int,   // From projects.taskCount
    val actualTaskCount: Int      // From COUNT(*) in tasks
) {
    val chatSyncIssue: Boolean get() = metadataChatCount != actualChatCount
    val taskSyncIssue: Boolean get() = metadataTaskCount != actualTaskCount
    val hasSyncIssue: Boolean get() = chatSyncIssue || taskSyncIssue
}

data class DiagnosticState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val diagnostics: List<ProjectDiagnostic> = emptyList(),
    val syncProgress: String? = null,
    val error: String? = null
)

@HiltViewModel
class SyncDiagnosticsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository,
    private val initialSyncManager: InitialSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticState())
    val state: StateFlow<DiagnosticState> = _state.asStateFlow()

    init {
        loadDiagnostics()
    }

    fun loadDiagnostics() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "No user logged in"
                    )
                    return@launch
                }

                // Get all user projects
                val projects = projectRepository.getUserProjectsFlow(currentUser.id).first()

                // For each project, compare metadata vs actual counts
                val diagnostics = projects.map { project ->
                    // Metadata counts (from projects table)
                    val metadataChatCount = project.chatCount
                    val metadataTaskCount = project.taskCount

                    // Actual counts (query chat_rooms and tasks tables)
                    val actualChatCount = countChatRoomsForProject(project.id, currentUser.id)
                    val actualTaskCount = countTasksForProject(project.id)

                    ProjectDiagnostic(
                        projectId = project.id,
                        projectName = project.name,
                        metadataChatCount = metadataChatCount,
                        actualChatCount = actualChatCount,
                        metadataTaskCount = metadataTaskCount,
                        actualTaskCount = actualTaskCount
                    )
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    currentUserId = currentUser.id,
                    currentUserName = currentUser.username,
                    diagnostics = diagnostics
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error loading diagnostics: ${e.message}"
                )
            }
        }
    }

    private suspend fun countChatRoomsForProject(projectId: String, userId: String): Int {
        return try {
            chatRepository.getChatRoomsForProject(userId, projectId).first().size
        } catch (e: Exception) {
            -1  // Error indicator
        }
    }

    private suspend fun countTasksForProject(projectId: String): Int {
        return try {
            taskRepository.getTasksForProjectFlow(projectId).first().size
        } catch (e: Exception) {
            -1  // Error indicator
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isSyncing = true, syncProgress = "Starting sync...", error = null)

                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        syncProgress = null,
                        error = "No user logged in"
                    )
                    return@launch
                }

                _state.value = _state.value.copy(syncProgress = "Syncing all data from Supabase...")

                val progress = initialSyncManager.syncAllData(currentUser.id)

                val resultMessage = buildString {
                    appendLine("Sync Complete!")
                    appendLine("Users: ${if (progress.usersComplete) "✅" else "❌"}")
                    appendLine("Projects: ${if (progress.projectsComplete) "✅" else "❌"}")
                    appendLine("Projects synced: ${progress.projectsSynced}/${progress.projectsTotal}")
                    if (progress.projectSyncErrors > 0) {
                        appendLine("⚠️ ${progress.projectSyncErrors} projects had errors")
                    }
                }

                _state.value = _state.value.copy(
                    isSyncing = false,
                    syncProgress = resultMessage
                )

                // Reload diagnostics to see updated counts
                kotlinx.coroutines.delay(1000)
                loadDiagnostics()

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSyncing = false,
                    syncProgress = null,
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }
}

@Composable
fun SyncDiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: SyncDiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDiagnostics() },
                        enabled = !state.isLoading && !state.isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
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
            // User info
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current User", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ID: ${state.currentUserId ?: "Not logged in"}")
                        Text("Name: ${state.currentUserName ?: "N/A"}")
                    }
                }
            }

            // Manual sync button
            item {
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !state.isSyncing && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isSyncing) "Syncing..." else "Trigger Manual Sync")
                }
            }

            // Sync progress
            if (state.syncProgress != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Sync Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                state.syncProgress!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Error message
            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                state.error!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Loading indicator
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

            // Diagnostics header
            item {
                Text(
                    "Project Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Compares badge counts (metadata) vs actual data in tables",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Project diagnostics
            items(state.diagnostics) { diagnostic ->
                ProjectDiagnosticCard(diagnostic)
            }

            // Empty state
            if (!state.isLoading && state.diagnostics.isEmpty() && state.error == null) {
                item {
                    Card {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("No projects found")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDiagnosticCard(diagnostic: ProjectDiagnostic) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (diagnostic.hasSyncIssue) Color(0xFFDC2626).copy(alpha = 0.1f) else ColorTokens.ReactTheme.card
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    diagnostic.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (diagnostic.hasSyncIssue) {
                    Text(
                        "⚠️ SYNC ISSUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "✅ OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat counts
            DiagnosticRow(
                label = "Chats",
                metadataCount = diagnostic.metadataChatCount,
                actualCount = diagnostic.actualChatCount,
                hasIssue = diagnostic.chatSyncIssue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Task counts
            DiagnosticRow(
                label = "Tasks",
                metadataCount = diagnostic.metadataTaskCount,
                actualCount = diagnostic.actualTaskCount,
                hasIssue = diagnostic.taskSyncIssue
            )
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    metadataCount: Int,
    actualCount: Int,
    hasIssue: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "Badge (Metadata)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                Text(
                    metadataCount.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "Actual (Table)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                Text(
                    actualCount.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasIssue) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            }
        }
    }
}
