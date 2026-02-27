# Real-Time Sync & Navigation Redesign - Implementation Plan

## Date: 2025-11-09
## Status: APPROVED FOR IMPLEMENTATION

---

## PROBLEM STATEMENTS

### 1. Stats Not Updating in Real-Time
**Current Behavior**: Stats show 0 even after creating chats/tasks/members
**Root Cause**: ProjectViewModel uses one-time `loadProjectStats()` instead of Flow
**Impact**: Users think app is broken

### 2. Navigation UX Issue
**Current Behavior**: Tapping Chat/Task/Members in bottom nav navigates away, losing project context
**User Request**: "Keep NavBar visible while navigating project sections"
**Impact**: Too many back button presses, poor UX

### 3. CreateChatDialog Shows User IDs
**Current Behavior**: Dialog shows "userId123" instead of "John Doe"
**Root Cause**: Not loading user details before showing dialog
**Impact**: Confusing UX

### 4. Vertical Text in TaskScreens
**Current Behavior**: Tags display vertically when Row overflows
**Root Cause**: Missing FlowRow or horizontal scroll
**Impact**: Unreadable UI

---

## SOLUTIONS

### Solution 1: Real-Time Stats Updates (PRIORITY 1)

#### Changes Required

**A. ProjectViewModel.kt** - Switch to Flow-based stats loading

```kotlin
// BEFORE (one-time load):
fun loadProjectStats(projectId: String) {
    viewModelScope.launch {
        val stats = projectRepository.getProjectStats(projectId)
        _uiState.value = _uiState.value.copy(projectStats = updatedStats)
    }
}

// AFTER (real-time flow):
fun loadProjectStatsFlow(projectId: String) {
    viewModelScope.launch {
        projectRepository.getProjectStatsFlow(projectId).collect { stats ->
            val updatedStats = _uiState.value.projectStats.toMutableMap()
            updatedStats[projectId] = stats
            _uiState.value = _uiState.value.copy(projectStats = updatedStats)
        }
    }
}
```

**B. ProjectDetailsScreenWrapper.kt** - Use Flow instead of one-time load

```kotlin
// BEFORE:
LaunchedEffect(projectId) {
    viewModel.loadProjectMembers(projectId)
    viewModel.loadProjectStats(projectId)  // ❌ One-time
}

// AFTER:
LaunchedEffect(projectId) {
    viewModel.loadProjectMembers(projectId)
    viewModel.loadProjectStatsFlow(projectId)  // ✅ Continuous updates
}
```

**Result**: Stats will update automatically when chats/tasks/members change

---

### Solution 2: Persistent Bottom Navigation (PRIORITY 2)

#### Architecture Change

**Current Navigation Structure**:
```
MainActivity
  └── ProjectDetailsScreen
        ├── Taps "Chats" → Navigate to ChatListScreen (loses bottom nav)
        ├── Taps "Tasks" → Navigate to TaskBoardScreen (loses bottom nav)
        └── Taps "Members" → Navigate to MembersListScreen (loses bottom nav)
```

**New Navigation Structure**:
```
MainActivity
  └── ProjectWorkspaceScreen (NEW CONTAINER)
        ├── Bottom Nav (Always Visible)
        └── Content Area
              ├── Tab 0: ProjectDetailsScreen (Overview + Activity)
              ├── Tab 1: ChatListScreen (Chats for this project)
              ├── Tab 2: TaskBoardScreen (Tasks for this project)
              ├── Tab 3: MembersListScreen (Members for this project)
              └── Tab 4: ProjectActivityScreen (Activity feed)
```

#### Implementation Steps

**Step 1**: Create `ProjectWorkspaceScreen.kt`
- Container with persistent bottom nav
- Switches between 5 tabs inline
- No navigation - just tab switching

**Step 2**: Update navigation routes in `MainActivity.kt`
```kotlin
// OLD:
composable("project_details/{projectId}") { ... }
composable("chat_list/{projectId}") { ... }
composable("task_board/{projectId}") { ... }

// NEW:
composable("project_workspace/{projectId}") { backStackEntry ->
    val projectId = backStackEntry.arguments?.getString("projectId")
    ProjectWorkspaceScreen(
        projectId = projectId,
        onChatClick = { chatId -> navController.navigate("chat/$chatId") },
        onTaskClick = { taskId -> navController.navigate("task/$taskId") },
        onBackClick = { navController.popBackStack() }
    )
}
```

**Step 3**: Bottom nav items
- Overview (home icon)
- Chats (chat icon)
- Tasks (checkmark icon)
- Members (people icon)
- Activity (history icon)

**Benefits**:
- ✅ Single tap to switch sections (no navigation)
- ✅ No back button spam
- ✅ Context preserved (always in same project)
- ✅ Modern app UX (like Slack, Discord)

---

### Solution 3: Real User Data in CreateChatDialog (COMPLETED ✅)

**Status**: Already implemented in previous changes
- LaunchedEffect loads users via `viewModel.getUserById()`
- Passes real User objects to dialog
- Shows proper display names

---

### Solution 4: Fix Vertical Text in TaskScreens (PENDING)

#### Changes Required

**File**: `TaskScreens.kt`

**Locations to Fix**:
1. Edit Task Dialog - Tags Row (line ~541)
2. Task Detail Screen - Tags Row (line ~892)
3. Task Card - Tags Row (line ~1323)

