# Phase 2 Testing Guide

**Date:** November 2, 2025
**Purpose:** Comprehensive manual testing checklist for Phase 2 features
**Features to Test:** Project Stats, MyTasks Cross-Project, Edit Project Dialog

---

## Pre-Testing Setup

### 1. Build and Install
```bash
./gradlew installDebug
```

### 2. Launch the App
```bash
adb shell am start -n com.example.kosmos/.MainActivity
```

### 3. Verify Logcat
```bash
adb logcat -s com.example.kosmos
```

---

## Test Suite 1: Project Stats (ProjectListScreen)

**Feature:** Real-time member count, chat count, task count display

### Test 1.1: Initial Stats Display
**Steps:**
1. Open the app and navigate to ProjectListScreen
2. Observe the project cards

**Expected Results:**
- [ ] Each project card shows member count (not 0)
- [ ] Each project card shows chat count (not 0)
- [ ] Each project card shows task count
- [ ] Completed task count displays correctly
- [ ] Pending task count displays correctly
- [ ] No "0, 0, 0" placeholders visible

**Screenshots to Take:**
- Project list showing real stats

---

### Test 1.2: Stats Accuracy
**Steps:**
1. Note the stats for a specific project
2. Open Supabase dashboard
3. Manually count:
   - Members in `project_members` WHERE `project_id = [your_project]`
   - Chat rooms in `chat_rooms` WHERE `project_id = [your_project]`
   - Tasks in `tasks` WHERE `project_id = [your_project]`

**Expected Results:**
- [ ] Member count matches database
- [ ] Chat count matches database
- [ ] Task count matches database
- [ ] Completed count = tasks WHERE status = 'DONE'
- [ ] Pending count = tasks WHERE status != 'DONE' AND status != 'CANCELLED'

---

### Test 1.3: Pull to Refresh Stats
**Steps:**
1. Navigate to ProjectListScreen
2. Pull down to refresh
3. Wait for refresh to complete

**Expected Results:**
- [ ] Loading indicator appears
- [ ] Stats refresh (check timestamp or data changes)
- [ ] No crash or error messages
- [ ] UI updates smoothly

---

### Test 1.4: Sort by Members
**Steps:**
1. Navigate to ProjectListScreen
2. Tap sort dropdown
3. Select "Sort by Members"

**Expected Results:**
- [ ] Projects reorder by member count (descending)
- [ ] Project with most members appears first
- [ ] Stats remain accurate after sort

---

### Test 1.5: Sort by Tasks
**Steps:**
1. Navigate to ProjectListScreen
2. Tap sort dropdown
3. Select "Sort by Tasks"

**Expected Results:**
- [ ] Projects reorder by task count (descending)
- [ ] Project with most tasks appears first
- [ ] Stats remain accurate after sort

---

### Test 1.6: Real-Time Stats Update
**Steps:**
1. Note stats for a project
2. Add a new member to that project
3. Return to ProjectListScreen
4. Pull to refresh

**Expected Results:**
- [ ] Member count increases by 1
- [ ] Other stats remain accurate
- [ ] Update happens smoothly

**Alternative Test:**
1. Create a new task in a project
2. Refresh ProjectListScreen
3. Task count should increase

---

### Test 1.7: Offline Stats
**Steps:**
1. Load ProjectListScreen (stats should cache)
2. Enable airplane mode
3. Kill and restart the app
4. Navigate to ProjectListScreen

**Expected Results:**
- [ ] Cached stats display (not zeros)
- [ ] No crash due to network error
- [ ] "Offline" indicator may appear (if implemented)
- [ ] Stats match last known values

---

## Test Suite 2: MyTasks Cross-Project View

**Feature:** Show ALL user tasks across ALL projects

### Test 2.1: Initial Load
**Steps:**
1. Navigate to MyTasksScreen
2. Wait for tasks to load

