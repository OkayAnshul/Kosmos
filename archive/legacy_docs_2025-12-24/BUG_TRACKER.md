# KOSMOS - BUG TRACKER & FIX LOG

**Created**: 2025-11-02
**Last Updated**: 2025-11-02
**Status**: Initial Analysis Complete, Fixes In Progress

---

## BUG SUMMARY

| Severity | Total | Fixed | In Progress | Pending |
|----------|-------|-------|-------------|---------|
| **P0 (Critical)** | 3 | 1 | 0 | 2 |
| **P1 (High)** | 8 | 0 | 0 | 8 |
| **P2 (Medium)** | 12 | 0 | 0 | 12 |
| **P3 (Low)** | 7 | 0 | 0 | 7 |
| **TOTAL** | **30** | **1** | **0** | **29** |

---

## P0 - CRITICAL BUGS (App Breaking, Core Features Broken)

### BUG-P0-001: "Add to Project" Feature Not Implemented
**Status**: ✅ FIXED (2025-11-02)
**Severity**: P0 (Critical)
**Impact**: Cannot add users to projects, blocking multi-user collaboration
**Affects**: User Profile Screen, Project Collaboration

**Location**:
- `app/src/main/java/com/example/kosmos/features/users/presentation/UserProfileScreen.kt:213-223`
- `app/src/main/java/com/example/kosmos/features/users/presentation/components/AddToProjectDialog.kt` (NEW)
- `app/src/main/java/com/example/kosmos/features/users/presentation/UserProfileViewModel.kt:159-287` (ENHANCED)

**Description** (Original):
When clicking "Add to Project" button on a user's profile, nothing happens. The feature was marked with TODO comment and was not implemented.

**Expected Behavior**:
1. Click "Add to Project" button
2. Dialog/sheet opens with list of user's projects
3. Select project
4. Choose role for new member (MEMBER/MANAGER)
5. User is added to `project_members` table
6. Success message shows
7. User appears in project members list

**Actual Behavior** (Before Fix):
Button existed but was disabled (TODO placeholder at line 185)

**FIX IMPLEMENTED** ✅

**Implementation Details**:

1. ✅ **Created AddToProjectDialog.kt** (370 lines)
   - Material Design 3 dialog with proper sizing
   - LazyColumn for project list with selection state
   - Role picker with FilterChips (MEMBER/MANAGER)
   - RBAC note: ADMIN role requires project settings
   - Empty state handling when user has no projects
   - Loading/error states with proper feedback
   - Clean dismiss/confirm actions

2. ✅ **Enhanced UserProfileViewModel.kt**
   - `loadMyProjects()`: Fetches user's projects via repository
   - `addUserToProject()`: Adds user with role validation
   - `setShowAddToProjectDialog()`: State management
   - `clearAddToProjectSuccess()`: Success message handling
   - Enhanced UserProfileState with 6 new fields
   - Comprehensive error handling and logging
   - RBAC enforcement (prevents ADMIN assignment)

3. ✅ **Updated UserProfileScreen.kt**
   - Enabled "Add to Project" button (removed `enabled = false`)
   - Added PersonAdd icon to button
   - Integrated AddToProjectDialog display
   - Success feedback via LaunchedEffect
   - Proper state management with dialog show/hide

**Technical Approach**:
- Reuses existing `ProjectRepository.addMember()` (no code duplication)
- Fetches projects via `getProjectsWithMembersByUserId()`
- Follows app's MVVM + Repository pattern
- Material Design 3 components (consistent with app theme)
- Optimistic UI with loading indicators
- Error handling at every layer

**RBAC Enforcement**:
- Only MEMBER and MANAGER roles can be assigned via this dialog
- ADMIN role validation with clear error message
- Prevents privilege escalation
- Respects project permission hierarchy

