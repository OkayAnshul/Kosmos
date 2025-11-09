package com.example.kosmos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kosmos.data.sync.InitialSyncManager
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.features.auth.presentation.LoginScreen
import com.example.kosmos.features.auth.presentation.SignUpScreen
import com.example.kosmos.features.profile.presentation.ProfileScreen
import com.example.kosmos.features.profile.presentation.EditProfileScreen
import com.example.kosmos.features.profile.presentation.PrivacySettingsScreen
import com.example.kosmos.features.profile.presentation.NotificationSettingsScreen
import com.example.kosmos.features.tasks.presentation.TaskBoardScreen
import com.example.kosmos.features.users.presentation.UserSearchScreen
import com.example.kosmos.features.users.presentation.UserProfileScreen
import com.example.kosmos.features.users.presentation.InviteMembersScreen
import com.example.kosmos.features.projects.presentation.MembersListScreen
// Voice features disabled for MVP - will be re-enabled in Phase 5
// import com.example.kosmos.features.voice.presentation.SpeechRecognitionScreen
import com.example.kosmos.shared.ui.theme.KosmosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var initialSyncManager: InitialSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            KosmosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KosmosApp(
                        modifier = Modifier.padding(innerPadding),
                        initialSyncManager = initialSyncManager
                    )
                }
            }
        }
    }
}

