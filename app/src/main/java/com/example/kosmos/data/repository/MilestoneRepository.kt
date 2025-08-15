package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.MilestoneDao
import com.example.kosmos.core.models.Milestone
import com.example.kosmos.core.models.MilestoneStatus
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.data.datasource.SupabaseMilestoneDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Milestone Repository
 *
 * Offline-first CRUD for milestones:
 * 1. Write to Room immediately (guaranteed success, works offline)
 * 2. Sync to Supabase best-effort; on failure, queue for retry
 */
@Singleton
class MilestoneRepository @Inject constructor(
    private val milestoneDao: MilestoneDao,
    private val supabaseMilestoneDataSource: SupabaseMilestoneDataSource,
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
) {
    companion object {
        private const val TAG = "MilestoneRepository"
    }

    // ========================================================================
    // READ (reactive)
    // ========================================================================

    fun getMilestonesForProjectFlow(projectId: String): Flow<List<Milestone>> =
        milestoneDao.getMilestonesForProjectFlow(projectId)

    fun getActiveMilestonesFlow(projectId: String): Flow<List<Milestone>> =
        milestoneDao.getActiveMilestonesFlow(projectId)

    fun getMilestoneFlow(milestoneId: String): Flow<Milestone?> =
        milestoneDao.getMilestoneFlow(milestoneId)

    // ========================================================================
    // CREATE
    // ========================================================================

    suspend fun createMilestone(milestone: Milestone): Result<Milestone> {
        return try {
            milestoneDao.insertMilestone(milestone)
            Log.d(TAG, "✅ Milestone saved locally: ${milestone.id}")

            try {
                val result = supabaseMilestoneDataSource.insertMilestone(milestone)
                if (result.isFailure) {
                    Log.w(TAG, "⚠️ Failed to sync milestone to Supabase (will retry)", result.exceptionOrNull())
                    SyncQueueHelper.queueMilestone(syncQueueDao, milestone, SyncOperation.CREATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Error syncing milestone (offline mode?)", e)
                SyncQueueHelper.queueMilestone(syncQueueDao, milestone, SyncOperation.CREATE)
            }

            Result.success(milestone)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to create milestone", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // UPDATE
    // ========================================================================

    suspend fun updateMilestone(milestone: Milestone): Result<Milestone> {
        return try {
            milestoneDao.updateMilestone(milestone)
            Log.d(TAG, "✅ Milestone updated locally: ${milestone.id}")

            try {
                val result = supabaseMilestoneDataSource.updateMilestone(milestone)
                if (result.isFailure) {
                    SyncQueueHelper.queueMilestone(syncQueueDao, milestone, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SyncQueueHelper.queueMilestone(syncQueueDao, milestone, SyncOperation.UPDATE)
            }

            Result.success(milestone)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to update milestone", e)
            Result.failure(e)
        }
    }

    suspend fun updateMilestoneStatus(milestoneId: String, status: MilestoneStatus): Result<Unit> {
        return try {
            milestoneDao.updateMilestoneStatus(milestoneId, status)

            val milestone = milestoneDao.getMilestoneById(milestoneId)
            if (milestone != null) {
                try {
                    supabaseMilestoneDataSource.updateMilestone(milestone.copy(status = status))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    SyncQueueHelper.queueMilestone(syncQueueDao, milestone.copy(status = status), SyncOperation.UPDATE)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to update milestone status", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // DELETE
    // ========================================================================

    suspend fun deleteMilestone(milestoneId: String): Result<Unit> {
        return try {
            milestoneDao.deleteMilestoneById(milestoneId)
            Log.d(TAG, "✅ Milestone deleted locally: $milestoneId")

            try {
                val result = supabaseMilestoneDataSource.deleteMilestone(milestoneId)
                if (result.isFailure) {
                    Log.w(TAG, "⚠️ Failed to delete milestone from Supabase", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Error deleting milestone from Supabase (offline mode?)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete milestone", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // SYNC (initial load from Supabase → Room)
    // ========================================================================

    suspend fun syncMilestonesForProject(projectId: String): Result<Unit> {
        return try {
            val result = supabaseMilestoneDataSource.getMilestonesForProject(projectId)
            if (result.isSuccess) {
                val milestones = result.getOrThrow()
                milestoneDao.insertMilestones(milestones)
                Log.d(TAG, "✅ Synced ${milestones.size} milestones for project $projectId")
            } else {
                Log.w(TAG, "⚠️ Failed to sync milestones from Supabase", result.exceptionOrNull())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Error syncing milestones", e)
            Result.failure(e)
        }
    }
}