**Expected Results:**
- [ ] Tasks from ALL user projects appear (not just one)
- [ ] Each task shows project name (not "Unknown Project")
- [ ] Task titles display correctly
- [ ] Priority indicators visible
- [ ] Due dates display if set

**How to Verify:**
- Check Supabase: `SELECT * FROM tasks WHERE assigned_to_id = [your_user_id]`
- Count should match what's shown in app

---

### Test 2.2: Project Name Display
**Steps:**
1. Look at tasks in MyTasksScreen
2. Note the project name for each task
3. Verify in Supabase: `SELECT t.*, p.name FROM tasks t JOIN projects p ON t.project_id = p.id WHERE t.assigned_to_id = [your_user_id]`

**Expected Results:**
- [ ] Project names display correctly (match database)
- [ ] No "Unknown Project" labels (unless task has invalid project_id)
- [ ] Project names update if project renamed

---

### Test 2.3: Filter by Status
**Steps:**
1. Navigate to MyTasksScreen
2. Tap filter dropdown
3. Select "TODO" status
4. Observe filtered tasks
5. Repeat for "IN_PROGRESS" and "DONE"

**Expected Results:**
- [ ] Only tasks with selected status appear
- [ ] Tasks from all projects still included
- [ ] Task count updates in UI
- [ ] Filter persists during session

---

### Test 2.4: Filter by Priority
**Steps:**
1. Navigate to MyTasksScreen
2. Tap priority filter
3. Select "HIGH" priority
4. Observe filtered tasks

**Expected Results:**
- [ ] Only high-priority tasks appear
- [ ] Works across all projects
- [ ] Can combine with status filter
- [ ] Correct tasks remain visible

---

### Test 2.5: Sort by Due Date
**Steps:**
1. Navigate to MyTasksScreen
2. Ensure sort is set to "Due Date"
3. Observe task order

**Expected Results:**
- [ ] Tasks with nearest due dates appear first
- [ ] Tasks with no due date appear last
- [ ] Overdue tasks highlighted (if implemented)
- [ ] Sort works across all projects

---

### Test 2.6: Sort by Priority
**Steps:**
1. Navigate to MyTasksScreen
2. Change sort to "Priority"
3. Observe task order

**Expected Results:**
- [ ] HIGH priority tasks appear first
- [ ] MEDIUM priority in middle
- [ ] LOW priority at end
- [ ] Sort works across all projects

---

### Test 2.7: Edit Task
**Steps:**
1. Navigate to MyTasksScreen
2. Tap on a task
3. Tap "Edit" button
4. Change task title or description
5. Save changes

**Expected Results:**
- [ ] Edit dialog opens
- [ ] Current values pre-populate
- [ ] Changes save successfully
- [ ] Task updates in list immediately
- [ ] Changes persist in database

---

### Test 2.8: Delete Task
**Steps:**
1. Navigate to MyTasksScreen
2. Tap on a task
3. Tap "Delete" button
4. Confirm deletion

**Expected Results:**
- [ ] Confirmation dialog appears (if implemented)
- [ ] Task removed from list
- [ ] Task count decreases
- [ ] Deletion syncs to Supabase
- [ ] Task removed from all views

---

### Test 2.9: Change Task Status
**Steps:**
1. Navigate to MyTasksScreen
2. Find a TODO task
3. Tap status dropdown
4. Change to "IN_PROGRESS"
5. Later change to "DONE"

**Expected Results:**
- [ ] Status changes immediately in UI
- [ ] Task moves to correct filter category
- [ ] Change syncs to database
- [ ] Change visible in ProjectTaskScreen
- [ ] Project stats update (completed count)

---

### Test 2.10: Pull to Refresh
**Steps:**
1. Navigate to MyTasksScreen
2. Pull down to refresh
3. Wait for refresh completion

**Expected Results:**
- [ ] Loading indicator appears
- [ ] Tasks reload from all projects
- [ ] New tasks appear if created elsewhere
- [ ] No duplicates in list
- [ ] Smooth UI update

