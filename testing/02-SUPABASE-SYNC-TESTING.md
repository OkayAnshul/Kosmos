# Supabase Sync Testing

**Date**: 2026-02-15
**Status**: VERIFIED

## Data Integrity Check

All existing data is consistent:
- All task creators are active members of their projects (50/50 checked)
- Project metadata counts (member_count, task_count) match actual counts
- No orphaned records found

## RLS Compatibility Audit (10 Data Sources)

| Data Source | RLS Status | Notes |
|-------------|-----------|-------|
| SupabaseProjectDataSource | SAFE | ownerId set in Repository |
| SupabaseProjectMemberDataSource | SAFE | All queries filter by project_id |
| SupabaseChatDataSource | SAFE | createdBy set in ChatListViewModel |
| SupabaseMessageDataSource | SAFE | senderId set by caller |
| SupabaseTaskDataSource | SAFE | createdById set in Repository |
| SupabaseUserDataSource | SAFE | SELECT uses USING(true), update/delete restricted to own |
| SupabaseTaskActivityDataSource | SAFE | actorId set in Repository |
| SupabaseDependencyDataSource | SAFE | All queries filter by task_id |
| SupabaseMilestoneDataSource | SAFE | All queries filter by project_id |
| SupabaseTimeEntryDataSource | SAFE | userId set, project membership checked |

## Entity Sync Matrix

| Entity | Create | Read | Update | Delete | RLS Works? |
|--------|--------|------|--------|--------|------------|
| User profile | ownerId = auth.uid() | SELECT all (public) | Own only | Own only | YES |
| Project | ownerId set in repo | Members only | Owner/admin | Owner only | YES |
| Project Members | Admin/manager add | Co-members see | Admin update | Admin/self remove | YES |
| Tasks | Members create | Members see | Creator/assignee/admin | Creator/admin | YES |
| Chat Rooms | Members create | Members see | Creator/admin | Creator/admin | YES |
| Messages | sender_id = caller | Room members see | Sender only | Sender/admin | YES |
| Task Activity | Members insert | Members see | N/A | N/A | YES |
| Task Comments | Members add | Members see | Author only | Author only | YES |
| Time Entries | Own user insert | Members see | Own only | Own only | YES |
| Dependencies | Members add | Members see | N/A | Members delete | YES |

## Silent Failure Behavior

Unauthorized operations (e.g., non-admin trying to delete a project) return 0 affected rows.
This is **expected and correct** — the app already checks RBAC in the Repository layer before calling data sources.
RLS acts as a server-side safety net, not primary enforcement.

## Sync Architecture Verification

- Offline-first: Room updated immediately, Supabase synced async
- SyncQueueManager retries failed operations
- FKRetryQueue handles foreign key violations
- Incremental sync via sync_timestamps table
- Real-time via Supabase WebSocket subscriptions
