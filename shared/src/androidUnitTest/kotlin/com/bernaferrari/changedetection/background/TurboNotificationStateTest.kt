/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TurboNotificationStateTest {
    @Test fun `checks stay quiet while configuration stop and expiry publish immediately`() = runTest {
        var checks = 0
        val session = TurboSession(
            backgroundScope,
            CheckRunner {
                checks++
                SyncResult.Success(it, true, snapId = "saved-$checks")
            },
            object : MonitorClock {
                override fun wallTimeMillis() = 1_000_000L + testScheduler.currentTime
                override fun elapsedMillis() = testScheduler.currentTime
            },
        )
        val emitted = mutableListOf<TurboNotificationState>()
        backgroundScope.launch { session.state.notificationStates().collect { emitted += it } }
        runCurrent()
        session.start(setOf("a", "b"), 15, 15)
        runCurrent()
        assertEquals(TurboNotificationState(true, 2, 15), emitted.last())
        val initialCount = emitted.size
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(checks >= 8)
        assertTrue(session.state.value.updatesSaved >= 8)
        assertEquals(initialCount, emitted.size)

        // Different identities with the same displayed count do not rebuild the notification.
        session.start(setOf("b", "c"), 15, 15)
        runCurrent()
        assertEquals(initialCount, emitted.size)
        session.start(setOf("c"), 30, 15)
        runCurrent()
        assertEquals(initialCount + 1, emitted.size)
        assertEquals(TurboNotificationState(true, 1, 30), emitted.last())
        session.stop()
        runCurrent()
        assertEquals(TurboNotificationState(false, 1, 30, TurboEndReason.Stopped), emitted.last())

        session.start(setOf("c"), 120, 15)
        runCurrent()
        val restartedCount = emitted.size
        advanceTimeBy(15 * 60_000L)
        runCurrent()
        assertEquals(restartedCount + 1, emitted.size)
        assertEquals(TurboNotificationState(false, 1, 120, TurboEndReason.Expired), emitted.last())
    }
}
