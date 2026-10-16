package com.bernaferrari.changedetection.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class HomeScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test fun titleExpandsWithMouseWheel() = verifyTitleExpansion(mouseWheel = true)

    @Test fun titleExpandsWithTouchSwipes() = verifyTitleExpansion(mouseWheel = false)

    @Test fun pullShowsExactlyWhenReleaseWillTriggerAndCancelsWhenRetracted() {
        var refreshes = 0
        var refreshing by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                PullToCheckBox(
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    refreshing = refreshing, enabled = true,
                    onRefresh = { refreshes++; refreshing = true },
                    modifier = Modifier.size(400.dp, 700.dp),
                ) {
                    LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                        items(20) { Text("Page $it", Modifier.height(80.dp)) }
                    }
                }
            }
        }
        val list = compose.onNodeWithTag("list")
        compose.onNodeWithText("Release to check pages").assertDoesNotExist()
        list.performTouchInput {
            down(center.copy(y = 100f))
            moveBy(androidx.compose.ui.geometry.Offset(0f, 100f), delayMillis = 500)
        }
        compose.onNodeWithText("Pull to check pages").assertExists()
        compose.runOnIdle { assertEquals(0, refreshes) }
        list.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, 160f), delayMillis = 500) }
        compose.onNodeWithText("Release to check pages").assertIsDisplayed()
        compose.runOnIdle { assertEquals("Crossing the threshold must wait for release", 0, refreshes) }
        list.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, -160f), delayMillis = 500) }
        compose.onNodeWithText("Pull to check pages").assertExists()
        list.performTouchInput { advanceEventTime(200); up() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("Retracting below the threshold cancels the check", 0, refreshes) }
        list.performTouchInput {
            down(center.copy(y = 100f))
            moveBy(androidx.compose.ui.geometry.Offset(0f, 260f), delayMillis = 500)
        }
        compose.onNodeWithText("Release to check pages").assertIsDisplayed()
        list.performTouchInput { advanceEventTime(200); up() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.onNodeWithText("Checking pages…").assertIsDisplayed()
    }

    private fun verifyTitleExpansion(mouseWheel: Boolean) {
        lateinit var behavior: TopAppBarScrollBehavior
        var refreshes = 0
        compose.setContent {
            MaterialTheme {
                behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(
                    modifier = Modifier.size(400.dp, 700.dp),
                    topBar = { LargeFlexibleTopAppBar(title = { Text("Home") }, scrollBehavior = behavior) },
                ) { padding ->
                    PullToCheckBox(scrollBehavior = behavior, refreshing = false, enabled = true, onRefresh = { refreshes++ }, modifier = Modifier.fillMaxSize().padding(padding)) {
                        LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                            items(30) { Text("Page $it", Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
        if (mouseWheel) {
            compose.onNodeWithTag("list").performMouseInput {
                moveTo(center)
                scroll(1500f)
            }
        } else {
            compose.onNodeWithTag("list").performTouchInput { swipeUp() }
        }
        compose.runOnIdle { assertTrue("Title should collapse", behavior.state.collapsedFraction > 0.9f) }
        repeat(if (mouseWheel) 20 else 5) {
            if (mouseWheel) {
                compose.onNodeWithTag("list").performMouseInput {
                    moveTo(center)
                    scroll(-500f)
                }
            } else {
                compose.onNodeWithTag("list").performTouchInput { swipeDown() }
            }
        }
        compose.runOnIdle {
            assertEquals("Returning to the top must expand the title", 0f, behavior.state.collapsedFraction, 0.01f)
            if (!mouseWheel) assertTrue("Pull-to-refresh should still work after expanding the title", refreshes > 0)
        }
    }
}