---

### Test 2.11: Cross-Project Verification
**Steps:**
1. Note tasks visible in MyTasksScreen
2. Count tasks from Project A
3. Count tasks from Project B
4. Verify totals match

**Expected Results:**
- [ ] Tasks from Project A present
- [ ] Tasks from Project B present
- [ ] Tasks from Project C present (if exists)
- [ ] Total count = sum of all project tasks
- [ ] No missing projects

**Database Verification:**
```sql
SELECT project_id, COUNT(*)
FROM tasks
WHERE assigned_to_id = [your_user_id]
GROUP BY project_id;
```

---

### Test 2.12: Empty State
**Steps:**
1. Complete or delete all tasks assigned to you
2. Navigate to MyTasksScreen

**Expected Results:**
- [ ] Empty state message appears
- [ ] "No tasks" illustration or text
- [ ] "Create Task" button visible
- [ ] No crash or error

---

## Test Suite 3: Edit Project Dialog

**Feature:** Edit project name and description with validation

### Test 3.1: Open Edit Dialog
**Steps:**
1. Navigate to ProjectListScreen
2. Tap three-dot menu on a project card
3. Tap "Edit Project"

**Expected Results:**
- [ ] Dialog opens with animation
- [ ] Current project name pre-populated
- [ ] Current description pre-populated
- [ ] Name field is focused
- [ ] Save and Cancel buttons visible

---

### Test 3.2: Name Validation - Empty
**Steps:**
1. Open edit dialog
2. Clear the name field (delete all text)
3. Observe validation

**Expected Results:**
- [ ] Error message appears: "Project name cannot be empty"
- [ ] Error text shown below field
- [ ] Save button disabled
- [ ] Text field shows error state (red border)

---

### Test 3.3: Name Validation - Too Short
**Steps:**
1. Open edit dialog
2. Enter only 2 characters (e.g., "AB")
3. Observe validation

**Expected Results:**
- [ ] Error message: "Project name must be at least 3 characters"
- [ ] Save button disabled
- [ ] Error state visible

---

### Test 3.4: Name Validation - Too Long
**Steps:**
1. Open edit dialog
2. Enter 51+ characters
3. Observe validation

**Expected Results:**
- [ ] Error message: "Project name must be less than 50 characters"
- [ ] Save button disabled
- [ ] Error state visible

---

### Test 3.5: Valid Name
**Steps:**
1. Open edit dialog
2. Enter a valid name (3-50 chars)
3. Observe validation

**Expected Results:**
- [ ] No error message
- [ ] Save button enabled
- [ ] Normal field appearance (no red border)

---

### Test 3.6: Save Changes
**Steps:**
1. Open edit dialog
2. Change project name to "Updated Project Name"
3. Change description to "New description"
4. Tap "Save"

**Expected Results:**
- [ ] Loading indicator appears briefly
- [ ] Dialog closes automatically
- [ ] Success message appears (toast/snackbar)
- [ ] Project name updates in list immediately
- [ ] Changes sync to Supabase
- [ ] Changes persist after app restart

**Database Verification:**
```sql
SELECT name, description FROM projects WHERE id = [project_id];
```

---

### Test 3.7: Cancel Changes
**Steps:**
1. Open edit dialog
2. Change project name
3. Tap "Cancel"

**Expected Results:**
- [ ] Dialog closes immediately
- [ ] No changes saved
- [ ] Project name remains unchanged
- [ ] No success/error messages

---

### Test 3.8: Loading State
**Steps:**
1. Open edit dialog
2. Make valid changes
3. Tap "Save"
4. Observe during save operation

**Expected Results:**
- [ ] Loading indicator appears
- [ ] All controls disabled during save
- [ ] Cannot dismiss dialog during save
- [ ] Save/Cancel buttons disabled
- [ ] Progress indicator visible

---

