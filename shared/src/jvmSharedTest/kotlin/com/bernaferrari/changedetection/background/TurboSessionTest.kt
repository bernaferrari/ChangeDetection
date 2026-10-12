package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TurboSessionTest {
    private fun TestScope.session(check: suspend (String) -> SyncResult) = TurboSession(
        backgroundScope,
        CheckRunner(check),
        object : MonitorClock {
            override fun wallTimeMillis() = 1_000_000L + testScheduler.currentTime
            override fun elapsedMillis() = testScheduler.currentTime
        },
    )

    @Test
    fun `quick checks run every five seconds`() = runTest {
        val ticks = mutableListOf<Long>()
        val session = session { id ->
            ticks += testScheduler.currentTime
            SyncResult.Success(id, false)
        }
        session.start(setOf("page"), 5, 15)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(listOf(0L, 5_000L, 10_000L), ticks)
        session.stop()
    }

    @Test
    fun `five second checks keep cadence without overlapping slow captures`() = runTest {
        val ticks = mutableListOf<Long>()
        var running = 0
        val session = session { id ->
            assertEquals(0, running)
            running++
            ticks += testScheduler.currentTime
            delay(7_000)
            running--
            SyncResult.Success(id, false)
        }
        session.start(setOf("page"), 5, 15)
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf(0L, 12_000L), ticks)
        assertEquals(5, session.state.value.intervalSeconds)
        session.stop()
    }

    @Test
    fun `dispatch delay does not extend the deadline announced at start`() = runTest {
        var elapsedOffset = 0L
        val session = TurboSession(
            backgroundScope,
            CheckRunner { SyncResult.Success(it, false) },
            object : MonitorClock {
                override fun wallTimeMillis() = testScheduler.currentTime
                override fun elapsedMillis() = testScheduler.currentTime + elapsedOffset
            },
        )
        session.start(setOf("page"), 60, 15)
        elapsedOffset = 5_000
        runCurrent()
        assertEquals(895_000L, session.state.value.remainingTimeMillis())
        advanceTimeBy(895_000)
        runCurrent()
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
        assertFalse(session.state.value.isActive)
        assertNull(session.state.value.remainingTimeSource)
    }

    @Test
    fun `remaining time follows elapsed lifetime despite wall clock corrections`() = runTest {
        var wallOffset = 0L
        val session = TurboSession(
            backgroundScope,
            CheckRunner { SyncResult.Success(it, false) },
            object : MonitorClock {
                override fun wallTimeMillis() = 1_000_000L + wallOffset + testScheduler.currentTime
                override fun elapsedMillis() = testScheduler.currentTime
            },
        )
        session.start(setOf("page"), 60, 15)
        runCurrent()
        assertEquals(15 * 60_000L, session.state.value.remainingTimeMillis())
        advanceTimeBy(60_000)
        wallOffset = 24 * 3600_000L
        assertEquals(14 * 60_000L, session.state.value.remainingTimeMillis(Long.MAX_VALUE))
        wallOffset = -24 * 3600_000L
        advanceTimeBy(60_000)
        assertEquals(13 * 60_000L, session.state.value.remainingTimeMillis(0))
        session.start(setOf("page"), 30, 180)
        assertEquals(13 * 60_000L, session.state.value.remainingTimeMillis())
        advanceTimeBy(13 * 60_000L)
        runCurrent()
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
        assertEquals(0L, session.state.value.remainingTimeMillis())
    }

    @Test
    fun `saved update count excludes baselines failures skips and unsaved results`() = runTest {
        var calls = 0
        val session = session { id ->
            when (calls++) {
                0 -> SyncResult.Success(id, false, snapId = "baseline")
                1 -> SyncResult.Success(id, true, snapId = "saved-change")
                2 -> SyncResult.Success(id, true)
                3 -> throw IllegalStateException("Failed capture")
                else -> SyncResult.Skipped(id, "Already checking")
            }
        }
        session.start(setOf("page"), 15, 15)
        runCurrent()
        repeat(4) {
            advanceTimeBy(15_000)
            runCurrent()
        }
        assertEquals(3, session.state.value.successfulChecks)
        assertEquals(1, session.state.value.failedChecks)
        assertEquals(1, session.state.value.updatesSaved)
        session.stop()
        assertEquals(1, session.state.value.updatesSaved)
        session.start(setOf("page"), 30, 15)
        assertEquals(0, session.state.value.updatesSaved)
        session.stop()
    }

    @Test
    fun `restoring an interrupted process keeps configuration without starting checks`() = runTest {
        var checks = 0
        val session = session { id ->
            checks++
            SyncResult.Success(id, false)
        }
        session.restore(TurboState(isActive = true, siteIds = setOf("saved"), intervalSeconds = 15, startTime = 100, deadlineTime = 200))
        runCurrent()
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Interrupted, session.state.value.endReason)
        assertEquals(setOf("saved"), session.state.value.siteIds)
        assertEquals(15, session.state.value.intervalSeconds)
        assertEquals(200L, session.state.value.deadlineTime)
        assertEquals(0, checks)
    }

    @Test
    fun `late restoration cannot overwrite a newly started or stopped session`() = runTest {
        val session = session { id -> SyncResult.Success(id, false) }
        session.start(setOf("new"), 60)
        session.restore(TurboState(isActive = true, siteIds = setOf("old")))
        assertTrue(session.state.value.isActive)
        assertEquals(setOf("new"), session.state.value.siteIds)
        session.stop()
        session.restore(TurboState(isActive = true, siteIds = setOf("old")))
        assertEquals(TurboEndReason.Stopped, session.state.value.endReason)
        assertEquals(setOf("new"), session.state.value.siteIds)
    }

    @Test
    fun `restoration preserves a completed outcome and validates legacy cadence`() = runTest {
        val session = session { id -> SyncResult.Success(id, false) }
        session.restore(TurboState(endReason = TurboEndReason.Expired, intervalSeconds = 120))
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
        assertEquals(30, session.state.value.intervalSeconds)
    }

    @Test
    fun `shortening cadence takes effect during the current wait without extending the session`() = runTest {
        val ticks = mutableListOf<Long>()
        val session = session { id ->
            ticks += testScheduler.currentTime
            SyncResult.Success(id, false)
        }
        session.start(setOf("one"), 60)
        runCurrent()
        val deadline = session.state.value.deadlineTime
        advanceTimeBy(10_000)
        session.start(setOf("one"), 15)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf(0L, 15_000L), ticks)
        assertEquals(deadline, session.state.value.deadlineTime)
        session.stop()
    }

    @Test
    fun `expiry cancels an in flight check exactly at the session limit`() = runTest {
        var cancelled = false
        var completed = false
        val session = session { id ->
            try {
                delay(TURBO_DURATION_MS + 1)
                completed = true
                SyncResult.Success(id, false)
            } finally {
                cancelled = true
            }
        }
        session.start(setOf("one"), 30)
        runCurrent()
        advanceTimeBy(TURBO_DURATION_MS)
        runCurrent()
        assertTrue(cancelled)
        assertFalse(completed)
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
        assertEquals(0, session.state.value.successfulChecks)
    }

    @Test
    fun `late completion from a stopped session cannot change its replacement`() = runTest {
        val session = session { id ->
            if (id == "old") kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { delay(1_000) }
            SyncResult.Success(id, false)
        }
        session.start(setOf("old"), 30)
        runCurrent()
        session.stop()
        session.start(setOf("new"), 15)
        runCurrent()
        val replacement = session.state.value
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(replacement, session.state.value)
        assertTrue(session.state.value.isActive)
        assertEquals(setOf("new"), session.state.value.siteIds)
        assertEquals(1, session.state.value.successfulChecks)
        session.stop()
    }

    @Test
    fun `lengthening cadence cancels the earlier pending tick`() = runTest {
        val ticks = mutableListOf<Long>()
        val session = session { id ->
            ticks += testScheduler.currentTime
            SyncResult.Success(id, false)
        }
        session.start(setOf("one"), 15)
        runCurrent()
        advanceTimeBy(10_000)
        session.start(setOf("one"), 60)
        runCurrent()
        advanceTimeBy(49_999)
        runCurrent()
        assertEquals(listOf(0L), ticks)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(0L, 60_000L), ticks)
        session.stop()
    }

    @Test
    fun `changing cadence during capture does not restart or overlap it`() = runTest {
        val ticks = mutableListOf<Long>()
        val session = session { id ->
            ticks += testScheduler.currentTime
            delay(40_000)
            SyncResult.Success(id, false)
        }
        session.start(setOf("one"), 60)
        runCurrent()
        advanceTimeBy(10_000)
        session.start(setOf("one"), 15)
        runCurrent()
        advanceTimeBy(45_000)
        runCurrent()
        assertEquals(listOf(0L, 55_000L), ticks)
        assertEquals(1, session.state.value.successfulChecks)
        session.stop()
    }

    @Test
    fun `a page timeout counts as failure and the page checks again`() = runTest {
        var attempts = 0
        val session = session { id ->
            attempts++
            if (attempts == 1) kotlinx.coroutines.withTimeout(1_000) { delay(2_000) }
            SyncResult.Success(id, false)
        }
        session.start(setOf("one"), 30)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(1, session.state.value.failedChecks)
        assertEquals(1, session.state.value.successfulChecks)
        assertTrue(session.state.value.isActive)
        session.stop()
    }

    @Test
    fun `all sites check immediately and every thirty seconds independent of count`() = runTest {
        val ticks = mutableListOf<Pair<String, Long>>()
        val session = session { id ->
            ticks += id to testScheduler.currentTime
            delay(2_000)
            SyncResult.Success(id, false)
        }
        session.start(setOf("one", "two", "three"), 30)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(6, ticks.size)
        assertEquals(setOf(0L, 30_000L), ticks.map { it.second }.toSet())
        session.stop()
    }

    @Test
    fun `stop cancels in flight checks and no checks start afterwards`() = runTest {
        var started = 0
        var completed = 0
        val session = session { id ->
            started++
            delay(40_000)
            completed++
            SyncResult.Success(id, false)
        }
        session.start(setOf("one"), 30)
        runCurrent()
        session.stop()
        advanceTimeBy(TURBO_DURATION_MS + 1)
        runCurrent()
        assertEquals(1, started)
        assertEquals(0, completed)
        assertFalse(session.state.value.isActive)
    }

    @Test
    fun `expiry cancels work at three hours even when selection changes`() = runTest {
        val session = session { id -> SyncResult.Success(id, false) }
        session.start(setOf("one"), 30)
        runCurrent()
        val deadline = session.state.value.deadlineTime
        advanceTimeBy(TURBO_DURATION_MS - 1_000)
        session.start(setOf("one", "two"), 15)
        assertEquals(deadline, session.state.value.deadlineTime)
        advanceTimeBy(1_000)
        runCurrent()
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
    }

    @Test
    fun `removing a page cancels only that page and does not restart others`() = runTest {
        val ticks = mutableListOf<String>()
        val session = session { id ->
            ticks += id
            delay(20_000)
            SyncResult.Success(id, false)
        }
        session.start(setOf("one", "two"), 30)
        runCurrent()
        advanceTimeBy(10_000)
        session.start(setOf("one"), 30)
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf("one", "two", "one"), ticks)
        session.stop()
    }

    @Test
    fun `slow failed check retries without overlap or catch up burst`() = runTest {
        val ticks = mutableListOf<Long>()
        val session = session { _ ->
            ticks += testScheduler.currentTime
            delay(45_000)
            error("offline")
        }
        session.start(setOf("one"), 30)
        runCurrent()
        advanceTimeBy(75_000)
        runCurrent()
        assertEquals(listOf(0L, 75_000L), ticks)
        assertEquals(1, session.state.value.failedChecks)
        session.stop()
    }
}
