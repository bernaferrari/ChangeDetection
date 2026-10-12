package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TurboDurationTest {
    private fun TestScope.session(runner: CheckRunner = CheckRunner { SyncResult.Success(it, false) }) = TurboSession(
        backgroundScope,
        runner,
        object : MonitorClock {
            override fun wallTimeMillis() = 1_000_000L + testScheduler.currentTime
            override fun elapsedMillis() = testScheduler.currentTime
        },
    )

    @Test fun `short session expires at its selected deadline and cancels an in-flight check`() = runTest {
        var cancelled = false
        val session = session(
            CheckRunner {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            },
        )
        session.start(setOf("page"), 30, 15)
        runCurrent()
        assertEquals(15, session.state.value.durationMinutes)
        assertEquals(1_900_000L, session.state.value.deadlineTime)
        advanceTimeBy(15 * 60_000L - 1)
        runCurrent()
        assertTrue(session.state.value.isActive)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
        assertTrue(cancelled)
    }

    @Test fun `changing cadence or selected pages never extends a short session`() = runTest {
        val session = session()
        session.start(setOf("page"), 30, 15)
        advanceTimeBy(60_000)
        session.start(setOf("page", "another"), 15, 180)
        assertEquals(15, session.state.value.durationMinutes)
        assertEquals(1_900_000L, session.state.value.deadlineTime)
        advanceTimeBy(14 * 60_000L)
        runCurrent()
        assertEquals(TurboEndReason.Expired, session.state.value.endReason)
    }

    @Test fun `invalid duration cannot alter a running session`() = runTest {
        val session = session()
        session.start(setOf("page"), 30, 30)
        val before = session.state.value
        listOf(0, -1, 5, 181, Int.MAX_VALUE).forEach {
            try {
                session.start(setOf("other"), 15, it)
                fail("Must reject $it")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, session.state.value)
        }
    }

    @Test fun `restore retains the chosen duration but never resumes a session`() = runTest {
        val session = session()
        session.restore(TurboState(isActive = true, durationMinutes = 60))
        assertFalse(session.state.value.isActive)
        assertEquals(60, session.state.value.durationMinutes)
        assertEquals(TurboEndReason.Interrupted, session.state.value.endReason)
    }
}
