# Kosmos App - Complete Design Brief for Figma/Design Tools

**Last Updated:** January 11, 2026
**Document Purpose:** Comprehensive design specification for UI/UX design in Figma or other design tools

---

## 1. PROJECT OVERVIEW

### What is Kosmos?

**Kosmos** is a **Team Collaboration & Project Management Platform** for Android that unifies:
- Project workspaces with role-based access control
- Real-time team messaging integrated with tasks
- Kanban-style task management with time tracking
- Offline-first architecture with real-time sync

**NOT a chat app** - It's a complete workspace tool where communication happens in project context.

**Target Users:**
- Software development teams managing sprints
- Remote teams coordinating tasks and updates
- Project-based organizations tracking progress
- Any team needing unified communication + task management

**Current Status:** Production-ready with 24 fully functional screens

---

## 2. COMPLETE SCREEN INVENTORY (24 Screens)

### Authentication Screens (2)

#### Screen 1: Login Screen
**Purpose:** User authentication with email/password or Google Sign-In

**UI Elements:**
- App logo with brand name "Kosmos"
- Subtitle tagline
- Email input field with envelope icon
- Password input field with lock icon and show/hide toggle
- "Remember Me" checkbox
- "Forgot Password?" link
- Login button (primary CTA)
- "Don't have an account? Sign Up" link
- Error message card (shown on validation failure)

**User Actions:**
- Enter email and password
- Toggle password visibility
- Check "Remember Me"
- Click "Forgot Password" (future feature)
- Click Login button
- Navigate to Sign Up screen

**States:**
- Default state
- Loading state (during authentication)
- Error state (invalid credentials, network error)
- Success state (redirect to Projects)

---

#### Screen 2: Sign Up Screen
**Purpose:** User registration with comprehensive profile setup

**UI Elements:**
- App logo (smaller, 64dp)
- Large heading "Let's get started."
- Subtitle text

**Required Fields (5):**
- Display Name input
- Username input with @ prefix and availability checker (green checkmark / red X)
- Email input
- Password input with toggle
- Confirm Password input with toggle

**Optional Fields Toggle Button:** "Add optional info" (expands/collapses section)

**Optional Fields (12) - Collapsed by Default:**
- Age (number input)
- Role/Title (text)
- Location (text)
- Bio (multiline, 500 char limit with counter)
- Social Links Section (expandable):
  - GitHub URL
  - Twitter/X URL
  - LinkedIn URL
  - Website URL
  - Portfolio URL

**Buttons:**
- Create Account (primary CTA, disabled until validation passes)
- "Already have an account? Login" link

**Validation Messages:**
- Username availability: "✓ Available" or "✗ Already taken"
- Password match: "Passwords don't match" error below confirm field
- Email format: "Invalid email address"

**States:**
- Default state
- Username checking state (spinner on username field)
- Validation error states (red text below fields)
- Loading state (during account creation)
- Success state (redirect to Projects)

---

### Project Management Screens (3)

#### Screen 3: Project List Screen
**Purpose:** View all user's projects with filtering, sorting, and management

**Top Bar:**
- Title "Projects" (or animated greeting "Good morning, [username]" on first load)
- Notification bell icon with badge (unread count)
- Sort dropdown menu button

**Sort Menu Options:**
- Name (A-Z)
- Recent Activity
- Members (most to least)
- Tasks (most to least)

**Content Area:**
- Offline mode banner (compact, shown only when offline)
- Search bar "Search projects..." with search icon
- Filter chips row:
  - All
  - Active
  - Archived
- Project card list (vertical scroll)

**Project Card (Glassmorphic Style):**
- Project name (bold, large)
- Description (2 lines max, truncated)
- Status badge (top-right corner): Active / Archived / On Hold
- Stats row with icons:
  - Members count with user icon
  - Chats count with chat bubble icon (badge if unread)
  - Tasks count with checkbox icon
- Progress bar (task completion: X/Y completed)
- Last activity timestamp "Last activity: 2 hours ago"

**Swipe Actions on Cards:**
- Swipe left: Archive button (purple background)
- Swipe right: Edit button (blue background)

**Floating Action Button (FAB):**
- "Create" button with + icon (purple gradient)

**Empty State:**
- Illustration or icon
- "No projects yet"
- "Create your first project to get started"
- Create Project button

**Loading State:**
- Skeleton cards (3-4 shimmer placeholders)

**Error State:**
- Error icon
- "Failed to load projects"
- Retry button

---

#### Screen 4: Project Details Screen
**Purpose:** Comprehensive project view with 5 tabs for different sections

**Top Bar:**
- Back button
- Project name (title)
- Edit button (pencil icon)
- More menu (3 dots) → Settings, Archive

**Bottom Navigation Bar (Animated):**
- 5 tabs with icons + labels:
  1. **Overview** (home icon)
  2. **Chats** (message bubble icon) - badge if unread
  3. **Tasks** (checkbox icon) - badge if pending
  4. **Members** (users icon)
  5. **Activity** (clock icon)

**Tab 1: Overview**
- Project description card
- Stats cards (3 columns):
  - Chats (count + badge)
  - Tasks (count + badge)
  - Members (count)
- Quick actions row:
  - "New Chat" button
  - "New Task" button
- Team members preview:
  - Heading "Team Members"
  - Horizontal scroll of member avatars (max 5 visible)
  - "+X more" indicator
  - "View All" button

**Tab 2: Chats**
- Create Chat button (top-right)
- List of chat rooms:
  - Chat name
  - Last message preview (1 line)
  - Timestamp
  - Unread count badge
  - Online status indicators (if direct chat)

**Tab 3: Tasks**
- Create Task button (top-right)
- List of tasks:
  - Task title
  - Description preview (1 line)
  - Status badge (TODO/IN_PROGRESS/DONE)
  - Priority badge (LOW/MEDIUM/HIGH/URGENT)
  - Assigned user avatar
  - Due date

**Tab 4: Members**
- Invite Member button (top-right)
- List of members:
  - Avatar
  - Display name
  - Role badge (ADMIN/MANAGER/MEMBER)
  - Online status dot

