# UI Integration Guide - Activity & Time Tracking

**Date**: January 3, 2026
**Status**: ✅ Components Ready, Integration Steps Documented

---

## Overview

This guide provides step-by-step instructions for integrating the activity tracking and time tracking UI components into the existing TaskDetailScreen.

## What's Already Built

### Backend (✅ Complete)
- `NotificationRulesEngine` - Determines notification recipients and sends notifications
- `ReminderScheduler` - Schedules WorkManager jobs for due date reminders
- `TaskRepository` - Automatically tracks activities and triggers notifications
- `TaskActivityDao` - Room DAO for querying task activities
- `TimeEntryDao` - Room DAO for querying time entries

### UI Components (✅ Complete)
- `ActivityTimeline` - Displays chronological activity log with avatars, changes, and commit messages
- `TimeTrackerWidget` - Shows running timer, time summary, and time entries list
- `ElapsedTimeDisplay` - Live countdown display for running timers
- `AddManualTimeEntryDialog` - Dialog for adding manual time entries

---

## Integration Steps

### Step 1: Update TaskDetailViewModel

**File**: `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt`

#### 1.1 Add Repository Injections

```kotlin
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    // ADD THESE:
    private val timeTrackingService: TimeTrackingService,
    private val activityLogRepository: ActivityLogRepository // Or use taskRepository directly
) : ViewModel() {
```

#### 1.2 Update UI State

```kotlin
data class TaskDetailUiState(
    val currentUserId: String,
    val task: Task? = null,
    val assignedUser: User? = null,
    val availableUsers: List<User> = emptyList(),
    val subtasks: List<Task> = emptyList(),
    val comments: List<TaskComment> = emptyList(), // Existing
    // ADD THESE:
    val activities: List<TaskActivity> = emptyList(),
    val timeEntries: List<TimeEntry> = emptyList(),
    val runningTimer: TimeEntry? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)
```

#### 1.3 Load Activities in `loadTask()`

```kotlin
fun loadTask(taskId: String) {
    viewModelScope.launch {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Existing task loading code...
            taskRepository.getTaskByIdFlow(taskId).collect { task ->
                // ... existing code ...
            }
        } catch (e: Exception) {
            // ... error handling ...
        }
    }

    // EXISTING: Load subtasks
    viewModelScope.launch {
        taskRepository.getSubtasksFlow(taskId).collect { subtasks ->
            _uiState.update { it.copy(subtasks = subtasks) }
        }
    }

    // ADD: Load activities
    viewModelScope.launch {
        try {
            taskRepository.getActivityForTaskFlow(taskId).collect { activities ->
                _uiState.update { it.copy(activities = activities) }
            }
        } catch (e: Exception) {
            // Non-blocking error
            Log.w("TaskDetailViewModel", "Failed to load activities", e)
        }
    }

    // ADD: Load time entries
    viewModelScope.launch {
        try {
            timeTrackingService.getTimeEntriesForTask(taskId).collect { entries ->
                _uiState.update { it.copy(timeEntries = entries) }
            }
        } catch (e: Exception) {
            Log.w("TaskDetailViewModel", "Failed to load time entries", e)
        }
    }

    // ADD: Load running timer
    viewModelScope.launch {
        try {
            timeTrackingService.getRunningTimerForTask(taskId).collect { timer ->
                _uiState.update { it.copy(runningTimer = timer) }
            }
        } catch (e: Exception) {
            Log.w("TaskDetailViewModel", "Failed to load running timer", e)
        }
    }
}
```

#### 1.4 Add Time Tracking Functions

```kotlin
fun startTimer() {
    viewModelScope.launch {
        val task = _uiState.value.task ?: return@launch
        try {
            val result = timeTrackingService.startTimer(
                taskId = task.id,
                userId = _uiState.value.currentUserId
            )
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to start timer: ${result.exceptionOrNull()?.message}") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to start timer: ${e.message}") }
        }
    }
}

fun stopTimer(commitMessage: String? = null) {
    viewModelScope.launch {
        val timer = _uiState.value.runningTimer ?: return@launch
        try {
            val result = timeTrackingService.stopTimer(
                timerId = timer.id,
                commitMessage = commitMessage
            )
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to stop timer: ${result.exceptionOrNull()?.message}") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to stop timer: ${e.message}") }
        }
    }
}

fun addManualTimeEntry(durationMinutes: Int, description: String?) {
    viewModelScope.launch {
        val task = _uiState.value.task ?: return@launch
        try {
            val result = timeTrackingService.addManualEntry(
                taskId = task.id,
                userId = _uiState.value.currentUserId,
                durationMinutes = durationMinutes,
                description = description
            )
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to add time entry: ${result.exceptionOrNull()?.message}") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add time entry: ${e.message}") }
        }
    }
}
```

