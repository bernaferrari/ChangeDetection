package com.bernaferrari.changedetection.image

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DiffCancellationTest {
    @Test fun pixelScanChecksCancellationWhileWorkingAndProducesNoPartialResult() {
        val job = Job()
        var checks = 0
        var published = false
        val image = RgbaImage(128, 128, ByteArray(128 * 128 * 4))
        try {
            RgbaDiff.compare(image, image, 0.1, true) {
                if (++checks == 3) job.cancel()
                job.ensureActive()
            }
            published = true
            fail("A cancelled pixel scan must stop")
        } catch (_: CancellationException) { }
        assertEquals(3, checks)
        assertFalse(published)
    }

    @Test fun rowAlignmentChecksCancellationInsideItsWork() {
        val job = Job()
        var checks = 0
        val before = IntArray(128 * 200) { 0xffeeeeee.toInt() }
        try {
            VisualRegions.compare(before, 200, before, 200, 128) {
                if (++checks == 4) job.cancel()
                job.ensureActive()
            }
            fail("A cancelled region comparison must stop")
        } catch (_: CancellationException) { }
        assertEquals(4, checks)
    }

    @Test fun checkpointsDoNotChangeUncancelledPixelResults() {
        val image = RgbaImage(100, 100, ByteArray(40_000) { it.toByte() })
        var checks = 0
        val expected = RgbaDiff.compare(image, image, 0.1)
        val actual = RgbaDiff.compare(image, image, 0.1, true) { checks++ }
        assertEquals(expected.copy(diffImage = null), actual.copy(diffImage = null))
        assertArrayEquals(expected.diffImage, actual.diffImage)
        assertTrue(checks > 1)
    }
}
