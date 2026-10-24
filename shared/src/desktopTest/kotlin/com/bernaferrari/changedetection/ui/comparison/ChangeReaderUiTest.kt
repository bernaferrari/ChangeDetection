package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChangeReaderUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = ReaderState()

    private fun show(before: String, after: String, fontScale: Float = 1f, short: Boolean = false, html: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Column(Modifier.size(320.dp, if (short) 180.dp else 600.dp)) {
                        ChangeReader(before, after, html, html, state, {}, Modifier.weight(1f))
                        if (!short) {
                            Row {
                                TextButton(onClick = { scope.launch { state.go(-1) } }) { Text("Previous") }
                                TextButton(onClick = { scope.launch { state.go(1) } }) { Text("Next") }
                                TextButton(onClick = { scope.launch { state.go(0) } }) { Text("Jump") }
                                Text("${state.current + 1}/${state.changeCount}")
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun shortChangesRemainNavigableWhenTheLastItemCannotScrollToTheTop() {
        show("A old\nSame one\nB old\nSame two\nC old", "A new\nSame one\nB new\nSame two\nC new")
        compose.waitUntil(5_000) { state.changeCount == 3 }
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("2/3").assertIsDisplayed()
    }

    @Test fun expandedContextHasAnExplicitCollapseControlAndRemainsVirtualized() {
        val context = (1..500).joinToString("\n") { "Unchanged context $it" }
        show("$context\nPrice 10", "$context\nPrice 12")
        compose.waitUntil(5_000) { state.changeCount == 1 }
        val fold = compose.onNodeWithText("500 unchanged lines")
        val density = compose.density.density
        assertTrue("Fold target is at least 48dp high", fold.fetchSemanticsNode().boundsInRoot.height >= 48f * density)
        fold.performClick()
        compose.onNodeWithText("Hide unchanged lines").assertIsDisplayed()
        compose.onNodeWithText("Unchanged context 500").assertDoesNotExist()
        compose.runOnIdle { assertTrue("Expanded context shifts the change anchor", state.changeIndices.single() >= 500) }
        compose.onNodeWithText("Jump").performClick()
        compose.onNodeWithContentDescription("After: Price 12", substring = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, state.current) }
    }

    @Test fun revealingLongReplacementsKeepsEveryLineReachableWithoutComposingTheWholeChange() {
        val before = (1..100).joinToString("\n") { "Old line $it" }
        val after = (1..100).joinToString("\n") { "New line $it" }
        show(before, after)
        // A change this big opens as a digest first; the full comparison is one tap away.
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("comparison-digest").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("comparison-digest").performScrollToNode(hasText("Show full comparison"))
        compose.onNodeWithText("Show full comparison").performClick()
        compose.waitUntil(5_000) { state.changeCount == 1 }
        compose.onAllNodesWithText("Show 88 more lines").onFirst().performScrollTo().performClick()
        compose.onNodeWithContentDescription("After: New line 100").assertDoesNotExist()
        compose.onNodeWithTag("comparison-lines").performScrollToNode(hasContentDescription("After: New line 100"))
        compose.onNodeWithContentDescription("After: New line 100").assertIsDisplayed()
        compose.onNodeWithTag("comparison-lines").performScrollToNode(hasText("Show fewer lines"))
        compose.onNodeWithText("Show fewer lines").performClick()
        compose.onNodeWithContentDescription("After: New line 100").assertDoesNotExist()
    }

    @Test fun busyPagesOpenAsNewRemovedAndNumberDigest() {
        val words = listOf("apple", "bridge", "cloud", "desert", "engine", "forest", "garden", "harbor", "island", "jungle", "kettle", "lantern")
        val before = words.flatMap { listOf("Story about the $it", "${it.length * 7} points · 3 hours ago") }.joinToString("\n")
        val after = (listOf("A brand new headline appears") + words.dropLast(1)).flatMap { listOf(if (it.contains(' ')) it else "Story about the $it", "${it.length * 7 + 4} points · 5 hours ago") }.joinToString("\n")
        show(before, after)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("New").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Removed").assertExists()
        compose.onNodeWithText("Numbers changed in", substring = true).assertExists()
    }

    @Test fun inlineChangesDescribeBeforeAndAfterForScreenReaders() {
        show("Price 10", "Price 12")
        compose.waitUntil(5_000) { state.changeCount == 1 }
        compose.onNodeWithContentDescription("Before: Price 10\nAfter: Price 12").assertIsDisplayed()
    }

    @Test fun hiddenChangeExplanationAndSourceActionRemainReachableAtLargeTextSize() {
        show("<p>Same words</p><script>before()</script>", "<p>Same words</p><script>after()</script>", fontScale = 2f, short = true, html = true)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Show source changes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Show source changes").performScrollTo().assertIsDisplayed()
    }
}