**Tab 5: Activity**
- Activity timeline (vertical):
  - User avatar
  - Action description "User X created task Y"
  - Timestamp
  - Commit message (if applicable)
- Empty state: "No activity yet"

---

#### Screen 5: Project Workspace Screen
**Purpose:** Persistent workspace with bottom navigation for quick tab switching

**Layout:**
- Top app bar with project name + edit menu
- Content area (renders current tab content)
- Persistent bottom navigation (5 tabs: Overview, Chats, Tasks, Members, Activity)

**Behavior:**
- Same tabs as Project Details but with persistent bottom nav
- Tab switches render different content above nav
- Back button returns to Project List

---

### Chat & Messaging Screens (2)

#### Screen 6: Chat List Screen
**Purpose:** View all chat rooms across projects with filtering

**Top Bar:**
- Title "Chats"
- Back button
- Search icon button

**Filter Chips:**
- All
- Unread
- Mentions
- Archived

**Chat Room List:**
Each room card shows:
- Chat name (bold)
- Last message preview (1-2 lines, truncated)
- Timestamp (relative: "5m ago", "2h ago")
- Unread count badge (red circle with number)
- Online status indicator (green dot if any members online)
- Pinned indicator (pin icon if pinned)

**Swipe Actions:**
- Swipe left: Archive (orange)
- Swipe right: Delete (red)
- Pin/Unpin (tap and hold)

**FAB:**
- Create Chat button with + icon

**User Menu (Top-Right 3 Dots):**
- Profile
- Settings
- Logout

**Empty State:**
- "No chats yet"
- "Start a conversation"
- Create Chat button

---

#### Screen 7: Chat Screen
**Purpose:** Real-time messaging with reactions, threading, and task creation

**Top Bar:**
- Back button
- Chat room name (title)
- Search icon
- Task board icon (navigates to task board for this chat)
- More menu (3 dots)

**Message List (Grouped by Sender):**
Each message group:
- User avatar (shown once per group)
- User name (shown once per group)
- Messages (stacked vertically)
  - Message text
  - Timestamp (shown on hover or long-press)
  - Read receipts (checkmarks)
  - Reaction chips (emoji badges below message)

**Message Types:**
- Text message (plain)
- System message (centered, gray, italic)
- Task created message (blue background, "Task: [title]" with link)

**Date Headers:**
- Sticky date separators "Today", "Yesterday", "Jan 8, 2026"

**Typing Indicator:**
- "User is typing..." (gray text with dots animation)

**Jump to Bottom FAB:**
- Appears when scrolled up >200dp
- Circle button with down arrow icon

**Message Input Bar (Bottom):**
- Text input field "Type a message..."
- Send button (paper plane icon, enabled only when text exists)

**Long-Press Message Context Menu:**
- React (emoji picker)
- Reply (quotes message)
- Edit (if own message)
- Delete (if own message or admin)
- Copy

**Double-Tap Behavior:**
- Quick react with default emoji (heart)

**Reply Indicator (When Replying):**
- Shows quoted message above input
- "Replying to [user]"
- Cancel button

**Offline Mode:**
- Banner at top: "You're offline. Messages will send when reconnected."

---

### Task Management Screens (7)

#### Screen 8: My Tasks Screen
**Purpose:** Cross-project task hub with list and Kanban board views

**Top Bar:**
- Title "My Tasks"
- Back button
- View mode toggle (List icon / Board icon)
- Filter button (funnel icon)
- Sort dropdown

**Sort Options:**
- Due Date
- Priority
- Created Date
- Updated Date

**List View:**
- Task cards:
  - Task title (bold)
  - Description preview (1 line)
  - Status badge
  - Priority badge
  - Due date with calendar icon
  - Project name (small text)
  - Assigned user avatar

**Board View:**
- 3 columns (horizontal scroll):
  - **TO DO** column
  - **IN PROGRESS** column
  - **DONE** column
- Each column has header with count
- Cards show same info as list view but more compact

**Filter Sheet (Bottom Sheet):**
- Status checkboxes (TODO, IN_PROGRESS, DONE, CANCELLED)
- Priority checkboxes (LOW, MEDIUM, HIGH, URGENT)
- Apply button
- Clear filters button

**FAB:**
- Create Task button

**Empty State:**
- "No tasks assigned to you"
- "Tasks you're assigned will appear here"

---

#### Screen 9: Task Board Screen (Kanban)
**Purpose:** Project-specific Kanban board for task management

**Top Bar:**
- Back button
- Project name (bold)
- Team info (subtitle: "X members")
- Current user avatar (top-right)

**Search Bar:**
- "Search tasks..." with search icon

**Tab Buttons:**
- My Tasks
- All Tasks

**Kanban Columns (Horizontal Scroll):**
- **TO DO** (navy header, count badge)
- **IN PROGRESS** (navy header, count badge)
- **DONE** (navy header, count badge)

**Task Card (Within Column):**
- Task title
- Priority badge (color-coded: Low=blue, Medium=orange, High=red, Urgent=dark red)
- Due date with calendar icon
- Assignee avatar (bottom-right)
- Status indicator (color stripe on left edge)

**Drag & Drop:**
- Cards can be dragged between columns
- Automatically updates task status

**Create Task:**
- + button in each column header OR
- FAB at bottom-right

**Offline Mode:**
- Banner at top when offline

---

#### Screen 10: Task Detail Screen
**Purpose:** View-only comprehensive task information

**Top Bar:**
- Back button
- "Task Details" title
- More menu → Delete

**Content (Scrollable):**

**Task Title Section:**
- Large task title (white text on dark background)
- Due date display "Due Oct 12, 2024"

**Badges Row:**
- Status badge (green "In Progress")
- Priority badge (red "High Priority")

**Assigned User Section:**
- Avatar
- User name
- Role badge
- "Assign" button (if unassigned)

**Metadata Section:**
- Due date field (clickable to edit)
- Tags (chips, clickable to edit)
- "Add tags" button

**Description Section:**
- Heading "Description"
- Description text (multiline, read-only with edit icon)
- Edit icon button

**Subtasks Section:**
- Heading "Subtasks"
- Progress indicator "2/3 completed"
- Checkbox list
- "Add subtask" button

