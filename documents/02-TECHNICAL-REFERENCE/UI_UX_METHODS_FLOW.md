# UI/UX METHODS & FLOW DOCUMENTATION

**Document Version:** 1.1
**Generated:** 2025-12-23
**Last Updated:** 2025-12-29 (Task Management Enhancements - Phase 8)
**Purpose:** Complete UI method inventory enabling full redesign without codebase access

---

## CRITICAL: THIS DOCUMENT ENABLES COMPLETE UI REDESIGN

This document catalogs EVERY UI element, handler, method, and data flow in the Kosmos application. A new team should be able to redesign the entire UI from this document alone without accessing the codebase.

---

## COMPLETE SCREEN INVENTORY (22 Screens)

### Authentication Flow (2 Screens)

#### 1. LoginScreen
**File:** `features/auth/presentation/AuthScreens.kt` (lines 25-176)
**Status:** ✅ Fully Functional
**Complexity:** Simple (176 lines)
**ViewModel:** AuthViewModel

**UI Elements & Handlers:**

| Element | Type | Event | Handler Method | Backend Method | Status |
|---------|------|-------|----------------|----------------|--------|
| Email Input | OutlinedTextField | onValueChange | `AuthViewModel.onEmailChange()` | - | ✅ |
| Password Input | OutlinedTextField | onValueChange | `AuthViewModel.onPasswordChange()` | - | ✅ |
| Password Visibility Toggle | IconButton | onClick | `AuthViewModel.togglePasswordVisibility()` | - | ✅ |
| Login Button | Button | onClick | `AuthViewModel.login()` | `AuthRepository.signInWithEmailAndPassword()` | ✅ |
| Sign Up Link | TextButton | onClick | Navigation to SignUpScreen | - | ✅ |
| Error Card | Card | - | Display `uiState.error` | - | ✅ |

**User Flows:**
1. **Happy Path:** Enter email/password → Click Login → Navigate to ProjectList
2. **Error Path:** Invalid credentials → Show error card → User retries
3. **Navigation:** Click "Sign Up" → Navigate to SignUpScreen

**Data Flow:**
```
User Input → AuthViewModel._uiState.update
         ↓
onClick Login → AuthViewModel.login()
         ↓
AuthRepository.signInWithEmailAndPassword(email, password)
         ↓
Supabase Auth API
         ↓
Success: userDao.insert(user) + _uiState.update(currentUser)
         ↓
LaunchedEffect observes isLoggedIn → Navigate to ProjectList
```

**Missing Functionality:** None - Complete

---

#### 2. SignUpScreen
**File:** `features/auth/presentation/AuthScreens.kt` (lines 178-687)
**Status:** ✅ Fully Functional
**Complexity:** Complex (510 lines)
**ViewModel:** AuthViewModel

**Form Fields (17 total):**

**Required:**
- Display Name (TextField)
- Username (TextField with availability check)
- Email (TextField, email keyboard)
- Password (TextField, password type)
- Confirm Password (TextField, password type)

**Optional (Expandable Section):**
- Age (TextField, number keyboard)
- Role/Title (TextField)
- Location (TextField)
- Bio (TextField, multiline, 500 char limit)
- GitHub URL (TextField, URL keyboard)
- Twitter/X URL (TextField)
- LinkedIn URL (TextField)
- Website URL (TextField)
- Portfolio URL (TextField)

**Key Methods:**
- `AuthViewModel.checkUsernameAvailability(username)` - Real-time check with debounce
- `AuthViewModel.signUp()` - Creates account with all fields
- Password confirmation validation (client-side)

**User Flow:**
```
Enter required fields → Check username availability (debounced)
    ↓
Expand optional fields → Fill social links
    ↓
Click Sign Up → Validate (passwords match, username available)
    ↓
AuthRepository.createUserWithEmailAndPassword(...)
    ↓
Navigate to ProjectList
```

---

### Project Management (4 Screens)

#### 3. ProjectListScreen (Redesign)
**File:** `features/projects/presentation/redesign/ProjectListScreen.kt`
**Status:** ✅ Mostly Functional
**ViewModel:** ProjectViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Project Card | onClick → Navigate to ProjectDetails | - | ✅ |
| Create Project FAB | onClick → Show dialog | - | ✅ |
| Create Project Dialog | onConfirm → `ProjectViewModel.createProject()` | `ProjectRepository.createProject()` | ✅ |
| Search Bar | onValueChange | ⚠️ Not wired | ⚠️ TODO |
| Filter Dropdown | onSelect | ⚠️ Not wired | ⚠️ TODO |
| Sort Dropdown | onSelect | ⚠️ Not wired | ⚠️ TODO |

**Project Card Contents:**
- Project name (Text)
- Description (Text, 2 lines max)
- Member count badge (Icon + Text)
- Chat count badge (Icon + Text)
- Task count badge (Icon + Text)
- Last activity timestamp (Text, relative time)

**Missing Functionality:**
- ⚠️ Search by project name/description
- ⚠️ Filter by status (Active/Archived/Completed)
- ⚠️ Sort by recent/name/members

---

