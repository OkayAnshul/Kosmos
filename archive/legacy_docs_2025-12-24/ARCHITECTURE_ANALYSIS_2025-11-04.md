# KOSMOS ARCHITECTURE ANALYSIS - 2025-11-04

## 🎯 CORRECT ARCHITECTURE UNDERSTANDING

### System Type
**Project Management System with Team Collaboration**
- NOT a standalone chat app
- Projects are the primary entity
- Everything flows from Projects → Members → Chatrooms → Tasks

### Entity Hierarchy (Top to Bottom)

```
┌─────────────────────────────────────────┐
│           1. PROJECTS                    │
│  - Created by Project Owner (ADMIN)      │
│  - Has name, description, status         │
│  - Contains project members with roles   │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
┌──────▼─────────┐  ┌──▼──────────────────┐
│ 2. MEMBERS      │  │ 3. CHAT ROOMS       │
│  - PROJECT_ADMIN│  │  - Created within    │
│  - MANAGER      │  │    a project         │
│  - MEMBER       │  │  - Participants from │
│  - Permissions  │  │    project members   │
└──────┬─────────┘  └──┬──────────────────┘
       │               │
       └───────┬───────┘
               │
      ┌────────▼────────┐
      │   4. TASKS       │
      │ - Created by     │
      │   authorized     │
      │   members        │
      │ - Assigned to    │
      │   project members│
      │ - Referenced in  │
      │   chat rooms     │
      └──────────────────┘
```

## 📋 CORRECT WORKFLOW

### 1. Project Creation
```
User (becomes ADMIN) → Creates Project → Invites Members
```

### 2. Member Invitation
```
ADMIN/MANAGER → Invites users to project → Users become PROJECT MEMBERS
```

### 3. Chat Room Creation
```
ADMIN/MANAGER → Creates chat room WITHIN project → Selects participants FROM existing project members
```

### 4. Task Creation
```
ADMIN/MANAGER/MEMBER (with permission) → Creates task IN project → Can reference in chat room
```

## 🐛 IDENTIFIED ISSUES

### Issue 1: Invite Members Flow is Wrong ❌
**Current Behavior**: Shows global user search (find users from entire system)
**Correct Behavior**: Should show project member invitation (invite external users to project)

**What it should do**:
1. User clicks "Invite Members" for a project
2. Shows user search across ALL system users (not just project members)
3. Selected users receive project invitation
4. Once accepted, they become project members with assigned role

### Issue 2: Create Chat Room Flow is Wrong ❌
**Current Behavior**: Shows "Find Users" with global search
**Correct Behavior**: Should show selection from existing project members ONLY

**What it should do**:
1. User clicks "Create Chat Room" within a project
2. Shows list of EXISTING project members ONLY
3. User selects multiple members
4. Creates chat room with selected participants
5. All participants must already be project members

### Issue 3: Task Creation Validation Error ❌
**Error**: "You are not a member of this project"
**Root Cause**: TaskViewModel doesn't receive/use projectId correctly

**Code Flow**:
```
QuickTaskCreationSheetWrapper (has projectId)
  → viewModel.createTask() (uses currentProjectId = null/empty)
    → TaskRepository.createTask(task with projectId="")
      → Checks project membership: projectMemberDao.getMemberByProjectAndUser(projectId="", userId)
        → Returns null (no member found)
          → Throws "You are not a member of this project"
```

**What's wrong**:
- Wrapper receives `projectId` parameter ✅
- Wrapper calls `viewModel.createTask()` but doesn't pass projectId ❌
- ViewModel uses `currentProjectId` which is null unless `loadTasks()` was called ❌
- Task is created with empty projectId ❌

### Issue 4: Missing Project Context in Task Creation ❌
**Problem**: TaskViewModel maintains state `currentProjectId` that relies on loading chat room first
**Issue**: When creating task from project view (not chat), projectId is not set

## 🔧 FIXES REQUIRED

### Fix 1: Update TaskViewModel.createTask() signature
```kotlin
// CURRENT (WRONG)
fun createTask(
    chatRoomId: String? = null,
    title: String,
    description: String,
    priority: TaskPriority = TaskPriority.MEDIUM,
    assignedToId: String? = null,
    dueDate: Long? = null,
    tags: List<String> = emptyList()
)

// CORRECT
fun createTask(
    projectId: String,  // ← ADD THIS REQUIRED PARAMETER
    chatRoomId: String? = null,
    title: String,
    description: String,
    priority: TaskPriority = TaskPriority.MEDIUM,
    assignedToId: String? = null,
    dueDate: Long? = null,
    tags: List<String> = emptyList()
)
```

### Fix 2: Update QuickTaskCreationSheetWrapper to pass projectId
```kotlin
// In onCreate handler
viewModel.createTask(
    projectId = projectId,  // ← ADD THIS
    chatRoomId = chatRoomId,
    title = quickTaskData.title,
    // ... rest
)
```

### Fix 3: Fix Create Chat Flow
**File**: `ChatListViewModel.kt` - `loadProjectMembers()`

**Current**: Already correct! Uses `projectRepository.getProjectMembersFlow(projectId)`

**UI Issue**: Dialog title/text needs clarification that these are project members

### Fix 4: Fix Invite Members Flow
**File**: `InviteMembersScreen.kt` / `InviteMembersViewModel.kt`

**Current**: Searches ALL users (correct for invitation)
**Issue**: Needs to actually INVITE them to project, not just show them

**Required**:
1. Search returns users NOT already in project ✅
2. Selection adds them with role assignment
3. Sends invitation to project
4. Updates project_members table

## 📊 DATABASE SCHEMA VALIDATION

