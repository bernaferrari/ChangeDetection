package com.bernaferrari.changedetection.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureModeTest {
    @Test fun inheritedMonitorsKeepTheCurrentGlobalChoice() {
        ComparisonMethod.entries.forEach { assertEquals(it, CaptureMode.DEFAULT.resolve(it)) }
    }

    @Test fun explicitPageChoicesAreIndependentOfTheGlobalChoice() {
        ComparisonMethod.entries.forEach { default ->
            assertEquals(ComparisonMethod.HTML, CaptureMode.HTML.resolve(default))
            assertEquals(ComparisonMethod.HTML, CaptureMode.TEXT.resolve(default))
            assertEquals(ComparisonMethod.HTML, CaptureMode.SOURCE.resolve(default))
            assertEquals(ComparisonMethod.SCREENSHOT, CaptureMode.SCREENSHOT.resolve(default))
        }
    }
}
