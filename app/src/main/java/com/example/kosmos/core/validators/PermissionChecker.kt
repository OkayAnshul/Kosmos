package com.example.kosmos.core.validators

import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Checks permissions for project members
 * Handles both default role-based permissions and custom permissions
 */
object PermissionChecker {

    /**
     * Permission check result
     */
    sealed class PermissionResult {
        object Granted : PermissionResult()
        data class Denied(val reason: String) : PermissionResult()

        fun isGranted(): Boolean = this is Granted
        fun getDeniedReason(): String? = (this as? Denied)?.reason
    }

    /**
     * Check if a member has a specific permission
     *
     * Logic:
     * 1. If member has custom permissions set, check against those
     * 2. Otherwise, check against default permissions for their role
     *
     * @param member The project member to check
     * @param permission The permission to check for
     * @return PermissionResult indicating granted or denied with reason
     */
    fun hasPermission(member: ProjectMember, permission: Permission): PermissionResult {
        val permissions = getEffectivePermissions(member)

        return if (permissions.contains(permission)) {
            PermissionResult.Granted
        } else {
            PermissionResult.Denied(
                "This action requires '${permission.getDescription()}' permission. " +
                "Your role (${member.role.getDisplayName()}) does not have this permission."
            )
        }
    }

    /**
     * Check if a member has ALL of the specified permissions
     *
     * @param member The project member to check
     * @param permissions List of permissions to check
     * @return PermissionResult indicating granted or denied with reason
     */
    fun hasAllPermissions(member: ProjectMember, permissions: List<Permission>): PermissionResult {
        val effectivePermissions = getEffectivePermissions(member)
        val missingPermissions = permissions.filter { it !in effectivePermissions }

        return if (missingPermissions.isEmpty()) {
            PermissionResult.Granted
        } else {
            PermissionResult.Denied(
                "Missing permissions: ${missingPermissions.joinToString { it.getDescription() }}"
            )
        }
    }

    /**
     * Check if a member has ANY of the specified permissions
     *
     * @param member The project member to check
     * @param permissions List of permissions to check
     * @return PermissionResult indicating granted or denied with reason
     */
    fun hasAnyPermission(member: ProjectMember, permissions: List<Permission>): PermissionResult {
        val effectivePermissions = getEffectivePermissions(member)
        val hasAny = permissions.any { it in effectivePermissions }

        return if (hasAny) {
            PermissionResult.Granted
        } else {
            PermissionResult.Denied(
                "This action requires one of: ${permissions.joinToString { it.getDescription() }}"
            )
        }
    }

    /**
     * Get the effective permissions for a member
     * Returns custom permissions if set, otherwise default permissions for the role
     *
     * @param member The project member
     * @return Set of permissions the member has
     */
    fun getEffectivePermissions(member: ProjectMember): Set<Permission> {
        // If custom permissions are set, use those
        member.customPermissions?.let { customPermsJson ->
            return try {
                val permissionNames = Json.decodeFromString<List<String>>(customPermsJson)
                permissionNames.mapNotNull { name ->
                    try {
                        Permission.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null // Skip invalid permission names
                    }
                }.toSet()
            } catch (e: Exception) {
                // If parsing fails, fall back to default permissions
                member.role.getDefaultPermissions()
            }
        }

        // Otherwise, use default permissions for the role
        return member.role.getDefaultPermissions()
    }

    /**
     * Require a permission - throws exception if not granted
     * Use this in repository/service layer to enforce permissions
     *
     * @param member The project member
     * @param permission The permission to require
     * @throws PermissionDeniedException if permission is not granted
     */
    @Throws(PermissionDeniedException::class)
    fun requirePermission(member: ProjectMember, permission: Permission) {
        val result = hasPermission(member, permission)
        if (result is PermissionResult.Denied) {
            throw PermissionDeniedException(result.reason)
        }
    }

    /**
     * Require multiple permissions - throws exception if any are not granted
     *
     * @param member The project member
     * @param permissions The permissions to require
     * @throws PermissionDeniedException if any permission is not granted
     */
    @Throws(PermissionDeniedException::class)
    fun requireAllPermissions(member: ProjectMember, permissions: List<Permission>) {
        val result = hasAllPermissions(member, permissions)
        if (result is PermissionResult.Denied) {
            throw PermissionDeniedException(result.reason)
        }
    }

    /**
     * Check if a member can perform a specific action
     * Convenience methods for common actions
     */
    object Actions {
        fun canViewProject(member: ProjectMember): Boolean =
            hasPermission(member, Permission.VIEW_PROJECT).isGranted()

        fun canEditProject(member: ProjectMember): Boolean =
            hasPermission(member, Permission.EDIT_PROJECT).isGranted()

        fun canDeleteProject(member: ProjectMember): Boolean =
            hasPermission(member, Permission.DELETE_PROJECT).isGranted()

        fun canInviteMembers(member: ProjectMember): Boolean =
            hasPermission(member, Permission.INVITE_MEMBERS).isGranted()

        fun canRemoveMembers(member: ProjectMember): Boolean =
            hasPermission(member, Permission.REMOVE_MEMBERS).isGranted()

        fun canChangeRoles(member: ProjectMember): Boolean =
            hasPermission(member, Permission.CHANGE_MEMBER_ROLES).isGranted()

        fun canCreateTasks(member: ProjectMember): Boolean =
            hasPermission(member, Permission.CREATE_TASKS).isGranted()

        fun canEditAnyTask(member: ProjectMember): Boolean =
            hasPermission(member, Permission.EDIT_ANY_TASK).isGranted()

        fun canEditOwnTasks(member: ProjectMember): Boolean =
            hasPermission(member, Permission.EDIT_OWN_TASKS).isGranted()

        fun canDeleteAnyTask(member: ProjectMember): Boolean =
            hasPermission(member, Permission.DELETE_ANY_TASK).isGranted()

        fun canAssignTasks(member: ProjectMember): Boolean =
            hasPermission(member, Permission.ASSIGN_TASKS).isGranted()

        fun canSendMessages(member: ProjectMember): Boolean =
            hasPermission(member, Permission.SEND_MESSAGES).isGranted()

        fun canDeleteAnyMessage(member: ProjectMember): Boolean =
            hasPermission(member, Permission.DELETE_ANY_MESSAGE).isGranted()

        fun canCreateChatRooms(member: ProjectMember): Boolean =
            hasPermission(member, Permission.CREATE_CHAT_ROOMS).isGranted()

        fun canUploadFiles(member: ProjectMember): Boolean =
            hasPermission(member, Permission.UPLOAD_FILES).isGranted()
    }

    /**
     * Get a user-friendly list of what a member can and cannot do
     * Useful for displaying in UI
     *
     * @param member The project member
     * @return Map of permission categories to lists of granted/denied permissions
     */
    fun getPermissionSummary(member: ProjectMember): Map<String, List<PermissionSummaryItem>> {
        val effectivePermissions = getEffectivePermissions(member)
        val categories = Permission.getPermissionsByCategory()

        return categories.mapValues { (_, permissions) ->
            permissions.map { permission ->
                PermissionSummaryItem(
                    permission = permission,
                    granted = permission in effectivePermissions,
                    description = permission.getDescription()
                )
            }
        }
    }

    /**
     * Data class for permission summary
     */
    data class PermissionSummaryItem(
        val permission: Permission,
        val granted: Boolean,
        val description: String
    )

    /**
     * Exception thrown when a required permission is not granted
     */
    class PermissionDeniedException(message: String) : SecurityException(message)
}