**Testing Criteria** (Device Testing Required):
- [ ] Dialog opens when button clicked
- [ ] User's projects load and display
- [ ] Can select project from list
- [ ] Selected project highlights with checkmark
- [ ] Can choose MEMBER or MANAGER role
- [ ] Role chips toggle correctly
- [ ] "Add to Project" button enables when project selected
- [ ] Loading indicator shows during operation
- [ ] User added to Supabase `project_members` table
- [ ] User appears in project members list
- [ ] Error handling works:
  - [ ] User already a member error
  - [ ] Network error with retry
  - [ ] No projects available shows empty state
- [ ] Success closes dialog
- [ ] Can cancel without adding

**Files Created/Modified**:
- ✅ Created: `app/.../components/AddToProjectDialog.kt` (+370 lines)
- ✅ Modified: `UserProfileViewModel.kt` (+135 lines)
- ✅ Modified: `UserProfileScreen.kt` (~30 lines modified)

**Fixed By**: Claude Code
**Fixed On**: 2025-11-02
**Status**: ✅ READY FOR DEVICE TESTING

---

### BUG-P0-002: Chat Creation Participant Selection May Not Work
**Status**: 🔴 NOT FIXED
**Severity**: P0 (Critical)
**Impact**: Cannot create chat rooms with specific participants
**Affects**: Chat Creation Flow, Team Communication

**Location**:
- `app/src/main/java/com/example/kosmos/features/chat/presentation/ChatScreens.kt` (CreateChatDialog)
- `app/src/main/java/com/example/kosmos/features/chat/presentation/redesign/EnhancedChatListScreen.kt`

**Description**:
Based on code analysis, create chat dialog exists but the participant selection UX needs verification. User search integration may not be properly connected.

**Expected Behavior**:
1. Click "Create Chat" in chat list
2. Dialog opens with chat name input
3. Participant search/selection UI displays
4. Can search for users by name/username
5. Selected users show as chips
6. Can remove selected users
7. "Create" button creates chat with selected participants
8. All participants added to `chat_participants` table

**Actual Behavior** (To Be Verified with Testing):
- Dialog may open
- Participant selection UI may be missing or non-functional
- May not allow adding multiple participants

**Fix Plan**:
1. Review existing CreateChatDialog implementation
2. Integrate UserSearch component for participant selection
3. Add multi-select capability with chip display
4. Ensure `ChatRepository.createChatRoom()` accepts participants list
5. Insert all participants into `chat_participants` table
6. Add error handling for empty participants, duplicate chats

**Files to Modify**:
- `ChatScreens.kt` - Enhance CreateChatDialog
- `ChatListViewModel.kt` - Add participant management state
- `ChatRepository.kt` - Verify createChatRoom() signature
- Consider reusing `UserSearchScreen` component

**Testing**:
- [ ] Dialog opens successfully
- [ ] Can search for users
- [ ] Can select multiple users
- [ ] Selected users display as chips
- [ ] Can remove selected users
- [ ] Chat created with all participants
- [ ] All participants in `chat_participants` table
- [ ] All participants can access chat

**Assigned To**: TBD
**ETA**: Day 4-5

---

### BUG-P0-003: Build Successful But Needs Device Testing
**Status**: ✅ BUILD SUCCESSFUL
**Severity**: P0 (Information)
**Impact**: Cannot verify actual functionality without device testing

**Description**:
Build completed successfully with warnings (deprecations) but no errors. However, many features need actual device testing to verify they work as expected.

**Build Output**:
```
BUILD SUCCESSFUL in 26s
42 actionable tasks: 17 executed, 25 from cache
```

**Warnings** (62 deprecation warnings):
- hiltViewModel() moved to new package
- Icons.Filled.* icons have AutoMirrored versions
- TabRow/ScrollableTabRow replaced with Primary/Secondary variants
- Divider renamed to HorizontalDivider
- AlertDialog deprecated in favor of BasicAlertDialog

**Action Required**:
1. Connect Android device or start emulator
2. Install APK: `./gradlew installDebug`
3. Begin systematic testing per TESTING_LOGBOOK.md
4. Document actual bugs found during testing

**Fix Plan** (Warnings - Low Priority):
- Update to new hiltViewModel import (P3)
- Migrate to AutoMirrored icons (P3)
- Update TabRow to PrimaryTabRow/SecondaryTabRow (P3)
- Update deprecated compose APIs (P3)

