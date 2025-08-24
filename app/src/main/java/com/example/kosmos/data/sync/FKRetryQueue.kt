package com.example.kosmos.data.sync

import android.util.Log
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.Message
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * FK Retry Queue - Handles foreign key constraint violations during sync
 *
 * When inserting data fails due to missing FK reference (e.g., Task referencing
 * non-existent User), this queue retries the insert after a delay.
 *
 * Common FK violations:
 * - tasks.assignee_id → users.id (user not synced yet)
 * - messages.sender_id → users.id (user not synced yet)
 * - messages.chat_room_id → chat_rooms.id (room not synced yet)
 */
@Singleton
class FKRetryQueue @Inject constructor(
    private val database: KosmosDatabase
) {
    private val TAG = "FKRetryQueue"

    private val taskRetryQueue = mutableListOf<Task>()
    private val messageRetryQueue = mutableListOf<Message>()

    /**
     * Add task to retry queue (FK violation during insert)
     */
    fun queueTaskRetry(task: Task) {
        synchronized(taskRetryQueue) {
            if (!taskRetryQueue.any { it.id == task.id }) {
                taskRetryQueue.add(task)
                Log.d(TAG, "Queued task ${task.id} for FK retry (assignee: ${task.assignedToId})")
            }
        }
    }

    /**
     * Add message to retry queue (FK violation during insert)
     */
    fun queueMessageRetry(message: Message) {
        synchronized(messageRetryQueue) {
            if (!messageRetryQueue.any { it.id == message.id }) {
                messageRetryQueue.add(message)
                Log.d(TAG, "Queued message ${message.id} for FK retry (sender: ${message.senderId})")
            }
        }
    }

    /**
     * Process retry queue after users sync completes
     * Called by InitialSyncManager after Step 1 (users sync)
     */
    suspend fun processRetryQueue() {
        val taskDao = database.taskDao()
        val messageDao = database.messageDao()

        // Copy queues to avoid holding locks during suspend calls
        val tasksToRetry = synchronized(taskRetryQueue) {
            taskRetryQueue.toList()
        }
        val messagesToRetry = synchronized(messageRetryQueue) {
            messageRetryQueue.toList()
        }

        Log.d(TAG, "Processing retry queue (${tasksToRetry.size} tasks, ${messagesToRetry.size} messages)")

        var tasksRetried = 0
        var tasksFailed = 0
        var messagesRetried = 0
        var messagesFailed = 0

        // Retry tasks
        for (task in tasksToRetry) {
            try {
                taskDao.insertTask(task)
                tasksRetried++
                synchronized(taskRetryQueue) {
                    taskRetryQueue.removeAll { it.id == task.id }
                }
                Log.d(TAG, "✅ Retry succeeded for task ${task.id}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                    tasksFailed++
                    Log.w(TAG, "❌ Retry failed for task ${task.id} - FK still missing")
                    // Keep in queue for next retry
                } else {
                    synchronized(taskRetryQueue) {
                        taskRetryQueue.removeAll { it.id == task.id }
                    }
                    throw e
                }
            }
        }

        // Retry messages
        for (message in messagesToRetry) {
            try {
                messageDao.insertMessage(message)
                messagesRetried++
                synchronized(messageRetryQueue) {
                    messageRetryQueue.removeAll { it.id == message.id }
                }
                Log.d(TAG, "✅ Retry succeeded for message ${message.id}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                    messagesFailed++
                    Log.w(TAG, "❌ Retry failed for message ${message.id} - FK still missing")
                    // Keep in queue for next retry
                } else {
                    synchronized(messageRetryQueue) {
                        messageRetryQueue.removeAll { it.id == message.id }
                    }
                    throw e
                }
            }
        }

        Log.d(TAG, "Retry complete: Tasks ($tasksRetried succeeded, $tasksFailed still queued), Messages ($messagesRetried succeeded, $messagesFailed still queued)")
    }

    /**
     * Clear retry queue (on logout or database reset)
     */
    fun clear() {
        synchronized(taskRetryQueue) { taskRetryQueue.clear() }
        synchronized(messageRetryQueue) { messageRetryQueue.clear() }
        Log.d(TAG, "Retry queue cleared")
    }
}
