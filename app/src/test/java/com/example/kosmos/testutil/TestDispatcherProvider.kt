package com.example.kosmos.testutil

import com.example.kosmos.core.coroutines.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test implementation of DispatcherProvider that uses a single test dispatcher
 * for all coroutine operations, making tests deterministic and synchronous.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}
