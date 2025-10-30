package com.example.kosmos

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.features.auth.presentation.LoginScreen
import com.example.kosmos.features.auth.presentation.SignUpScreen
import com.example.kosmos.features.chat.presentation.ChatListScreen
import com.example.kosmos.features.chat.presentation.ChatScreen
import com.example.kosmos.features.profile.presentation.ProfileScreen
import com.example.kosmos.features.tasks.presentation.TaskBoardScreen
import com.example.kosmos.features.users.presentation.UserSearchScreen
import com.example.kosmos.features.users.presentation.UserProfileScreen
import com.example.kosmos.features.project.presentation.ProjectListScreen
import com.example.kosmos.features.project.presentation.ProjectDetailScreen
// Voice features disabled for MVP - will be re-enabled in Phase 5
// import com.example.kosmos.features.voice.presentation.SpeechRecognitionScreen
import com.example.kosmos.shared.ui.theme.KosmosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            KosmosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KosmosApp(
                        modifier = Modifier.padding(innerPadding)
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
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

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
                onClearError = authViewModel::clearError
            )
        }

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
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChats = { projId ->
                    navController.navigate(Screen.ChatList.createRoute(projId))
                }
            )
        }

        composable(Screen.ChatList.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ChatListScreen(
                projectId = projectId,
                onNavigateToChat = { chatRoomId ->
                    navController.navigate(Screen.Chat.createRoute(chatRoomId))
                },
                onNavigateToUserSearch = {
                    navController.navigate(Screen.UserSearch.createRoute(projectId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onBackToProjects = {
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

        composable(Screen.Chat.route) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
            ChatScreen(
                chatRoomId = chatRoomId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.TaskBoard.createRoute(chatRoomId))
                }
            )
        }

        composable(Screen.TaskBoard.route) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
            TaskBoardScreen(
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
    object TaskBoard : Screen("taskBoard/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "taskBoard/$chatRoomId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object SpeechDemo : Screen("speechDemo")
    object UserSearch : Screen("userSearch/{projectId}") {
        fun createRoute(projectId: String) = "userSearch/$projectId"
    }
    object UserProfile : Screen("userProfile/{userId}/{projectId}") {
        fun createRoute(userId: String, projectId: String) = "userProfile/$userId/$projectId"
    }
}

// Note: ChatListScreen is implemented in Chat.kt