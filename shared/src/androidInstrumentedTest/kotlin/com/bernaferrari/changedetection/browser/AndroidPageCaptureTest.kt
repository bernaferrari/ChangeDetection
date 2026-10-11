/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPageCaptureTest {
    @Test fun partialFinalTileIncludesFooterAfterTextAndMargins() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:white;color:#182226;font-family:sans-serif"><h1>Camera restock</h1><p>12 cameras available today</p><div style="height:1800px;background:#eef3f8">Delivery details</div><h2 style="margin:0;height:100px;background:#ff0000">End of captured page</h2></body>""").use { server ->
            val result = capture(server.url)
            val bitmap = BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)
            try {
                val bottom = bitmap.getPixel(bitmap.width / 2, bitmap.height - 20)
                assertTrue("The partial final tile must include the red footer", android.graphics.Color.red(bottom) > 200 && android.graphics.Color.green(bottom) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun shortStyledPageContainsItsVisibleText() = runBlocking {
        TestPageServer(200, """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{font-family:system-ui;background:#f6f8fa;padding:32px;color:#182226}article{background:white;padding:28px;border-radius:24px}h1{font-size:32px}</style></head><body><article><p>STOCK WATCH</p><h1>Everyday camera</h1><p>Now available — 12 cameras in stock</p><p>Orders are open.</p></article></body></html>""").use { server ->
            val capture = capture(server.url)
            val bitmap = BitmapFactory.decodeByteArray(capture.bytes, 0, capture.bytes.size)
            try {
                var darkPixels = 0
                for (y in 0 until bitmap.height step 4) {
                    for (x in 0 until bitmap.width step 4) {
                        val pixel = bitmap.getPixel(x, y)
                        if (android.graphics.Color.red(pixel) < 100 && android.graphics.Color.green(pixel) < 100) darkPixels++
                    }
                }
                assertTrue("Short pages must contain text, not just the page background", darkPixels > 20)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun fullPageCaptureIncludesOffscreenContentAndStaysWithinPixelBudget() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:white"><div style="height:4000px">TOP OF PAGE</div><div style="height:300px;background:#ff0000">BOTTOM OF PAGE</div></body>""").use { server ->
            val screenshot = capture(server.url)
            assertTrue(screenshot.height > 1920)
            assertTrue(screenshot.width.toLong() * screenshot.height <= 7_000_000)
            val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
            try {
                val bottom = bitmap.getPixel(bitmap.width / 2, bitmap.height - 20)
                assertTrue("The offscreen bottom marker must be red", android.graphics.Color.red(bottom) > 200 && android.graphics.Color.green(bottom) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun httpFailureCannotBecomeASuccessfulScreenshot() = runBlocking {
        TestPageServer(503, "Service unavailable").use { server ->
            val error = runCatching { capture(server.url) }.exceptionOrNull()
            assertNotNull(error)
            assertTrue(error?.message.orEmpty().contains("503"))
        }
    }

    @Test(timeout = 25_000)
    fun slowSecondaryResourcesDoNotBlockAUsablePage() = runBlocking {
        TestPageServer(200, "<body style='background:#0000ff'>Ready content<img src='/slow-image' width='1' height='1'></body>", resourceDelayMs = 45_000).use { server ->
            val screenshot = capture(server.url)
            val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
            try {
                val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height - 20)
                assertTrue("Capture the usable document while the secondary resource is still loading", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun freshChecksSeeChangesEvenWhenTheServerAllowsLongLivedCaching() = runBlocking {
        TestPageServer(200, "<body style='background:#ff0000'>Original</body>", cacheControl = "public, max-age=86400").use { server ->
            val first = capture(server.url)
            server.html = "<body style='background:#0000ff'>Changed</body>"
            val changed = capture(server.url)
            val bitmap = BitmapFactory.decodeByteArray(changed.bytes, 0, changed.bytes.size)
            try {
                val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height - 20)
                assertTrue("A fresh check must see the new blue page, not the cached red page", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
                assertFalse(first.bytes.contentEquals(changed.bytes))
                assertArrayEquals(changed.bytes, capture(server.url).bytes)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun identicalStaticPagesProduceTheSameImage() = runBlocking {
        TestPageServer(200, "<body style='background:white;color:black'><h1>Stable page</h1><p>Unchanged content.</p></body>").use { server ->
            assertArrayEquals(capture(server.url).bytes, capture(server.url).bytes)
        }
    }

    @Test(timeout = 60_000)
    fun repeatedTallChangingPagesStayWithinBudgetWithoutSqueezingContent() = runBlocking {
        repeat(3) { version ->
            val color = if (version % 2 == 0) "#ff0000" else "#0000ff"
            TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:white"><div style="height:18000px;background:$color">Version $version</div><div style="height:300px;background:#00ff00">Beyond the capture limit</div></body>""").use { server ->
                val screenshot = capture(server.url)
                assertTrue(screenshot.height <= 10_000)
                assertTrue(screenshot.width.toLong() * screenshot.height <= 7_000_000)
                val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
                try {
                    val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height - 10)
                    if (version % 2 == 0) {
                        assertTrue(android.graphics.Color.red(pixel) > 200 && android.graphics.Color.blue(pixel) < 40)
                    } else {
                        assertTrue("Selected pixel: ${Integer.toHexString(pixel)}", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    @Test(timeout = 30_000)
    fun oversizedPageFailsExplicitlyAndTheNextCaptureStillWorks() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0"><div style="height:200000px">Too tall</div></body>""").use { server ->
            val error = runCatching { capture(server.url) }.exceptionOrNull()
            assertTrue(error?.message.orEmpty().contains("too tall"))
        }
        TestPageServer(200, "<body style='background:#ff0000'>Recovered</body>").use { server ->
            val screenshot = capture(server.url)
            assertTrue(screenshot.bytes.isNotEmpty())
        }
    }

    @Test fun selectedPartBelowWholePageBudgetIsCapturedDirectly() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:red"><div style="height:6000px"></div><div id="target" style="width:100px;height:120px;background:#00ff00"></div></body>""").use { server ->
            val screenshot = capture(server.url, "#target")
            assertEquals(200, screenshot.width)
            assertEquals(240, screenshot.height)
            val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
            try {
                val pixel = bitmap.getPixel(100, 120)
                assertTrue("Selected pixel: ${Integer.toHexString(pixel)}", android.graphics.Color.green(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun selectedPartUsesActualOverviewScale() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=1200"><body style="margin:0;width:1200px;background:red"><div style="height:6000px"></div><div id="target" style="margin-left:700px;width:160px;height:120px;background:#0000ff"></div></body>""").use { server ->
            val screenshot = capture(server.url, "#target")
            assertTrue(kotlin.math.abs(screenshot.width - 320) <= 3)
            assertTrue(kotlin.math.abs(screenshot.height - 240) <= 3)
            val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
            try {
                val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                assertTrue("Selected pixel: ${Integer.toHexString(pixel)}", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun aTallSelectedPartIncludesItsLastTile() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:red"><div style="height:6000px"></div><div id="target" style="width:100px"><div style="height:1600px;background:#0000ff"></div><div style="height:200px;background:#00ff00"></div></div></body>""").use { server ->
            val screenshot = capture(server.url, "#target")
            assertEquals(200, screenshot.width)
            assertEquals(3600, screenshot.height)
            val bitmap = BitmapFactory.decodeByteArray(screenshot.bytes, 0, screenshot.bytes.size)
            try {
                val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height - 20)
                assertTrue(android.graphics.Color.green(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun selectedPartExceedingBudgetFailsInsteadOfSavingPartialContent() = runBlocking {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0"><div id="target" style="height:9000px">Too large</div></body>""").use { server ->
            val error = runCatching { capture(server.url, "#target") }.exceptionOrNull()
            assertTrue("Expected a budget rejection, got $error", error?.message.orEmpty().contains("too large"))
        }
    }

    private suspend fun capture(url: String, only: String = "") = withContext(Dispatchers.Main.immediate) {
        val browser = com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory(
            ApplicationProvider.getApplicationContext<Context>(),
        ).create()
        try {
            val site = com.bernaferrari.changedetection.repo.Site(null, url, 0L, "capture-test", com.bernaferrari.changedetection.repo.ColorGroup(0, 0), true, false).copy(textIncludeSelector = only)
            val result = browser.capture(site, 300) as com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome.Success
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size, bounds)
            WebViewScreenshot(result.contentType, result.bytes, bounds.outWidth, bounds.outHeight)
        } finally {
            browser.close()
        }
    }
}