#### 4. ProjectDetailsScreen (Redesign)
**File:** `features/projects/presentation/redesign/ProjectDetailsScreen.kt`
**Status:** ✅ Fully Functional
**ViewModel:** ProjectViewModel

**Tabs:**
1. **Overview Tab**
   - Project description
   - Recent chats (top 5)
   - Recent tasks (top 5)
   - Recent members (top 8)
   - Quick action buttons

2. **Chats Tab** → Navigates to EnhancedChatListScreen

3. **Tasks Tab** → Navigates to TaskBoardScreen

4. **Members Tab** → Navigates to MembersListScreen

**Quick Actions:**
- Create Chat → Navigate to CreateChatDialog
- Create Task → Open QuickTaskCreationSheet
- Invite Member → Navigate to InviteMembersScreen
- Edit Project → Open EditProjectDialog ⚠️ Incomplete

**Real-Time Stats:**
- Member count (updates on member add/remove)
- Chat count (updates on chat create/delete)
- Task count (updates on task create/delete)
- Completed tasks (updates on task status change)

---

#### 5. ProjectWorkspaceScreen (Redesign)
**File:** `features/projects/presentation/redesign/ProjectWorkspaceScreen.kt`
**Status:** ✅ Fully Functional
**Created:** Nov 9, 2025
**ViewModel:** ProjectViewModel

**Features:**
- Persistent animated bottom navigation
- Tab switching with spring animations
- Real-time stats fixed (JobCancellationException resolved)
- Smooth transitions between tabs

---

#### 6. MembersListScreen
**File:** `features/projects/presentation/MembersListScreen.kt`
**Status:** ✅ Fully Functional (490 lines)
**ViewModel:** MembersListViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Member List Item | onClick → Navigate to UserProfile | - | ✅ |
| Add Members FAB | onClick → Navigate to InviteMembers | - | ✅ |
| Online Status Indicator | - | Real-time from user.isOnline | ✅ |
| Role Badge | - | Display member.role | ✅ |
| Remove Member Button (Admin) | onClick → Confirmation → Remove | `ProjectRepository.removeMember()` | ⚠️ UI incomplete |
| Change Role Button (Admin) | onClick → Show picker → Change | `ProjectRepository.changeRole()` | ⚠️ UI incomplete |

**Member List Item Contents:**
- Avatar (circular, 40dp)
- Display name (Text, headline)
- Username (Text, body)
- Role badge (Chip, color-coded)
- Online status (Circle, 8dp, green/gray)
- Last seen (Text, relative time)

**Missing Functionality:**
- ⚠️ Remove member dialog (admin only)
- ⚠️ Change role dialog (admin only)

---

### Chat & Messaging (2 Screens)

#### 7. EnhancedChatListScreen (Redesign)
**File:** `features/chat/presentation/redesign/EnhancedChatListScreen.kt`
**Status:** ✅ Fully Functional
**ViewModel:** ChatListViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Chat List Item | onClick → Navigate to Chat | - | ✅ |
| Swipe Right (Archive) | onSwipe → Archive | `ChatRepository.archiveChatRoom()` | ✅ |
| Swipe Left (Delete) | onSwipe → Confirmation → Delete | `ChatRepository.deleteChatRoom()` | ✅ |
| Long Press | onLongPress → Pin | `ChatRepository.pinChatRoom()` | ✅ |
| Create Chat FAB | onClick → CreateChatDialog | - | ✅ |

**Chat List Item Contents:**
- Avatar (group avatar or user avatar, 56dp)
- Chat name (Text, headline)
- Last message preview (Text, body, 1 line)
- Timestamp (Text, relative time)
- Unread badge (Badge, count)
- Pin indicator (Icon, if pinned)
- Archive indicator (Icon, if archived)

---

#### 8. EnhancedChatScreen (Redesign)
**File:** `features/chat/presentation/redesign/EnhancedChatScreen.kt`
**Status:** ✅ Fully Functional
**ViewModel:** ChatViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Message List | LazyColumn | `ChatRepository.getMessagesFlow()` | ✅ |
| Text Input | onValueChange | - | ✅ |
| Send Button | onClick → Send | `ChatRepository.sendMessage()` | ✅ |
| Long Press Message | onLongPress → Context Menu | - | ✅ |
| React (from menu) | onClick → Toggle | `ChatRepository.toggleReaction()` | ✅ |
| Edit (from menu) | onClick → Edit mode | `ChatRepository.editMessage()` | ✅ |
| Delete (from menu) | onClick → Confirmation | `ChatRepository.deleteMessage()` | ✅ |
| Reply (from menu) | onClick → Reply mode | Set replyToMessage | ✅ |
| Copy (from menu) | onClick → Clipboard | Android Clipboard API | ✅ |
| Create Task (from menu) | onClick → Create task from message | Navigate to CreateTaskDialog | ✅ |
| Voice Record Button | onPress → Record | ⚠️ Disabled for MVP | ❌ |
| Camera Button | onClick → Camera | ⚠️ Placeholder | ❌ |
| Attach File Button | onClick → File picker | ⚠️ Placeholder | ❌ |