**Assigned To**: TBD
**ETA**: Day 2-3 (Testing Phase)

---

## P1 - HIGH PRIORITY BUGS (Major Features Broken)

### BUG-P1-001: Chat Search Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot search within chat messages
**Affects**: Chat Screen UX

**Location**:
- `EnhancedChatScreen.kt:119`
- `EnhancedChatListScreen.kt:166`

**Description**:
Two search features marked as TODO:
1. Search icon in chat screen (search within messages)
2. Search icon in chat list screen (search for chat rooms)

**Expected Behavior**:
1. Click search icon in chat
2. Search bar appears
3. Type query
4. Messages matching query highlight
5. Can navigate between matches

**Fix Plan**:
1. Add search state to ChatViewModel
2. Implement message filtering by content
3. Add search UI with input field
4. Highlight matching messages
5. Add navigation between matches

**Files to Modify**:
- `EnhancedChatScreen.kt`
- `ChatViewModel.kt`
- `EnhancedChatListScreen.kt`
- `ChatListViewModel.kt`

**Assigned To**: TBD
**ETA**: Day 5

---

### BUG-P1-002: Edit Project Screen Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot edit project name or description after creation
**Affects**: Project Management

**Location**:
- `MainActivity.kt:147`
- `ProjectDetailsScreenWrapper.kt` (Edit button exists)

**Description**:
Edit project button shows in ProjectDetailsScreen but navigation is not implemented.

**Expected Behavior**:
1. Click Edit icon in project details
2. Navigate to EditProjectScreen
3. Can update name, description
4. Save updates to Supabase
5. Changes reflect immediately

**Fix Plan**:
1. Create `EditProjectScreen` composable
2. Pre-populate with existing project data
3. Add validation (name required)
4. Add `updateProject()` to ProjectRepository
5. Update ProjectViewModel
6. Add navigation route

**Files to Create/Modify**:
- Create: `EditProjectScreen.kt`
- `MainActivity.kt` - Add navigation route
- `ProjectViewModel.kt` - Add updateProject()
- `ProjectRepository.kt` - Add update logic

**Assigned To**: TBD
**ETA**: Day 5

---

### BUG-P1-003: Project-Level Task View Not Accessible
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Can only view tasks within chat rooms, not project-wide
**Affects**: Task Management

**Location**:
- `MainActivity.kt:132`

**Description**:
Navigation to project-level task view is not implemented. Currently tasks are only viewable per chat room.

**Expected Behavior**:
1. From project details, click Tasks tab
2. See all tasks for entire project
3. Can filter by status, priority, assignee
4. Can create tasks at project level

**Fix Plan**:
1. MyTasksScreen already exists (redesigned)
2. Add navigation from ProjectDetailsScreen Tasks tab
3. Pass projectId to filter tasks
4. Ensure TaskRepository can filter by projectId

**Files to Modify**:
- `MainActivity.kt` - Add navigation
- `ProjectDetailsScreen.kt` - Wire up Tasks tab
- `TaskViewModel.kt` - Add project filtering

**Assigned To**: TBD
**ETA**: Day 5

---

### BUG-P1-004: Members Screen Navigation Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot access detailed members management screen
**Affects**: Project Member Management

**Location**:
- `MainActivity.kt:135`
- `ProjectDetailsScreenWrapper.kt` (Members tab shows basic list)

**Description**:
Members tab in ProjectDetailsScreen shows basic list, but full members management screen navigation is TODO.

**Expected Behavior**:
1. Click member in Members tab
2. Navigate to member detail screen
3. Can view member role, joined date, activity
4. (ADMIN only) Can change role
5. (ADMIN only) Can remove member

**Fix Plan**:
1. Create `MemberDetailScreen` or enhance inline display
2. Add role change dialog (ADMIN only)
3. Add remove member confirmation (ADMIN only)
4. Enforce RBAC permissions
5. Update `project_members` table

