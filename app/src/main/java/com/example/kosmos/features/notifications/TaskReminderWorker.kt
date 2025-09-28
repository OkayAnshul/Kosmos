package com.example.kosmos.features.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.models.TaskStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Task Reminder Worker
 *
 * WorkManager background job that sends due date reminders.
 * Triggered by ReminderScheduler at scheduled times.
 *
 * Features:
 * - Checks if task still incomplete
 * - Sends notification via SupabaseNotificationService
 * - Runs reliably even if app is closed
 * - Handles errors gracefully
 *
 * Input Data:
 * - task_id: String (required)
 * - task_title: String (required)
 * - reminder_type: String (one_week_before, three_days_before, one_day_before, one_hour_before)
 */
@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val notificationService: SupabaseNotificationService
) : CoroutineWorker(context, params) {

    private val TAG = "TaskReminderWorker"

    override suspend fun doWork(): Result {
        return try {
            // Get input data
            val taskId = inputData.getString("task_id")
            val taskTitle = inputData.getString("task_title")
            val reminderType = inputData.getString("reminder_type")

            if (taskId == null || taskTitle == null || reminderType == null) {
                Log.e(TAG, "Missing required input data")
                return Result.failure()
            }

            Log.d(TAG, "Processing $reminderType reminder for task: $taskId")

            // Fetch current task state from database
            val task = taskDao.getTaskById(taskId)

            if (task == null) {
                Log.w(TAG, "Task not found: $taskId (may have been deleted)")
                return Result.success() // Don't retry
            }

            // Check if task is already done
            if (task.status == TaskStatus.DONE) {
                Log.d(TAG, "Task already completed, skipping reminder: $taskId")
                return Result.success()
            }

            // Check if task still has assignee
            val assigneeId = task.assignedToId
            if (assigneeId == null) {
                Log.w(TAG, "Task has no assignee, skipping reminder: $taskId")
                return Result.success()
            }

            // Generate notification content
            val (title, body) = generateNotificationContent(task.title, task.dueDate, reminderType)

            // Send notification
            val result = notificationService.sendNotification(
                userId = assigneeId,
                title = title,
                body = body,
                type = "task_reminder",
                data = mapOf(
                    "task_id" to taskId,
                    "project_id" to task.projectId,
                    "reminder_type" to reminderType
                )
            )

            if (result.isSuccess) {
                Log.d(TAG, "Successfully sent $reminderType reminder for task: $taskId")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send reminder: ${result.exceptionOrNull()}")
                Result.retry() // Retry on failure
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error in TaskReminderWorker", e)
            Result.retry()
        }
    }

    /**
     * Generate notification title and body based on reminder type
     *
     * @param taskTitle The task title
     * @param dueDate The due date timestamp
     * @param reminderType The type of reminder
     * @return Pair of (title, body)
     */
    private fun generateNotificationContent(
        taskTitle: String,
        dueDate: Long?,
        reminderType: String
    ): Pair<String, String> {
        val timeUntil = dueDate?.let { formatTimeUntil(it) } ?: "soon"

        return when (reminderType) {
            "one_week_before" -> {
                "Task due in 1 week" to "Don't forget: $taskTitle"
            }
            "three_days_before" -> {
                "Task due in 3 days" to "$taskTitle is due $timeUntil"
            }
            "one_day_before" -> {
                "Task due tomorrow!" to "$taskTitle needs your attention"
            }
            "one_hour_before" -> {
                "Task due in 1 hour!" to "Urgent: $taskTitle is due very soon"
            }
            else -> {
                "Task reminder" to taskTitle
            }
        }
    }

    /**
     * Format time until due date
     *
     * @param dueDate Due date timestamp
     * @return Formatted string (e.g., "in 2 days", "tomorrow at 3:00 PM")
     */
    private fun formatTimeUntil(dueDate: Long): String {
        val now = System.currentTimeMillis()
        val diff = dueDate - now

        if (diff < 0) {
            return "overdue"
        }

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24

        return when {
            days > 1 -> "in $days days"
            days == 1L -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "tomorrow at ${timeFormat.format(Date(dueDate))}"
            }
            hours > 1 -> "in $hours hours"
            else -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "at ${timeFormat.format(Date(dueDate))}"
            }
        }
    }
}
