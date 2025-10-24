package com.example.kosmos.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.kosmos.features.smart.services.ActionDetectionService
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.core.models.ActionItem
import com.example.kosmos.core.models.ActionType
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service for processing detected action items and converting them to tasks
 * This service runs when there are unprocessed action items to handle
 */
@AndroidEntryPoint
class ActionDetectionWorkerService : Service() {

    @Inject
    lateinit var actionDetectionService: ActionDetectionService

    @Inject
    lateinit var taskRepository: TaskRepository

    private var job: Job? = null

    companion object {
        private const val TAG = "ActionDetectionWorker"

        /**
         * Start the action detection service to process unprocessed action items
         */
        fun startService(context: Context) {
            val intent = Intent(context, ActionDetectionWorkerService::class.java)
            context.startService(intent)
            Log.d(TAG, "Action detection service start requested")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Action detection service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action detection service started with startId: $startId")

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                processUnprocessedActions()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing action items", e)
            } finally {
                stopSelf(startId)
            }
        }

        // Don't restart if killed
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        Log.d(TAG, "Action detection service destroyed")
    }

    private suspend fun processUnprocessedActions() {
        try {
            Log.d(TAG, "Starting to process unprocessed action items")

            // Get all unprocessed action items
            val unprocessedActions = actionDetectionService.processUnprocessedActions()
            Log.d(TAG, "Found ${unprocessedActions.size} unprocessed action items")

            if (unprocessedActions.isEmpty()) {
                Log.d(TAG, "No unprocessed action items found")
                return
            }

            var processedCount = 0
            var skippedCount = 0

            // Process each action item
            unprocessedActions.forEach { actionItem ->
                try {
                    if (shouldCreateTask(actionItem)) {
                        val task = createTaskFromActionItem(actionItem)
                        val result = taskRepository.createTask(task)

                        if (result.isSuccess) {
                            val taskId = result.getOrNull()
                            Log.d(TAG, "Created task '$taskId' from action item: ${actionItem.text}")

                            // Mark action item as processed
                            actionDetectionService.markActionAsProcessed(actionItem.id, taskId)
                            processedCount++

                            // Send notification about new task
                            notifyTaskCreated(task, actionItem)
                        } else {
                            Log.e(TAG, "Failed to create task from action item ${actionItem.id}: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.d(TAG, "Skipping action item ${actionItem.id}: ${actionItem.text} (confidence: ${actionItem.confidence})")
                        skippedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception processing action item ${actionItem.id}", e)
                }
            }

            Log.d(TAG, "Action processing completed. Processed: $processedCount, Skipped: $skippedCount")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process unprocessed action items", e)
        }
    }

    private fun shouldCreateTask(actionItem: ActionItem): Boolean {
        // Only create tasks for high-confidence action items of certain types
        return when {
            actionItem.confidence < 0.6f -> {
                Log.d(TAG, "Skipping low confidence action: ${actionItem.confidence}")
                false
            }
            actionItem.type !in listOf(ActionType.TASK, ActionType.REMINDER, ActionType.DEADLINE) -> {
                Log.d(TAG, "Skipping non-task action type: ${actionItem.type}")
                false
            }
            actionItem.text.length < 10 -> {
                Log.d(TAG, "Skipping short action text: '${actionItem.text}'")
                false
            }
            else -> true
        }
    }

    private fun createTaskFromActionItem(actionItem: ActionItem): Task {
        // Determine priority based on action type and confidence
        val priority = when {
            actionItem.type == ActionType.DEADLINE -> TaskPriority.URGENT
            actionItem.confidence > 0.8f -> TaskPriority.HIGH
            actionItem.confidence > 0.6f -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        }

        // Generate task title and description
        val title = generateTaskTitle(actionItem.text, actionItem.type)
        val description = generateTaskDescription(actionItem)

        return Task(
            id = "", // Will be generated by repository
            chatRoomId = actionItem.chatRoomId,
            title = title,
            description = description,
            status = TaskStatus.TODO,
            priority = priority,
            createdByName = "Smart Assistant", // AI-generated task
            sourceMessageId = actionItem.messageId,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun generateTaskTitle(text: String, type: ActionType): String {
        // Clean up the text for use as a title
        val cleanText = text.trim()
            .replace("\\s+".toRegex(), " ")
            .replace("^(need to|should|must|have to|remember to|don't forget to)\\s+".toRegex(RegexOption.IGNORE_CASE), "")

        return when (type) {
            ActionType.TASK -> cleanText.replaceFirstChar { it.uppercase() }
            ActionType.REMINDER -> "Reminder: ${cleanText.replaceFirstChar { it.lowercase() }}"
            ActionType.DEADLINE -> "Deadline: ${cleanText.replaceFirstChar { it.lowercase() }}"
            ActionType.MEETING -> "Meeting: ${cleanText.replaceFirstChar { it.lowercase() }}"
            ActionType.FOLLOW_UP -> "Follow up: ${cleanText.replaceFirstChar { it.lowercase() }}"
        }.take(100) // Limit title length
    }

    private fun generateTaskDescription(actionItem: ActionItem): String {
        return buildString {
            appendLine("Auto-generated from conversation")
            appendLine()
            appendLine("Original text: \"${actionItem.extractedText}\"")
            appendLine("Detected action: ${actionItem.text}")
            appendLine("Type: ${actionItem.type}")
            appendLine("Confidence: ${(actionItem.confidence * 100).toInt()}%")
            if (actionItem.messageId != null) {
                appendLine("Source: Message")
            }
            if (actionItem.voiceMessageId != null) {
                appendLine("Source: Voice message")
            }
        }
    }

    private suspend fun notifyTaskCreated(task: Task, actionItem: ActionItem) {
        Log.d(TAG, "Task created from AI detection: ${task.title}")

        // TODO: You could send FCM notification here
        // val fcmData = mapOf(
        //     "type" to "task_created",
        //     "taskId" to task.id,
        //     "taskTitle" to task.title,
        //     "chatRoomId" to task.chatRoomId,
        //     "source" to "ai_detection"
        // )
        // sendFCMNotification(fcmData)
    }
}

/**
 * Extension function to start action detection service easily from anywhere
 */
fun Context.startActionDetectionService() {
    ActionDetectionWorkerService.startService(this)
}