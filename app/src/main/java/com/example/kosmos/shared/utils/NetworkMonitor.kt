package com.example.kosmos.shared.utils

import kotlinx.coroutines.flow.StateFlow

/**
 * Network Monitor Interface
 *
 * Monitors network connectivity state
 */
interface NetworkMonitor {
    /**
     * StateFlow that emits true when device is offline, false when online
     */
    val isOffline: StateFlow<Boolean>
}
