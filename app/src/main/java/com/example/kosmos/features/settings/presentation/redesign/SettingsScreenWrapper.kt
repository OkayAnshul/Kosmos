package com.example.kosmos.features.settings.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.BuildConfig
import com.example.kosmos.core.config.AppConfigRepository
import com.example.kosmos.features.projects.presentation.redesign.ProjectAccentStyle
import com.example.kosmos.features.settings.presentation.SettingsViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppConfigEntryPoint {
    fun appConfigRepository(): AppConfigRepository
}

/**
 * Wrapper for SettingsScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject SettingsViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Handle logout success navigation
 * - Provide app version information
 * - Collect AppConfig from AppConfigRepository for dynamic strings
 */
@Composable
fun SettingsScreenWrapper(
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect remote app config
    val context = LocalContext.current
    val appConfigRepo = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppConfigEntryPoint::class.java
        ).appConfigRepository()
    }
    val appConfig by appConfigRepo.config.collectAsStateWithLifecycle()

    // Re-fetch config from Supabase (ensures latest values after remote changes)
    LaunchedEffect(Unit) {
        appConfigRepo.refresh()
    }

    // Navigate to login when logout is successful
    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onNavigateToLogin()
        }
    }

    // Read accent style preference from SharedPreferences
    val prefs = remember {
        context.getSharedPreferences("kosmos_prefs", android.content.Context.MODE_PRIVATE)
    }
    var accentStyle by remember {
        mutableStateOf(ProjectAccentStyle.fromPref(prefs.getString(ProjectAccentStyle.PREF_KEY, null)))
    }

    // Get app version
    val appVersion = remember {
        try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            "1.0.0"
        }
    }

    SettingsScreen(
        appVersion = appVersion,
        appConfig = appConfig,
        isClearing = uiState.isClearing,
        isLoggingOut = uiState.isLoggingOut,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToPrivacy = onNavigateToPrivacy,
        onNavigateToNotifications = onNavigateToNotifications,
        onClearCache = viewModel::clearCache,
        onLogout = viewModel::logout,
        onNavigateBack = onNavigateBack,
        projectAccentStyle = accentStyle,
        onProjectAccentStyleChange = { style ->
            accentStyle = style
            prefs.edit().putString(ProjectAccentStyle.PREF_KEY, style.name).apply()
        },
        modifier = modifier
    )
}
