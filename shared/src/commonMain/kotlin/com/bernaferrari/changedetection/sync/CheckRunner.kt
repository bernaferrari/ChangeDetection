package com.bernaferrari.changedetection.sync

/** One awaited observation, including capture, comparison, persistence and notification. */
fun interface CheckRunner {
    suspend fun check(siteId: String): SyncResult
}
