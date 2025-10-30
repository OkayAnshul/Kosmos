package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): Project?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: String): Flow<Project?>

    @Query("SELECT * FROM projects WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getProjectsByOwner(userId: String): Flow<List<Project>>

    @Query("""
        SELECT DISTINCT p.* FROM projects p
        LEFT JOIN project_members pm ON p.id = pm.projectId
        WHERE p.ownerId = :userId OR pm.userId = :userId
        ORDER BY p.updatedAt DESC
    """)
    fun getProjectsByUserMembership(userId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: ProjectStatus): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchProjects(query: String): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<Project>)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("UPDATE projects SET status = :status, updatedAt = :timestamp WHERE id = :projectId")
    suspend fun updateProjectStatus(projectId: String, status: ProjectStatus, timestamp: Long)

    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :userId")
    suspend fun getProjectCountByOwner(userId: String): Int
}