### Tables Structure (Based on SCHEMA_FIX_COMPLETE_V2.sql)

#### 1. projects
```sql
- id (UUID, PK)
- name (TEXT, NOT NULL)
- description (TEXT)
- owner_id (UUID, FK → users)
- status (TEXT)
- created_at (BIGINT)
- updated_at (BIGINT)
```

#### 2. project_members
```sql
- id (UUID, PK)
- project_id (UUID, FK → projects, CASCADE)
- user_id (UUID, FK → users, CASCADE)
- role (TEXT) -- PROJECT_ADMIN, MANAGER, MEMBER
- joined_at (BIGINT)
- invited_by (UUID, FK → users)
- is_active (BOOLEAN)
- last_activity_at (BIGINT)
- custom_permissions (JSONB)

UNIQUE (project_id, user_id)
```

#### 3. chat_rooms
```sql
- id (UUID, PK)
- project_id (UUID, FK → projects, CASCADE)
- name (TEXT, NOT NULL)
- description (TEXT)
- type (TEXT) -- GENERAL, CHANNEL, ANNOUNCEMENTS
- created_by (UUID, FK → users)
- created_at (BIGINT)
- participant_ids (TEXT[]) -- Array of user IDs
- is_active (BOOLEAN)
```

#### 4. tasks
```sql
- id (UUID, PK)
- project_id (UUID, FK → projects, CASCADE)  ← REQUIRED!
- chat_room_id (UUID, FK → chat_rooms, NULL) ← Optional reference
- title (TEXT, NOT NULL)
- description (TEXT)
- status (TEXT)
- priority (TEXT)
- assigned_to_id (UUID, FK → users, NULL)
- assigned_to_name (TEXT)
- created_by_id (UUID, FK → users)
- created_by_name (TEXT)
- due_date (BIGINT)
- tags (TEXT[])
- parent_task_id (UUID, FK → tasks, NULL)
- comments (JSONB)
- created_at (BIGINT)
- updated_at (BIGINT)
```

### Schema Validation: ✅ CORRECT

The database schema is **correctly designed** for a project management system:

1. ✅ Projects are top-level entities
2. ✅ project_members links users to projects with roles
3. ✅ chat_rooms belong to projects (CASCADE delete)
4. ✅ tasks belong to projects (CASCADE delete)
5. ✅ Foreign key constraints ensure referential integrity
6. ✅ Indexes on common query paths

## 🎯 CORRECT USER FLOWS

### Flow 1: New User Joins System
```
1. User registers/logs in
2. User creates their first project (becomes PROJECT_ADMIN)
   OR
   User receives invitation to existing project (becomes MEMBER)
```

### Flow 2: Project Owner Builds Team
```
1. ADMIN clicks "Invite Members"
2. Searches ALL users in system
3. Selects users and assigns roles (MANAGER/MEMBER)
4. System sends invitations
5. Invited users accept → become project_members
```

### Flow 3: Creating Communication Channels
```
1. ADMIN/MANAGER clicks "Create Chat Room"
2. Enters room name and description
3. Selects participants FROM existing project members
4. Creates room
5. All participants can view/chat
```

### Flow 4: Task Management
```
1. Authorized member (ADMIN/MANAGER/MEMBER with permission) creates task
2. Task is created IN the project
3. Task can optionally be referenced in a chat room
4. Task can be assigned to any project member
5. Task visible in project task board
```

## ⚠️ CRITICAL DESIGN DECISIONS

### Decision 1: Project Context is ALWAYS Required
- Tasks MUST have a projectId
- Chat rooms MUST have a projectId
- Members belong to projects
- NEVER create cross-project entities

### Decision 2: Role Hierarchy
```
PROJECT_ADMIN (Project Owner)
  ↓ Can do everything
MANAGER
  ↓ Can manage members, create tasks, create chat rooms
MEMBER
  ↓ Can view, participate, create tasks (if granted permission)
```

### Decision 3: Permission System
- Roles have default permissions
- Custom permissions can be granted per member
- Permission checks happen in Repository layer
- UI should respect permissions (hide unauthorized actions)

## 📝 IMPLEMENTATION CHECKLIST

### Immediate Fixes (Critical)
- [ ] Fix TaskViewModel.createTask() - Add projectId parameter
- [ ] Fix QuickTaskCreationSheetWrapper - Pass projectId
- [ ] Fix CreateChatDialog - Clarify "project members only"
- [ ] Test task creation with correct projectId

### Follow-up Improvements (Important)
- [ ] Add project context to all ViewModels that need it
- [ ] Implement proper invitation system for project members
- [ ] Add UI indicators for project context (breadcrumbs)
- [ ] Add role-based UI visibility (hide unauthorized actions)
- [ ] Add project member count in project list

### Documentation Updates
- [ ] Update DEVELOPMENT_LOGBOOK.md with correct flows
- [ ] Document RBAC system usage
- [ ] Add project management user guide
- [ ] Document database schema relationships

## 🎓 LESSONS LEARNED

1. **Project Context is King**: Everything flows from projects. Never lose project context.
2. **RBAC from Day 1**: Permission checks are already implemented correctly in repositories.
3. **Schema is Correct**: Database design matches the intended architecture perfectly.
4. **UI/Code Mismatch**: The backend/database is right, some UI flows are wrong.
5. **Parameter Propagation**: Need to pass projectId through entire call chain.

## 🚀 NEXT STEPS

1. Fix task creation (30 mins)
2. Clarify chat creation UI (15 mins)
3. Test all flows with project context (1 hour)
4. Update documentation (30 mins)
5. Verify RBAC in action (30 mins)

Total estimated time: ~2.5 hours for complete fix
