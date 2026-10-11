/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import org.junit.Assert.*
import org.junit.Test

class CaptureBoundsTest {
    @Test fun selectedPartsAreNeverSilentlyTruncatedAtTheHeightBudget() {
        assertEquals(6000.0, selectedCaptureHeight(5000.0, 11000.0, 6000.0), 0.0)
        for ((top, bottom) in listOf(5000.0 to 11001.0, 1.0 to 1.0, 0.0 to Double.NaN)) {
            assertThrows(IllegalArgumentException::class.java) { selectedCaptureHeight(top, bottom, 6000.0) }
        }
    }

    @Test fun aSelectionOutsideTheDocumentIsRejectedRatherThanClampedToOneRow() {
        assertThrows(IllegalArgumentException::class.java) {
            selectedCaptureBounds(listOf(0.0, 6000.0, 100.0, 6120.0, 6120.0, 400.0), 2.0, 800, 4000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            selectedCaptureBounds(listOf(-1.0, 0.0, 100.0, 120.0, 400.0, 400.0), 2.0, 800, 800)
        }
        assertEquals(CaptureBounds(350, 3000, 430, 3060), selectedCaptureBounds(listOf(700.0, 6000.0, 860.0, 6120.0, 7000.0, 1200.0), 0.5, 600, 3500))
    }
}
