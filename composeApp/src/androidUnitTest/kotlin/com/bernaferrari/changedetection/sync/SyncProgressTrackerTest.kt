/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProgressTrackerTest {
    @Test
    fun `nested work keeps page active until final operation completes`() = runTest {
        val tracker = SyncProgressTracker()

        tracker.track("page") {
            assertEquals(setOf("page"), tracker.activeSiteIds.value)
            tracker.track("page") {
                assertEquals(setOf("page"), tracker.activeSiteIds.value)
            }
            assertEquals(setOf("page"), tracker.activeSiteIds.value)
        }

        assertTrue(tracker.activeSiteIds.value.isEmpty())
    }

    @Test
    fun `cancellation always clears active page`() = runTest {
        val tracker = SyncProgressTracker()
        val job = async {
            tracker.track("page") {
                throw CancellationException("stop")
            }
        }

        runCatching { job.await() }
        assertTrue(tracker.activeSiteIds.value.isEmpty())
    }
}
