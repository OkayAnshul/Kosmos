# TEST DATA SETUP GUIDE

**Purpose**: Step-by-step guide to populate the Kosmos app with test data for comprehensive testing
**Last Updated**: 2025-11-02

---

## PREREQUISITES

### 1. Build & Install App
```bash
cd /home/anshul/WORK/DEVELOPEMENT/ANDROID-DEV/Projects/Kosmos
./gradlew clean
./gradlew assembleDebug
./gradlew installDebug
```

### 2. Verify Supabase Connection
- Open Supabase Dashboard: https://app.supabase.com
- Verify project is active
- Check tables exist:
  - `users`
  - `projects`
  - `project_members`
  - `chat_rooms`
  - `chat_participants`
  - `messages`
  - `tasks`

### 3. Clear Existing Test Data (Optional)
If you need a clean slate, run these SQL queries in Supabase SQL Editor:

```sql
-- WARNING: This deletes ALL data!
DELETE FROM messages;
DELETE FROM chat_participants;
DELETE FROM chat_rooms;
DELETE FROM tasks;
DELETE FROM project_members;
DELETE FROM projects;
DELETE FROM users WHERE email LIKE 'test%@kosmos.app';
```

---

## PHASE 1: CREATE TEST USERS

### User 1: Primary Test Account (Admin)
1. Open Kosmos app
2. Tap "Sign Up"
3. Fill in **required fields**:
   - Display Name: `Test User One`
   - Username: `testuser1` (app adds @ automatically)
   - Email: `test1@kosmos.app`
   - Password: `password123`
   - Confirm Password: `password123`
4. Expand **optional fields** (tap to expand)
5. Fill in optional fields:
   - Age: `28`
   - Role/Title: `Product Manager`
   - Location: `San Francisco, CA`
   - Bio: `Test account for primary testing and admin scenarios. Owner of Project Alpha.`
   - GitHub: `github.com/testuser1`
   - Website: `testuser1.dev`
6. Tap "Create Account"
7. **Verify**: Navigates to Project List screen
8. **Verify**: Username shows as `@testuser1` in Supabase `users` table
9. Log out: Settings → Logout

### User 2: Secondary Test Account (Manager)
1. Tap "Sign Up"
2. Fill in **required fields**:
   - Display Name: `Test User Two`
   - Username: `testuser2`
   - Email: `test2@kosmos.app`
   - Password: `password123`
   - Confirm Password: `password123`
3. Fill in optional fields:
   - Role/Title: `Developer`
   - Location: `New York, NY`
   - Bio: `Test account for manager role testing. Owner of Project Beta.`
4. Tap "Create Account"
5. **Verify**: Account created successfully
6. Log out

### User 3: Tertiary Test Account (Member)
1. Tap "Sign Up"
2. Fill in **required fields**:
   - Display Name: `Test User Three`
   - Username: `testuser3`
   - Email: `test3@kosmos.app`
   - Password: `password123`
   - Confirm Password: `password123`
3. Fill in optional fields:
   - Role/Title: `Designer`
   - Location: `Austin, TX`
   - Bio: `Test account for member role testing and collaboration scenarios.`
4. Tap "Create Account"
5. **Verify**: Account created successfully
6. Log out

### Verification Checklist
- [ ] All 3 users created successfully
- [ ] Username availability check worked during signup
- [ ] All users appear in Supabase `users` table
- [ ] Passwords are hashed (not plain text)
- [ ] Each user has unique `id` (UUID)

---

## PHASE 2: CREATE TEST PROJECTS

### Login as Test User One
```
Email: test1@kosmos.app
Password: password123
```

### Project 1: "Project Alpha" (User 1 as ADMIN)
1. From Project List, tap FAB (+) button
2. Fill in Create Project dialog:
   - Name: `Project Alpha`
   - Description: `Comprehensive test project for feature testing. Includes chat rooms, tasks, and multiple team members with different roles.`
3. Tap "Create"
4. **Verify**: Project appears in Project List
5. **Verify**: Project card shows:
   - Name: "Project Alpha"
   - Description (truncated)
   - "OWNER" badge
   - Created date (today)
6. Tap project to open details
7. **Verify**: All 5 tabs visible (Overview, Chats, Tasks, Members, Activity)
8. **Verify**: Stats show: 0 Chats, 0 Tasks, 1 Member (you)
9. Navigate back to Project List

### Logout and Login as Test User Two
```
Email: test2@kosmos.app
Password: password123
```