#### 1.5 Update Existing Task Modification Functions

Add `commitMessage` parameter to existing functions:

```kotlin
fun updateTaskStatus(status: TaskStatus, commitMessage: String? = null) {
    viewModelScope.launch {
        val task = _uiState.value.task ?: return@launch
        try {
            _uiState.update { it.copy(isUpdating = true) }
            val result = taskRepository.updateTaskStatus(
                taskId = task.id,
                status = status,
                actorId = _uiState.value.currentUserId,
                commitMessage = commitMessage  // ADD THIS
            )
            // ... rest of function
        }
    }
}

fun updatePriority(priority: TaskPriority, commitMessage: String? = null) {
    // Similar pattern - add commitMessage parameter
}

fun updateDescription(description: String, commitMessage: String? = null) {
    // Similar pattern - add commitMessage parameter
}
```

---

### Step 2: Update TaskDetailScreen

**File**: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreen.kt`

#### 2.1 Add Parameters

```kotlin
@Composable
fun TaskDetailScreen(
    task: Task?,
    assignedUser: User?,
    currentUserId: String,
    availableUsers: List<User>,
    subtasks: List<Task>,
    // ADD THESE:
    activities: List<TaskActivity>,
    timeEntries: List<TimeEntry>,
    runningTimer: TimeEntry?,
    isLoading: Boolean,
    isUpdating: Boolean,
    isOffline: Boolean,
    error: String?,
    // Existing callbacks...
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    // ADD THESE:
    onStatusChangeWithMessage: (TaskStatus, String?) -> Unit,
    onPriorityChangeWithMessage: (TaskPriority, String?) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: (String?) -> Unit,
    onAddManualTimeEntry: (Int, String?) -> Unit,
    onViewAllActivities: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
```

#### 2.2 Update TaskDetailContent

Pass new parameters through:

```kotlin
TaskDetailContent(
    task = task,
    assignedUser = assignedUser,
    currentUserId = currentUserId,
    availableUsers = availableUsers,
    subtasks = subtasks,
    // ADD THESE:
    activities = activities,
    timeEntries = timeEntries,
    runningTimer = runningTimer,
    isUpdating = isUpdating,
    isOffline = isOffline,
    // ... other parameters
    onStartTimer = onStartTimer,
    onStopTimer = onStopTimer,
    onAddManualTimeEntry = onAddManualTimeEntry,
    onViewAllActivities = onViewAllActivities
)
```

#### 2.3 Add UI Components to LazyColumn

In the `TaskDetailContent` LazyColumn, add these items **after the Subtasks section** and **before the Comments section**:

```kotlin
// EXISTING: Subtasks section
item {
    SectionHeader(title = "Subtasks (${completedSubtasks}/${subtasks.size})")
    Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
}
// ... subtasks items ...

// ADD: Time Tracker Widget
item {
    Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
    SectionHeader(title = "Time Tracking")
    Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
}

item {
    TimeTrackerWidget(
        task = task,
        runningTimer = runningTimer,
        timeEntries = timeEntries.take(5), // Show last 5
        onStartTimer = onStartTimer,
        onStopTimer = { message ->
            onStopTimer(message)
        },
        onAddManualEntry = {
            // Show AddManualTimeEntryDialog
        },
        onViewAllEntries = {
            // Navigate to TimeEntriesScreen
        },
        modifier = Modifier.fillMaxWidth()
    )
}

// ADD: Activity Timeline
item {
    Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
    SectionHeader(title = "Activity")
    Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
}

item {
    ActivityTimeline(
        activities = activities.take(10), // Show last 10
        onLoadMore = null, // Optional: implement pagination
        hasMore = activities.size > 10,
        isLoading = false,
        modifier = Modifier.fillMaxWidth()
    )

    if (activities.size > 10) {
        Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
        TextButton(
            onClick = onViewAllActivities,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "View full history (${activities.size} activities)",
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.Stitch.primary
            )
        }
    }
}

