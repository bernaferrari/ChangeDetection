/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.detection.selectableTextDocument
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TextRegionPickerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ignoringContentKeepsTheWatchedAreaVisibleAndConfirmsBothRules() = runBlocking {
        val html = "<main id='page'><p id='stock'>In stock</p><p id='clock'>Countdown</p></main>"
        val document = selectableTextDocument(html)
        val pageIndex = document.regions.indexOfFirst { it.selector == "#page" }
        val clockIndex = document.regions.indexOfFirst { it.selector == "#clock" }
        var highlights: com.bernaferrari.changedetection.detection.RegionHighlightUpdate? = null
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(440.dp, 800.dp), pageRenderer = { current, selected, choose, _, layout ->
                    highlights = com.bernaferrari.changedetection.detection.regionHighlightUpdate(current, selected)
                    androidx.compose.foundation.layout.Column(layout) {
                        androidx.compose.material3.Button(onClick = { choose("#page") }) { androidx.compose.material3.Text("Choose page") }
                        androidx.compose.material3.Button(onClick = { choose("#clock") }) { androidx.compose.material3.Text("Choose countdown") }
                    }
                })
            }
        }
        compose.onNodeWithText("Choose page").performClick()
        compose.runOnIdle { assertEquals(pageIndex, highlights?.selected) }
        compose.onNodeWithText("Ignore").performClick()
        compose.runOnIdle { assertTrue("Watched area disappeared when entering Ignore", pageIndex in requireNotNull(highlights).watched) }
        compose.onNodeWithText("Choose countdown").performClick()
        compose.runOnIdle {
            assertTrue(pageIndex in requireNotNull(highlights).watched)
            assertTrue(clockIndex in requireNotNull(highlights).ignored)
            assertEquals(clockIndex, highlights?.selected)
        }
        compose.onNodeWithText("Watch").performClick()
        compose.runOnIdle {
            assertEquals(pageIndex, highlights?.selected)
            assertTrue(clockIndex in requireNotNull(highlights).ignored)
        }
        compose.waitUntil(5_000) { !compose.onNodeWithText("Use selection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle {
            assertEquals(TextRegion("#page", "#clock"), result)
            val extracted = requireNotNull(result).extractHtml(html, true)
            assertTrue("In stock" in extracted)
            assertFalse("Countdown" in extracted)
        }
    }

    @Test fun modeAndDraftEditsDoNotRecreateThePage() = runBlocking {
        val document = selectableTextDocument("<p id='stock'>In stock</p>")
        var mounts = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, {}, Modifier.size(440.dp, 800.dp), pageRenderer = { _, _, choose, _, layout ->
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        mounts++
                        onDispose {}
                    }
                    androidx.compose.material3.Button(onClick = { choose("#stock") }, modifier = layout) { androidx.compose.material3.Text("Choose stock") }
                })
            }
        }
        compose.runOnIdle { assertEquals(1, mounts) }
        compose.onNodeWithText("Choose stock").performClick()
        compose.onNodeWithText("Add area").performClick()
        compose.runOnIdle { assertEquals(1, mounts) }
        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Watch").performClick()
        compose.runOnIdle { assertEquals(1, mounts) }
        compose.onNodeWithText("Ignore").performClick()
        compose.onNodeWithText("Choose stock").performClick()
        compose.runOnIdle { assertEquals(1, mounts) }
        compose.onNodeWithText("Ignore").performClick()
        compose.runOnIdle { assertEquals(1, mounts) }
    }

    @Test fun reopeningRestoresMultipleWatchedAreasAndConfirmsTheSameRule() = runBlocking {
        val document = selectableTextDocument("<p id='price'>Price 100</p><p id='stock'>Available</p><p id='clock'>Countdown</p>").copy(selectionHtml = null)
        val initial = TextRegion("#price,#stock", "#clock")
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(440.dp, 800.dp), initialRegion = initial)
            }
        }
        compose.onNodeWithText("Price 100", substring = false).performScrollTo().assertIsSelected()
        compose.onNodeWithText("Available", substring = false).performScrollTo().assertIsSelected()
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        compose.waitUntil(5_000) { !compose.onNodeWithText("Use selection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle { assertEquals(initial, result) }
    }

    @Test fun ignoredAreaScopeCanExpandAndRecoverWithoutChangingWatchedArea() = runBlocking {
        val document = selectableTextDocument("<main id='all'><p id='stock'>Available</p><section id='noise'><p id='ad'>Ad</p><p id='clock'>Countdown</p></section></main>").copy(selectionHtml = null)
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(440.dp, 800.dp))
            }
        }
        compose.onNodeWithText("Available Ad Countdown").performScrollTo().performClick()
        compose.onNodeWithText("Ignore", substring = false).performClick()
        compose.onNodeWithText("Countdown", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Smaller").assertIsNotEnabled()
        compose.onNodeWithText("Larger").performClick()
        compose.onNodeWithText("Preview", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Restore: Ad Countdown").performScrollTo().assertExists()
        compose.onNodeWithText("Smaller").performClick()
        compose.onNodeWithText("Restore: Countdown").performScrollTo().assertExists()
        compose.onNodeWithText("Undo").performClick()
        compose.waitUntil(5_000) { !compose.onNodeWithText("Use selection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle { assertEquals(TextRegion("#all", "#noise"), result) }
    }

    @Test fun existingExclusionParticipatesInValidationAndCanBeRestored() = runBlocking {
        val document = selectableTextDocument("<p id='stock'>Available</p>").copy(selectionHtml = null)
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(440.dp, 800.dp), initialRegion = TextRegion("#stock", "#stock"))
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nothing remains to watch. Restore an ignored area or choose another region.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Use selection").assertIsNotEnabled()
        compose.onNodeWithText("Preview", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Restore: Available").performScrollTo().performClick()
        compose.waitUntil(5_000) { !compose.onNodeWithText("Use selection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle { assertEquals(TextRegion("#stock"), result) }
    }

    @Test fun ignoringAllContentBlocksConfirmationAndUndoRecovers() = runBlocking {
        val html = "<main id='all'><p id='stock'>Available</p><p id='clock'>10 seconds</p></main>"
        val document = selectableTextDocument(html).copy(selectionHtml = null)
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(440.dp, 800.dp))
            }
        }
        compose.onNodeWithText("Available 10 seconds").performScrollTo().performClick()
        compose.onNodeWithText("Ignore", substring = false).performClick()
        compose.onNodeWithText("10 seconds", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Available", substring = false).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Nothing remains to watch. Restore an ignored area or choose another region.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Use selection").assertIsNotEnabled()
        compose.onNodeWithText("Undo").performClick()
        compose.waitUntil(5_000) { !compose.onNodeWithText("Use selection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithText("Preview", substring = false).performScrollTo().performClick()
        compose.onNodeWithTag("region-result-text").performScrollTo().assertTextEquals("Available")
        val image = File("../build/desktop-review/region-preview-light.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("region-picker").captureToImage().toAwtImage(), "png", image)
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle {
            assertEquals(TextRegion("#all", "#clock"), result)
            assertFalse(requireNotNull(result).extractHtml(html, true).contains("seconds"))
        }
    }

    @Test fun additionalAreaIsRetainedAndConfirmedWithTheActiveChoice() = runBlocking {
        val html = "<p id='price'>Price 100</p><p id='noise'>Unrelated</p><p id='stock'>Available</p>"
        val document = selectableTextDocument(html).copy(selectionHtml = null)
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(400.dp, 700.dp))
            }
        }
        compose.onNodeWithText("Price 100").performScrollTo().performClick()
        compose.onNodeWithText("Add area").performClick()
        compose.onNodeWithText("Available").performScrollTo().performClick()
        compose.onNodeWithText("Price 100", substring = false).assertIsSelected()
        compose.onNodeWithText("Available").assertIsSelected()
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle {
            assertEquals(TextRegion("#price, #stock"), result)
            assertFalse(requireNotNull(result).extractHtml(html, true).contains("Unrelated"))
        }
    }

    @Test fun scopeAndUndoUpdateThePreviewAndConfirmedRule() = runBlocking {
        val document = selectableTextDocument("<main id='main'><p id='first'>First</p><p id='stock'>Stock</p></main>")
        var result: TextRegion? = null
        var observedViewport = -1
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionPicker(document, {}, { result = it }, Modifier.size(400.dp, 700.dp), pageRenderer = { _, selected, choose, position, layout ->
                    observedViewport = position.viewport.y
                    androidx.compose.material3.Button(onClick = {
                        position.viewport = com.bernaferrari.changedetection.detection.RegionViewport(0, 900)
                        choose("#stock")
                    }, modifier = layout) {
                        androidx.compose.material3.Text("Preview: ${selected ?: "none"}")
                    }
                })
            }
        }
        compose.onNodeWithText("Larger").assertIsNotEnabled()
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        compose.onNodeWithText("Preview: none").performClick()
        compose.onNodeWithText("Smaller").assertIsNotEnabled()
        compose.onNodeWithText("Larger").performClick()
        compose.onNodeWithText("Preview: #main").assertIsDisplayed()
        compose.runOnIdle { assertEquals(900, observedViewport) }
        compose.onNodeWithText("Text list").performScrollTo().performClick()
        compose.onNodeWithText("Page", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(900, observedViewport) }
        compose.onNodeWithText("Larger").assertIsNotEnabled()
        compose.onNodeWithText("Smaller").performClick()
        compose.onNodeWithText("Preview: #stock").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Preview: #main").assertIsDisplayed()
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle { assertEquals(TextRegion("#main"), result) }
    }

    @Test fun renderedChoiceSurvivesViewSwitchAndRequiresConfirmationAtLargeText() = runBlocking {
        val document = selectableTextDocument("<p id='stock'>In stock</p>")
        var result: TextRegion? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    TextRegionPicker(document, {}, { result = it }, Modifier.size(280.dp, 480.dp), pageRenderer = { doc, selected, choose, _, layout ->
                        androidx.compose.material3.Button(onClick = { choose(doc.regions.first().selector) }, modifier = layout) {
                            androidx.compose.material3.Text(if (selected == null) "Rendered fixture" else "Selected fixture")
                        }
                    })
                }
            }
        }
        compose.onNodeWithText("Use selection").assertIsNotEnabled()
        val previewBounds = compose.onNodeWithTag("region-page-preview").getUnclippedBoundsInRoot()
        assertTrue(previewBounds.bottom - previewBounds.top >= 80.dp)
        compose.onNodeWithText("Rendered fixture").performClick()
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithText("Text list").performScrollTo().performClick()
        compose.onNodeWithTag("region-choices").performScrollToKey("#stock")
        compose.onNodeWithText("In stock").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Page", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Selected fixture").assertIsDisplayed()
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle { assertEquals(TextRegion("#stock"), result) }
    }

    @Test fun previewSelectionRequiresConfirmationAndReturnsACaptureRule() = runBlocking {
        val html = "<main><p id='stock'>Available: 3</p><p>Delivery tomorrow</p></main>"
        val document = selectableTextDocument(html).copy(selectionHtml = null)
        var selected: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                CapturePreviewPanel(CapturePreviewState.Ready(CapturePreview.Html("Available: 3 Delivery tomorrow", html.length, document)), true, {}, {}, { selected = it })
            }
        }
        compose.onNodeWithText("Choose text on the page").performClick()
        compose.onNodeWithText("Use selection").assertIsNotEnabled()
        compose.onNodeWithText("Available: 3").performClick()
        compose.onNodeWithText("Available: 3").assertIsSelected()
        val image = File("../build/desktop-review/region-picker-light.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("region-picker").captureToImage().toAwtImage(), "png", image)
        compose.runOnIdle { assertNull(selected) }
        compose.onNodeWithText("Use selection").performClick()
        compose.runOnIdle {
            assertTrue(requireNotNull(selected).extractHtml(html, true).contains("Available: 3"))
            assertFalse(requireNotNull(selected).extractHtml(html, true).contains("Delivery"))
        }
        compose.onNodeWithText("Use selection").assertDoesNotExist()
    }

    @Test fun narrowLargeTextKeepsSelectionAndConfirmationReachable() = runBlocking {
        val document = selectableTextDocument("<p id='stock'>In stock</p><p>Other content</p>").copy(selectionHtml = null)
        var result: TextRegion? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    TextRegionPicker(document, {}, { result = it }, Modifier.size(280.dp, 480.dp))
                }
            }
        }
        compose.onNodeWithTag("region-choices").performScrollToKey(document.regions.first().selector)
        compose.onNodeWithText("In stock").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("In stock").assertIsSelected()
        compose.onNodeWithText("Tools").assertIsDisplayed().performClick()
        listOf("Smaller", "Larger", "Add area", "Undo").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        val menuImage = File("../build/desktop-review/region-tools-large-dark.png")
        menuImage.parentFile.mkdirs()
        ImageIO.write(compose.onNode(isPopup()).captureToImage().toAwtImage(), "png", menuImage)
        compose.onNodeWithText("Undo").assertIsDisplayed().performClick()
        compose.onNodeWithText("Use selection").assertIsNotEnabled()
        compose.onNodeWithText("In stock").performScrollTo().performClick()
        compose.onNodeWithText("Use selection").assertIsDisplayed().performClick()
        compose.runOnIdle { assertNotNull(result) }
        val image = File("../build/desktop-review/region-picker-large-dark.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("region-picker").captureToImage().toAwtImage(), "png", image)
        Unit
    }

    @Test fun savedSelectionStaysReadableWithoutExposingCssAndCanReset() {
        var result: TextRegion? = null
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TextRegionFields(TextRegion("#stock"), false, true) { result = it }
            }
        }
        compose.onNodeWithText("Selected text").assertIsDisplayed()
        compose.onNodeWithText("#stock").assertDoesNotExist()
        compose.onNodeWithText("Whole page").performClick()
        compose.runOnIdle { assertEquals(TextRegion(), result) }
    }

    @Test fun cancelDoesNotApplyDraftSelection() = runBlocking {
        val document = selectableTextDocument("<p id='stock'>In stock</p>").copy(selectionHtml = null)
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                TextRegionPickerAction(document) { error("Cancelled selection was applied") }
            }
        }
        compose.onNodeWithText("Choose text on the page").performClick()
        compose.onNodeWithText("In stock").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Use selection").assertDoesNotExist()
    }
}
