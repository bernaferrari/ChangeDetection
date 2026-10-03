/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ComparisonWorkspaceUiTest {
    @get:Rule val compose = createComposeRule()
    private val snaps = listOf(
        Snap("new", "page", 1_700_003_600_000L, "text/plain", "UTF-8", 30),
        Snap("old", "page", 1_700_000_000_000L, "text/plain", "UTF-8", 30),
    )

    @Test fun missingBaselineKeepsRetryReachableInAShortLargeTextReader() {
        var retries = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.size(280.dp, 180.dp)) {
                        SingleSnapshotState(snaps.last(), null, null, false, true, { retries++ })
                    }
                }
            }
        }
        compose.onNodeWithText("Retry").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, retries) }
    }

    @Test fun navigationReachesSeparateLongPassagesAndPickerReturnsToTheFirst() = checkPassageNavigation(1f)

    @Test fun largeTextKeepsPassageNavigationReachable() = checkPassageNavigation(2f)

    private fun checkPassageNavigation(fontScale: Float) {
        val longBefore = (1..45).joinToString("\n") { "Old program line $it" }
        val longAfter = (1..45).joinToString("\n") { "New program line $it" }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.size(390.dp, 650.dp)) {
                        SimpleDiffView("Morning\n$longBefore\nAfternoon\n20 places", "Morning\n$longAfter\nAfternoon\n12 places")
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Passage 1 of 2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Previous passage").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next passage").performClick()
        compose.onNodeWithText("12 places").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next passage").assertIsNotEnabled()
        val capture = File("../build/desktop-review/passage-navigation-${if (fontScale == 1f) "normal" else "large"}.png")
        capture.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", capture)
        compose.onNodeWithContentDescription("Passage 2 of 2").performClick()
        compose.onNode(hasText("Morning") and hasClickAction()).performClick()
        compose.onNodeWithText("New program line 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous passage").assertIsNotEnabled()
    }

    @Test fun shortPassagesStillAdvanceWhenAllContentAlreadyFits() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.size(500.dp, 1000.dp)) {
                    SimpleDiffView("Morning\n09:00\nAfternoon\n20 places", "Morning\n10:00\nAfternoon\n12 places")
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Passage 1 of 2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Next passage").performClick()
        compose.onNodeWithContentDescription("Passage 2 of 2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous passage").performClick()
        compose.onNodeWithContentDescription("Passage 1 of 2").assertIsDisplayed()
    }

    @Test fun deletionOnlyPassageShowsRemovedTextImmediately() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.size(390.dp, 600.dp)) {
                    SimpleDiffView("Applications close Friday", "")
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Applications close Friday").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Applications close Friday").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hide previous text").performClick()
        compose.onNodeWithText("Applications close Friday").assertDoesNotExist()
    }

    @Test fun readerShowsPairAndKeepsHistoryReachable() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.width(390.dp).height(780.dp)) {
                    DiffFirstContent(
                        site = Site("Program", "https://example.test", 0L, "", "page", 0 to 0),
                        snaps = snaps, selectedPairIndex = 0,
                        content1 = "Conference program\nOpening session 10:30", content2 = "Conference program\nOpening session 10:00",
                        bytes1 = null, bytes2 = null, imageDiff = null, isLoadingDiff = false,
                        onSelectPair = {}, onRetry = {}, onShare = {},
                    )
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Opening session 10:30", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("After")[0].assertIsDisplayed()
        compose.onNodeWithText("Opening session 10:00", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Show previous text").performClick()
        compose.onNodeWithText("Opening session 10:00", substring = true).assertIsDisplayed()
        val file = File("../build/desktop-review/comparison-workspace-dark.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
        compose.onNodeWithContentDescription("Version history").performClick()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test fun shortLargeTextWorkspacePreservesReaderAndScrollableChronology() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.size(280.dp, 360.dp).testTag("short-workspace")) {
                        DiffFirstContent(
                            site = Site("Program", "https://example.test", 0L, "", "page", 0 to 0),
                            snaps = snaps, selectedPairIndex = 0,
                            content1 = "Opening 10:30", content2 = "Opening 10:00",
                            bytes1 = null, bytes2 = null, imageDiff = null, isLoadingDiff = false,
                            onSelectPair = {}, onRetry = {}, onShare = {},
                        )
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("comparison-lines").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("comparison-lines").performScrollToKey("after-0-0")
        val reader = compose.onNodeWithTag("comparison-reader").fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithTag("comparison-controls").fetchSemanticsNode().boundsInRoot
        assertTrue("Reader must retain more height than controls", reader.height > controls.height)
        compose.onAllNodesWithText("Opening 10:30", substring = true)[0].performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("After")[0].performScrollTo().assertIsDisplayed()
        val file = File("../build/desktop-review/comparison-short-large-dark.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("short-workspace").captureToImage().toAwtImage(), "png", file)
        compose.onNodeWithContentDescription("Version history").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test fun pairLabelsRemainReadableAtDoubleTextSize() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.width(280.dp)) { LatestChangeHeader(snaps[0], snaps[1], 0) }
                }
            }
        }
        compose.onNodeWithText("Before").assertIsDisplayed()
        compose.onAllNodesWithText("After")[0].assertIsDisplayed()
    }
}
