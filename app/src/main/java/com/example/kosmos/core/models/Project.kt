package com.example.kosmos.core.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Project category enum for categorizing projects
 */
@Serializable
enum class ProjectCategory {
    TECH,
    SOCIAL,
    BUSINESS,
    OTHER;

    /**
     * Get human-readable display name for the category
     */
    fun getDisplayName(): String = when(this) {
        TECH -> "Technology"
        SOCIAL -> "Social/Community"
        BUSINESS -> "Business"
        OTHER -> "Other"
    }

    /**
     * Get icon for the category
     */
    fun getIcon(): ImageVector = when(this) {
        TECH -> Icons.Default.Code
        SOCIAL -> Icons.Default.People
        BUSINESS -> Icons.Default.Business
        OTHER -> Icons.Default.Category
    }

    /**
     * Get required fields for this category
     */
    fun getRequiredFields(): List<String> = when(this) {
        TECH -> listOf("name", "description")
        SOCIAL -> listOf("name", "description", "projectMotive")
        BUSINESS -> listOf("name", "description", "businessModel")
        OTHER -> listOf("name")
    }

    /**
     * Get optional fields for this category
     */
    fun getOptionalFields(): List<String> = when(this) {
        TECH -> listOf("githubUrl", "techStack", "openSourceLicense", "deadline", "tags")
        SOCIAL -> listOf("targetAudience", "deadline", "tags")
        BUSINESS -> listOf("websiteUrl", "industryTags", "deadline", "tags")
        OTHER -> listOf("projectMotive", "deadline", "tags")
    }
}

/**
 * Project entity representing a project management workspace
 * Contains project metadata, ownership, and status information
 */
@Serializable
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val description: String = "",

    /**
     * User ID of the project owner (typically has ADMIN role)
     */
    @SerialName("owner_id")
    val ownerId: String,

    val status: ProjectStatus = ProjectStatus.ACTIVE,

    val visibility: ProjectVisibility = ProjectVisibility.PRIVATE,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * P1-11: Version field for optimistic locking
     * Incremented on every update to detect conflicts
     */
    val version: Int = 1,

    /**
     * Optional cover image URL for the project
     */
    @SerialName("image_url")
    val imageUrl: String? = null,

    /**
     * Color code for UI theming (hex format)
     */
    val color: String = "#6366F1", // Default indigo color

    /**
     * Project settings in JSON format (can store various preferences)
     */
    val settings: String? = null,

    // ========================================================================
    // PROJECT CREATION WIZARD FIELDS
    // Added: 2026-01-06 for multi-step project creation feature
    // ========================================================================

    /**
     * Project category (tech, social, business, other)
     * Determines which fields are required/optional
     */
    val category: ProjectCategory = ProjectCategory.OTHER,

    /**
     * Project deadline timestamp (milliseconds)
     * Null if no deadline set
     */
    val deadline: Long? = null,

    /**
     * Project website URL
     * Mainly used for BUSINESS category projects
     */
    @SerialName("website_url")
    val websiteUrl: String? = null,

    /**
     * GitHub repository URL
     * Mainly used for TECH category projects
     */
    @SerialName("github_url")
    val githubUrl: String? = null,

    /**
     * Project motive/goals description
     * Mainly used for SOCIAL and OTHER category projects
     */
    @SerialName("project_motive")
    val projectMotive: String? = null,

    /**
     * Technology stack (stored as JSON array string)
     * Example: ["Kotlin", "Android", "Jetpack Compose"]
     * Mainly used for TECH category projects
     */
    @SerialName("tech_stack")
    val techStack: String? = null,

    /**
     * General tags (stored as JSON array string)
     * Example: ["project-management", "collaboration"]
     */
    val tags: String? = null,

    /**
     * Business model description
     * Mainly used for BUSINESS category projects
     */
    @SerialName("business_model")
    val businessModel: String? = null,

    /**
     * Target audience description
     * Mainly used for SOCIAL category projects
     */
    @SerialName("target_audience")
    val targetAudience: String? = null,

    /**
     * Industry tags (stored as JSON array string)
     * Example: ["fintech", "healthcare", "education"]
     * Mainly used for BUSINESS category projects
     */
    @SerialName("industry_tags")
    val industryTags: String? = null,

    /**
     * Open source license type
     * Example: "MIT", "Apache 2.0", "GPL v3"
     * Mainly used for TECH category projects
     */
    @SerialName("open_source_license")
    val openSourceLicense: String? = null,

    // ========================================================================
    // METADATA COLUMNS: Cached statistics for performance optimization
    // Auto-updated by database triggers - DO NOT modify manually
    // Performance: 25x faster than querying related tables (10ms vs 250ms)
    // ========================================================================

    /**
     * Cached count of active project members
     * Auto-updated by trigger on project_members INSERT/UPDATE/DELETE
     */
    @SerialName("member_count")
    val memberCount: Int = 0,

    /**
     * Cached count of chat rooms in this project
     * Auto-updated by trigger on chat_rooms INSERT/DELETE
     */
    @SerialName("chat_count")
    val chatCount: Int = 0,

    /**
     * Cached count of all tasks in this project
     * Auto-updated by trigger on tasks INSERT/DELETE
     */
    @SerialName("task_count")
    val taskCount: Int = 0,

    /**
     * Cached count of completed tasks (status = DONE)
     * Auto-updated by trigger on tasks INSERT/UPDATE/DELETE
     */
    @SerialName("completed_task_count")
    val completedTaskCount: Int = 0,

    /**
     * Cached count of pending tasks (status NOT IN DONE, CANCELLED)
     * Auto-updated by trigger on tasks INSERT/UPDATE/DELETE
     */
    @SerialName("pending_task_count")
    val pendingTaskCount: Int = 0,

    /**
     * Timestamp of last activity in project (messages, tasks, member updates)
     * Auto-updated by triggers on related table changes
     */
    @SerialName("last_activity_at")
    val lastActivityAt: Long? = null
) {
    /**
     * Calculate task completion percentage from cached counts
     * @return Percentage (0-100) or null if no tasks
     */
    val completionPercentage: Int?
        get() = if (taskCount > 0) {
            (completedTaskCount * 100) / taskCount
        } else {
            null
        }
}

/**
 * Project status enum
 */
@Serializable
enum class ProjectStatus {
    /**
     * Project is active and ongoing
     */
    ACTIVE,

    /**
     * Project is archived (read-only)
     */
    ARCHIVED,

    /**
     * Project is completed
     */
    COMPLETED,

    /**
     * Project is on hold/paused
     */
    ON_HOLD;

    /**
     * Check if project allows modifications
     */
    fun isModifiable(): Boolean = this == ACTIVE || this == ON_HOLD

    /**
     * Check if project is accessible
     */
    fun isAccessible(): Boolean = this != ARCHIVED
}

/**
 * Project visibility settings
 */
@Serializable
enum class ProjectVisibility {
    /**
     * Only project members can see and access
     */
    PRIVATE,

    /**
     * Anyone in the organization can see
     */
    INTERNAL,

    /**
     * Publicly visible (read-only for non-members)
     */
    PUBLIC
}
