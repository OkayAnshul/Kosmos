package com.example.kosmos.core.feedback

import android.util.Log
import com.example.kosmos.core.validators.PermissionChecker
import kotlinx.coroutines.CancellationException

/**
 * Wraps a suspend block and routes known exceptions to UserFeedbackManager.
 *
 * Usage in a ViewModel:
 *   safeCall(feedbackManager, tag = "MembersListViewModel") {
 *       projectInviteRepository.syncFromSupabase(projectId)
 *   }
 *
 * @param feedbackManager The singleton to post events to
 * @param tag Log tag for debugging
 * @param action Human-readable description of what was attempted (for error messages)
 * @param isCritical If true, posts Error event (user-visible); if false, posts SyncWarning
 * @param block The suspend block to execute
 */
suspend fun safeCall(
    feedbackManager: UserFeedbackManager,
    tag: String = "safeCall",
    action: String = "complete this action",
    isCritical: Boolean = false,
    block: suspend () -> Unit
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e  // NEVER swallow CancellationException — breaks structured concurrency
    } catch (e: PermissionChecker.PermissionDeniedException) {
        Log.w(tag, "Permission denied for action '$action'", e)
        feedbackManager.post(
            FeedbackEvent.PermissionDenied(
                action = action,
                reason = e.message ?: "You don't have permission to $action"
            )
        )
    } catch (e: Exception) {
        Log.w(tag, "Failed to $action", e)
        if (isCritical) {
            feedbackManager.error("Failed to $action. ${e.message ?: "Please try again."}")
        } else {
            feedbackManager.syncWarning("Couldn't sync data — showing cached content")
        }
    }
}
