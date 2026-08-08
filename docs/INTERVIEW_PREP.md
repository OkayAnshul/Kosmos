# Interview Prep

Status: Grounded in current code — every claim below is verifiable against the files cited.

## Purpose
Talking points for technical interviews about Kosmos, organized by the areas most likely to get follow-up questions. Each answer names the actual file/mechanism so it survives "can you show me" or "walk me through the code" follow-ups.

---

## Offline-first sync & conflict resolution

**Q: How does the app stay usable offline, and how do you reconcile changes made while offline?**
Every write goes to Room first, then a queue entry is enqueued for the remote side. `data/sync/SyncQueueManager.kt` drains that queue with exponential backoff and is triggered by `NetworkMonitor` reconnect events. For `UPDATE` operations specifically, the queue doesn't replay the stale JSON payload that was captured at enqueue time — it re-reads the current Room state before sending, so a retry after a long offline period pushes the *latest* local state, not a snapshot from minutes/hours ago.

**Q: What happens when two devices edit the same task while both are offline?**
`core/sync/TaskConflictResolver.kt` does field-level, timestamp-based last-write-wins across the task's editable fields. If the two edits touch disjoint fields (e.g. one changed status, the other changed the description), it auto-merges both — no user prompt. A true conflict (same field, both sides changed) surfaces a `ConflictResolutionDialog` in the UI (wired via `TaskEditScreenReactWrapper`) instead of silently picking a winner.

**Q: Why field-level instead of row-level (whole-record) conflict resolution?**
Row-level LWW would silently drop a valid concurrent edit to an unrelated field just because the other device's write landed a few seconds later. Field-level resolution only forces a user decision when there's an actual semantic conflict.

---

## Realtime architecture

**Q: How is realtime collaboration (typing, presence, live task edits) implemented?**
`data/realtime/SupabaseRealtimeManager.kt` maintains three separate `ConcurrentHashMap`-backed channel pools (`activeChannels`, `activeTaskChannels`, `memberChannels`) keyed by resource ID, layered over Supabase Realtime (Postgres change streams + broadcast channels for typing/presence/task-editing).

**Q: Why three separate pools instead of one generic channel map?**
Keeps subscription lifecycles independent — a chat room's channel and a task's live-edit channel can be opened/closed without interfering with each other, and using `ConcurrentHashMap` avoids a race where two coroutines racing to subscribe the same key clobber each other's channel reference.

**Q: Any bugs you hit and fixed in this layer?**
Yes — at one point the typing-indicator channel and the messages channel were using the same map key (`"messages:$id"`), so the second subscription's duplicate-guard silently no-op'd and typing events never fired. Fixed by giving typing its own key (`"typing:$id"`). Lesson: channel map keys need a namespace per subscription *type*, not just per resource ID.

---

## Data modeling & migrations

**Q: What does the schema look like and how have you evolved it safely?**
17 Room entities, currently at DB version 12 (`core/database/KosmosDatabase.kt:30`), driven by a 12-step migration chain — each migration is additive or a deliberate, documented breaking change (e.g. v11→12 added `project_invites`, `user_connections`, `project_join_requests`).

**Q: Give an example of a migration that fixed a real bug.**
An earlier migration switched several foreign keys from `CASCADE` to `NO_ACTION` to stop a destructive side effect: a `REPLACE`-strategy upsert on a parent row was cascading and silently deleting child rows it shouldn't have touched. Separately, `TaskActivity`'s FK to `users.id` was removed entirely — activity/audit log rows are records of what happened and shouldn't fail to insert (silently, inside a try/catch) just because the actor isn't cached locally yet. SQLite has no `DROP CONSTRAINT`, so both required the standard Room migration pattern: create new table → copy data → drop old → rename.

**Q: What would you flag as technical debt here?**
`docs/DECISIONS.md` D-002: the Kotlin package namespace and Android `applicationId` diverge (`com.example.kosmos` vs `com.aravya.apps.kosmos`) — deliberately deferred rather than risking a mass-rename during release hardening. That's an honest, logged tradeoff, not an oversight.

---

## RBAC & security

**Q: How is authorization enforced?**
Two layers, both real gates (not just UI hints): client-side, a 28-permission / 3-role system enforced via a `PermissionGated` composable + `PermissionChecker` that hides/disables UI the user can't act on; server-side, Supabase Row-Level Security policies on every table, so even a modified client can't bypass authorization.

**Q: How do you handle writes that legitimately need to cross user boundaries** (e.g. inserting a notification for someone else)?
RLS blocks `INSERT` where `user_id != auth.uid()` by design. For the legitimate cross-user case, the fix is a `SECURITY DEFINER` RPC function (`insert_notification`) — a narrow, explicit escape hatch instead of loosening the RLS policy generally. Same pattern for count/audit triggers: they must be `SECURITY DEFINER SET search_path = public`, because `SECURITY INVOKER` silently no-ops when the triggering user's RLS blocks the `UPDATE` on the target table.

---

## Testing & CI

**Q: What's the test coverage story?**
112 passing unit tests plus 11 instrumented/Compose UI test files. Unit tests use MockK for repository mocking (`kotlin-allopen` + `@Singleton` makes repositories subclassable by MockK) and Robolectric where a test touches an Android API like `android.util.Patterns`. An integration test (`TaskRepositoryIntegrationTest`) uses an in-memory Room DB with relaxed MockK for the Supabase-facing dependencies, so it exercises real DAO/query behavior without hitting the network.

**Q: What does CI actually verify?**
A 3-job GitHub Actions pipeline: unit tests + Jacoco coverage, an instrumented build on an API 33 emulator, and a plain build job — plus a release-time gate (`scripts/verify_bundle_signature.sh`) added specifically because `bundleRelease` can succeed while producing an *unsigned* artifact when signing properties are absent.

---

## What would you do differently / what's deferred

Answering this honestly signals engineering judgment more than pretending everything's finished. From `docs/DECISIONS.md` and `docs/CODEBASE_FINDINGS.md`:
- **Deferred features** (D-008): voice recording/playback pipeline, richer task-board drag/drop, an attachments module, a complete blocked-users data model, and an admin management UI — all explicitly scoped out of the internal-testing track rather than half-built.
- **Known hotspots**: a handful of files (`TaskRepository.kt` ~1589 lines, `SupabaseRealtimeManager.kt` ~1230 lines, `MainActivity.kt` ~1124 lines) have grown large enough that they're flagged for a refactor pass before they become a review/regression bottleneck.
- **Namespace debt** (D-002): intentionally not fixed yet — see above.
- **Realtime under reconnect churn**: flagged as a medium production risk; more edge-case testing around offline/online convergence is the next investment, not a hidden gap.

---

*Sources: `docs/DECISIONS.md`, `docs/CODEBASE_FINDINGS.md`, `docs/ARCHITECTURE.md`, and direct inspection of `core/sync/TaskConflictResolver.kt`, `data/sync/SyncQueueManager.kt`, `data/realtime/SupabaseRealtimeManager.kt`, `core/database/KosmosDatabase.kt`.*
