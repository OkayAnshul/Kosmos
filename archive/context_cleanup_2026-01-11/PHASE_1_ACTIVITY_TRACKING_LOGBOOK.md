# Phase 1: Core Activity Tracking Implementation Logbook

**Started:** 2025-12-31
**Status:** 🚧 In Progress
**Plan Reference:** `/home/anshul/.claude/plans/wobbly-zooming-stardust.md`

---

## Overview

Implementing Git-style activity tracking system with complete audit trail, commit messages, and activity timeline UI.

**Goal:** Track all task changes (create, update, status change, assignment, etc.) with before/after values and optional user commit messages.

---

## Phase 1 Tasks Breakdown

### ✅ Completed Tasks
- [x] Created implementation logbook document

### 🚧 In Progress Tasks
None currently

### ⏳ Pending Tasks

#### 1.1 Database Migration (Supabase)
- [ ] Create `task_activity` table with JSONB changes field
- [ ] Add indexes: task_id, project_id, actor_id, timestamp
- [ ] Add CHECK constraint for valid action_type
- [ ] Test migration in Supabase dashboard
- **Files:** `TASK_ACTIVITY_MIGRATION.sql`

#### 1.2 Data Models
- [ ] Create `TaskActivity.kt` entity with Room annotations
- [ ] Create `ActivityActionType` enum (17 actions)
- [ ] Create `FieldChange` data class
- [ ] Create `ActivityDescriptionGenerator` object
- **Files:** `/app/src/main/java/com/example/kosmos/core/models/TaskActivity.kt` (~180 lines)

#### 1.3 Room DAO
- [ ] Create `TaskActivityDao` interface
- [ ] Implement queries: getActivityForTaskFlow, getActivityForProjectFlow, getActivityForUserFlow
- [ ] Implement insert/delete operations
- [ ] Add to KosmosDatabase
- **Files:** `/app/src/main/java/com/example/kosmos/core/database/dao/TaskActivityDao.kt` (~120 lines)

#### 1.4 Supabase Data Source
- [ ] Create `SupabaseTaskActivityDataSource`
- [ ] Implement insertActivity, getActivitiesForTask, getActivitiesForProject
- [ ] Add pagination support (before timestamp)
- [ ] Error handling with Result pattern
- **Files:** `/app/src/main/java/com/example/kosmos/data/datasource/SupabaseTaskActivityDataSource.kt` (~200 lines)

#### 1.5 Repository Integration
- [ ] Inject TaskActivityDao and SupabaseTaskActivityDataSource into TaskRepository
- [ ] Create `trackActivity()` helper method
- [ ] Create `calculateFieldChanges()` diff method
- [ ] Add activity tracking to createTask()
- [ ] Add activity tracking to updateTaskStatus()
- [ ] Add activity tracking to assignTask()
- [ ] Add activity tracking to updateTask()
- [ ] Add activity tracking to deleteTask()
- **Files Modified:** `/app/src/main/java/com/example/kosmos/data/repository/TaskRepository.kt` (+150 lines)

#### 1.6 Activity Timeline UI Component
- [ ] Create `ActivityTimeline.kt` composable
- [ ] Create `ActivityTimelineItem` with avatar, description, timestamp
- [ ] Create `FieldChangesDisplay` for before→after changes
- [ ] Implement "Load more" pagination
- [ ] Add commit message card display
- [ ] Style with Stitch design system
- **Files:** `/app/src/main/java/com/example/kosmos/features/tasks/components/ActivityTimeline.kt` (~300 lines)

#### 1.7 Commit Message Dialog
- [ ] Create `CommitMessageDialog.kt` composable
- [ ] Add changes summary display
- [ ] Add optional commit message text field
- [ ] Add "Don't ask again this session" checkbox
- [ ] Implement confirm/cancel actions
- **Files:** `/app/src/main/java/com/example/kosmos/features/tasks/components/CommitMessageDialog.kt` (~150 lines)

