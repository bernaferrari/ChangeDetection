package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.desktopImageDispatcher

import com.bernaferrari.changedetection.image.decodeDesktopImage

import com.bernaferrari.changedetection.image.RgbaDiff

import com.bernaferrari.changedetection.image.RgbaImage

import com.bernaferrari.changedetection.image.PlatformImageDiff

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test

class DesktopImageDecoderTest {
    @Test(timeout = 10_000)
    fun nativeWorkIsBoundedAndCancelledQueuedRequestsNeverStart() = runBlocking {
        val entered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val extraEntered = CountDownLatch(1)
        val cancelledRan = AtomicBoolean(false)
        val workers = List(2) {
            launch(desktopImageDispatcher) {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val obsolete = launch(desktopImageDispatcher) { cancelledRan.set(true) }
            val replacement = launch(desktopImageDispatcher) { extraEntered.countDown() }
            obsolete.cancel()
            assertFalse("Third operation must wait for a native worker", extraEntered.await(150, TimeUnit.MILLISECONDS))
            release.countDown()
            withTimeout(5000) {
                workers.joinAll()
                obsolete.join()
                replacement.join()
            }
            assertFalse(cancelledRan.get())
            assertEquals(0L, extraEntered.count)
        } finally {
            release.countDown()
            workers.forEach { it.cancel() }
        }
    }

    @Test fun largePngIsSampledBeforeDisplayAndComparisonKeepsOriginalDimensions() = runBlocking {
        val bytes = png(6000, 1600)
        val display = requireNotNull(decodeDesktopImage(bytes, 4_000_000, 4096))
        assertEquals(6000, display.originalWidth)
        assertEquals(1600, display.originalHeight)
        assertTrue(display.image.width <= 4096)
        assertTrue(display.image.width.toLong() * display.image.height <= 4_000_000)
        val same = PlatformImageDiff.compare(bytes, bytes, 0.0)
        assertTrue(same.match)
        assertTrue(same.isSampled)
        assertEquals(6000, same.beforeWidth)
        assertEquals(1600, same.beforeHeight)
        assertTrue(same.totalPixels in 1..1_000_000)
        val resized = PlatformImageDiff.compare(bytes, png(6001, 1600), 0.0)
        assertFalse(resized.match)
        assertNotNull(resized.dimensionMismatch)
        assertEquals(6001, resized.afterWidth)
    }

    @Test fun smallImagesPreservePixelsAndMalformedInputFailsSafely() = runBlocking {
        val decoded = requireNotNull(decodeDesktopImage(png(32, 24), 4_000_000, 4096))
        assertEquals(32, decoded.image.width)
        assertEquals(24, decoded.image.height)
        val pixels = IntArray(32 * 24)
        decoded.image.readPixels(pixels)
        assertTrue(pixels.all { it == Color(34, 120, 210).rgb })
        assertNull(decodeDesktopImage(byteArrayOf(1, 2, 3), 4_000_000, 4096))
        val changed = PlatformImageDiff.compare(png(32, 24), png(32, 24, Color.RED), 0.0)
        assertFalse(changed.match)
        assertFalse(changed.isSampled)
        assertEquals(100.0, changed.mismatchPercentage, 0.0)
    }

    @Test fun decodedPngUsesTheSameColorAndAlphaToleranceAsRgbaComparison() = runBlocking {
        val beforePixels = intArrayOf(0xff000000.toInt(), 0xff000000.toInt(), 0xff000000.toInt())
        val afterPixels = intArrayOf(0xff1e0000.toInt(), 0xff320000.toInt(), 0x80000000.toInt())
        fun encoded(pixels: IntArray): ByteArray {
            val image = BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB)
            return try {
                image.setRGB(0, 0, 3, 1, pixels, 0, 3)
                ByteArrayOutputStream().use { output ->
                    check(ImageIO.write(image, "png", output))
                    output.toByteArray()
                }
            } finally {
                image.flush()
            }
        }
        val beforeBytes = encoded(beforePixels)
        val afterBytes = encoded(afterPixels)
        val rgbaBefore = RgbaImage(3, 1, byteArrayOf(0, 0, 0, -1, 0, 0, 0, -1, 0, 0, 0, -1))
        val rgbaAfter = RgbaImage(3, 1, byteArrayOf(30, 0, 0, -1, 50, 0, 0, -1, 0, 0, 0, -128))
        for ((threshold, expectedCount) in listOf(0.0 to 3L, 0.1 to 2L, 1.0 to 0L)) {
            val actual = PlatformImageDiff.compare(beforeBytes, afterBytes, threshold)
            val reference = RgbaDiff.compare(rgbaBefore, rgbaAfter, threshold)
            val statisticsOnly = PlatformImageDiff.compare(beforeBytes, afterBytes, threshold, includeDiffImage = false)
            assertEquals(actual.copy(diffImagePng = null), statisticsOnly)
            assertNull(statisticsOnly.diffImagePng)
            assertEquals(expectedCount, actual.differentPixels)
            assertEquals(reference.differentPixels, actual.differentPixels)
            assertEquals(reference.mismatchPercentage, actual.mismatchPercentage, 0.0)
            assertEquals(reference.match, actual.match)
            val rendered = ImageIO.read(requireNotNull(actual.diffImagePng).inputStream())
            try {
                assertEquals(3, rendered.width)
                assertEquals(1, rendered.height)
                val expectedRgba = requireNotNull(reference.diffImage)
                for (x in 0..2) {
                    val color = rendered.getRGB(x, 0)
                    assertEquals(expectedRgba[x * 4].toInt() and 255, (color ushr 16) and 255)
                    assertEquals(expectedRgba[x * 4 + 1].toInt() and 255, (color ushr 8) and 255)
                    assertEquals(expectedRgba[x * 4 + 2].toInt() and 255, color and 255)
                    assertEquals(255, (color ushr 24) and 255)
                }
            } finally {
                rendered.flush()
            }
        }
    }

    private fun png(width: Int, height: Int, color: Color = Color(34, 120, 210)): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        try {
            image.createGraphics().let { graphics ->
                try {
                    graphics.color = color
                    graphics.fillRect(0, 0, width, height)
                } finally {
                    graphics.dispose()
                }
            }
            return ByteArrayOutputStream().use { output ->
                check(ImageIO.write(image, "png", output))
                output.toByteArray()
            }
        } finally {
            image.flush()
        }
    }
}
