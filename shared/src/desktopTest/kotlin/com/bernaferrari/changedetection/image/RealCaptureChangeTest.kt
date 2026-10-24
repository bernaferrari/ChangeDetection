package com.bernaferrari.changedetection.image

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pairs of real phone captures, compared exactly the way a scheduled check compares them. */
class RealCaptureChangeTest {
    private fun capture(name: String) =
        requireNotNull(javaClass.getResourceAsStream("/captures/$name.png")).readBytes()

    /** One short line of text rewritten: its area is only 4 working rows tall, which rounding once dropped. */
    @Test
    fun aRewrittenShortLineIsAVisibleChange() = runBlocking {
        val change = requireNotNull(
            computeVisualChange(
                capture("short-text-edit-before"),
                capture("short-text-edit-after"),
                opaqueWhiteBackdrop = true
            )
        )
        assertTrue(change.visible)
        assertTrue(change.regions.isNotEmpty())
    }
}
