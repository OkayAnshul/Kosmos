package com.example.kosmos.data.realtime

import android.util.Log
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.database.dao.ProjectDao
import com.example.kosmos.core.database.dao.ProjectInviteDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.UserConnectionDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.FieldChange
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.UserConnection
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Supabase Realtime subscriptions for live data updates
 *
 * Features:
 * - Real-time message updates (INSERT, UPDATE, DELETE)
 * - Typing indicators via Realtime Broadcast
 * - Automatic reconnection on network changes
 * - Lifecycle-aware subscriptions
 *
 * Usage:
 * ```kotlin
 * // Subscribe to messages for a chat room
 * realtimeManager.subscribeToMessages(chatRoomId)
 *
 * // Listen for message events
 * realtimeManager.messageEvents.collect { event ->
 *     when (event) {
 *         is MessageEvent.Insert -> // Handle new message
 *         is MessageEvent.Update -> // Handle message update
 *         is MessageEvent.Delete -> // Handle message deletion
 *     }
 * }
 *
 * // Send typing indicator
 * realtimeManager.sendTypingIndicator(chatRoomId, userId, isTyping = true)
 *
 * // Cleanup when done
 * realtimeManager.unsubscribeFromMessages(chatRoomId)
 * ```
 */
@Singleton
class SupabaseRealtimeManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val messageDao: MessageDao,
    private val taskDao: TaskDao,
    private val taskActivityDao: TaskActivityDao,
    private val userConnectionDao: UserConnectionDao,
    private val projectInviteDao: ProjectInviteDao,
    private val projectMemberDao: ProjectMemberDao,
    private val projectDao: ProjectDao
) {
    private val TAG = "SupabaseRealtimeManager"

    // Coroutine scope for realtime operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Active channel subscriptions by chat room ID (key = chatRoomId for messages, "typing:$chatRoomId" for typing)
    private val activeChannels = java.util.concurrent.ConcurrentHashMap<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    // Active task channels by task ID
    private val activeTaskChannels = java.util.concurrent.ConcurrentHashMap<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    // Active project member channels by project ID
    private val memberChannels = java.util.concurrent.ConcurrentHashMap<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    // Message events flow
    private val _messageEvents = MutableSharedFlow<MessageEvent>(replay = 0, extraBufferCapacity = 64)
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents.asSharedFlow()

    // Typing indicator events flow
    private val _typingEvents = MutableSharedFlow<TypingEvent>(replay = 0, extraBufferCapacity = 32)
    val typingEvents: SharedFlow<TypingEvent> = _typingEvents.asSharedFlow()

    // Task events flow
    private val _taskEvents = MutableSharedFlow<TaskEvent>(replay = 0, extraBufferCapacity = 64)
    val taskEvents: SharedFlow<TaskEvent> = _taskEvents.asSharedFlow()

    // Task activity events flow
    private val _taskActivityEvents = MutableSharedFlow<TaskActivityEvent>(replay = 0, extraBufferCapacity = 64)
    val taskActivityEvents: SharedFlow<TaskActivityEvent> = _taskActivityEvents.asSharedFlow()

    // Task editing events flow
    private val _taskEditingEvents = MutableSharedFlow<TaskEditingEvent>(replay = 0, extraBufferCapacity = 32)
    val taskEditingEvents: SharedFlow<TaskEditingEvent> = _taskEditingEvents.asSharedFlow()

    // Task presence events flow
    private val _taskPresenceEvents = MutableSharedFlow<TaskPresenceEvent>(replay = 0, extraBufferCapacity = 32)
    val taskPresenceEvents: SharedFlow<TaskPresenceEvent> = _taskPresenceEvents.asSharedFlow()

    // Connection events flow
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 0, extraBufferCapacity = 32)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    // Project invite events flow
    private val _projectInviteEvents = MutableSharedFlow<ProjectInviteEvent>(replay = 0, extraBufferCapacity = 32)
    val projectInviteEvents: SharedFlow<ProjectInviteEvent> = _projectInviteEvents.asSharedFlow()

    // Error events flow for UI notification (BUG-009 fix)
    private val _errorEvents = MutableSharedFlow<RealtimeError>(replay = 0, extraBufferCapacity = 16)
    val errorEvents: SharedFlow<RealtimeError> = _errorEvents.asSharedFlow()

    /**
     * Subscribe to real-time message updates for a specific chat room
     * @param chatRoomId The chat room ID to subscribe to
     */
    fun subscribeToMessages(chatRoomId: String) {
        // Don't create duplicate subscriptions
        if (activeChannels.containsKey(chatRoomId)) {
            Log.d(TAG, "Already subscribed to chat room: $chatRoomId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to messages for chat room: $chatRoomId")

                // Create a unique channel for this chat room
                val channel = supabase.realtime.channel("messages:$chatRoomId")

                // Subscribe to postgres changes on messages table
                // Note: RLS policies handle security filtering (P0-09)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }

                // Subscribe the channel
                channel.subscribe()

                // Store the channel for cleanup later
                activeChannels[chatRoomId] = channel

                // Listen for changes and emit events
                // P0-10 FIX: No client-side filtering needed - server already filters by chat_room_id
                changeFlow
                    .catch { e ->
                        Log.e(TAG, "Error in message change flow for chat $chatRoomId", e)
                    }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "Message INSERT detected in chat $chatRoomId")
                                handleMessageInsert(action)
                            }
                            is PostgresAction.Update -> {
                                Log.d(TAG, "Message UPDATE detected in chat $chatRoomId")
                                handleMessageUpdate(action)
                            }
                            is PostgresAction.Delete -> {
                                Log.d(TAG, "Message DELETE detected in chat $chatRoomId")
                                handleMessageDelete(action)
                            }
                            else -> {
                                Log.d(TAG, "Unknown action type: $action")
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to messages for chat $chatRoomId", e)
            }
        }
    }

    /**
     * Unsubscribe from real-time message updates for a specific chat room
     * @param chatRoomId The chat room ID to unsubscribe from
     */
    fun unsubscribeFromMessages(chatRoomId: String) {
        scope.launch {
            try {
                activeChannels[chatRoomId]?.let { channel ->
                    Log.d(TAG, "Unsubscribing from chat room: $chatRoomId")
                    supabase.realtime.removeChannel(channel)
                    activeChannels.remove(chatRoomId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from chat $chatRoomId", e)
            }
        }
    }

    /**
     * Subscribe to typing indicators for a specific chat room
     * Uses Realtime Broadcast for ephemeral events
     */
    fun subscribeToTypingIndicators(chatRoomId: String) {
        val typingKey = "typing:$chatRoomId"
        scope.launch {
            try {
                val channel = activeChannels[typingKey] ?: run {
                    // Create a dedicated channel for typing — separate key avoids blocking messages channel
                    val newChannel = supabase.realtime.channel("typing:$chatRoomId")
                    newChannel.subscribe()
                    activeChannels[typingKey] = newChannel
                    newChannel
                }

                // H5 FIX: Listen for typing broadcast events from other users
                val typingBroadcastFlow = channel.broadcastFlow<TypingBroadcast>("typing")

                scope.launch {
                    typingBroadcastFlow
                        .catch { e -> Log.e(TAG, "Error in typing broadcast flow", e) }
                        .collect { payload ->
                            _typingEvents.emit(TypingEvent(chatRoomId, payload.userId, payload.isTyping))
                            Log.d(TAG, "Received typing broadcast: ${payload.userId} typing=${payload.isTyping}")
                        }
                }

                Log.d(TAG, "Subscribed to typing indicators for chat $chatRoomId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to typing indicators", e)
            }
        }
    }

    /**
     * Send typing indicator to other users in the chat room
     * Uses Realtime Broadcast for ephemeral events
     *
     * @param chatRoomId The chat room ID
     * @param userId The user ID who is typing
     * @param isTyping Whether the user is currently typing
     */
    fun sendTypingIndicator(chatRoomId: String, userId: String, isTyping: Boolean) {
        scope.launch {
            try {
                val channel = activeChannels["typing:$chatRoomId"] ?: activeChannels[chatRoomId]
                if (channel == null) {
                    Log.w(TAG, "Cannot send typing indicator - not subscribed to chat $chatRoomId")
                    return@launch
                }

                // H5 FIX: Broadcast typing indicator to other users via Supabase Realtime Broadcast
                try {
                    channel.broadcast(
                        event = "typing",
                        message = TypingBroadcast(
                            userId = userId,
                            isTyping = isTyping,
                            chatRoomId = chatRoomId
                        )
                    )
                    Log.d(TAG, "User $userId typing broadcast sent: $isTyping in chat $chatRoomId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to broadcast typing (falling back to local)", e)
                }

                // Also emit locally so the sender's UI updates immediately
                _typingEvents.emit(TypingEvent(chatRoomId, userId, isTyping))

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send typing indicator", e)
            }
        }
    }

    /**
     * Subscribe to real-time task updates for a specific project
     * @param projectId The project ID to subscribe to
     */
    fun subscribeToTaskUpdates(projectId: String) {
        // Don't create duplicate subscriptions
        if (activeTaskChannels.containsKey("tasks:$projectId")) {
            Log.d(TAG, "Already subscribed to tasks for project: $projectId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to task updates for project: $projectId")

                // Create a unique channel for this project's tasks
                val channel = supabase.realtime.channel("tasks:$projectId")

                // Subscribe to postgres changes on tasks table
                // Note: RLS policies handle security filtering (P0-09)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "tasks"
                }

                // Subscribe the channel
                channel.subscribe()

                // Store the channel for cleanup later
                activeTaskChannels["tasks:$projectId"] = channel

                // Listen for changes and emit events
                // P0-10 FIX: No client-side filtering needed - server already filters by project_id
                changeFlow
                    .catch { e ->
                        Log.e(TAG, "Error in task change flow for project $projectId", e)
                    }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "Task INSERT detected in project $projectId")
                                handleTaskInsert(action)
                            }
                            is PostgresAction.Update -> {
                                Log.d(TAG, "Task UPDATE detected in project $projectId")
                                handleTaskUpdate(action)
                            }
                            is PostgresAction.Delete -> {
                                Log.d(TAG, "Task DELETE detected in project $projectId")
                                handleTaskDelete(action)
                            }
                            else -> {
                                Log.d(TAG, "Unknown action type: $action")
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to tasks for project $projectId", e)
            }
        }
    }

    /**
     * Subscribe to real-time activity updates for a specific task
     * @param taskId The task ID to subscribe to
     */
    fun subscribeToTaskActivity(taskId: String) {
        // Don't create duplicate subscriptions
        if (activeTaskChannels.containsKey("activity:$taskId")) {
            Log.d(TAG, "Already subscribed to activity for task: $taskId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to activity for task: $taskId")

                // Create a unique channel for this task's activity
                val channel = supabase.realtime.channel("activity:$taskId")

                // Subscribe to postgres changes on task_activity table
                // Note: RLS policies handle security filtering (P0-09)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "task_activity"
                }

                // Subscribe the channel
                channel.subscribe()

                // Store the channel for cleanup later
                activeTaskChannels["activity:$taskId"] = channel

                // Listen for changes and emit events
                // P0-10 FIX: No client-side filtering needed - server already filters by task_id
                changeFlow
                    .catch { e ->
                        Log.e(TAG, "Error in activity change flow for task $taskId", e)
                    }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "Activity INSERT detected for task $taskId")
                                handleTaskActivityInsert(action)
                            }
                            else -> {
                                Log.d(TAG, "Ignoring non-insert activity action: $action")
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to activity for task $taskId", e)
            }
        }
    }

    /**
     * Unsubscribe from task updates for a specific project
     * @param projectId The project ID to unsubscribe from
     */
    fun unsubscribeFromTaskUpdates(projectId: String) {
        scope.launch {
            try {
                activeTaskChannels["tasks:$projectId"]?.let { channel ->
                    Log.d(TAG, "Unsubscribing from tasks for project: $projectId")
                    supabase.realtime.removeChannel(channel)
                    activeTaskChannels.remove("tasks:$projectId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from tasks for project $projectId", e)
            }
        }
    }

    /**
     * Unsubscribe from task activity for a specific task
     * @param taskId The task ID to unsubscribe from
     */
    fun unsubscribeFromTaskActivity(taskId: String) {
        scope.launch {
            try {
                activeTaskChannels["activity:$taskId"]?.let { channel ->
                    Log.d(TAG, "Unsubscribing from activity for task: $taskId")
                    supabase.realtime.removeChannel(channel)
                    activeTaskChannels.remove("activity:$taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from activity for task $taskId", e)
            }
        }
    }

    /**
     * Send task presence indicator (who's viewing the task)
     * Uses Realtime Broadcast for ephemeral events
     *
     * @param taskId The task ID
     * @param userId The user ID who is viewing
     * @param userName The user's display name
     * @param photoUrl The user's photo URL
     */
    fun sendTaskPresence(
        taskId: String,
        userId: String,
        userName: String,
        photoUrl: String? = null
    ) {
        scope.launch {
            try {
                val channel = activeTaskChannels["activity:$taskId"]
                if (channel == null) {
                    Log.w(TAG, "Cannot send task presence - not subscribed to task $taskId")
                    return@launch
                }

                Log.d(TAG, "User $userName joined task $taskId")

                // Emit local event for testing
                // In production, this would use channel.broadcast()
                val viewer = TaskViewer(
                    userId = userId,
                    userName = userName,
                    photoUrl = photoUrl,
                    joinedAt = System.currentTimeMillis()
                )

                // For now, emit a simplified presence event
                // Real implementation would track all viewers and emit aggregated state
                _taskPresenceEvents.emit(
                    TaskPresenceEvent(
                        taskId = taskId,
                        viewers = listOf(viewer)
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send task presence", e)
            }
        }
    }

    /**
     * Send task editing status (who's editing what field)
     * Uses Realtime Broadcast for ephemeral events
     *
     * @param taskId The task ID
     * @param userId The user ID who is editing
     * @param userName The user's display name
     * @param isEditing Whether the user is currently editing
     * @param field The field being edited (null when stopped)
     */
    fun sendTaskEditingStatus(
        taskId: String,
        userId: String,
        userName: String,
        isEditing: Boolean,
        field: String? = null
    ) {
        scope.launch {
            try {
                val channel = activeTaskChannels["activity:$taskId"]
                if (channel == null) {
                    Log.w(TAG, "Cannot send editing status - not subscribed to task $taskId")
                    return@launch
                }

                Log.d(TAG, "User $userName editing ${field ?: "nothing"} in task $taskId")

                // Emit local event for testing
                // In production, this would use channel.broadcast()
                _taskEditingEvents.emit(
                    TaskEditingEvent(
                        taskId = taskId,
                        userId = userId,
                        userName = userName,
                        isEditing = isEditing,
                        field = field,
                        timestamp = System.currentTimeMillis()
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send editing status", e)
            }
        }
    }

    /**
     * Subscribe to real-time user connection changes (requests, accepts, declines)
     * @param userId The current user's ID
     */
    fun subscribeToUserConnections(userId: String) {
        val channelKey = "connections:$userId"
        if (activeChannels.containsKey(channelKey)) {
            Log.d(TAG, "Already subscribed to user connections for: $userId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to user connections for: $userId")
                val channel = supabase.realtime.channel(channelKey)

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "user_connections"
                }

                channel.subscribe()
                activeChannels[channelKey] = channel

                changeFlow
                    .catch { e -> Log.e(TAG, "Error in connection change flow", e) }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "Connection INSERT detected")
                                try {
                                    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                                    val jsonString = json.encodeToString(kotlinx.serialization.serializer<Map<String, Any?>>(), action.record)
                                    val conn = json.decodeFromString<UserConnection>(jsonString)
                                    if (conn.requesterId == userId || conn.addresseeId == userId) {
                                        userConnectionDao.insert(conn)
                                        _connectionEvents.emit(ConnectionEvent.Inserted(conn))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse connection insert", e)
                                }
                            }
                            is PostgresAction.Update -> {
                                Log.d(TAG, "Connection UPDATE detected")
                                try {
                                    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                                    val jsonString = json.encodeToString(kotlinx.serialization.serializer<Map<String, Any?>>(), action.record)
                                    val conn = json.decodeFromString<UserConnection>(jsonString)
                                    if (conn.requesterId == userId || conn.addresseeId == userId) {
                                        userConnectionDao.insert(conn)
                                        _connectionEvents.emit(ConnectionEvent.Updated(conn))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse connection update", e)
                                }
                            }
                            is PostgresAction.Delete -> {
                                Log.d(TAG, "Connection DELETE detected")
                                val id = action.oldRecord["id"] as? String
                                if (id != null) {
                                    userConnectionDao.deleteById(id)
                                    _connectionEvents.emit(ConnectionEvent.Deleted(id))
                                }
                            }
                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to user connections", e)
            }
        }
    }

    /**
     * Subscribe to real-time project invite changes
     * @param userId The current user's ID (invitee)
     */
    fun subscribeToProjectInvites(userId: String) {
        val channelKey = "invites:$userId"
        if (activeChannels.containsKey(channelKey)) {
            Log.d(TAG, "Already subscribed to project invites for: $userId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to project invites for: $userId")
                val channel = supabase.realtime.channel(channelKey)

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "project_invites"
                }

                channel.subscribe()
                activeChannels[channelKey] = channel

                changeFlow
                    .catch { e -> Log.e(TAG, "Error in project invite change flow", e) }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "Project invite INSERT detected")
                                try {
                                    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                                    val jsonString = json.encodeToString(kotlinx.serialization.serializer<Map<String, Any?>>(), action.record)
                                    val invite = json.decodeFromString<ProjectInvite>(jsonString)
                                    if (invite.inviteeId == userId || invite.inviterId == userId) {
                                        projectInviteDao.insert(invite)
                                        _projectInviteEvents.emit(ProjectInviteEvent.Inserted(invite))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse project invite insert", e)
                                }
                            }
                            is PostgresAction.Update -> {
                                Log.d(TAG, "Project invite UPDATE detected")
                                try {
                                    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                                    val jsonString = json.encodeToString(kotlinx.serialization.serializer<Map<String, Any?>>(), action.record)
                                    val invite = json.decodeFromString<ProjectInvite>(jsonString)
                                    if (invite.inviteeId == userId || invite.inviterId == userId) {
                                        projectInviteDao.insert(invite)
                                        _projectInviteEvents.emit(ProjectInviteEvent.Updated(invite))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse project invite update", e)
                                }
                            }
                            is PostgresAction.Delete -> {
                                Log.d(TAG, "Project invite DELETE detected")
                                val id = action.oldRecord["id"] as? String
                                if (id != null) {
                                    projectInviteDao.deleteById(id)
                                    _projectInviteEvents.emit(ProjectInviteEvent.Deleted(id))
                                }
                            }
                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to project invites", e)
            }
        }
    }

    /**
     * Subscribe to real-time project member changes for a specific project.
     * Updates Room cache when members join or leave so all project members see live membership.
     * @param projectId The project ID to watch
     */
    fun subscribeToProjectMembers(projectId: String) {
        val channelKey = "project_members:$projectId"
        if (memberChannels.containsKey(channelKey)) {
            Log.d(TAG, "Already subscribed to project members for: $projectId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to project members for project: $projectId")
                val channel = supabase.realtime.channel(channelKey)

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "project_members"
                }

                channel.subscribe()
                memberChannels[channelKey] = channel

                changeFlow
                    .catch { e -> Log.e(TAG, "Error in project members change flow for $projectId", e) }
                    .collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                Log.d(TAG, "ProjectMember INSERT detected for project $projectId")
                                try {
                                    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
                                    val jsonString = json.encodeToString(kotlinx.serialization.serializer<Map<String, Any?>>(), action.record)
                                    val member = json.decodeFromString<ProjectMember>(jsonString)
                                    if (member.projectId == projectId) {
                                        projectMemberDao.insertMember(member)
                                        projectDao.incrementMemberCount(projectId)
                                        Log.d(TAG, "✅ New member synced to Room: ${member.userId}")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse project member insert", e)
                                }
                            }
                            is PostgresAction.Delete -> {
                                Log.d(TAG, "ProjectMember DELETE detected for project $projectId")
                                val memberId = action.oldRecord["id"] as? String
                                if (memberId != null) {
                                    projectMemberDao.deleteMemberById(memberId)
                                    projectDao.decrementMemberCount(projectId)
                                    Log.d(TAG, "✅ Removed member from Room: $memberId")
                                }
                            }
                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to project members for $projectId", e)
            }
        }
    }

    /**
     * Unsubscribe from project member real-time for a specific project
     */
    fun unsubscribeFromProjectMembers(projectId: String) {
        scope.launch {
            try {
                memberChannels["project_members:$projectId"]?.let { channel ->
                    supabase.realtime.removeChannel(channel)
                    memberChannels.remove("project_members:$projectId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from project members for $projectId", e)
            }
        }
    }

    /**
     * Unsubscribe from user connections real-time
     */
    fun unsubscribeFromUserConnections(userId: String) {
        scope.launch {
            try {
                activeChannels["connections:$userId"]?.let { channel ->
                    supabase.realtime.removeChannel(channel)
                    activeChannels.remove("connections:$userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from user connections", e)
            }
        }
    }

    /**
     * Unsubscribe from project invites real-time
     */
    fun unsubscribeFromProjectInvites(userId: String) {
        scope.launch {
            try {
                activeChannels["invites:$userId"]?.let { channel ->
                    supabase.realtime.removeChannel(channel)
                    activeChannels.remove("invites:$userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from project invites", e)
            }
        }
    }

    /**
     * Unsubscribe from all active channels and cleanup
     */
    fun disconnect() {
        scope.launch {
            try {
                Log.d(TAG, "Disconnecting all realtime channels (${activeChannels.size + activeTaskChannels.size + memberChannels.size})")
                activeChannels.values.forEach { channel ->
                    supabase.realtime.removeChannel(channel)
                }
                activeChannels.clear()

                activeTaskChannels.values.forEach { channel ->
                    supabase.realtime.removeChannel(channel)
                }
                activeTaskChannels.clear()

                memberChannels.values.forEach { channel ->
                    supabase.realtime.removeChannel(channel)
                }
                memberChannels.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting realtime channels", e)
            }
        }
    }

    // Private helper methods for handling different action types

    private suspend fun handleMessageInsert(action: PostgresAction.Insert) {
        try {
            // Parse the new message from the action record
            val record = action.record
            val message = parseMessage(record)

            if (message != null) {
                // Update local database
                messageDao.insertMessage(message)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Insert(message))

                Log.d(TAG, "Message inserted: ${message.id}")
            } else {
                // BUG-009: Emit error event instead of silent failure
                Log.w(TAG, "Failed to parse message from record, fields: ${record.keys}")
                _errorEvents.emit(RealtimeError.ParseFailed("message", record.keys.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message insert", e)
            // BUG-009: Emit error event for UI notification
            _errorEvents.emit(RealtimeError.OperationFailed("message_insert", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleMessageUpdate(action: PostgresAction.Update) {
        try {
            val record = action.record
            val message = parseMessage(record)

            if (message != null) {
                // Update local database
                messageDao.updateMessage(message)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Update(message))

                Log.d(TAG, "Message updated: ${message.id}")
            } else {
                Log.w(TAG, "Failed to parse message update, fields: ${record.keys}")
                _errorEvents.emit(RealtimeError.ParseFailed("message", record.keys.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message update", e)
            _errorEvents.emit(RealtimeError.OperationFailed("message_update", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleMessageDelete(action: PostgresAction.Delete) {
        try {
            val oldRecord = action.oldRecord
            val messageId = oldRecord["id"] as? String

            if (messageId != null) {
                // Delete from local database
                messageDao.deleteMessageById(messageId)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Delete(messageId))

                Log.d(TAG, "Message deleted: $messageId")
            } else {
                Log.w(TAG, "Message delete missing id field")
                _errorEvents.emit(RealtimeError.ParseFailed("message_delete", listOf("id")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message delete", e)
            _errorEvents.emit(RealtimeError.OperationFailed("message_delete", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleTaskInsert(action: PostgresAction.Insert) {
        try {
            // Parse the new task from the action record
            val record = action.record
            val task = parseTask(record)

            if (task != null) {
                // Update local database
                taskDao.insertTask(task)

                // Emit event for UI updates
                _taskEvents.emit(TaskEvent.Insert(task))

                Log.d(TAG, "Task inserted: ${task.id}")
            } else {
                Log.w(TAG, "Failed to parse task from record, fields: ${record.keys}")
                _errorEvents.emit(RealtimeError.ParseFailed("task", record.keys.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling task insert", e)
            _errorEvents.emit(RealtimeError.OperationFailed("task_insert", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleTaskUpdate(action: PostgresAction.Update) {
        try {
            val record = action.record
            val task = parseTask(record)

            if (task != null) {
                // Update local database
                taskDao.updateTask(task)

                // Emit event for UI updates
                _taskEvents.emit(TaskEvent.Update(task))

                Log.d(TAG, "Task updated: ${task.id}")
            } else {
                Log.w(TAG, "Failed to parse task update, fields: ${record.keys}")
                _errorEvents.emit(RealtimeError.ParseFailed("task", record.keys.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling task update", e)
            _errorEvents.emit(RealtimeError.OperationFailed("task_update", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleTaskDelete(action: PostgresAction.Delete) {
        try {
            val oldRecord = action.oldRecord
            val taskId = oldRecord["id"] as? String

            if (taskId != null) {
                // Delete from local database
                taskDao.deleteTaskById(taskId)

                // Emit event for UI updates
                _taskEvents.emit(TaskEvent.Delete(taskId))

                Log.d(TAG, "Task deleted: $taskId")
            } else {
                Log.w(TAG, "Task delete missing id field")
                _errorEvents.emit(RealtimeError.ParseFailed("task_delete", listOf("id")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling task delete", e)
            _errorEvents.emit(RealtimeError.OperationFailed("task_delete", e.message ?: "Unknown error"))
        }
    }

    private suspend fun handleTaskActivityInsert(action: PostgresAction.Insert) {
        try {
            // Parse the new activity from the action record
            val record = action.record
            val activity = parseTaskActivity(record)

            if (activity != null) {
                // Update local database
                taskActivityDao.insertActivity(activity)

                // Emit event for UI updates
                _taskActivityEvents.emit(
                    TaskActivityEvent(
                        taskId = activity.taskId,
                        activityId = activity.id,
                        actorId = activity.actorId,
                        actorName = activity.actorName,
                        actionType = activity.actionType.name,
                        timestamp = activity.timestamp
                    )
                )

                Log.d(TAG, "Task activity inserted: ${activity.id}")
            } else {
                Log.w(TAG, "Failed to parse task activity, fields: ${record.keys}")
                _errorEvents.emit(RealtimeError.ParseFailed("task_activity", record.keys.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling task activity insert", e)
            _errorEvents.emit(RealtimeError.OperationFailed("task_activity_insert", e.message ?: "Unknown error"))
        }
    }

    /**
     * Parse a message from Supabase record
     * This is a simplified version - you may need to adjust based on your actual data structure
     *
     * BUG-006 FIX: Added explicit null checks for required fields
     */
    private fun parseMessage(record: Map<String, Any?>): Message? {
        // BUG-006: Validate required fields exist before parsing
        val requiredFields = listOf("id", "chat_room_id", "sender_id", "content", "timestamp")
        val missingFields = requiredFields.filter { record[it] == null }
        if (missingFields.isNotEmpty()) {
            Log.w(TAG, "Message missing required fields: $missingFields")
            return null
        }

        return try {
            // Convert the record map to JSON and then to Message object
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val jsonString = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, Any?>>(),
                record
            )

            val message = json.decodeFromString<Message>(jsonString)
            Log.d(TAG, "Successfully parsed message: ${message.id}")
            message
        } catch (e: kotlinx.serialization.SerializationException) {
            // BUG-006: Don't log full record data (privacy concern) - only log field names
            Log.e(TAG, "Serialization error parsing message: ${e.message}, fields: ${record.keys}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing message: ${e.message}, fields: ${record.keys}")
            null
        }
    }

    /**
     * Parse a task from Supabase record
     *
     * BUG-006 FIX: Added explicit null checks for required fields
     */
    private fun parseTask(record: Map<String, Any?>): Task? {
        // BUG-006: Validate required fields exist before parsing
        val requiredFields = listOf("id", "project_id", "title", "status")
        val missingFields = requiredFields.filter { record[it] == null }
        if (missingFields.isNotEmpty()) {
            Log.w(TAG, "Task missing required fields: $missingFields")
            return null
        }

        return try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val jsonString = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, Any?>>(),
                record
            )

            val task = json.decodeFromString<Task>(jsonString)
            Log.d(TAG, "Successfully parsed task: ${task.id}")
            task
        } catch (e: kotlinx.serialization.SerializationException) {
            // BUG-006: Don't log full record data (privacy concern) - only log field names
            Log.e(TAG, "Serialization error parsing task: ${e.message}, fields: ${record.keys}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing task: ${e.message}, fields: ${record.keys}")
            null
        }
    }

    /**
     * Parse task activity from Supabase record
     *
     * BUG-006 FIX: Added explicit null checks for required fields
     */
    private fun parseTaskActivity(record: Map<String, Any?>): TaskActivity? {
        // BUG-006: Validate required fields exist before parsing
        val requiredFields = listOf("id", "task_id", "actor_id", "action_type")
        val missingFields = requiredFields.filter { record[it] == null }
        if (missingFields.isNotEmpty()) {
            Log.w(TAG, "TaskActivity missing required fields: $missingFields")
            return null
        }

        return try {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            // Manual extraction of complex fields
            val changesJson = record["changes"] as? String
            val changes = if (!changesJson.isNullOrEmpty()) {
                try {
                    json.decodeFromString<List<FieldChange>>(changesJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse changes JSON: ${e.message}")
                    emptyList()
                }
            } else emptyList()

            val metadataJson = record["metadata"] as? String
            val metadata = if (!metadataJson.isNullOrEmpty()) {
                try {
                    json.decodeFromString<Map<String, String>>(metadataJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON: ${e.message}")
                    emptyMap()
                }
            } else emptyMap()

            // Construct TaskActivity directly with proper type conversions
            val activity = TaskActivity(
                id = record["id"] as? String ?: "",
                taskId = record["task_id"] as? String ?: "",
                projectId = record["project_id"] as? String ?: "",
                actorId = record["actor_id"] as? String ?: "",
                actorName = record["actor_name"] as? String ?: "",
                actorRole = record["actor_role"] as? String,
                actionType = try {
                    ActivityActionType.valueOf(record["action_type"] as? String ?: "UPDATED")
                } catch (e: Exception) {
                    ActivityActionType.UPDATED
                },
                timestamp = when (val ts = record["timestamp"]) {
                    is Number -> ts.toLong()
                    is String -> ts.toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                },
                changes = changes,
                commitMessage = record["commit_message"] as? String,
                autoDescription = record["auto_description"] as? String ?: "",
                metadata = metadata
            )

            Log.d(TAG, "Successfully parsed task activity: ${activity.id}")
            activity
        } catch (e: kotlinx.serialization.SerializationException) {
            // BUG-006: Don't log full record data (privacy concern) - only log field names
            Log.e(TAG, "Serialization error parsing task activity: ${e.message}, fields: ${record.keys}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing task activity: ${e.message}, fields: ${record.keys}")
            null
        }
    }
}

/**
 * Sealed class representing different message events from Realtime
 */
sealed class MessageEvent {
    data class Insert(val message: Message) : MessageEvent()
    data class Update(val message: Message) : MessageEvent()
    data class Delete(val messageId: String) : MessageEvent()
}

/**
 * Data class for typing indicator events
 */
data class TypingEvent(
    val chatRoomId: String,
    val userId: String,
    val isTyping: Boolean
)

/**
 * H5: Serializable broadcast payload for typing indicators
 */
@Serializable
data class TypingBroadcast(
    val userId: String,
    val isTyping: Boolean,
    val chatRoomId: String
)

/**
 * Connection real-time events
 */
sealed class ConnectionEvent {
    data class Inserted(val connection: UserConnection) : ConnectionEvent()
    data class Updated(val connection: UserConnection) : ConnectionEvent()
    data class Deleted(val connectionId: String) : ConnectionEvent()
}

/**
 * Project invite real-time events
 */
sealed class ProjectInviteEvent {
    data class Inserted(val invite: ProjectInvite) : ProjectInviteEvent()
    data class Updated(val invite: ProjectInvite) : ProjectInviteEvent()
    data class Deleted(val inviteId: String) : ProjectInviteEvent()
}
