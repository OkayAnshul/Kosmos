package com.example.kosmos.features.profile.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.UserSettings
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for Privacy Settings Screen
 * Manages user privacy preferences and settings
 * Now persists to Supabase database instead of SharedPreferences
 */
@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,  // Phase 4 TODO FIX: Added for data export
    private val taskRepository: TaskRepository,        // Phase 4 TODO FIX: Added for data export
    @ApplicationContext private val context: Context   // Phase 4 TODO FIX: Added for data export
) : ViewModel() {

    companion object {
        private const val TAG = "PrivacySettingsVM"
    }

    private val _uiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = _uiState.asStateFlow()

    // Debounce save: prevents write amplification on rapid toggling.
    // [FUTURE F4] When blocked-users list is implemented, route its saves through this trigger too.
    private val _saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Phase 4 TODO FIX: Data export file path (for sharing)
    private val _exportFilePath = MutableStateFlow<String?>(null)
    val exportFilePath: StateFlow<String?> = _exportFilePath.asStateFlow()

    private val currentUserId: String?
        get() = authRepository.getCurrentUser()?.id

    init {
        loadSettings()
        viewModelScope.launch {
            _saveTrigger
                .debounce(800)
                .collect { saveSettings() }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Wait for AuthRepository's async init to complete if needed
            val userId = currentUserId ?: run {
                val user = authRepository.userFlow.filterNotNull().first()
                user.id
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = userRepository.getUserSettings(userId)

            if (result.isSuccess) {
                val settings = result.getOrNull() ?: UserSettings()
                _uiState.update {
                    it.copy(
                        profileVisibility = settings.privacy.profileVisibility,
                        showEmail = settings.privacy.showEmail,
                        showLastSeen = settings.privacy.showLastSeen,
                        showOnlineStatus = settings.privacy.showOnlineStatus,
                        allowDirectMessages = settings.privacy.allowDirectMessages,
                        allowMentions = settings.privacy.allowMentions,
                        blockedUsers = emptyList(), // TODO v1.1: Requires database schema update + DAO + Repository methods
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    private suspend fun saveSettings() {
        val userId = currentUserId ?: return

        // P1-03 FIX: Add try/catch for error handling
        try {
            // Get current settings from database to preserve notification settings
            val currentSettings = userRepository.getUserSettings(userId).getOrNull() ?: UserSettings()

            // Get current UI state
            val state = _uiState.value

            // Create updated settings object (preserve notifications, update privacy)
            val updatedSettings = currentSettings.copy(
                privacy = com.example.kosmos.core.models.PrivacySettings(
                    profileVisibility = state.profileVisibility,
                    showEmail = state.showEmail,
                    showLastSeen = state.showLastSeen,
                    showOnlineStatus = state.showOnlineStatus,
                    allowDirectMessages = state.allowDirectMessages,
                    allowMentions = state.allowMentions
                )
            )

            // Save to database
            val result = userRepository.updateUserSettings(userId, updatedSettings)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = "Failed to save settings: ${result.exceptionOrNull()?.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update {
                it.copy(error = "Failed to save privacy settings: ${e.message}")
            }
        }
    }

    fun updateProfileVisibility(visibility: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileVisibility = visibility) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun toggleShowEmail(show: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(showEmail = show) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun toggleShowLastSeen(show: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(showLastSeen = show) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun toggleShowOnlineStatus(show: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(showOnlineStatus = show) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun toggleAllowDirectMessages(allow: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(allowDirectMessages = allow) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun toggleAllowMentions(allow: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(allowMentions = allow) }
            _saveTrigger.tryEmit(Unit)
        }
    }

    fun requestDataDownload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingData = true, error = null) }

            try {
                val userId = currentUserId ?: throw Exception("User not logged in")

                // Collect all user data for GDPR-compliant export
                val exportData = withContext(Dispatchers.IO) {
                    collectUserData(userId)
                }

                // Write to file
                val file = withContext(Dispatchers.IO) {
                    writeExportFile(exportData)
                }

                _exportFilePath.value = file.absolutePath
                _uiState.update {
                    it.copy(
                        isDownloadingData = false,
                        dataDownloadSuccess = true,
                        exportFileReady = true
                    )
                }

                Log.d(TAG, "Data export complete: ${file.absolutePath}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Data export failed", e)
                _uiState.update {
                    it.copy(
                        isDownloadingData = false,
                        dataDownloadSuccess = false,
                        error = "Failed to export data: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Collect all user data for GDPR-compliant export
     * Includes: profile, settings, projects, tasks, export metadata
     */
    private suspend fun collectUserData(userId: String): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        // User profile
        val user = authRepository.getCurrentUser()
        if (user != null) {
            data["profile"] = mapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "username" to (user.username ?: ""),
                "bio" to (user.bio ?: ""),
                "createdAt" to user.createdAt
            )
        }

        // User settings
        val settings = userRepository.getUserSettings(userId).getOrNull()
        if (settings != null) {
            data["settings"] = mapOf(
                "privacy" to mapOf(
                    "profileVisibility" to settings.privacy.profileVisibility,
                    "showEmail" to settings.privacy.showEmail,
                    "showLastSeen" to settings.privacy.showLastSeen,
                    "showOnlineStatus" to settings.privacy.showOnlineStatus
                ),
                "notifications" to mapOf(
                    "enabled" to settings.notifications.enabled,
                    "messages" to settings.notifications.messages,
                    "tasks" to settings.notifications.tasks,
                    "mentions" to settings.notifications.mentions
                )
            )
        }

        // Projects (basic info only)
        val projects = projectRepository.getUserProjectsFlow(userId).first()
        data["projects"] = projects.map { project ->
            mapOf(
                "id" to project.id,
                "name" to project.name,
                "description" to project.description,
                "createdAt" to project.createdAt,
                "status" to project.status.name
            )
        }

        // Tasks assigned to user
        val tasks = taskRepository.getAllUserTasksFlow(userId).first()
        data["tasks"] = tasks.map { task ->
            mapOf(
                "id" to task.id,
                "title" to task.title,
                "status" to task.status.name,
                "priority" to task.priority.name,
                "createdAt" to task.createdAt
            )
        }

        // Export metadata
        data["exportInfo"] = mapOf(
            "exportedAt" to System.currentTimeMillis(),
            "version" to "1.0",
            "app" to "Kosmos"
        )

        return data
    }

    /**
     * Write export data to JSON file in app's private storage
     * Returns: File with name format "kosmos_data_export_YYYY-MM-DD_HHmmss.json"
     */
    private fun writeExportFile(data: Map<String, Any>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "kosmos_data_export_$timestamp.json"

        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)
        val json = Json { prettyPrint = true }

        // Convert to JSON string
        val jsonString = buildString {
            appendLine("{")
            data.entries.forEachIndexed { index, (key, value) ->
                append("  \"$key\": ")
                append(valueToJson(value))
                if (index < data.size - 1) append(",")
                appendLine()
            }
            append("}")
        }

        file.writeText(jsonString)
        return file
    }

    /**
     * Convert value to JSON string representation
     */
    private fun valueToJson(value: Any?, indent: Int = 2): String {
        val indentStr = " ".repeat(indent)
        return when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> {
                val entries = value.entries.map { (k, v) ->
                    "$indentStr  \"$k\": ${valueToJson(v, indent + 2)}"
                }
                "{\n${entries.joinToString(",\n")}\n$indentStr}"
            }
            is List<*> -> {
                if (value.isEmpty()) "[]"
                else {
                    val items = value.map { "$indentStr  ${valueToJson(it, indent + 2)}" }
                    "[\n${items.joinToString(",\n")}\n$indentStr]"
                }
            }
            else -> "\"${value}\""
        }
    }

    /**
     * Get the export file for sharing
     */
    fun getExportFileUri(): android.net.Uri? {
        val filePath = _exportFilePath.value ?: return null
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to get file URI", e)
            null
        }
    }

    fun clearDataDownloadStatus() {
        _uiState.update { it.copy(dataDownloadSuccess = false, exportFileReady = false) }
        _exportFilePath.value = null
    }
}

/**
 * UI state for Privacy Settings screen
 */
data class PrivacySettingsUiState(
    val profileVisibility: String = "PUBLIC", // PUBLIC, FRIENDS_ONLY, PRIVATE
    val showEmail: Boolean = true,
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val allowMentions: Boolean = true,
    val blockedUsers: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isDownloadingData: Boolean = false,
    val dataDownloadSuccess: Boolean = false,
    val exportFileReady: Boolean = false,  // Phase 4 TODO FIX: Added for data export
    val error: String? = null
)