**Time Tracking Section:** ✨ NEW - Phase 8
- Heading "Time Tracking"
- Estimated hours: "8.0 hours"
- Actual hours: "6.0 hours"
- Progress bar with percentage "75% complete" (green if <100%, red if >100%)

**Activity Section:**
- Heading "Activity"
- Timeline of changes:
  - User avatar
  - Action description
  - Timestamp
- "View full history" link

**Comment Section:**
- Heading "Comments"
- Comment input field
- Send button
- Comment list (if any):
  - User avatar
  - User name
  - Comment text
  - Timestamp

---

#### Screen 11: Task Edit Screen
**Purpose:** Edit all 21 task fields with validation

**Top Bar:**
- Back button (warns if unsaved changes)
- "Edit Task" title
- Save button (enabled only if changes made)

**Form Fields (Scrollable):**

1. **Title** (required)
   - Text input, max 200 chars
   - Validation: Cannot be empty

2. **Description** (optional)
   - Multiline text input, max 5000 chars
   - Character counter

3. **Status** (required)
   - Dropdown picker: TODO, IN_PROGRESS, DONE, CANCELLED
   - Default: TODO

4. **Priority** (required)
   - Chip selector: LOW, MEDIUM, HIGH, URGENT
   - Color-coded
   - Default: MEDIUM

5. **Assigned To** (optional)
   - User picker (shows all project members)
   - Displays: Avatar + Name + Role

6. **Due Date** (optional)
   - Date picker dialog
   - Displays: "Oct 12, 2024" or "No due date"

7. **Estimated Hours** (optional)
   - Decimal number input
   - Validation: Must be positive

8. **Actual Hours** (optional)
   - Decimal number input
   - Validation: Must be positive

9. **Tags** (optional)
   - Tag input dialog (opens on click)
   - Chip display
   - Max 10 tags, each max 50 chars

10. **Subtasks** (optional)
    - View and manage subtasks
    - Add/remove subtask buttons

11. **Comments** (view-only in edit screen)
    - Display existing comments
    - "Add comment" redirects to Task Detail

**Validation Errors:**
- Shown per field below input
- Red text with error icon

**Unsaved Changes Warning:**
- Dialog on back press
- "You have unsaved changes. Discard or save?"
- Discard / Cancel / Save buttons

**Commit Message Dialog:**
- Shown on save if conflicts detected
- "Describe your changes" textarea
- Optional field
- Save / Cancel buttons

---

#### Screen 12: Task Management Screen
**Purpose:** Quick task actions via bottom sheet (alternative to full edit)

**Top Bar:**
- Back button
- "Task Management" title
- More menu → Delete

**Read-Only Task Display:**
- All task details shown (same as Task Detail screen)
- No inline editing

**Time Tracking Widget:**
- Real-time elapsed time display (updates every second)
- Displays: "Tracking time: 00:23:45"

**Bottom Sheet (Triggered by FAB or Button):**
- Animated slide-up sheet with 4 quick actions

**Quick Action 1: Change Status**
- Status picker (4 options)
- Immediately updates on select

**Quick Action 2: Assign User**
- User picker (all project members)
- Immediately updates on select

**Quick Action 3: Edit Full Details**
- Button that navigates to Task Edit screen

**Quick Action 4: Time Tracking**
- Start/Stop timer button
- Manual time entry option

---

#### Screen 13: Activity Log Screen
**Purpose:** Project-wide activity audit trail with filtering

**Top Bar:**
- Back button
- "Activity Log" title
- Search icon

**Search Bar:**
- "Search by message, description, actor..."
- Debounced (500ms)

**Filter Chips Row:**
- Action Type dropdown (Created, Updated, Status Changed, etc.)
- User filter dropdown (all project members)
- Clear Filters button (if filters applied)

**Activity Timeline:**
Each activity item:
- User avatar
- User name + role badge
- Action description (auto-generated)
  - Example: "Changed status from TODO to IN_PROGRESS"
- Commit message (if provided by user)
- Timestamp (relative: "5 minutes ago")

**Pagination:**
- 100 items per page
- "Load more" button at bottom
- Loading indicator during load

**Empty State:**
- "No activity yet"
- "Activity will appear as team members work"

---

### User & Profile Screens (6)

#### Screen 14: User Profile Screen (Other Users)
**Purpose:** View other team members' profiles (read-only)

**Top Bar:**
- Back button
- "Profile" title

**Profile Header:**
- Large centered avatar (120dp)
- Display name (bold, large)
- @username (smaller, gray)
- Bio section (if available, multiline)

**Contact Information Cards:**
- Email (with mail icon)
- LinkedIn (with LinkedIn icon + external link)
- GitHub (with GitHub icon + external link)
- Website (with globe icon + external link)
- Portfolio (with briefcase icon + external link)

**Stats Section:**
- Heading "Overview"
- Active Projects count (with project icon)
- On-time task completion rate (percentage)

**Action Button:**
- "Message" button (primary CTA, if not current user)
- Navigates to Direct Message chat

**Loading State:**
- Skeleton placeholders for avatar, name, stats

**Error State:**
- "Failed to load profile"
- Retry button

---

#### Screen 15: Profile Screen (Own Profile)
**Purpose:** View and manage own profile with settings access

**Top Bar:**
- Back button
- "Profile" title
- Settings icon (gear)

**Profile Header:**
- Large centered avatar (120dp) with camera icon overlay (edit indicator)
- Display name
- @username

**Action Button:**
- "Edit Profile" button (primary CTA, blue)

**Contact Information:**
- Same cards as User Profile screen

**Stats Section:**
- Active Projects count
- On-time rate

**Divider**

**Settings Menu Buttons:**
- Privacy Settings (with arrow)
- Notification Settings (with arrow)
- Logout (destructive red text)

**App Version Footer:**
- Small gray text at bottom
- "Kosmos v1.0.0"

---

#### Screen 16: Edit Profile Screen
**Purpose:** Edit all 17 profile fields with photo upload

**Top Bar:**
- Back button
- "Edit Profile" title
- Save button (in top bar)