**Message Bubble Components:**
- Sender name (if not own message)
- Message content (Text, linkify)
- Timestamp (Text, small, gray)
- Edit indicator (if edited)
- Read receipts (double check marks)
- Reactions (Row of emoji chips)
- Reply preview (if replying to another message)
- Thread indicator (if has replies) ⚠️ UI incomplete

**Message Grouping:**
- Messages from same sender within 5 minutes are grouped
- Only show sender name on first message in group
- Only show avatar on first message in group

**Real-Time Features:**
- New messages appear instantly
- Typing indicators (shows "User is typing...")
- Online status in app bar

---

### Task Management (3 Screens)

#### 9. TaskBoardScreen
**File:** `features/tasks/presentation/TaskScreens.kt` (lines 34-327)
**Status:** ⚠️ Old Version with Partial Redesign (95% design system compliance)
**ViewModel:** TaskViewModel
**Complexity:** Very Complex (1,418 lines total file)

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Tab (All/TODO/In Progress/Done) | onClick → Filter | Client-side filter | ✅ |
| My Tasks Filter Chip | onClick → Toggle | Client-side filter | ✅ |
| Task Card | onClick → EditTaskDialog | - | ✅ |
| Task Completion Checkbox | onClick → Toggle | `TaskRepository.updateTaskStatus()` | ✅ |
| Create Task FAB | onClick → CreateTaskDialog | - | ✅ |

**Task Card Contents:**
- Completion checkbox (if TODO or IN_PROGRESS)
- Priority badge (Circle, color-coded: 🔵🟡🟠🔴)
- Task title (Text, headline)
- Task description (Text, body, 2 lines max)
- Status chip (Chip, color-coded)
- Assigned user (Avatar + name)
- Due date (Text with warning icons: ⚠️ overdue, ⏰ due soon)
- Tags (Row of chips, max 2 + count)
- Time tracking (Text: "2h / 4h est" with red if over)
- Comments count (Icon + count)

**Missing Functionality:**
- ⚠️ Drag-and-drop to change status
- ⚠️ Bulk actions (select multiple tasks)
- ⚠️ Task search/filter

---

#### 10. CreateTaskDialog
**File:** `features/tasks/presentation/TaskScreens.kt` (lines 330-707)
**Status:** ✅ Fully Functional
**Complexity:** Complex (377 lines)

**Form Fields:**

| Field | UI Element | Required | Handler | Status |
|-------|-----------|----------|---------|--------|
| Title | TextField | ✅ | onValueChange | ✅ |
| Description | TextField (multiline) | ❌ | onValueChange | ✅ |
| Priority | Chip Group | ❌ | onClick (LOW/MEDIUM/HIGH/URGENT) | ✅ |
| Assigned To | Card (user picker) | ❌ | onClick → Bottom Sheet | ✅ |
| Due Date | Card (date picker) | ❌ | onClick → Material DatePicker | ✅ |
| Tags | Add/remove chips | ❌ | onClick add, chip click remove | ✅ |
| Estimated Hours | TextField (number) | ❌ | onValueChange | ✅ |
| Actual Hours | TextField (number) | ❌ | onValueChange | ✅ |

**Actions:**
- Cancel → Dismiss dialog
- Create Task → `TaskViewModel.createTask()` → `TaskRepository.createTask()`

---

#### 11. EditTaskDialog
**File:** `features/tasks/presentation/TaskScreens.kt` (lines 710-1122)
**Status:** ✅ Fully Functional
**Complexity:** Very Complex (412 lines)

**Additional Fields (beyond Create):**
- Status selector (4 chips: TODO/IN_PROGRESS/DONE/CANCELLED)
- Parent task selector (for subtasks) ⚠️ UI picker incomplete
- Comments section (expandable):
  - Comment list (sorted by newest)
  - Add comment input
  - Comment metadata (author, timestamp)

**Actions:**
- Save → `TaskViewModel.updateTask()`
- Delete → Confirmation → `TaskViewModel.deleteTask()`

---

#### 11B. TaskDetailScreen (Redesigned) - **NEW 2025-12-29** ✨

**Files:**
- `features/tasks/presentation/redesign/TaskDetailScreen.kt`
- `features/tasks/presentation/redesign/TaskDetailScreenWrapper.kt`
- `features/tasks/presentation/TaskDetailViewModel.kt`

**Status:** ✅ Fully Enhanced (Phase 8 - Dec 29, 2025)
**Complexity:** Complex (1,112 lines)

**Major Enhancements:**

##### 1. Interactive Status Badge (PROMINENT Design) 🎯
- **Location:** Top of detail screen, after title/due date
- **Behavior:** Clickable badge opens dropdown menu
- **Design Philosophy:** "Status is LOUD" - always visible, prominent action
- **Permission Enforcement:** Only assigned user can mark task as DONE
  - If task assigned: Only assignee sees DONE enabled
  - If task unassigned: Anyone can complete
- **Visual Feedback:**
  - Color-coded badges (TODO: gray, IN_PROGRESS: blue, DONE: green, CANCELLED: red)
  - Current status shows checkmark in dropdown
  - Instant update on selection
- **Backend:** `TaskViewModel.updateTaskStatus()` → `TaskRepository.updateTaskStatus()`

