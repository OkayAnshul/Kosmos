# Kosmos

Kosmos is an Android collaboration platform that unifies project planning, execution, and communication in one mobile-first system.

Status convention in this README:
- **Verified**: backed by current code/config/scripts in this repository.
- **Planned**: accepted next-step work not fully completed yet.

Date: 2026-03-07

## 1. Product Overview
Kosmos is designed for teams that need project workspaces, task execution, member governance, and conversation context in a single app.

### Verified
- Android-first implementation using Kotlin + Jetpack Compose.
- Core domains implemented in code: auth, projects, members, tasks, chat, notifications, profile/settings, connections/discover.
- Production app identifier: `com.aravya.apps.kosmos`.

### Planned
- Add curated visual demo evidence (screenshots and guided video) in a dedicated documentation-media pass.

## 2. Core USP (Why This Is Different)
### Verified
- **Execution + communication in one context**: project-scoped chat and tasks co-exist by design.
- **Offline-first posture**: Room-backed local state with sync/retry patterns for eventual convergence.
- **Governed collaboration**: role and permission validation in project/member/task flows.
- **Change accountability**: task activity timeline with optional commit-style change notes.

### Planned
- Expand advanced collaboration intelligence and richer cross-feature context surfaces.

## 3. Android Engineering Baseline
### Verified
- `compileSdk=36`, `targetSdk=36`, `minSdk=26`
- `versionCode=1`, `versionName=1.0`
- `applicationId=com.aravya.apps.kosmos`
- `namespace=com.example.kosmos` (transitional technical debt tracked intentionally)
- Stack: Kotlin, Compose, Material3, Hilt, Room, Supabase, Coroutines/Flow

### Build + release gate commands
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

## 4. Architecture And Execution Model
### Verified
- Layered structure under `com.example.kosmos`:
  - `core` (models, DB, validators, config)
  - `data` (datasources, repositories, realtime, sync)
  - `features` (UI + viewmodels by domain)
  - `shared` (design system, reusable components/services)
  - `navigation` (route structure)
- Main runtime flow:
  - UI intent -> ViewModel -> Repository -> local Room write + remote sync -> Flow-driven UI updates
- Realtime flow:
  - Supabase Realtime channels (message, task, typing, project-members, connections/invites)
  - event fanout via `SupabaseRealtimeManager`

### Planned
- Further decomposition of hotspot files (large repositories/screens) into tighter bounded modules.

## 5. Deep Core Feature Coverage

### 5.1 Task System (Deep)
### Verified
- Task lifecycle states and status transitions (`TODO`, `IN_PROGRESS`, `DONE`, `CANCELLED`).
- Task assignment/unassignment with role + permission checks.
- Due date support and overdue-oriented querying.
- Subtask model via `parentTaskId` and parent task picker flow.
- Task tags and structured comments (`TaskComment`) persisted/synced.
- Estimated vs actual hours fields.
- Task dependency model (`task_dependencies`) and circular-dependency checks.
- Commit-style change notes through commit message dialogs and task activity records.

### Planned
- Further polish for dependency title wiring and remaining TODO-marked task UX paths.

### 5.2 Time Tracking
### Verified
- `time_entries` entity and DAO/data-source/repository pipeline.
- Start/stop timers + manual time entry support.
- Auto-updating `actualHours` from time entries.
- Time tracker UI widget and manual-entry dialog.
- Due-date reminder scheduling integration with task operations.

### Planned
- Expand analytics and reporting around tracked hours.

### 5.3 Activity Center
### Verified
- `task_activity` entity with field-change payload support.
- Activity stream/timeline UI (`ActivityTimeline`) with relative-time and commit-note rendering.
- Task and project activity retrieval flows.
- Commit-message search capability in activity repository.

### Planned
- Broader project-level activity aggregation and filtering options.

### 5.4 Role Management And Permissions
### Verified
- Project roles: admin/manager/member model with hierarchy validation.
- Permission checker with default role permissions + optional custom permission override parsing.
- Guarded actions for project edits, member invites/removals, role changes, task create/edit/delete/assign/status.
- Assignment guardrails using role hierarchy.

### Planned
- Continue hardening edge-case validations and UX feedback around denied operations.

### 5.5 Chat And Collaboration
### Verified
- Multiple chatrooms per project (`chat_rooms`, participants).
- Message model and Room/Supabase data paths.
- Participant add/remove operations.
- Chat room pin/archive operations.
- Typing indicator support via realtime broadcast.
- Chat search dialogs and message lookup flows.