### Test 3.9: Permission Check
**Steps:**
1. Log in as a MEMBER (not ADMIN/MANAGER)
2. Try to edit a project
3. Observe behavior

**Expected Results:**
- [ ] Edit option hidden OR
- [ ] Edit fails with permission error
- [ ] Error message: "You don't have permission to edit this project"
- [ ] Dialog closes or doesn't open

**Note:** This depends on your RBAC implementation in ProjectListScreen

---

### Test 3.10: Network Error Handling
**Steps:**
1. Open edit dialog
2. Enable airplane mode
3. Make changes and tap "Save"

**Expected Results:**
- [ ] Save attempt fails gracefully
- [ ] Error message appears
- [ ] Dialog remains open (user can retry)
- [ ] Changes not lost
- [ ] Can cancel to exit

---

### Test 3.11: Multiple Edit Cycles
**Steps:**
1. Edit project A, save
2. Immediately edit project B, save
3. Edit project A again, save
4. Repeat 5 times

**Expected Results:**
- [ ] All edits save successfully
- [ ] No memory leaks
- [ ] No UI glitches
- [ ] All changes persist
- [ ] No stale data displayed

---

## Test Suite 4: Integration Tests

**Feature:** Verify features work together correctly

### Test 4.1: Stats Update After Edit
**Steps:**
1. Note stats for a project
2. Edit project name
3. Return to ProjectListScreen
4. Verify stats unchanged

