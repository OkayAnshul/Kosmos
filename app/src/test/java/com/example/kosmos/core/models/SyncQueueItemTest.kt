package com.example.kosmos.core.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SyncQueueItemTest {

    private fun item(
        retryCount: Int = 0,
        maxRetries: Int = 5,
        lastAttemptTimestamp: Long = System.currentTimeMillis()
    ) = SyncQueueItem(
        entityType = SyncEntityType.TASK,
        entityId = "task-1",
        operation = SyncOperation.CREATE,
        entityJson = "{}",
        retryCount = retryCount,
        maxRetries = maxRetries,
        lastAttemptTimestamp = lastAttemptTimestamp
    )

    @Test
    fun getNextRetryDelayMs_retry0_returns1Second() {
        assertEquals(1_000L, item(retryCount = 0).getNextRetryDelayMs())
    }

    @Test
    fun getNextRetryDelayMs_retry1_returns2Seconds() {
        assertEquals(2_000L, item(retryCount = 1).getNextRetryDelayMs())
    }

    @Test
    fun getNextRetryDelayMs_retry2_returns4Seconds() {
        assertEquals(4_000L, item(retryCount = 2).getNextRetryDelayMs())
    }

    @Test
    fun getNextRetryDelayMs_retry3_returns8Seconds() {
        assertEquals(8_000L, item(retryCount = 3).getNextRetryDelayMs())
    }

    @Test
    fun getNextRetryDelayMs_retry6_cappedAt60Seconds() {
        // 2^6 = 64s > cap of 60s
        assertEquals(60_000L, item(retryCount = 6).getNextRetryDelayMs())
    }

    @Test
    fun getNextRetryDelayMs_retry10_cappedAt60Seconds() {
        assertEquals(60_000L, item(retryCount = 10).getNextRetryDelayMs())
    }

    @Test
    fun shouldRetryNow_notExceededAndLongEnoughWait_returnsTrue() {
        // retryCount=0 → delay=1s; lastAttempt was 2s ago
        val item = item(retryCount = 0, lastAttemptTimestamp = System.currentTimeMillis() - 2_000L)
        assertTrue(item.shouldRetryNow())
    }

    @Test
    fun shouldRetryNow_tooSoon_returnsFalse() {
        // retryCount=2 → delay=4s; lastAttempt was only 1s ago
        val item = item(retryCount = 2, lastAttemptTimestamp = System.currentTimeMillis() - 1_000L)
        assertFalse(item.shouldRetryNow())
    }

    @Test
    fun shouldRetryNow_exceededMaxRetries_returnsFalse() {
        val item = item(retryCount = 5, maxRetries = 5, lastAttemptTimestamp = 0L)
        assertFalse(item.shouldRetryNow())
    }

    @Test
    fun hasExceededMaxRetries_atMaxRetries_returnsTrue() {
        assertTrue(item(retryCount = 5, maxRetries = 5).hasExceededMaxRetries())
    }

    @Test
    fun hasExceededMaxRetries_overMaxRetries_returnsTrue() {
        assertTrue(item(retryCount = 6, maxRetries = 5).hasExceededMaxRetries())
    }

    @Test
    fun hasExceededMaxRetries_belowMaxRetries_returnsFalse() {
        assertFalse(item(retryCount = 4, maxRetries = 5).hasExceededMaxRetries())
    }

    @Test
    fun hasExceededMaxRetries_zero_returnsFalse() {
        assertFalse(item(retryCount = 0, maxRetries = 5).hasExceededMaxRetries())
    }
}
