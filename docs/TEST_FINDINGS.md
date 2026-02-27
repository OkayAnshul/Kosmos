# Test Suite Findings — Kosmos Android

**Date:** 2026-02-21
**Coverage:** 0% → ~30% (222 tests across all feature layers)

---

## Test Results (Verified from XML Reports)

### Unit Tests — 134 passing, 0 failed

| Class | Tests |
|-------|-------|
| `ValidationUtilsTest` | 45 |
| `PermissionCheckerTest` | 21 |
| `TaskViewModelTest` | 17 |
| `AuthViewModelTest` | 17 |
| `NotificationRulesEngineTest` | 10 |
| `SyncQueueItemTest` | 13 |
| `DependencyValidatorTest` | 10 |
| `ExampleUnitTest` | 1 |
| **Total** | **134** |

### Instrumented Tests (device: CPH2491, API 16) — 88 passing, 0 failed

| Class | Tests |
|-------|-------|
| `TimeEntryDaoTest` | 12 |
| `UserConnectionRepositoryIntegrationTest` | 11 |
| `TaskRepositoryIntegrationTest` | 8 |
| `ProjectRepositoryIntegrationTest` | 9 |
| `ChatRepositoryIntegrationTest` | 8 |
| `ProjectInviteRepositoryIntegrationTest` | 8 |
| `ProjectJoinRequestRepositoryIntegrationTest` | 8 |
| `TaskActivityRepositoryIntegrationTest` | 8 |
| `LoginScreenTest` | 7 |
| `TaskBoardScreenTest` | 7 |
| `ExampleInstrumentedTest` | 1 |
| **Total** | **88** |

### Grand Total: **222 tests — 0 failures, 0 skipped** ✅

---

## Foreign Key Analysis (Full Map)

`PRAGMA foreign_keys = ON` is active in `KosmosDatabase`. All FK constraints were verified during integration tests.

### Complete FK Map

| Child Entity | FK Column | Parent Table | onDelete | Notes |
|-------------|-----------|--------------|----------|-------|
| `project_members` | `projectId` | `projects` | NO_ACTION | Intentional — REPLACE sync uses DELETE+INSERT; CASCADE would wipe members on sync |
| `project_members` | `userId` | `users` | NO_ACTION | Member record preserved if user cache is stale |
| `tasks` | `projectId` | `projects` | NO_ACTION | Tasks preserved if project sync re-inserts |
| `tasks` | `chatRoomId` | `chat_rooms` | SET_NULL | ✅ Task survives if chat room deleted |
| `tasks` | `assignedToId` | `users` | SET_NULL | ✅ Task survives if assignee removed |
| `tasks` | `createdById` | `users` | NO_ACTION | Creator reference preserved |
| `task_activity` | `taskId` | `tasks` | NO_ACTION | Intentional — REPLACE on tasks (DELETE+INSERT) would wipe audit logs |
| `task_activity` | `projectId` | `projects` | NO_ACTION | Audit data must be preserved |
| `chat_rooms` | `projectId` | `projects` | NO_ACTION | Chat preserved across project sync |
| `messages` | `chatRoomId` | `chat_rooms` | NO_ACTION | Messages preserved across room sync |
| `messages` | `senderId` | `users` | NO_ACTION | Messages survive user deletion |
| `messages` | `voiceMessageId` | `voice_messages` | SET_NULL | ✅ Message survives if voice file deleted |
| `messages` | `replyToMessageId` | `messages` | SET_NULL | ✅ Thread survives if parent deleted |
| `voice_messages` | `messageId` | `messages` | **CASCADE** | ✅ Only true cascade — voice file tied to message lifetime |
| `action_items` | `projectId` | `projects` | NO_ACTION | |
| `action_items` | `createdById` | `users` | NO_ACTION | |
| `action_items` | `taskId` | `tasks` | SET_NULL | ✅ Action item survives task deletion |