**Expected Results:**
- [ ] Stats remain accurate (edit doesn't affect counts)
- [ ] Project name updated in list
- [ ] No data corruption

---

### Test 4.2: MyTasks After Project Edit
**Steps:**
1. Edit a project name
2. Navigate to MyTasksScreen
3. Find tasks from that project

**Expected Results:**
- [ ] Tasks show new project name
- [ ] All tasks still visible
- [ ] No tasks lost

---

### Test 4.3: Stats After Task Creation
**Steps:**
1. Note task count for a project
2. Create a new task in that project
3. Return to ProjectListScreen
4. Pull to refresh

**Expected Results:**
- [ ] Task count increases by 1
- [ ] Pending count increases by 1
- [ ] Other stats unchanged
- [ ] Update happens quickly

---

### Test 4.4: Stats After Task Completion
**Steps:**
1. Note completed/pending counts
2. Complete a pending task (change status to DONE)
3. Refresh ProjectListScreen

**Expected Results:**
- [ ] Total task count unchanged
- [ ] Completed count increases by 1
- [ ] Pending count decreases by 1
- [ ] Completion percentage updates

---

### Test 4.5: Cross-Screen Navigation
**Steps:**
1. ProjectListScreen → MyTasksScreen → Back → Edit Project → Save
2. Repeat several times

**Expected Results:**
- [ ] No crashes
- [ ] Data remains consistent
- [ ] Navigation smooth
- [ ] Back button works correctly

---

## Test Suite 5: Edge Cases

### Test 5.1: Project with Zero Stats
**Steps:**
1. Create a new empty project (no members, chats, or tasks)
2. View in ProjectListScreen

**Expected Results:**
- [ ] Shows "0 members, 0 chats, 0 tasks"
- [ ] No crash
- [ ] Can still edit project
- [ ] Stats update when data added

---

### Test 5.2: User with No Tasks
**Steps:**
1. Create a new user with no assigned tasks
2. Navigate to MyTasksScreen

**Expected Results:**
- [ ] Empty state displays
- [ ] No error messages
- [ ] Can navigate away smoothly

---

### Test 5.3: Very Long Project Name
**Steps:**
1. Edit project with 49 character name
2. Save and view in list

**Expected Results:**
- [ ] Name displays correctly (may truncate with ellipsis)
- [ ] No UI overflow
- [ ] Full name visible in edit dialog

---

### Test 5.4: Special Characters
**Steps:**
1. Edit project name with special chars: "Project™ & Co. (2024)"
2. Save changes

**Expected Results:**
- [ ] Special characters preserved
- [ ] Displays correctly in UI
- [ ] Saves to database correctly
- [ ] No encoding issues

---

### Test 5.5: Concurrent Edits
**Steps:**
1. Open app on Device A and Device B
2. Edit same project on both devices
3. Save changes

**Expected Results:**
- [ ] Both save (last write wins)
- [ ] No data corruption
- [ ] Both devices eventually consistent
- [ ] No crashes

---

## Test Suite 6: Performance Tests

### Test 6.1: Stats Load Time
**Steps:**
1. Clear app cache
2. Open ProjectListScreen with 10+ projects
3. Measure time until stats appear

**Expected Results:**
- [ ] Stats load within 1-2 seconds
- [ ] No noticeable lag
- [ ] Progressive loading acceptable

---

### Test 6.2: MyTasks with Many Tasks
**Steps:**
1. Assign 50+ tasks to yourself
2. Open MyTasksScreen
3. Observe load time and scrolling

**Expected Results:**
- [ ] Loads within 2-3 seconds
- [ ] Smooth scrolling
- [ ] No frame drops
- [ ] Pagination if implemented

---

### Test 6.3: Large Database Query
**Steps:**
1. Have 100+ tasks across 10+ projects
2. Open MyTasksScreen
3. Apply filters and sorts

**Expected Results:**
- [ ] Filtering is instant
- [ ] Sorting is instant
- [ ] No UI freezing
- [ ] Indexes working properly

---

## Bug Reporting Template

If you find any issues, report them using this format:

```
**Bug ID:** BUG-PHASE2-001
**Feature:** [Project Stats / MyTasks / Edit Dialog]
**Severity:** [Critical / High / Medium / Low]
**Device:** [Your device name]
**Android Version:** [Your Android version]

**Steps to Reproduce:**
1.
2.
3.

**Expected Behavior:**


**Actual Behavior:**


**Screenshots/Logs:**
[Paste logcat or attach screenshots]

**Database State:**
[Run relevant SQL query to show data state]
```

---

## Testing Checklist Summary

### Priority 1: Project Stats
- [ ] 1.1 Initial stats display
- [ ] 1.2 Stats accuracy
- [ ] 1.3 Pull to refresh
- [ ] 1.4 Sort by members
- [ ] 1.5 Sort by tasks
- [ ] 1.6 Real-time update
- [ ] 1.7 Offline stats

### Priority 2: MyTasks
- [ ] 2.1 Initial load
- [ ] 2.2 Project names
- [ ] 2.3 Filter by status
- [ ] 2.4 Filter by priority
- [ ] 2.5 Sort by due date
- [ ] 2.6 Sort by priority
- [ ] 2.7 Edit task
- [ ] 2.8 Delete task
- [ ] 2.9 Change status
- [ ] 2.10 Pull to refresh
- [ ] 2.11 Cross-project verification
- [ ] 2.12 Empty state

### Priority 3: Edit Dialog
- [ ] 3.1 Open dialog
- [ ] 3.2 Empty name validation
- [ ] 3.3 Short name validation
- [ ] 3.4 Long name validation
- [ ] 3.5 Valid name
- [ ] 3.6 Save changes
- [ ] 3.7 Cancel changes
- [ ] 3.8 Loading state
- [ ] 3.9 Permission check
- [ ] 3.10 Network error
- [ ] 3.11 Multiple edits

### Integration & Edge Cases
- [ ] 4.1-4.5 Integration tests
- [ ] 5.1-5.5 Edge cases
- [ ] 6.1-6.3 Performance tests

---

## Post-Testing

After completing all tests, provide:

1. **Test Results Summary:**
   - Total tests: ___
   - Passed: ___
   - Failed: ___
   - Blocked: ___

2. **Critical Issues Found:** (if any)

3. **Performance Notes:**

4. **Recommendations:**

---

**Happy Testing!** 🧪

Report your findings and we'll fix any issues before Phase 3.
