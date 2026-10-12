package com.bernaferrari.changedetection.storage

import com.bernaferrari.changedetection.repo.Snap
import org.junit.Assert.*
import org.junit.Test

class SnapshotRetentionTest {
    @Test fun keepsLatestHundredVersionsIncludingRepeatedContent() {
        val versions = (1..150).map { snap("page", it) }
        val obsolete = SnapshotRetention().obsoleteVersions(versions)
        assertEquals((1..50).map { "page-$it" }.toSet(), obsolete.map { it.snapId }.toSet())
    }

    @Test fun byteBudgetPrunesOldestVersionsAcrossPages() {
        val versions = (1..5).map { snap("one", it) } + (6..10).map { snap("two", it) }
        val obsolete = SnapshotRetention(byteBudget = 600).obsoleteVersions(versions)
        assertEquals(setOf("one-1", "one-2", "one-3", "two-6"), obsolete.map { it.snapId }.toSet())
    }

    @Test fun oversizedMinimumHistoryNeverLosesLatestComparisonPair() {
        val versions = (1..3).map { snap("one", it) } + (4..6).map { snap("two", it) }
        val obsolete = SnapshotRetention(byteBudget = 1).obsoleteVersions(versions)
        assertEquals(setOf("one-1", "two-4"), obsolete.map { it.snapId }.toSet())
    }

    private fun snap(page: String, version: Int) = Snap(
        snapId = "$page-$version",
        siteId = page,
        timestamp = version.toLong(),
        contentType = "text/html",
        contentCharset = "UTF-8",
        contentSize = 100,
        contentHash = "same-content",
    )
}
