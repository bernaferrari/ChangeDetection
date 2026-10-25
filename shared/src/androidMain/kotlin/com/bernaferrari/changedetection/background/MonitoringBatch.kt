package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.SyncResult
import com.bernaferrari.changedetection.error.BackgroundExecutionUnavailable
import kotlinx.coroutines.CancellationException

/** Durable remainder, not a promise that Android will grant uninterrupted execution. */
internal data class BatchCheckpoint(val remaining: Set<String> = emptySet(), val retryNeeded: Boolean = false, val cursor: String? = null)

/** Leave two minutes of ordinary worker budget for cancellation, persistence and OS overhead. */
internal suspend fun runMonitoringBatch(
    sites: List<Site>,
    checkpoint: BatchCheckpoint,
    save: suspend (BatchCheckpoint) -> Unit,
    elapsedMillis: () -> Long,
    budgetMs: Long = 8 * 60_000L,
    pageReserveMs: Long = 90_000L,
    check: suspend (Site, Int, Int) -> SyncResult,
): Boolean {
    require(pageReserveMs > 0 && budgetMs >= pageReserveMs)
    val started = elapsedMillis()
    val active = sites.filter { it.isSyncEnabled }.associateBy { it.id }
    val pending = checkpoint.remaining.intersect(active.keys).ifEmpty { active.keys }
    if (pending.isEmpty()) { save(BatchCheckpoint());return false }
    val sorted = pending.mapNotNull(active::get).sortedWith(compareBy<Site> { it.timestamp }.thenBy { it.id })
    // A worker stopped mid-capture keeps that page pending, but tries the other pending pages first.
    val pivot = sorted.indexOfFirst { it.id == checkpoint.cursor }
    val ordered = if (pivot >= 0) sorted.drop(pivot + 1) + sorted.take(pivot + 1) else sorted
    var state = BatchCheckpoint(pending, if (checkpoint.remaining.any { it in active }) checkpoint.retryNeeded else false)
    save(state)
    ordered.forEachIndexed { index, site ->
        if (elapsedMillis() - started > budgetMs - pageReserveMs) return true
        state = state.copy(cursor = site.id)
        save(state) // Durable before capture: cancellation must not lose its place.
        val result = check(site, index, ordered.size)
        state = state.copy(remaining = state.remaining - site.id, retryNeeded = state.retryNeeded || result is SyncResult.Failed && result.error.isRetryable)
        save(state)
    }
    val retry = state.retryNeeded
    save(BatchCheckpoint()) // New/deleted/paused pages are reconciled afresh on the next round.
    return retry
}

/** Capture cannot run if Android declined the foreground operation. Cancellation remains cancellation. */
internal suspend fun <T> checkAfterForeground(prepare: suspend () -> Unit, check: suspend () -> T, denied: suspend (BackgroundExecutionUnavailable) -> T): T {
    try {
        prepare()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        return denied(BackgroundExecutionUnavailable(error))
    }
    return check()
}