**Profile Photo Section:**
- Large centered avatar (120dp)
- Camera icon overlay
- "Pick photo" button
- Selected photo preview

**Form Fields (Scrollable):**

1. **Display Name** (required)
   - Text input
   - Max 100 chars

2. **Bio** (optional)
   - Multiline text input
   - Max 500 chars
   - Character counter "235/500"

3. **Age** (optional)
   - Number input

4. **Role/Title** (optional)
   - Text input
   - Example: "Senior Android Developer"

5. **Location** (optional)
   - Text input
   - Example: "San Francisco, CA"

**Social Links Section (Expandable):**
- Expand/collapse toggle

6. **GitHub URL** (optional)
   - URL input with validation

7. **Twitter URL** (optional)

8. **LinkedIn URL** (optional)

9. **Website URL** (optional)

10. **Portfolio URL** (optional)

**Validation:**
- Required fields: Cannot be empty
- URL fields: Must be valid URLs
- Character limits: Enforced with error message

**Save Button:**
- Enabled only if changes made
- Shows loading spinner during save

**Unsaved Changes Warning:**
- Dialog on back press if changes exist

---

#### Screen 17: User Search Screen
**Purpose:** Find and view other users

**Top Bar:**
- Back button
- "Find Users" title

**Search Bar:**
- "Search by name, @username, or email"
- Real-time search (debounced 500ms)

**User List:**
Each user card:
- Avatar (left)
- Display name (bold)
- @username (gray)
- Bio preview (1 line, if available)
- Shared project count indicator (if any): "3 shared projects"
- Tap to view full profile

**Empty Search Prompt:**
- "Search for users by name, username, or email"
- Search icon illustration

**No Results:**
- "No users found"
- "Try a different search"

**Loading State:**
- Skeleton placeholders (3-4)

**Error State:**
- "Failed to search users"
- Retry button

---

#### Screen 18: Invite Members Screen
**Purpose:** Add members to project with role selection

**Top Bar:**
- Back button
- "Invite Members" title
- Selection count "3 selected"
- Clear selection button (X icon)

**Search Bar:**
- "Search users..."
- Real-time search

**Selected Users Chips (Top Section):**
- Horizontal scroll of chips
- Each chip: Avatar + Name + X button (to remove)

**User List:**
Each user row:
- Avatar
- Display name
- @username
- Checkbox (selected state)

**Role Selector (Bottom Section):**
- Heading "Select role for invited members"
- Chip selector:
  - ADMIN (red chip)
  - MANAGER (amber chip)
  - MEMBER (green chip)
- Default selected: MEMBER

**Bottom Action Bar (Fixed):**
- "Send Invites" button (primary CTA)
- Enabled only if users selected
- Shows loading spinner during invite

**Success State:**
- Toast: "Invited 3 members"
- Navigate back to Members List

---

### Settings & Preferences Screens (3)

#### Screen 19: Notification Settings Screen
**Purpose:** Configure notification preferences

**Top Bar:**
- Back button
- "Notification Settings" title

**Master Toggle Card:**
- Toggle switch "All Notifications"
- Description: "Master switch for all notifications"

**Notification Type Toggles:**
- Message Notifications (toggle)
- Task Notifications (toggle)
- Project Update Notifications (toggle)
- Mention Notifications (toggle)

**Mentions-Only Mode:**
- Toggle switch
- Description: "Only receive notifications when mentioned"

**Sound & Vibration Section:**
- Heading "Sound & Vibration"
- Sound toggle
- Vibration toggle

**Do Not Disturb Section:**
- Heading "Do Not Disturb"
- DND toggle
- Start time picker (clickable)
  - Shows time picker dialog
  - Displays: "10:00 PM"
- End time picker (clickable)
  - Shows time picker dialog
  - Displays: "7:00 AM"

**Info Card (Bottom):**
- Light blue background
- Info icon
- Explanation: "You won't receive notifications during Do Not Disturb hours"

---

#### Screen 20: Privacy Settings Screen
**Purpose:** Control profile and communication privacy

**Top Bar:**
- Back button
- "Privacy Settings" title

**Profile Visibility Section:**
- Heading "Profile Visibility"
- Radio button selector (vertical):
  - **Public** - "Anyone can see your profile"
  - **Friends Only** - "Only people in your projects"
  - **Private** - "Only you can see your profile"

**Visibility Toggles Section:**
- Heading "Visibility Settings"
- Show email (toggle)
- Show last seen (toggle)
- Show online status (toggle)

**Communication Preferences Section:**
- Heading "Communication"
- Allow direct messages (toggle)
- Allow mentions (toggle)

**Data Management Section:**
- Heading "Data Management"
- "Download your data" button
  - Starts data export
  - Shows progress during export

**Blocked Users Section:**
- Heading "Blocked Users"
- List of blocked users (if any):
  - User avatar + name
  - "Unblock" button
- Empty state: "No blocked users"

---

#### Screen 21: Settings Screen
**Purpose:** Central hub for all app settings

**Top Bar:**
- Back button
- "Settings" title

**Account Section:**
- Heading "Account"
- Profile (with arrow)
- Privacy Settings (with arrow)
- Notifications (with arrow)

**Preferences Section:**
- Heading "Preferences"
- Language (with arrow) → Shows language selector
- Theme (with arrow) → Shows theme selector (Light/Dark/System)
- Data Usage (with arrow) → Shows data management options

**About Section:**
- Heading "About"
- App version display "Version 1.0.0"
- Help & Support (with arrow)
- Terms & Conditions (with arrow)
- Privacy Policy (with arrow)

**Account Actions Section:**
- Heading "Actions"
- Clear Cache (with confirmation dialog)
  - Dialog: "Clear all cached data?"
  - Cancel / Clear buttons
- Logout (destructive red)
  - Dialog: "Are you sure you want to logout?"
  - Cancel / Logout buttons

---

### Notification Center (1)

#### Screen 22: Notification List Screen
**Purpose:** View all notifications with management

**Top Bar:**
- Back button
- "Notifications" title
- Unread count subtitle "5 unread"
- Mark all as read button (checkmark icon)
- Clear all button (trash icon)

