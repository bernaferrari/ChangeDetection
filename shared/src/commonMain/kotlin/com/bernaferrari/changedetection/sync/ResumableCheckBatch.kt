package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site

/** Persist the attempted page before capture so an OS expiration cannot starve later pages. */
internal suspend fun checkResumableBatch(
    sites: List<Site>,
    cursor: String?,
    saveCursor: suspend (String?) -> Unit,
    check: suspend (String) -> SyncResult,
): Int {
    val ordered = sites.filter { it.isSyncEnabled }.sortedWith(compareBy<Site> { it.timestamp }.thenBy { it.id })
    val pivot = ordered.indexOfFirst { it.id == cursor }
    val resumed = if (pivot >= 0) ordered.drop(pivot + 1) + ordered.take(pivot + 1) else ordered
    return try {
        checkBatch(resumed.map { it.id }) { id ->
            saveCursor(id)
            check(id)
        }.also { saveCursor(null) }
    } catch (failure: CheckBatchFailure) {
        // All pages were attempted; only cancellation keeps a continuation cursor.
        saveCursor(null)
        throw failure
    }
}
