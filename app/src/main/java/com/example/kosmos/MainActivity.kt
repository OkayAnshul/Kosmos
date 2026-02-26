package com.example.kosmos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kosmos.data.sync.InitialSyncManager
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.features.auth.presentation.redesign.LoginScreen
import com.example.kosmos.features.auth.presentation.redesign.SignUpScreen
import com.example.kosmos.features.auth.presentation.redesign.SplashAnimationScreen
import com.example.kosmos.features.auth.presentation.redesign.GoogleProfileSetupScreen
import com.example.kosmos.features.profile.presentation.redesign.ProfileScreenWrapper
import com.example.kosmos.features.profile.presentation.redesign.EditProfileScreenWrapper
import com.example.kosmos.features.profile.presentation.redesign.PrivacySettingsScreenWrapper
import com.example.kosmos.features.profile.presentation.redesign.NotificationSettingsScreenWrapper
import com.example.kosmos.features.tasks.presentation.redesign.TaskBoardScreenWrapper
import com.example.kosmos.features.tasks.presentation.redesign.TaskDetailScreenReactWrapper
import com.example.kosmos.features.tasks.presentation.redesign.TaskEditScreenReactWrapper
import com.example.kosmos.core.config.AppConfigRepository
import com.example.kosmos.features.announcements.AnnouncementScreen
import com.example.kosmos.features.announcements.AnnouncementViewModel
import com.example.kosmos.features.settings.presentation.redesign.AppConfigEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.example.kosmos.features.settings.presentation.redesign.SettingsScreenWrapper
import com.example.kosmos.features.users.presentation.redesign.UserSearchScreenWrapper
import com.example.kosmos.features.users.presentation.redesign.UserProfileScreenWrapper
import com.example.kosmos.features.users.presentation.redesign.InviteMembersScreenWrapper
import com.example.kosmos.features.projects.presentation.redesign.MembersListScreenWrapper
import com.example.kosmos.features.projects.components.CreateProjectDialog
import com.example.kosmos.features.test.QuickDataCheckScreen
import com.example.kosmos.features.test.ReactScreensTestLauncher
import com.example.kosmos.shared.ui.designsystem.ColorTokens
// import com.example.kosmos.features.voice.presentation.SpeechRecognitionScreen
import com.example.kosmos.shared.ui.theme.KosmosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var initialSyncManager: InitialSyncManager

    @Inject
    lateinit var notificationListener: com.example.kosmos.features.notifications.NotificationListener

    @Inject
    @com.example.kosmos.data.sync.ApplicationScope
    lateinit var applicationScope: kotlinx.coroutines.CoroutineScope  // Survives navigation

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle OAuth deep link when app is already running
        intent.data?.let { uri ->
            if (uri.scheme == "kosmos" && uri.host == "auth-callback") {
                com.example.kosmos.core.config.SupabaseConfig.client.handleDeeplinks(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle OAuth deep link if app was launched via kosmos://auth-callback
        intent?.data?.let { uri ->
            if (uri.scheme == "kosmos" && uri.host == "auth-callback") {
                com.example.kosmos.core.config.SupabaseConfig.client.handleDeeplinks(intent)
            }
        }

        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            KosmosTheme {
                KosmosApp(
                    modifier = Modifier.fillMaxSize(),
                    initialSyncManager = initialSyncManager,
                    notificationListener = notificationListener,
                    applicationScope = applicationScope
                )
            }
        }
    }
}

