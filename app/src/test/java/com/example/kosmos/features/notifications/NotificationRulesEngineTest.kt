package com.example.kosmos.features.notifications

import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.DoNotDisturbSettings
import com.example.kosmos.core.models.NotificationSettings
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NotificationRulesEngineTest {

    private val userDao: UserDao = mockk(relaxed = true)
    private val notificationService: SupabaseNotificationService = mockk(relaxed = true)
    private lateinit var engine: NotificationRulesEngine

    private val actorId = "actor-1"
    private val assigneeId = "assignee-1"
    private val creatorId = "creator-2"

    private val assignee = User(
        id = assigneeId,
        email = "assignee@test.com",
        username = "assignee",
        displayName = "Assignee"
    )
    private val creator = User(
        id = creatorId,
        email = "creator@test.com",
        username = "creator",
        displayName = "Creator"
    )

    private val task = Task(
        id = "task-1",
        title = "Test Task",
        projectId = "proj-1",
        status = TaskStatus.TODO,
        priority = TaskPriority.MEDIUM,
        assignedToId = assigneeId,
        createdById = creatorId
    )

    private fun activity(
        actionType: ActivityActionType,
        actorId: String = this.actorId,
        commitMessage: String? = null
    ) = TaskActivity(
        taskId = task.id,
        projectId = task.projectId,
        actorId = actorId,
        actorName = "Actor",
        actionType = actionType,
        autoDescription = actionType.name.lowercase(),
        commitMessage = commitMessage
    )

    @Before
    fun setUp() {
        engine = NotificationRulesEngine(userDao, notificationService)
        coEvery { userDao.getUserById(assigneeId) } returns assignee
        coEvery { userDao.getUserById(creatorId) } returns creator
        coEvery { userDao.getUserById(actorId) } returns null
        coEvery { notificationService.sendNotification(any(), any(), any(), any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun evaluateAndNotify_assigned_notifiesAssignee() = runTest {
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        coVerify(atLeast = 1) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_actorSelfAssigned_notNotified() = runTest {
        val selfAssignTask = task.copy(assignedToId = actorId)
        coEvery { userDao.getUserById(actorId) } returns User(
            id = actorId, email = "actor@test.com", username = "actor", displayName = "Actor"
        )
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), selfAssignTask)
        coVerify(exactly = 0) { notificationService.sendNotification(actorId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_statusChanged_notifiesAssigneeAndCreator() = runTest {
        engine.evaluateAndNotify(activity(ActivityActionType.STATUS_CHANGED), task)
        coVerify(atLeast = 1) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
        coVerify(atLeast = 1) { notificationService.sendNotification(creatorId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_commentAdded_notifiesAssigneeAndCreator() = runTest {
        engine.evaluateAndNotify(activity(ActivityActionType.COMMENT_ADDED), task)
        coVerify(atLeast = 1) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
        coVerify(atLeast = 1) { notificationService.sendNotification(creatorId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_globalNotificationsDisabled_noNotification() = runTest {
        val disabledUser = assignee.copy(
            settings = UserSettings(notifications = NotificationSettings(enabled = false))
        )
        coEvery { userDao.getUserById(assigneeId) } returns disabledUser
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        coVerify(exactly = 0) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_taskNotificationsDisabled_noNotification() = runTest {
        val disabledUser = assignee.copy(
            settings = UserSettings(notifications = NotificationSettings(enabled = true, tasks = false))
        )
        coEvery { userDao.getUserById(assigneeId) } returns disabledUser
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        coVerify(exactly = 0) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_mentionsOnlyMode_notMentioned_noNotification() = runTest {
        val mentionsUser = assignee.copy(
            settings = UserSettings(notifications = NotificationSettings(mentionsOnlyMode = true))
        )
        coEvery { userDao.getUserById(assigneeId) } returns mentionsUser
        engine.evaluateAndNotify(
            activity(ActivityActionType.ASSIGNED, commitMessage = "no mentions here"),
            task
        )
        coVerify(exactly = 0) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_rateLimitedSecondCall_noSecondNotification() = runTest {
        // First call should notify
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        // Second immediate call should be rate-limited
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        // Service should be called exactly once (not twice) due to rate limit
        coVerify(exactly = 1) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_dndEnabled_duringDndHours_noNotification() = runTest {
        // DND enabled 0:00 - 23:59 (always in DND for this test)
        val dndUser = assignee.copy(
            settings = UserSettings(
                notifications = NotificationSettings(
                    dnd = DoNotDisturbSettings(
                        enabled = true,
                        startHour = 0,
                        startMinute = 0,
                        endHour = 23,
                        endMinute = 59
                    )
                )
            )
        )
        coEvery { userDao.getUserById(assigneeId) } returns dndUser
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), task)
        coVerify(exactly = 0) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }

    @Test
    fun evaluateAndNotify_noAssignee_noNotification() = runTest {
        val unassignedTask = task.copy(assignedToId = null)
        engine.evaluateAndNotify(activity(ActivityActionType.ASSIGNED), unassignedTask)
        coVerify(exactly = 0) { notificationService.sendNotification(assigneeId, any(), any(), any(), any()) }
    }
}