### Project 2: "Project Beta" (User 2 as ADMIN)
1. Tap FAB (+) button
2. Fill in Create Project dialog:
   - Name: `Project Beta`
   - Description: `Secondary test project for multi-user collaboration scenarios and cross-project testing.`
3. Tap "Create"
4. **Verify**: Project appears in list
5. Navigate back to Project List

### Verification Checklist
- [ ] 2 projects created
- [ ] Each project has correct owner
- [ ] Projects appear in Supabase `projects` table
- [ ] Owners auto-added to `project_members` with ADMIN role
- [ ] Each project has unique `id` (UUID)

---

## PHASE 3: ADD MEMBERS TO PROJECTS

> **IMPORTANT**: This step tests the "Add to Project" feature which is currently **NOT IMPLEMENTED** (TODO at UserProfileScreen.kt:185).
>
> **Workaround**: Manually insert into Supabase `project_members` table using SQL until feature is implemented.

### Manual Workaround (Temporary)

#### Get User IDs
Run this query in Supabase SQL Editor:
```sql
SELECT id, email, display_name FROM users
WHERE email IN ('test1@kosmos.app', 'test2@kosmos.app', 'test3@kosmos.app')
ORDER BY email;
```

**Copy the UUIDs**:
- test1@kosmos.app → `[USER_1_ID]`
- test2@kosmos.app → `[USER_2_ID]`
- test3@kosmos.app → `[USER_3_ID]`

#### Get Project IDs
```sql
SELECT id, name, owner_id FROM projects
WHERE name IN ('Project Alpha', 'Project Beta')
ORDER BY name;
```

**Copy the UUIDs**:
- Project Alpha → `[PROJECT_ALPHA_ID]`
- Project Beta → `[PROJECT_BETA_ID]`

#### Add Members to Project Alpha
```sql
-- Add User 2 as MANAGER to Project Alpha
INSERT INTO project_members (project_id, user_id, role, joined_at)
VALUES ('[PROJECT_ALPHA_ID]', '[USER_2_ID]', 'MANAGER', NOW());

-- Add User 3 as MEMBER to Project Alpha
INSERT INTO project_members (project_id, user_id, role, joined_at)
VALUES ('[PROJECT_ALPHA_ID]', '[USER_3_ID]', 'MEMBER', NOW());
```

#### Add Member to Project Beta
```sql
-- Add User 1 as MEMBER to Project Beta
INSERT INTO project_members (project_id, user_id, role, joined_at)
VALUES ('[PROJECT_BETA_ID]', '[USER_1_ID]', 'MEMBER', NOW());
```

### Verify in App

**Login as User 1**:
- [ ] See both "Project Alpha" and "Project Beta" in list
- [ ] Project Alpha shows "OWNER" badge
- [ ] Project Beta shows "MEMBER" badge

**Login as User 2**:
- [ ] See both projects
- [ ] Project Alpha shows "MANAGER" badge
- [ ] Project Beta shows "OWNER" badge

**Login as User 3**:
- [ ] See "Project Alpha" only
- [ ] Shows "MEMBER" badge

### When Feature is Implemented
1. Login as User 1
2. Navigate to Project Alpha → Members tab
3. Tap "Invite" button
4. Search for "testuser2"
5. Select user from results
6. Choose role: MANAGER
7. Tap "Add to Project"
8. Repeat for testuser3 with MEMBER role

---

## PHASE 4: CREATE CHAT ROOMS

### Login as Test User One
```
Email: test1@kosmos.app
Password: password123
```

### Chat 1: "General" (in Project Alpha)
1. Open "Project Alpha"
2. Go to "Chats" tab
3. Tap "Create Chat" or FAB (+)
4. Fill in dialog:
   - Chat Name: `General`
   - **Add Participants**:
     - Search "testuser2" → select
     - Search "testuser3" → select
5. Tap "Create"
6. **Verify**: Chat appears in list
7. **Verify**: Can navigate to chat screen
8. Navigate back

> **NOTE**: If participant selection doesn't work, this indicates the create chat UX needs fixing (known issue).

### Chat 2: "Development" (in Project Alpha)
1. In Chats tab, tap FAB (+)
2. Chat Name: `Development`
3. Add Participants: testuser2 only
4. Tap "Create"
5. **Verify**: Chat created

### Chat 3: "Design Review" (in Project Alpha)
1. In Chats tab, tap FAB (+)
2. Chat Name: `Design Review`
3. Add Participants: testuser3 only
4. Tap "Create"
5. **Verify**: Chat created

