/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import androidx.datastore.preferences.core.*
import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TurboPreferencesTest {
    @Test fun interruptedSessionRetainsConfigurationWithoutResumingChecks() = runTest {
        val stored = mutablePreferencesOf()
        TurboPreferences.write(stored, TurboState(isActive = true, siteIds = setOf("page-b", "page-a"), intervalSeconds = 15, durationMinutes = 30, startTime = 100, deadlineTime = 1_800_100))
        var checks = 0
        val session = TurboSession(
            backgroundScope,
            CheckRunner {
                checks++
                SyncResult.Success(it, false)
            },
        )
        session.restore(TurboPreferences.read(stored))
        assertFalse(session.state.value.isActive)
        assertEquals(TurboEndReason.Interrupted, session.state.value.endReason)
        assertEquals(setOf("page-a", "page-b"), session.state.value.siteIds)
        assertEquals(30, session.state.value.durationMinutes)
        assertEquals(15, session.state.value.intervalSeconds)
        assertEquals(1_800_100L, session.state.value.deadlineTime)
        assertEquals(0, checks)
    }

    @Test fun olderPreferencesDefaultDurationAndUnknownOutcomeSafely() {
        val stored = mutablePreferencesOf(stringPreferencesKey("quick_monitor_site_ids") to "page-a,,page-b", stringPreferencesKey("quick_monitor_end_reason") to "future-reason")
        val state = TurboPreferences.read(stored)
        assertEquals(setOf("page-a", "page-b"), state.siteIds)
        assertEquals(180, state.durationMinutes)
        assertNull(state.endReason)
        assertFalse(state.isActive)
    }

    @Test fun stoppedRecordReplacesRunningRecordAndPreservesOtherSettings() {
        val stored = mutablePreferencesOf(MonitoringSchedule.INTERVAL to 10080L)
        TurboPreferences.write(stored, TurboState(isActive = true, siteIds = setOf("page-a")))
        val stopped = TurboState(siteIds = setOf("page-b"), durationMinutes = 15, endReason = TurboEndReason.Stopped)
        TurboPreferences.write(stored, stopped)
        assertEquals(stopped, TurboPreferences.read(stored))
        assertEquals(10080L, stored[MonitoringSchedule.INTERVAL])
    }
}
