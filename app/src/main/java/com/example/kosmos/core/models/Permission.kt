package com.example.kosmos.core.models

import kotlinx.serialization.Serializable

/**
 * Permission enum defining all possible actions in the project management system
 * Used for role-based access control (RBAC)
 */
@Serializable
enum class Permission {
    // Project Management Permissions
    /**
     * Can view project details and settings
     */
    VIEW_PROJECT,

    /**
     * Can edit project name, description, settings
     */
    EDIT_PROJECT,

    /**
     * Can delete the entire project
     */
    DELETE_PROJECT,

    /**
     * Can archive/unarchive the project
     */
    ARCHIVE_PROJECT,

    // Member Management Permissions
    /**
     * Can view list of project members
     */
    VIEW_MEMBERS,

    /**
     * Can invite new members to the project
     */
    INVITE_MEMBERS,

    /**
     * Can remove members from the project
     */
    REMOVE_MEMBERS,

    /**
     * Can change roles of other members
     */
    CHANGE_MEMBER_ROLES,

    // Task Management Permissions
    /**
     * Can view tasks in the project
     */
    VIEW_TASKS,

    /**
     * Can create new tasks
     */
    CREATE_TASKS,

    /**
     * Can edit any task (not just assigned ones)
     */
    EDIT_ANY_TASK,

    /**
     * Can edit only tasks assigned to self
     */
    EDIT_OWN_TASKS,

    /**
     * Can delete any task
     */
    DELETE_ANY_TASK,

    /**
     * Can delete only tasks created by self
     */
    DELETE_OWN_TASKS,

    /**
     * Can assign tasks to other members
     */
    ASSIGN_TASKS,

    /**
     * Can change task status (TODO, IN_PROGRESS, DONE, etc.)
     */
    CHANGE_TASK_STATUS,

    /**
     * Can change task priority
     */
    CHANGE_TASK_PRIORITY,

    /**
     * Can add comments to tasks
     */
    COMMENT_ON_TASKS,

    // Chat/Communication Permissions
    /**
     * Can view chat rooms
     */
    VIEW_CHAT,

    /**
     * Can send messages in chat rooms
     */
    SEND_MESSAGES,

    /**
     * Can delete own messages
     */
    DELETE_OWN_MESSAGES,

    /**
     * Can delete any message in the project
     */
    DELETE_ANY_MESSAGE,

    /**
     * Can create new chat rooms within project
     */
    CREATE_CHAT_ROOMS,

    /**
     * Can manage (edit/delete) chat rooms
     */
    MANAGE_CHAT_ROOMS,

    // File/Storage Permissions
    /**
     * Can upload files to the project
     */
    UPLOAD_FILES,

    /**
     * Can delete any file
     */
    DELETE_ANY_FILE,

    /**
     * Can delete only files uploaded by self
     */
    DELETE_OWN_FILES;