#### 1.8 Activity Log Screen
- [ ] Create `ActivityLogViewModel` with filtering/search
- [ ] Create `ActivityLogScreen.kt` composable
- [ ] Create `ActivityLogScreenWrapper.kt` for Hilt DI
- [ ] Add filters: task, user, action type
- [ ] Add search by commit message
- [ ] Implement pagination (100 items)
- **Files:**
  - `/app/src/main/java/com/example/kosmos/features/tasks/presentation/ActivityLogScreen.kt` (~250 lines)
  - `/app/src/main/java/com/example/kosmos/features/tasks/presentation/ActivityLogViewModel.kt` (~150 lines)
  - `/app/src/main/java/com/example/kosmos/features/tasks/presentation/ActivityLogScreenWrapper.kt` (~100 lines)

#### 1.9 Integration Points
- [ ] Add activity timeline section to TaskDetailScreen (last 5 entries)
- [ ] Add "View full history" button → navigate to ActivityLogScreen
- [ ] Add ActivityLog route to MainActivity
- [ ] Wire commit message dialog to status/assignment changes
- **Files Modified:**
  - `/app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreen.kt` (+80 lines)
  - `/app/src/main/java/com/example/kosmos/MainActivity.kt` (+20 lines)

#### 1.10 Testing & Verification
- [ ] Test activity tracking on task creation
- [ ] Test activity tracking on status change with commit message
- [ ] Test activity tracking on assignment change
- [ ] Test activity timeline display in TaskDetailScreen
- [ ] Test full ActivityLog screen with filters
- [ ] Test offline sync (create activity offline, sync when online)
- [ ] Verify JSONB changes field stores correct before/after values
- [ ] Test pagination in activity timeline

---

## Implementation Progress

### Files to Create (8 files)

| File | Status | Lines | Notes |
|------|--------|-------|-------|
| `TASK_ACTIVITY_MIGRATION.sql` | ✅ Complete | 180 | Supabase migration with RLS policies |
| `TaskActivity.kt` | ✅ Complete | 240 | Core data model + enums + generator |
| `FieldChangeListConverter.kt` | ✅ Complete | 35 | Room TypeConverter for JSONB |
| `TaskActivityDao.kt` | ✅ Complete | 190 | Room DAO with all queries |
| `SupabaseTaskActivityDataSource.kt` | ✅ Complete | 380 | Remote data source with pagination |
| `ActivityTimeline.kt` | ✅ Complete | 430 | Timeline UI component with pagination |
| `CommitMessageDialog.kt` | ✅ Complete | 320 | Commit dialog with changes summary |
| `ActivityLogScreen.kt` | ✅ Complete | 540 | Activity log screen with filtering |
| `ActivityLogViewModel.kt` | ✅ Complete | 220 | ViewModel with reactive filtering |
| `ActivityLogScreenWrapper.kt` | ✅ Complete | 45 | Hilt wrapper |

**Total New Files:** 10 (including converters)
**Total New LOC:** ~2,600 lines

### Files to Modify (4 files)

| File | Status | Lines Added | Changes |
|------|--------|-------------|---------|
| `TaskRepository.kt` | ✅ Complete | +285 | Activity tracking integration, helper methods |
| `MainActivity.kt` | ✅ Complete | +15 | ActivityLog route + Screen object |
| `KosmosDatabase.kt` | ✅ Complete | +4 | TaskActivity entity + TaskActivityDao + TypeConverter |
| `IconSet.kt` | ✅ Complete | +2 | Added arrowForward and arrowBack icons |

**Total Modified Files:** 4
**Total LOC Added:** ~306 lines

---

## Database Schema

### task_activity Table

```sql
CREATE TABLE task_activity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_name TEXT NOT NULL,
    actor_role TEXT,

    action_type TEXT NOT NULL,
    timestamp BIGINT NOT NULL,

    changes JSONB,
    commit_message TEXT,
    auto_description TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT valid_action_type CHECK (action_type IN (
        'created', 'updated', 'status_changed', 'priority_changed',
        'assigned', 'unassigned', 'description_changed', 'due_date_changed',
        'tags_updated', 'comment_added', 'time_logged', 'dependency_added',
        'dependency_removed', 'subtask_added', 'archived', 'restored'
    ))
);

-- Indexes
CREATE INDEX idx_task_activity_task_id ON task_activity(task_id, timestamp DESC);
CREATE INDEX idx_task_activity_project_id ON task_activity(project_id, timestamp DESC);
CREATE INDEX idx_task_activity_actor_id ON task_activity(actor_id, timestamp DESC);
CREATE INDEX idx_task_activity_timestamp ON task_activity(timestamp DESC);
```

