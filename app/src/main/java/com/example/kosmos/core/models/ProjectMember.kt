package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * ProjectMember entity representing a user's membership in a project
 * Includes role-based access control and permission management
 */
@Serializable
@Entity(tableName = "project_members")
data class ProjectMember(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * Project ID this membership belongs to
     */
    val projectId: String,

    /**
     * User ID of the member
     */
    val userId: String,

    /**
     * Role of the member in the project
     */
    val role: ProjectRole,

    /**
     * When the member joined the project
     */
    val joinedAt: Long = System.currentTimeMillis(),

    /**
     * Who invited/added this member
     */
    val invitedBy: String? = null,

    /**
     * Is this member active in the project
     */
    val isActive: Boolean = true,

    /**
     * Last activity timestamp
     */
    val lastActivityAt: Long = System.currentTimeMillis(),

    /**
     * Custom permissions override (JSON array of Permission enum names)
     * If null, uses default permissions for the role
     */
    val customPermissions: String? = null
)

/**
 * Project role enum with hierarchical weights
 * Higher weight means higher authority in the hierarchy
 */
@Serializable
enum class ProjectRole {
    /**
     * Project administrator - Full control over project
     * Weight: 3 (highest)
     */
    ADMIN,

    /**
     * Project manager - Can manage tasks and members
     * Weight: 2
     */
    MANAGER,

    /**
     * Regular project member - Limited permissions
     * Weight: 1 (lowest)
     */
    MEMBER;

    /**
     * Hierarchical weight for role comparison
     * Higher weight = higher authority
     */
    val weight: Int
        get() = when (this) {
            ADMIN -> 3
            MANAGER -> 2
            MEMBER -> 1
        }

    /**
     * Check if this role can manage another role
     * (must have strictly higher weight)
     *
     * @param other The role to check management capability against
     * @return true if this role has authority over the other role
     *
     * Examples:
     * - ADMIN.canManage(MANAGER) = true
     * - ADMIN.canManage(ADMIN) = false (equal)
     * - MANAGER.canManage(ADMIN) = false
     */
    fun canManage(other: ProjectRole): Boolean {
        return this.weight > other.weight
    }

    /**
     * Check if this role can assign tasks to another role
     * (must have equal or higher weight)
     *
     * @param other The role to check assignment capability against
     * @return true if this role can assign to the other role
     *
     * Examples:
     * - ADMIN.canAssignTo(MANAGER) = true
     * - ADMIN.canAssignTo(ADMIN) = true (equal)
     * - MEMBER.canAssignTo(MANAGER) = false
     */
    fun canAssignTo(other: ProjectRole): Boolean {
        return this.weight >= other.weight
    }

    /**
     * Check if this role can perform an action on a member with another role
     * Generic method for any hierarchical permission check
     *
     * @param other The target role
     * @param requireStrictlyHigher If true, requires higher weight. If false, allows equal weight
     * @return true if action is allowed
     */
    fun canActOn(other: ProjectRole, requireStrictlyHigher: Boolean = true): Boolean {
        return if (requireStrictlyHigher) {
            this.weight > other.weight
        } else {
            this.weight >= other.weight
        }
    }

    /**
     * Get default permissions for this role
     * @return Set of permissions granted to this role by default
     */
    fun getDefaultPermissions(): Set<Permission> {
        return when (this) {
            ADMIN -> Permission.ADMIN_PERMISSIONS
            MANAGER -> Permission.MANAGER_PERMISSIONS
            MEMBER -> Permission.MEMBER_PERMISSIONS
        }
    }

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return when (this) {
            ADMIN -> "Administrator"
            MANAGER -> "Manager"
            MEMBER -> "Member"
        }
    }

    /**
     * Get color code for role badge in UI
     */
    fun getColorCode(): String {
        return when (this) {
            ADMIN -> "#EF4444" // Red
            MANAGER -> "#F59E0B" // Amber
            MEMBER -> "#10B981" // Green
        }
    }
}
