# Multi-User & RBAC Testing

**Date**: 2026-02-15
**Status**: 60% coverage — Task CRUD good, Chat completely unprotected

## Users in System

| User | Email | Memberships | Role |
|------|-------|-------------|------|
| Anshul | anshulisokay@gmail.com | 29 | Owner/ADMIN |
| Anshul (alt) | anshulis.okay@gmail.com | 13 | ADMIN |
| Test Admin | admin@rbactest.kosmos | 18 | ADMIN |
| Test Manager | manager@rbactest.kosmos | 19 | MANAGER |
| Test Member | member@rbactest.kosmos | 19 | MEMBER |
| Test Second Admin | admin2@rbactest.kosmos | 19 | ADMIN |

## RBAC Coverage by Repository

### TaskRepository: 60% covered
**Enforced**: createTask, updateTask, updateTaskStatus, assignTask, deleteTask
**Missing RBAC**:
- Comment operations (no COMMENT_ON_TASKS check)
- Journal entries (no project membership validation)
- Time tracking (startTimer, stopTimer, addManualTimeEntry, deleteTimeEntry)
- Task dependencies (addDependency, removeDependency)
- unassignTask (no ASSIGN_TASKS check)

### ProjectRepository: 85% covered
**Enforced**: updateProject, deleteProject, updateProjectStatus, addMember, removeMember, changeRole
**Missing**: Project creation (no org-level permission)

### ChatRepository: 10% covered
**Missing RBAC on ALL operations**:
- sendMessage, createChatRoom, deleteChatRoom, editMessage, deleteMessage
- toggleReaction, archiveChatRoom, pinChatRoom, addUserToChatRoom

## Role Hierarchy: CORRECT
- ADMIN > MANAGER > MEMBER weight system works
- canManage(), canAssignTo(), canActOn() properly implemented
- RoleValidator prevents escalation (can't assign higher role than own)
- canRemoveWithoutBreakingProject() ensures last admin can't be removed

## Custom Permissions Override: WORKING
- PermissionChecker.getEffectivePermissions() handles default + custom
- JSON parsing fallback to defaults on error

## RLS Server-Side Enforcement: ENABLED
All 11 tables now have RLS with correct policies matching RBAC roles.