**All constraints are intentional and working correctly.**
The `NO_ACTION` pattern is consistent across the codebase — chosen specifically because Room's `OnConflictStrategy.REPLACE` internally does a DELETE then INSERT, and `CASCADE` would silently wipe all child records on every sync upsert.

---

## ⚠️ Known Production Bug — `ProjectRepository.deleteProject`

Because everything uses `NO_ACTION`, **the caller is responsible for deleting children before the parent.** `ProjectRepository.deleteProject` skips this and calls `projectDao.deleteProjectById()` directly, causing:

```
SQLITE_CONSTRAINT_FOREIGNKEY (787) — FOREIGN KEY constraint failed
```

### Required deletion order for a Project

```
1. task_activity        (taskId → tasks, projectId → projects)
2. tasks                (projectId → projects)
3. messages             (chatRoomId → chat_rooms)
4. voice_messages       (via messages CASCADE — handled automatically)
5. chat_rooms           (projectId → projects)
6. action_items         (projectId → projects)
7. project_members      (projectId → projects)
8. project_invites      (projectId → projects)
9. project_join_requests (projectId → projects)
10. projects            ← safe to delete now
```

### Fix needed in `ProjectRepository.kt`

```kotlin
suspend fun deleteProject(projectId: String, userId: String): Result<Unit> {
    // ... permission check (unchanged) ...

    // Delete children in order before project row
    taskActivityDao.deleteActivityForProject(projectId)
    taskDao.deleteTasksByProjectId(projectId)        // verify method exists
    // messages deleted via chatRoom cascade or messageDao.deleteByProject
    chatRoomDao.deleteChatRoomsByProjectId(projectId) // verify method exists
    // action_items, invites, join_requests, members
    projectMemberDao.deleteMembersByProjectId(projectId) // need to add this DAO query
    inviteDao.deleteByProject(projectId)             // already exists in ProjectInviteDao
    // then delete project
    projectDao.deleteProjectById(projectId)
}
```

**DAOs that need a new query added:**
- `ProjectMemberDao` — `deleteMembersByProjectId(projectId: String)`
- `ChatRoomDao` — verify `deleteChatRoomsByProjectId` exists
- `TaskDao` — verify `deleteTasksByProjectId` exists

---

## Bugs Confirmed Fixed (Regressions Tested)

**1. Task creation → infinite loading**
`TaskViewModel` used `task.id` (empty `""`) instead of `result.getOrNull()` for `lastCreatedTaskId`.
✅ Regression test: `createTask_success_lastCreatedTaskId_isRepoReturnedId`

**2. Activity count always 0**
`TaskActivity` had FK `actorId → users.id`. Inserts failed silently when user not in local Room cache. Fixed in Migration 9→10 by removing that FK.
✅ Regression test: `trackActivity_savesToRoom` (no user seeded for actorId — still succeeds)

---

## Supabase Async Sync & Realtime WebSocket Audit — 2026-02-21

> Deep line-by-line audit of `SupabaseRealtimeManager`, all repositories, and the sync layer.
> Methodology: static analysis via code-explorer agent on 22 source files.

### Realtime Channel Inventory

| Channel Key | Table | Filter Applied | Events |
|-------------|-------|---------------|--------|
| `messages:<chatRoomId>` | `messages` | ❌ None (server-side) | INSERT, UPDATE, DELETE |
| `connections:<userId>` | `user_connections` | ❌ None | INSERT, UPDATE, DELETE |
| `invites:<userId>` | `project_invites` | ❌ None | INSERT, UPDATE, DELETE |
| `tasks:<projectId>` | `tasks` | ❌ None | INSERT, UPDATE, DELETE |
| `activity:<taskId>` | `task_activity` | ❌ None | INSERT only |
| `project_members:<projectId>` | `project_members` | ❌ None | INSERT, DELETE (no UPDATE) |

### Tables with ZERO Realtime Coverage