##### 2. Interactive Priority Badge (SUBTLE Design) ✏️
- **Location:** Next to status badge
- **Behavior:** Small edit icon (14dp, 60% opacity) opens dropdown
- **Design Philosophy:** "Editing is SUBTLE" - inline, not loud
- **Visual Feedback:**
  - Color-coded priorities (URGENT: dark red, HIGH: red, MEDIUM: orange, LOW: blue)
  - Current priority shows checkmark
- **Backend:** `TaskViewModel.updateTaskPriority()` → `TaskRepository.updateTask()`

##### 3. Time Tracking Section ⏱️
- **Location:** Between Assign and Tags sections
- **Layout:** Dedicated card with title "Time Tracking"
- **Fields:**
  - **Estimated Hours:** Decimal input with "hrs" suffix
  - **Actual Hours:** Decimal input with "hrs" suffix
  - **Keyboard:** Decimal type with validation (positive numbers only)
- **Progress Visualization:**
  - Appears automatically when both hours are set
  - Shows percentage: "75% complete" or "112% (over budget)"
  - Color-coded progress bar:
    - **Green:** 0-80% complete (on track)
    - **Orange:** 80-100% complete (warning)
    - **Red:** >100% complete (over budget)
  - Progress bar fills proportionally, capped at 100% visual
- **Backend:**
  - `TaskViewModel.updateEstimatedHours()` → `TaskRepository.updateTask()`
  - `TaskViewModel.updateActualHours()` → `TaskRepository.updateTask()`

##### 4. Helper Functions Added
- `getStatusColor(status: TaskStatus): Color` - Maps status to color
- `getStatusLabel(status: TaskStatus): String` - Maps status to display text
- `getPriorityColor(priority: TaskPriority): Color` - Maps priority to color
- `getPriorityLabel(priority: TaskPriority): String` - Maps priority to display text
- `TimeInputField` component - Reusable time input with validation

##### UI State Management
- `currentUserId` added to `TaskDetailUiState` for permission checks
- Real-time updates via Flow from ViewModel
- Offline-first: Updates save to Room immediately, sync to Supabase in background

**User Flows:**

**Flow 1: Update Task Status**
1. User opens task in TaskDetailScreen
2. User taps status badge (large, prominent)
3. Dropdown shows all statuses with color indicators
4. User sees "Done" option:
   - If assigned to them: Enabled
   - If assigned to someone else: Disabled
   - If unassigned: Enabled
5. User taps "Done"
6. Badge updates to green "Done"
7. Change syncs to database (offline-first)

**Flow 2: Track Task Progress**
1. User starts working on task
2. Opens task detail
3. Scrolls to "Time Tracking" section
4. Enters "8" in Estimated Hours
5. Enters "6" in Actual Hours
6. Progress bar appears: "75% complete" (green)
7. Later enters "9" actual hours
8. Progress bar turns red: "112% (over budget)"

---

#### 11C. TaskBoardScreen (Kanban) - **UPDATED 2025-12-29** 🧹

**File:** `features/tasks/presentation/redesign/TaskBoardScreen.kt`
**Status:** ✅ Cleanup Complete
**Changes:** Removed redundant "Add Card" buttons from columns

**Before:**
- Each Kanban column (TODO, IN PROGRESS, DONE) had "Add Card" button
- Confused UX with FAB at bottom-right

**After:**
- Clean column layout with only task cards
- Single FAB for task creation (consistent across app)
- ~40 lines of redundant code removed

**Impact:** Cleaner, less cluttered Kanban board UI

---

#### 11D. QuickTaskCreationSheet - **FIXED 2025-12-29** 🐛

**File:** `features/tasks/presentation/redesign/QuickTaskCreationSheetWrapper.kt`
**Status:** ✅ Bug Fixed
**Issue:** Selected status was ignored, tasks always created as TODO

**Fix:**
- Added `status = quickTaskData.status.toDomainStatus()` parameter
- TaskViewModel.createTask() now accepts status parameter with default TODO
- Tasks now respect user's selected status during creation

**Impact:** Critical bug resolved - tasks created with correct status

---

#### 12. MyTasksScreen (Redesign)
**File:** `features/tasks/presentation/redesign/MyTasksScreen.kt`
**Status:** ✅ Implemented
**Purpose:** Cross-project task view

**Features:**
- My assigned tasks from all projects
- Filter by project
- Filter by status
- Sort by due date/priority/created

---

### User Management (4 Screens)

#### 13. UserSearchScreen
**File:** `features/users/presentation/UserSearchScreen.kt`
**Status:** ✅ Fully Functional (203 lines)
**ViewModel:** UserSearchViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Search Input | onValueChange (debounced 500ms) | `UserRepository.searchUsers()` | ✅ |
| User List Item | onClick → Navigate to UserProfile | - | ✅ |

---