**Notification List:**
Each notification:
- Notification icon (based on type):
  - Message: chat bubble
  - Task: checkbox
  - Project: folder
  - Mention: @ symbol
- Title (bold if unread)
- Description/message (1-2 lines)
- Timestamp (relative)
- Unread indicator (blue dot on left edge)

**Swipe to Delete:**
- Swipe left to reveal delete button

**Clear All Confirmation Dialog:**
- "Clear all notifications?"
- "This cannot be undone"
- Cancel / Clear All buttons

**Empty State:**
- Bell icon illustration
- "No notifications"
- "You're all caught up!"

**Pull-to-Refresh:**
- Swipe down to refresh

**Loading State:**
- Skeleton placeholders

---

### Additional Screens & Dialogs (Not Full Screens)

#### Screen 23: Create Project Dialog
**Purpose:** Quick project creation

**Dialog Layout:**
- Heading "Create Project"
- Project name input (required)
- Description input (optional, multiline)
- Create button
- Cancel button

---

#### Screen 24: Create Chat Dialog
**Purpose:** Create new chat room

**Dialog Layout:**
- Heading "Create Chat Room"
- Chat name input (required)
- Chat type selector (chips):
  - General
  - Channel
  - Direct
- Select members (user picker)
- Create button
- Cancel button

---

## 3. DESIGN SYSTEM REQUIREMENTS

### Color Palette

**Primary Color:**
- Purple/Indigo: `#6200EA` (Midnight Plum)
- Used for: Primary buttons, active states, links, selected items

**Status Colors:**
- Success/Active: `#4CAF50` (Green)
- Error/High Priority: `#F44336` (Red)
- Warning/Medium Priority: `#FF9800` (Orange)
- Info/In Progress: `#2196F3` (Blue)

**Role Badge Colors:**
- Admin: Red `#EF5350`
- Manager: Amber `#FFA726`
- Member: Green `#66BB6A`

**Task Priority Colors:**
- Low: Blue `#42A5F5`
- Medium: Orange `#FFA726`
- High: Red `#EF5350`
- Urgent: Dark Red `#C62828`

**Neutral Colors:**
- Background Light: `#FAFAFA`
- Surface Light: `#FFFFFF`
- Text Primary: `#1C1B1F`
- Text Secondary: `#49454F`
- Borders: `#E0E0E0`

**Dark Mode (Future):**
- Background Dark: `#1A1625` (Navy-purple)
- Surface Dark: `#2C2438`
- Text Primary Dark: `#E6E1E5`

---

### Typography

**Font Family:**
- Primary: Roboto or System Default
- Weights: Regular (400), Medium (500), Bold (700)

**Type Scale:**
- **Headline Large:** 32sp, Bold
- **Headline Medium:** 28sp, Bold
- **Headline Small:** 24sp, Bold
- **Title Large:** 22sp, Medium
- **Title Medium:** 16sp, Medium
- **Title Small:** 14sp, Medium
- **Body Large:** 16sp, Regular
- **Body Medium:** 14sp, Regular
- **Body Small:** 12sp, Regular
- **Label Large:** 14sp, Medium
- **Label Medium:** 12sp, Medium
- **Label Small:** 11sp, Medium
- **Caption:** 10sp, Regular

**Line Heights:**
- Headlines: 1.2x font size
- Titles: 1.3x font size
- Body: 1.5x font size
- Captions: 1.4x font size

---

### Spacing System

**Spacing Scale (8dp base):**
- XXS: `4dp`
- XS: `8dp`
- SM: `12dp`
- MD: `16dp`
- LG: `24dp`
- XL: `32dp`
- XXL: `48dp`
- XXXL: `64dp`

**Usage:**
- Card padding: `16dp` (MD)
- Screen padding: `16dp` (MD)
- Section spacing: `24dp` (LG)
- Between cards: `8dp` (XS)
- Between list items: `4dp` (XXS)

---

### Corner Radius

**Radius Scale:**
- XS: `4dp`
- SM: `8dp`
- MD: `12dp`
- LG: `16dp`
- XL: `24dp`
- XXL: `32dp`
- Pill: `9999dp` (fully rounded)

**Usage:**
- Cards: `12dp` (MD)
- Buttons: `8dp` (SM)
- Input fields: `8dp` (SM)
- Chips/Badges: `16dp` (LG) or Pill
- Dialog: `16dp` (LG)
- Bottom Sheet: `16dp` top corners only

---

### Elevation & Shadows

**Material 3 Elevation Levels:**
- Level 0: `0dp` - Background, no shadow
- Level 1: `1dp` - Cards, minimal shadow
- Level 2: `3dp` - Floating buttons, menus
- Level 3: `6dp` - Dialogs
- Level 4: `8dp` - Navigation bars
- Level 5: `12dp` - Top app bars

**Shadow Properties:**
- Shadow color: Black with 12% opacity
- Blur radius: 2x elevation level
- Offset Y: 0.5x elevation level

---

### Icons

**Icon Set:** Material Icons (Google Material Design)

**Icon Sizes:**
- Small: `16dp`
- Medium: `24dp` (default)
- Large: `32dp`
- Avatar: `40dp` - `120dp`

**Commonly Used Icons:**
- Projects: folder icon
- Chats: chat bubble icon
- Tasks: checkbox icon
- Users: person icon
- Settings: gear icon
- Notifications: bell icon
- Add: plus icon
- Edit: pencil icon
- Delete: trash icon
- Search: magnifying glass icon
- Sort: arrows up/down icon
- Filter: funnel icon
- More: 3 vertical dots
- Back: left arrow
- Close: X icon

---

### Component Specifications

#### Buttons

**Primary Button:**
- Background: Primary color gradient
- Text: White, bold (700 weight)
- Height: `48dp` minimum (WCAG AA)
- Padding: `16dp` horizontal, `12dp` vertical
- Corner radius: `8dp`
- Elevation: `2dp`

**Secondary Button:**
- Background: Transparent
- Border: `1dp` primary color
- Text: Primary color, medium weight (600)
- Height: `48dp`
- Padding: Same as primary
- Corner radius: `8dp`

