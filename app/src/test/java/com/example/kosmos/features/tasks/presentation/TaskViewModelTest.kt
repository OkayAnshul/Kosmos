package com.example.kosmos.features.tasks.presentation

import com.example.kosmos.core.feedback.UserFeedbackManager
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.testutil.TestDispatcherRule
import com.example.kosmos.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for TaskViewModel.
 */
@RunWith(JUnit4::class)
class TaskViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var taskRepository: TaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var viewModel: TaskViewModel

    private val testUser = TestFixtures.user(id = "user-1", email = "user@test.com")

    @Before
    fun setup() {
        taskRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        projectRepository = mockk(relaxed = true)
        chatRepository = mockk(relaxed = true)
        val feedbackManager = mockk<UserFeedbackManager>(relaxed = true)

        every { authRepository.getCurrentUser() } returns testUser
        coEvery { taskRepository.getPendingTasksCountFlow("user-1") } returns flowOf(0)

        viewModel = TaskViewModel(
            taskRepository, userRepository, authRepository, projectRepository, chatRepository, feedbackManager
        )
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state - currentUserId is set from auth`() {
        assertThat(viewModel.uiState.value.currentUserId).isEqualTo("user-1")
    }

    // ─── createTask - critical bug regression ─────────────────────────────────

    @Test
    fun `createTask success - lastCreatedTaskId is the repo-returned ID not blank`() = runTest {
        val realId = "real-uuid-from-repo"
        coEvery { taskRepository.createTask(any(), any()) } returns Result.success(realId)

        viewModel.createTask(
            projectId = "proj-1",
            title = "Test Task",
            description = "desc"
        )

        // Critical regression check: must be real ID from repository, not ""
        assertThat(viewModel.uiState.value.lastCreatedTaskId).isEqualTo(realId)
        assertThat(viewModel.uiState.value.lastCreatedTaskId).isNotEmpty()
    }

    @Test
    fun `createTask success - lastCreatedTaskId is never empty string`() = runTest {
        coEvery { taskRepository.createTask(any(), any()) } returns Result.success("some-uuid")

        viewModel.createTask(projectId = "proj-1", title = "My Task", description = "")

        assertThat(viewModel.uiState.value.lastCreatedTaskId).isNotEqualTo("")
    }

    // ─── createTask validation ────────────────────────────────────────────────

    @Test
    fun `createTask blank title - sets error and does not call repo`() = runTest {
        viewModel.createTask(projectId = "proj-1", title = "", description = "desc")

        assertThat(viewModel.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { taskRepository.createTask(any(), any()) }
    }

    @Test
    fun `createTask blank projectId - sets error and does not call repo`() = runTest {
        viewModel.createTask(projectId = "", title = "Task", description = "desc")

        assertThat(viewModel.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { taskRepository.createTask(any(), any()) }
    }

    @Test
    fun `createTask no user - sets error`() = runTest {
        every { authRepository.getCurrentUser() } returns null
        // Recreate VM with null user
        val vmNoUser = TaskViewModel(
            taskRepository, userRepository, authRepository, projectRepository, chatRepository, mockk(relaxed = true)
        )

        vmNoUser.createTask(projectId = "proj-1", title = "Task", description = "")

        assertThat(vmNoUser.uiState.value.error).isNotNull()
    }

    // ─── createTask loading state ─────────────────────────────────────────────

    @Test
    fun `createTask failure - isCreatingTask is false after`() = runTest {
        coEvery { taskRepository.createTask(any(), any()) } returns
            Result.failure(Exception("DB error"))

        viewModel.createTask(projectId = "proj-1", title = "Task", description = "")

        assertThat(viewModel.uiState.value.isCreatingTask).isFalse()
    }

    @Test
    fun `createTask failure - error is set`() = runTest {
        coEvery { taskRepository.createTask(any(), any()) } returns
            Result.failure(Exception("Network error"))

        viewModel.createTask(projectId = "proj-1", title = "Task", description = "")

        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    // ─── canMarkTaskComplete ──────────────────────────────────────────────────

    @Test
    fun `canMarkTaskComplete - task assigned to current user returns true`() {
        val task = TestFixtures.task(assignedToId = "user-1")
        assertThat(viewModel.canMarkTaskComplete(task)).isTrue()
    }

    @Test
    fun `canMarkTaskComplete - unassigned task returns true`() {
        val task = TestFixtures.task(assignedToId = null)
        assertThat(viewModel.canMarkTaskComplete(task)).isTrue()
    }

    @Test
    fun `canMarkTaskComplete - task assigned to other user returns false`() {
        val task = TestFixtures.task(assignedToId = "other-user")
        assertThat(viewModel.canMarkTaskComplete(task)).isFalse()
    }

    // ─── showCreateTaskDialog / hideCreateTaskDialog ──────────────────────────

    @Test
    fun `showCreateTaskDialog - sets showCreateTaskDialog true`() {
        viewModel.showCreateTaskDialog()
        assertThat(viewModel.uiState.value.showCreateTaskDialog).isTrue()
    }

    @Test
    fun `hideCreateTaskDialog - sets showCreateTaskDialog false`() {
        viewModel.showCreateTaskDialog()
        viewModel.hideCreateTaskDialog()
        assertThat(viewModel.uiState.value.showCreateTaskDialog).isFalse()
    }

    // ─── showDeleteConfirmation / confirmDeleteTask ───────────────────────────

    @Test
    fun `showDeleteConfirmation - shows confirmation dialog`() {
        val task = TestFixtures.task()
        viewModel.showDeleteConfirmation(task)

        assertThat(viewModel.uiState.value.showDeleteConfirmation).isTrue()
        assertThat(viewModel.uiState.value.taskToDelete).isEqualTo(task)
    }

    @Test
    fun `confirmDeleteTask - calls repository`() = runTest {
        val task = TestFixtures.task(id = "task-to-delete")
        viewModel.showDeleteConfirmation(task)

        coEvery { taskRepository.deleteTask(any(), any()) } returns Result.success(Unit)
        viewModel.confirmDeleteTask()

        coVerify { taskRepository.deleteTask(any(), any()) }
    }

}
