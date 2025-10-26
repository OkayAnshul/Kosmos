package com.example.kosmos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
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
                    navController.navigate(Screen.UserSearch.route)
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

        composable(Screen.UserSearch.route) {
            UserSearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.UserProfile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartChat = { userId ->
                    // TODO: Implement chat creation with selected user
                    // For now, just navigate back
                    navController.popBackStack()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "More settings coming soon...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    object UserSearch : Screen("userSearch")
    object UserProfile : Screen("userProfile/{userId}") {
        fun createRoute(userId: String) = "userProfile/$userId"
    }
}

// Note: ChatListScreen is implemented in Chat.kt