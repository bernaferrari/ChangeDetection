/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import org.junit.Assert.*
import org.junit.Test

class WatchlistProjectionTest {
    private fun page(id: String, title: String, unread: Boolean = false, paused: Boolean = false) = SiteAndLastSnap(
        Site(title, "https://example.test/$id", 1L, "private note", id, 0 to 0).copy(isRead = !unread, isSyncEnabled = !paused),
        Snap("capture-$id", id, 1L, "text/html", "UTF-8", 10),
        if (unread) 2 else 1,
    )

    @Test fun searchCountsDescribeMatchesWhileHealthStillDescribesAllPages() {
        val pages = listOf(page("b", "Workshop"), page("a", "Conference", unread = true), page("c", "Other", paused = true))
        val result = projectWatchlist(pages, "  WORKSHOP  ", MonitorFilter.Changed, setOf("c"))
        assertEquals(MonitorCounts(3, 1, 1, 1, 0), result.totalCounts)
        assertEquals(MonitorCounts(1, 0, 0, 0, 0), result.matchingCounts)
        assertTrue(result.visibleSites.isEmpty())
        assertEquals(listOf("a"), projectWatchlist(pages, "", MonitorFilter.Changed, setOf("c")).visibleSites.map { it.site.id })
    }

    @Test fun querySupportsNotesAndUrlsWithoutInterpretingWildcards() {
        val pages = listOf(page("a", "100% complete"), page("b", "Normal"))
        assertEquals(2, projectWatchlist(pages, "PRIVATE NOTE", MonitorFilter.All, emptySet()).matchingCounts.all)
        assertEquals(listOf("b"), projectWatchlist(pages, "example.test/b", MonitorFilter.All, emptySet()).visibleSites.map { it.site.id })
        assertEquals(1, projectWatchlist(pages, "%", MonitorFilter.All, emptySet()).matchingCounts.all)
        assertEquals(0, projectWatchlist(pages, "missing", MonitorFilter.All, emptySet()).matchingCounts.all)
    }

    @Test fun readAndFailureChangesUpdateFacetsWithoutReorderingAllRows() {
        val pages = listOf(page("b", "Second"), page("a", "First", unread = true))
        val before = projectWatchlist(pages, "", MonitorFilter.All, setOf("b"))
        val after = projectWatchlist(pages.map { it.copy(site = it.site.copy(isRead = true)) }, "", MonitorFilter.All, emptySet())
        assertEquals(listOf("a", "b"), before.visibleSites.map { it.site.id })
        assertEquals(before.visibleSites.map { it.site.id }, after.visibleSites.map { it.site.id })
        assertEquals(0, after.matchingCounts.changed)
        assertEquals(0, after.matchingCounts.errors)
    }
}
