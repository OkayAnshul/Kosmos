package com.example.kosmos.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for Activity Log Screen
 *
 * Manages project-wide activity log with:
 * - Filtering by action type
 * - Filtering by user
 * - Search by commit message
 * - Pagination (100 items at a time)
 *
 * Pattern: Reactive UI state with Flow-based filtering
 */
@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50  // Phase 4 TODO FIX: Pagination page size
    }

    private val _uiState = MutableStateFlow(ActivityLogUiState())
    val uiState: StateFlow<ActivityLogUiState> = _uiState.asStateFlow()

    // Current filters
    private val _searchQuery = MutableStateFlow("")
    private val _selectedActionType = MutableStateFlow<ActivityActionType?>(null)
    private val _selectedUserId = MutableStateFlow<String?>(null)

    // Phase 4 TODO FIX: Pagination state
    private var currentOffset = 0
    private var allActivitiesLoaded = false

    /**
     * Load activity log for a project
     * Phase 4 TODO FIX: Now with pagination support
     */
    fun loadActivityLog(projectId: String) {
        // Reset pagination state
        currentOffset = 0
        allActivitiesLoaded = false

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, projectId = projectId) }

                // Collect activity from repository
                taskRepository.getActivityForProjectFlow(projectId).collect { activities ->
                    // Phase 4 TODO FIX: Implement pagination with offset
                    val paginatedActivities = activities.sortedByDescending { it.timestamp }.take(PAGE_SIZE)
                    allActivitiesLoaded = activities.size <= PAGE_SIZE
                    currentOffset = paginatedActivities.size

                    // Apply filters
                    val filteredActivities = applyFilters(paginatedActivities)

                    // Get unique users for filter dropdown
                    val uniqueUserIds = activities.map { it.actorId }.distinct()
                    val users = uniqueUserIds.mapNotNull { userId ->
                        userRepository.getUserById(userId)
                    }

                    _uiState.update {
                        it.copy(
                            activities = filteredActivities,
                            allActivities = activities,
                            paginatedActivities = paginatedActivities,
                            availableUsers = users,
                            isLoading = false,
                            hasMore = !allActivitiesLoaded
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load activity log: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update search query and re-filter
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFiltersToState()
    }

    /**
     * Filter by action type
     */
    fun filterByActionType(actionType: ActivityActionType?) {
        _selectedActionType.value = actionType
        applyFiltersToState()
    }

    /**
     * Filter by user
     */
    fun filterByUser(userId: String?) {
        _selectedUserId.value = userId
        applyFiltersToState()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedActionType.value = null
        _selectedUserId.value = null
        applyFiltersToState()
    }

    /**
     * Load more activities (pagination)
     * Phase 4 TODO FIX: Implemented pagination with offset
     */
    fun loadMore() {
        if (allActivitiesLoaded || _uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val allActivities = _uiState.value.allActivities
                val nextBatch = allActivities.drop(currentOffset).take(PAGE_SIZE)

                if (nextBatch.isNotEmpty()) {
                    currentOffset += nextBatch.size
                    allActivitiesLoaded = currentOffset >= allActivities.size

                    val currentPaginated = _uiState.value.paginatedActivities
                    val newPaginated = currentPaginated + nextBatch
                    val filtered = applyFilters(newPaginated)

                    _uiState.update {
                        it.copy(
                            activities = filtered,
                            paginatedActivities = newPaginated,
                            isLoadingMore = false,
                            hasMore = !allActivitiesLoaded
                        )
                    }
                } else {
                    allActivitiesLoaded = true
                    _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = "Failed to load more: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Apply current filters to state
     */
    private fun applyFiltersToState() {
        val currentState = _uiState.value
        val filtered = applyFilters(currentState.allActivities)
        _uiState.update { it.copy(activities = filtered) }
    }

    /**
     * Apply all active filters to activity list
     */
    private fun applyFilters(activities: List<TaskActivity>): List<TaskActivity> {
        var filtered = activities

        // Filter by action type
        _selectedActionType.value?.let { actionType ->
            filtered = filtered.filter { it.actionType == actionType }
        }

        // Filter by user
        _selectedUserId.value?.let { userId ->
            filtered = filtered.filter { it.actorId == userId }
        }

        // Search by commit message or description
        val query = _searchQuery.value.trim()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { activity ->
                activity.commitMessage?.contains(query, ignoreCase = true) == true ||
                        activity.autoDescription.contains(query, ignoreCase = true) ||
                        activity.actorName.contains(query, ignoreCase = true)
            }
        }

        return filtered
    }

    /**
     * Get filter summary text
     */
    fun getFilterSummary(): String {
        val filters = mutableListOf<String>()

        _selectedActionType.value?.let { filters.add(formatActionType(it)) }
        _selectedUserId.value?.let { userId ->
            val user = _uiState.value.availableUsers.find { it.id == userId }
            user?.let { filters.add(it.displayName) }
        }
        if (_searchQuery.value.isNotEmpty()) {
            filters.add("\"${_searchQuery.value}\"")
        }

        return when {
            filters.isEmpty() -> "All activity"
            filters.size == 1 -> filters.first()
            else -> filters.joinToString(" • ")
        }
    }

    /**
     * Format action type for display
     */
    private fun formatActionType(actionType: ActivityActionType): String {
        return when (actionType) {
            ActivityActionType.CREATED -> "Created"
            ActivityActionType.UPDATED -> "Updated"
            ActivityActionType.STATUS_CHANGED -> "Status Changed"
            ActivityActionType.PRIORITY_CHANGED -> "Priority Changed"
            ActivityActionType.ASSIGNED -> "Assigned"
            ActivityActionType.UNASSIGNED -> "Unassigned"
            ActivityActionType.DESCRIPTION_CHANGED -> "Description Changed"
            ActivityActionType.DUE_DATE_CHANGED -> "Due Date Changed"
            ActivityActionType.TAGS_UPDATED -> "Tags Updated"
            ActivityActionType.COMMENT_ADDED -> "Comment Added"
            ActivityActionType.TIME_LOGGED -> "Time Logged"
            ActivityActionType.DEPENDENCY_ADDED -> "Dependency Added"
            ActivityActionType.DEPENDENCY_REMOVED -> "Dependency Removed"
            ActivityActionType.SUBTASK_ADDED -> "Subtask Added"
            ActivityActionType.ARCHIVED -> "Archived"
            ActivityActionType.RESTORED -> "Restored"
            ActivityActionType.DELETED -> "Deleted"
            ActivityActionType.JOURNAL_ENTRY -> "Journal Entry"
        }
    }
}

/**
 * UI State for Activity Log Screen
 */
data class ActivityLogUiState(
    val projectId: String = "",
    val activities: List<TaskActivity> = emptyList(),
    val allActivities: List<TaskActivity> = emptyList(),
    val paginatedActivities: List<TaskActivity> = emptyList(),  // Phase 4 TODO FIX: Added for pagination
    val availableUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)
