package com.example.kosmos.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.models.TimeEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeEntryDaoTest {

    private lateinit var db: KosmosDatabase
    private lateinit var dao: TimeEntryDao

    private val taskId = "task-time-1"
    private val projectId = "proj-time-1"
    private val userId = "user-time-1"

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        dao = db.timeEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(
        id: String = java.util.UUID.randomUUID().toString(),
        startTime: Long = System.currentTimeMillis() - 3_600_000L,
        endTime: Long? = System.currentTimeMillis(),
        durationSeconds: Int? = 3600,
        isBillable: Boolean = true,
        isManual: Boolean = false
    ) = TimeEntry(
        id = id,
        taskId = taskId,
        projectId = projectId,
        userId = userId,
        startTime = startTime,
        endTime = endTime,
        durationSeconds = durationSeconds,
        isBillable = isBillable,
        isManual = isManual
    )

    @Test
    fun insertEntry_savesToRoom() = runTest {
        val e = entry()
        dao.insertEntry(e)
        assertNotNull(dao.getEntryById(e.id))
    }

    @Test
    fun getEntryById_returnsCorrectEntry() = runTest {
        val e = entry(id = "specific-entry")
        dao.insertEntry(e)
        val fetched = dao.getEntryById("specific-entry")
        assertNotNull(fetched)
        assertEquals(taskId, fetched?.taskId)
    }

    @Test
    fun getEntriesForTaskFlow_emitsInsertedEntries() = runTest {
        dao.insertEntry(entry())
        dao.insertEntry(entry())
        val entries = dao.getEntriesForTaskFlow(taskId).first()
        assertEquals(2, entries.size)
    }

    @Test
    fun getRunningTimersFlow_onlyRunningEntries() = runTest {
        val running = entry(endTime = null, durationSeconds = null)
        val stopped = entry()
        dao.insertEntry(running)
        dao.insertEntry(stopped)

        val runningTimers = dao.getRunningTimersFlow(userId).first()
        assertEquals(1, runningTimers.size)
        assertTrue(runningTimers[0].isRunning())
    }

    @Test
    fun hasRunningTimer_runningEntryExists_returnsTrue() = runTest {
        val running = entry(endTime = null, durationSeconds = null)
        dao.insertEntry(running)
        assertTrue(dao.hasRunningTimer(userId))
    }

    @Test
    fun hasRunningTimer_noRunningEntry_returnsFalse() = runTest {
        dao.insertEntry(entry()) // stopped entry
        val result = dao.hasRunningTimer(userId)
        assertEquals(false, result)
    }

    @Test
    fun getTotalTimeForTask_sumsCompletedEntries() = runTest {
        dao.insertEntry(entry(durationSeconds = 1800))
        dao.insertEntry(entry(durationSeconds = 900))
        dao.insertEntry(entry(endTime = null, durationSeconds = null)) // running, excluded
        val total = dao.getTotalTimeForTask(taskId)
        assertEquals(2700, total)
    }

    @Test
    fun getBillableTimeForTask_sumsOnlyBillable() = runTest {
        dao.insertEntry(entry(durationSeconds = 3600, isBillable = true))
        dao.insertEntry(entry(durationSeconds = 1800, isBillable = false))
        val billable = dao.getBillableTimeForTask(taskId)
        assertEquals(3600, billable)
    }

    @Test
    fun deleteEntriesForTask_removesAll() = runTest {
        dao.insertEntry(entry())
        dao.insertEntry(entry())
        dao.deleteEntriesForTask(taskId)
        val entries = dao.getEntriesForTaskFlow(taskId).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun deleteEntryById_removesSpecificEntry() = runTest {
        val e1 = entry(id = "e1")
        val e2 = entry(id = "e2")
        dao.insertEntry(e1)
        dao.insertEntry(e2)
        dao.deleteEntryById("e1")
        assertNull(dao.getEntryById("e1"))
        assertNotNull(dao.getEntryById("e2"))
    }

    @Test
    fun timeEntry_isRunning_noEndTime_returnsTrue() {
        val running = TimeEntry.createTimer(taskId, projectId, userId)
        assertTrue(running.isRunning())
    }

    @Test
    fun timeEntry_stop_setsEndTimeAndDuration() {
        val start = System.currentTimeMillis() - 3600_000L
        val timer = TimeEntry.createTimer(taskId, projectId, userId, startTime = start)
        val stopped = timer.stop(start + 3600_000L)
        assertEquals(3600, stopped.durationSeconds)
        assertEquals(start + 3600_000L, stopped.endTime)
    }
}