**Files to Create/Modify**:
- Create: `MemberDetailScreen.kt` (optional)
- `ProjectDetailsScreen.kt` - Add member click handler
- `ProjectViewModel.kt` - Add updateMemberRole(), removeMember()
- `ProjectRepository.kt` - Implement member management

**Assigned To**: TBD
**ETA**: Day 6

---

### BUG-P1-005: Quick Task Creation Sheet Not Connected
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Quick task creation from project overview doesn't work
**Affects**: Task Creation UX

**Location**:
- `MainActivity.kt:141`
- `ProjectDetailsScreen.kt` - "Create Task" quick action

**Description**:
Quick task creation button exists in project overview but doesn't open the QuickTaskCreationSheet.

**Expected Behavior**:
1. Click "Create Task" in project overview
2. Bottom sheet opens
3. Quick form: Title, Priority, Assignee
4. Create task with minimal input
5. Task appears in tasks list

**Fix Plan**:
1. QuickTaskCreationSheetWrapper already exists
2. Add state to show/hide sheet
3. Connect button to show sheet
4. Wire up task creation to TaskViewModel
5. Ensure projectId is passed correctly

**Files to Modify**:
- `ProjectDetailsScreenWrapper.kt` - Add sheet state
- `ProjectDetailsScreen.kt` - Connect button
- Verify `QuickTaskCreationSheet` works correctly

**Assigned To**: TBD
**ETA**: Day 5

---

### BUG-P1-006: Edit Profile Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot update user profile after signup
**Affects**: User Profile Management

**Location**:
- `ProfileScreen.kt:131`

**Description**:
Edit Profile button exists but doesn't navigate or open editor.

**Expected Behavior**:
1. Click "Edit Profile"
2. Navigate to EditProfileScreen or open dialog
3. Can update: display name, bio, role, location, social links
4. Can upload new avatar (Supabase Storage)
5. Save updates to `users` table
6. Changes reflect immediately

**Fix Plan**:
1. Create `EditProfileScreen` composable
2. Pre-populate with current user data
3. Add avatar upload (Supabase Storage bucket)
4. Add validation
5. Add `updateProfile()` to UserRepository
6. Update AuthViewModel

**Files to Create/Modify**:
- Create: `EditProfileScreen.kt`
- `MainActivity.kt` - Add navigation route
- `AuthViewModel.kt` - Add updateProfile()
- `UserRepository.kt` - Add update logic
- Consider reusing SignUpScreen field components

**Assigned To**: TBD
**ETA**: Day 6

---

### BUG-P1-007: Privacy Settings Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot control privacy preferences
**Affects**: User Privacy

**Location**:
- `ProfileScreen.kt:140`

**Description**:
Privacy Settings button exists but is not implemented.

**Expected Behavior**:
1. Click "Privacy Settings"
2. Navigate to settings screen
3. Options: Online status visibility, Last seen, Profile visibility
4. Save preferences
5. Enforce privacy rules

**Fix Plan**:
1. Create `PrivacySettingsScreen`
2. Add settings to `users` table or new `user_settings` table
3. Add toggle switches for each setting
4. Update presence logic to respect settings
5. Update profile visibility queries

**Files to Create/Modify**:
- Create: `PrivacySettingsScreen.kt`
- `MainActivity.kt` - Add navigation
- Schema: Add privacy columns to users table
- `UserRepository.kt` - Add settings CRUD

**Assigned To**: TBD
**ETA**: Day 6 (Lower priority than core features)

---

### BUG-P1-008: Notification Settings Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P1 (High)
**Impact**: Cannot control notification preferences
**Affects**: User Notifications

**Location**:
- `ProfileScreen.kt:149`

**Description**:
Notifications button exists but is not implemented.

**Expected Behavior**:
1. Click "Notifications"
2. Navigate to notification settings
3. Options: Message notifications, Task notifications, Mention notifications
4. Per-project notification settings
5. Save preferences
6. FCM respects settings

**Fix Plan**:
1. Create `NotificationSettingsScreen`
2. Add settings table
3. Add toggle switches
4. Integrate with FCM token management
5. Server-side: Check settings before sending

