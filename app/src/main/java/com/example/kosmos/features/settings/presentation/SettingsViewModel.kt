package com.example.kosmos.features.settings.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.example.kosmos.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for Settings Screen
 * Handles app settings, cache management, and logout
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context  // Context for cache clearing (Coil + file cache)
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isClearing = true,
                    error = null
                )

                // Clear all caches: Coil images, file cache, external cache
                withContext(Dispatchers.IO) {
                    var totalCleared = 0L

                    // 1. Clear Coil image cache
                    try {
                        context.imageLoader.diskCache?.clear()
                        context.imageLoader.memoryCache?.clear()
                        Log.d(TAG, "Coil image cache cleared")
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Failed to clear Coil cache", e)
                    }

                    // 2. Clear app file cache
                    try {
                        val cacheDir = context.cacheDir
                        totalCleared += deleteRecursively(cacheDir)
                        Log.d(TAG, "File cache cleared: ${totalCleared / 1024}KB")
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Failed to clear file cache", e)
                    }

                    // 3. Clear external cache (if exists)
                    try {
                        context.externalCacheDir?.let { externalCache ->
                            totalCleared += deleteRecursively(externalCache)
                            Log.d(TAG, "External cache cleared")
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Failed to clear external cache", e)
                    }

                    Log.d(TAG, "Total cache cleared: ${totalCleared / 1024}KB")
                }

                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    successMessage = "Cache cleared successfully"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Cache clearing failed", e)
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    /**
     * Recursively delete files in a directory
     * Returns total bytes deleted
     */
    private fun deleteRecursively(file: File): Long {
        var totalDeleted = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                totalDeleted += deleteRecursively(child)
            }
        }
        val size = file.length()
        if (file.delete()) {
            totalDeleted += size
        }
        return totalDeleted
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = true,
                    error = null
                )

                authRepository.signOut()

                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    logoutSuccess = true
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false,
                    error = "Failed to logout: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null
        )
    }
}

/**
 * UI state for Settings screen
 */
data class SettingsUiState(
    val isClearing: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