**Tertiary Button:**
- Background: Transparent
- Border: None
- Text: Primary color, medium weight (500)
- Height: `48dp`
- Padding: `8dp` horizontal

**FAB (Floating Action Button):**
- Background: Primary color
- Icon: White, 24dp
- Size: `56dp` diameter
- Elevation: `6dp`
- Corner radius: Pill (circle)
- Position: Bottom-right, 16dp from edges

#### Cards

**Standard Card:**
- Background: White
- Border: `1dp` gray `#E0E0E0`
- Corner radius: `12dp`
- Padding: `16dp`
- Elevation: `1dp`
- Margin between cards: `8dp`

**Glassmorphic Card (Optional):**
- Background: White with 85% opacity
- Blur: `10dp`
- Border: `1dp` white with 30% opacity
- Shadow: Soft, `2dp` elevation with 12% alpha

#### Input Fields

**Text Input:**
- Background: Transparent
- Border: `1dp` gray (unfocused), `2dp` primary (focused)
- Corner radius: `8dp`
- Height: `56dp`
- Padding: `16dp` horizontal
- Label: Floating above field when focused
- Leading icon: `24dp` icon, 16dp from left edge
- Trailing icon: Show/hide password, clear text

**Search Bar:**
- Background: Light gray `#F5F5F5`
- Border: None
- Corner radius: `24dp` (pill-shaped)
- Height: `48dp`
- Padding: `16dp` horizontal
- Leading icon: Search icon
- Placeholder: Gray text

#### Badges

**Status Badge:**
- Background: Status color (green/blue/orange/red)
- Text: White, small (12sp), medium weight
- Padding: `8dp` horizontal, `4dp` vertical
- Corner radius: `16dp`
- Height: `24dp`

**Role Badge:**
- Background: Role color (red/amber/green)
- Text: White, small (12sp)
- Same padding/radius as status badge

**Unread Count Badge:**
- Background: Red `#F44336`
- Text: White, small (11sp), bold
- Size: `20dp` diameter (circle)
- Position: Top-right corner of icon/avatar

#### Chips

**Filter Chip:**
- Background: Light gray (unselected), primary color (selected)
- Text: Dark gray (unselected), white (selected)
- Padding: `12dp` horizontal, `8dp` vertical
- Corner radius: `16dp`
- Height: `32dp`

**Tag Chip:**
- Background: Light blue `#E3F2FD`
- Text: Dark blue `#1976D2`
- Padding: `12dp` horizontal, `6dp` vertical
- Corner radius: `16dp`
- X icon: For removable chips

#### Avatars

**Sizes:**
- Small: `32dp`
- Medium: `40dp`
- Large: `56dp`
- Profile: `80dp` - `120dp`

**Shape:** Circle (fully rounded)

**Border:** `2dp` white border when overlapping

**Fallback:** Initials on colored background if no photo

**Online Status Indicator:**
- Size: `12dp` diameter
- Position: Bottom-right corner of avatar
- Color: Green `#4CAF50` (online), gray (offline)
- Border: `2dp` white

---

## 4. KEY USER FLOWS

### Flow 1: Sign Up → Create First Project → Invite Team → Create Task

**Steps:**
1. User clicks "Sign Up" on Login screen
2. Fills required fields (name, username, email, password)
3. Clicks "Create Account"
4. Redirected to Project List screen (empty state)
5. Clicks "Create Project" FAB
6. Enters project name "My First Project"
7. Clicks "Create"
8. Navigated to Project Details screen
9. Clicks "Members" tab
10. Clicks "Invite Member" button
11. Searches for teammate
12. Selects teammate, chooses MEMBER role
13. Clicks "Send Invites"
14. Clicks "Tasks" tab
15. Clicks "Create Task" button
16. Enters task title "Setup backend"
17. Assigns to teammate
18. Clicks "Create"
19. Task appears in Kanban board

---

### Flow 2: Check Tasks → Update Status → Log Time

**Steps:**
1. User opens app (lands on Project List)
2. Taps bottom nav "Tasks" icon
3. Sees My Tasks screen (list view)
4. Taps task "Fix login bug"
5. Views Task Detail screen
6. Clicks prominent Status badge "TODO"
7. Dropdown appears
8. Selects "IN_PROGRESS"
9. Status updates, badge turns blue
10. Scrolls to Time Tracking section
11. Sees "Estimated: 4.0 hours"
12. Clicks "Actual hours" field
13. Enters "2.5" hours
14. Clicks Save
15. Progress bar updates: "63% complete"
16. Navigates back to My Tasks
17. Task moved to "IN PROGRESS" column (if board view)

---

### Flow 3: Chat → Create Task from Message → Complete Task

**Steps:**
1. User taps bottom nav "Chats" icon
2. Views Chat List screen
3. Taps chat "Project Discussion"
4. Views Chat Screen
5. Sees message "We need to update the API docs"
6. Long-presses message
7. Context menu appears
8. Taps "Create Task"
9. Task creation dialog opens
10. Title pre-filled: "Update the API docs"
11. Selects assignee (teammate)
12. Clicks "Create"
13. Task created, linked to message
14. Message shows blue "Task: Update the API docs" link
15. Later: User opens Task Detail
16. Clicks status "IN_PROGRESS"
17. Selects "DONE"
18. Status updates, badge turns green
19. Project stats update: completedTaskCount +1

---

## 5. INTERACTION PATTERNS

### Gestures

**Tap:**
- Open card/item
- Click button
- Select from dropdown

**Long Press:**
- Show context menu (messages)
- Show tooltip (icons)

**Swipe Left:**
- Archive action (chat rooms, projects)
- Reveal delete button

**Swipe Right:**
- Edit action (projects)

**Pull Down:**
- Refresh list (pull-to-refresh)

**Drag & Drop:**
- Reorder Kanban task cards between columns

**Double Tap:**
- Quick react (messages)

---

### Animations

**Screen Transitions:**
- Duration: `300ms`
- Easing: Ease-in-out
- Type: Slide-in from right (forward), slide-out to right (back)

**Button Press:**
- Scale animation: `1.0` → `0.98` → `1.0`
- Duration: `150ms`
- Easing: Spring bounce

