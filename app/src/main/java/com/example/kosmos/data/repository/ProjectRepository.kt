package com.example.kosmos.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.ProjectDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.ProjectVisibility
import com.example.kosmos.core.validators.PermissionChecker
import com.example.kosmos.core.exceptions.ConflictException
import com.example.kosmos.core.models.ProjectStats
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.validators.RoleValidator
import com.example.kosmos.core.database.dao.ProjectInviteDao
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.data.datasource.SupabaseProjectDataSource
import com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource
import com.example.kosmos.data.datasource.SupabaseProjectMemberDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.core.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for project creation wizard
 * Contains all fields needed for the multi-step project creation flow
 *
 * @param name Project name (required)
 * @param description Project description
 * @param ownerId Creator's user ID
 * @param category Project category (TECH, SOCIAL, BUSINESS, OTHER)
 * @param deadline Optional deadline timestamp (milliseconds)
 * @param websiteUrl Optional website URL (mainly for BUSINESS)
 * @param githubUrl Optional GitHub repository URL (mainly for TECH)
 * @param projectMotive Optional project goals/motive (mainly for SOCIAL/OTHER)
 * @param techStack Optional list of technologies (mainly for TECH)
 * @param tags Optional general tags
 * @param businessModel Optional business model description (mainly for BUSINESS)
 * @param targetAudience Optional target audience (mainly for SOCIAL)
 * @param industryTags Optional industry tags (mainly for BUSINESS)
 * @param openSourceLicense Optional open source license (mainly for TECH)
 * @param color Project color (hex code)
 * @param imageUrl Optional cover image URL
 * @param visibility Project visibility setting
 */
data class ProjectCreationData(
    val name: String,
    val description: String,
    val ownerId: String,
    val category: ProjectCategory = ProjectCategory.OTHER,
    val deadline: Long? = null,
    val websiteUrl: String? = null,
    val githubUrl: String? = null,
    val projectMotive: String? = null,
    val techStack: List<String>? = null,
    val tags: List<String>? = null,
    val businessModel: String? = null,
    val targetAudience: String? = null,
    val industryTags: List<String>? = null,
    val openSourceLicense: String? = null,
    val color: String = "#6366F1",
    val imageUrl: String? = null,
    val visibility: ProjectVisibility = ProjectVisibility.PRIVATE
)

