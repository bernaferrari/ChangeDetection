/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.RgbaDiff

import com.bernaferrari.changedetection.image.RgbaImage

import org.junit.Assert.*
import org.junit.Test

class RgbaDiffTest {
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