**List Item Appearance:**
- Fade in + slide up
- Duration: `250ms`
- Stagger delay: `50ms` between items

**Bottom Sheet:**
- Slide up from bottom
- Duration: `350ms`
- Easing: Decelerate (slow at end)

**Dialog:**
- Fade in + scale up
- Duration: `200ms`

**Badge Pulse (Unread Count):**
- Scale animation: `1.0` → `1.1` → `1.0`
- Duration: `600ms`
- Repeat: 3 times on new message

**Skeleton Loading:**
- Shimmer animation
- Duration: `1200ms`
- Opacity: `0.3` → `0.5` → `0.3`

---

### Feedback

**Haptic Feedback:**
- Button press: Light tap
- Important action (delete): Medium tap
- Error: Double tap

**Visual Feedback:**
- Button press: Ripple effect (Material)
- Input focus: Border color change + glow
- Success action: Green checkmark toast
- Error: Red error card with icon

**Audio Feedback (Optional):**
- Message sent: Soft whoosh sound
- Message received: Gentle ding
- Task completed: Success chime

---

## 6. RESPONSIVE & ACCESSIBILITY

### Screen Sizes

**Supported:**
- Small phones: 360dp x 640dp (minimum)
- Medium phones: 375dp x 667dp
- Large phones: 414dp x 896dp
- Tablets: 768dp x 1024dp (future)

**Layout Rules:**
- Use `match_parent` / `fillMaxWidth` for full-width cards
- Use `weight` for flexible layouts
- Minimum touch target: `48dp` x `48dp` (WCAG AA)
- Maximum text width: `600dp` for readability

---

### Accessibility

**Touch Targets:**
- Minimum size: `48dp` x `48dp`
- Spacing between targets: `8dp` minimum

**Color Contrast:**
- Text on background: `4.5:1` minimum (WCAG AA)
- Large text (18sp+): `3:1` minimum
- Status colors: High contrast on white background

**Content Descriptions:**
- All icons: Descriptive text
- All images: Alt text
- All buttons: Action description

**Screen Reader Support:**
- Semantic headings
- Focusable elements in logical order
- Announce state changes (loading, error)

**Focus Indicators:**
- Visible border: `2dp` primary color
- Visible on keyboard navigation

---

## 7. STATES TO DESIGN

For EVERY screen, design these states:

1. **Default/Success State** - Normal state with content
2. **Loading State** - Skeleton placeholders or spinner
3. **Empty State** - No content yet (illustration + message + CTA)
4. **Error State** - Failed to load (error icon + message + retry button)
5. **Offline State** - Network disconnected (banner at top)
6. **Disabled State** - Action not allowed (grayed out, tooltip)
7. **Selected State** - Item selected (checkmark, highlight)
8. **Editing State** - Form fields editable vs display-only

For DIALOGS/BOTTOM SHEETS:

9. **Open State** - Dialog visible with overlay
10. **Submitting State** - Loading spinner on submit button
11. **Validation Error State** - Red text below fields

For LISTS:

12. **Loading More State** - Spinner at bottom while paginating
13. **End of List State** - "You've reached the end" message

---

## 8. DESIGN DELIVERABLES CHECKLIST

### Required Figma Files

**File 1: Design System**
- Color palette (with hex codes)
- Typography scale (with sizes, weights, line heights)
- Spacing tokens (visual scale)
- Component library:
  - Buttons (all variants)
  - Input fields (all states)
  - Cards (standard, glassmorphic)
  - Chips, badges, avatars
  - Icons (frequently used)
  - Bottom sheets, dialogs
  - Navigation bars

**File 2: Authentication Screens**
- Login screen (all states)
- Sign Up screen (all states, expanded/collapsed optional fields)

**File 3: Project Management Screens**
- Project List (with search, filters, cards)
- Project Details (all 5 tabs)
- Project Workspace (persistent nav)

**File 4: Chat Screens**
- Chat List (with filters, swipe actions)
- Chat Screen (message types, context menu, threading)

**File 5: Task Management Screens**
- My Tasks (list + board view)
- Task Board (Kanban columns)
- Task Detail (all sections)
- Task Edit (form fields)
- Task Management (bottom sheet)
- Activity Log

**File 6: User & Profile Screens**
- User Profile (other users)
- Profile (own profile)
- Edit Profile (form fields)
- User Search
- Invite Members

**File 7: Settings Screens**
- Settings hub
- Privacy Settings
- Notification Settings
- Notification List

**File 8: User Flows**
- Sign Up → Create Project flow (visual diagram)
- Create Task → Update Status flow
- Chat → Create Task flow

---

### Annotations Needed

For EACH screen, include:
- Screen name
- Screen purpose (1 sentence)
- Key user actions (bullets)
- Interactive elements (marked with hotspots)
- Navigation arrows (where does each button go?)
- State variations (default, loading, error, empty)

For COMPONENTS:
- Dimensions (width, height)
- Padding (all sides)
- Corner radius
- Elevation/shadow
- Color hex codes
- Font size, weight, line height
- Icon size
- States (default, hover, pressed, disabled, focused)

---

## 9. TECHNICAL CONSTRAINTS

### Development Platform
- Android only (iOS not planned)
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 14+ (API 34+)
- Framework: Jetpack Compose (native Android UI)

### Performance Requirements
- App launch: <3 seconds (cold start)
- Screen transitions: <300ms
- List rendering: <100ms for 100 items
- Real-time updates: <500ms latency

### Design Constraints
- No custom fonts (use Roboto or system default)
- No heavy animations (battery concern)
- Minimize blur effects (performance impact on low-end devices)
- No video/animated backgrounds
- Support dark mode (future requirement)

---

## 10. BRAND IDENTITY (If Needed)

### App Name
**Kosmos** (with capital K)

### Tagline Ideas
- "Where teams collaborate, tasks align"
- "Your project workspace, unified"
- "Team collaboration, simplified"

### Logo Requirements
- Must work in:
  - Square icon (512x512px) for app icon
  - Horizontal lockup (logo + text) for splash screen
  - Icon-only variant for small sizes