### Planned
- Additional optimization for server-side joins currently noted as improvement areas.

### 5.6 Connections, Discover, Invites, Join Requests
### Verified
- Connections domain with accepted/pending request flows.
- Discover screens for users/projects with join-request actions.
- Project invite lifecycle (create/accept/decline/cancel/expire).
- Join request lifecycle and notification hooks.
- Supporting entities: `user_connections`, `project_invites`, `project_join_requests`.

### Planned
- Further UX convergence between discover and connections action surfaces.

### 5.7 Profile + Project Rich Fields
### Verified
- Profile model fields include bio/location/website and related settings surfaces.
- Project model includes extended metadata beyond title/description:
  - category
  - deadline
  - website/github links
  - project motive
  - tech stack
  - tags and industry tags
  - business model/target audience/open-source license
  - member count tracking

### Planned
- Ongoing consistency pass for profile/settings fields with remaining deferred UI pieces.

## 6. Data Layer Depth: Room + Supabase

### 6.1 Room (Local Source of Responsiveness)
### Verified
- `KosmosDatabase` is versioned to **v12** with sequential migrations.
- Major migration milestones include:
  - project extended fields
  - task activity table
  - sync queue + sync timestamps
  - FK behavior hardening (CASCADE -> NO_ACTION)
  - `time_entries` + `task_dependencies`
  - `project_invites` + `user_connections` + `project_join_requests`

### 6.2 Supabase Integration (Remote Convergence)
### Verified
- Supabase client configured with Auth/Postgrest/Realtime/Storage modules.
- Datasources mapped to core tables:
  - `tasks`, `task_activity`, `task_dependencies`, `time_entries`
  - `projects`, `project_members`, `project_invites`, `project_join_requests`
  - `chat_rooms`, `messages`
  - `users`, `user_connections`
- Repository layer performs best-effort remote sync and queues retries on failure paths.

### Planned
- Additional convergence checks and optimization of selective server-side filtering paths.

## 7. Realtime And WebSocket Behavior
### Verified
- Realtime channels and Postgres change flows are managed in `SupabaseRealtimeManager`.
- Covered event domains include:
  - messages
  - task updates
  - task activity
  - typing indicators
  - project members
  - user connections and invites
- Supabase realtime uses websocket-backed channels (as configured in Supabase client stack).

### Planned
- Close remaining TODO-marked subscription hardening paths identified in audits.

## 8. Background Work (WorkManager)
### Verified
- WorkManager-backed scheduling is used for reminders:
  - `ReminderScheduler`
  - `TaskReminderWorker` (`CoroutineWorker`)
- Task repository integrates reminder scheduling/cancellation in task lifecycle operations.

### Planned
- Add richer observability around worker execution and retry outcomes.

## 9. Security And Release Discipline
### Verified
- Local-only secret handling expected for runtime and signing keys.
- `.gitignore` excludes local properties and keystore material.
- Preflight + signed-bundle verification gates exist.

Required local keys:
- Runtime: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- Signing: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

### Planned
- Final legal links/store metadata completion and rollout governance checks.

## 10. Quality Reality
### Verified
- Main Kotlin files: **262**
- Unit test files: **11**
- Android test files: **11**
- Test surface exists, but depth remains lower than total feature breadth.

### Planned
- Increase deterministic e2e smoke and reconnect/conflict/retry coverage.

## 11. Developer Ideation -> Execution Journey
This repository reflects an iterative build approach:
1. Foundation and architecture scaffolding
2. Core data models and DAOs
3. Supabase datasource + repository wiring
4. Auth/chat/project/task feature expansion
5. Role-permission governance and collaboration flows
6. Realtime event systems and sync hardening
7. UX component growth and redesign wrappers
8. Testing/release documentation and production cleanup

This pattern demonstrates deliberate evolution from architecture-first setup to feature maturity and release governance.

