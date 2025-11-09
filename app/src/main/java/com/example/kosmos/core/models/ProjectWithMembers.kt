package com.example.kosmos.core.models

/**
 * Data class combining a Project with its members
 * Used for displaying project information with member context
 */
data class ProjectWithMembers(
    val project: Project,
    val members: List<ProjectMember>
)
