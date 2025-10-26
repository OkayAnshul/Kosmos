package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

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
    val ownerId: String,

    val status: ProjectStatus = ProjectStatus.ACTIVE,

    val visibility: ProjectVisibility = ProjectVisibility.PRIVATE,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * Optional cover image URL for the project
     */
    val imageUrl: String? = null,

    /**
     * Color code for UI theming (hex format)
     */
    val color: String = "#6366F1", // Default indigo color

    /**
     * Project settings in JSON format (can store various preferences)
     */
    val settings: String? = null
)

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
