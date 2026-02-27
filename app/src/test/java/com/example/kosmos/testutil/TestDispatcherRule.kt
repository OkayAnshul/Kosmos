package com.example.kosmos.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit4 Rule that replaces all coroutine dispatchers with a TestDispatcher.
 * Apply with @get:Rule annotation in test classes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule : TestWatcher() {
    val scheduler = TestCoroutineScheduler()
    val testDispatcher = UnconfinedTestDispatcher(scheduler)

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
