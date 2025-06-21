package com.example.kosmos.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1-12: Dispatcher Provider for proper threading
 *
 * Provides coroutine dispatchers for different operations:
 * - IO: Network requests, database operations, file I/O
 * - Default: CPU-intensive operations, data processing
 * - Main: UI updates (already handled by Compose/viewModelScope)
 *
 * Benefits:
 * - Prevents NetworkOnMainThreadException
 * - Testable (can inject test dispatchers)
 * - Consistent threading across app
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/**
 * Production dispatcher provider using standard Kotlin coroutine dispatchers
 */
@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
