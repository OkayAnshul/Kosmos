package com.example.kosmos.data.sync

import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Sync Queue Helper (P0-08 FIX)
 *
 * Helper functions for queueing failed Supabase operations.
 * Used by repositories to queue operations for automatic retry.
 */
object SyncQueueHelper {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Queue a failed task operation
     *
     * @param syncQueueDao DAO for persisting queue item
     * @param task Task entity
     * @param operation Type of operation (CREATE/UPDATE/DELETE)
     * @param priority Priority (default 0)
     */
    suspend fun queueTask(
        syncQueueDao: SyncQueueDao,
        task: Task,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.TASK,
            entityId = task.id,
            operation = operation,
            entityJson = json.encodeToString(task),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed task activity operation
     */
    suspend fun queueTaskActivity(
        syncQueueDao: SyncQueueDao,
        activity: TaskActivity,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.TASK_ACTIVITY,
            entityId = activity.id,
            operation = operation,
            entityJson = json.encodeToString(activity),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed project operation
     */
    suspend fun queueProject(
        syncQueueDao: SyncQueueDao,
        project: Project,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.PROJECT,
            entityId = project.id,
            operation = operation,
            entityJson = json.encodeToString(project),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed project member operation
     */
    suspend fun queueProjectMember(
        syncQueueDao: SyncQueueDao,
        member: ProjectMember,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.PROJECT_MEMBER,
            entityId = member.id,
            operation = operation,
            entityJson = json.encodeToString(member),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed message operation
     */
    suspend fun queueMessage(
        syncQueueDao: SyncQueueDao,
        message: Message,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.MESSAGE,
            entityId = message.id,
            operation = operation,
            entityJson = json.encodeToString(message),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed chat room operation
     */
    suspend fun queueChatRoom(
        syncQueueDao: SyncQueueDao,
        chatRoom: ChatRoom,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.CHAT_ROOM,
            entityId = chatRoom.id,
            operation = operation,
            entityJson = json.encodeToString(chatRoom),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed user operation
     */
    suspend fun queueUser(
        syncQueueDao: SyncQueueDao,
        user: User,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.USER,
            entityId = user.id,
            operation = operation,
            entityJson = json.encodeToString(user),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    suspend fun queueProjectInvite(
        syncQueueDao: SyncQueueDao,
        invite: ProjectInvite,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.PROJECT_INVITE,
            entityId = invite.id,
            operation = operation,
            entityJson = json.encodeToString(invite),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    suspend fun queueUserConnection(
        syncQueueDao: SyncQueueDao,
        connection: UserConnection,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.USER_CONNECTION,
            entityId = connection.id,
            operation = operation,
            entityJson = json.encodeToString(connection),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    /**
     * Queue a failed time entry operation
     */
    suspend fun queueTimeEntry(
        syncQueueDao: SyncQueueDao,
        timeEntry: TimeEntry,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.TIME_ENTRY,
            entityId = timeEntry.id,
            operation = operation,
            entityJson = json.encodeToString(timeEntry),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    suspend fun queueJoinRequest(
        syncQueueDao: SyncQueueDao,
        request: ProjectJoinRequest,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.JOIN_REQUEST,
            entityId = request.id,
            operation = operation,
            entityJson = json.encodeToString(request),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    suspend fun queueMilestone(
        syncQueueDao: SyncQueueDao,
        milestone: Milestone,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.MILESTONE,
            entityId = milestone.id,
            operation = operation,
            entityJson = json.encodeToString(milestone),
            priority = priority
        )
        syncQueueDao.insert(item)
    }

    suspend fun queueTaskDependency(
        syncQueueDao: SyncQueueDao,
        dependency: TaskDependency,
        operation: SyncOperation,
        priority: Int = 0
    ) {
        val item = SyncQueueItem(
            entityType = SyncEntityType.TASK_DEPENDENCY,
            entityId = dependency.id,
            operation = operation,
            entityJson = json.encodeToString(dependency),
            priority = priority
        )
        syncQueueDao.insert(item)
    }
}