@Composable
fun KosmosApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    initialSyncManager: InitialSyncManager,
    notificationListener: com.example.kosmos.features.notifications.NotificationListener,
    applicationScope: kotlinx.coroutines.CoroutineScope
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Collect unread notification count
    val unreadNotificationCount by notificationListener.unreadCount.collectAsStateWithLifecycle()

    // Collect latest notification for in-app snackbar
    val latestNotification by notificationListener.latestNotification.collectAsStateWithLifecycle()
    val globalSnackbarHostState = remember { SnackbarHostState() }

    // Show in-app snackbar when a new notification arrives
    LaunchedEffect(latestNotification) {
        val notification = latestNotification ?: return@LaunchedEffect
        val result = globalSnackbarHostState.showSnackbar(
            message = "${notification.title}: ${notification.body}",
            actionLabel = "View",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            navController.navigate(Screen.NotificationList.route)
        }
        notificationListener.consumeLatestNotification()
    }

    // Remote app config (pre-seeded from SharedPreferences, refreshed at startup)
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val appConfigRepo = remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            AppConfigEntryPoint::class.java
        ).appConfigRepository()
    }
    val appConfig by appConfigRepo.config.collectAsStateWithLifecycle()

    // Collect UserFeedbackManager events and route to global Snackbar
    val feedbackManager = remember(appContext) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            appContext,
            UserFeedbackEntryPoint::class.java
        ).userFeedbackManager()
    }
    LaunchedEffect(Unit) {
        feedbackManager.events.collect { event ->
            when (event) {
                is com.example.kosmos.core.feedback.FeedbackEvent.PermissionDenied -> {
                    globalSnackbarHostState.showSnackbar(
                        message = "Cannot ${event.action}: ${event.reason}",
                        actionLabel = "OK",
                        duration = androidx.compose.material3.SnackbarDuration.Long
                    )
                }
                is com.example.kosmos.core.feedback.FeedbackEvent.SyncWarning -> {
                    globalSnackbarHostState.showSnackbar(
                        message = event.message,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                is com.example.kosmos.core.feedback.FeedbackEvent.Error -> {
                    val result = globalSnackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (event.retryAction != null) "Retry" else null,
                        duration = androidx.compose.material3.SnackbarDuration.Long
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        event.retryAction?.invoke()
                    }
                }
                is com.example.kosmos.core.feedback.FeedbackEvent.Success -> {
                    globalSnackbarHostState.showSnackbar(
                        message = event.message,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                is com.example.kosmos.core.feedback.FeedbackEvent.Info -> {
                    globalSnackbarHostState.showSnackbar(
                        message = event.message,
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Announcement overlay
    val announcementViewModel: AnnouncementViewModel = hiltViewModel()
    val pendingAnnouncement by announcementViewModel.pending.collectAsStateWithLifecycle()

    // ViewModels for badge counts
    val chatViewModel: com.example.kosmos.features.chat.presentation.ChatViewModel = hiltViewModel()
    val taskViewModel: com.example.kosmos.features.tasks.presentation.TaskViewModel = hiltViewModel()

    // Collect badge counts
    val unreadChatsCount by chatViewModel.unreadCount.collectAsStateWithLifecycle()
    val pendingTasksCount by taskViewModel.pendingCount.collectAsStateWithLifecycle()

    // Check for unseen announcements whenever the logged-in user changes
    LaunchedEffect(authUiState.currentUser?.id) {
        authUiState.currentUser?.id?.let { uid ->
            announcementViewModel.checkAnnouncements(uid)
        }
    }

    // Start/stop notification listener when user logs in/out
    LaunchedEffect(authUiState.currentUser?.id) {
        val currentUser = authUiState.currentUser
        if (currentUser != null) {
            Log.d("KosmosApp", "Starting notification listener for user: ${currentUser.id}")
            notificationListener.startListening(currentUser.id)
        } else {
            Log.d("KosmosApp", "Stopping notification listener")
            notificationListener.stopListening()
        }
    }

    // Track if sync has already started to prevent duplicate sync jobs
    var syncJobStarted by remember { mutableStateOf(false) }

    // Trigger initial sync when user logs in (using Application scope to survive navigation)
    LaunchedEffect(authUiState.isLoggedIn, authUiState.currentUser?.id) {
        val currentUser = authUiState.currentUser
        if (authUiState.isLoggedIn && currentUser != null && !syncJobStarted) {
            syncJobStarted = true
            val userId = currentUser.id

            Log.d("KosmosApp", "User logged in, starting initial sync in Application scope...")

            // Run sync in Application scope (survives navigation)
            applicationScope.launch {
                try {
                    // CRITICAL FIX: Use NonCancellable to make sync immune to parent cancellation
                    // This prevents Ktor HTTP client from receiving cancellation signals when
                    // LaunchedEffect is cancelled during navigation/recomposition
                    val progress = withContext(NonCancellable) {
                        initialSyncManager.syncAllData(userId)
                    }

                    withContext(Dispatchers.Main) {
                        if (progress.isComplete && !progress.hasErrors) {
                            Log.d("KosmosApp", "✅ Initial sync successful - ${progress.projectsSynced}/${progress.projectsTotal} projects")
                        } else if (progress.hasErrors) {
                            Log.w("KosmosApp", "⚠️ Initial sync completed with errors")
                            Log.w("KosmosApp", "   Users: ${if (progress.usersComplete) "✅" else "❌"}")
                            Log.w("KosmosApp", "   Projects: ${if (progress.projectsComplete) "✅" else "❌"}")
                            Log.w("KosmosApp", "   Project Data: ${progress.projectsSynced}/${progress.projectsTotal} synced (${progress.projectSyncErrors} errors)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("KosmosApp", "❌ Initial sync failed", e)
                }
            }
        }
    }

    // Navigate new Google users to profile setup immediately after sign-in
    LaunchedEffect(authUiState.isNewGoogleUser) {
        if (authUiState.isNewGoogleUser) {
            navController.navigate(Screen.GoogleProfileSetup.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // One-time announcement overlay — shown before the nav host so it appears above all screens
    pendingAnnouncement?.let { ann ->
        AnnouncementScreen(
            announcement = ann,
            onDismiss = {
                announcementViewModel.dismiss(ann.id, authUiState.currentUser!!.id)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ColorTokens.ReactTheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.statusBarsPadding(),
        ) {
            composable(Screen.Splash.route) {
                SplashAnimationScreen(
                    isAuthReady = !authUiState.isCheckingAuth,
                    appConfig = appConfig,
                    onFinished = {
                        navController.navigate(
                            if (authUiState.isLoggedIn) Screen.ProjectList.route else Screen.Login.route
                        ) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
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
                onClearError = authViewModel::clearError,
                onSendPasswordReset = authViewModel::sendPasswordResetEmail,
                onClearPasswordResetState = authViewModel::clearPasswordResetState,
                getSavedEmail = authViewModel::getSavedEmail,
                isRememberMeEnabled = authViewModel::isRememberMeEnabled,
                onGoogleIdToken = { idToken, rawNonce ->
                    authViewModel.signInWithGoogleIdToken(idToken, rawNonce)
                }
            )
        }
            composable("quick_data_check") {
                QuickDataCheckScreen(onBack = { navController.popBackStack() })
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

            composable(Screen.GoogleProfileSetup.route) {
                GoogleProfileSetupScreen(
                    uiState = authUiState,
                    onCheckUsernameAvailability = authViewModel::checkUsernameAvailability,
                    onSaveProfile = { displayName, username, age, role, bio, location,
                                      githubUrl, twitterUrl, linkedinUrl, websiteUrl, portfolioUrl ->
                        authViewModel.saveGoogleUserProfile(
                            displayName, username, age, role, bio, location,
                            githubUrl, twitterUrl, linkedinUrl, websiteUrl, portfolioUrl
                        )
                        navController.navigate(Screen.ProjectList.route) {
                            popUpTo(Screen.GoogleProfileSetup.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        authViewModel.clearNewGoogleUserFlag()
                        navController.navigate(Screen.ProjectList.route) {
                            popUpTo(Screen.GoogleProfileSetup.route) { inclusive = true }
                        }
                    }
                )
            }

        composable(Screen.ProjectList.route) {
            Scaffold(
                containerColor = ColorTokens.ReactTheme.background,
                bottomBar = {
                    KosmosHomeBottomBar(
                        onProjectsClick = { /* already on projects */ },
                        onDiscoverClick = { navController.navigate(Screen.Discover.route) },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            ) { innerPadding ->
                com.example.kosmos.features.projects.presentation.redesign.ProjectListScreenReactWrapper(
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectWorkspace.createRoute(projectId))
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.NotificationList.route)
                    },
                    notificationBadgeCount = unreadNotificationCount,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // ProjectWorkspace - Primary project view with bottom tabs (Overview, Chats, Tasks, Members, Activity)
        composable(Screen.ProjectWorkspace.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable

            // State for creation dialogs
            var showCreateChatDialog by remember { mutableStateOf(false) }
            var showCreateTaskDialog by remember { mutableStateOf(false) }

            com.example.kosmos.features.projects.presentation.redesign.ProjectWorkspaceScreen(
                projectId = projectId,
                onChatClick = { chatRoomId ->
                    navController.navigate(Screen.Chat.createRoute(chatRoomId))
                },
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId, projectId))
                },
                onCreateChat = {
                    showCreateChatDialog = true
                },
                onCreateTask = {
                    showCreateTaskDialog = true
                },
                onInviteMembers = {
                    navController.navigate(Screen.InviteMembers.createRoute(projectId))
                },
                onEditProject = {
                    navController.navigate(Screen.ProjectEdit.createRoute(projectId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )

            // Create Chat Dialog
            if (showCreateChatDialog) {
                com.example.kosmos.features.chat.presentation.CreateChatDialogWrapper(
                    projectId = projectId,
                    onDismiss = { showCreateChatDialog = false },
                    onChatCreated = { chatRoomId ->
                        showCreateChatDialog = false
                        // Navigate to the created chat if we have an ID
                        if (chatRoomId.isNotEmpty()) {
                            navController.navigate(Screen.Chat.createRoute(chatRoomId))
                        }
                    }
                )
            }

            // Create Task Dialog
            if (showCreateTaskDialog) {
                com.example.kosmos.features.tasks.presentation.redesign.QuickTaskCreationSheetWrapper(
                    projectId = projectId,
                    chatRoomId = null, // Not tied to specific chat
                    onDismiss = { showCreateTaskDialog = false },
                    onCreate = { taskId ->
                        showCreateTaskDialog = false
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }
        }

        // ProjectDetail - Legacy route, keep for backward compatibility
        // NOTE: This route is deprecated. Use ProjectWorkspace route instead for full functionality.
        composable(Screen.ProjectDetail.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            // Use new React design wrapper
            com.example.kosmos.features.projects.presentation.redesign.ProjectDetailsScreenReactWrapper(
                projectId = projectId,
                onBack = {
                    navController.popBackStack()
                },
                onNewTask = {
                    // Legacy route: Navigate to full ProjectWorkspace for task creation functionality
                    navController.navigate(Screen.ProjectWorkspace.createRoute(projectId)) {
                        popUpTo(Screen.ProjectDetail.route) { inclusive = true }
                    }
                },
                onNewChat = {
                    // Legacy route: Navigate to full ProjectWorkspace for chat creation functionality
                    navController.navigate(Screen.ProjectWorkspace.createRoute(projectId)) {
                        popUpTo(Screen.ProjectDetail.route) { inclusive = true }
                    }
                },
                onViewMembers = {
                    // Navigate to invite members screen
                    navController.navigate(Screen.InviteMembers.createRoute(projectId))
                }
            )
        }

        // ChatHub - All chats across all projects (bottom nav destination)
        composable(Screen.ChatHub.route) {
            var selectedProjectForChat by remember { mutableStateOf<String?>(null) }
            // GAP-005 FIX: State for project picker dialog
            var showProjectPicker by remember { mutableStateOf(false) }

            // Get ProjectViewModel to access projects list for picker
            val projectViewModel: com.example.kosmos.features.project.presentation.ProjectViewModel = hiltViewModel()
            val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()

            com.example.kosmos.features.chat.presentation.redesign.ChatHubScreenWrapper(
                onChatClick = { chatRoomId ->
                    navController.navigate(Screen.Chat.createRoute(chatRoomId))
                },
                onCreateChat = {
                    // GAP-005 FIX: Show project picker dialog
                    showProjectPicker = true
                }
            )

            // GAP-005 FIX: Project picker for chat creation
            if (showProjectPicker) {
                com.example.kosmos.shared.ui.components.ProjectPickerDialog(
                    projects = projectUiState.projects,
                    title = "Select Project for Chat",
                    onProjectSelected = { projectId ->
                        showProjectPicker = false
                        selectedProjectForChat = projectId
                    },
                    onDismiss = { showProjectPicker = false }
                )
            }

            // Create chat dialog (after project selected)
            if (selectedProjectForChat != null) {
                com.example.kosmos.features.chat.presentation.CreateChatDialogWrapper(
                    projectId = selectedProjectForChat!!,
                    onDismiss = { selectedProjectForChat = null },
                    onChatCreated = {
                        // Chat created, just dismiss (no navigation since we don't have chatRoomId)
                        selectedProjectForChat = null
                    }
                )
            }
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
            com.example.kosmos.features.chat.presentation.redesign.ChatRoomScreenReactWrapper(
                chatRoomId = chatRoomId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // MyTasks Hub - All tasks across all projects (bottom nav destination)
        composable(Screen.MyTasks.route) {
            var selectedProjectForTask by remember { mutableStateOf<String?>(null) }
            // GAP-005 FIX: State for project picker dialog
            var showProjectPicker by remember { mutableStateOf(false) }

            // Get ProjectViewModel to access projects list for picker
            val projectViewModel: com.example.kosmos.features.project.presentation.ProjectViewModel = hiltViewModel()
            val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()

            com.example.kosmos.features.tasks.presentation.redesign.MyTasksScreenReactWrapper(
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onTaskEdit = { taskId ->
                    navController.navigate(Screen.TaskEdit.createRoute(taskId))
                },
                onCreateTask = {
                    // GAP-005 FIX: Show project picker dialog
                    showProjectPicker = true
                }
            )

            // GAP-005 FIX: Project picker for task creation
            if (showProjectPicker) {
                com.example.kosmos.shared.ui.components.ProjectPickerDialog(
                    projects = projectUiState.projects,
                    title = "Select Project for Task",
                    onProjectSelected = { projectId ->
                        showProjectPicker = false
                        selectedProjectForTask = projectId
                    },
                    onDismiss = { showProjectPicker = false }
                )
            }

            // Create task dialog (after project selected)
            if (selectedProjectForTask != null) {
                com.example.kosmos.features.tasks.presentation.redesign.QuickTaskCreationSheetWrapper(
                    projectId = selectedProjectForTask!!,
                    chatRoomId = null,
                    onDismiss = { selectedProjectForTask = null },
                    onCreate = { taskId ->
                        selectedProjectForTask = null
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    }
                )
            }
        }

        composable(
            Screen.TaskBoard.route,
            arguments = listOf(
                androidx.navigation.navArgument("projectId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("chatRoomId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId")
            TaskBoardScreenWrapper(
                projectId = projectId,
                chatRoomId = chatRoomId,
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onEditTask = { taskId ->
                    navController.navigate(Screen.TaskEdit.createRoute(taskId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Task Edit Screen (comprehensive editing)
        composable(Screen.TaskEdit.route) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            TaskEditScreenReactWrapper(
                taskId = taskId,
                onBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Project - Full screen
        composable(Screen.ProjectEdit.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kosmos.features.projects.presentation.redesign.EditProjectScreenReactWrapper(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onDeleteSuccess = {
                    // Pop back to project list after deletion
                    navController.popBackStack(Screen.ProjectList.route, inclusive = false)
                }
            )
        }

        // Use new React design for TaskDetail
        composable(Screen.TaskDetail.route) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreenReactWrapper(
                taskId = taskId,
                onBack = {
                    navController.popBackStack()
                },
                onEdit = {
                    navController.navigate(Screen.TaskEdit.createRoute(taskId))
                }
            )
        }

        // More Tab - Profile + Settings menu (bottom nav destination)
        composable(Screen.More.route) {
            com.example.kosmos.features.profile.presentation.redesign.MoreTabScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPrivacySettings = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToAbout = {
                    // GAP-005 FIX: Navigate to About screen
                    navController.navigate(Screen.About.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreenWrapper(
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
                },
                onNavigateToSettings = {
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

        composable(Screen.EditProfile.route) {
            EditProfileScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreenWrapper(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationList.route) {
            // Immediately reset the badge counter so the bell shows 0 without waiting for Supabase roundtrip
            LaunchedEffect(Unit) { notificationListener.resetUnreadCount() }
            com.example.kosmos.features.notifications.NotificationListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNotificationTap = { notificationId, data ->
                    val taskId = data["task_id"]
                    val chatId = data["chat_id"]
                    val projectId = data["project_id"]
                    val connectionId = data["connection_id"]
                    val inviteId = data["invite_id"]
                    val requestId = data["request_id"]

                    when {
                        !taskId.isNullOrBlank() -> navController.navigate(Screen.TaskDetail.createRoute(taskId))
                        !chatId.isNullOrBlank() -> navController.navigate(Screen.Chat.createRoute(chatId))
                        !connectionId.isNullOrBlank() -> navController.navigate(Screen.Connections.route)
                        !inviteId.isNullOrBlank() -> navController.navigate(Screen.Connections.route)
                        !requestId.isNullOrBlank() && !projectId.isNullOrBlank() -> navController.navigate(Screen.ProjectWorkspace.createRoute(projectId))
                        !projectId.isNullOrBlank() -> navController.navigate(Screen.ProjectWorkspace.createRoute(projectId))
                        else -> { /* No navigation needed */ }
                    }
                },
                onNavigateToConnections = { navController.navigate(Screen.Connections.route) }
            )
        }

        composable(Screen.Discover.route) {
            com.example.kosmos.features.discover.presentation.DiscoverScreenWrapper(
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId, ""))
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectWorkspace.createRoute(projectId))
                }
            )
        }

        composable(Screen.Connections.route) {
            com.example.kosmos.features.connections.presentation.ConnectionsScreenWrapper(
                onUserClick = { userId ->
                    // Navigate to user profile - use empty projectId since connections are cross-project
                    navController.navigate(Screen.UserProfile.createRoute(userId, ""))
                }
            )
        }

        composable(Screen.UserSearch.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            UserSearchScreenWrapper(
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
            InviteMembersScreenWrapper(
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MembersList.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            MembersListScreenWrapper(
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.UserProfile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            UserProfileScreenWrapper(
                userId = userId,
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartChat = { targetUserId, chatRoomId ->
                    navController.navigate(Screen.Chat.createRoute(chatRoomId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreenWrapper(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ActivityLog.route) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            com.example.kosmos.features.tasks.presentation.ActivityLogScreenWrapper(
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // GAP-005 FIX: About Screen
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // React Screens Test Launcher (Phase 1 testing)
        composable(Screen.ReactTest.route) {
            ReactScreensTestLauncher()
        }

        // Speech Recognition Screen (optional demo screen)
        // Voice features disabled for MVP - will be re-enabled in Phase 5
        // composable(Screen.SpeechDemo.route) {
        //     SpeechRecognitionScreen()
        // }
        }
    }
    // Global in-app notification snackbar (overlays all screens)
    SnackbarHost(
        hostState = globalSnackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    } // end Box
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ProjectList : Screen("projectList")
    object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
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
    // TaskEdit for comprehensive editing
    object TaskEdit : Screen("taskEdit/{taskId}") {
        fun createRoute(taskId: String) = "taskEdit/$taskId"
    }
    // LEGACY: Keep for backward compatibility (redirects to TaskManagement)
    object TaskDetail : Screen("taskDetail/{taskId}") {
        fun createRoute(taskId: String) = "taskDetail/$taskId"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("editProfile")
    object Settings : Screen("settings")
    object PrivacySettings : Screen("privacySettings")
    object NotificationSettings : Screen("notificationSettings")
    object NotificationList : Screen("notificationList")
    object About : Screen("about")  // GAP-005 FIX: Added About screen
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
    object ActivityLog : Screen("activityLog/{projectId}") {
        fun createRoute(projectId: String) = "activityLog/$projectId"
    }
    object ProjectEdit : Screen("projectEdit/{projectId}") {
        fun createRoute(projectId: String) = "projectEdit/$projectId"
    }
    object ReactTest : Screen("reactTest")

    // Hub Screens for Bottom Navigation
    object MyTasks : Screen("myTasks") // All tasks across projects
    object ChatHub : Screen("chatHub") // All chats across projects
    object More : Screen("more") // Profile + Settings menu
    object Discover : Screen("discover") // Global search & discovery
    object Connections : Screen("connections") // User connections
    object ProjectWorkspace : Screen("projectWorkspace/{projectId}") {
        fun createRoute(projectId: String) = "projectWorkspace/$projectId"
    }
    object Splash : Screen("splash")
    object GoogleProfileSetup : Screen("googleProfileSetup")
}

// Note: ChatListScreen is implemented in Chat.kt

/**
 * GAP-005 FIX: Simple About Screen
 * Displays app information, version, and links
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Kosmos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // App Logo/Title Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kosmos",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTokens.ReactTheme.primary
                    )
                    Text(
                        text = "Project Management & Collaboration",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                // Description
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kosmos is a powerful project management application designed for teams to collaborate effectively. Features include real-time messaging, task management with Kanban boards, team member collaboration, and offline-first functionality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            item {
                // Features
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeatureItem("Project Management with role-based access control")
                    FeatureItem("Real-time team chat with multimedia support")
                    FeatureItem("Task management with Kanban boards")
                    FeatureItem("Activity tracking and notifications")
                    FeatureItem("Offline-first architecture with sync")
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                // Tech Stack
                Text(
                    text = "Built With",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kotlin • Jetpack Compose • Material 3 • Room Database • Supabase • Dagger Hilt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                // Credits
                Text(
                    text = "Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developed with care for efficient team collaboration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
    }
}

/**
 * Home Bottom Bar - shown only on ProjectList screen
 * 4 items: Projects, Discover, Profile, Settings
 */
@Composable
fun KosmosHomeBottomBar(
    onProjectsClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0)
    ) {
        NavigationBarItem(
            selected = true,
            onClick = onProjectsClick,
            icon = { Icon(Icons.Filled.Folder, contentDescription = "Projects", modifier = Modifier.size(22.dp)) },
            label = { Text("Projects", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorTokens.ReactTheme.primary,
                selectedTextColor = ColorTokens.ReactTheme.primary,
                indicatorColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onDiscoverClick,
            icon = { Icon(Icons.Filled.Search, contentDescription = "Discover", modifier = Modifier.size(22.dp)) },
            label = { Text("Discover", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = ColorTokens.ReactTheme.mutedForeground,
                unselectedTextColor = ColorTokens.ReactTheme.mutedForeground
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile", modifier = Modifier.size(22.dp)) },
            label = { Text("Profile", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = ColorTokens.ReactTheme.mutedForeground,
                unselectedTextColor = ColorTokens.ReactTheme.mutedForeground
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = ColorTokens.ReactTheme.mutedForeground,
                unselectedTextColor = ColorTokens.ReactTheme.mutedForeground
            )
        )
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTokens.ReactTheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}