@Composable
fun KosmosApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    initialSyncManager: InitialSyncManager
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Trigger initial sync when user logs in
    LaunchedEffect(authUiState.isLoggedIn, authUiState.currentUser?.id) {
        val currentUser = authUiState.currentUser
        if (authUiState.isLoggedIn && currentUser != null) {
            val userId = currentUser.id
            Log.d("KosmosApp", "User logged in, starting initial sync...")

            try {
                val progress = initialSyncManager.syncAllData(userId)

                if (progress.isComplete && !progress.hasErrors) {
                    Log.d("KosmosApp", "✅ Initial sync successful")
                } else if (progress.hasErrors) {
                    Log.w("KosmosApp", "⚠️ Initial sync completed with errors")
                } else {
                    Log.w("KosmosApp", "⚠️ Initial sync incomplete")
                }
            } catch (e: Exception) {
                Log.e("KosmosApp", "❌ Initial sync failed", e)
                // Continue anyway - app will work with cached data
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authUiState.isLoggedIn) Screen.ProjectList.route else Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                uiState = authUiState,
                onLogin = authViewModel::login,
                onClearError = authViewModel::clearError
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                uiState = authUiState,
                onSignUp = authViewModel::signUp,
                onCheckUsernameAvailability = authViewModel::checkUsernameAvailability
            )
        }

        composable(Screen.ProjectList.route) {
            com.example.kosmos.features.projects.presentation.redesign.ProjectListScreenWrapper(
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                },
                onCreateProject = {
                    // TODO: Navigate to create project screen
                },
                onBackClick = {
                    // Project list is root, so back should exit app or show menu
                }
            )
        }

        composable(Screen.ProjectDetail.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kosmos.features.projects.presentation.redesign.ProjectDetailsScreenWrapper(
                projectId = projectId,
                onViewAllChats = {
                    navController.navigate(Screen.ChatList.createRoute(projectId))
                },
                onViewAllTasks = {
                    navController.navigate(Screen.TaskBoard.createRoute(projectId))
                },
                onViewAllMembers = {
                    navController.navigate(Screen.MembersList.createRoute(projectId))
                },
                onCreateChat = {
                    navController.navigate(Screen.UserSearch.createRoute(projectId))
                },
                onCreateTask = {
                    // TODO: Open quick task creation sheet
                },
                onInviteMember = {
                    navController.navigate(Screen.InviteMembers.createRoute(projectId))
                },
                onEditProject = {
                    // TODO: Navigate to edit project screen
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ChatList.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kosmos.features.chat.presentation.redesign.EnhancedChatListScreenWrapper(
                projectId = projectId,
                onChatClick = { chatRoomId ->
                    navController.navigate(Screen.Chat.createRoute(chatRoomId))
                },
                onCreateChat = {
                    navController.navigate(Screen.UserSearch.createRoute(projectId))
                },
                onSearchClick = {
                    // TODO: Implement search functionality
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
            com.example.kosmos.features.chat.presentation.redesign.EnhancedChatScreenWrapper(
                chatRoomId = chatRoomId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TaskBoard.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId")
            TaskBoardScreen(
                projectId = projectId,
                chatRoomId = chatRoomId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProfileClick = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onPrivacySettingsClick = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onNotificationSettingsClick = {
                    navController.navigate(Screen.NotificationSettings.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.UserSearch.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            UserSearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId, projectId))
                }
            )
        }

        composable(Screen.InviteMembers.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kosmos.features.users.presentation.InviteMembersScreen(
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MembersList.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            // We need to get the current user's role in this project
            // For now, we'll use a placeholder and let the screen handle it
            MembersListScreen(
                projectId = projectId,
                onBackClick = {
                    navController.popBackStack()
                },
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId, projectId))
                },
                onAddMembersClick = {
                    navController.navigate(Screen.InviteMembers.createRoute(projectId))
                }
            )
        }

        composable(Screen.UserProfile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartChat = { targetUserId, chatRoomId ->
                    // Navigate to the chat room
                    navController.navigate(Screen.Chat.createRoute(chatRoomId)) {
                        // Pop back to chat list
                        popUpTo(Screen.ChatList.createRoute(projectId)) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Speech Recognition Screen (optional demo screen)
        // Voice features disabled for MVP - will be re-enabled in Phase 5
        // composable(Screen.SpeechDemo.route) {
        //     SpeechRecognitionScreen()
        // }
    }
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onLogout: () -> Unit) {
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Information Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow("App Name", "Kosmos")
                        InfoRow("Version", "1.0.0 (MVP)")
                        InfoRow("Build Type", if (BuildConfig.DEBUG) "Debug" else "Release")
                    }
                }
            }

            // Preferences Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Coming soon: Theme, Notifications, Online Status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Storage Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow("Cache Size", "~0 MB")

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showClearCacheDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Cache")
                        }
                    }
                }
            }

            // About Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Kosmos is a project management and team communication app with real-time messaging, task management, and role-based access control.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Built with: Jetpack Compose, Supabase, Room Database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Logout Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.ExitToApp, "Logout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Clear Cache Dialog
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Clear Cache?") },
                text = { Text("This will clear all cached data. The app will need to re-download data from the server.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // TODO: Implement cache clearing
                            showClearCacheDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ProjectList : Screen("projectList")
    object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
    object ChatList : Screen("chatList/{projectId}") {
        fun createRoute(projectId: String) = "chatList/$projectId"
    }
    object Chat : Screen("chat/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "chat/$chatRoomId"
    }
    object TaskBoard : Screen("taskBoard/{projectId}?chatRoomId={chatRoomId}") {
        fun createRoute(projectId: String, chatRoomId: String? = null) =
            if (chatRoomId != null) {
                "taskBoard/$projectId?chatRoomId=$chatRoomId"
            } else {
                "taskBoard/$projectId"
            }
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("editProfile")
    object Settings : Screen("settings")
    object PrivacySettings : Screen("privacySettings")
    object NotificationSettings : Screen("notificationSettings")
    object SpeechDemo : Screen("speechDemo")
    object UserSearch : Screen("userSearch/{projectId}") {
        fun createRoute(projectId: String) = "userSearch/$projectId"
    }
    object InviteMembers : Screen("inviteMembers/{projectId}") {
        fun createRoute(projectId: String) = "inviteMembers/$projectId"
    }
    object MembersList : Screen("membersList/{projectId}") {
        fun createRoute(projectId: String) = "membersList/$projectId"
    }
    object UserProfile : Screen("userProfile/{userId}/{projectId}") {
        fun createRoute(userId: String, projectId: String) = "userProfile/$userId/$projectId"
    }
}

// Note: ChatListScreen is implemented in Chat.kt