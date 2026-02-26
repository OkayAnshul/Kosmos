package com.example.kosmos.core.feedback

sealed class FeedbackEvent {
    /** User tried an action they don't have permission for */
    data class PermissionDenied(
        val action: String,        // e.g. "delete this task"
        val reason: String,        // from PermissionChecker.Denied.reason
        val requiredRole: String? = null  // e.g. "Manager or higher"
    ) : FeedbackEvent()

    /** A background sync failed but app is still functional (shows cached data) */
    data class SyncWarning(val message: String) : FeedbackEvent()

    /** A user-initiated action failed */
    data class Error(
        val message: String,
        val retryAction: (() -> Unit)? = null
    ) : FeedbackEvent()

    /** A user-initiated action succeeded */
    data class Success(val message: String) : FeedbackEvent()

    /** Informational message (offline mode, etc.) */
    data class Info(val message: String) : FeedbackEvent()
}