#### 14. UserProfileScreen
**File:** `features/users/presentation/UserProfileScreen.kt`
**Status:** ✅ Fully Functional (516 lines)
**ViewModel:** UserProfileViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Start Chat Button | onClick → Create 1-on-1 chat | `ChatRepository.createChatRoom()` + Navigate | ✅ |
| Add to Project Button | onClick → AddToProjectDialog | `ProjectRepository.addMember()` | ✅ |
| Social Link (GitHub) | onClick → Open browser | Android Intent | ✅ |
| Social Link (Twitter) | onClick → Open browser | Android Intent | ✅ |
| Social Link (LinkedIn) | onClick → Open browser | Android Intent | ✅ |
| Social Link (Website) | onClick → Open browser | Android Intent | ✅ |
| Social Link (Portfolio) | onClick → Open browser | Android Intent | ✅ |

---

#### 15. InviteMembersScreen
**File:** `features/users/presentation/InviteMembersScreen.kt`
**Status:** ✅ Fully Functional (334 lines)
**ViewModel:** InviteMembersViewModel

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Search Input | onValueChange (debounced) | `UserRepository.searchUsers()` | ✅ |
| User List Item | onClick → Toggle selection | - | ✅ |
| Selected Chip | onClick → Remove from selection | - | ✅ |
| Role Selector | onClick (Admin/Manager/Member) | - | ✅ |
| Invite Button | onClick → Invite all selected | `ProjectRepository.addMember()` (forEach) | ✅ |

---

#### 16. AddToProjectDialog
**File:** `features/users/presentation/components/AddToProjectDialog.kt`
**Status:** ✅ Implemented

**UI Elements:**
- Project picker (dropdown)
- Role selector (chips)
- Add button

---

### Profile & Settings (5 Screens)

#### 17. ProfileScreen
**File:** `features/profile/presentation/ProfileScreen.kt`
**Status:** ✅ Fully Functional (186 lines)
**Design System:** 100% compliant

**UI Elements:**

| Element | Handler | Status |
|---------|---------|--------|
| Profile Photo (120dp) | - | ✅ |
| Display Name | - | ✅ |
| Email | - | ✅ |
| Edit Profile | onClick → Navigate | ✅ |
| Privacy Settings | onClick → Navigate | ✅ |
| Notifications | onClick → Navigate | ✅ |

---

#### 18. EditProfileScreen
**File:** `features/profile/presentation/EditProfileScreen.kt`
**Status:** ⚠️ Partial (409 lines)

**UI Elements:**

| Element | Handler | Backend Method | Status |
|---------|---------|----------------|--------|
| Photo Upload Button | onClick → Image picker | ⚠️ Supabase Storage upload TODO | ⚠️ |
| Display Name | onValueChange | `UserRepository.updateUser()` | ✅ |
| Bio | onValueChange | `UserRepository.updateUser()` | ✅ |
| Location | onValueChange | `UserRepository.updateUser()` | ✅ |
| Social Links | onValueChange | `UserRepository.updateUser()` | ✅ |
| Save Button | onClick → Update | `UserRepository.updateUser()` | ✅ |

**Known Issue:** Photo picker works, selected photo shows in UI, but Supabase Storage upload not implemented. Photo doesn't persist after app restart.

---

#### 19. PrivacySettingsScreen
**File:** `features/profile/presentation/PrivacySettingsScreen.kt`
**Status:** ⚠️ Minimal (346 lines)
**ViewModel:** PrivacySettingsViewModel

**UI Elements (Toggles):**
- Show online status
- Read receipts
- Typing indicators
- Last seen visibility
- Profile photo visibility
- Who can add me to projects

**Backend:** ⚠️ Minimal implementation, settings not persisted

---

#### 20. NotificationSettingsScreen
**File:** `features/profile/presentation/NotificationSettingsScreen.kt`
**Status:** ⚠️ Minimal (490 lines)
**ViewModel:** NotificationSettingsViewModel

**UI Elements (Toggles):**
- Push notifications (master)
- Message notifications
- Task notifications
- Mention notifications
- Sound
- Vibration

**Backend:** ⚠️ Minimal implementation, settings not persisted

---

#### 21. SettingsScreen
**File:** `MainActivity.kt` (lines 366-553)
**Status:** ✅ Fully Functional
**Location:** Inline in MainActivity

**UI Elements:**
- App info card (name, version, build type)
- Clear cache button
- Logout button

---

### Dialogs & Bottom Sheets (Additional Components)

#### CreateChatDialog
**File:** `features/chat/components/CreateChatDialog.kt`
**Status:** ✅ Fully Functional (Created Nov 9, 2025)

**Features:**
- Multi-select user picker (WhatsApp-style)
- Selected users as chips (removable)
- Chat name input
- Chat type selector (Channel/Direct/Task Discussion)

---

#### EditProjectDialog
**File:** `features/projects/components/EditProjectDialog.kt`
**Status:** ⚠️ Exists but Incomplete

**Working:**
- Edit name
- Edit description

**Missing:**
- Archive project
- Delete project
- Change visibility

---

#### ChatOptionsBottomSheet
**File:** Implemented in EnhancedChatListScreen
**Status:** ✅ Fully Functional

**Options:**
- Pin/Unpin chat
- Archive/Unarchive chat
- Delete chat (with confirmation)

---