**Files to Create/Modify**:
- Create: `NotificationSettingsScreen.kt`
- `MainActivity.kt` - Add navigation
- Schema: Create `notification_settings` table
- FCM integration updates

**Assigned To**: TBD
**ETA**: Day 6 (Lower priority)

---

## P2 - MEDIUM PRIORITY BUGS (Minor Features, Workarounds Exist)

### BUG-P2-001: Chat Settings Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot access chat-specific settings
**Affects**: Chat Customization

**Location**:
- `EnhancedChatScreen.kt:129`

**Description**:
More menu (⋮) in chat screen should open settings.

**Expected Behavior**:
1. Click more menu
2. Open chat settings
3. Options: Rename chat, Manage participants, Notifications, Leave chat
4. Save changes

**Fix Plan**:
1. Create `ChatSettingsDialog` or sheet
2. Add chat management options
3. Wire to ChatViewModel
4. Update `chat_rooms` table

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-002: Copy Message to Clipboard Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot copy message text easily
**Affects**: Message UX

**Location**:
- `EnhancedChatScreen.kt:287`

**Description**:
Context menu has "Copy" option but it's not implemented.

**Expected Behavior**:
1. Long-press message
2. Select "Copy"
3. Message text copied to clipboard
4. Toast: "Copied to clipboard"

**Fix Plan**:
1. Use Android ClipboardManager
2. Add copy action to ChatViewModel
3. Show confirmation toast
4. Handle emoji and special characters

**Files to Modify**:
- `EnhancedChatScreen.kt`
- `ChatViewModel.kt`

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-003: Reply to Message Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot create threaded conversations
**Affects**: Chat UX

**Location**:
- `EnhancedChatScreen.kt:291`

**Description**:
Context menu has "Reply" option but it's not implemented.

**Expected Behavior**:
1. Long-press message
2. Select "Reply"
3. Message input shows reply context
4. Reply references original message
5. UI shows reply thread

**Fix Plan**:
1. Add `reply_to_message_id` column to messages table
2. Add reply state to ChatViewModel
3. Show reply preview in message input
4. Display reply context in message bubble
5. Can click reply to jump to original message

**Files to Modify**:
- Schema: Add reply_to_message_id to messages
- `EnhancedChatScreen.kt`
- `ChatViewModel.kt`
- `Message` model
- `MessageBubble` component

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-004: Activity Feed Not Loading Data
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Activity tab shows placeholder
**Affects**: Project Activity Tracking

**Location**:
- `ProjectDetailsScreenWrapper.kt:90`

**Description**:
Activity tab in project details doesn't load actual activity data.

**Expected Behavior**:
1. Click Activity tab
2. See recent project events: messages sent, tasks created, members joined, etc.
3. Shows user, action, timestamp
4. Can filter by event type

**Fix Plan**:
1. Create `project_activity` table or aggregate from existing tables
2. Add ActivityRepository
3. Create activity feed query
4. Display in chronological order
5. Add real-time updates

**Files to Create/Modify**:
- Schema: Create `project_activity` table or view
- Create: `ActivityRepository.kt`
- `ProjectViewModel.kt` - Add activity state
- `ProjectDetailsScreen.kt` - Display activity

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-005: Member Online Status Not Real-Time
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Online status may not update in real-time
**Affects**: Presence System

**Location**:
- `ProjectDetailsScreenWrapper.kt:86`

**Description**:
Member online status shows but may not update in real-time via Supabase presence.

**Expected Behavior**:
1. User goes online/offline
2. Status updates within 5 seconds
3. Green dot shows online users
4. Last seen shows for offline users

**Fix Plan**:
1. Implement Supabase Presence channel
2. Track user presence per project
3. Update presence on app lifecycle changes
4. Display real-time status in UI
5. Clean up stale presence records

**Files to Modify**:
- Create: `PresenceService.kt`
- `ProjectViewModel.kt` - Subscribe to presence
- `UserRepository.kt` - Manage presence
- MainActivity - Lifecycle hooks

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-006: Clear Cache Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot clear cached data
**Affects**: Settings, Storage Management

