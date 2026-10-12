package com.bernaferrari.changedetection.storage

import com.bernaferrari.changedetection.repo.Snap

/** History policy expressed independently of databases and file systems. */
data class SnapshotRetention(
    val versionsPerPage: Int = 100,
    val byteBudget: Long = 250L * 1024 * 1024,
) {
    init {
        require(versionsPerPage >= 2)
        require(byteBudget > 0)
    }

    /** Always preserve the newest pair, even if those essential versions exceed the budget. */
    fun obsoleteVersions(snapshots: List<Snap>): List<Snap> {
        val newestFirst = snapshots.sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId })
        val byPage = newestFirst.groupBy { it.siteId }
        val protected = byPage.values.flatMap { it.take(2) }.mapTo(mutableSetOf()) { it.snapId }
        val obsolete = byPage.values.flatMap { it.drop(versionsPerPage) }.associateByTo(linkedMapOf()) { it.snapId }
        var retainedBytes = newestFirst.sumOf { it.contentSize.toLong().coerceAtLeast(0) } - obsolete.values.sumOf { it.contentSize.toLong().coerceAtLeast(0) }
        for (snap in newestFirst.asReversed()) {
            if (retainedBytes <= byteBudget) break
            if (snap.snapId !in protected && snap.snapId !in obsolete) {
                obsolete[snap.snapId] = snap
                retainedBytes -= snap.contentSize.toLong().coerceAtLeast(0)
            }
        }
        return obsolete.values.toList()
    }
}
