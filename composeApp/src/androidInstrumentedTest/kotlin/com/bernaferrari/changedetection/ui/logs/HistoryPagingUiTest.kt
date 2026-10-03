/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryPagingUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun largeHistorySummaryWrapsWithReadableDynamicLightColors() = assertSummaryReadable(dark = false)

    @Test fun largeHistorySummaryWrapsWithReadableDynamicDarkColors() = assertSummaryReadable(dark = true)

    private fun assertSummaryReadable(dark: Boolean) {
        var contrast = 0f
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = true) {
                    val foreground = MaterialTheme.colorScheme.onSecondaryContainer.luminance()
                    val background = MaterialTheme.colorScheme.secondaryContainer.luminance()
                    contrast = (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f)
                    Box(Modifier.width(280.dp)) { LogsPulse(999_999, 999) }
                }
            }
        }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/history-summary-$dark.png"),
        ).use { it.readBytes() }
        listOf("999999 saved versions", "Across 999 pages").forEach { text ->
            compose.onNodeWithText(text).assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getResults(results))
                assertTrue(results.isNotEmpty())
                assertTrue("Summary $text must wrap without clipping: ${results.map { "size=${it.size}, lines=${it.lineCount}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}" }}", results.none { it.hasVisualOverflow })
            }
        }
        compose.runOnIdle { assertTrue("Dynamic summary text contrast was $contrast", contrast >= 4.5f) }
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