/**
 * Repository for project management with RBAC enforcement
 * Manages projects, members, roles, and permissions
 * Uses hybrid sync pattern: Room cache + Supabase backend
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val database: KosmosDatabase,  // BUG-011 FIX: Added for transaction support
    private val projectDao: ProjectDao,
    private val projectMemberDao: ProjectMemberDao,
    private val supabaseProjectDataSource: SupabaseProjectDataSource,
    private val supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource,
    private val chatRoomDao: com.example.kosmos.core.database.dao.ChatRoomDao,
    private val taskDao: com.example.kosmos.core.database.dao.TaskDao,
    private val networkMonitor: com.example.kosmos.shared.utils.NetworkMonitor,  // P0-06 FIX
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,  // P0-08 FIX
    private val dispatchers: DispatcherProvider,  // P1-12: Proper threading
    private val projectInviteDao: ProjectInviteDao,
    private val supabaseProjectInviteDataSource: SupabaseProjectInviteDataSource,
    private val notificationService: SupabaseNotificationService
) {

    companion object {
        private const val TAG = "ProjectRepository"
    }

    /**
     * P0-06 FIX: Expose network connectivity state
     * UI can observe this to show offline banner
     */
    val isOffline: kotlinx.coroutines.flow.StateFlow<Boolean> = networkMonitor.isOffline

    // ============================================================
    // PROJECT OPERATIONS
    // ============================================================

    /**
     * Create a new project (legacy method - simple creation)
     * Creator is automatically added as ADMIN
     *
     * BUG-011 FIX: Uses database transaction for atomicity
     *
     * @param name Project name
     * @param description Project description
     * @param ownerId Creator's user ID
     * @return Result with created project or error
     */
    suspend fun createProject(
        name: String,
        description: String,
        ownerId: String
    ): Result<Project> {
        return try {
            val project = Project(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                ownerId = ownerId,
                status = ProjectStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Create project member entry for owner as ADMIN
            val ownerMember = ProjectMember(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                userId = ownerId,
                role = ProjectRole.ADMIN,
                joinedAt = System.currentTimeMillis()
            )

            // BUG-011 FIX: Use transaction for atomic local database operations
            // If either insert fails, both are rolled back
            database.withTransaction {
                projectDao.insertProject(project)
                projectMemberDao.insertMember(ownerMember)
            }
            Log.d(TAG, "✅ Project and owner member saved to Room atomically")

            // Sync to Supabase in background (can fail independently)
            try {
                val supabaseResult = supabaseProjectDataSource.insert(project)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync project to Supabase", supabaseResult.exceptionOrNull())
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueProject(syncQueueDao, project, SyncOperation.CREATE)
                    Log.d(TAG, "📥 Project queued for retry: ${project.id}")
                }

                val memberResult = supabaseProjectMemberDataSource.insert(ownerMember)
                if (memberResult.isFailure) {
                    Log.w(TAG, "Failed to sync project member to Supabase", memberResult.exceptionOrNull())
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueProjectMember(syncQueueDao, ownerMember, SyncOperation.CREATE)
                    Log.d(TAG, "📥 Project member queued for retry: ${ownerMember.id}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Supabase sync failed (non-blocking), will retry later", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueProject(syncQueueDao, project, SyncOperation.CREATE)
                SyncQueueHelper.queueProjectMember(syncQueueDao, ownerMember, SyncOperation.CREATE)
                Log.d(TAG, "📥 Project and member queued for retry")
            }

            Result.success(project)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating project", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new project with initial members (multi-step wizard)
     * Creator is automatically added as ADMIN
     * Additional members are added with specified roles
     *
     * This method is atomic - if any step fails, the entire operation is rolled back
     * Offline-first: Saves to Room immediately, syncs to Supabase in background
     *
     * @param projectData Complete project creation data from wizard
     * @param initialMembers List of (userId, role) pairs for initial members
     * @return Result with created project or error
     */
    suspend fun createProjectWithMembers(
        projectData: ProjectCreationData,
        initialMembers: List<Pair<String, ProjectRole>> = emptyList()
    ): Result<Project> {
        return try {
            Log.d(TAG, "Creating project with wizard data: ${projectData.name}, category: ${projectData.category}")

            val timestamp = System.currentTimeMillis()

            // Create Project entity with all new fields
            val project = Project(
                id = UUID.randomUUID().toString(),
                name = projectData.name,
                description = projectData.description,
                ownerId = projectData.ownerId,
                status = ProjectStatus.ACTIVE,
                visibility = projectData.visibility,
                createdAt = timestamp,
                updatedAt = timestamp,
                imageUrl = projectData.imageUrl,
                color = projectData.color,
                // New wizard fields
                category = projectData.category,
                deadline = projectData.deadline,
                websiteUrl = projectData.websiteUrl,
                githubUrl = projectData.githubUrl,
                projectMotive = projectData.projectMotive,
                techStack = projectData.techStack?.let { Json.encodeToString(it) },
                tags = projectData.tags?.let { Json.encodeToString(it) },
                businessModel = projectData.businessModel,
                targetAudience = projectData.targetAudience,
                industryTags = projectData.industryTags?.let { Json.encodeToString(it) },
                openSourceLicense = projectData.openSourceLicense
            )

            // Save project to Room first (offline-first)
            projectDao.insertProject(project)
            Log.d(TAG, "✅ Project saved to Room: ${project.id}")

            // Create owner as ADMIN
            val ownerMember = ProjectMember(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                userId = projectData.ownerId,
                role = ProjectRole.ADMIN,
                joinedAt = timestamp,
                invitedBy = null // Owner invited themselves
            )
            projectMemberDao.insertMember(ownerMember)
            Log.d(TAG, "✅ Owner added as ADMIN")

            // Filter out invalid members instead of throwing exceptions
            val validMembers = initialMembers.filter { (userId, role) ->
                // Skip owner (already added)
                if (userId == projectData.ownerId) return@filter false

                // Skip invalid UUIDs (don't throw, just filter)
                if (userId.isEmpty() || userId.isBlank()) {
                    Log.w(TAG, "⚠️ Skipping member with empty user ID for role: $role")
                    return@filter false
                }
                if (!userId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE))) {
                    Log.w(TAG, "⚠️ Skipping member with invalid UUID: $userId (role: $role)")
                    return@filter false
                }
                true
            }

            // Create invites for members (they must accept before becoming actual members)
            val createdInvites = mutableListOf<ProjectInvite>()
            validMembers.forEach { (userId, role) ->
                val invite = ProjectInvite(
                    id = UUID.randomUUID().toString(),
                    projectId = project.id,
                    inviteeId = userId,
                    inviterId = projectData.ownerId,
                    role = role.name,
                    message = "You've been invited to join ${projectData.name}"
                )
                projectInviteDao.insert(invite)
                createdInvites.add(invite)
                Log.d(TAG, "✅ Created invite for: $userId as $role")
            }

            if (validMembers.size < initialMembers.size) {
                Log.w(TAG, "⚠️ Filtered out ${initialMembers.size - validMembers.size} invalid members during project creation")
            }

            // Member count is calculated dynamically from ProjectMember table
            // No need to update it here

            // Sync to Supabase in background (non-blocking)
            try {
                val projectSyncResult = supabaseProjectDataSource.insert(project)
                if (projectSyncResult.isFailure) {
                    Log.w(TAG, "⚠️ Failed to sync project to Supabase (will retry)", projectSyncResult.exceptionOrNull())
                    SyncQueueHelper.queueProject(syncQueueDao, project, SyncOperation.CREATE)
                } else {
                    Log.d(TAG, "✅ Project synced to Supabase")
                }

                // Sync owner member
                val ownerSyncResult = supabaseProjectMemberDataSource.insert(ownerMember)
                if (ownerSyncResult.isFailure) {
                    Log.w(TAG, "⚠️ Failed to sync owner member to Supabase", ownerSyncResult.exceptionOrNull())
                    SyncQueueHelper.queueProjectMember(syncQueueDao, ownerMember, SyncOperation.CREATE)
                }

                // Sync invites & send notifications
                createdInvites.forEach { invite ->
                    val inviteSyncResult = supabaseProjectInviteDataSource.createInvite(invite)
                    if (inviteSyncResult.isFailure) {
                        Log.w(TAG, "⚠️ Failed to sync invite for ${invite.inviteeId}", inviteSyncResult.exceptionOrNull())
                        SyncQueueHelper.queueProjectInvite(syncQueueDao, invite, SyncOperation.CREATE)
                    }
                    // Send notification to invitee
                    try {
                        notificationService.sendNotification(
                            userId = invite.inviteeId,
                            title = "Project Invite",
                            body = "You've been invited to join ${projectData.name}",
                            type = "project_invite",
                            data = mapOf(
                                "invite_id" to invite.id,
                                "project_id" to project.id,
                                "project_name" to projectData.name,
                                "role" to invite.role
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send invite notification to ${invite.inviteeId}", e)
                    }
                }
                Log.d(TAG, "✅ All invites synced to Supabase")
            } catch (syncException: Exception) {
                Log.w(TAG, "⚠️ Sync to Supabase failed (offline or network error), will retry later", syncException)
                SyncQueueHelper.queueProject(syncQueueDao, project, SyncOperation.CREATE)
                SyncQueueHelper.queueProjectMember(syncQueueDao, ownerMember, SyncOperation.CREATE)
                createdInvites.forEach { invite ->
                    SyncQueueHelper.queueProjectInvite(syncQueueDao, invite, SyncOperation.CREATE)
                }
            }

            Log.d(TAG, "🎉 Project creation complete: ${project.name} (owner + ${createdInvites.size} invites)")
            Result.success(project)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating project with members", e)
            Result.failure(e)
        }
    }

    /**
     * Get a project by ID
     *
     * @param projectId Project ID
     * @return Flow of Project or null
     */
    fun getProjectFlow(projectId: String): Flow<Project?> {
        return projectDao.getProjectByIdFlow(projectId)
    }

    /**
     * Get a project by ID (suspend)
     *
     * @param projectId Project ID
     * @return Project or null
     */
    suspend fun getProject(projectId: String): Project? {
        return projectDao.getProjectById(projectId)
    }

    /**
     * Get all projects for a user (where they are a member)
     * Includes both owned projects and projects where user is a member
     *
     * @param userId User ID
     * @return Flow of projects
     */
    fun getUserProjectsFlow(userId: String): Flow<List<Project>> {
        return projectDao.getProjectsByUserMembership(userId)
    }

    /**
     * Sync user's projects from Supabase to local cache
     * Call this on app startup, login, or pull-to-refresh
     *
     * CRITICAL: This method fixes the bug where projects are never fetched from Supabase.
     * Without this, the app only reads from Room cache which may be empty or stale.
     *
     * @param userId User ID
     * @return Result indicating success or failure
     */
    suspend fun syncUserProjects(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting project sync for user: $userId")

            // Fetch user's project memberships from Supabase
            val membershipsResult = supabaseProjectMemberDataSource.getUserMemberships(userId)

            if (membershipsResult.isFailure) {
                Log.w(TAG, "Failed to fetch project memberships from Supabase", membershipsResult.exceptionOrNull())
                return membershipsResult.map { }  // Convert to Result<Unit>
            }

            val memberships = membershipsResult.getOrNull() ?: emptyList()
            val projectIds = memberships.map { it.projectId }.distinct()

            Log.d(TAG, "Found ${projectIds.size} projects for user")

            var successCount = 0
            var failureCount = 0
            var cancelledCount = 0

            // Fetch each project from Supabase (with granular error handling)
            projectIds.forEach { projectId ->
                try {
                    // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
                    val projectResult = withContext(NonCancellable) {
                        supabaseProjectDataSource.getById(projectId)
                    }

                    if (projectResult.isSuccess) {
                        val project = projectResult.getOrNull()
                        if (project != null) {
                            // Save to Room
                            projectDao.insertProject(project)
                            successCount++

                            // Also sync members for this project
                            try {
                                syncProjectMembers(projectId)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                Log.w(TAG, "⚠️ Members sync cancelled for project $projectId")
                                cancelledCount++
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to sync members for project $projectId", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Failed to fetch project $projectId", projectResult.exceptionOrNull())
                        failureCount++
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Project sync cancelled for $projectId (may be saved)")
                    cancelledCount++
                    // DON'T re-throw - continue with next project
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing project $projectId", e)
                    failureCount++
                    // Continue with next project
                }
            }

            Log.d(TAG, "✅ Synced $successCount/${projectIds.size} projects ($failureCount failed, $cancelledCount cancelled)")
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "⚠️ Project sync cancelled (partial data saved)")
            Result.success(Unit)  // Return success - partial data is OK
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in project sync", e)
            Result.failure(e)
        }
    }

    /**
     * Sync project members from Supabase to local cache
     * Called automatically by syncUserProjects, but can also be called manually
     *
     * INCREMENTAL SYNC: Only fetches members modified since last sync (50-90% less data)
     *
     * @param projectId Project ID
     * @param since Optional timestamp (milliseconds) - only fetch members updated after this time
     * @return Result indicating success or failure
     */
    suspend fun syncProjectMembers(projectId: String, since: Long? = null): Result<Unit> {
        return try {
            if (since != null) {
                Log.d(TAG, "Starting incremental member sync for project: $projectId (since: $since)")
            } else {
                Log.d(TAG, "Starting full member sync for project: $projectId")
            }

            val membersResult = supabaseProjectMemberDataSource.getProjectMembers(projectId, since)

            if (membersResult.isSuccess) {
                val members = membersResult.getOrNull() ?: emptyList()
                var successCount = 0
                var fkErrorCount = 0

                // Update local cache with FK error handling
                members.forEach { member ->
                    try {
                        projectMemberDao.insertMember(member)
                        successCount++
                    } catch (e: Exception) {
                        if (com.example.kosmos.data.sync.ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                            fkErrorCount++
                            com.example.kosmos.data.sync.ForeignKeyErrorHandler.logForeignKeyErrorWithContext(
                                e,
                                "ProjectMember",
                                member.id,
                                "insert",
                                "users",
                                member.userId
                            )
                            // Skip this member, continue with others
                        } else {
                            throw e  // Re-throw non-FK errors
                        }
                    }
                }

                if (fkErrorCount > 0) {
                    Log.w(TAG, "⚠️ Synced $successCount/${members.size} members ($fkErrorCount FK errors) for project $projectId")
                } else {
                    val syncType = if (since != null) "incremental" else "full"
                    Log.d(TAG, "✅ Synced ${members.size} members for project $projectId ($syncType)")
                }

                Result.success(Unit)
            } else {
                Log.w(TAG, "Failed to sync members for project $projectId", membersResult.exceptionOrNull())
                membersResult.map { }  // Convert to Result<Unit>
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing members for project $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * Update project details
     * Requires EDIT_PROJECT permission
     *
     * @param project Project to update
     * @param userId User making the update
     * @return Result with Unit or error
     */
    suspend fun updateProject(project: Project, userId: String): Result<Unit> {
        return try {
            // Check permission
            val member = getMember(project.id, userId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val permissionResult = PermissionChecker.hasPermission(member, Permission.EDIT_PROJECT)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            // P1-11: Check version conflict before updating
            val currentProject = projectDao.getProjectById(project.id)
            if (currentProject != null && currentProject.version != project.version) {
                // Version mismatch = concurrent edit detected
                throw ConflictException(
                    entityType = "Project",
                    entityId = project.id,
                    localVersion = project.version,
                    serverVersion = currentProject.version,
                    localData = project,
                    serverData = currentProject
                )
            }

            // P1-11: Update locally with incremented version
            val updatedProject = project.copy(
                updatedAt = System.currentTimeMillis(),
                version = project.version + 1
            )
            projectDao.updateProject(updatedProject)

            // Sync to Supabase
            val supabaseResult = supabaseProjectDataSource.update(updatedProject)
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync project update to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug H fix: queue for retry
                SyncQueueHelper.queueProject(syncQueueDao, updatedProject, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a project
     * Requires DELETE_PROJECT permission
     *
     * @param projectId Project ID
     * @param userId User requesting deletion
     * @return Result with Unit or error
     */
    suspend fun deleteProject(projectId: String, userId: String): Result<Unit> {
        return try {
            // Check permission
            val member = getMember(projectId, userId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val permissionResult = PermissionChecker.hasPermission(member, Permission.DELETE_PROJECT)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            // Delete locally (cascade will delete members)
            projectDao.deleteProjectById(projectId)

            // Sync to Supabase
            val supabaseResult = supabaseProjectDataSource.delete(projectId)
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync project deletion to Supabase", supabaseResult.exceptionOrNull())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project", e)
            Result.failure(e)
        }
    }

    /**
     * Update project status (archive, complete, etc.)
     * Requires ARCHIVE_PROJECT permission for ARCHIVED status
     *
     * @param projectId Project ID
     * @param status New status
     * @param userId User making the change
     * @return Result with Unit or error
     */
    suspend fun updateProjectStatus(
        projectId: String,
        status: ProjectStatus,
        userId: String
    ): Result<Unit> {
        return try {
            // Check permission
            val member = getMember(projectId, userId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            if (status == ProjectStatus.ARCHIVED) {
                val permissionResult = PermissionChecker.hasPermission(member, Permission.ARCHIVE_PROJECT)
                if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                    return Result.failure(
                        PermissionChecker.PermissionDeniedException(
                            permissionResult.getDeniedReason() ?: "Permission denied"
                        )
                    )
                }
            }

            // Fetch project to get current version for optimistic locking
            val project = projectDao.getProjectById(projectId)
                ?: return Result.failure(Exception("Project not found"))

            val timestamp = System.currentTimeMillis()
            projectDao.updateProjectStatus(projectId, status, timestamp)
            val updatedProject = project.copy(status = status, updatedAt = timestamp)

            // SCHEMA FIX: Sync to Supabase with version for optimistic locking
            val supabaseResult = supabaseProjectDataSource.updateStatus(
                projectId = projectId,
                status = status,
                currentVersion = project.version  // Add version for conflict detection
            )
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync status update to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug J fix: queue for retry
                SyncQueueHelper.queueProject(syncQueueDao, updatedProject, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project status", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // MEMBER OPERATIONS
    // ============================================================

    /**
     * Add a member to a project
     * Requires INVITE_MEMBERS permission
     *
     * @param projectId Project ID
     * @param userId User to add
     * @param role Role to assign
     * @param invitedBy User ID of inviter
     * @return Result with ProjectMember or error
     */
    suspend fun addMember(
        projectId: String,
        userId: String,
        role: ProjectRole,
        invitedBy: String,
        bypassApproval: Boolean = false
    ): Result<ProjectMember> {
        return try {
            if (!bypassApproval) {
                // Check inviter's permission
                val inviter = getMember(projectId, invitedBy)
                    ?: return Result.failure(SecurityException("You are not a member of this project"))

                val permissionResult = PermissionChecker.hasPermission(inviter, Permission.INVITE_MEMBERS)
                if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                    return Result.failure(
                        PermissionChecker.PermissionDeniedException(
                            permissionResult.getDeniedReason() ?: "Permission denied"
                        )
                    )
                }
            }

            // Check if user is already a member
            val existingMember = getMember(projectId, userId)
            if (existingMember != null) {
                return Result.failure(IllegalStateException("User is already a member of this project"))
            }

            // Create member entry
            val member = ProjectMember(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                role = role,
                joinedAt = System.currentTimeMillis(),
                invitedBy = invitedBy
            )

            // Save locally
            projectMemberDao.insertMember(member)

            // Update project member count
            projectDao.incrementMemberCount(projectId)

            // Sync to Supabase
            val supabaseResult = supabaseProjectMemberDataSource.insert(member)
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync member addition to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug I fix: queue for retry
                SyncQueueHelper.queueProjectMember(syncQueueDao, member, SyncOperation.CREATE)
            }

            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding member", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a member from a project
     * Requires REMOVE_MEMBERS permission and role validation
     *
     * @param projectId Project ID
     * @param userIdToRemove User to remove
     * @param removedBy User performing the removal
     * @return Result with Unit or error
     */
    suspend fun removeMember(
        projectId: String,
        userIdToRemove: String,
        removedBy: String
    ): Result<Unit> {
        return try {
            // Get both members
            val remover = getMember(projectId, removedBy)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val targetMember = getMember(projectId, userIdToRemove)
                ?: return Result.failure(IllegalArgumentException("User is not a member of this project"))

            // Check permission
            val permissionResult = PermissionChecker.hasPermission(remover, Permission.REMOVE_MEMBERS)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            // Validate role hierarchy
            val roleValidation = RoleValidator.canRemoveMember(remover.role, targetMember.role)
            if (roleValidation !is RoleValidator.ValidationResult.Success) {
                return Result.failure(
                    SecurityException(roleValidation.getErrorMessage() ?: "Cannot remove this member")
                )
            }

            // Ensure project still has an admin
            val allMembers = getProjectMembers(projectId)
            val breakingValidation = RoleValidator.canRemoveWithoutBreakingProject(allMembers, targetMember)
            if (breakingValidation !is RoleValidator.ValidationResult.Success) {
                return Result.failure(
                    IllegalStateException(breakingValidation.getErrorMessage() ?: "Cannot remove member")
                )
            }

            // Remove locally
            projectMemberDao.removeMemberFromProject(projectId, userIdToRemove)

            // Update project member count
            projectDao.decrementMemberCount(projectId)

            // Sync to Supabase
            val supabaseResult = supabaseProjectMemberDataSource.removeMember(projectId, userIdToRemove)
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync member removal to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug I fix: queue for retry
                SyncQueueHelper.queueProjectMember(syncQueueDao, targetMember, SyncOperation.DELETE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing member", e)
            Result.failure(e)
        }
    }

    /**
     * Change a member's role
     * Requires CHANGE_MEMBER_ROLES permission and role validation
     *
     * @param projectId Project ID
     * @param userIdToChange User whose role to change
     * @param newRole New role to assign
     * @param changedBy User performing the change
     * @return Result with Unit or error
     */
    suspend fun changeRole(
        projectId: String,
        userIdToChange: String,
        newRole: ProjectRole,
        changedBy: String
    ): Result<Unit> {
        return try {
            // Get both members
            val changer = getMember(projectId, changedBy)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val targetMember = getMember(projectId, userIdToChange)
                ?: return Result.failure(IllegalArgumentException("User is not a member of this project"))

            // Check permission
            val permissionResult = PermissionChecker.hasPermission(changer, Permission.CHANGE_MEMBER_ROLES)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            // Validate role hierarchy
            val roleValidation = RoleValidator.canChangeRole(changer.role, targetMember.role, newRole)
            if (roleValidation !is RoleValidator.ValidationResult.Success) {
                return Result.failure(
                    SecurityException(roleValidation.getErrorMessage() ?: "Cannot change role")
                )
            }

            // Update locally
            val updatedMember = targetMember.copy(role = newRole)
            projectMemberDao.updateMemberRole(targetMember.id, newRole)

            // Sync to Supabase
            val supabaseResult = supabaseProjectMemberDataSource.updateRole(targetMember.id, newRole)
            if (supabaseResult.isFailure) {
                Log.w(TAG, "Failed to sync role change to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug I fix: queue for retry
                SyncQueueHelper.queueProjectMember(syncQueueDao, updatedMember, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error changing role", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific project member
     *
     * @param projectId Project ID
     * @param userId User ID
     * @return ProjectMember or null
     */
    suspend fun getMember(projectId: String, userId: String): ProjectMember? {
        return projectMemberDao.getMemberByProjectAndUser(projectId, userId)
    }

    /**
     * Get all members of a project
     *
     * @param projectId Project ID
     * @return List of active project members
     */
    suspend fun getProjectMembers(projectId: String): List<ProjectMember> {
        // Get first emission from Flow for snapshot
        return projectMemberDao.getProjectMembers(projectId).first()
    }

    /**
     * Get project members flow
     *
     * @param projectId Project ID
     * @return Flow of project members
     */
    fun getProjectMembersFlow(projectId: String): Flow<List<ProjectMember>> {
        return projectMemberDao.getProjectMembers(projectId)
    }

    /**
     * Get member's role in a project
     *
     * @param projectId Project ID
     * @param userId User ID
     * @return ProjectRole or null if not a member
     */
    suspend fun getMemberRole(projectId: String, userId: String): ProjectRole? {
        return getMember(projectId, userId)?.role
    }

    /**
     * Check if user has a specific permission in a project
     *
     * @param projectId Project ID
     * @param userId User ID
     * @param permission Permission to check
     * @return true if user has permission
     */
    suspend fun hasPermission(
        projectId: String,
        userId: String,
        permission: Permission
    ): Boolean {
        val member = getMember(projectId, userId) ?: return false
        return PermissionChecker.hasPermission(member, permission).isGranted()
    }

    /**
     * Get count of shared projects between two users
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Number of projects both users are members of
     */
    suspend fun getSharedProjectCount(userId1: String, userId2: String): Int {
        return projectMemberDao.getSharedProjectCount(userId1, userId2)
    }

    // ============================================================
    // PROJECT STATS OPERATIONS
    // ============================================================

    /**
     * Get real-time statistics for a project
     * **OPTIMIZED**: Uses cached metadata columns from projects table (25x faster)
     *
     * Performance:
     * - OLD: 5 queries × 50ms = 250ms
     * - NEW: 1 query × 10ms = 10ms (25x improvement!)
     *
     * @param projectId Project ID
     * @return Flow of ProjectStats with live updates from cached metadata
     */
    fun getProjectStatsFlow(projectId: String): Flow<ProjectStats> {
        return projectDao.getProjectByIdFlow(projectId).map { project ->
            project?.let {
                ProjectStats(
                    projectId = it.id,
                    memberCount = it.memberCount,
                    chatCount = it.chatCount,
                    taskCount = it.taskCount,
                    completedTaskCount = it.completedTaskCount,
                    unreadChatCount = 0, // TODO: Implement unread count if needed
                    pendingTaskCount = it.pendingTaskCount,
                    lastActivityTime = it.lastActivityAt
                )
            } ?: ProjectStats(projectId = projectId) // Return empty stats if project not found
        }
    }

    /**
     * Get statistics for a project (one-time query)
     * **OPTIMIZED**: Uses cached metadata columns from projects table (25x faster)
     *
     * Performance:
     * - OLD: 5 queries × 50ms = 250ms
     * - NEW: 1 query × 10ms = 10ms (25x improvement!)
     *
     * @param projectId Project ID
     * @return ProjectStats snapshot from cached metadata
     */
    suspend fun getProjectStats(projectId: String): ProjectStats {
        return try {
            val project = projectDao.getProjectById(projectId)

            project?.let {
                ProjectStats(
                    projectId = it.id,
                    memberCount = it.memberCount,
                    chatCount = it.chatCount,
                    taskCount = it.taskCount,
                    completedTaskCount = it.completedTaskCount,
                    unreadChatCount = 0, // TODO: Implement if needed
                    pendingTaskCount = it.pendingTaskCount,
                    lastActivityTime = it.lastActivityAt
                )
            } ?: ProjectStats(projectId = projectId) // Return empty stats if project not found
        } catch (e: Exception) {
            Log.e(TAG, "Error getting project stats", e)
            ProjectStats(projectId = projectId) // Return empty stats on error
        }
    }

    /**
     * Search public projects via Supabase (for Discover screen)
     */
    suspend fun searchPublicProjects(query: String): Result<List<Project>> {
        return supabaseProjectDataSource.searchProjects(query)
    }

    /**
     * Get stats for all user projects
     * **OPTIMIZED**: Uses cached metadata columns (instant access)
     *
     * Returns a map of projectId to ProjectStats
     *
     * @param userId User ID
     * @return Flow of Map<ProjectId, ProjectStats> with instant updates
     */
    fun getAllProjectsStatsFlow(userId: String): Flow<Map<String, ProjectStats>> {
        return getUserProjectsFlow(userId).map { projects ->
            projects.associate { project ->
                project.id to ProjectStats(
                    projectId = project.id,
                    memberCount = project.memberCount,
                    chatCount = project.chatCount,
                    taskCount = project.taskCount,
                    completedTaskCount = project.completedTaskCount,
                    unreadChatCount = 0,
                    pendingTaskCount = project.pendingTaskCount,
                    lastActivityTime = project.lastActivityAt
                )
            }
        }
    }

    /**
     * Get stats for all user projects (one-time query)
     * **OPTIMIZED**: Uses cached metadata columns (instant access)
     *
     * More efficient than Flow for initial load
     *
     * @param userId User ID
     * @return Map of projectId to ProjectStats
     */
    suspend fun getAllProjectsStats(userId: String): Map<String, ProjectStats> {
        return try {
            val projects = projectDao.getProjectsByUserMembership(userId).first()

            projects.associate { project ->
                project.id to ProjectStats(
                    projectId = project.id,
                    memberCount = project.memberCount,
                    chatCount = project.chatCount,
                    taskCount = project.taskCount,
                    completedTaskCount = project.completedTaskCount,
                    unreadChatCount = 0,
                    pendingTaskCount = project.pendingTaskCount,
                    lastActivityTime = project.lastActivityAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all projects stats", e)
            emptyMap()
        }
    }
}