**Change**:
```kotlin
// BEFORE:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
) {
    tags.forEach { tag ->
        AssistChip(/* ... */)
    }
}

// AFTER (Option A - FlowRow wraps to new line):
import androidx.compose.foundation.layout.FlowRow

FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
) {
    tags.forEach { tag ->
        AssistChip(/* ... */)
    }
}

// AFTER (Option B - Horizontal scroll):
Row(
    modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
) {
    tags.forEach { tag ->
        AssistChip(/* ... */)
    }
}
```

**Recommendation**: Use FlowRow (Option A) - better UX, all tags visible

---

## COMPREHENSIVE REAL-TIME SYNC STRATEGY

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         UI LAYER                             │
│  (Compose Screens collect Flows, automatic recomposition)   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      VIEWMODEL LAYER                         │
│  (Collects repository Flows, transforms to UI state)        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                          │
│  • Emits Room database Flows (local source of truth)        │
│  • Listens to Supabase Realtime (remote updates)            │
│  • Syncs changes bidirectionally                            │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────┐          ┌──────────────┐
│  ROOM (DB)   │          │   SUPABASE   │
│  Local Cache │◄────────►│  PostgreSQL  │
│  (Offline)   │  Sync    │  (Cloud)     │
└──────────────┘          └──────────────┘
```

### Current State Analysis

#### ✅ What Works
1. **ChatRepository** - Real-time messages via `getMessagesFlow()`
2. **ProjectRepository** - Real-time project list via `getUserProjectsFlow()`
3. **TaskRepository** - Real-time tasks via `getTasksForProjectFlow()`
4. **Metadata Updates** - Counters increment/decrement (we just added this!)

#### ❌ What Doesn't Work
1. **ProjectViewModel** - Uses one-time `loadProjectStats()` instead of Flow
2. **Project Stats** - Not subscribed to Flow, so UI doesn't react to DB changes
3. **Member List** - Manual refresh needed (should auto-update when member added/removed)

### Implementation Plan

#### Phase 1: Fix ProjectViewModel Stats (IMMEDIATE)

**File**: `ProjectViewModel.kt`

**Change**: Replace all `loadProjectStats()` calls with Flow collection

```kotlin
// Add this method:
fun observeProjectStats(projectId: String) {
    viewModelScope.launch {
        projectRepository.getProjectStatsFlow(projectId).collect { stats ->
            val updatedStats = _uiState.value.projectStats.toMutableMap()
            updatedStats[projectId] = stats
            _uiState.value = _uiState.value.copy(projectStats = updatedStats)
        }
    }
}
```

#### Phase 2: Fix MembersListViewModel (IMMEDIATE)

**File**: `MembersListViewModel.kt`

**Change**: Use Flow instead of suspend function

```kotlin
// BEFORE:
fun loadMembers(projectId: String) {
    viewModelScope.launch {
        projectRepository.syncProjectMembers(projectId)
        val members = projectRepository.getProjectMembers(projectId)  // ❌ One-time
        // ...
    }
}

// AFTER:
fun observeMembers(projectId: String) {
    viewModelScope.launch {
        projectRepository.syncProjectMembers(projectId)
        projectRepository.getProjectMembersFlow(projectId).collect { members ->  // ✅ Continuous
            // Transform to UI state with user details
            val membersWithUsers = members.mapNotNull { member ->
                val user = userRepository.getUserById(member.userId)
                if (user != null) MemberWithUser(member, user) else null
            }
            _uiState.update { it.copy(members = membersWithUsers, filteredMembers = membersWithUsers) }
        }
    }
}
```

#### Phase 3: Supabase Realtime Integration (FUTURE)

**Current Status**: Realtime listeners exist but may not be actively updating Room

**Files to Check**:
- `SupabaseRealtimeManager.kt` - Ensure listeners call DAO insert/update
- `ChatRepository.kt` - Verify realtime subscription updates local DB
- `TaskRepository.kt` - Verify realtime subscription updates local DB

**Test**:
1. Open app on Device A
2. Create chat on Device B
3. Device A should see new chat immediately (without manual refresh)

---

## IMPLEMENTATION ORDER

### Immediate (This Session)
1. ✅ Fix CreateChatDialog user data (DONE)
2. 🔄 Switch ProjectViewModel to Flow-based stats
3. 🔄 Switch MembersListViewModel to Flow-based members
4. 🔄 Fix vertical text in TaskScreens with FlowRow

### Next Session
5. Create ProjectWorkspaceScreen with persistent bottom nav
6. Update MainActivity navigation routing
7. Test real-time sync Device A ↔ Device B

### Future Enhancement
8. Optimize Supabase Realtime listeners
9. Add presence indicators (online/offline)
10. Add typing indicators in chat

---

## SUCCESS CRITERIA

### Stats Update Test
- [ ] Create chat → chatCount increases from 0 → 1 immediately
- [ ] Create task → taskCount increases from 0 → 1 immediately
- [ ] Add member → memberCount increases from 2 → 3 immediately
- [ ] Delete task → taskCount decreases from 1 → 0 immediately

### Navigation Test
- [ ] Tap "Chats" in bottom nav → Shows chats WITHOUT navigating away
- [ ] Tap "Tasks" in bottom nav → Shows tasks WITHOUT navigating away
- [ ] Bottom nav always visible in project workspace
- [ ] Back button exits project (not individual tabs)

### Real-Time Sync Test (Two Devices)
- [ ] Device A creates chat → Device B sees it immediately
- [ ] Device A adds member → Device B sees update immediately
- [ ] Device A creates task → Device B sees it immediately

---

## NOTES

- All Flow-based updates require `viewModelScope.launch` + `.collect`
- Room database automatically triggers Flow emissions when data changes
- Our metadata increment/decrement methods update `updatedAt` which triggers Flow
- No manual refresh needed - everything reactive!

