package com.bernaferrari.changedetection.browser

import android.content.Context
import android.view.ContextThemeWrapper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.detection.prepareTextCapture
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.sync.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureStabilityRegressionTest {
    @Test fun pendingVisibleImageMustNotBecomeASavedCapture() = runBlocking {
        TestPageServer(200, "<body>Stable content<img src='/slow-image' width='160' height='120'></body>", resourceDelayMs = 20_000).use { server ->
            val error = runCatching { capture(server.url, CaptureMode.SCREENSHOT, "") }.exceptionOrNull()
            assertNotNull("A pending visible image must fail instead of saving an incomplete page", error)
        }
    }

    @Test fun aFontUsedOnlyOutsideTheSelectedCaptureDoesNotBlockIt() = runBlocking {
        FontFixtureServer().use { server ->
            server.delayMs = 20_000
            server.customHtml = """<meta name='viewport' content='width=device-width,initial-scale=1'><style>@font-face{font-family:CaptureFixture;src:url('/font.ttf')}</style><div id='watched' style='width:160px;height:120px;font:20px sans-serif'>Stable selected content</div><div style='margin-top:6000px;font:24px CaptureFixture'>Outside capture</div><script>document.fonts.load('24px CaptureFixture')</script>"""
            assertNotNull(capture(server.url, CaptureMode.SCREENSHOT, "#watched"))
        }
    }

    @Test fun delayedWebFontDoesNotProduceAFalseVisualChange() = runBlocking {
        FontFixtureServer().use { server ->
            server.delayMs = 5_000
            val first = capture(server.url, CaptureMode.SCREENSHOT, "")
            server.delayMs = 0
            val second = capture(server.url, CaptureMode.SCREENSHOT, "")
            assertArrayEquals("Only font response timing changed; rendered content must match", first.bytes, second.bytes)
        }
    }

    @Test fun missingScreenshotSelectionIsARegionFailureNotAnOfflineError() = runBlocking {
        TestPageServer(200, "<body>Healthy unselected content</body>").use { server ->
            val error = runCatching { capture(server.url, CaptureMode.SCREENSHOT, "#missing") }.exceptionOrNull()
            assertTrue("Expected a region failure; got $error", error is TextRegionException)
        }
    }

    @Test fun screenshotWaitsForSelectedContentAfterDomReadiness() = runBlocking {
        delayedPage().use { server ->
            val result = capture(server.url, CaptureMode.SCREENSHOT, "#late")
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size, bounds)
            assertEquals(320, bounds.outWidth)
            assertEquals(240, bounds.outHeight)
        }
    }

    @Test fun textWaitsForTheSameSelectedContentAsScreenshot() = runBlocking {
        delayedPage().use { server ->
            val result = capture(server.url, CaptureMode.TEXT, "#late")
            val selected = prepareTextCapture(result.bytes, result.contentType, TextRegion("#late"))
            assertTrue(selected.data.decodeToString().contains("Stable inventory"))
        }
    }

    private fun delayedPage() = TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0">Loading<script>setTimeout(function(){var p=document.createElement('div');p.id='late';p.style.cssText='width:160px;height:120px;background:blue';p.textContent='Stable inventory';document.body.appendChild(p)},3500)</script></body>""")

    private suspend fun capture(url: String, mode: CaptureMode, selector: String): ScreenshotCaptureOutcome.Success = withContext(Dispatchers.Main.immediate) {
        val browser = AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create()
        try {
            browser.capture(Site("Fixture", url, 0L, "stability", ColorGroup(0, 0), true, false).copy(captureMode = mode, textIncludeSelector = selector), 0) as ScreenshotCaptureOutcome.Success
        } finally {
            browser.close()
        }
    }
}