### Chat 4: "Beta Team Chat" (in Project Beta)
1. Navigate to "Project Beta"
2. Go to Chats tab
3. Tap FAB (+)
4. Chat Name: `Beta Team Chat`
5. Add Participants: testuser1 (you're user 2 now)
6. Tap "Create"
7. **Verify**: Chat created

### Verification Checklist
- [ ] 4 chat rooms created total
- [ ] 3 chats in Project Alpha
- [ ] 1 chat in Project Beta
- [ ] All chats in Supabase `chat_rooms` table
- [ ] Participants in `chat_participants` table
- [ ] Can navigate to each chat screen

---

## PHASE 5: SEND TEST MESSAGES

### In "General" Chat (Project Alpha)

**Login as User 1** and send:
1. `Hey team! Welcome to Project Alpha 👋`
2. `Let's use this channel for general updates and announcements.`
3. `Who's working on the authentication feature?`

**Login as User 2** and send:
4. `Hi everyone! Excited to be here.`
5. `@testuser1 I can take the authentication feature.`
6. `I've done similar work before.`

**Login as User 3** and send:
7. `Hello team! Happy to help with design.`
8. `Should I create mockups for the login screen?`

**Login as User 1** and send:
9. `@testuser3 Yes please! That would be great.`
10. `Let's aim to have the designs by end of week.`

**Add Reactions**:
- User 2: React to message #10 with 👍
- User 3: React to message #8 with ✅
- User 1: React to message #5 with 🎉

**Edit a Message**:
- User 1: Long-press message #3, select "Edit"
- Change to: `Who's working on the authentication feature? @testuser2`
- Save

**Delete a Message**:
- User 1: Send `This is a test message to delete`
- Long-press → Delete
- Verify it's removed

### In "Development" Chat (Project Alpha)

**Login as User 1** and send:
1. `Let's discuss the authentication implementation here.`
2. `We need to support email/password and Google Sign-In.`

**Login as User 2** and send:
3. `Got it. I'll use Firebase Auth for this.`
4. `Should be straightforward. ETA: 3 days.`

**Login as User 1** and send:
5. `Perfect! Let me know if you need any help.`

### In "Design Review" Chat (Project Alpha)

**Login as User 1** and send:
1. `Hi @testuser3! This is where we'll review designs.`

**Login as User 3** and send:
2. `Sounds good! I'll share the login screen mockup soon.`

### In "Beta Team Chat" (Project Beta)

**Login as User 2** and send:
1. `Welcome to Project Beta!`
2. `This is a smaller project for testing purposes.`

**Login as User 1** and send:
3. `Thanks for adding me!`

### Verification Checklist
- [ ] All messages sent successfully
- [ ] Messages appear immediately (optimistic UI)
- [ ] Messages sync to Supabase `messages` table
- [ ] Reactions display correctly
- [ ] Edit shows "edited" badge
- [ ] Deleted message removed from all clients
- [ ] Message grouping works (same sender < 5 min)
- [ ] Timestamps display correctly
- [ ] Scroll to bottom button appears when needed

---

## PHASE 6: TEST REAL-TIME FEATURES

### Setup: 2 Devices/Emulators
- **Device A**: User 1 logged in
- **Device B**: User 2 logged in

### Test Typing Indicators
1. **Device A**: Navigate to "General" chat
2. **Device B**: Navigate to "General" chat
3. **Device B**: Start typing in message input
4. **Device A**: Verify typing indicator appears: "testuser2 is typing..."
5. **Device B**: Stop typing for 3 seconds
6. **Device A**: Verify typing indicator disappears

### Test Real-Time Message Delivery
1. **Device A**: Send message "Real-time test from User 1"
2. **Device B**: Verify message appears instantly (< 500ms)
3. **Device B**: Send message "Real-time test from User 2"
4. **Device A**: Verify message appears instantly

### Test Real-Time Reactions
1. **Device A**: React to a message with 😊
2. **Device B**: Verify reaction appears instantly
3. **Device B**: Click same reaction to toggle
4. **Device A**: Verify reaction count updated

### Test Real-Time Edits
1. **Device A**: Send message "Original text"
2. **Device B**: Verify message received
3. **Device A**: Edit message to "Edited text"
4. **Device B**: Verify message updates with "edited" badge

### Verification Checklist
- [ ] Typing indicators work in both directions
- [ ] Messages appear in real-time (< 500ms)
- [ ] Reactions update in real-time
- [ ] Edits update in real-time
- [ ] Deletes remove in real-time
- [ ] No duplicate messages
- [ ] No lag or performance issues

---

## PHASE 7: CREATE TEST TASKS

### In Project Alpha (as User 1)

#### Task 1: High Priority TODO
1. Navigate to Project Alpha → Tasks tab
2. Tap FAB (+) or "Create Task"
3. Fill in:
   - Title: `Design UI mockups for login screen`
   - Description: `Create high-fidelity mockups for the login screen including email/password and Google Sign-In options. Follow Material Design 3 guidelines.`
   - Priority: **High** (tap chip)
   - Assign to: testuser3 (tap → select from list)
   - Due Date: [Select 3 days from today]
   - Tags: `design`, `ui`, `login`
4. Tap "Create Task"
5. **Verify**: Task appears in Tasks tab with High priority badge

#### Task 2: Urgent IN_PROGRESS
1. Create another task:
   - Title: `Implement Firebase Authentication`
   - Description: `Set up Firebase Auth with email/password and Google Sign-In providers. Include error handling and session management.`
   - Priority: **Urgent**
   - Assign to: testuser2
   - Due Date: [Select 5 days from today]
   - Tags: `development`, `backend`, `auth`
2. After creating, **click the task** to open EditTaskDialog
3. Change Status: IN_PROGRESS (if status picker available)
4. Add Comment: `Started working on this. Firebase project is set up.`
5. Tap "Save"

#### Task 3: Medium TODO
1. Create task:
   - Title: `Write unit tests for authentication flow`
   - Description: `Create comprehensive unit tests for login, signup, and logout functionality. Aim for 80%+ code coverage.`
   - Priority: **Medium**
   - Assign to: testuser2
   - Due Date: [Select 7 days from today]
   - Tags: `testing`, `qa`
2. Create task

#### Task 4: Low TODO
1. Create task:
   - Title: `Update README documentation`
   - Description: `Add setup instructions, API documentation, and contribution guidelines to the README.`
   - Priority: **Low**
   - Assign to: testuser1 (yourself)
   - Due Date: [Select 10 days from today]
   - Tags: `documentation`
2. Create task

#### Task 5: DONE Task
1. Create task:
   - Title: `Setup development environment`
   - Description: `Install Android Studio, configure emulators, and set up Supabase connection.`
   - Priority: **Medium**
   - Assign to: testuser1
   - Due Date: [Yesterday's date]
2. Click task to edit
3. Change Status: DONE
4. Add Comment: `Environment fully configured and tested.`
5. Save

### In Project Beta (as User 2)

#### Task 6: High Priority TODO
1. Navigate to Project Beta → Tasks tab
2. Create task:
   - Title: `Research competitors and feature gaps`
   - Description: `Analyze competing apps and identify features we should prioritize.`
   - Priority: **High**
   - Assign to: testuser1
   - Due Date: [Select 4 days from today]
   - Tags: `research`, `product`
3. Create task

### Verification Checklist
- [ ] 6 tasks created total (5 in Alpha, 1 in Beta)
- [ ] Tasks show correct priority badges
- [ ] Assigned users display correctly
- [ ] Due dates formatted correctly
- [ ] Tags appear as chips
- [ ] Status filters work (TODO, In Progress, Done tabs)
- [ ] "My Tasks" filter works
- [ ] Tasks sync to Supabase `tasks` table
- [ ] Comments saved correctly
- [ ] Task counts in project stats are accurate

---

## PHASE 8: TEST TASK MANAGEMENT FEATURES

### Test Task Filtering (as User 2)
1. Navigate to "My Tasks" (global view)
2. **Verify**: Only tasks assigned to testuser2 show
3. Tap Filter button
4. Select Status: **TODO**
5. Tap Apply
6. **Verify**: Only TODO tasks assigned to you show
7. Tap Filter → Select Priority: **High**
8. **Verify**: Only High priority TODO tasks show
9. Tap active filter chip to clear all
10. **Verify**: All your tasks show again

### Test Task Views
1. Tap view toggle (List ↔ Board)
2. **Verify List View**: Vertical scrolling task cards
3. **Verify Board View**: Kanban columns (TODO, In Progress, Done)
4. **Verify**: Tasks in correct columns based on status

### Test Task Updates
1. Click a task to edit
2. Change priority: Medium → High
3. Update description (add more text)
4. Add a comment: `Updated priority based on deadline.`
5. Change due date to tomorrow
6. Save
7. **Verify**: All changes saved
8. **Verify**: Changes synced to Supabase

### Test Task from Chat
1. Navigate to "Development" chat
2. Tap Task Board icon (top-right)
3. **Verify**: Opens TaskBoardScreen for this chat
4. **Verify**: Shows only tasks related to this chat/project
5. Create a task from here
6. **Verify**: Task appears in project tasks

### Verification Checklist
- [ ] Task filtering by status works
- [ ] Task filtering by priority works
- [ ] Multiple filters combine correctly
- [ ] List view displays all task details
- [ ] Board view shows Kanban columns
- [ ] Tasks in correct columns
- [ ] Task editing saves changes
- [ ] Comments feature works
- [ ] Task board from chat works
- [ ] Task counts update in real-time

---

## PHASE 9: TEST USER SEARCH & PROFILES

### Test User Search (as User 1)
1. Navigate to any Chat List screen
2. Tap Search icon (if available) or navigate to UserSearchScreen
3. **Test Case 1**: Search by display name
   - Type: `Test User Two`
   - **Verify**: testuser2 appears in results
4. Clear search
5. **Test Case 2**: Search by username
   - Type: `@testuser3`
   - **Verify**: testuser3 appears in results
6. Clear search
7. **Test Case 3**: Search by email
   - Type: `test2@kosmos.app`
   - **Verify**: testuser2 appears in results
8. **Test Case 4**: Partial search
   - Type: `test`
   - **Verify**: All 3 test users appear
9. **Test Case 5**: Case insensitive
   - Type: `TEST USER`
   - **Verify**: Results still appear
10. **Test Case 6**: No results
    - Type: `nonexistentuser`
    - **Verify**: "No users found" message

### Test User Profile (as User 1)
1. From search results, click on testuser2
2. **Verify UserProfileScreen opens**:
   - Avatar displays (120dp)
   - Display name: "Test User Two"
   - Email: test2@kosmos.app
   - Username: @testuser2
   - Online status indicator
   - "Member since" date
3. **Test "Start Chat" button**:
   - Tap "Start Chat"
   - **Verify**: Creates or navigates to 1:1 chat
   - **Verify**: Chat name is DM or user name
   - Send a test message
   - **Verify**: Message delivered
4. Navigate back to profile
5. **Test "Add to Project" button**:
   - Tap "Add to Project"
   - **Expected**: Opens project picker dialog (CURRENTLY NOT IMPLEMENTED)
   - **Actual**: Check if anything happens
   - **Document**: Bug if nothing happens

### Verification Checklist
- [ ] Search by name works
- [ ] Search by username works
- [ ] Search by email works
- [ ] Search is case-insensitive
- [ ] Partial search works
- [ ] Debounce prevents too many requests (300ms)
- [ ] "No results" state displays
- [ ] Error state displays on network error
- [ ] User profile displays all info
- [ ] "Start Chat" creates/navigates to chat
- [ ] 1:1 messages work
- [ ] "Add to Project" works (or documented as broken)

---

## PHASE 10: TEST SETTINGS & PROFILE

### Test Current User Profile
1. From any screen, navigate to Profile
2. **Verify**:
   - Profile picture displays
   - Display name shows
   - Email shows
   - All info matches current user
3. Tap "Edit Profile"
4. **Expected**: Edit profile screen opens (CURRENTLY NOT IMPLEMENTED)
5. **Document**: Feature status

### Test Settings
1. Navigate to Settings
2. **Verify App Information Card**:
   - App name: "Kosmos"
   - Version displays
   - Build type: "Debug"
3. **Verify Storage Card**:
   - Cache size displays (or placeholder)
4. Tap "Clear Cache"
5. **Verify**: Confirmation dialog opens
6. Cancel → dialog closes
7. Tap again → Confirm
8. **Expected**: Cache cleared (CURRENTLY NOT IMPLEMENTED)

### Test Logout
1. Scroll to Logout card
2. Tap "Logout" button
3. **Verify**: Warning dialog appears
4. Cancel → stays logged in
5. Tap Logout again → Confirm
6. **Verify**: Session cleared
7. **Verify**: Navigates to LoginScreen
8. **Verify**: Can't navigate back to app without login

### Verification Checklist
- [ ] Profile displays current user info
- [ ] Edit profile button exists (works or TODO)
- [ ] Settings screen loads
- [ ] App info displays correctly
- [ ] Clear cache has confirmation
- [ ] Logout has warning dialog
- [ ] Logout clears session completely
- [ ] Must re-login to access app

---

## PHASE 11: TEST OFFLINE MODE

### Test Offline Viewing
1. **While Online**: Navigate to General chat with messages
2. Enable Airplane Mode on device
3. **Verify**: Can still view cached messages
4. Navigate to Tasks
5. **Verify**: Can view cached tasks
6. Navigate to Projects
7. **Verify**: Can view cached projects

### Test Offline Operations
1. **While Offline**: In General chat
2. Type new message: `This message was sent offline`
3. Tap Send
4. **Verify**: Message appears in UI (optimistic)
5. **Verify**: Sync indicator shows pending (if implemented)
6. Create a task while offline
7. **Verify**: Task appears in UI

### Test Online Sync
1. Disable Airplane Mode (go online)
2. Wait 5-10 seconds
3. **Verify**: Offline message syncs to Supabase
4. **Verify**: Offline task syncs to Supabase
5. Check on another device:
   - **Verify**: Offline message appears
   - **Verify**: Offline task appears

### Verification Checklist
- [ ] Can view cached data offline
- [ ] Can send messages offline (queued)
- [ ] Can create tasks offline (queued)
- [ ] Optimistic UI shows pending operations
- [ ] Going online triggers auto-sync
- [ ] Offline operations sync successfully
- [ ] No data loss during offline period
- [ ] No duplicate messages after sync

---

## TEST DATA SUMMARY

After completing all phases, you should have:

### Users (3)
- test1@kosmos.app (ADMIN of Alpha, MEMBER of Beta)
- test2@kosmos.app (MANAGER of Alpha, ADMIN of Beta)
- test3@kosmos.app (MEMBER of Alpha)

### Projects (2)
- Project Alpha (Owner: User 1, Members: All 3 users)
- Project Beta (Owner: User 2, Members: User 1 & 2)

### Chat Rooms (4)
- General (Project Alpha, 3 participants, ~10 messages)
- Development (Project Alpha, 2 participants, ~5 messages)
- Design Review (Project Alpha, 2 participants, ~2 messages)
- Beta Team Chat (Project Beta, 2 participants, ~3 messages)

### Messages (~20 total)
- Mix of users
- Various lengths
- Some with reactions
- Some edited
- Some with @ mentions

### Tasks (6)
- 5 in Project Alpha (various statuses and priorities)
- 1 in Project Beta
- Assigned to different users
- Some with comments
- Mix of priorities: Low, Medium, High, Urgent
- Mix of statuses: TODO, IN_PROGRESS, DONE

### Real-Time Events Tested
- Typing indicators
- Message delivery
- Reactions
- Edits/Deletes
- Task updates

---

## TROUBLESHOOTING

### Can't Create Chat Room
**Symptom**: Create chat dialog doesn't allow adding participants
**Workaround**:
1. Check CreateChatDialog implementation
2. May need to manually insert into `chat_participants` table
3. Document as bug

### Can't Add Member to Project
**Symptom**: "Add to Project" button doesn't work
**Workaround**:
1. Use SQL INSERT into `project_members` (shown in Phase 3)
2. This is a known TODO (UserProfileScreen.kt:185)

### Messages Not Syncing
**Symptom**: Messages don't appear on other device
**Check**:
1. Supabase connection (API URL and key)
2. Real-time subscriptions enabled in Supabase project
3. Check logcat for errors: `adb logcat -s Supabase:*`

### Username Availability Always Shows Error
**Symptom**: Username check fails during signup
**Check**:
1. Supabase RPC function `check_username_availability` exists
2. Network connection is stable
3. Try a different username

### Tasks Not Filtering
**Symptom**: Filter doesn't apply to task list
**Check**:
1. Are there tasks matching the filter criteria?
2. Try different filter combinations
3. Pull-to-refresh to reload

---

## NEXT STEPS

After completing this data setup:

1. ✅ Proceed to systematic testing using TESTING_LOGBOOK.md
2. ✅ Test each screen's functionality
3. ✅ Document all bugs found
4. ✅ Prioritize bugs (P0-P3)
5. ✅ Fix critical issues
6. ✅ Re-test after fixes
7. ✅ Complete integration testing
8. ✅ Update DEVELOPMENT_LOGBOOK.md

---

**End of Test Data Setup Guide**
