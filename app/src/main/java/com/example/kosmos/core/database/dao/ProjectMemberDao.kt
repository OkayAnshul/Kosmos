package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMemberDao {
    @Query("SELECT * FROM project_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: String): ProjectMember?

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): ProjectMember?

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId")
    fun getMemberByProjectAndUserFlow(projectId: String, userId: String): Flow<ProjectMember?>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND isActive = 1 ORDER BY joinedAt ASC")
    fun getProjectMembers(projectId: String): Flow<List<ProjectMember>>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND role = :role AND isActive = 1")
    fun getMembersByRole(projectId: String, role: ProjectRole): Flow<List<ProjectMember>>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND role = :role AND isActive = 1")
    suspend fun getMembersByRoleSync(projectId: String, role: String): List<ProjectMember>

    @Query("SELECT * FROM project_members WHERE userId = :userId AND isActive = 1 ORDER BY lastActivityAt DESC")
    fun getUserMemberships(userId: String): Flow<List<ProjectMember>>

    @Query("SELECT COUNT(*) FROM project_members WHERE projectId = :projectId AND isActive = 1")
    suspend fun getActiveMemberCount(projectId: String): Int

    @Query("SELECT COUNT(*) FROM project_members WHERE projectId = :projectId AND isActive = 1")
    fun getActiveMemberCountFlow(projectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM project_members WHERE projectId = :projectId AND role = :role AND isActive = 1")
    suspend fun getMemberCountByRole(projectId: String, role: ProjectRole): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ProjectMember>)

    @Update
    suspend fun updateMember(member: ProjectMember)

    @Delete
    suspend fun deleteMember(member: ProjectMember)

    @Query("DELETE FROM project_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun removeMemberFromProject(projectId: String, userId: String)

    @Query("UPDATE project_members SET role = :newRole WHERE id = :memberId")
    suspend fun updateMemberRole(memberId: String, newRole: ProjectRole)

    @Query("UPDATE project_members SET isActive = :isActive WHERE id = :memberId")
    suspend fun updateMemberStatus(memberId: String, isActive: Boolean)

    @Query("UPDATE project_members SET lastActivityAt = :timestamp WHERE projectId = :projectId AND userId = :userId")
    suspend fun updateLastActivity(projectId: String, userId: String, timestamp: Long)

    @Query("""
        SELECT COUNT(DISTINCT pm1.projectId)
        FROM project_members pm1
        INNER JOIN project_members pm2 ON pm1.projectId = pm2.projectId
        WHERE pm1.userId = :userId1 AND pm2.userId = :userId2
        AND pm1.isActive = 1 AND pm2.isActive = 1
    """)
    suspend fun getSharedProjectCount(userId1: String, userId2: String): Int

    /**
     * Get all project IDs where user is an active member
     * Used for finding recent collaborators
     *
     * @param userId User ID
     * @return List of project IDs
     */
    @Query("SELECT DISTINCT projectId FROM project_members WHERE userId = :userId AND isActive = 1")
    suspend fun getUserProjectIds(userId: String): List<String>

    /**
     * Get collaborator user IDs from specified projects
     * Returns other members (excluding specified user) ordered by recent activity
     * Used for suggesting recent collaborators in project creation wizard
     *
     * @param projectIds List of project IDs to search
     * @param excludeUserId User ID to exclude (typically the requesting user)
     * @param limit Maximum number of collaborators to return
     * @return List of user IDs ordered by lastActivityAt DESC
     */
    @Query("""
        SELECT DISTINCT userId
        FROM project_members
        WHERE projectId IN (:projectIds)
        AND userId != :excludeUserId
        AND isActive = 1
        ORDER BY lastActivityAt DESC
        LIMIT :limit
    """)
    suspend fun getCollaboratorIds(
        projectIds: List<String>,
        excludeUserId: String,
        limit: Int
    ): List<String>
}