## NAVIGATION FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────┐
│                 AUTHENTICATION LAYER                     │
├─────────────────────────────────────────────────────────┤
│  LoginScreen  ←→  SignUpScreen                          │
└──────────────────────┬──────────────────────────────────┘
                       │ (on login success)
                       ↓
┌─────────────────────────────────────────────────────────┐
│                  BOTTOM NAVIGATION                       │
├─────────────────────────────────────────────────────────┤
│  [Projects]  [Chats]  [Tasks]  [More]                   │
└───┬──────────────┬─────────────┬──────────────┬─────────┘
    │              │             │              │
┌───▼──────┐   ┌───▼──────┐  ┌──▼─────┐   ┌───▼──────┐
│ Project  │   │ Enhanced │  │ My     │   │ Profile  │
│ List     │   │ Chat     │  │ Tasks  │   │          │
│          │   │ List     │  │        │   ├──────────┤
│          │   │          │  │        │   │ Settings │
└───┬──────┘   └───┬──────┘  └────────┘   └───┬──────┘
    │              │                            │
    │              │                        ┌───▼──────────┐
┌───▼───────────┐  │                        │ Edit Profile │
│ Project       │  │                        ├──────────────┤
│ Details/      │  │                        │ Privacy      │
│ Workspace     │  │                        │ Settings     │
├───────────────┤  │                        ├──────────────┤
│ [Overview]    │  │                        │ Notification │
│ [Chats]       │◄─┘                        │ Settings     │
│ [Tasks]       │                           └──────────────┘
│ [Members]     │
└───┬───────────┘
    │
    ├─→ Enhanced Chat → Chat Room
    │
    ├─→ TaskBoard → Create/Edit Task Dialog
    │
    ├─→ MembersList → UserProfile → Start Chat / Add to Project
    │
    ├─→ UserSearch → UserProfile
    │
    └─→ InviteMembers → Select Users → Send Invites