**Location**:
- `MainActivity.kt:440`

**Description**:
Clear Cache button shows confirmation dialog but doesn't actually clear cache.

**Expected Behavior**:
1. Click "Clear Cache"
2. Confirm in dialog
3. Room database cache cleared (except user session)
4. Image cache cleared
5. Toast: "Cache cleared"
6. Cache size updates to 0

**Fix Plan**:
1. Call Room clearAllTables() (except user/session tables)
2. Clear Coil image cache
3. Clear Supabase client cache
4. Recalculate cache size
5. Show success message

**Files to Modify**:
- `MainActivity.kt`
- `KosmosDatabase.kt` - Add selective clear method

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-007: Error Snackbar Not Showing in TaskBoard
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Errors not communicated to user
**Affects**: Task Board Error Handling

**Location**:
- `TaskScreens.kt:160`

**Description**:
TODO comment indicates snackbar should show on error but it's not implemented.

**Expected Behavior**:
1. Error occurs (network, validation, etc.)
2. Snackbar appears at bottom
3. Shows error message
4. Optional Retry action
5. Auto-dismisses after 5 seconds

**Fix Plan**:
1. Add SnackbarHostState to TaskBoardScreen
2. Show snackbar on error state
3. Add retry action where applicable
4. Consistent error messaging

**Files to Modify**:
- `TaskScreens.kt`

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-008: Project Archive/Unarchive Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot archive completed projects
**Affects**: Project Management

**Location**:
- `ProjectDetailsScreen.kt` - More menu has Archive option

**Description**:
Archive/Unarchive option exists in menu but doesn't work.

**Expected Behavior**:
1. Click More → Archive Project
2. Confirmation dialog
3. Project hidden from default list
4. Can view archived projects separately
5. Can unarchive later

**Fix Plan**:
1. Add `is_archived` column to projects table
2. Add archive/unarchive to ProjectRepository
3. Filter archived projects from list by default
4. Add "View Archived" option in ProjectList
5. Update UI for archived project badge

**Files to Modify**:
- Schema: Add is_archived to projects
- `ProjectRepository.kt`
- `ProjectViewModel.kt`
- `ProjectDetailsScreen.kt`
- `ProjectListScreen.kt` - Add filter toggle

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-009: User Name Lookup Using Placeholder
**Status**: 🟡 WORKAROUND EXISTS
**Severity**: P2 (Medium)
**Impact**: May show "Unknown User" in some places
**Affects**: Data Display

**Location**:
- `ProjectDataMapper.kt:53`

**Description**:
User name lookup uses placeholder logic, may not fetch actual names.

**Expected Behavior**:
User names display correctly everywhere.

**Fix Plan**:
1. Implement proper user lookup in mapper
2. Cache user data in Room
3. Fetch missing user data on demand
4. Update mapper to use cached data

**Files to Modify**:
- `ProjectDataMapper.kt`
- `UserRepository.kt` - Add lookup methods

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-010: Unread Count Not Implemented
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: Cannot see unread message counts
**Affects**: Chat List UX

**Location**:
- `ProjectRepository.kt:551, 578`

**Description**:
Unread count logic is TODO in repository.

**Expected Behavior**:
1. Chat rooms show unread message count badge
2. Count updates in real-time
3. Clears when messages marked as read
4. Projects show total unread count

**Fix Plan**:
1. Track last_read_message_id per user per chat
2. Count messages after last read
3. Update on real-time message events
4. Display badges in UI

**Files to Modify**:
- Schema: Add user_chat_read_status table
- `ChatRepository.kt`
- `ProjectRepository.kt`
- UI: Add badge to chat cards

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-011: No Offline Indicator
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: User doesn't know when offline
**Affects**: Offline Mode UX

**Description**:
App works offline but doesn't indicate offline state to user.

**Expected Behavior**:
1. Network disconnects
2. Banner shows "You're offline"
3. Sync indicator shows pending operations
4. Network reconnects
5. Banner shows "Back online, syncing..."
6. Syncing indicator appears