**Status:** ⏳ Not created yet

---

## Key Design Decisions

### 1. JSONB for Changes Field
**Rationale:** Flexible schema for field-level changes without migrations when adding new trackable fields.

**Structure:**
```json
[
  {
    "field": "status",
    "fromValue": "TODO",
    "toValue": "IN_PROGRESS",
    "displayFrom": "To Do",
    "displayTo": "In Progress"
  }
]
```

### 2. Offline-First Activity Tracking
**Pattern:**
1. Save to Room immediately (instant UI update)
2. Sync to Supabase in background
3. Continue on failure (activity is safe locally)

### 3. Optional Commit Messages
**When to prompt:**
- Status changes
- Assignment changes
- Major edits (title, description, due date)

**When to skip:**
- Minor edits (tags, priority)
- System actions (auto-assignments)

### 4. Auto-Description Generation
**Examples:**
- `"created this task"`
- `"changed status from To Do to In Progress"`
- `"assigned to John Doe"`
- `"updated description"`

---

## Testing Checklist

### Unit Tests
- [ ] ActivityDescriptionGenerator.generate() for all action types
- [ ] calculateFieldChanges() detects all field changes correctly
- [ ] FieldChange data class serialization/deserialization
- [ ] ActivityActionType enum coverage

### Integration Tests
- [ ] TaskActivityDao queries return correct results
- [ ] SupabaseTaskActivityDataSource syncs to Supabase
- [ ] TaskRepository.trackActivity() creates activity entries
- [ ] Offline sync works (Room → Supabase when online)

### UI Tests
- [ ] ActivityTimeline renders correctly with 5 entries
- [ ] CommitMessageDialog shows/hides correctly
- [ ] ActivityLogScreen filters work
- [ ] Pagination loads more activities

### Manual Testing
- [ ] Create task → activity logged
- [ ] Change status with commit message → both stored
- [ ] Assign task → activity shows old/new assignee
- [ ] Edit multiple fields → all changes tracked
- [ ] View activity timeline in TaskDetailScreen
- [ ] Navigate to full ActivityLog screen
- [ ] Filter by user/action type
- [ ] Test offline mode → sync when online

---

## Known Issues & Blockers

None currently.

---

## Notes & Observations

- **TypeConverter needed:** Room will need a TypeConverter for `List<FieldChange>` to JSON
- **Supabase JSONB queries:** May need to add GIN index on `changes` field if we filter by specific field changes
- **Performance:** With many activities, consider pagination limit and lazy loading
- **Real-time updates:** Phase 2 will add real-time activity subscriptions

---

## Next Steps

1. ✅ Create this logbook document
2. ⏳ Create database migration for task_activity table
3. ⏳ Implement TaskActivity data model
4. ⏳ Create Room DAO and Supabase data source
5. ⏳ Integrate activity tracking into TaskRepository
6. ⏳ Build UI components (timeline, dialog, log screen)
7. ⏳ Wire everything together in MainActivity
8. ⏳ Test thoroughly

---

## Success Criteria for Phase 1

✅ **Phase 1 Complete When:**
- [ ] All task CRUD operations tracked in task_activity table
- [ ] Activity timeline displays in TaskDetailScreen (last 5 entries)
- [ ] Commit message dialog appears on status/assignment changes
- [ ] ActivityLogScreen navigable with filters working
- [ ] Offline sync verified (activities sync to Supabase when online)
- [ ] All 8 new files created and compiling
- [ ] All 4 modified files updated and compiling
- [ ] Manual testing passes all scenarios

---

**Last Updated:** 2025-12-31
**Current Task:** Phase 1 Implementation (9/10 tasks complete)
**Status:** ✅ Core implementation COMPLETE - Ready for testing
**Next Task:** Phase 1.10 - Testing and verification