```

---

## COMPLETE USER JOURNEY MAPS

### Journey 1: Create Project → Invite Team → Start Collaborating

**Steps:**
1. Login (LoginScreen)
2. ProjectList loads → Click Create Project FAB
3. CreateProjectDialog → Enter name, description → Click Create
4. Navigate to ProjectDetails (new project)
5. Click "Invite Members" button
6. InviteMembersScreen → Search users
7. Select 3 users → Choose role (Manager) → Click Invite
8. Back to ProjectDetails → Members Tab shows new members
9. Click "Create Chat" button
10. CreateChatDialog → Select 2 members, name chat "Design Team" → Create
11. Navigate to EnhancedChatScreen
12. Type "Welcome to the team!" → Click Send
13. Message appears in bubble
14. Team members see message in real-time (WebSocket)

**Total Screens:** 6 unique screens
**Total Interactions:** 13 user actions
**Backend Calls:** 4 (create project, invite members×3, create chat, send message)

---

### Journey 2: Create Task → Assign → Track Progress → Complete

**Steps:**
1. ProjectDetails → Tasks Tab
2. Click Create Task FAB
3. CreateTaskDialog:
   - Title: "Design landing page"
   - Description: "Create mockups for home page"
   - Priority: HIGH (click orange chip)
   - Assigned To: Click → Bottom sheet → Select "Jane Designer"
   - Due Date: Click → DatePicker → Select tomorrow
   - Tags: Add "design", "ui"
   - Estimated Hours: 4
   - Click Create Task
4. Navigate to TaskBoard
5. Task card appears in "To Do" column
6. Jane sees task in her "My Tasks" screen
7. Jane clicks task → EditTaskDialog
8. Change status to IN_PROGRESS
9. Add comment: "Started working on this"
10. Save
11. Task moves to "In Progress" column (real-time update)
12. Jane works for 3 hours
13. Click task → Update actual hours: 3
14. Complete work → Change status to DONE
15. Task moves to "Done" column
16. Project stats update: completedTaskCount + 1

**Total Screens:** 3 unique screens (ProjectDetails, TaskBoard, EditTaskDialog)
**Total Interactions:** 15 user actions
**Backend Calls:** 5 (create task, assign, update status×2, add comment, update time)

---

### Journey 3: Send Message → React → Reply → Create Task

**Steps:**
1. ProjectDetails → Chats Tab → EnhancedChatListScreen
2. Click chat room "General"
3. EnhancedChatScreen loads messages
4. User types: "We should redesign the dashboard"
5. Click Send → Message appears instantly (optimistic update)
6. Supabase syncs in background
7. Team member sees message (real-time WebSocket)
8. Team member long presses message
9. Context menu appears
10. Click "React" → Emoji picker (not shown, simplified) → Select 👍
11. Reaction appears below message (both users see it)
12. Another member clicks "Reply"
13. Reply bar shows original message preview
14. Types: "I agree, let's create a task for it"
15. Click Send
16. Reply shows with thread indicator
17. First user long presses original message again
18. Click "Create Task"
19. CreateTaskDialog pre-fills title: "Redesign the dashboard"
20. Fill remaining fields → Create
21. System message appears in chat: "Task created: Redesign the dashboard"

**Total Screens:** 3 (ChatList, ChatRoom, CreateTaskDialog)
**Total Interactions:** 21 user actions
**Backend Calls:** 6 (send message×3, add reaction, create task, send system message)

---

## BACKEND METHOD → UI MAPPING

| UI Action | Screen | UI Handler | ViewModel Method | Repository Method | Database | API/Service |
|-----------|--------|------------|------------------|-------------------|----------|-------------|
| Click Login | LoginScreen | onClick | `login()` | `signInWithEmailAndPassword()` | `userDao.insert()` | Supabase Auth |
| Create Project | ProjectListScreen | onClick | `createProject()` | `ProjectRepository.createProject()` | `projectDao.insert()` | Supabase `projects` |
| Send Message | ChatScreen | onClick | `sendMessage()` | `ChatRepository.sendMessage()` | `messageDao.insert()` | Supabase `messages` |
| Create Task | TaskBoard | onClick | `createTask()` | `TaskRepository.createTask()` | `taskDao.insert()` | Supabase `tasks` |
| Toggle Reaction | ChatScreen | onClick | `toggleReaction()` | `ChatRepository.toggleReaction()` | `messageDao.update()` | Supabase `messages` |
| Invite Member | InviteMembers | onClick | `inviteMembers()` | `ProjectRepository.addMember()` | `projectMemberDao.insert()` | Supabase `project_members` |
| Archive Chat | ChatList | onSwipe | `archiveChat()` | `ChatRepository.archiveChatRoom()` | `chatRoomDao.update()` | Supabase `chat_rooms` |
| Assign Task | EditTask | onClick | `assignTask()` | `TaskRepository.assignTask()` | `taskDao.update()` | Supabase `tasks` |
| Update Profile | EditProfile | onClick | `updateProfile()` | `UserRepository.updateUser()` | `userDao.update()` | Supabase `users` |
| Change Role | MembersList | onClick | `changeRole()` | `ProjectRepository.changeRole()` | `projectMemberDao.update()` | Supabase `project_members` |

---

## MISSING UI FUNCTIONALITY (Backend Exists, No UI)

### High Priority

1. **EditProjectDialog - Archive/Delete**
   - Backend: `ProjectRepository.deleteProject()`, `updateProjectStatus()`
   - UI: ❌ Dialog exists but buttons missing
   - Impact: Users can create projects but not manage them

2. **MembersList - Remove/Change Role Actions**
   - Backend: `ProjectRepository.removeMember()`, `changeRole()`
   - UI: ❌ No dialogs or context menus
   - Impact: Admins can't manage team

3. **Photo Upload - Supabase Storage**
   - Backend: ❌ Upload method missing
   - UI: ✅ Photo picker works
   - Impact: Profile photos don't persist

### Medium Priority

4. **Project Search/Filter**
   - Backend: ✅ Can filter client-side
   - UI: ⚠️ Placeholder inputs not wired
   - Impact: Hard to find projects with many items

5. **Task Subtask Picker**
   - Backend: ✅ `task.parentTaskId` supported
   - UI: ⚠️ Selector exists but user picker incomplete
   - Impact: Can't create task hierarchies

6. **Message Threading UI**
   - Backend: ✅ `message.replyToMessageId` supported
   - UI: ⚠️ Thread indicator exists, but can't navigate to original
   - Impact: Conversations hard to follow

### Low Priority

7. **Chat Search**
   - Backend: ❌ Not implemented
   - UI: ⚠️ Search button placeholder
   - Impact: Hard to find old messages

---

## DATA FLOW PATTERNS

### Pattern 1: Optimistic Update (Write)

```
User Action (Click Send Message)
    ↓
1. ViewModel updates local state immediately
   _uiState.update { it.copy(messages = messages + newMessage) }
    ↓
2. Repository saves to Room (instant)
   messageDao.insertMessage(message)
    ↓
3. Flow emits → UI updates (message appears in list)
    ↓
4. Repository syncs to Supabase (async, background)
   supabaseDataSource.insertMessage(message)
    ↓
5. If sync fails → Retry with SyncRetryHelper (3 attempts, exponential backoff)
    ↓
6. Success → Log, done
   Failure after 3 attempts → Show error, mark for retry
```

**Result:** User sees instant feedback (0ms perceived latency)

---

### Pattern 2: Real-Time Update (Read)

```
Supabase Database Change (Another user sends message)
    ↓
1. Supabase Realtime WebSocket event fires
   PostgresAction.Insert detected
    ↓
2. SupabaseRealtimeManager.handleMessageInsert()
    ↓
3. Parse message from JSON
   val message = parseMessage(record)
    ↓
4. Save to Room
   messageDao.insertMessage(message)
    ↓
5. Room Flow emits new value
   Flow<List<Message>> emits updated list
    ↓
6. ViewModel collects Flow
   messages: StateFlow = repository.getMessagesFlow().stateIn(...)
    ↓
7. UI observes StateFlow
   val messages by viewModel.messages.collectAsStateWithLifecycle()
    ↓
8. Compose recomposes with new message
```

**Result:** All users see updates in <500ms (real-time)

---

### Pattern 3: Form Validation (Input)

```
User Types in TextField
    ↓
