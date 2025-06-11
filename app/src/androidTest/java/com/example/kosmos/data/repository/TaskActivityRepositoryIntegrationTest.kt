package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.SyncEntityType
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskActivityRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var taskActivityDao: TaskActivityDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val supabaseDataSource: SupabaseTaskActivityDataSource = mockk(relaxed = true)

    private lateinit var repository: TaskActivityRepository

    private val taskId = "task-act-1"
    private val projectId = "proj-act-1"
    private val actorId = "actor-1"

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        taskActivityDao = db.taskActivityDao()
        syncQueueDao = db.syncQueueDao()

        // Seed FK-required rows: User → Project → Task
        kotlinx.coroutines.runBlocking {
            db.userDao().insertUser(User(id = actorId, email = "a@test.com", username = "actor", displayName = "Actor"))
            db.projectDao().insertProject(Project(id = projectId, name = "Test Project", ownerId = actorId))
            db.taskDao().insertTask(Task(id = taskId, title = "Test Task", projectId = projectId, createdById = actorId))
        }

        coEvery { supabaseDataSource.insertActivity(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseDataSource.deleteActivity(any()) } returns Result.success(Unit)
        coEvery { supabaseDataSource.deleteActivitiesForTask(any()) } returns Result.success(Unit)

        repository = TaskActivityRepository(taskActivityDao, supabaseDataSource, syncQueueDao)
    }

    @After
    fun tearDown() { db.close() }

    private fun newActivity(actionType: ActivityActionType = ActivityActionType.CREATED) = TaskActivity(
        taskId = taskId,
        projectId = projectId,
        actorId = actorId,
        actorName = "Actor",
        actionType = actionType,
        autoDescription = "test activity"
    )

    @Test
    fun trackActivity_savesToRoom() = runTest {
        val activity = newActivity()
        val result = repository.trackActivity(activity)
        assertTrue(result.isSuccess)
        val fetched = taskActivityDao.getActivityById(activity.id)
        assertNotNull(fetched)
    }

    @Test
    fun trackActivity_supabaseFailure_stillSavesToRoom() = runTest {
        coEvery { supabaseDataSource.insertActivity(any()) } returns Result.failure(RuntimeException("network error"))
        val activity = newActivity()
        val result = repository.trackActivity(activity)
        assertTrue("Should succeed even if Supabase fails", result.isSuccess)
        assertNotNull(taskActivityDao.getActivityById(activity.id))
    }

    @Test
    fun trackActivity_supabaseFailure_queuesForRetry() = runTest {
        coEvery { supabaseDataSource.insertActivity(any()) } returns Result.failure(RuntimeException("offline"))
        repository.trackActivity(newActivity())
        val pending = syncQueueDao.getAllPendingItems()
        assertTrue(pending.any { it.entityType == SyncEntityType.TASK_ACTIVITY })
    }

    @Test
    fun getActivityForTaskFlow_emitsInsertedActivities() = runTest {
        repository.trackActivity(newActivity(ActivityActionType.CREATED))
        repository.trackActivity(newActivity(ActivityActionType.STATUS_CHANGED))
        val activities = repository.getActivityForTaskFlow(taskId).first()
        assertEquals(2, activities.size)
    }

    @Test
    fun getActivityCountForTask_countsCorrectly() = runTest {
        repository.trackActivity(newActivity())
        repository.trackActivity(newActivity())
        repository.trackActivity(newActivity())
        assertEquals(3, repository.getActivityCountForTask(taskId))
    }

    @Test
    fun deleteActivity_removesFromRoom() = runTest {
        val activity = newActivity()
        repository.trackActivity(activity)
        assertNotNull(taskActivityDao.getActivityById(activity.id))
        repository.deleteActivity(activity.id)
        val fetched = taskActivityDao.getActivityById(activity.id)
        assert(fetched == null)
    }

    @Test
    fun deleteActivityForTask_removesAll() = runTest {
        repository.trackActivity(newActivity(ActivityActionType.CREATED))
        repository.trackActivity(newActivity(ActivityActionType.STATUS_CHANGED))
        repository.deleteActivityForTask(taskId)
        assertEquals(0, repository.getActivityCountForTask(taskId))
    }

    @Test
    fun syncPendingActivities_retriesQueuedItems() = runTest {
        // Queue a failed activity
        coEvery { supabaseDataSource.insertActivity(any()) } returns Result.failure(RuntimeException("offline"))
        repository.trackActivity(newActivity())

        val pendingBefore = syncQueueDao.getAllPendingItems()
            .filter { it.entityType == SyncEntityType.TASK_ACTIVITY }
        assertTrue(pendingBefore.isNotEmpty())

        // Now simulate online sync
        coEvery { supabaseDataSource.insertActivity(any()) } returns Result.success(mockk(relaxed = true))
        repository.syncPendingActivities()

        val pendingAfter = syncQueueDao.getAllPendingItems()
            .filter { it.entityType == SyncEntityType.TASK_ACTIVITY }
        assertTrue(pendingAfter.isEmpty())
    }
}