**Fix Plan**:
1. Monitor network connectivity
2. Show persistent banner when offline
3. Track pending operations
4. Show sync progress when online
5. Dismiss banner when synced

**Files to Create/Modify**:
- Create: `NetworkMonitor.kt`
- Add offline banner to MainActivity
- Add sync state to ViewModels

**Assigned To**: TBD
**ETA**: Day 7

---

### BUG-P2-012: No Sync Status Indicator
**Status**: 🔴 NOT FIXED
**Severity**: P2 (Medium)
**Impact**: User doesn't know if data is synced
**Affects**: Data Sync UX

**Description**:
Hybrid sync works but no visual feedback on sync status.

**Expected Behavior**:
1. Creating/updating data shows sync icon
2. Icon animates while syncing
3. Checkmark shows when synced
4. Error icon if sync fails with retry

**Fix Plan**:
1. Add sync state to each operation
2. Show sync indicator in UI
3. Handle sync conflicts
4. Add retry mechanism

**Files to Modify**:
- All repositories - Add sync state
- UI components - Add sync indicators
- ViewModels - Expose sync state

**Assigned To**: TBD
**ETA**: Day 7

---

## P3 - LOW PRIORITY BUGS (Cosmetic, Nice-to-Have)

### BUG-P3-001: Deprecated API Usage (62 Warnings)
**Status**: 🟡 BUILDS SUCCESSFULLY
**Severity**: P3 (Low)
**Impact**: Future compatibility issues
**Affects**: Build, Code Quality

**Description**:
Build produces 62 deprecation warnings for outdated Compose and library APIs.

**Warnings**:
1. hiltViewModel() moved to new package (15 occurrences)
2. Icons.Filled.* should use AutoMirrored versions (30+ occurrences)
3. TabRow/ScrollableTabRow replaced with Primary/Secondary variants (3 occurrences)
4. Divider renamed to HorizontalDivider (2 occurrences)
5. AlertDialog deprecated → BasicAlertDialog (1 occurrence)
6. statusBarColor deprecated (1 occurrence)

**Fix Plan** (Low Priority):
1. Update hiltViewModel imports
2. Migrate to AutoMirrored icons
3. Update TabRow to PrimaryTabRow
4. Replace Divider with HorizontalDivider
5. Update AlertDialog usages
6. Remove deprecated statusBarColor usage

**Assigned To**: TBD
**ETA**: Day 8 (After all functional bugs fixed)

---

### BUG-P3-002: Theme Selection Not Available
**Status**: 🔴 NOT IMPLEMENTED
**Severity**: P3 (Low)
**Impact**: Stuck with system theme
**Affects**: Settings, Personalization

**Location**:
- `MainActivity.kt` SettingsScreen - Placeholder message

**Description**:
Dark/Light theme toggle not implemented in settings.

**Expected Behavior**:
1. Settings → Theme
2. Options: System, Light, Dark
3. Save preference
4. App updates theme immediately

**Fix Plan**:
1. Add theme preference to SharedPreferences
2. Add ThemeManager
3. Add theme switcher UI
4. Apply theme in MainActivity

**Assigned To**: TBD
**ETA**: Day 8

---

### BUG-P3-003: Online Status Toggle Not Available
**Status**: 🔴 NOT IMPLEMENTED
**Severity**: P3 (Low)
**Impact**: Cannot manually go invisible
**Affects**: Settings, Privacy

**Description**:
Cannot manually set online/offline status.

**Expected Behavior**:
Settings → Online Status toggle

**Fix Plan**:
Include in Privacy Settings implementation (BUG-P1-007)

**Assigned To**: TBD
**ETA**: Day 8

---

### BUG-P3-004: No Data Usage Statistics
**Status**: 🔴 NOT IMPLEMENTED
**Severity**: P3 (Low)
**Impact**: Cannot track data usage
**Affects**: Settings, Monitoring

**Description**:
No visibility into data usage (bandwidth, storage, API calls).

**Expected Behavior**:
Settings → Data Usage → See stats

