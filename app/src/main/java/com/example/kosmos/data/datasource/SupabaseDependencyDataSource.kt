package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.TaskDependency
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Task Dependency Data Source
 *
 * Handles all Supabase operations for task_dependencies table.
 * Provides sync functionality for offline-first architecture.
 */
@Singleton
class SupabaseDependencyDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseDependencyDataSource"
        private const val TABLE_NAME = "task_dependencies"
    }

    // ========================================================================
    // INSERT OPERATIONS
    // ========================================================================

    /**
     * Insert a new dependency into Supabase
     */
    suspend fun insertDependency(dependency: TaskDependency): Result<TaskDependency> {
        return try {
            Log.d(TAG, "Inserting dependency: ${dependency.id}")

            supabase.from(TABLE_NAME).insert(buildJsonObject {
                put("id", dependency.id)
                put("task_id", dependency.taskId)
                put("depends_on_task_id", dependency.dependsOnTaskId)
                put("dependency_type", dependency.dependencyType.name.lowercase())
                put("created_at", dependency.createdAt)
                put("created_by", dependency.createdBy)
            })

            Log.d(TAG, "Successfully inserted dependency: ${dependency.id}")
            Result.success(dependency)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert dependency", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a dependency from Supabase
     */
    suspend fun deleteDependency(dependencyId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting dependency: $dependencyId")

            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("id", dependencyId)
                }
            }

            Log.d(TAG, "Successfully deleted dependency: $dependencyId")
            Result.success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete dependency", e)
            Result.failure(e)
        }
    }

    /**
     * Delete dependency between two tasks
     */
    suspend fun deleteDependencyBetweenTasks(
        taskId: String,
        dependsOnTaskId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting dependency between $taskId and $dependsOnTaskId")

            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("task_id", taskId)
                    eq("depends_on_task_id", dependsOnTaskId)
                }
            }

            Log.d(TAG, "Successfully deleted dependency between tasks")
            Result.success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete dependency between tasks", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get all dependencies for a specific task
     */
    suspend fun getDependenciesForTask(taskId: String): Result<List<TaskDependency>> {
        return try {
            Log.d(TAG, "Fetching dependencies for task: $taskId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("task_id", taskId)
                }
            }.decodeList<TaskDependency>()

            Log.d(TAG, "Successfully fetched ${response.size} dependencies for task: $taskId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch dependencies for task: $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get all tasks that depend on this task
     */
    suspend fun getDependentTasks(taskId: String): Result<List<TaskDependency>> {
        return try {
            Log.d(TAG, "Fetching dependent tasks for: $taskId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("depends_on_task_id", taskId)
                }
            }.decodeList<TaskDependency>()

            Log.d(TAG, "Successfully fetched ${response.size} dependent tasks")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch dependent tasks", e)
            Result.failure(e)
        }
    }

    /**
     * Get blocking dependencies for a task
     */
    suspend fun getBlockingDependencies(taskId: String): Result<List<TaskDependency>> {
        return try {
            Log.d(TAG, "Fetching blocking dependencies for: $taskId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("task_id", taskId)
                    eq("dependency_type", "blocks")
                }
            }.decodeList<TaskDependency>()

            Log.d(TAG, "Successfully fetched ${response.size} blocking dependencies")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch blocking dependencies", e)
            Result.failure(e)
        }
    }
}
