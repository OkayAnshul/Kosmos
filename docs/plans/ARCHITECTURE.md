# Kosmos Architecture Document

> **Living Document** — Updated: 2026-02-20
> **Purpose:** Senior architect reference, onboarding guide, and audit tracker
> **Scope:** Full-stack Android (Kotlin/Compose) + Supabase PostgreSQL production system

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Layer Responsibilities](#2-layer-responsibilities)
3. [Data Flow](#3-data-flow)
4. [Sync Strategy](#4-sync-strategy)
5. [State Management](#5-state-management)
6. [RBAC & Security Model](#6-rbac--security-model)
7. [Module Audit](#7-module-audit)
8. [Known Issues Tracker](#8-known-issues-tracker)
9. [Improvement Roadmap](#9-improvement-roadmap)

---

## 1. System Overview

### 1.1 Product

Kosmos is a real-time collaborative project management Android application. Core domain model: **Projects → Members → Chat Rooms → Tasks** with role-based access control.

### 1.2 Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9+ |
| UI | Jetpack Compose + Material 3 | Latest |
| Architecture | MVVM + Repository | — |
| DI | Dagger Hilt | 2.51+ |
| Local DB | Room | v12 (16 entities) |
| Remote DB | Supabase PostgreSQL | 17.6.1 |
| Supabase Client | supabase-kt | 3.2.5 |
| Auth | Firebase Auth + Google Sign-In | — |
| Push Notifications | Firebase Cloud Messaging (FCM) | — |
| Real-time | Supabase Realtime (WebSocket) | — |
| File Storage | Supabase Storage | — |
| Min SDK | Android 8.0 (API 26) | — |
| Target SDK | 36 | — |
| Build | Gradle + KSP | — |

### 1.3 Architecture Overview (ASCII)

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID CLIENT                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              UI Layer (Jetpack Compose)                   │  │
│  │  Screens → Wrappers → React-style Components             │  │
│  │  collectAsStateWithLifecycle() for all StateFlow          │  │
│  └─────────────────────┬────────────────────────────────────┘  │
│                         │ events / uiState                      │
│  ┌──────────────────────▼────────────────────────────────────┐  │
│  │           ViewModel Layer (18 ViewModels)                 │  │
│  │  @HiltViewModel, StateFlow<UiState>, Job cancellation     │  │
│  └──────────────────────┬────────────────────────────────────┘  │
│                          │ calls                                 │
│  ┌───────────────────────▼───────────────────────────────────┐  │
│  │        Repository Layer (6 hybrid repositories)           │  │
│  │  Room-first write → async Supabase sync                   │  │
│  └──────┬──────────────────────────────────┬─────────────────┘  │
│         │ Flow<Entity>                      │ suspend fun        │
│  ┌──────▼──────────┐              ┌─────────▼───────────────┐   │
│  │   Room DB (v12)  │              │  Supabase Data Sources  │   │
│  │  16 entities     │              │  8 sources + Realtime   │   │
│  │  16 DAOs         │◄─────────── │  WebSocket subscriptions│   │
│  └──────────────────┘  upsert     └─────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Sync Layer                                   │   │
│  │  InitialSyncManager + SyncQueueManager + FKRetryQueue    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │ HTTPS + WebSocket
┌─────────────────────────────▼───────────────────────────────────┐
│                      SUPABASE (Remote)                          │
│                                                                 │
│  PostgreSQL 17 │ RLS Policies │ Edge Functions │ Realtime       │
│  14 tables     │ all enabled  │ (notifications) │ (WebSocket)   │
│                                                                 │
│  Storage: Supabase Buckets (voice, images, files)               │
└─────────────────────────────────────────────────────────────────┘
```

### 1.4 Supabase Schema Summary (14 Tables)

All tables have Row Level Security (RLS) **enabled**. Timestamps are `bigint` epoch-milliseconds unless noted.

| Table | Rows | Purpose | Notes |
|-------|------|---------|-------|
| `users` | 6 | User profiles, settings JSONB, FCM token | FK root — must sync first |
| `projects` | 35 | Projects with metadata, cached counts, version | Optimistic locking |
| `project_members` | 14 | Membership with role (ADMIN/MANAGER/MEMBER) | Unique (project_id, user_id) |
| `chat_rooms` | 2 | Chat rooms per project, types, last message cache | — |
| `messages` | 7 | Chat messages with reactions JSONB, read_by array | Has `reply_to_id` AND `reply_to_message_id` (duplicate) |
| `tasks` | 12 | Tasks with comments JSONB, version, dependencies | Optimistic locking |
| `task_comments` | **0** | Structured comments (UNUSED — app uses tasks.comments JSONB) | Dead schema |
| `task_activity` | 21 | Audit log with actor_id FK → users.id | FK exists in Supabase, removed from Room (migration 9→10) |
| `time_entries` | 0 | Time tracking per task | ⚠️ `created_at`/`updated_at` are `timestamptz` not `bigint` |
| `task_dependencies` | 0 | Task dependency graph | ⚠️ `created_at` is `timestamptz` not `bigint` |
| `notifications` | 2 | Push notification records | ⚠️ INSERT RLS always-true (security issue) |
| `project_invites` | 2 | Project invite workflow (PENDING→ACCEPTED/DECLINED/EXPIRED) | — |
| `project_join_requests` | 0 | Public project join workflow | — |
| `user_connections` | 1 | Social graph (friend requests, PENDING/ACCEPTED) | — |

**Views:**
- `users_public` — exposes safe user fields; ⚠️ defined as `SECURITY DEFINER` (security issue)

### 1.5 Room DB Summary (v12, 16 Entities)

| Entity | Table | Added in Version |
|--------|-------|-----------------|
| User | users | v1 |
| ChatRoom | chat_rooms | v1 |
| Message | messages | v1 |
| VoiceMessage | voice_messages | v1 |
| Task | tasks | v1 |
| ActionItem | action_items | v1 |
| Project | projects | v1 |
| ProjectMember | project_members | v1 |
| TaskActivity | task_activity | v5 |
| SyncQueueItem | sync_queue | v7 |
| SyncTimestamp | sync_timestamps | v9 |
| TimeEntry | time_entries | v11 |
| TaskDependency | task_dependencies | v11 |
| ProjectInvite | project_invites | v12 |
| UserConnection | user_connections | v12 |
| ProjectJoinRequest | project_join_requests | v12 |

**Migration History:** 1→2, 2→3 (empty/safety), 3→4 (project wizard fields), 4→5 (task_activity table, users.version), 5→6 (FK enforcement + indexes), 6→7 (sync_queue), 7→8 (is_pinned, estimated_hours, actual_hours), 8→9 (sync_timestamps), 9→10 (CASCADE→NO_ACTION, remove actorId FK from task_activity), 10→11 (time_entries, task_dependencies), 11→12 (project_invites, user_connections, project_join_requests)

---

## 2. Layer Responsibilities

### 2.1 UI Layer (Jetpack Compose)

**Pattern:** Screen → Wrapper → ViewModel injection

- **Screens (`*Screen.kt`)**: Pure Compose UI, no business logic. Observe `uiState: StateFlow` via `collectAsStateWithLifecycle()` (lifecycle-safe, no background collection).
- **Wrappers (`*Wrapper.kt`)**: Bridge between Navigation and Screen. Inject ViewModel via `hiltViewModel()`, pass callbacks as lambdas.
- **Components**: Reusable Compose components in `shared/ui/` and feature-specific `components/`.

**Naming convention:** Feature screens named `[Feature]ScreenReact.kt` (new design system) vs `[Feature]Screen.kt` (legacy).

**Rule enforced:** No nested `LazyColumn` inside another `LazyColumn` — causes fatal crash ("infinity max height"). Use `Column { forEach { } }` for inner lists. (See MEMORY.md)

### 2.2 ViewModel Layer (18 ViewModels)

All ViewModels follow:
```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    init { loadData() }
    override fun onCleared() { viewModelScope.coroutineContext.cancelChildren() }
}
```

**Known ViewModels:**
- `AuthViewModel` — Login/signup, Google Sign-In
- `ProjectViewModel` — Project CRUD, project list
- `TaskViewModel` — Task CRUD, status updates, RBAC enforcement
- `TaskDetailViewModel` — Task detail, comments loading, activity timeline
- `TaskEditViewModel` — Task edit form, conflict detection
- `ActivityLogViewModel` — Task activity history
- `ChatViewModel` — Chat room messages, real-time, typing
- `ChatListViewModel` — Chat room list with unread counts
- `UserProfileViewModel` — User profile display
- `UserSearchViewModel` — User search/discover
- `InviteMembersViewModel` — Invite flow
- `MembersListViewModel` — Project members management
- `NotificationSettingsViewModel` — Settings (⚠️ save not wired to Supabase)
- `PrivacySettingsViewModel` — Privacy settings (⚠️ save not wired to Supabase)
- `ProjectJoinRequestRepository` — (Repository used as ViewModel in some contexts)
- `ProjectInviteRepository` — Invite management

### 2.3 Repository Layer (6+ Hybrid Repositories)

**Pattern:** Room-first (instant UI) + async Supabase sync (eventual consistency)

| Repository | Room Source | Supabase Source |
|------------|-------------|-----------------|
| `TaskRepository` | TaskDao | SupabaseTaskDataSource + SupabaseTaskActivityDataSource |
| `ChatRepository` | ChatRoomDao + MessageDao | SupabaseChatDataSource + SupabaseMessageDataSource |
| `ProjectRepository` | ProjectDao + ProjectMemberDao | SupabaseProjectDataSource + SupabaseProjectMemberDataSource |
| `UserRepository` | UserDao | SupabaseUserDataSource |
| `UserConnectionRepository` | UserConnectionDao | SupabaseUserConnectionDataSource |
| `ProjectInviteRepository` | ProjectInviteDao | SupabaseProjectInviteDataSource |
| `ProjectJoinRequestRepository` | ProjectJoinRequestDao | SupabaseProjectJoinRequestDataSource |
| `TaskActivityRepository` | TaskActivityDao | SupabaseTaskActivityDataSource |

**RBAC enforcement:** `TaskRepository` checks `Permission` enum before mutations. `PermissionChecker` + `RoleValidator` utilities.

### 2.4 DataSource Layer (8 Supabase Sources)

Direct Supabase PostgreSQL access via supabase-kt:
- `SupabaseTaskDataSource` — Task CRUD, version increment (optimistic locking)
- `SupabaseTaskActivityDataSource` — Activity log CRUD
- `SupabaseChatDataSource` — Chat room CRUD
- `SupabaseMessageDataSource` — Message CRUD, pagination
- `SupabaseProjectDataSource` — Project CRUD
- `SupabaseProjectMemberDataSource` — Member management
- `SupabaseUserDataSource` — User profile, settings
- `SupabaseTimeEntryDataSource` — Time tracking
- `SupabaseDependencyDataSource` — Task dependency graph
- `SupabaseUserConnectionDataSource` — Social connections
- `SupabaseProjectInviteDataSource` — Invite workflow
- `SupabaseProjectJoinRequestDataSource` — Join request workflow

### 2.5 Sync Layer

Three components working in concert:

| Component | Responsibility |
|-----------|---------------|
| `InitialSyncManager` | Project-centric full sync on app start. Incremental via `sync_timestamps`. Mutex prevents concurrent syncs. 30s debounce. |
| `SyncQueueManager` | Processes queued operations for offline retry. Exponential backoff, max 5 retries. |
| `FKRetryQueue` | Handles FK constraint violations during sync (e.g., message arrives before user synced). Retries in correct dependency order. |

### 2.6 Room DB (Local Source of Truth)

- **Source of truth:** UI always reads from Room, never directly from Supabase.
- **Reactivity:** Room DAOs return `Flow<T>` — any write causes UI to recompose automatically.
- **FK Strategy:** All FKs use `ON DELETE NO ACTION` (changed from CASCADE in migration 9→10) to prevent data loss during `OnConflictStrategy.REPLACE` sync operations.
- **TypeConverters:** `Converters`, `UserSettingsConverters`, `FieldChangeListConverter`

### 2.7 Supabase (Remote Source)

- **PostgreSQL 17.6** with RLS on all 14 tables
- **Realtime:** WebSocket subscriptions via `SupabaseRealtimeManager`
- **Auth:** Firebase Auth + Supabase JWT bridge
- **Storage:** Supabase buckets (not yet fully integrated for photo upload)
- **Triggers:** Supabase triggers maintain denormalized counts (`member_count`, `task_count`, etc.) on `projects` table

---

## 3. Data Flow

### 3.1 Write Flow (Optimistic Update)

```
User action (e.g., tap "Create Task")
    ↓
ViewModel.createTask(task)
    ↓
[RBAC check] PermissionChecker.hasPermission(currentUser, EDIT_ANY_TASK / EDIT_OWN_TASKS)
    ↓
Repository.createTask()
    ├── Room.insert(task)            ← IMMEDIATE (emits Flow → UI recomposes)
    │   [optimistic state visible]
    └── [background coroutine]
        ├── NetworkMonitor.isOnline?
        │   ├── YES → SupabaseTaskDataSource.createTask()
        │   │         ├── Success → update Room with server response (UUID, timestamps)
        │   │         └── Failure → SyncQueueManager.enqueue(CREATE, task)
        │   └── NO  → SyncQueueManager.enqueue(CREATE, task)
        └── trackActivity(task, CREATED)
```

**Task ID note (critical):** `Task.id` defaults to `""`. Repository generates UUID if blank. Always use `result.getOrNull()` for the actual created ID (not the original task object) to avoid navigation to `TaskDetail("")`.

### 3.2 Read Flow (Reactive)

```
ViewModel.init()
    ↓
repository.getTasksForProject(projectId): Flow<List<Task>>
    ↓
Room.queryFlow() — cold Flow, emits on any Room write to tasks table
    ↓
ViewModel._uiState.update { it.copy(tasks = tasks) }
    ↓
UI recomposes via collectAsStateWithLifecycle()
```

No polling. No manual refresh. All reads are reactive Room Flows.

### 3.3 Real-time Flow (WebSocket → Room → UI)

```
App start / project open
    ↓
RealtimeManager.subscribeToProject(projectId)
    ↓
Supabase WebSocket channel established
    ├── tasks INSERT   → Room.upsert(task)   → Flow emits → UI
    ├── tasks UPDATE   → Room.upsert(task)   → Flow emits → UI
    ├── messages INSERT → Room.insert(msg)   → Flow emits → UI
    ├── task_activity INSERT → Room.insert() → Flow emits → UI
    ├── user_connections → Room.upsert()     → Flow emits → UI
    └── project_invites  → Room.upsert()     → Flow emits → UI

Broadcast channel (typing indicators):
    realtimeManager.sendTypingIndicator(chatRoomId, userId, isTyping)
    → channel.broadcast("typing", payload)
    → Other clients receive → UI shows typing animation
```

### 3.4 Conflict Detection Flow

```
ViewModel.updateTask(localTask)
    ↓
SupabaseTaskDataSource.updateTask()
    ↓
UPDATE tasks SET version = version + 1, ...
WHERE id = $id AND version = $localVersion   ← optimistic lock check
    ├── Rows affected = 1 → Success
    └── Rows affected = 0 → ConflictException thrown
        ↓
    TaskRepository catches ConflictException
        ↓
    [CURRENT GAP] ConflictResolutionDialog.kt exists but not wired
    → User not notified of conflict, update silently fails
```

---

## 4. Sync Strategy

### 4.1 Initial Sync (App Launch)

**Order (respects FK dependencies):**
1. Sync `users` (root FK dependency for everything)
2. Sync `projects` (user's memberships)
3. For each project (parallel via `async { }` in supervisorScope):
   a. Sync `project_members` (incremental)
   b. Sync `chat_rooms` (incremental)
   c. Sync `tasks` (incremental)
   d. Per chat room: last 50 `messages`
   e. Sync `project_invites`, `user_connections`, `project_join_requests`
4. Update `sync_timestamps` per (project_id, resource_type)

**Debouncing:** `syncMutex` prevents concurrent syncs. 30s minimum between full syncs (`MIN_SYNC_INTERVAL_MS`).

### 4.2 Incremental Sync

The `sync_timestamps` Room table tracks `lastSyncTimestamp` per (projectId, resourceType). On subsequent syncs, only records `WHERE updated_at > lastSyncTimestamp` are fetched. Reduces data transfer by 50–90%.

### 4.3 Offline Sync Queue

`SyncQueueItem` (Room table: `sync_queue`) stores failed operations:
```
{ entityType, entityId, operation(CREATE/UPDATE/DELETE), entityJson, retryCount, maxRetries=5, priority }
```

`SyncQueueManager` processes queue on next network availability. Exponential backoff between retries. Operations expire after `maxRetries = 5`.

### 4.4 FK Retry Queue

`FKRetryQueue` handles ordering violations during sync. Example: message arrives before its chat_room is synced. The message insert is retried after the chat_room sync completes.

### 4.5 Conflict Resolution (Current State)

- **projects** and **tasks** both have a `version: Integer` column (optimistic locking)
- Update query: `WHERE id = $id AND version = $version` — fails if server version is higher
- `ConflictException` is defined and thrown on failure
- **Gap:** `ConflictResolutionDialog.kt` is implemented but not wired into the update flow — conflicts fail silently

---

## 5. State Management

### 5.1 StateFlow Pattern

All 18 ViewModels use the same pattern:
```kotlin
private val _uiState = MutableStateFlow(FooUiState())
val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()
```

UI collects with `collectAsStateWithLifecycle()` (not `collectAsState()`) — lifecycle-aware, stops collection when app is backgrounded.

### 5.2 UiState Design

Sealed class or data class UiState:
```kotlin
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTask: Task? = null
)
```

### 5.3 Threading

`DispatcherProvider` injectable for testability:
- `Dispatchers.IO` for Room and Supabase operations
- `Dispatchers.Main` for UI state updates (automatic via StateFlow)
- `NonCancellable` context for HTTP calls that must complete even on cancellation

### 5.4 Lifecycle & Job Cancellation

ViewModels cancel all coroutines in `onCleared()`. `viewModelScope` used consistently. No raw `GlobalScope` usage.

### 5.5 Debounced Operations

Search fields use 300ms debounce. InitialSyncManager has 30s debounce. Both implemented with `kotlinx.coroutines.flow.debounce`.

---

## 6. RBAC & Security Model

### 6.1 Role Hierarchy

Three roles (enforced at both Android and Supabase RLS level):
```
ADMIN ──► Full access: create/edit/delete any resource, manage members
MANAGER ► Intermediate: edit/create tasks and chats, cannot delete members
MEMBER ──► Limited: edit own tasks, read-only on most resources
```

### 6.2 Android-side RBAC

**Permission enum** (49 values, subset shown):
```kotlin
enum class Permission {
    EDIT_ANY_TASK, EDIT_OWN_TASKS,
    DELETE_ANY_TASK, DELETE_OWN_TASKS,
    CREATE_TASK, ASSIGN_TASK,
    MANAGE_MEMBERS, INVITE_MEMBERS,
    EDIT_PROJECT, DELETE_PROJECT,
    CREATE_CHAT_ROOM, DELETE_CHAT_ROOM,
    // ... 37 more
}
```

**Enforcement points:**
- `TaskRepository`: Checks `EDIT_ANY_TASK` / `EDIT_OWN_TASKS` / `DELETE_ANY_TASK` before mutations
- `ProjectRepository`: Checks `MANAGE_MEMBERS` / `EDIT_PROJECT` before mutations
- `PermissionChecker`: Utility that maps (role, operation) → boolean
- `RoleValidator`: Validates role transitions (can't elevate beyond own role)

### 6.3 Supabase RLS (Second Layer)

All 14 tables have RLS enabled. Policies enforce:
- Users can only read their own data, plus data from projects they're members of
- Members can only write within their project scope
- Role-specific restrictions mirror Android RBAC

### 6.4 Security Issues Found (Supabase Audit)

| Severity | Issue | Detail | Fix |
|----------|-------|--------|-----|
| **ERROR** | `users_public` SECURITY DEFINER view | View executes with creator's permissions, bypassing querying user's RLS. All users can see all user data regardless of their RLS policies. | Drop and recreate with `security_invoker = true` |
| **WARN** | `notifications` INSERT always-true | RLS policy "Authenticated users can insert notifications" has `WITH CHECK (true)` — any authenticated user can insert notifications targeting any user_id | Restrict to `auth.uid() = user_id` |
| **WARN** | Leaked password protection disabled | Supabase Auth not checking HaveIBeenPwned.org for compromised passwords | Enable in Supabase Auth → Security settings |

**Fix SQL for notifications RLS:**
```sql
DROP POLICY "Authenticated users can insert notifications" ON public.notifications;
CREATE POLICY "Service role or self insert" ON public.notifications
  FOR INSERT WITH CHECK (auth.uid() = user_id);
```

**Fix SQL for users_public view:**
```sql
DROP VIEW IF EXISTS public.users_public;
CREATE VIEW public.users_public
  WITH (security_invoker = true) AS
  SELECT id, display_name, username, photo_url, bio, role, location
  FROM public.users;
GRANT SELECT ON public.users_public TO authenticated;
```

### 6.5 Auth Flow

```
User → Google Sign-In → Firebase Auth JWT
    ↓
Firebase JWT → Supabase Auth (JWT bridge)
    ↓
Supabase: auth.uid() available in RLS policies
    ↓
All subsequent calls authenticated with Supabase JWT
```

---

## 7. Module Audit

> Format per module: **Expected → Actual → Gaps → Priority**

---

### Module A: Task Management

**Expected:** Full CRUD, Kanban board status transitions (TODO→IN_PROGRESS→DONE/CANCELLED), RBAC enforcement, offline-first, activity tracking, comments, time entries, dependencies, sub-tasks

**Actual Implementation:**

| Feature | Status | File |
|---------|--------|------|
| Task CRUD + RBAC | ✅ Working | `TaskRepository.kt` |
| Status transitions | ✅ Working | `TaskViewModel.kt` |
| Activity tracking | ✅ Working | `TaskActivityRepository.kt`, `TaskActivityDao` |
| Comments via tasks.comments JSONB | ✅ Wired | `TaskDetailViewModel.kt` |
| Time entries Room + Supabase | ✅ Implemented | `TimeEntryDao`, `SupabaseTimeEntryDataSource` |
| Task dependencies | ✅ Implemented | `TaskDependencyDao`, `DependencyValidator` |
| Sub-tasks (parentTaskId) | ⚠️ Schema only | Not exposed in UI |
| Conflict resolution dialog | ⚠️ UI exists, not wired | `ConflictResolutionDialog.kt` |
| task_comments table | ❌ Unused | 0 rows, app uses tasks.comments JSONB |

**Gaps:**

1. **Dual comments schema**: `tasks.comments` JSONB (in use) + `task_comments` relational table (0 rows, unused). This is a dead schema causing confusion and potential future data split.

2. **time_entries timestamp mismatch**: `time_entries.created_at` and `time_entries.updated_at` in Supabase are `timestamptz` (ISO8601 string), but Room `TimeEntry` entity stores them as `INTEGER` (epoch ms bigint). The `SupabaseTimeEntryDataSource` must parse timestamptz → Long. If not done correctly, parsing will fail or return incorrect timestamps.

3. **task_activity actor_id FK divergence**: Supabase `task_activity` has `actor_id → users.id` FK (live constraint). Room removed this FK in migration 9→10. Activity records inserted from Android will be accepted by Room (no FK check on actorId). However, if current user is not in Supabase `users` table, Supabase INSERT will fail with FK violation. This is the inverse of the Room scenario.

4. **ConflictResolutionDialog not wired**: Version conflict detection throws `ConflictException` but no UI dialog is shown. User sees silent failure.

**Recommended Fixes:**
- **P1**: Wire `ConflictResolutionDialog` to `ConflictException` in `TaskEditViewModel`
- **P1**: Standardize comments: migrate `tasks.comments` JSONB → `task_comments` table (needs migration + data move)
- **P2**: Fix `time_entries` timestamp types in Supabase to `bigint` (consistent with all other tables)
- **P3**: Expose sub-tasks (parentTaskId) in TaskDetail UI

---

### Module B: Real-time Sync

**Expected:** Zero-latency UI updates, no manual refresh needed, bi-directional sync, typing indicators

**Actual Implementation:**

`SupabaseRealtimeManager` subscribes via Supabase Realtime WebSocket channels:

| Subscription | Table | Writes to |
|-------------|-------|----------|
| tasks changes | tasks | TaskDao |
| messages | messages | MessageDao |
| task_activity | task_activity | TaskActivityDao |
| user_connections | user_connections | UserConnectionDao |
| project_invites | project_invites | ProjectInviteDao |
| project_members | project_members | ProjectMemberDao |
| projects | projects | ProjectDao |
| typing broadcast | (broadcast) | UI state only |

**Gaps:**

1. **No WebSocket reconnection logic**: If the WebSocket connection drops (network switch, background, server restart), subscriptions may silently die. No documented reconnection handler or keepalive. `supabase-kt` may auto-reconnect at the SDK level, but this is not verified or explicitly handled in `SupabaseRealtimeManager`.

2. **Filter scoping in subscriptions**: Some `postgresChangeFlow` subscriptions may not filter by `projectId` at the server level. If subscriptions receive all changes for a table globally, cross-project data leakage in memory is possible in multi-project scenarios. Verify each subscription has a `filter` clause matching the current project.

3. **No subscription cleanup on project switch**: When user navigates away from a project, old subscriptions may remain active, consuming bandwidth.

**Recommended Fixes:**
- **P1**: Add explicit WebSocket reconnection handler with exponential backoff in `SupabaseRealtimeManager`
- **P2**: Audit all `postgresChangeFlow` calls to ensure server-side `filter` by `project_id` is set
- **P2**: Unsubscribe old channels when project context changes

---

### Module C: Chat System

**Expected:** Real-time messages, chronological ordering, offline queue, read receipts, message replies, reactions

**Actual Implementation:**

| Feature | Status | Notes |
|---------|--------|-------|
| Real-time messages | ✅ Working | Via RealtimeManager |
| Message ordering | ✅ Working | Ordered by `timestamp` |
| Offline queue | ✅ Working | Via FKRetryQueue + SyncQueue |
| Reactions (JSONB) | ✅ Schema | UI may not be fully wired |
| Message replies | ⚠️ Dual columns | `reply_to_id` AND `reply_to_message_id` both exist |
| Read receipts | ⚠️ text[] array | `read_by` is `text[]` on messages — no dedicated table, no per-recipient RLS |
| Voice messages | ⚠️ Entity exists | `VoiceMessage` entity and schema, but feature disabled |

**Gaps:**

1. **Duplicate reply-to columns**: `messages.reply_to_id` and `messages.reply_to_message_id` both exist in Supabase. Room entity uses `replyToMessageId`. The `reply_to_id` column is never used (legacy) — causes confusion and wastes storage.

2. **Read receipts via text array**: `read_by: text[]` does not scale for large groups and has no per-recipient access control. Any user can see who read any message. No RLS possible per recipient with this design.

3. **Message deletion**: `is_deleted` and `deleted_at` columns exist in Supabase but soft-delete is not surfaced to the Android app (no `deleted_at` in Room `Message` entity, no "deleted message" placeholder UI).

**Recommended Fixes:**
- **P2**: Drop `messages.reply_to_id` (keep `reply_to_message_id`), update Room entity
- **P3**: For read receipts at scale, move to a dedicated `message_reads` table
- **P3**: Implement soft-delete support (show "Message deleted" placeholder)

---

### Module D: Security (Supabase)

**Expected:** All DB operations scoped to authenticated user and their role. No cross-user data access.

**Actual Implementation:**

| Check | Status |
|-------|--------|
| RLS enabled on all 14 tables | ✅ All enabled |
| users_public view | ❌ SECURITY DEFINER (bypasses RLS) |
| notifications INSERT policy | ❌ Always-true WITH CHECK |
| Leaked password protection | ❌ Disabled |
| Android-side RBAC | ✅ 49-permission enum, enforced in repositories |
| FK constraints (Supabase) | ✅ Enforced server-side |

**Critical Issues (require immediate fix):**

**Issue D-1: users_public SECURITY DEFINER view (ERROR)**
- Any authenticated user who queries `users_public` gets results as if they are the view creator (likely a superuser), bypassing their own RLS restrictions.
- Impact: Users can discover other users outside their project scope.
- Fix: Recreate view with `security_invoker = true`.

**Issue D-2: notifications INSERT always-true (WARN)**
- Any authenticated user can insert a notification with `user_id` set to any other user's ID.
- Impact: Spam/phishing attack vector — malicious user can flood any other user's notification feed.
- Fix: `WITH CHECK (auth.uid() = user_id)` or route all notification writes through a service role Edge Function.

**Issue D-3: Leaked password protection disabled (WARN)**
- Supabase Auth does not check HaveIBeenPwned.org. Users can set compromised passwords.
- Fix: Enable in Supabase Dashboard → Auth → Security.

---

### Module E: User Profiles & Settings

**Expected:** Profile editable with photo, settings persist to Supabase, privacy controls enforced, notification preferences effective

**Actual Implementation:**

| Feature | Status | Notes |
|---------|--------|-------|
| Profile display | ✅ Working | `UserProfileScreen` |
| Profile editing form | ✅ UI exists | `EditProfileScreen` |
| Settings JSONB in Supabase | ✅ Schema | `users.settings` column has full default structure |
| NotificationSettings save | ⚠️ Unknown wiring | `NotificationSettingsViewModel.saveSettings()` — verify if writes to `UserRepository → Supabase` |
| PrivacySettings save | ⚠️ Unknown wiring | Same concern as above |
| Photo upload | ❌ Not implemented | Button exists, Supabase Storage upload code missing |

**Gaps:**

1. **Settings persistence**: The `users.settings` JSONB column exists with a complete default structure (privacy + notifications). If `NotificationSettingsViewModel` and `PrivacySettingsViewModel` only update local state without calling `UserRepository.updateSettings()` → `SupabaseUserDataSource`, settings are lost on app restart.

2. **Photo upload incomplete**: Upload button/UI exists but the `Supabase Storage` upload call is not implemented. This was noted as a P0 known issue.

**Recommended Fixes:**
- **P1**: Verify and wire `saveSettings()` in both settings ViewModels to write to `users.settings` via `UserRepository`
- **P1**: Implement photo upload using `supabase.storage.from("avatars").upload(path, bytes)`

---

### Module F: State Management & Architecture

**Expected:** No stale state, no memory leaks, correct lifecycle handling, no recomposition storms

**Actual Implementation:**

| Practice | Status | Notes |
|----------|--------|-------|
| StateFlow throughout | ✅ Consistent | All 18 ViewModels |
| collectAsStateWithLifecycle() | ✅ Used | Not collectAsState() |
| Job cancellation in onCleared() | ✅ Present | viewModelScope used |
| No nested LazyColumn | ✅ Fixed (historical) | MEMORY.md: replaced with Column + forEach |
| DispatcherProvider | ✅ Injectable | Testability-ready |
| Debounced search | ✅ 300ms | Flow.debounce |
| Debounced sync | ✅ 30s | Mutex + MIN_SYNC_INTERVAL_MS |
| 0% test coverage | ❌ No tests | JUnit + Turbine not set up |

**Note on testability:** All dependencies are injected via Hilt constructor injection. `DispatcherProvider` allows test dispatchers. The architecture is test-ready but zero tests exist.

---

## 8. Known Issues Tracker

### P0 — Critical (Security / Data Integrity)

| ID | Issue | Module | Location | Fix |
|----|-------|--------|----------|-----|
| P0-1 | `users_public` SECURITY DEFINER view | Security | Supabase view | Drop + recreate with `security_invoker = true` |
| P0-2 | `notifications` INSERT RLS always-true | Security | Supabase RLS policy | `WITH CHECK (auth.uid() = user_id)` |
| P0-3 | 0% unit test coverage | All | Entire codebase | Add JUnit5 + Turbine + Hilt test setup |

### P1 — High (User-Facing Bugs / Data Bugs)

| ID | Issue | Module | Location | Fix |
|----|-------|--------|----------|-----|
| P1-1 | Settings don't persist to Supabase (suspected) | Profile | `NotificationSettingsViewModel`, `PrivacySettingsViewModel` | Wire `saveSettings()` → `UserRepository` → `SupabaseUserDataSource` |
| P1-2 | Photo upload not implemented | Profile | Upload button in `EditProfileScreen` | Implement `supabase.storage.from("avatars").upload(...)` |
| P1-3 | ConflictResolutionDialog not wired | Tasks | `TaskEditViewModel`, `ConflictResolutionDialog.kt` | Catch `ConflictException` → show dialog |
| P1-4 | Dual comments schema (JSONB + unused table) | Tasks | `tasks.comments` JSONB + `task_comments` table | Decide on one; migrate data if moving to `task_comments` |
| P1-5 | `time_entries` timestamp type mismatch | Tasks/Sync | `SupabaseTimeEntryDataSource`, Supabase schema | Standardize to `bigint` epoch ms in Supabase (migration needed) |

### P2 — Medium (Quality / Scalability)

| ID | Issue | Module | Location | Fix |
|----|-------|--------|----------|-----|
| P2-1 | WebSocket reconnection not explicitly handled | Realtime | `SupabaseRealtimeManager.kt` | Add reconnect handler with backoff |
| P2-2 | Realtime subscriptions not verified for project-scoped filters | Realtime | `SupabaseRealtimeManager.kt` | Audit and add `filter = "project_id=eq.$projectId"` |
| P2-3 | Leaked password protection disabled | Auth | Supabase Dashboard | Enable in Auth → Security settings |
| P2-4 | `messages.reply_to_id` duplicate column | Chat | Supabase `messages` table | Drop `reply_to_id` (keep `reply_to_message_id`) |
| P2-5 | No input validation on forms | All | All create/edit forms | Add Compose-level validation layer |
| P2-6 | Conflict resolution silent failure | Tasks | `TaskRepository.kt` | Surface `ConflictException` to UI |

### P3 — Low (Future Improvements)

| ID | Issue | Module | Location | Fix |
|----|-------|--------|----------|-----|
| P3-1 | Voice features disabled | Voice | `extras/voice_disabled/` | Restore in future phase |
| P3-2 | Read receipts don't scale | Chat | `messages.read_by` array | Dedicated `message_reads` table |
| P3-3 | Sub-tasks not exposed in UI | Tasks | `tasks.parent_task_id` | Add sub-task UI to TaskDetail |
| P3-4 | Message soft-delete not surfaced | Chat | `messages.is_deleted` | Show "Message deleted" placeholder |
| P3-5 | Subscription cleanup on project switch | Realtime | `SupabaseRealtimeManager.kt` | Unsubscribe old channels on project change |

---

## 9. Improvement Roadmap

### Phase 1: Security Hardening (Immediate)

**Goal:** Eliminate all P0 security issues.

1. **Fix notifications RLS** (30 min)
   ```sql
   DROP POLICY "Authenticated users can insert notifications" ON public.notifications;
   CREATE POLICY "Service role or self insert" ON public.notifications
     FOR INSERT WITH CHECK (auth.uid() = user_id);
   ```

2. **Fix users_public view** (30 min)
   ```sql
   DROP VIEW IF EXISTS public.users_public;
   CREATE VIEW public.users_public
     WITH (security_invoker = true) AS
     SELECT id, display_name, username, photo_url, bio, role, location
     FROM public.users;
   GRANT SELECT ON public.users_public TO authenticated;
   ```

3. **Enable leaked password protection** (5 min)
   - Supabase Dashboard → Auth → Security → Enable HaveIBeenPwned check

4. **Verify after**: Run `get_advisors` → expect 0 ERRORs

### Phase 2: P1 Bug Fixes (Sprint 1)

**Goal:** Fix all user-facing bugs.

1. **Settings persistence** — Trace `saveSettings()` in both settings ViewModels; wire to `UserRepository.updateSettings()` if missing
2. **Photo upload** — Implement `supabase.storage.from("avatars").upload(path, imageBytes)`
3. **Conflict resolution** — Wire `ConflictException` → `ConflictResolutionDialog` in `TaskEditViewModel`
4. **Comments schema** — Decision required: stay with JSONB (simpler) or migrate to `task_comments` (relational, better querying)
5. **time_entries timestamp** — Change `created_at`/`updated_at` in Supabase `time_entries` to `bigint` epoch ms

### Phase 3: Quality & Reliability (Sprint 2)

1. **WebSocket reconnection** — Add reconnection logic with exponential backoff in `SupabaseRealtimeManager`
2. **Subscription scoping** — Audit and fix all `postgresChangeFlow` subscriptions for project-scoped filters
3. **Input validation** — Form validation layer for task creation, project creation, profile edit
4. **Remove duplicate DB columns** — Drop `messages.reply_to_id`

### Phase 4: Test Coverage (Ongoing)

**Goal:** Reach 60% coverage on repositories and ViewModels.

Recommended stack:
- JUnit 5 for unit tests
- Turbine for Flow testing
- Hilt testing for ViewModel injection
- MockK for mocking Supabase data sources
- Room in-memory DB for DAO tests

Priority test targets:
1. `TaskRepository` — RBAC enforcement, optimistic lock, sync queue
2. `InitialSyncManager` — Sync order, FK dependencies
3. `TaskViewModel` — State transitions, error states
4. `PermissionChecker` / `RoleValidator` — All permission combinations

### Phase 5: Future Features

- Voice messaging (code in `extras/voice_disabled/` ready to restore)
- Sub-tasks UI (`parentTaskId` schema already in place)
- Message soft-delete UI
- Read receipts at scale (`message_reads` table)
- Conflict resolution UI (dialog exists, needs wiring)

---

## Appendix: Critical Code Locations

| File | Purpose |
|------|---------|
| `core/database/KosmosDatabase.kt` | Room v12, all 12 migrations |
| `data/sync/InitialSyncManager.kt` | Sync orchestration, incremental sync |
| `data/sync/SyncQueueManager.kt` | Offline retry queue |
| `data/sync/FKRetryQueue.kt` | FK ordering violation retry |
| `data/realtime/SupabaseRealtimeManager.kt` | WebSocket subscriptions |
| `data/repository/TaskRepository.kt` | RBAC + task CRUD (most complex repository) |
| `data/repository/ChatRepository.kt` | Chat + messages |
| `MainActivity.kt` | Navigation graph (25+ routes) |
| `features/tasks/presentation/TaskViewModel.kt` | Primary task ViewModel |
| `features/tasks/presentation/TaskDetailViewModel.kt` | Task detail + comments + activity |
| `core/validators/PermissionChecker.kt` | RBAC enforcement utility |
| `documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql` | Authoritative DB schema |

---

*Last updated: 2026-02-20 | Maintainer: Architecture review session*
*Next review: After P0 security fixes applied and verified*