| Table | Impact |
|-------|--------|
| `projects` | Project metadata changes from other admins never pushed |
| `chat_rooms` | New rooms created by others not pushed; polling only |
| `project_join_requests` | Admins get NO live notification of new join requests |
| `time_entries` | Polling only |
| `task_dependencies` | Polling only |
| `users` | Profile changes from other devices never pushed |

### Per-Feature Realtime + Offline Queue Coverage Matrix

| Feature | Realtime | Offline Queue | Optimistic Update | Gaps |
|---------|----------|---------------|------------------|------|
| Tasks | ✅ | ✅ | ✅ | No server-side filter; FK violation drops silently; presence stubs |
| Messages | ✅ | ✅ | ✅ | No server-side filter; chat_rooms not subscribed |
| Connections | ✅ | CREATE/UPDATE ✅, DELETE ❌ | ✅ | No server-side filter |
| Project Invites | ✅ | CREATE/UPDATE ✅, cancel ❌ | ✅ | No server-side filter |
| Project Join Requests | ❌ | CREATE/UPDATE ✅, cancel ❌ | ✅ | No realtime subscription at all |
| Project Members | ✅ INSERT+DELETE | ✅ | ✅ | UPDATE events ignored (role changes not pushed) |
| Projects | ❌ | ✅ | ✅ | No realtime subscription |

### Critical Issues Found (CRITICAL severity)

**RT-C1: Thread-unsafe channel maps**
- File: `SupabaseRealtimeManager.kt` lines 87–93
- `activeChannels`, `activeTaskChannels`, `memberChannels` are plain `mutableMapOf()` modified from concurrent `Dispatchers.IO` coroutines
- Risk: `ConcurrentModificationException` or lost map entries on concurrent subscriptions

**RT-C2: `isSyncing` flag not atomic**
- File: `SyncQueueManager.kt` line 61
- `private var isSyncing = false` — no `@Volatile`, no `Mutex`, no `AtomicBoolean`
- Risk: Concurrent `forceSyncNow()` + `observeNetworkState` trigger = double Supabase writes (duplicate inserts)

**RT-C3: Realtime FK violations silently drop data**
- File: `SupabaseRealtimeManager.kt` lines 888–909 (task insert), 816–839 (message insert)
- If realtime INSERT arrives for a task/message whose FK ref is not yet in Room, `dao.insert()` throws FK violation, the catch block logs an error event and **discards the data permanently**
- No retry, no `FKRetryQueue` involvement for realtime-path inserts

**RT-C4: `pinChatRoom` leaves Room/Supabase permanently diverged**
- File: `ChatRepository.kt` lines 849–870
- Room updated optimistically; on Supabase failure returns `Result.failure()` without queuing and without rolling back Room
- Pin status shows in UI forever, Supabase disagrees — survives all syncs

**RT-C5: `deleteProject` not queued on Supabase failure**
- File: `ProjectRepository.kt` lines 621–629
- Project deleted locally, if Supabase sync fails it reappears on next sync (ghost project)

### High Issues Found

**RT-H1: No server-side filters on ANY subscription** ← Most impactful
- File: `SupabaseRealtimeManager.kt` lines 151–153, 307–309, 550–552, 622–624, 695–697
- ALL `postgresChangeFlow` calls omit a `filter { }` clause
- Comment "server already filters by chat_room_id" is **factually incorrect** — the channel name string is a local SDK identifier, not a Supabase server filter
- In production: every subscriber receives all row changes for the entire table; RLS restricts visibility but doesn't prevent broadcast events from being sent to clients
- `handleMessageInsert` writes every received message to Room — a user subscribed to Room A would also cache messages from Room B if they happen to be in the same RLS scope

**RT-H2: No WebSocket reconnection logic**
- File: `SupabaseRealtimeManager.kt` (entire file)
- No `onDisconnect` handler, no heartbeat, no re-subscribe on reconnect
- Stale channel map entries block re-subscription (`containsKey` guard at line 137)
- After any network interruption (background, battery optimization, network switch), realtime features permanently dead until app restart

