package com.example.kosmos.core.validators

import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole

/**
 * Validates role-based hierarchy rules for project management
 * Enforces business rules around task assignment and member management
 */
object RoleValidator {

    /**
     * Validation result sealed class
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()

        fun isSuccess(): Boolean = this is Success
        fun getErrorMessage(): String? = (this as? Error)?.message
    }

    /**
     * Check if a user can assign a task to another user based on role hierarchy
     *
     * Business Rule: A user can only assign tasks to members with equal or lower role weight
     * - ADMIN (weight 3) can assign to: ADMIN, MANAGER, MEMBER
     * - MANAGER (weight 2) can assign to: MANAGER, MEMBER
     * - MEMBER (weight 1) can assign to: MEMBER only
     *
     * @param assignerRole Role of the person assigning the task
     * @param assigneeRole Role of the person being assigned the task
     * @return ValidationResult indicating success or error with message
     */
    fun canAssignTask(assignerRole: ProjectRole, assigneeRole: ProjectRole): ValidationResult {
        return if (assignerRole.canAssignTo(assigneeRole)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                "Cannot assign task to ${assigneeRole.getDisplayName()}. " +
                "${assignerRole.getDisplayName()}s can only assign to members with equal or lower roles."
            )
        }
    }

    /**
     * Check if a user can change another member's role
     *
     * Business Rule: Only users with strictly higher role weight can change roles
     * - ADMIN can change: MANAGER, MEMBER roles
     * - MANAGER can change: MEMBER roles only
     * - MEMBER cannot change any roles
     *
     * @param changerRole Role of the person changing the role
     * @param targetCurrentRole Current role of the target member
     * @param newRole New role being assigned
     * @return ValidationResult indicating success or error with message
     */
    fun canChangeRole(
        changerRole: ProjectRole,
        targetCurrentRole: ProjectRole,
        newRole: ProjectRole
    ): ValidationResult {
        // Must have strictly higher role than current role
        if (!changerRole.canManage(targetCurrentRole)) {
            return ValidationResult.Error(
                "Cannot change role of ${targetCurrentRole.getDisplayName()}. " +
                "Only users with higher roles can change member roles."
            )
        }

        // Must have strictly higher role than new role being assigned
        if (!changerRole.canManage(newRole)) {
            return ValidationResult.Error(
                "Cannot assign ${newRole.getDisplayName()} role. " +
                "You can only assign roles lower than your own."
            )
        }

        return ValidationResult.Success
    }

    /**
     * Check if a user can remove another member from the project
     *
     * Business Rule: Only users with strictly higher role weight can remove members
     * - ADMIN can remove: MANAGER, MEMBER
     * - MANAGER can remove: MEMBER only
     * - MEMBER cannot remove anyone
     *
     * @param removerRole Role of the person removing the member
     * @param targetRole Role of the member being removed
     * @return ValidationResult indicating success or error with message
     */
    fun canRemoveMember(removerRole: ProjectRole, targetRole: ProjectRole): ValidationResult {
        return if (removerRole.canManage(targetRole)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                "Cannot remove ${targetRole.getDisplayName()}. " +
                "Only users with higher roles can remove members."
            )
        }
    }

    /**
     * Check if a user can invite members to the project
     *
     * Business Rule: ADMIN and MANAGER can invite, MEMBER cannot
     *
     * @param inviterRole Role of the person inviting
     * @return ValidationResult indicating success or error with message
     */
    fun canInviteMember(inviterRole: ProjectRole): ValidationResult {
        return if (inviterRole == ProjectRole.ADMIN || inviterRole == ProjectRole.MANAGER) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                "Members cannot invite new members. Only Admins and Managers can invite."
            )
        }
    }

    /**
     * Check if a user can edit project settings
     *
     * Business Rule: Only ADMIN can edit project settings
     *
     * @param userRole Role of the user
     * @return ValidationResult indicating success or error with message
     */
    fun canEditProject(userRole: ProjectRole): ValidationResult {
        return if (userRole == ProjectRole.ADMIN) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                "Only Admins can edit project settings."
            )
        }
    }

    /**
     * Check if a user can delete the project
     *
     * Business Rule: Only ADMIN can delete projects
     *
     * @param userRole Role of the user
     * @return ValidationResult indicating success or error with message
     */
    fun canDeleteProject(userRole: ProjectRole): ValidationResult {
        return if (userRole == ProjectRole.ADMIN) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(
                "Only Admins can delete projects."
            )
        }
    }

    /**
     * Get the highest role among a list of members
     *
     * @param members List of project members
     * @return Highest role or null if list is empty
     */
    fun getHighestRole(members: List<ProjectMember>): ProjectRole? {
        return members.maxByOrNull { it.role.weight }?.role
    }

    /**
     * Filter members that a user can assign tasks to based on role
     *
     * @param assignerRole Role of the assigner
     * @param allMembers List of all project members
     * @return List of members that can be assigned tasks
     */
    fun getAssignableMembers(
        assignerRole: ProjectRole,
        allMembers: List<ProjectMember>
    ): List<ProjectMember> {
        return allMembers.filter { member ->
            assignerRole.canAssignTo(member.role)
        }
    }

    /**
     * Check if a member list has at least one admin
     * Useful for ensuring a project always has an admin before removing one
     *
     * @param members List of project members
     * @return true if at least one admin exists
     */
    fun hasAdmin(members: List<ProjectMember>): Boolean {
        return members.any { it.role == ProjectRole.ADMIN && it.isActive }
    }

    /**
     * Validate that removing a member won't leave the project without an admin
     *
     * @param members Current project members
     * @param memberToRemove Member being removed
     * @return ValidationResult indicating success or error with message
     */
    fun canRemoveWithoutBreakingProject(
        members: List<ProjectMember>,
        memberToRemove: ProjectMember
    ): ValidationResult {
        // If removing an admin, ensure at least one other admin remains
        if (memberToRemove.role == ProjectRole.ADMIN) {
            val activeAdmins = members.filter {
                it.role == ProjectRole.ADMIN &&
                it.isActive &&
                it.id != memberToRemove.id
            }
            if (activeAdmins.isEmpty()) {
                return ValidationResult.Error(
                    "Cannot remove the last admin. Projects must have at least one admin."
                )
            }
        }
        return ValidationResult.Success
    }
}