    companion object {
        /**
         * Default permissions for ADMIN role
         * Admins have full control over the project
         */
        val ADMIN_PERMISSIONS = setOf(
            // Project
            VIEW_PROJECT,
            EDIT_PROJECT,
            DELETE_PROJECT,
            ARCHIVE_PROJECT,

            // Members
            VIEW_MEMBERS,
            INVITE_MEMBERS,
            REMOVE_MEMBERS,
            CHANGE_MEMBER_ROLES,

            // Tasks
            VIEW_TASKS,
            CREATE_TASKS,
            EDIT_ANY_TASK,
            EDIT_OWN_TASKS,
            DELETE_ANY_TASK,
            DELETE_OWN_TASKS,
            ASSIGN_TASKS,
            CHANGE_TASK_STATUS,
            CHANGE_TASK_PRIORITY,
            COMMENT_ON_TASKS,

            // Chat
            VIEW_CHAT,
            SEND_MESSAGES,
            DELETE_OWN_MESSAGES,
            DELETE_ANY_MESSAGE,
            CREATE_CHAT_ROOMS,
            MANAGE_CHAT_ROOMS,

            // Files
            UPLOAD_FILES,
            DELETE_ANY_FILE,
            DELETE_OWN_FILES
        )

        /**
         * Default permissions for MANAGER role
         * Managers can manage tasks and chat but not project settings
         */
        val MANAGER_PERMISSIONS = setOf(
            // Project (read-only)
            VIEW_PROJECT,

            // Members (can view and invite, but not remove or change roles)
            VIEW_MEMBERS,
            INVITE_MEMBERS,

            // Tasks (full control)
            VIEW_TASKS,
            CREATE_TASKS,
            EDIT_ANY_TASK,
            EDIT_OWN_TASKS,
            DELETE_ANY_TASK,
            DELETE_OWN_TASKS,
            ASSIGN_TASKS,
            CHANGE_TASK_STATUS,
            CHANGE_TASK_PRIORITY,
            COMMENT_ON_TASKS,

            // Chat (full control)
            VIEW_CHAT,
            SEND_MESSAGES,
            DELETE_OWN_MESSAGES,
            DELETE_ANY_MESSAGE,
            CREATE_CHAT_ROOMS,
            MANAGE_CHAT_ROOMS,

            // Files
            UPLOAD_FILES,
            DELETE_OWN_FILES
        )

        /**
         * Default permissions for MEMBER role
         * Members have basic read/write access but limited management capabilities
         */
        val MEMBER_PERMISSIONS = setOf(
            // Project (read-only)
            VIEW_PROJECT,

            // Members (read-only)
            VIEW_MEMBERS,

            // Tasks (can only edit own tasks, can't assign or delete others)
            VIEW_TASKS,
            CREATE_TASKS,
            EDIT_OWN_TASKS,
            DELETE_OWN_TASKS,
            CHANGE_TASK_STATUS, // Can update status of assigned tasks
            COMMENT_ON_TASKS,

            // Chat (basic messaging)
            VIEW_CHAT,
            SEND_MESSAGES,
            DELETE_OWN_MESSAGES,

            // Files (basic upload)
            UPLOAD_FILES,
            DELETE_OWN_FILES
        )

        /**
         * Get all permissions as a list for UI/admin panels
         */
        fun getAllPermissions(): List<Permission> = values().toList()

        /**
         * Get permissions by category for organized UI display
         */
        fun getPermissionsByCategory(): Map<String, List<Permission>> {
            return mapOf(
                "Project Management" to listOf(
                    VIEW_PROJECT,
                    EDIT_PROJECT,
                    DELETE_PROJECT,
                    ARCHIVE_PROJECT
                ),
                "Member Management" to listOf(
                    VIEW_MEMBERS,
                    INVITE_MEMBERS,
                    REMOVE_MEMBERS,
                    CHANGE_MEMBER_ROLES
                ),
                "Task Management" to listOf(
                    VIEW_TASKS,
                    CREATE_TASKS,
                    EDIT_ANY_TASK,
                    EDIT_OWN_TASKS,
                    DELETE_ANY_TASK,
                    DELETE_OWN_TASKS,
                    ASSIGN_TASKS,
                    CHANGE_TASK_STATUS,
                    CHANGE_TASK_PRIORITY,
                    COMMENT_ON_TASKS
                ),
                "Communication" to listOf(
                    VIEW_CHAT,
                    SEND_MESSAGES,
                    DELETE_OWN_MESSAGES,
                    DELETE_ANY_MESSAGE,
                    CREATE_CHAT_ROOMS,
                    MANAGE_CHAT_ROOMS
                ),
                "File Management" to listOf(
                    UPLOAD_FILES,
                    DELETE_ANY_FILE,
                    DELETE_OWN_FILES
                )
            )
        }
    }

    /**
     * Get human-readable description of this permission
     */
    fun getDescription(): String {
        return when (this) {
            VIEW_PROJECT -> "View project details and settings"
            EDIT_PROJECT -> "Edit project information"
            DELETE_PROJECT -> "Delete the project"
            ARCHIVE_PROJECT -> "Archive or restore the project"

            VIEW_MEMBERS -> "View project members"
            INVITE_MEMBERS -> "Invite new members"
            REMOVE_MEMBERS -> "Remove members from project"
            CHANGE_MEMBER_ROLES -> "Change member roles"

            VIEW_TASKS -> "View all tasks"
            CREATE_TASKS -> "Create new tasks"
            EDIT_ANY_TASK -> "Edit any task"
            EDIT_OWN_TASKS -> "Edit tasks assigned to you"
            DELETE_ANY_TASK -> "Delete any task"
            DELETE_OWN_TASKS -> "Delete your own tasks"
            ASSIGN_TASKS -> "Assign tasks to members"
            CHANGE_TASK_STATUS -> "Update task status"
            CHANGE_TASK_PRIORITY -> "Change task priority"
            COMMENT_ON_TASKS -> "Add comments to tasks"

            VIEW_CHAT -> "View chat messages"
            SEND_MESSAGES -> "Send chat messages"
            DELETE_OWN_MESSAGES -> "Delete your own messages"
            DELETE_ANY_MESSAGE -> "Delete any message"
            CREATE_CHAT_ROOMS -> "Create new chat rooms"
            MANAGE_CHAT_ROOMS -> "Manage chat room settings"

            UPLOAD_FILES -> "Upload files to project"
            DELETE_ANY_FILE -> "Delete any file"
            DELETE_OWN_FILES -> "Delete your own files"
        }
    }

    /**
     * Check if this is a high-risk permission that requires confirmation
     */
    fun isHighRisk(): Boolean {
        return this in setOf(
            DELETE_PROJECT,
            REMOVE_MEMBERS,
            CHANGE_MEMBER_ROLES,
            DELETE_ANY_TASK,
            DELETE_ANY_MESSAGE,
            DELETE_ANY_FILE
        )
    }
}