## 12. Feature Checkpoint Matrix (Code + History Evidence)
| Capability | Code Evidence | First Commit Checkpoint | Milestone Context |
|---|---|---|---|
| Chat rooms + messages core models | `core/models/ChatRoom.kt`, `core/models/Message.kt` | `f9ab6d0` (2025-07-13) | `milestone/v0.4-auth-chat` |
| Project + permissions model | `core/models/Project.kt`, `core/models/Permission.kt` | `68f978a` (2025-07-16) | `milestone/v0.5-projects` |
| Project member role model | `core/models/ProjectMember.kt` | `e875410` (2025-07-18) | `milestone/v0.5-projects` |
| Task + activity model | `core/models/Task.kt`, `core/models/TaskActivity.kt` | `420cff0` (2025-07-23) | `milestone/v0.6-tasks` |
| Time entry + user core models | `core/models/TimeEntry.kt`, `core/models/User.kt` | `7325f7c` (2025-07-26) | `milestone/v0.6-tasks` |
| Permission/role validation engine | `core/validators/PermissionChecker.kt`, `RoleValidator.kt` | `e4eaadc` (2025-07-31) | `milestone/v0.5-projects` |
| Realtime manager backbone | `data/realtime/SupabaseRealtimeManager.kt` | `de9f061` (2025-08-12) | `milestone/v0.3-supabase` |
| Invite + join-request repositories | `data/repository/ProjectInviteRepository.kt`, `ProjectJoinRequestRepository.kt` | `3f06509` (2025-08-17) | `milestone/v0.5-projects` |
| Project/task repositories (core orchestration) | `data/repository/ProjectRepository.kt`, `TaskRepository.kt` | `988787b` (2025-08-20) | `milestone/v0.6-tasks` |
| User connection repository | `data/repository/UserConnectionRepository.kt` | `7658fca` (2025-08-22) | `milestone/v0.5-projects` |
| Reminder scheduling (WorkManager path) | `features/notifications/ReminderScheduler.kt` | `6a47fda` (2025-09-26) | `milestone/v0.8-android-assets` |
| Task reminder worker | `features/notifications/TaskReminderWorker.kt` | `c451368` (2025-09-29) | `milestone/v0.8-android-assets` |
| Connections experience | `features/connections/presentation/ConnectionsScreen.kt`, `ConnectionsViewModel.kt` | `4f5d118` (2025-09-19) | `milestone/v0.5-projects` |
| Discover experience | `features/discover/presentation/DiscoverScreen.kt`, `DiscoverViewModel.kt` | `67e3965` (2025-09-21) | `milestone/v0.5-projects` |
| Activity timeline UI | `features/tasks/components/ActivityTimeline.kt` | `5d87799` (2025-11-03) | `milestone/v0.6-tasks` |
| Commit-message workflow UI | `features/tasks/components/CommitMessageDialog.kt` | `e6fffcc` (2025-11-05) | `milestone/v0.6-tasks` |
| Task picker/subtask selection UI | `features/tasks/components/TaskPickerBottomSheet.kt` | `8229372` (2025-11-08) | `milestone/v0.6-tasks` |
| Time tracker widget | `features/tasks/components/TimeTrackerWidget.kt` | `66ca128` (2025-11-10) | `milestone/v0.6-tasks` |
| Room database foundation/migration lineage | `core/database/KosmosDatabase.kt` | `b0f143d` (2025-06-21) | `milestone/v0.2-core-data` |
| Production-ready docs + release framing | `docs/*`, release docs | `39b5c9d` + `6d1afff` | `milestone/v0.9-quality-docs`, `milestone/v1.0-release-ready` |

Note: some feature checkpoints are **implicit** in synthetic commit subjects, but explicit in file-level commit introduction and current code evidence.

## 13. Risks And Next Priorities
### Verified risk bands
- **High**: signing completion and signed artifact verification before external distribution.
- **Medium**: realtime/reconnect consistency and large-file maintainability risk.
- **Lower (important)**: remaining deferred UX polish in selected settings/profile paths.

### Planned 6-week focus
- Weeks 1-2: release-signing closure and verified signed artifact path.
- Weeks 3-4: realtime/retry hardening + deterministic conflict tests.
- Weeks 5-6: hotspot modularization + UX/accessibility consistency sweep.

## 14. Documentation Map
- [Production Docs Index](docs/README.md)
- [Project One Sheet](docs/PROJECT_ONE_SHEET.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Codebase Findings](docs/CODEBASE_FINDINGS.md)
- [UI/UX Design](docs/UI_UX_DESIGN.md)
- [Security](docs/SECURITY.md)
- [Testing](docs/TESTING.md)
- [Release Runbook](docs/RELEASE.md)
- [Archive References](docs/ARCHIVE_REFERENCES.md)

## 15. Bottom Line
Kosmos demonstrates full-stack Android collaboration engineering across data modeling, sync/realtime architecture, role-governed workflows, and release operationalization. The remaining work is concentrated in release completion and targeted hardening, not missing product foundation.
