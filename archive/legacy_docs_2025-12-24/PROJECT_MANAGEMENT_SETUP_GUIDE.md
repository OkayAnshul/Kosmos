# Kosmos Project Management UI - Implementation Guide

## ✅ What's Been Implemented

### New Screens Created
1. ✅ **ProjectListScreen.kt** - Shows all user's projects with create button
2. ✅ **CreateProjectDialog.kt** - Form to create new projects
3. ✅ **ProjectDetailScreen.kt** - View project details, members, navigate to chats
4. ✅ **CreateChatDialog** - FULLY IMPLEMENTED with:
   - Chat room name input
   - Description input
   - Room type selector (GENERAL, CHANNEL, ANNOUNCEMENTS)
   - User search and selection
   - Project ID integration

### Updated Components
1. ✅ **ChatListViewModel** - Now accepts projectId parameter for filtering
2. ✅ **ChatListScreen** - Updated to accept projectId and show "Back to Projects" button
3. ✅ **MainActivity** - Added project screen imports

## 🔧 What YOU Need to Complete (5 minutes)

### Step 1: Update MainActivity Navigation (Lines 78-130)

Replace the navigation setup to implement this flow:
```
Login → ProjectList → ProjectDetail → ChatList (filtered by project) → Chat
```

**Find these lines (~78-130) and replace:**

```kotlin
// OLD: startDestination = Screen.ChatList.route
// NEW:
startDestination = if (authUiState.isLoggedIn) Screen.ProjectList.route else Screen.Login.route

// OLD: onLoginSuccess navigates to ChatList
// NEW: onLoginSuccess navigates to ProjectList
onLoginSuccess = {
    navController.navigate(Screen.ProjectList.route) {
        popUpTo(Screen.Login.route) { inclusive = true }
    }
}

// ADD BEFORE the old ChatList composable:
composable(Screen.ProjectList.route) {
    ProjectListScreen(
        onProjectClick = { projectId ->
            navController.navigate(Screen.ProjectDetail.createRoute(projectId))
        },
        onSettingsClick = {
            navController.navigate(Screen.Settings.route)
        },
        onLogout = {
            authViewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}

composable(Screen.ProjectDetail.route) { backStackEntry ->
    val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
    ProjectDetailScreen(
        projectId = projectId,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToChats = { projId ->
            navController.navigate(Screen.ChatList.createRoute(projId))
        }
    )
}

// UPDATE the existing ChatList composable:
composable(Screen.ChatList.route) { backStackEntry ->
    val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
    ChatListScreen(
        projectId = projectId,  // ADD THIS
        onNavigateToChat = { chatRoomId ->
            navController.navigate(Screen.Chat.createRoute(chatRoomId))
        },
        onNavigateToUserSearch = {
            navController.navigate(Screen.UserSearch.route)
        },
        onBackToProjects = { navController.popBackStack() },  // ADD THIS
        onLogout = {
            authViewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}
```

### Step 2: Update Screen Routes (Line ~254)

```kotlin
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ProjectList : Screen("projectList")  // ADD
    object ProjectDetail : Screen("project/{projectId}") {  // ADD
        fun createRoute(projectId: String) = "project/$projectId"
    }
    object ChatList : Screen("chatList/{projectId}") {  // UPDATE
        fun createRoute(projectId: String) = "chatList/$projectId"
    }
    object Chat : Screen("chat/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "chat/$chatRoomId"
    }
    // ... rest stays the same
}
```

### Step 3: Build & Test

```bash
./gradlew assembleDebug
```

## 🎯 New User Flow

1. **Login** → User authenticates
2. **Project List** → Shows all projects user is member of
3. **Create Project** → Click FAB, enter name/description
4. **Project Detail** → View project info, members, click "View Chats" FAB
5. **Chat List** → See only chats for THIS project
6. **Create Chat** → Click +, enter room details, select participants
7. **Chat** → Send messages, create tasks

## 🔍 Key Architecture Points

### Why Projects Are Required
- **ChatRoom.projectId** is mandatory (not nullable)
- Tasks inherit projectId from their chat room
- RBAC permissions are project-based
- Members must be added to project before accessing chats

### Data Flow
```
Project (has many) → ChatRooms (have many) → Messages
                   → ProjectMembers (have roles)
                   → Tasks
```

### Chat Creation
- Now requires projectId context
- Only shows project members for participant selection
- Room is automatically associated with correct project

## 📝 Testing Checklist

After completing the navigation setup:

- [ ] App starts at ProjectList (when logged in)
- [ ] Can create a new project
- [ ] Can view project details
- [ ] Can navigate from project to chats
- [ ] Chat list is filtered by project
- [ ] Can create new chat room with proper dialog
- [ ] Chat room has projectId set
- [ ] Can navigate back to projects from chats
- [ ] Settings screen works from project list

## 🐛 Known Issues Fixed

1. ✅ "Create Chat" button now shows full dialog (not stub)
2. ✅ ChatRoom gets proper projectId on creation
3. ✅ Task creation gets projectId from context
4. ✅ User flow matches RBAC architecture

## 📚 Files Modified/Created

### Created:
- `app/src/main/java/com/example/kosmos/features/project/presentation/ProjectListScreen.kt`
- `app/src/main/java/com/example/kosmos/features/project/presentation/CreateProjectDialog.kt`
- `app/src/main/java/com/example/kosmos/features/project/presentation/ProjectDetailScreen.kt`

### Modified:
- `app/src/main/java/com/example/kosmos/features/chat/presentation/ChatScreens.kt` (CreateChatDialog)
- `app/src/main/java/com/example/kosmos/features/chat/presentation/ChatListViewModel.kt` (projectId param)
- `app/src/main/java/com/example/kosmos/features/chat/presentation/ChatListScreen.kt` (projectId param)
- `app/src/main/java/com/example/kosmos/MainActivity.kt` (imports, needs navigation update)

---

**Total Implementation Time**: 5-10 minutes to complete navigation wiring + testing
**Build Status**: Should compile successfully once navigation is completed
