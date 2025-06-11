package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.coroutines.DispatcherProvider
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.TaskDependencyDao
import com.example.kosmos.core.database.dao.TimeEntryDao
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseDependencyDataSource
import com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource
import com.example.kosmos.data.datasource.SupabaseTaskDataSource
import com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource
import com.example.kosmos.data.sync.FKRetryQueue
import com.example.kosmos.features.notifications.NotificationRulesEngine
import com.example.kosmos.features.notifications.ReminderScheduler
import com.example.kosmos.shared.utils.NetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Integration tests for TaskRepository.
 *
 * Uses a real in-memory Room database so we test actual DAO queries and
 * Room entity mapping, while mocking all remote Supabase dependencies so
 * tests are fast and hermetic (no network calls).
 *
 * Coverage:
 *  - createTask: success for member with CREATE_TASKS permission
 *  - createTask: denied for non-member
 *  - updateTask: updates persisted in Room
 *  - deleteTask: row removed from Room
 *  - getTasksForProject: flow emits inserted tasks
 *  - getTaskById: returns correct task after insert
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TaskRepositoryIntegrationTest {

    // ---- Real in-memory Room database ----------------------------------
    private lateinit var db: KosmosDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskActivityDao: TaskActivityDao
    private lateinit var projectMemberDao: ProjectMemberDao
    private lateinit var userDao: UserDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var timeEntryDao: TimeEntryDao
    private lateinit var taskDependencyDao: TaskDependencyDao

    // ---- Mocked Supabase / external deps --------------------------------
    private val supabaseTaskDataSource: SupabaseTaskDataSource = mockk(relaxed = true)
    private val supabaseTaskActivityDataSource: SupabaseTaskActivityDataSource = mockk(relaxed = true)
    private val notificationRulesEngine: NotificationRulesEngine = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val fkRetryQueue: FKRetryQueue = mockk(relaxed = true)
    private val supabaseTimeEntryDataSource: SupabaseTimeEntryDataSource = mockk(relaxed = true)
    private val supabaseDependencyDataSource: SupabaseDependencyDataSource = mockk(relaxed = true)

    private lateinit var repository: TaskRepository

    // Test dispatcher — runs everything on the calling thread for determinism
    private val testDispatcher = object : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
    }

    // Fixtures
    private val projectId = "proj-integration-1"
    private val userId    = "user-integration-1"
    private val memberId  = "member-integration-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // In-memory DB with fallback destructive migration (fine for tests)
        db = Room.inMemoryDatabaseBuilder(context, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()   // ok for integration tests
            .build()

        taskDao           = db.taskDao()
        taskActivityDao   = db.taskActivityDao()
        projectMemberDao  = db.projectMemberDao()
        userDao           = db.userDao()
        syncQueueDao      = db.syncQueueDao()
        timeEntryDao      = db.timeEntryDao()
        taskDependencyDao = db.taskDependencyDao()

        // Supabase always succeeds (we only care about the Room side)
        coEvery { supabaseTaskDataSource.insertTask(any()) } returns Result.success(
            Task(id = "x", title = "x", projectId = projectId)
        )
        coEvery { supabaseTaskDataSource.updateTask(any()) } returns Result.success(Unit)
        coEvery { supabaseTaskDataSource.deleteTask(any()) } returns Result.success(Unit)
        // insertActivity returns Result<TaskActivity> — use relaxed mock (auto-stubs return values)

        // NetworkMonitor reports online
        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)

        repository = TaskRepository(
            taskDao                       = taskDao,
            projectDao                    = db.projectDao(),
            projectMemberDao              = projectMemberDao,
            userDao                       = userDao,
            taskActivityDao               = taskActivityDao,
            supabaseTaskDataSource        = supabaseTaskDataSource,
            supabaseTaskActivityDataSource= supabaseTaskActivityDataSource,
            notificationRulesEngine       = notificationRulesEngine,
            reminderScheduler             = reminderScheduler,
            networkMonitor                = networkMonitor,
            syncQueueDao                  = syncQueueDao,
            dispatchers                   = testDispatcher,
            fkRetryQueue                  = fkRetryQueue,
            timeEntryDao                  = timeEntryDao,
            taskDependencyDao             = taskDependencyDao,
            supabaseTimeEntryDataSource   = supabaseTimeEntryDataSource,
            supabaseDependencyDataSource  = supabaseDependencyDataSource
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /** Seed: insert a project + a member with the given role */
    private suspend fun seedProjectAndMember(role: ProjectRole = ProjectRole.MEMBER) {
        db.projectDao().insertProject(
            Project(id = projectId, name = "Test Project", ownerId = userId)
        )
        userDao.insertUser(
            User(id = userId, email = "u@test.com", username = "u", displayName = "U")
        )
        projectMemberDao.insertMember(
            ProjectMember(
                id        = memberId,
                projectId = projectId,
                userId    = userId,
                role      = role
            )
        )
    }

    private fun newTask(title: String = "My Task") = Task(
        id          = "",          // blank → repository assigns UUID
        title       = title,
        projectId   = projectId,
        status      = TaskStatus.TODO,
        priority    = TaskPriority.MEDIUM,
        createdById = userId
    )

    // ------------------------------------------------------------------ //
    //  Tests                                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun createTask_memberWithPermission_savesToRoom() = runTest {
        seedProjectAndMember(ProjectRole.MEMBER)

        val result = repository.createTask(newTask(), userId)

        assertTrue("Expected success but got: ${result.exceptionOrNull()}", result.isSuccess)
        val createdId = result.getOrThrow()
        assertNotNull(taskDao.getTaskById(createdId))
    }

    @Test
    fun createTask_nonMember_returnsFailure() = runTest {
        // No member row → should be denied
        db.projectDao().insertProject(
            Project(id = projectId, name = "Test Project", ownerId = "other-owner")
        )

        val result = repository.createTask(newTask(), userId)

        assertTrue("Expected failure for non-member", result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Error message should mention member: $msg", msg.contains("member", ignoreCase = true))
    }

    @Test
    fun createTask_adminRole_savesToRoom() = runTest {
        seedProjectAndMember(ProjectRole.ADMIN)

        val result = repository.createTask(newTask("Admin Task"), userId)

        assertTrue(result.isSuccess)
        val task = taskDao.getTaskById(result.getOrThrow())
        assertEquals("Admin Task", task?.title)
    }

    @Test
    fun updateTask_changesArePersisted() = runTest {
        // ADMIN has EDIT_ANY_TASK; plain MEMBER only has EDIT_OWN_TASKS
        seedProjectAndMember(ProjectRole.ADMIN)
        val createResult = repository.createTask(newTask("Original"), userId)
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()

        val original = taskDao.getTaskById(taskId)!!
        val updated  = original.copy(title = "Updated Title", status = TaskStatus.IN_PROGRESS)

        val updateResult = repository.updateTask(updated, userId)
        assertTrue("Update failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val persisted = taskDao.getTaskById(taskId)
        assertEquals("Updated Title", persisted?.title)
        assertEquals(TaskStatus.IN_PROGRESS, persisted?.status)
    }

    @Test
    fun deleteTask_removesFromRoom() = runTest {
        // ADMIN has DELETE_ANY_TASK; plain MEMBER only has DELETE_OWN_TASKS
        seedProjectAndMember(ProjectRole.ADMIN)
        val createResult = repository.createTask(newTask("To Delete"), userId)
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertNotNull(taskDao.getTaskById(taskId))

        // TaskActivity has a FK → tasks.id, so delete activities first
        taskActivityDao.deleteActivityForTask(taskId)

        val deleteResult = repository.deleteTask(taskId, userId)
        assertTrue("Delete failed: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)

        assertNull(taskDao.getTaskById(taskId))
    }

    @Test
    fun getTasksForProject_flowEmitsInsertedTasks() = runTest {
        seedProjectAndMember()
        repository.createTask(newTask("Task A"), userId)
        repository.createTask(newTask("Task B"), userId)

        val tasks = taskDao.getTasksForProjectFlow(projectId).first()
        assertEquals(2, tasks.size)
        val titles = tasks.map { it.title }.toSet()
        assertTrue(titles.containsAll(setOf("Task A", "Task B")))
    }

    @Test
    fun getTaskById_returnsCorrectTask() = runTest {
        seedProjectAndMember()
        val createResult = repository.createTask(newTask("Specific Task"), userId)
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()

        val fetched = taskDao.getTaskById(taskId)
        assertNotNull(fetched)
        assertEquals("Specific Task", fetched?.title)
        assertEquals(projectId, fetched?.projectId)
    }

    @Test
    fun createTask_assignsUuidWhenIdIsBlank() = runTest {
        seedProjectAndMember()

        val task = Task(
            id          = "",
            title       = "UUID Check",
            projectId   = projectId,
            createdById = userId
        )
        val result = repository.createTask(task, userId)

        assertTrue(result.isSuccess)
        val id = result.getOrThrow()
        assertTrue("ID should be a non-blank UUID", id.isNotBlank())
        assertEquals(36, id.length) // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    }
}
