package com.example.kosmos.features.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Reminder Scheduler
 *
 * Schedules WorkManager jobs for due date reminders.
 * Sends notifications at: 1 week, 3 days, 1 day, 1 hour before due date.
 *
 * Features:
 * - Reliable background execution via WorkManager
 * - Auto-cancel when task completed
 * - Reschedule on due date change
 * - Persistent across app restarts
 *
 * Usage:
 * ```kotlin
 * reminderScheduler.scheduleReminders(task)
 * ```
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ReminderScheduler"
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule reminders for a task
     * Cancels existing reminders and schedules new ones
     *
     * @param task The task to schedule reminders for
     */
    fun scheduleReminders(task: Task) {
        // Don't schedule if no due date or already done
        if (task.dueDate == null || task.status == TaskStatus.DONE) {
            Log.d(TAG, "Skipping reminders for task ${task.id} (no due date or done)")
            return
        }

        // Cancel existing reminders first
        cancelReminders(task.id)

        val now = System.currentTimeMillis()
        val dueDate = task.dueDate

        // Schedule 1 week before (if applicable)
        val oneWeekBefore = dueDate - TimeUnit.DAYS.toMillis(7)
        if (oneWeekBefore > now) {
            scheduleReminder(
                taskId = task.id,
                taskTitle = task.title,
                triggerTime = oneWeekBefore,
                reminderType = "one_week_before"
            )
            Log.d(TAG, "Scheduled 1-week reminder for task ${task.id}")
        }

        // Schedule 3 days before (if applicable)
        val threeDaysBefore = dueDate - TimeUnit.DAYS.toMillis(3)
        if (threeDaysBefore > now) {
            scheduleReminder(
                taskId = task.id,
                taskTitle = task.title,
                triggerTime = threeDaysBefore,
                reminderType = "three_days_before"
            )
            Log.d(TAG, "Scheduled 3-day reminder for task ${task.id}")
        }

        // Schedule 1 day before (if applicable)
        val oneDayBefore = dueDate - TimeUnit.DAYS.toMillis(1)
        if (oneDayBefore > now) {
            scheduleReminder(
                taskId = task.id,
                taskTitle = task.title,
                triggerTime = oneDayBefore,
                reminderType = "one_day_before"
            )
            Log.d(TAG, "Scheduled 1-day reminder for task ${task.id}")
        }

        // Schedule 1 hour before (if applicable)
        val oneHourBefore = dueDate - TimeUnit.HOURS.toMillis(1)
        if (oneHourBefore > now) {
            scheduleReminder(
                taskId = task.id,
                taskTitle = task.title,
                triggerTime = oneHourBefore,
                reminderType = "one_hour_before"
            )
            Log.d(TAG, "Scheduled 1-hour reminder for task ${task.id}")
        }
    }

    /**
     * Schedule a single reminder
     *
     * @param taskId The task ID
     * @param taskTitle The task title
     * @param triggerTime When to trigger (timestamp)
     * @param reminderType Type of reminder (for unique work name)
     */
    private fun scheduleReminder(
        taskId: String,
        taskTitle: String,
        triggerTime: Long,
        reminderType: String
    ) {
        val now = System.currentTimeMillis()
        val delay = triggerTime - now

        if (delay <= 0) {
            Log.w(TAG, "Reminder time in the past, skipping: $reminderType for task $taskId")
            return
        }

        // Create input data
        val inputData = workDataOf(
            "task_id" to taskId,
            "task_title" to taskTitle,
            "reminder_type" to reminderType
        )

        // Create one-time work request
        val reminderWork = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(getWorkTag(taskId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // Enqueue unique work
        workManager.enqueueUniqueWork(
            getWorkName(taskId, reminderType),
            ExistingWorkPolicy.REPLACE,
            reminderWork
        )

        Log.d(TAG, "Scheduled $reminderType reminder for task $taskId in ${delay / 1000}s")
    }

    /**
     * Cancel all reminders for a task
     *
     * @param taskId The task ID
     */
    fun cancelReminders(taskId: String) {
        try {
            workManager.cancelAllWorkByTag(getWorkTag(taskId))
            Log.d(TAG, "Cancelled all reminders for task $taskId")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to cancel reminders for task $taskId", e)
        }
    }

    /**
     * Reschedule reminders for a task
     * Used when due date is updated
     *
     * @param task The task with updated due date
     */
    fun rescheduleReminders(task: Task) {
        Log.d(TAG, "Rescheduling reminders for task ${task.id}")
        cancelReminders(task.id)
        scheduleReminders(task)
    }

    /**
     * Cancel all reminders for a project
     * Used when user leaves project or project is deleted
     *
     * @param projectId The project ID
     */
    fun cancelRemindersForProject(projectId: String) {
        try {
            workManager.cancelAllWorkByTag("project_$projectId")
            Log.d(TAG, "Cancelled all reminders for project $projectId")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to cancel reminders for project $projectId", e)
        }
    }

    /**
     * Get unique work name for a task reminder
     */
    private fun getWorkName(taskId: String, reminderType: String): String {
        return "reminder_${taskId}_$reminderType"
    }

    /**
     * Get work tag for a task
     */
    private fun getWorkTag(taskId: String): String {
        return "task_reminder_$taskId"
    }
}
