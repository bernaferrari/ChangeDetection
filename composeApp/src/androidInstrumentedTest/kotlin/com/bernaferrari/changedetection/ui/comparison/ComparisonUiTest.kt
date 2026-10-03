/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToKey
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Covers the compact comparison toolbar used by the image-diff experience. */
@RunWith(AndroidJUnit4::class)
class ComparisonUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun longVersionHistoryOpensAtSelectionAndRemainsReachableAtDoubleTextInRtl() {
        val snaps = (0..1_000).map { Snap("version-$it", "page", 1_700_000_000_000L - it * 60_000L, "text/html", "UTF-8", 10) }
        var selected = -1
        var closed = false
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
            ) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    VersionSelector(snaps, 900, { selected = it }, { closed = true })
                }
            }
        }
        composeTestRule.onNodeWithText("Previous change 900").assertIsDisplayed()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/history-large-rtl.png"),
        ).use { it.readBytes() }
        assertTrue(composeTestRule.onAllNodesWithText("Previous change", substring = true).fetchSemanticsNodes().size < 20)
        composeTestRule.onNodeWithTag("version-history").performScrollToKey("version-999")
        composeTestRule.onNodeWithText("Previous change 999").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertEquals(999, selected) }
        composeTestRule.onNodeWithTag("version-history").performScrollToKey("version-0")
        composeTestRule.onNodeWithText("Latest change").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertEquals(0, selected) }
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertTrue(closed) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun readableTextIsDefaultAndSourceRemainsAvailableAtLargeType() {
        val old = "<p>Camera sold out</p>"
        val new = "<p>Camera available</p>"
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    DiffFirstContent(
                        site = Site("Camera", "https://example.test", 2L, "", "page", 0 to 0),
                        snaps = listOf(Snap("new", "page", 2L, "text/html", "UTF-8", 20), Snap("old", "page", 1L, "text/html", "UTF-8", 20)),
                        selectedPairIndex = 0,
                        content1 = new,
                        content2 = old,
                        bytes1 = null,
                        bytes2 = null,
                        imageDiff = null,
                        isLoadingDiff = false,
                        onSelectPair = {}, onRetry = {}, onShare = {},
                    )
                }
            }
        }
        composeTestRule.waitUntil(5_000) { composeTestRule.onAllNodesWithText("Camera available").fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithText("Camera available").assertIsDisplayed()
        composeTestRule.onNodeWithText(new).assertDoesNotExist()
        composeTestRule.onNodeWithText("Source").assertIsDisplayed().performClick()
        composeTestRule.waitUntil(5_000) { composeTestRule.onAllNodesWithText(new).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithText(new).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share comparison").assertIsDisplayed()
    }

    @Test fun largeComparisonsComposeOnlyVisibleRowsAndReachTheLastChange() {
        val old = (0 until 10_000).joinToString("\n") { "before $it" }
        val new = (0 until 10_000).joinToString("\n") { "after $it" }
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SimpleDiffView(oldContent = old, newContent = new)
            }
        }
        composeTestRule.waitUntil(10_000) { composeTestRule.onAllNodesWithText("before 0").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(composeTestRule.onAllNodesWithText("before ", substring = true).fetchSemanticsNodes().size < 40)
        composeTestRule.onNodeWithTag("comparison-lines").performScrollToKey("after-9999")
        composeTestRule.onNodeWithText("after 9999").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithText("after ", substring = true).fetchSemanticsNodes().size < 40)
    }

    @Test fun minifiedDocumentsNearTheCaptureLimitKeepTheirLastCharactersReachable() {
        val size = 10 * 1024 * 1024 - 2_048
        val old = "O".repeat(size) + "old_end"
        val new = "N".repeat(size) + "new_end"
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SimpleDiffView(oldContent = old, newContent = new)
            }
        }
        composeTestRule.waitUntil(15_000) { composeTestRule.onAllNodesWithText("+1").fetchSemanticsNodes().isNotEmpty() }
        // Fragmentation must not turn one changed source line into thousands of changes.
        composeTestRule.onNodeWithText("+1").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("comparison-lines").performScrollToKey("after-${size / 2_048}")
        composeTestRule.onNodeWithText("new_end").assertIsDisplayed()
        composeTestRule.onNodeWithTag("comparison-lines").performScrollToKey("before-${size / 2_048}")
        composeTestRule.onNodeWithText("old_end").assertIsDisplayed()
    }

    @Test fun longBaselineTextRemainsScrollableToItsEnd() {
        val size = 1024 * 1024
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SnapshotTextContent("X".repeat(size) + "baseline_end", monospace = true)
            }
        }
        composeTestRule.waitUntil(10_000) { composeTestRule.onAllNodesWithText("X".repeat(2_048)).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag("snapshot-text").performScrollToKey("snapshot-fragment-${size / 2_048}")
        composeTestRule.onNodeWithText("baseline_end").assertIsDisplayed()
    }

    @Test fun renderedComparisonDoesNotHideReorderedWords() {
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                RenderedHtmlComparison(oldContent = "red blue red", newContent = "blue red blue")
            }
        }
        composeTestRule.onNodeWithText("blue red blue").assertIsDisplayed()
        composeTestRule.onNodeWithText("No differences found").assertDoesNotExist()
    }

    @Test
    fun imageModesRemainSelectableWhileShareStaysAvailable() {
        var selectedMode by mutableStateOf(ComparisonMode.ImageDiff)

        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                ComparisonModeSelector(
                    modes = listOf(
                        ComparisonMode.ImageDiff,
                        ComparisonMode.ImageSwipe,
                        ComparisonMode.ImageBefore,
                        ComparisonMode.ImageAfter,
                    ),
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it },
                    canShare = true,
                    onShare = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Compare").performClick()
        composeTestRule.onNodeWithContentDescription("Share comparison").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(ComparisonMode.ImageSwipe, selectedMode)
        }
    }
}