- Colors: Primary purple + white or gradient
- Style: Modern, minimal, geometric

### Voice & Tone
- Professional but friendly
- Clear and concise
- Encouraging (empty states, errors)
- Respectful of user time

---

## 11. NEXT STEPS FOR DESIGNERS

1. **Review this brief thoroughly** - Ask questions about anything unclear
2. **Set up Figma files** - Use File 1-8 structure above
3. **Build design system first** - Colors, typography, components
4. **Design authentication screens** - Login + Sign Up (validate system works)
5. **Design 3 key screens** - Project List, Task Detail, Chat Screen
6. **Review with team** - Get feedback before designing all 24 screens
7. **Complete all screens** - Design all states (default, loading, error, empty)
8. **Create user flow diagrams** - Visual maps of key journeys
9. **Annotate all screens** - Add specs, dimensions, colors, states
10. **Export assets** - Icons, images, illustrations (if needed)
11. **Handoff to developers** - Figma dev mode or exported spec sheets

---

## 12. REFERENCE & INSPIRATION

### Similar Apps (Study These)
- **Linear** - Clean minimal task management UI
- **Notion** - Progressive disclosure, excellent empty states
- **ClickUp** - Comprehensive project management
- **Slack** - Chat UX patterns, threading
- **Asana** - Kanban boards, task detail screens
- **Monday.com** - Status badges, visual hierarchy

### Design Systems to Reference
- **Material Design 3** (Google) - Component library, motion
- **Fluent Design** (Microsoft) - Depth, transparency
- **Polaris** (Shopify) - Clear, functional design

### Key Design Principles
1. **"Status is LOUD"** - Task status should be visually prominent, large clickable badges
2. **"Editing is SUBTLE"** - Inline edit icons, not full-screen dialogs for small changes
3. **Offline-First Feedback** - Show immediate updates, sync in background
4. **Permission-Aware UI** - Disable/hide actions user can't perform
5. **Progressive Disclosure** - Show essential info first, details on demand

---

## 13. ADDITIONAL RESOURCES

### Data Examples for Design

**Sample Project:**
- Name: "Kosmos Mobile App"
- Description: "Build the next-generation team collaboration platform"
- Members: 8
- Chats: 3
- Tasks: 24 (12 completed)
- Status: Active

**Sample Task:**
- Title: "Implement real-time messaging"
- Description: "Add WebSocket support for chat with message delivery confirmations and typing indicators"
- Status: IN_PROGRESS
- Priority: HIGH
- Assigned to: Alex Chen (MANAGER)
- Due: Oct 15, 2026
- Estimated: 12.0 hours
- Actual: 8.5 hours (71% complete)

**Sample Chat Message:**
- Sender: Sarah Kim (ADMIN)
- Message: "Great work on the task board! The drag-and-drop feels smooth. Should we add time tracking next?"
- Timestamp: 2 hours ago
- Reactions: 👍 (3), 🎉 (1)
- Read by: 5 members

**Sample User:**
- Name: Jordan Lee
- Username: @jordanlee
- Role: Senior Product Designer
- Bio: "Passionate about creating delightful user experiences. Coffee enthusiast ☕"
- Location: San Francisco, CA
- GitHub: github.com/jordanlee
- LinkedIn: linkedin.com/in/jordanlee

---

## 14. QUESTIONS FOR DESIGNER CLARIFICATION

Before starting, consider these questions:

1. **Should we use glassmorphism** (frosted glass effect) or stick to Material 3 standard cards?
2. **What level of illustration** do you want for empty states? (Simple icons, detailed illustrations, or none?)
3. **Do you need dark mode designs** immediately or later?
4. **Should we design tablet layouts** now or mobile-only?
5. **Do you want animated prototypes** in Figma or static screens?
6. **What's the priority** - design all 24 screens at basic level first, or complete 5 key screens fully (all states)?
7. **Do you have brand guidelines** (logo, colors) or should designer create from scratch?
8. **What's the timeline** - how many weeks for full design completion?

---

## 15. CONTACT & COLLABORATION

**Design Review Process:**
1. Designer creates initial design system + 3 screens
2. Review with product team → Feedback round 1
3. Designer refines and completes 10 more screens
4. Review → Feedback round 2
5. Designer completes all 24 screens
6. Final review → Approval
7. Handoff to developers

**Communication Channels:**
- Design feedback: [Your preferred method - Figma comments, Slack, email]
- Questions: [Contact method]
- Approval signoff: [Who approves final designs?]

---

## APPENDIX: SCREEN CHECKLIST

Use this to track design progress:

### Authentication (2 screens)
- [ ] Login Screen
- [ ] Sign Up Screen

### Projects (3 screens)
- [ ] Project List Screen
- [ ] Project Details Screen
- [ ] Project Workspace Screen

### Chat (2 screens)
- [ ] Chat List Screen
- [ ] Chat Screen

### Tasks (7 screens)
- [ ] My Tasks Screen
- [ ] Task Board Screen
- [ ] Task Detail Screen
- [ ] Task Edit Screen
- [ ] Task Management Screen
- [ ] Quick Task Creation Sheet
- [ ] Activity Log Screen

### Users (6 screens)
- [ ] User Profile Screen (Other)
- [ ] Profile Screen (Own)
- [ ] Edit Profile Screen
- [ ] User Search Screen
- [ ] Invite Members Screen

### Settings (3 screens)
- [ ] Settings Screen
- [ ] Privacy Settings Screen
- [ ] Notification Settings Screen

### Notifications (1 screen)
- [ ] Notification List Screen

### Dialogs/Sheets (Not full screens but needed)
- [ ] Create Project Dialog
- [ ] Create Chat Dialog
- [ ] Confirmation Dialogs (Delete, Logout, etc.)
- [ ] Role Selector Bottom Sheet
- [ ] Filter Bottom Sheet
- [ ] User Picker Bottom Sheet

**Total: 24 Main Screens + 6 Dialogs/Sheets = 30 Design Deliverables**

---

**END OF DESIGN BRIEF**

This document provides everything a designer needs to create comprehensive UI/UX designs for the Kosmos app in Figma or any design tool. Good luck with your design work!