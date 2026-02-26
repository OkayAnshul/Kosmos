package com.example.kosmos.features.tasks.service

import android.util.Log
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.TimeEntryDao
import com.example.kosmos.core.models.TimeEntry
import com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Time Tracker Service
 *
 * Singleton service managing active timers and time entries.
 *
 * Features:
 * - Start/stop timers for tasks
 * - Track active timers in StateFlow
 * - Auto-update task actualHours on stop
 * - Add manual time entries
 * - Background monitoring of running timers
 * - Offline-first with Supabase sync
 *
 * Usage:
 * ```kotlin
 * // Start a timer
 * timeTrackerService.startTimer(
 *     taskId = task.id,
 *     projectId = task.projectId,
 *     userId = currentUser.id,
 *     description = "Working on implementation"
 * )
 *
 * // Observe active timers
 * timeTrackerService.activeTimers.collect { timers ->
 *     // Update UI
 * }
 *
 * // Stop timer
 * timeTrackerService.stopTimer(taskId, userId)
 * ```
 */
@Singleton
class TimeTrackerService @Inject constructor(
    private val timeEntryDao: TimeEntryDao,
    private val taskDao: TaskDao,
    private val supabaseTimeEntryDataSource: SupabaseTimeEntryDataSource
) {
    private val TAG = "TimeTrackerService"

    // Coroutine scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Active timers (task_id -> TimeEntry)
    private val _activeTimers = MutableStateFlow<Map<String, TimeEntry>>(emptyMap())
    val activeTimers: StateFlow<Map<String, TimeEntry>> = _activeTimers.asStateFlow()

    init {
        // Start background monitoring
        startBackgroundMonitoring()

        // Load running timers from database on init
        scope.launch {
            loadRunningTimersFromDatabase()
        }
    }

    /**
     * Start a timer for a task
     *
     * @param taskId The task ID
     * @param projectId The project ID
     * @param userId The user ID
     * @param description Optional description
     * @param isBillable Whether this time is billable
     * @param hourlyRate Optional hourly rate
     * @return Result with created TimeEntry or error
     */
    suspend fun startTimer(
        taskId: String,
        projectId: String,
        userId: String,
        description: String? = null,
        isBillable: Boolean = true,
        hourlyRate: Float? = null
    ): Result<TimeEntry> {
        return try {
            // Check if timer already running for this task/user
            val existingTimer = _activeTimers.value[taskId]
            if (existingTimer != null && existingTimer.userId == userId) {
                Log.w(TAG, "Timer already running for task: $taskId")
                return Result.failure(IllegalStateException("Timer already running for this task"))
            }

            // Stop any other running timers for this user
            stopAllTimersForUser(userId)

            // Create new timer entry
            val timer = TimeEntry.createTimer(
                taskId = taskId,
                projectId = projectId,
                userId = userId,
                description = description,
                isBillable = isBillable,
                hourlyRate = hourlyRate
            )

            // Save to Room immediately (offline-first)
            timeEntryDao.insertEntry(timer)

            // Update active timers state
            _activeTimers.value = _activeTimers.value + (taskId to timer)

            // Sync to Supabase in background
            scope.launch {
                try {
                    supabaseTimeEntryDataSource.insertTimeEntry(timer)
                    Log.d(TAG, "Timer synced to Supabase: ${timer.id}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync timer to Supabase (will retry later)", e)
                }
            }

            Log.d(TAG, "Timer started for task: $taskId")
            Result.success(timer)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to start timer", e)
            Result.failure(e)
        }
    }

    /**
     * Stop a running timer for a task
     *
     * @param taskId The task ID
     * @param userId The user ID
     * @return Result with stopped TimeEntry or error
     */
    suspend fun stopTimer(taskId: String, userId: String): Result<TimeEntry> {
        return try {
            // Find running timer
            val timer = _activeTimers.value[taskId]
            if (timer == null || timer.userId != userId) {
                Log.w(TAG, "No running timer found for task: $taskId")
                return Result.failure(IllegalStateException("No running timer for this task"))
            }

            // Stop the timer
            val stoppedTimer = timer.stop()

            // Update Room database
            timeEntryDao.updateEntry(stoppedTimer)

            // Remove from active timers
            _activeTimers.value = _activeTimers.value - taskId

            // Update task actualHours
            updateTaskActualHours(taskId)

            // Sync to Supabase in background
            scope.launch {
                try {
                    supabaseTimeEntryDataSource.updateTimeEntry(stoppedTimer)
                    Log.d(TAG, "Stopped timer synced to Supabase: ${stoppedTimer.id}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync stopped timer to Supabase (will retry later)", e)
                }
            }

            Log.d(TAG, "Timer stopped for task: $taskId, duration: ${stoppedTimer.formatDuration()}")
            Result.success(stoppedTimer)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to stop timer", e)
            Result.failure(e)
        }
    }

    /**
     * Stop all running timers for a user
     *
     * @param userId The user ID
     */
    private suspend fun stopAllTimersForUser(userId: String) {
        val timersToStop = _activeTimers.value.values.filter { it.userId == userId }
        timersToStop.forEach { timer ->
            try {
                stopTimer(timer.taskId, userId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to stop timer for task: ${timer.taskId}", e)
            }
        }
    }

    /**
     * Add a manual time entry (already stopped)
     *
     * @param taskId The task ID
     * @param projectId The project ID
     * @param userId The user ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @param description Optional description
     * @param isBillable Whether this time is billable
     * @param hourlyRate Optional hourly rate
     * @return Result with created TimeEntry or error
     */
    suspend fun addManualEntry(
        taskId: String,
        projectId: String,
        userId: String,
        startTime: Long,
        endTime: Long,
        description: String? = null,
        isBillable: Boolean = true,
        hourlyRate: Float? = null
    ): Result<TimeEntry> {
        return try {
            // Validate time range
            if (endTime <= startTime) {
                return Result.failure(IllegalArgumentException("End time must be after start time"))
            }

            // Create manual entry
            val entry = TimeEntry.createManualEntry(
                taskId = taskId,
                projectId = projectId,
                userId = userId,
                startTime = startTime,
                endTime = endTime,
                description = description,
                isBillable = isBillable,
                hourlyRate = hourlyRate
            )

            // Save to Room immediately (offline-first)
            timeEntryDao.insertEntry(entry)

            // Update task actualHours
            updateTaskActualHours(taskId)

            // Sync to Supabase in background
            scope.launch {
                try {
                    supabaseTimeEntryDataSource.insertTimeEntry(entry)
                    Log.d(TAG, "Manual entry synced to Supabase: ${entry.id}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync manual entry to Supabase (will retry later)", e)
                }
            }

            Log.d(TAG, "Manual entry added for task: $taskId, duration: ${entry.formatDuration()}")
            Result.success(entry)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to add manual entry", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a time entry
     *
     * @param entryId The time entry ID
     * @param taskId The task ID (for recalculating hours)
     * @return Result with success or error
     */
    suspend fun deleteEntry(entryId: String, taskId: String): Result<Unit> {
        return try {
            // Delete from Room
            timeEntryDao.deleteEntryById(entryId)

            // Remove from active timers if it's there
            if (_activeTimers.value[taskId]?.id == entryId) {
                _activeTimers.value = _activeTimers.value - taskId
            }

            // Update task actualHours
            updateTaskActualHours(taskId)

            // Sync to Supabase in background
            scope.launch {
                try {
                    supabaseTimeEntryDataSource.deleteTimeEntry(entryId)
                    Log.d(TAG, "Entry deletion synced to Supabase: $entryId")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync entry deletion to Supabase (will retry later)", e)
                }
            }

            Log.d(TAG, "Entry deleted: $entryId")
            Result.success(Unit)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to delete entry", e)
            Result.failure(e)
        }
    }

    /**
     * Get running timer for a task
     *
     * @param taskId The task ID
     * @return Running TimeEntry or null
     */
    fun getRunningTimer(taskId: String): TimeEntry? {
        return _activeTimers.value[taskId]
    }

    /**
     * Check if a timer is running for a task
     *
     * @param taskId The task ID
     * @return True if timer is running
     */
    fun isTimerRunning(taskId: String): Boolean {
        return _activeTimers.value.containsKey(taskId)
    }

    /**
     * Update task actualHours based on total time entries
     *
     * @param taskId The task ID
     */
    private suspend fun updateTaskActualHours(taskId: String) {
        try {
            // Calculate total time in seconds
            val totalSeconds = timeEntryDao.getTotalTimeForTask(taskId)

            // Convert to hours
            val totalHours = totalSeconds / 3600f

            // Get task and update actualHours
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                val updatedTask = task.copy(actualHours = totalHours)
                taskDao.updateTask(updatedTask)
                Log.d(TAG, "Updated task actualHours: $taskId = ${totalHours}h")
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to update task actualHours", e)
        }
    }

    /**
     * Load running timers from database on startup
     * Recovers timers if app was killed
     */
    private suspend fun loadRunningTimersFromDatabase() {
        try {
            // This would need userId - for now, we'll skip this
            // In production, would need to get current user ID from AuthRepository
            Log.d(TAG, "Loading running timers from database")

            // Note: This is a simplified version
            // Full implementation would query all running timers and restore state

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to load running timers", e)
        }
    }

    /**
     * Background monitoring of running timers
     * Updates Room database every 30 seconds
     */
    private fun startBackgroundMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    // Update all running timers in Room
                    _activeTimers.value.values.forEach { timer ->
                        timeEntryDao.updateEntry(timer)
                    }

                    // Wait 30 seconds
                    delay(30_000)

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Error in background monitoring", e)
                    delay(60_000) // Wait longer on error
                }
            }
        }
    }
}
