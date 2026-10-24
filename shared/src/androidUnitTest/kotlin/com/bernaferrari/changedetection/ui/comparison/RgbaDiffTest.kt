package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.RgbaDiff

import com.bernaferrari.changedetection.image.RgbaImage

import org.junit.Assert.*
import org.junit.Test

class RgbaDiffTest {
    @Test fun screenshotBackdropMatchesLegacyAlphaWithoutChangingRawImageSemantics() {
        val legacy = RgbaImage(1, 1, byteArrayOf(0, 0, 0, 0))
        val white = RgbaImage(1, 1, byteArrayOf(-1, -1, -1, -1))
        val original = legacy.pixels.copyOf()
        assertFalse(RgbaDiff.compare(legacy, white, 0.0, false).match)
        assertTrue(RgbaDiff.compare(legacy, white, 0.0, false, opaqueWhiteBackdrop = true).match)
        assertArrayEquals(original, legacy.pixels)
    }

    @Test fun screenshotBackdropPreservesPartialAlphaEdgesAndRealForegroundChanges() {
        val redEdge = RgbaImage(1, 1, byteArrayOf(-1, 0, 0, -128))
        val onWhite = RgbaImage(1, 1, byteArrayOf(-1, 127, 127, -1))
        assertTrue(RgbaDiff.compare(redEdge, onWhite, 0.0, false, opaqueWhiteBackdrop = true).match)
        val blue = RgbaImage(1, 1, byteArrayOf(0, 0, -1, -1))
        assertFalse(RgbaDiff.compare(redEdge, blue, 0.0, false, opaqueWhiteBackdrop = true).match)
        val black = RgbaImage(1, 1, byteArrayOf(0, 0, 0, -1))
        assertFalse(RgbaDiff.compare(onWhite, black, 0.0, false, opaqueWhiteBackdrop = true).match)
    }

    @Test fun defaultSensitivityRetainsBroadLowContrastButIgnoresAntialiasingNoise() {
        fun image(channel: Int) = RgbaImage(64, 64, ByteArray(64 * 64 * 4) { if (it % 4 == 3) 255.toByte() else channel.toByte() })
        val before = image(255)
        val broad = RgbaDiff.compare(before, image(230), com.bernaferrari.changedetection.image.DEFAULT_IMAGE_DIFF_THRESHOLD, false)
        assertEquals(4096L, broad.differentPixels)
        val noisy = before.pixels.copyOf()
        for (pixel in 0 until 16) for (channel in 0..2) noisy[pixel * 4 + channel] = 251.toByte()
        assertTrue(RgbaDiff.compare(before, RgbaImage(64, 64, noisy), com.bernaferrari.changedetection.image.DEFAULT_IMAGE_DIFF_THRESHOLD, false).match)
    }

    @Test fun unchangedPixelsMatchAndProduceNoRedHighlights() {
        val pixels = RgbaImage(1, 1, byteArrayOf(40, 50, 60, -1))
        val difference = RgbaDiff.compare(pixels, pixels, 0.1)
        assertTrue(difference.match)
        assertEquals(0L, difference.differentPixels)
        assertArrayEquals(byteArrayOf(25, 25, 25, -1), difference.diffImage)
    }

    @Test fun colorAndTransparencyChangesAreCounted() {
        val before = RgbaImage(2, 1, byteArrayOf(0, 0, 0, -1, 0, 0, 0, -1))
        val after = RgbaImage(2, 1, byteArrayOf(-1, 0, 0, -1, 0, 0, 0, 0))
        val difference = RgbaDiff.compare(before, after, 0.1)
        assertEquals(2L, difference.differentPixels)
        assertEquals(100.0, difference.mismatchPercentage, 0.0)
        val statisticsOnly = RgbaDiff.compare(before, after, 0.1, includeDiffImage = false)
        assertEquals(difference.copy(diffImage = null), statisticsOnly)
        assertNull(statisticsOnly.diffImage)
    }

    @Test fun toleranceUsesCombinedChannelDistanceAndRejectsInvalidValues() {
        val before = RgbaImage(1, 1, byteArrayOf(0, 0, 0, -1))
        assertTrue(RgbaDiff.compare(before, RgbaImage(1, 1, byteArrayOf(30, 0, 0, -1)), 0.1).match)
        assertFalse(RgbaDiff.compare(before, RgbaImage(1, 1, byteArrayOf(30, 30, 30, -1)), 0.1).match)
        assertFalse(RgbaDiff.compare(before, RgbaImage(1, 1, byteArrayOf(0, 0, 0, -128)), 0.1).match)
        for (invalid in listOf(-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { RgbaDiff.compare(before, before, invalid) }
        }
    }

    @Test fun thresholdIgnoresMinorNoiseAndDimensionsMustAgree() {
        val before = RgbaImage(1, 1, byteArrayOf(0, 0, 0, -1))
        val after = RgbaImage(1, 1, byteArrayOf(1, 1, 1, -1))
        assertTrue(RgbaDiff.compare(before, after, 0.1).match)
        assertFalse(RgbaDiff.compare(before, after, 0.0).match)
        assertFalse(RgbaDiff.compare(before, RgbaImage(2, 1, ByteArray(8)), 0.1).match)
    }
}