**Fix Plan**:
1. Track API call counts
2. Track bandwidth usage
3. Display in settings
4. Show Supabase quota usage

**Assigned To**: TBD
**ETA**: Day 8

---

### BUG-P3-005: Voice Messages Disabled for MVP
**Status**: 🟡 INTENTIONALLY DISABLED
**Severity**: P3 (Low)
**Impact**: Cannot send voice messages
**Affects**: Chat Features

**Description**:
Mic button exists but is disabled. Voice transcription code exists but is not active.

**Expected Behavior** (Future):
1. Hold mic button to record
2. Release to send
3. Audio uploads to Supabase Storage
4. Transcription via Google Cloud Speech API
5. Recipients can play audio or read transcript

**Fix Plan** (Post-MVP):
1. Enable audio recording permissions
2. Implement audio recording UI
3. Upload to Supabase Storage
4. Trigger transcription
5. Display audio player in chat

**Assigned To**: TBD
**ETA**: Post-MVP

---

### BUG-P3-006: Image/File Attachments Disabled
**Status**: 🟡 INTENTIONALLY DISABLED
**Severity**: P3 (Low)
**Impact**: Cannot share images or files
**Affects**: Chat Features

**Description**:
No UI for image/file attachments. Message types exist in model (IMAGE, FILE) but not implemented.

**Expected Behavior** (Future):
1. Click attachment button
2. Choose image or file
3. Upload to Supabase Storage
4. Send message with attachment URL
5. Recipients can view/download

**Fix Plan** (Post-MVP):
1. Add attachment button to message input
2. Implement file picker
3. Upload to Supabase Storage
4. Create message with attachment
5. Display images inline, files as download cards

**Assigned To**: TBD
**ETA**: Post-MVP

---

### BUG-P3-007: Task Dependencies Not Supported
**Status**: 🔴 NOT IMPLEMENTED
**Severity**: P3 (Low)
**Impact**: Cannot link related tasks
**Affects**: Task Management

**Description**:
Cannot create task dependencies (Task A blocks Task B).

**Expected Behavior** (Future):
Task editor → Add dependency → Select blocking task

**Fix Plan** (Post-MVP):
1. Add task_dependencies table
2. Add dependency picker UI
3. Validate no circular dependencies
4. Display dependency graph
5. Block task status changes if dependencies not done

**Assigned To**: TBD
**ETA**: Post-MVP

---

## TESTING REQUIRED

The following issues require device testing to verify:

1. **BUG-P0-002**: Chat creation participant selection
2. **All real-time features**: Message delivery, typing, reactions
3. **Offline mode**: Data caching, sync queue
4. **Authentication flow**: Signup, login, logout
5. **RBAC enforcement**: Role permissions
6. **Performance**: Startup time, message latency, memory usage

**Testing Device Required**:
- Android device or emulator with API 26+
- Connect via `adb devices`
- Install with `./gradlew installDebug`

---

## FIX PRIORITY ORDER

### Week 1: Critical Fixes (P0)
1. ✅ Build and verify app installs
2. Implement "Add to Project" feature
3. Fix/verify chat creation with participants
4. Begin device testing

### Week 2: High Priority (P1)
5. Implement edit profile
6. Add project-level task view navigation
7. Implement quick task creation connection
8. Add chat search functionality
9. Implement edit project screen
10. Add members management

### Week 3: Medium Priority (P2)
11. Implement activity feed
12. Add real-time presence
13. Implement chat settings
14. Add copy/reply message features
15. Implement clear cache
16. Add unread count tracking
17. Add offline/sync indicators

### Week 4: Low Priority & Polish (P3)
18. Fix deprecation warnings
19. Add theme selection
20. Add privacy/notification settings
21. Final testing and bug fixes

---

## NOTES

- All bugs are documented with exact file locations
- Fix plans include specific files to modify
- Testing criteria defined for each fix
- Priority based on user impact and app usability
- Build is successful - focus on functional bugs first
- Device testing required after initial fixes

---

**End of Bug Tracker**
