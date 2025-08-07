package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.FieldChange
import com.example.kosmos.core.models.TaskActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Task Activity Data Source
 *
 * Handles remote sync of task activity records to Supabase.
 * Provides queries for activity timelines, logs, and pagination.
 *
 * Pattern: Offline-first
 * - Room DAO handles local storage (primary)
 * - This data source syncs to Supabase (secondary)
 */
@Singleton
class SupabaseTaskActivityDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseTaskActivityDS"
        private const val TABLE_NAME = "task_activity"
        private const val DEFAULT_LIMIT = 100
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ============================================================================
    // INSERT OPERATIONS
    // ============================================================================

    /**
     * Insert activity record into Supabase
     *
     * @param activity TaskActivity to insert
     * @return Result with inserted activity or error
     */
    suspend fun insertActivity(activity: TaskActivity): Result<TaskActivity> {
        return try {
            // Convert complex fields to JSONB-compatible format
            val changesJson = if (activity.changes.isNotEmpty()) {
                json.encodeToString(activity.changes)
            } else null

            val metadataJson = if (activity.metadata.isNotEmpty()) {
                json.encodeToString(activity.metadata)
            } else null

            // Build JSON object manually to avoid serialization issues with Map<String, Any>
            val jsonObject = buildJsonObject {
                put("id", activity.id)
                put("task_id", activity.taskId)
                put("project_id", activity.projectId)
                put("actor_id", activity.actorId)
                put("actor_name", activity.actorName)
                activity.actorRole?.let { put("actor_role", it) }
                put("action_type", activity.actionType.name.lowercase())
                put("timestamp", activity.timestamp)
                changesJson?.let { put("changes", it) }
                activity.commitMessage?.let { put("commit_message", it) }
                put("auto_description", activity.autoDescription)
                metadataJson?.let { put("metadata", it) }
            }

            supabase.from(TABLE_NAME).insert(jsonObject)

            Log.d(TAG, "Activity inserted: taskId=${activity.taskId}, action=${activity.actionType}")
            Result.success(activity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting activity: taskId=${activity.taskId}", e)
            Result.failure(e)
        }
    }

    /**
     * Bulk insert activities (for initial sync or import)
     *
     * @param activities List of TaskActivity to insert
     * @return Result with count of successful inserts
     */
    suspend fun insertActivities(activities: List<TaskActivity>): Result<Int> {
        return try {
            val activityMaps = activities.map { activity ->
                mapOf(
                    "id" to activity.id,
                    "task_id" to activity.taskId,
                    "project_id" to activity.projectId,
                    "actor_id" to activity.actorId,
                    "actor_name" to activity.actorName,
                    "actor_role" to activity.actorRole,
                    "action_type" to activity.actionType.name.lowercase(),
                    "timestamp" to activity.timestamp,
                    "changes" to if (activity.changes.isNotEmpty()) json.encodeToString(activity.changes) else null,
                    "commit_message" to activity.commitMessage,
                    "auto_description" to activity.autoDescription,
                    "metadata" to if (activity.metadata.isNotEmpty()) json.encodeToString(activity.metadata) else null
                )
            }

            supabase.from(TABLE_NAME).insert(activityMaps)

            Log.d(TAG, "Bulk insert successful: ${activities.size} activities")
            Result.success(activities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error bulk inserting activities", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // TASK-LEVEL QUERIES
    // ============================================================================

    /**
     * Get all activities for a specific task
     *
     * @param taskId Task ID
     * @param limit Maximum number of activities to fetch
     * @return Result with list of activities or error
     */
    suspend fun getActivitiesForTask(
        taskId: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("task_id", taskId)
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Fetched ${response.size} activities for task: $taskId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activities for task: $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get activities before a specific timestamp (for pagination)
     *
     * @param taskId Task ID
     * @param beforeTimestamp Fetch activities before this timestamp
     * @param limit Maximum number of activities
     * @return Result with list of activities
     */
    suspend fun getActivitiesBeforeTimestamp(
        taskId: String,
        beforeTimestamp: Long,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("task_id", taskId)
                        lt("timestamp", beforeTimestamp)
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Fetched ${response.size} activities before $beforeTimestamp for task: $taskId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated activities for task: $taskId", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // PROJECT-LEVEL QUERIES
    // ============================================================================

    /**
     * Get all activities for a project
     *
     * @param projectId Project ID
     * @param limit Maximum number of activities
     * @return Result with list of activities
     */
    suspend fun getActivitiesForProject(
        projectId: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Fetched ${response.size} activities for project: $projectId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activities for project: $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * Get activities for project with pagination
     *
     * @param projectId Project ID
     * @param offset Offset for pagination
     * @param limit Page size
     * @return Result with list of activities
     */
    suspend fun getActivitiesForProjectPaginated(
        projectId: String,
        offset: Int = 0,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                    // Note: Supabase offset is via range, not a direct offset parameter
                    // Using timestamp-based pagination is more efficient
                }
                .decodeList<TaskActivity>()
                .drop(offset)
                .take(limit)

            Log.d(TAG, "Fetched page ${offset / limit} (${response.size} activities) for project: $projectId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated activities for project: $projectId", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // USER QUERIES
    // ============================================================================

    /**
     * Get activities by a specific user
     *
     * @param userId User ID
     * @param projectId Optional project filter
     * @param limit Maximum number of activities
     * @return Result with list of activities
     */
    suspend fun getActivitiesForUser(
        userId: String,
        projectId: String? = null,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("actor_id", userId)
                        if (projectId != null) {
                            eq("project_id", projectId)
                        }
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Fetched ${response.size} activities for user: $userId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activities for user: $userId", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // FILTERED QUERIES
    // ============================================================================

    /**
     * Get activities by action type
     *
     * @param projectId Project ID
     * @param actionType Action type filter
     * @param limit Maximum number of activities
     * @return Result with list of activities
     */
    suspend fun getActivitiesByActionType(
        projectId: String,
        actionType: ActivityActionType,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("action_type", actionType.name.lowercase())
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Fetched ${response.size} $actionType activities for project: $projectId")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activities by action type: $actionType", e)
            Result.failure(e)
        }
    }

    /**
     * Search activities by commit message
     *
     * @param projectId Project ID
     * @param searchQuery Search query
     * @param limit Maximum number of results
     * @return Result with list of matching activities
     */
    suspend fun searchActivitiesByCommitMessage(
        projectId: String,
        searchQuery: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TaskActivity>> {
        return try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        ilike("commit_message", "%$searchQuery%")
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<TaskActivity>()

            Log.d(TAG, "Search found ${response.size} activities matching: $searchQuery")
            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching activities by commit message: $searchQuery", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // DELETE OPERATIONS
    // ============================================================================

    /**
     * Delete activity by ID
     * Only admins should use this (moderation/cleanup)
     *
     * @param activityId Activity ID to delete
     * @return Result with Unit or error
     */
    suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", activityId)
                    }
                }

            Log.d(TAG, "Activity deleted: $activityId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting activity: $activityId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all activities for a task
     * Used when task is permanently deleted
     *
     * @param taskId Task ID
     * @return Result with Unit or error
     */
    suspend fun deleteActivitiesForTask(taskId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("task_id", taskId)
                    }
                }

            Log.d(TAG, "All activities deleted for task: $taskId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting activities for task: $taskId", e)
            Result.failure(e)
        }
    }
}