// EXISTING: Comments section
item {
    Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
    SectionHeader(title = "Comments")
    Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
}
// ... rest of existing content ...
```

---

### Step 3: Update TaskDetailScreenWrapper

**File**: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenWrapper.kt`

#### 3.1 Collect New State

```kotlin
@Composable
fun TaskDetailScreenWrapper(
    taskId: String,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onViewAllActivities: () -> Unit = { /* Navigate to ActivityLogScreen */ }
) {
    val uiState by viewModel.uiState.collectAsState()

    TaskDetailScreen(
        task = uiState.task,
        assignedUser = uiState.assignedUser,
        currentUserId = uiState.currentUserId,
        availableUsers = uiState.availableUsers,
        subtasks = uiState.subtasks,
        // ADD THESE:
        activities = uiState.activities,
        timeEntries = uiState.timeEntries,
        runningTimer = uiState.runningTimer,
        isLoading = uiState.isLoading,
        isUpdating = uiState.isUpdating,
        isOffline = uiState.isOffline,
        error = uiState.error,
        // Callbacks:
        onStatusChange = { status -> viewModel.updateTaskStatus(status) },
        onPriorityChange = { priority -> viewModel.updatePriority(priority) },
        onStatusChangeWithMessage = { status, message ->
            viewModel.updateTaskStatus(status, message)
        },
        onPriorityChangeWithMessage = { priority, message ->
            viewModel.updatePriority(priority, message)
        },
        onStartTimer = { viewModel.startTimer() },
        onStopTimer = { message -> viewModel.stopTimer(message) },
        onAddManualTimeEntry = { duration, description ->
            viewModel.addManualTimeEntry(duration, description)
        },
        onViewAllActivities = onViewAllActivities,
        onNavigateBack = onNavigateBack
    )
}
```

---

### Step 4: Add Commit Message Dialogs

#### 4.1 Create CommitMessageDialog Component

**File**: Already exists at `app/src/main/java/com/example/kosmos/features/tasks/components/CommitMessageDialog.kt`

#### 4.2 Use Dialog When Updating Tasks

In `TaskDetailScreen`, add state and dialog:

```kotlin
@Composable
fun TaskDetailContent(...) {
    // Existing state
    var showUserPicker by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // ADD THESE:
    var showCommitMessageDialog by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<TaskStatus?>(null) }
    var pendingPriorityChange by remember { mutableStateOf<TaskPriority?>(null) }

    // ... LazyColumn content ...

    // ADD: Commit Message Dialog
    if (showCommitMessageDialog) {
        CommitMessageDialog(
            title = "Add Commit Message",
            placeholder = "Describe your changes (optional)...",
            onConfirm = { message ->
                pendingStatusChange?.let { status ->
                    onStatusChangeWithMessage(status, message)
                    pendingStatusChange = null
                }
                pendingPriorityChange?.let { priority ->
                    onPriorityChangeWithMessage(priority, message)
                    pendingPriorityChange = null
                }
                showCommitMessageDialog = false
            },
            onDismiss = {
                pendingStatusChange = null
                pendingPriorityChange = null
                showCommitMessageDialog = false
            }
        )
    }
}
```

#### 4.3 Trigger Dialog on Status/Priority Changes

Replace direct calls with dialog triggers:

```kotlin
// OLD:
StatusDropdown(
    currentStatus = task.status,
    onStatusSelected = { status -> onStatusChange(status) }
)

// NEW:
StatusDropdown(
    currentStatus = task.status,
    onStatusSelected = { status ->
        pendingStatusChange = status
        showCommitMessageDialog = true
    }
)
```

---

## Testing Checklist

### Unit Tests
- [ ] `TaskDetailViewModel.loadActivities()` - loads activities successfully
- [ ] `TaskDetailViewModel.loadTimeEntries()` - loads time entries successfully
- [ ] `TaskDetailViewModel.startTimer()` - creates running timer
- [ ] `TaskDetailViewModel.stopTimer()` - stops timer with duration
- [ ] `TaskDetailViewModel.addManualTimeEntry()` - adds manual entry

### UI Tests
- [ ] ActivityTimeline displays in TaskDetailScreen
- [ ] TimeTrackerWidget displays in TaskDetailScreen
- [ ] Commit message dialog appears on status change
- [ ] Timer starts and stops successfully
- [ ] Manual time entry dialog works
- [ ] Activity log updates in real-time when task changes

