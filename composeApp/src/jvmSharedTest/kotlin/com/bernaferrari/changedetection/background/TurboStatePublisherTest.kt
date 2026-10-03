/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TurboStatePublisherTest {
    private fun TestScope.session(changes: Boolean = false) = TurboSession(
        backgroundScope,
        CheckRunner { SyncResult.Success(it, changes, snapId = if (changes) "saved" else null) },
        object : MonitorClock {
            override fun wallTimeMillis() = 1_000_000L + testScheduler.currentTime
            override fun elapsedMillis() = testScheduler.currentTime
        },
    )

    @Test fun `initial storage failure recovers without overwriting a stop performed meanwhile`() = runTest {
        val session = session()
        var reads = 0
        val saved = mutableListOf<TurboState>()
        val errors = mutableListOf<Throwable>()
        backgroundScope.launch {
            publishTurboState(session, read = {
                if (reads++ == 0) error("Storage unavailable")
                TurboState(isActive = true, siteIds = setOf("old"))
            }, publish = { saved += it }, onFailure = { errors += it })
        }
        runCurrent()
        assertTrue(saved.isEmpty())
        session.start(setOf("new"), 15)
        session.stop()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, reads)
        assertEquals(1, errors.size)
        assertFalse(saved.single().isActive)
        assertEquals(setOf("new"), saved.single().siteIds)
        assertEquals(TurboEndReason.Stopped, saved.single().endReason)
    }

    @Test fun `publication failure retries current stopped state instead of replaying active state`() = runTest {
        val session = session()
        var stored = TurboState()
        val delivered = mutableListOf<TurboState>()
        val errors = mutableListOf<Throwable>()
        backgroundScope.launch {
            publishTurboState(session, read = { stored }, publish = {
                stored = it
                if (it.isActive) error("Widget refresh failed after storage committed")
                delivered += it
            }, onFailure = { errors += it })
        }
        runCurrent()
        session.start(setOf("page"), 30)
        runCurrent()
        assertTrue(stored.isActive)
        session.stop()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, errors.size)
        assertFalse(stored.isActive)
        assertEquals(TurboEndReason.Stopped, stored.endReason)
        assertEquals(stored, delivered.last())
        assertTrue(delivered.none { it.isActive })
    }

    @Test fun `check counters do not rewrite preferences or redraw widgets every tick`() = runTest {
        val session = session(changes = true)
        val saved = mutableListOf<TurboState>()
        backgroundScope.launch { publishTurboState(session, { TurboState() }, { saved += it }, { throw it }) }
        runCurrent()
        session.start(setOf("page"), 15)
        runCurrent()
        advanceTimeBy(45_000)
        runCurrent()
        assertTrue(session.state.value.successfulChecks >= 3)
        assertTrue(session.state.value.updatesSaved >= 3)
        assertEquals(2, saved.size)
        assertEquals(0, saved.last().successfulChecks)
        assertEquals(0, saved.last().updatesSaved)
        assertNull(saved.last().remainingTimeSource)
    }

    @Test fun `cancellation ends publication without retrying or logging a failure`() = runTest {
        var reads = 0
        val errors = mutableListOf<Throwable>()
        val publisher = backgroundScope.launch {
            publishTurboState(session(), {
                reads++
                TurboState()
            }, { throw CancellationException("Application stopped") }, { errors += it })
        }
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertTrue(publisher.isCancelled)
        assertTrue(errors.isEmpty())
        assertEquals(1, reads)
    }
}
