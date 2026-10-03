/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.calculateInSampleSize

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapSamplingTest {
    @Test
    fun `small image keeps original resolution`() {
        assertEquals(
            1,
            calculateInSampleSize(
                width = 1_080,
                height = 1_920,
                maxPixels = 4_000_000,
                maxDimension = 4_096,
            ),
        )
    }

    @Test
    fun `very tall screenshot is reduced below memory and dimension limits`() {
        assertEquals(
            4,
            calculateInSampleSize(
                width = 1_080,
                height = 10_000,
                maxPixels = 4_000_000,
                maxDimension = 4_096,
            ),
        )
    }

    @Test
    fun `large square uses power of two sampling`() {
        assertEquals(
            4,
            calculateInSampleSize(
                width = 8_000,
                height = 8_000,
                maxPixels = 4_000_000,
                maxDimension = 4_096,
            ),
        )
    }
}
