package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActivityFeedUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialLoadingIsCenteredInTheAvailableViewport() {
        val state = LoadStates(LoadState.Loading, LoadState.NotLoading(true), LoadState.NotLoading(true))
        val history = flowOf(PagingData.from<LogUiModel>(emptyList(), sourceLoadStates = state))
        compose.setContent {
            val items = history.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.width(320.dp).height(600.dp).testTag("viewport")) {
                    ActivityFeed(items, emptyMap(), "", { _, _ -> }, {}, {})
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Loading…").fetchSemanticsNodes().isNotEmpty() }
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val loading = compose.onNodeWithContentDescription("Loading…").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.x, loading.center.x, 1f)
        assertEquals(viewport.center.y, loading.center.y, 1f)
    }

    @Test fun pagesThatHaveNeverSavedAVersionStillExposeEveryCurrentFailure() {
        var opened: String? = null
        val sites = (1..6).associate { number ->
            val id = "failure-$number"
            id to Site("Failed page $number", "https://example.com/$number", 1L, "", id, 0 to 0)
                .copy(isSuccessful = false, lastFailureCode = "CONNECTION")
        }
        val history = flowOf(PagingData.empty<LogUiModel>())
        compose.setContent {
            val items = history.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.width(320.dp).height(600.dp)) {
                    ActivityFeed(items, sites, "", { _, _ -> }, { opened = it }, {})
                }
            }
        }
        compose.onNodeWithText("Needs attention").assertIsDisplayed()
        compose.onNodeWithTag("activity-list").performScrollToKey("failure-failure-6")
        compose.onNodeWithText("Failed page 6").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("failure-6", opened) }
    }

    @Test fun failedHistoryLoadOffersRetryInsteadOfAnEmptyHistoryClaim() {
        val state = LoadStates(LoadState.Error(IOException("Unavailable")), LoadState.NotLoading(true), LoadState.NotLoading(true))
        val history = flowOf(PagingData.from<LogUiModel>(emptyList(), sourceLoadStates = state))
        compose.setContent {
            val items = history.collectAsLazyPagingItems()
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.width(320.dp).height(600.dp)) {
                    ActivityFeed(items, emptyMap(), "", { _, _ -> }, {}, {})
                }
            }
        }
        compose.onNodeWithText("Error loading data").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("No changes yet").assertDoesNotExist()
    }
}