### Integration Tests
- [ ] Update task → activity appears in timeline immediately
- [ ] Start timer → widget shows running timer
- [ ] Stop timer → time entry appears in list
- [ ] Add commit message → appears in activity timeline
- [ ] @mention in commit message → user gets notification

---

## Navigation Integration

### Add ActivityLogScreen Route

**File**: Navigation graph (e.g., `NavGraph.kt` or wherever routes are defined)

```kotlin
composable("task/{taskId}/activity") { backStackEntry ->
    val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
    ActivityLogScreenWrapper(
        taskId = taskId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Navigate from TaskDetailScreen

```kotlin
// In TaskDetailScreenWrapper or parent composable
onViewAllActivities = {
    navController.navigate("task/${taskId}/activity")
}
```

---

## Optional Enhancements

### 1. Real-Time Activity Updates

Use Supabase Realtime to update activity log when other users modify the task:

```kotlin
// In TaskDetailViewModel.loadTask()
viewModelScope.launch {
    // Subscribe to task_activities table for this task
    supabaseRealtimeManager.subscribeToTaskActivities(taskId) { newActivity ->
        _uiState.update { state ->
            state.copy(
                activities = (listOf(newActivity) + state.activities).distinctBy { it.id }
            )
        }
    }
}
```

### 2. Task Presence Indicators

Show which users are currently viewing/editing the task:

```kotlin
// Add to TaskDetailScreen header
Row {
    Text("Task Details")
    Spacer(Modifier.weight(1f))
    TaskPresenceIndicator(
        activeUsers = uiState.activeViewers,
        modifier = Modifier.padding(end = 8.dp)
    )
}
```

### 3. Inline Time Tracking

Add quick time log button to toolbar:

```kotlin
// In TaskDetailScreen toolbar actions
IconButton(onClick = { viewModel.quickLogTime(15) }) {
    Icon(Icons.Default.Timer, "Log 15 minutes")
}
```

---

## Performance Considerations

### Pagination

For tasks with hundreds of activities:

```kotlin
// In TaskDetailViewModel
private var activityPage = 0
private val activityPageSize = 20

fun loadMoreActivities() {
    viewModelScope.launch {
        val activities = taskRepository.getActivityForTask(
            taskId = taskId,
            limit = activityPageSize,
            offset = activityPage * activityPageSize
        )
        _uiState.update { state ->
            state.copy(activities = state.activities + activities)
        }
        activityPage++
    }
}
```

### Caching

Activities and time entries are already cached in Room, so they work offline.

---

## Troubleshooting

### Activities Not Showing
1. Check that `trackActivity()` is being called in TaskRepository
2. Verify `getActivityForTaskFlow()` is collecting in ViewModel
3. Check Room database inspector to see if activities are stored

### Timer Not Starting
1. Check WorkManager is initialized
2. Verify `TimeTrackingService` is injected
3. Check logcat for errors in `startTimer()`

### Notifications Not Sending
1. Verify FCM token is stored in `users.fcm_token`
2. Check Supabase Edge Function is deployed
3. Verify `FCM_SERVER_KEY` environment variable is set
4. Check `notification_log` table for errors

---

## Files Modified Summary

### To Implement Full Integration:

**ViewModels:**
- `TaskDetailViewModel.kt` - Add activities, time entries, and timer functions

**Screens:**
- `TaskDetailScreen.kt` - Add ActivityTimeline and TimeTrackerWidget to LazyColumn
- `TaskDetailScreenWrapper.kt` - Pass new state and callbacks

**Navigation:**
- `NavGraph.kt` or similar - Add route to ActivityLogScreen

**Total Estimated Changes:** ~300 lines of code across 4 files

---

## Next Steps

1. **Immediate**: Implement Step 1 (TaskDetailViewModel)
2. **Next**: Implement Step 2 (TaskDetailScreen UI)
3. **Then**: Implement Step 3 (Wrapper integration)
4. **Finally**: Implement Step 4 (Commit message dialogs)

5. **Testing**: Write unit tests for ViewModel functions
6. **Testing**: Manual testing on physical device
7. **Deployment**: Deploy database migrations
8. **Deployment**: Deploy Supabase Edge Function

---

**Status**: Components built ✅ | Integration documented ✅ | Ready for implementation ⏳

**Effort Estimate**: 4-6 hours for full integration + testing

**Risk Level**: Low (all components already built and tested independently)