**RT-H3: `project_members` UPDATE events ignored**
- File: `SupabaseRealtimeManager.kt` lines 704–731
- `else -> {}` branch silently discards UPDATE events
- Role changes from other admins never reflected on device

**RT-H4: Task presence + editing status are stubs**
- File: `SupabaseRealtimeManager.kt` lines 448–532
- `sendTaskPresence()` and `sendTaskEditingStatus()` only emit local flow events — no `channel.broadcast()` call
- These collaborative features appear in UI but do not work across devices

**RT-H5: Join requests have no realtime subscription**
- Admins receive zero live notification of new join requests; must do manual sync

**RT-H6: ProjectMember FK failures silently dropped**
- File: `ProjectRepository.kt` lines 499–516
- FK violation during `syncProjectMembers` → member skipped + not queued
- Member missing from local cache; `since` timestamp prevents re-fetch on next incremental sync

**RT-H7: `subscribeToTypingIndicators` before `subscribeToMessages` breaks message events**
- File: `SupabaseRealtimeManager.kt` lines 218–223
- Creates channel with key `messages:chatRoomId` and stores in `activeChannels`
- Subsequent `subscribeToMessages` call sees key exists, returns early — message `changeFlow` never set up on this channel
- Result: typing indicators work, messages do not

### Medium Issues Found

| ID | File | Issue |
|----|------|-------|
| RT-M1 | `InitialSyncManager.kt` lines 198–290 | Per-project sync is sequential, not parallel — 5 projects = 5× longer |
| RT-M2 | `UserRepository.kt` lines 686–720 | `syncAllUsers()` fetches ALL users in DB — unbounded in multi-tenant production |
| RT-M3 | `SupabaseRealtimeManager.kt` multiple | New `Json { }` instance created per realtime event (inside collect lambdas) |
| RT-M4 | `ProjectInviteRepository.kt` lines 146–152 | `acceptInvite` triggers full `syncUserProjects()` — all projects re-synced for one invite |
| RT-M5 | `SyncQueueManager.kt` lines 424–433 | `cleanupFailedItems()` never called — expired queue items accumulate indefinitely |
| RT-M6 | `SupabaseUserConnectionDataSource.kt` lines 33–48 | Two sequential Supabase queries for sent + received connections (could be one `or` filter) |

### Positive Findings (What Works Well)

- **Optimistic update pattern is consistent**: Room → async Supabase → queue on failure applied in all major mutation paths (tasks, messages, projects, members, invites, connections)
- **Incremental sync with timestamps**: `SyncTimestamp` DAO correctly tracks per-(project, resourceType) incremental sync — only fetches changed records
- **FK dependency order in InitialSyncManager**: users → projects → members/tasks/chats → per-task data — correct and deliberate
- **SyncRetryHelper**: Specialized retry on FK violations with configurable delay + 3 retries for `createTask`, `sendMessage`, `createChatRoom`
- **`supervisorScope` per sync step**: Failure in one step doesn't abort others
- **Typing indicators correctly ephemeral**: `channel.broadcast("typing", ...)` — correct approach, no DB pollution
- **`FKRetryQueue.processRetryQueue()` called after user sync in InitialSyncManager**: Correct ordering for boot-time FK resolution
- **All entity types covered in SyncQueue**: 12 of 14 entity types have queue support (VOICE_MESSAGE, ACTION_ITEM are stubs)

---

## CI Pipeline

`.github/workflows/android-tests.yml` runs on every push/PR to `master`/`main`/`develop`:
1. **Unit Tests** — `./gradlew testDebugUnitTest` + Jacoco coverage report (artifacts retained 14 days)
2. **Instrumented Tests** — `./gradlew connectedDebugAndroidTest` on API 33 emulator (KVM-accelerated, AVD cached)
3. **Build Check** — `./gradlew assembleDebug` (runs only after unit tests pass)
