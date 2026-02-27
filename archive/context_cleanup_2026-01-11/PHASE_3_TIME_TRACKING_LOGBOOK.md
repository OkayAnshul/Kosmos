# Phase 3: Advanced Time Tracking Implementation Logbook

**Started:** 2026-01-01
**Status:** 🚧 In Progress
**Plan Reference:** `/home/anshul/.claude/plans/wobbly-zooming-stardust.md`

---

## Overview

Implementing advanced time tracking with:
- Active timer service (start/stop tracking)
- Time entry history with manual entries
- Billable hours calculation
- Budget tracking (estimated vs. actual)
- Auto-update task actualHours field

**Goal:** Comprehensive time tracking that auto-syncs with tasks.

---

## Phase 3 Tasks Breakdown

### ✅ Completed Tasks
- [x] Created implementation logbook document
- [x] Created TIME_TRACKING_MIGRATION.sql with complete schema
- [x] Created TimeEntry.kt data model with helper methods
- [x] Created TimeEntryDao with comprehensive queries
- [x] Created SupabaseTimeEntryDataSource with CRUD operations

### 🚧 In Progress Tasks
None currently

### ⏳ Pending Tasks

#### 3.5 Time Tracker Service
- [ ] Create `TimeTrackerService` singleton
- [ ] Implement startTimer, stopTimer, addManualEntry
- [ ] Track active timers in StateFlow
- [ ] Auto-update task actualHours on stop
- [ ] Background worker to monitor running timers
- **Files:** `TimeTrackerService.kt` (~250 lines)

#### 3.5 Time Tracker Service
- [ ] Create `TimeTrackerService` singleton
- [ ] Implement startTimer, stopTimer, addManualEntry
- [ ] Track active timers in StateFlow
- [ ] Auto-update task actualHours on stop
- [ ] Background worker to monitor running timers
- **Files:** `TimeTrackerService.kt` (~250 lines)

#### 3.6 UI Components
- [ ] Create `TimeTrackerWidget.kt` - main widget
- [ ] Active timer display with live countdown
- [ ] Time summary cards (tracked, estimated, remaining)
- [ ] Time entries list (last 5)
- [ ] Start/Stop button
- **Files:** `TimeTrackerWidget.kt` (~350 lines)

#### 3.7 Manual Entry Dialog
- [ ] Create `AddManualTimeEntryDialog.kt`
- [ ] Start/end time pickers
- [ ] Duration calculator
- [ ] Description field
- [ ] Billable toggle
- **Files:** `AddManualTimeEntryDialog.kt` (~200 lines)

#### 3.8 Integration
- [ ] Add TimeTrackerWidget to TaskDetailScreen
- [ ] Wire TimeTrackerService to TaskDetailViewModel
- [ ] Auto-start timer when status → IN_PROGRESS (optional)
- [ ] Auto-stop timer when status → DONE (optional)
- [ ] Test time tracking flow end-to-end
- **Files Modified:** TaskDetailScreen.kt, TaskDetailViewModel.kt, KosmosDatabase.kt

---

## Implementation Progress

### Files to Create (7 files)

| File | Status | Lines | Notes |
|------|--------|-------|-------|
| `TIME_TRACKING_MIGRATION.sql` | ✅ Complete | 231 | Supabase migration with RLS, triggers, functions |
| `TimeEntry.kt` | ✅ Complete | 184 | Data model with helper methods |
| `TimeEntryDao.kt` | ✅ Complete | 168 | Room DAO with comprehensive queries |
| `SupabaseTimeEntryDataSource.kt` | ✅ Complete | 256 | Remote data source with CRUD |
| `TimeTrackerService.kt` | ⏳ Pending | ~250 | Singleton service |
| `TimeTrackerWidget.kt` | ⏳ Pending | ~350 | Main UI widget |
| `AddManualTimeEntryDialog.kt` | ⏳ Pending | ~200 | Manual entry UI |

**Total New Files:** 4 created, 3 pending
**Total LOC Created:** 839 lines (pending: ~800 lines)

### Files to Modify (3 files)

| File | Status | Lines Added | Changes |
|------|--------|-------------|---------|
| `TaskRepository.kt` | ⏳ Pending | +50 | Time tracking integration |
| `KosmosDatabase.kt` | ⏳ Pending | +2 | TimeEntry entity + TimeEntryDao |
| `TaskDetailScreen.kt` | ⏳ Pending | +100 | TimeTrackerWidget integration |

**Total Modified Files:** 3
**Total LOC Added:** ~152 lines

---

## Database Schema

### time_entries Table

```sql
CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    start_time BIGINT NOT NULL,
    end_time BIGINT,  -- NULL if running
    duration_seconds INTEGER,

    description TEXT,
    is_billable BOOLEAN DEFAULT true,
    hourly_rate DECIMAL(10, 2),
    is_manual BOOLEAN DEFAULT false,

    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,

    CONSTRAINT valid_time_range CHECK (end_time IS NULL OR end_time > start_time)
);

-- Indexes
CREATE INDEX idx_time_entries_task_id ON time_entries(task_id, start_time DESC);
CREATE INDEX idx_time_entries_user_id ON time_entries(user_id, start_time DESC);
CREATE INDEX idx_time_entries_running ON time_entries(user_id, end_time) WHERE end_time IS NULL;
```

**Status:** ⏳ Not created yet

---

## Key Design Decisions

### 1. Running Timer Tracking
**Pattern:**
- Store running timers in StateFlow (in-memory)
- Persist to Room immediately on start
- Update Room every 30 seconds while running
- Calculate duration on stop

### 2. Auto-Update Task Hours
**When to update:**
- On timer stop → calculate total time for task → update task.actualHours
- On manual entry add → recalculate total
- On entry delete → recalculate total

### 3. Background Monitoring
**Worker:**
- Check for orphaned running timers (app killed)
- Auto-stop timers older than 24 hours
- Sync unsynchronized entries to Supabase

### 4. Billable Hours
**Calculation:**
- Each entry has is_billable flag
- Hourly rate stored per entry (for history)
- Total billable = sum(duration * hourly_rate) where is_billable = true

---

## Next Steps

1. ⏳ Create TIME_TRACKING_MIGRATION.sql
2. ⏳ Create TimeEntry data model
3. ⏳ Create TimeEntryDao
4. ⏳ Create SupabaseTimeEntryDataSource
5. ⏳ Create TimeTrackerService
6. ⏳ Create TimeTrackerWidget UI
7. ⏳ Create AddManualTimeEntryDialog
8. ⏳ Integrate into TaskDetailScreen
9. ⏳ Test time tracking flow

---

**Last Updated:** 2026-01-01
**Current Task:** Phase 3 Setup
**Next Task:** Create TIME_TRACKING_MIGRATION.sql
