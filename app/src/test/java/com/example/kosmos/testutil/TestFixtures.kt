package com.example.kosmos.testutil

import com.example.kosmos.core.models.DependencyType
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskDependency
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.User
import java.util.UUID

/**
 * Common test data builders for use across unit and integration tests.
 */
object TestFixtures {

    fun user(
        id: String = UUID.randomUUID().toString(),
        email: String = "test@example.com",
        username: String = "testuser",
        displayName: String = "Test User"
    ) = User(
        id = id,
        email = email,
        username = username,
        displayName = displayName
    )

    fun projectMember(
        id: String = UUID.randomUUID().toString(),
        projectId: String = "proj-1",
        userId: String = "user-1",
        role: ProjectRole = ProjectRole.MEMBER,
        customPermissions: String? = null
    ) = ProjectMember(
        id = id,
        projectId = projectId,
        userId = userId,
        role = role,
        customPermissions = customPermissions
    )

    fun task(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Task",
        projectId: String = "proj-1",
        status: TaskStatus = TaskStatus.TODO,
        priority: TaskPriority = TaskPriority.MEDIUM,
        assignedToId: String? = null,
        createdById: String = "user-1"
    ) = Task(
        id = id,
        title = title,
        projectId = projectId,
        status = status,
        priority = priority,
        assignedToId = assignedToId,
        createdById = createdById
    )

    fun taskDependency(
        id: String = UUID.randomUUID().toString(),
        taskId: String,
        dependsOnTaskId: String,
        dependencyType: DependencyType = DependencyType.BLOCKS,
        createdBy: String = "user-1"
    ) = TaskDependency(
        id = id,
        taskId = taskId,
        dependsOnTaskId = dependsOnTaskId,
        dependencyType = dependencyType,
        createdBy = createdBy
    )
}
