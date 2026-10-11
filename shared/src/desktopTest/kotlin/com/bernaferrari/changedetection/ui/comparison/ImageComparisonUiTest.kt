/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.PlatformImageDiff

import com.bernaferrari.changedetection.image.SnapshotDimensionMismatch

import com.bernaferrari.changedetection.image.SnapshotImageDiff

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import java.io.File
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ImageComparisonUiTest {
    @get:Rule val compose = createComposeRule()
    private val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()

    private fun show(mode: ComparisonMode, before: ByteArray? = png, after: ByteArray? = png, direction: LayoutDirection = LayoutDirection.Ltr, diff: SnapshotImageDiff? = null, fontScale: Float = 1f, onRetry: (() -> Unit)? = null) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalLayoutDirection provides direction, LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    ImageComparisonContent(before, after, diff, mode, Modifier.size(280.dp, 200.dp), onRetry = onRetry)
                }
            }
        }
    }

    @Test fun aFlatColorChangeStillShowsItsEvidenceWhenNoDisplayRegionsSurvive() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val after = try {
            for (y in 0 until 10) for (x in 0 until 10) image.setRGB(x, y, 0xffffff)
            ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        } finally {
            image.flush()
        }
        val diff = kotlinx.coroutines.runBlocking { PlatformImageDiff.compare(png, after) }
        assertTrue(diff.differentPixels > 0)
        show(ComparisonMode.ImageChanges, after = after, diff = diff)
        compose.onNodeWithTag("pixel-diff-images").assertExists()
        compose.onNodeWithText("Nothing visible changed. The page may have moved slightly.").assertDoesNotExist()
    }

    @Test fun realDesktopDifferenceRendersHighlightsInsteadOfFallingBackToSwipe() {
        fun sample(changed: Boolean): ByteArray {
            val image = BufferedImage(240, 120, BufferedImage.TYPE_INT_RGB)
            return try {
                image.createGraphics().let { graphics ->
                    try {
                        graphics.color = java.awt.Color.WHITE
                        graphics.fillRect(0, 0, 240, 120)
                        graphics.color = java.awt.Color.DARK_GRAY
                        graphics.drawString("Conference program", 16, 24)
                        graphics.drawString("Opening session", 16, 48)
                        if (changed) graphics.fillRect(16, 70, 100, 24)
                    } finally {
                        graphics.dispose()
                    }
                }
                ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
            } finally {
                image.flush()
            }
        }
        val before = sample(false)
        val after = sample(true)
        val diff = kotlinx.coroutines.runBlocking { PlatformImageDiff.compare(before, after, 0.1) }
        assertNotNull(diff.diffImagePng)
        show(ComparisonMode.ImageDiff, before, after, diff = diff)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Changed pixels").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Changed pixels").assertIsDisplayed()
        compose.onNodeWithContentDescription("Before").assertDoesNotExist()
        val capture = compose.onRoot().captureToImage()
        val pixels = capture.toPixelMap()
        assertTrue(
            "The actual difference image must show red changed pixels",
            (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x -> pixels[x, y].let { it.red > 0.9f && it.green < 0.1f && it.blue < 0.1f } }
            },
        )
        val file = File("../build/desktop-review/desktop-pixel-highlights.png")
        file.parentFile.mkdirs()
        ImageIO.write(capture.toAwtImage(), "png", file)
    }

    @Test fun missingImageExplanationRemainsScrollableAtLargeTextSize() {
        show(ComparisonMode.ImageSwipe, before = null, fontScale = 2f)
        compose.onNodeWithText("Failed to load content").performScrollTo().assertIsDisplayed()
    }

    @Test fun identicalImagesExplanationRemainsScrollableBelowSummary() {
        val diff = SnapshotImageDiff(true, 0.0, 0L, 100L, 10, 10, 10, 10, null)
        show(ComparisonMode.ImageDiff, diff = diff, fontScale = 2f)
        compose.onNodeWithTag("pixel-diff-summary").assertDoesNotExist()
        compose.onNodeWithText("No differences detected in this preview.").performScrollTo().assertIsDisplayed()
        val file = File("../build/desktop-review/image-identical-large.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun pixelDiffKeepsImagesVisibleBelowLargeSummaryAndSizeWarning() {
        val diff = SnapshotImageDiff(false, 50.0, 50L, 100L, 10, 10, 20, 10, null, SnapshotDimensionMismatch(10, 10, 20, 10))
        show(ComparisonMode.ImageDiff, diff = diff, fontScale = 2f)
        val imageBounds = compose.onNodeWithTag("pixel-diff-images").fetchSemanticsNode().boundsInRoot
        val summaryBounds = compose.onNodeWithTag("pixel-diff-summary").fetchSemanticsNode().boundsInRoot
        assertTrue("Images retain more space than summary", imageBounds.height > summaryBounds.height)
        compose.onNodeWithContentDescription("Before").assertIsDisplayed()
        compose.onNodeWithContentDescription("After").assertIsDisplayed()
        compose.onNode(hasText("Size changed") and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Screenshot size changed:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        val file = File("../build/desktop-review/pixel-diff-short-large.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun missingImageOffersAWorkingRetryAction() {
        var retries = 0
        show(ComparisonMode.ImageSwipe, before = null, onRetry = { retries++ })
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun failedHighlightExplainsTheFailureInsteadOfSilentlyShowingSlider() {
        val diff = SnapshotImageDiff(false, 100.0, 0L, 0L, 10, 10, 10, 10, null, error = "Cannot decode")
        show(ComparisonMode.ImageDiff, diff = diff, onRetry = {})
        compose.onNodeWithText("The highlight could not be prepared.", substring = true).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertDoesNotExist()
        compose.onNodeWithText("Retry").performScrollTo().assertIsDisplayed()
    }

    @Test fun dimensionChangesDoNotClaimMisleadingPixelStatistics() {
        val diff = SnapshotImageDiff(false, 100.0, 0L, 0L, 10, 10, 20, 10, null, SnapshotDimensionMismatch(10, 10, 20, 10))
        show(ComparisonMode.ImageDiff, diff = diff)
        compose.onNodeWithText("100.0% changed").assertDoesNotExist()
        compose.onNodeWithText("0 pixels different").assertDoesNotExist()
        compose.onNodeWithText("Compare the captures below;", substring = true).assertIsDisplayed()
    }

    @Test fun beforeViewRetainsAvailableImageWhenAfterIsMissing() {
        show(ComparisonMode.ImageBefore, after = null)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Before").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Before").assertIsDisplayed()
    }

    @Test fun afterViewRetainsAvailableImageWhenBeforeIsMissing() {
        show(ComparisonMode.ImageAfter, before = null)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("After").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("After").assertIsDisplayed()
    }

    @Test fun rtlKeepsLabelsAndDividerAlignedWithPhysicalImages() {
        fun solidImage(rgb: Int): ByteArray {
            val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
            image.setRGB(0, 0, 10, 10, IntArray(100) { rgb }, 0, 10)
            return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        }
        show(ComparisonMode.ImageSwipe, before = solidImage(0xff0000), after = solidImage(0x0000ff), direction = LayoutDirection.Rtl)
        val before = compose.onNodeWithText("Before").fetchSemanticsNode().boundsInRoot
        val after = compose.onNodeWithText("After").fetchSemanticsNode().boundsInRoot
        assertTrue("Before labels the left image and After labels the right image", before.right < after.left)
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        val leftPosition = slider.fetchSemanticsNode().boundsInRoot.center.x
        slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertTrue("Right arrow moves the physical divider right in RTL too", slider.fetchSemanticsNode().boundsInRoot.center.x > leftPosition)
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertTrue(slider.fetchSemanticsNode().boundsInRoot.right <= compose.onRoot().fetchSemanticsNode().boundsInRoot.right)
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        val rendered = compose.onRoot().captureToImage()
        val pixels = rendered.toPixelMap()
        val leftPixel = pixels[(pixels.width * 0.35f).toInt(), pixels.height / 2]
        val rightPixel = pixels[(pixels.width * 0.75f).toInt(), pixels.height / 2]
        assertTrue("Left side retains Before image", leftPixel.red > 0.9f && leftPixel.blue < 0.1f)
        assertTrue("Right side retains After image", rightPixel.blue > 0.9f && rightPixel.red < 0.1f)
        val image = File("../build/desktop-review/image-swipe-rtl.png")
        image.parentFile.mkdirs()
        ImageIO.write(rendered.toAwtImage(), "png", image)
    }

    private fun tallBands(colors: List<Int>): ByteArray {
        val image = BufferedImage(100, 600, BufferedImage.TYPE_INT_RGB)
        return try {
            image.createGraphics().let { graphics ->
                try {
                    colors.forEachIndexed { index, rgb ->
                        graphics.color = java.awt.Color(rgb)
                        graphics.fillRect(0, index * 200, 100, 200)
                    }
                } finally {
                    graphics.dispose()
                }
            }
            ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        } finally {
            image.flush()
        }
    }

    @Test fun tallSliderKeepsBothCapturesAtReadableWidthAndScrollsThemTogether() {
        val before = tallBands(listOf(0xff0000, 0x00ff00, 0x0000ff))
        val after = tallBands(listOf(0x00ffff, 0xffff00, 0xff00ff))
        show(ComparisonMode.ImageSwipe, before, after)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("After").fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithContentDescription("Before").fetchSemanticsNodes().isNotEmpty() }
        val scroll = compose.onNodeWithTag("screenshot-scroll")
        val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("Long pages render at available width rather than fitting the whole document", range.maxValue() > 1_000f)
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, range.maxValue()) }
        compose.waitForIdle()
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val left = pixels[(pixels.width * 0.25f).toInt(), pixels.height / 2]
        val right = pixels[(pixels.width * 0.75f).toInt(), pixels.height / 2]
        assertTrue("Before reaches its blue footer", left.blue > 0.9f && left.red < 0.1f)
        assertTrue("After reaches its magenta footer at the same vertical position", right.red > 0.9f && right.blue > 0.9f && right.green < 0.1f)
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertIsDisplayed()
    }

    @Test fun tallHighlightKeepsItsChangedPixelsReachableByScrolling() {
        val image = tallBands(listOf(0xffffff, 0xffffff, 0xff0000))
        val diff = SnapshotImageDiff(false, 33.3, 20_000L, 60_000L, 100, 600, 100, 600, image)
        show(ComparisonMode.ImageDiff, image, image, diff = diff)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Changed pixels").fetchSemanticsNodes().isNotEmpty() }
        val scroll = compose.onNodeWithTag("screenshot-scroll")
        val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.maxValue() > 1_000f)
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, range.maxValue()) }
        compose.waitForIdle()
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val footer = pixels[pixels.width / 2, (pixels.height * 0.75f).toInt()]
        assertTrue("The highlight footer remains reachable", footer.red > 0.9f && footer.green < 0.1f)
    }

    @Test fun swipeSupportsKeyboardBoundsAndRejectsNonFiniteProgress() {
        show(ComparisonMode.ImageSwipe)
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(0.55f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0.001f) }
        slider.performKeyInput {
            pressKey(Key.MoveEnd)
            pressKey(Key.DirectionRight)
        }
        compose.runOnIdle { assertEquals(1f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0f) }
        compose.runOnIdle {
            assertTrue(slider.fetchSemanticsNode().boundsInRoot.right <= compose.onRoot().fetchSemanticsNode().boundsInRoot.right)
        }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { assertFalse(it(Float.NaN)) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { assertFalse(it(Float.POSITIVE_INFINITY)) }
        slider.performKeyInput {
            pressKey(Key.MoveHome)
            pressKey(Key.DirectionLeft)
        }
        compose.runOnIdle {
            assertEquals(0f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0f)
            assertTrue(slider.fetchSemanticsNode().boundsInRoot.left >= 0f)
        }
        val image = File("../build/desktop-review/image-swipe-keyboard-focus.png")
        image.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", image)
    }
}
