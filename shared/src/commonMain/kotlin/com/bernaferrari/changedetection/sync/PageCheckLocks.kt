package com.bernaferrari.changedetection.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serializes work for the same page without retaining locks for completed pages.
 * Waiters hold a reference before suspension, so cancellation cannot split a page
 * across two locks. Different page IDs remain independent, even with equal hashes.
 */
internal class PageCheckLocks {
    private class Entry(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val registry = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    suspend fun <T> withLock(pageId: String, block: suspend () -> T): T {
        val entry = retain(pageId)
        return try {
            entry.mutex.withLock { block() }
        } finally {
            release(pageId, entry)
        }
    }

    suspend fun <T> tryWithLock(pageId: String, onBusy: () -> T, block: suspend () -> T): T {
        val entry = retain(pageId)
        return try {
            if (!entry.mutex.tryLock()) {
                onBusy()
            } else {
                try {
                    block()
                } finally {
                    entry.mutex.unlock()
                }
            }
        } finally {
            release(pageId, entry)
        }
    }

    suspend fun isLocked(pageId: String): Boolean = registry.withLock { entries[pageId]?.mutex?.isLocked == true }

    /** Number of active or waiting keys, useful for diagnostics and stress verification. */
    suspend fun trackedPageCount(): Int = registry.withLock { entries.size }

    private suspend fun retain(pageId: String): Entry = registry.withLock {
        entries.getOrPut(pageId) { Entry() }.also { it.users++ }
    }

    private suspend fun release(pageId: String, entry: Entry) = withContext(NonCancellable) {
        registry.withLock {
            entry.users--
            if (entry.users == 0) entries.remove(pageId)
        }
    }
}