1. onValueChange fires
   TextField(value = email, onValueChange = { viewModel.onEmailChange(it) })
    ↓
2. ViewModel updates state
   fun onEmailChange(email: String) {
       _uiState.update { it.copy(email = email) }
       validateEmail(email)
   }
    ↓
3. Validation runs
   private fun validateEmail(email: String) {
       val isValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
       _uiState.update { it.copy(emailError = if (!isValid) "Invalid email" else null) }
   }
    ↓
4. UI reflects validation state
   TextField(
       isError = uiState.emailError != null,
       supportingText = { uiState.emailError?.let { Text(it) } }
   )
    ↓
5. Button enabled based on validation
   PrimaryButton(
       enabled = uiState.emailError == null && uiState.email.isNotBlank()
   )
```

**Result:** Real-time validation feedback

---

## STATE MANAGEMENT PATTERNS

### Pattern 1: Screen UI State

```kotlin
data class SomeScreenUiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedItem: Item? = null,
    val showDialog: Boolean = false
)

// In ViewModel
private val _uiState = MutableStateFlow(SomeScreenUiState())
val uiState: StateFlow<SomeScreenUiState> = _uiState.asStateFlow()

// In Screen
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

when {
    uiState.isLoading -> LoadingIndicator()
    uiState.error != null -> ErrorState(uiState.error)
    uiState.data.isEmpty() -> EmptyState()
    else -> DataList(uiState.data)
}
```

---

### Pattern 2: Dialog State

```kotlin
// In UiState
data class UiState(
    val showCreateDialog: Boolean = false,
    val dialogData: DialogData? = null
)

// In ViewModel
fun openDialog(data: DialogData) {
    _uiState.update { it.copy(showCreateDialog = true, dialogData = data) }
}

fun closeDialog() {
    _uiState.update { it.copy(showCreateDialog = false, dialogData = null) }
}

// In Screen
if (uiState.showCreateDialog) {
    CreateDialog(
        data = uiState.dialogData,
        onDismiss = { viewModel.closeDialog() },
        onConfirm = { viewModel.createItem(it); viewModel.closeDialog() }
    )
}
```

---

### Pattern 3: List Loading State

```kotlin
sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

// In ViewModel
private val _dataState = MutableStateFlow<DataState<List<Item>>>(DataState.Loading)
val dataState: StateFlow<DataState<List<Item>>> = _dataState.asStateFlow()

fun loadData() {
    viewModelScope.launch {
        _dataState.value = DataState.Loading

        repository.getData().fold(
            onSuccess = { _dataState.value = DataState.Success(it) },
            onFailure = { _dataState.value = DataState.Error(it.message ?: "Unknown error") }
        )
    }
}

// In Screen
when (val state = uiState.dataState) {
    is DataState.Loading -> CircularProgressIndicator()
    is DataState.Success -> LazyColumn { items(state.data) { ItemCard(it) } }
    is DataState.Error -> ErrorView(state.message, onRetry = { viewModel.loadData() })
}
```

---

## ACCESSIBILITY FEATURES

### Touch Targets
- All interactive elements: Minimum 48dp (Tokens.TouchTarget.minimum)
- Primary buttons: 56dp (Tokens.TouchTarget.recommended)
- Important actions: 64dp (Tokens.TouchTarget.comfortable)

### Color Contrast
- Text on background: WCAG AA compliant
- Status colors: High contrast (green, orange, red)
- Focus indicators: Visible borders

### Screen Reader Support
- ⚠️ Content descriptions mostly missing (TODO)
- ⚠️ Semantic structure needs improvement

---

## ANIMATION SPECIFICATIONS

### Screen Transitions
- Duration: 300ms (Tokens.Duration.medium)
- Easing: FastOutSlowInEasing
- Type: Fade + Slide

### Button Press
- Duration: 150ms (Tokens.Duration.ripple)
- Effect: Ripple + Scale (0.95)

### List Item Animations
- Enter: FadeIn + SlideIn (200ms)
- Exit: FadeOut + SlideOut (150ms)

### Bottom Navigation
- Tab switch: Spring animation (dampingRatio = 0.7f, stiffness = 400f)
- Icon scale: 1.0 → 1.2 → 1.0
- Background: Circular reveal (300ms)

---

## CONCLUSION

This document provides complete UI/UX method documentation for the Kosmos application. A new team can use this to:

1. **Understand all user interactions** - Every button, input, gesture documented
2. **Map complete data flows** - UI → ViewModel → Repository → Database
3. **Identify missing functionality** - Backend exists but UI incomplete
4. **Redesign from scratch** - All screens, navigation, and patterns documented
5. **Maintain consistency** - Design patterns and state management documented

**Total Screens Documented:** 22
**Total User Interactions:** 100+
**Total Backend Methods:** 50+
**Navigation Paths:** 30+

---

**Document Prepared By:** Claude Code Analysis System
**Related Documents:**
- PROJECT_OVERVIEW_STATUS.md
- CODEBASE_MODULE_DOCS.md
- LOGS_SESSIONS_ANALYSIS.md
- GAPS_RISKS_VERIFICATION.md
- IMPROVEMENT_ROADMAP.md
