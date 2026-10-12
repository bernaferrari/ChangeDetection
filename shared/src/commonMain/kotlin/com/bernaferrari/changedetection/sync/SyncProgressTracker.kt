package com.bernaferrari.changedetection.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Process-wide source of truth for pages actively being checked. */
class SyncProgressTracker {
    private val lock = Mutex()
    private val activeCounts = mutableMapOf<String, Int>()
    private val _activeSiteIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSiteIds: StateFlow<Set<String>> = _activeSiteIds.asStateFlow()

    suspend fun <T> track(
        siteId: String,
        block: suspend () -> T,
    ): T {
        updateCount(siteId, delta = 1)
        return try {
            block()
        } finally {
            updateCount(siteId, delta = -1)
        }
    }

    private suspend fun updateCount(siteId: String, delta: Int) {
        lock.withLock {
            val next = (activeCounts[siteId] ?: 0) + delta
            if (next > 0) {
                activeCounts[siteId] = next
            } else {
                activeCounts.remove(siteId)
            }
            _activeSiteIds.value = activeCounts.keys.toSet()
        }
    }
}
