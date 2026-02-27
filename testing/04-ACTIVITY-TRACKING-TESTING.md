# Activity Tracking Testing

**Date**: 2026-02-15
**Status**: SOLID with 3 gaps

## Strengths

- Offline-first: saves to Room immediately, syncs to Supabase as best-effort
- 12 fields tracked: status, priority, assignment, title, description, due date, tags, hours, etc.
- FK constraints correct: actorId has NO FK (migration 9->10), taskId/projectId have NO_ACTION
- Sync queue integration for failed Supabase operations
- ActivityDescriptionGenerator for auto-descriptions

## Gaps Found

### GAP-1: Time Tracking Operations Not Logged (Critical)
- `startTimer()` (line 1391) - NO activity tracked
- `stopTimer()` (line 1408) - NO activity tracked
- `addManualTimeEntry()` (line 1431) - NO activity tracked
- `deleteTimeEntry()` (line 1466) - NO activity tracked
- **Impact**: Users log time, actualHours changes, but activity log shows nothing
- **Fix**: Add `trackActivity(ActivityActionType.TIME_LOGGED, ...)` to each

### GAP-2: Sync-Driven Updates Not Logged
- `syncProjectTasks()` (line 225) - NO activity created
- `syncTasksForChatRoom()` (line 841) - NO activity created
- **Impact**: Remote edits don't appear in activity log after sync
- **Acceptable** for initial sync, but remote user edits should create activity

### GAP-3: Activity Duplicate Detection
- No unique constraint on (taskId, actorId, timestamp, actionType)
- If sync retries, same activity could be inserted twice
- **Fix**: Add unique check in DAO or Supabase constraint

## Supabase Verification

- `task_activity` table has RLS enabled with project membership policies
- 16 existing activity records verified in Supabase
- Sync queue properly enqueues failed activity inserts
