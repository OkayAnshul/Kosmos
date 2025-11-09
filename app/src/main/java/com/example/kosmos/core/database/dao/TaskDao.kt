package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE chatRoomId = :chatRoomId ORDER BY createdAt DESC")
    fun getTasksForChatRoomFlow(chatRoomId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE chatRoomId = :chatRoomId AND status = :status ORDER BY createdAt DESC")
    fun getTasksByStatusFlow(chatRoomId: String, status: TaskStatus): Flow<List<Task>>

    // Project-level task queries (tasks are independent, not nested in chats)
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProjectFlow(projectId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status = :status ORDER BY createdAt DESC")
    fun getProjectTasksByStatusFlow(projectId: String, status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE assignedToId = :userId AND status != 'DONE' AND status != 'CANCELLED' ORDER BY dueDate ASC")
    fun getMyActiveTasksFlow(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND dueDate < :timestamp AND status != 'DONE' AND status != 'CANCELLED'")
    suspend fun getOverdueTasks(timestamp: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE chatRoomId = :chatRoomId")
    suspend fun deleteTasksForChatRoom(chatRoomId: String)

    // Stats queries for project statistics
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    suspend fun getTaskCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    fun getTaskCountForProjectFlow(projectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND status = 'DONE'")
    suspend fun getCompletedTaskCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND status = 'DONE'")
    fun getCompletedTaskCountForProjectFlow(projectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND status != 'DONE' AND status != 'CANCELLED'")
    suspend fun getPendingTaskCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND status != 'DONE' AND status != 'CANCELLED'")
    fun getPendingTaskCountForProjectFlow(projectId: String): Flow<Int>

    // Cross-project task query for MyTasksScreen
    @Query("SELECT * FROM tasks WHERE assignedToId = :userId ORDER BY dueDate ASC")
    fun getAllTasksByUserFlow(userId: String): Flow<List<Task>>
}