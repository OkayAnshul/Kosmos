package com.example.kosmos.core.feedback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFeedbackManager @Inject constructor() {

    private val _events = MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<FeedbackEvent> = _events.asSharedFlow()

    /** Post an event. Safe to call from any thread (SharedFlow is thread-safe). */
    fun post(event: FeedbackEvent) {
        _events.tryEmit(event)
    }

    // Convenience helpers
    fun permissionDenied(action: String, reason: String, requiredRole: String? = null) =
        post(FeedbackEvent.PermissionDenied(action, reason, requiredRole))

    fun syncWarning(message: String) = post(FeedbackEvent.SyncWarning(message))

    fun error(message: String, retryAction: (() -> Unit)? = null) =
        post(FeedbackEvent.Error(message, retryAction))

    fun success(message: String) = post(FeedbackEvent.Success(message))

    fun info(message: String) = post(FeedbackEvent.Info(message))
}
