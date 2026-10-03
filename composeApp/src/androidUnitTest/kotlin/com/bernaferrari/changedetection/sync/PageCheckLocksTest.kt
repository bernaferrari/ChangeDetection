/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageCheckLocksTest {
    @Test fun parallelChecksNeverEnterTheSamePageTogether() = runTest {
        val locks = PageCheckLocks()
        val active = java.util.concurrent.atomic.AtomicIntegerArray(8)
        kotlinx.coroutines.coroutineScope {
            repeat(1_000) { index ->
                launch(kotlinx.coroutines.Dispatchers.Default) {
                    val key = index % 8
                    locks.withLock("page-$key") {
                        assertTrue("Page $key had overlapping checks", active.compareAndSet(key, 0, 1))
                        kotlinx.coroutines.yield()
                        active.set(key, 0)
                    }
                }
            }
        }
        assertEquals(0, locks.trackedPageCount())
    }

    @Test fun completedPagesDoNotAccumulateLocks() = runTest {
        val locks = PageCheckLocks()
        repeat(10_000) { id -> locks.withLock("page-$id") { assertTrue(locks.isLocked("page-$id")) } }
        assertEquals(0, locks.trackedPageCount())
    }

    @Test fun cancelledWaiterCannotSplitTheRemainingQueueAcrossLocks() = runTest {
        val locks = PageCheckLocks()
        val releaseFirst = CompletableDeferred<Unit>()
        val order = mutableListOf<Int>()
        val first = launch {
            locks.withLock("same") {
                order += 1
                releaseFirst.await()
            }
        }
        runCurrent()
        val cancelled = launch { locks.withLock("same") { error("Cancelled waiter must not run") } }
        val next = launch { locks.withLock("same") { order += 2 } }
        runCurrent()
        cancelled.cancel()
        cancelled.join()
        assertEquals(1, locks.trackedPageCount())
        assertEquals("busy", locks.tryWithLock("same", { "busy" }) { "incorrectly entered" })
        assertEquals(listOf(1), order)
        releaseFirst.complete(Unit)
        first.join()
        next.join()
        assertEquals(listOf(1, 2), order)
        assertEquals(0, locks.trackedPageCount())
    }

    @Test fun equalHashCodesDoNotBlockDifferentPages() = runTest {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val locks = PageCheckLocks()
        val release = CompletableDeferred<Unit>()
        val first = launch { locks.withLock("Aa") { release.await() } }
        runCurrent()
        assertEquals("entered", locks.tryWithLock("BB", { "busy" }) { "entered" })
        release.complete(Unit)
        first.join()
        assertEquals(0, locks.trackedPageCount())
    }

    @Test fun failedAndCancelledOwnersReleaseTheirLocks() = runTest {
        val locks = PageCheckLocks()
        runCatching { locks.withLock("failed") { error("failure") } }
        val owner = launch { locks.withLock("cancelled") { CompletableDeferred<Unit>().await() } }
        runCurrent()
        owner.cancel()
        owner.join()
        assertEquals(0, locks.trackedPageCount())
        assertEquals("entered", locks.tryWithLock("cancelled", { "busy" }) { "entered" })
    }
}
