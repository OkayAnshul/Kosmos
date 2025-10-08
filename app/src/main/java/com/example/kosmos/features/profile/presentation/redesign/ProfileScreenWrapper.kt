package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.features.auth.presentation.AuthViewModel
import javax.inject.Inject

/**
 * Wrapper for ProfileScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject AuthViewModel via Hilt
 * - Collect current user state
 * - Calculate stats (active projects, on-time rate)
 * - Delegate navigation actions
 */
@Composable
fun ProfileScreenWrapper(
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    projectRepository: ProjectRepository = hiltViewModel<ProfileStatsViewModel>().projectRepository,
    taskRepository: TaskRepository = hiltViewModel<ProfileStatsViewModel>().taskRepository
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = uiState.currentUser?.id

    // Calculate active project count
    val projects by projectRepository.getUserProjectsFlow(currentUserId ?: "")
        .collectAsState(initial = emptyList())

    val activeProjectCount = remember(projects) {
        projects.count { it.status == ProjectStatus.ACTIVE }
    }

    // Calculate on-time rate from completed tasks
    val myTasks by taskRepository.getMyActiveTasksFlow(currentUserId ?: "")
        .collectAsState(initial = emptyList())

    val onTimeRate = remember(myTasks) {
        val completedTasks = myTasks.filter { it.status == TaskStatus.DONE }

        if (completedTasks.isEmpty()) {
            0
        } else {
            val onTimeTasks = completedTasks.count { task ->
                val dueDate = task.dueDate
                val completedAt = task.updatedAt // Using updatedAt as completion timestamp

                // Task is on-time if:
                // 1. No due date (optional tasks are always on-time)
                // 2. Completed before or on due date
                dueDate == null || completedAt <= dueDate
            }

            // Calculate percentage
            (onTimeTasks * 100) / completedTasks.size
        }
    }

    ProfileScreen(
        user = uiState.currentUser,
        activeProjectCount = activeProjectCount,
        onTimeRate = onTimeRate,
        onNavigateBack = onNavigateBack,
        onEditProfileClick = onEditProfileClick,
        onPrivacySettingsClick = onPrivacySettingsClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onNavigateToSettings = onNavigateToSettings,
        onLogoutClick = {
            authViewModel.logout()
            onLogout()
        },
        modifier = modifier
    )
}

/**
 * Helper ViewModel to inject repositories
 * This is a workaround since we can't directly inject repositories in @Composable
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class ProfileStatsViewModel @Inject constructor(
    val projectRepository: ProjectRepository,
    val taskRepository: TaskRepository
) : androidx.lifecycle.ViewModel()
