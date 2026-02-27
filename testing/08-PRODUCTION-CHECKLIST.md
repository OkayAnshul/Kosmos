# Production Checklist

**Date**: 2026-02-15
**Overall Status**: NOT READY — 3 blockers, 8 warnings

## Security

- [x] RLS enabled on ALL 11 public tables (migration: `enable_rls_all_tables`)
- [x] RLS policies use `(select auth.uid())` for performance (migration: `fix_existing_policies_and_functions`)
- [ ] **MANUAL** Leaked password protection enabled (Supabase Dashboard → Auth → Settings)
- [x] Function search_path fixed (8 functions)
- [x] Notifications INSERT policy restricted to own user

## Data Sync

- [x] User profile CRUD syncs correctly
- [x] Project CRUD syncs correctly
- [x] Project Members CRUD syncs correctly
- [x] Task CRUD syncs correctly
- [x] Chat Room CRUD syncs correctly
- [x] Messages CRUD syncs correctly
- [x] Task Activity auto-tracked correctly (with gaps — see below)
- [ ] Settings persist correctly (P1: notification/privacy settings UI-only)

## Real-time

- [x] Chat messages appear in real-time
- [ ] **BUG** Typing indicators leak memory (BUG-001, BUG-002 — see 03-REALTIME-CHAT-TESTING.md)
- [ ] **BUG** Typing indicator no timeout (BUG-007)
- [x] Message reactions sync (fire-and-forget, no retry — acceptable for now)
- [x] Read receipts update

## RBAC

- [x] ADMIN can manage all
- [x] MANAGER limited correctly
- [x] MEMBER limited correctly
- [x] RLS enforces server-side (all 11 tables)
- [ ] **BLOCKER** ChatRepository has NO RBAC (10% coverage — see 05-MULTI-USER-TESTING.md)
- [ ] **WARNING** TaskRepository missing RBAC on time tracking, comments, dependencies (60% coverage)

## Code Quality

- [ ] **BLOCKER** Silent exception swallowing in time tracking (TaskRepository lines 1400-1528)
- [ ] **BLOCKER** RealtimeManager CoroutineScope never cancelled (memory leak)
- [x] ProGuard rules comprehensive and correct
- [x] No hardcoded secrets in code (keys in build.gradle.kts)
- [ ] **WARNING** SyncQueueManager doesn't classify recoverable vs unrecoverable errors

## Activity Tracking

- [x] Task CRUD operations logged
- [ ] **WARNING** Time tracking operations not logged (GAP-1)
- [ ] **WARNING** No activity duplicate detection (GAP-3)

## Performance

- [x] 17 duplicate indexes cleaned up (migration: `cleanup_duplicate_indexes_and_add_missing`)
- [x] 4 missing FK indexes added
- [x] No WARN/ERROR level performance advisors

## Build

- [x] Debug build compiles (`./gradlew compileDebugKotlin` passes)
- [ ] Release build not tested
- [ ] App crash testing not done (needs device)
- [ ] Offline mode not tested (needs device)

---

## Summary

### Blockers (must fix before release)
1. **ChatRepository RBAC**: Zero permission checks on send/delete/edit messages, create/delete chat rooms
2. **Silent exception swallowing**: Time tracking sync failures lost silently (6 catch blocks)
3. **RealtimeManager scope leak**: CoroutineScope created but never cancelled

### Warnings (should fix)
1. Typing indicator memory leak (BUG-001 + BUG-002)
2. Typing indicator no timeout (BUG-007)
3. SyncQueueManager no error classification
4. Time tracking not logged in activity
5. Activity duplicate detection missing
6. TaskRepository RBAC gaps (comments, time, dependencies)
7. Settings don't persist
8. Leaked password protection not enabled (manual dashboard action)

### Passed
- RLS enabled and policies correct on all 11 tables
- Function search_path secured
- Data sync verified for all entities
- RBAC role hierarchy correct (ADMIN > MANAGER > MEMBER)
- ProGuard rules correct
- Index cleanup complete
- Build compiles
