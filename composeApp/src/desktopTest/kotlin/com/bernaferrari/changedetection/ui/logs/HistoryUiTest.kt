/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.graphics.toAwtImage
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

class HistoryUiTest {
    @get:Rule val compose = createComposeRule()
    private val sites = (1..2).associate { index -> "page-$index" to Site("Page $index", "https://example.test/$index", 0L, "", "page-$index", 0xFF6352CA.toInt() to 0xFFEEEAFB.toInt()) }
    private fun item(index: Int) = LogUiModel.Item(Snap("snap-$index", "page-$index", 1_700_000_000_000L, "text/html", "UTF-8", 100))

    @Test fun calendarHeadingsHandleYearBoundaryAndKeepOlderYearsUnambiguous() {
        compose.setContent {
            val today = kotlinx.datetime.LocalDate(2026, 1, 1)
            androidx.compose.foundation.layout.Column {
                listOf("2026-01-01", "2025-12-31", "2025-12-30", "2026-01-02", "legacy date").forEach {
                    androidx.compose.material3.Text(historyDateLabel(it, today))
                }
            }
        }
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("Yesterday").assertIsDisplayed()
        compose.onNodeWithText("December 30, 2025").assertIsDisplayed()
        compose.onNodeWithText("January 2").assertIsDisplayed()
        compose.onNodeWithText("legacy date").assertIsDisplayed()
    }

    @Test fun savedContentDistinguishesVersionsAndOpeningKeepsExactIdentity() {
        val flow = flowOf(PagingData.from<LogUiModel>(listOf(item(1), item(2))))
        var opened: String? = null
        val requested = mutableListOf<String>()
        compose.setContent {
            val entries = flow.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                androidx.compose.material3.Surface(Modifier.size(430.dp, 850.dp)) {
                    HistoryList(entries, sites, 2, 2, { _, snap -> opened = snap }, { _, _ -> }, {}, loadPreview = { snap ->
                        requested.add(snap.snapId)
                        HistoryPreviewContent.Passage(
                            "Workshop starts at 09:00",
                            if (snap.snapId == "snap-1") "Workshop starts at 10:00" else "Workshop starts at 11:00",
                        )
                    })
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Workshop starts at 09:00", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Workshop starts at 10:00", substring = true).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("snap-1", opened)
            assertEquals(setOf("snap-1", "snap-2"), requested.toSet())
        }
        val image = File("../build/desktop-review/history-content-light.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", image)
    }

    @Test fun longAddressKeepsVersionTimeAndMenuReachableAtLargeText() {
        val snap = item(1).snap
        val time = com.bernaferrari.changedetection.util.formatTime(snap.timestamp, includeSeconds = true)
        val site = sites.getValue("page-1").copy(title = "Workshop schedule", url = "https://example.test/" + "long-address/".repeat(20))
        var opened = false
        var deleted = false
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    androidx.compose.material3.Surface(Modifier.width(320.dp)) {
                        LogItem(snap, site, { opened = true }, { deleted = true }, loadPreview = {
                            HistoryPreviewContent.Passage("Starts at 09:00", "Starts at 10:00")
                        })
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Starts at 10:00", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Starts at 10:00", substring = true).assertIsDisplayed()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(time, substring = true, useUnmergedTree = true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().getLineEnd(0, visibleEnd = true) >= time.length)
        compose.onNodeWithText("Workshop schedule").performClick()
        compose.runOnIdle { assertTrue(opened) }
        val image = File("../build/desktop-review/history-row-large-dark.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", image)
        compose.onNodeWithContentDescription("More options").assertIsDisplayed().performClick()
        compose.onNodeWithText("Delete").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }

    @Test fun failedRefreshKeepsEntriesOpenableAndRetryLoadsFreshHistory() {
        val attempts = AtomicInteger()
        val pager = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            object : PagingSource<Int, LogUiModel>() {
                override fun getRefreshKey(state: PagingState<Int, LogUiModel>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogUiModel> = when (attempts.incrementAndGet()) {
                    1 -> LoadResult.Page(listOf(item(1)), null, null)
                    2 -> LoadResult.Error(IOException("Internal database path and SQL details"))
                    else -> LoadResult.Page(listOf(item(2)), null, null)
                }
            }
        }
        var refresh: () -> Unit = {}
        var opened: String? = null
        compose.setContent {
            val entries = pager.flow.collectAsLazyPagingItems()
            refresh = entries::refresh
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                androidx.compose.material3.Surface(Modifier.size(400.dp, 700.dp)) {
                    HistoryList(entries, sites, 2, 2, { _, snap -> opened = snap }, { _, _ -> }, {})
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Page 1").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { refresh() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Couldn’t refresh history. Showing previously loaded versions.").assertIsDisplayed()
        compose.onNodeWithText("Internal database path", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Page 1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("snap-1", opened) }
        val image = File("../build/desktop-review/history-refresh-recovery.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", image)
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Page 2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithText("Page 2").performClick()
        compose.runOnIdle {
            assertEquals("snap-2", opened)
            assertEquals(3, attempts.get())
        }
    }

    @Test fun filteredHistoryDoesNotPresentWholeArchiveTotalsAsSearchResults() {
        val flow = flowOf(PagingData.from<LogUiModel>(listOf(item(1))))
        compose.setContent {
            val entries = flow.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                HistoryList(entries, sites, 200, 50, { _, _ -> }, { _, _ -> }, {}, searchQuery = "Page 1")
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Page 1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Matching saved versions").assertIsDisplayed()
        compose.onNodeWithText("200 saved versions").assertDoesNotExist()
        compose.onNodeWithText("Across 50 pages").assertDoesNotExist()
    }

    @Test fun appendFailureKeepsLoadedEntriesAndRetryLoadsTheNextPage() {
        val appends = AtomicInteger()
        val refreshes = AtomicInteger()
        val sites = (1..6).associate { index ->
            "page-$index" to Site("Page $index", "https://example.test/$index", 0L, "", "page-$index", 0 to 0)
        }
        fun items(indices: IntRange) = indices.map { index ->
            LogUiModel.Item(Snap("snap-$index", "page-$index", 1_700_000_000_000L - index * 60_000, "text/html", "UTF-8", 100))
        }
        val pager = Pager(PagingConfig(pageSize = 3, initialLoadSize = 3, prefetchDistance = 1, enablePlaceholders = false)) {
            object : PagingSource<Int, LogUiModel>() {
                override fun getRefreshKey(state: PagingState<Int, LogUiModel>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogUiModel> {
                    if (params.key == null) {
                        refreshes.incrementAndGet()
                        return LoadResult.Page(items(1..3), prevKey = null, nextKey = 1)
                    }
                    if (appends.incrementAndGet() == 1) return LoadResult.Error(IOException("Transient read failure"))
                    return LoadResult.Page(items(4..6), prevKey = null, nextKey = null)
                }
            }
        }
        var opened: String? = null
        compose.setContent {
            val entries = pager.flow.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                HistoryList(entries, sites, 6, 6, { _, snap -> opened = snap }, { _, _ -> }, {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Couldn’t load more history", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Page 1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("snap-1", opened) }
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Page 4").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithTag("history-list").performScrollToKey("snap-6")
        compose.onNodeWithText("Page 6").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("snap-6", opened)
            assertEquals(1, refreshes.get())
            assertEquals(2, appends.get())
        }
        compose.onNodeWithTag("history-list").performScrollToKey("snap-1")
        compose.onNodeWithText("Page 1").assertIsDisplayed()
    }
}
