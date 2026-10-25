package com.bernaferrari.changedetection.browser

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.detection.TextRegionIssue
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleVisualRegionVisibilityTest {
    private val html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body style='margin:0'><div id='watched' style='width:160px;height:120px;background:blue'>Stable selected inventory</div></body></html>"

    @Test fun excludingTheEntireWatchedElementMustFailInsteadOfReturningBlankPixels() = runBlocking {
        TestPageServer(200, html).use { server ->
            val error = runCatching { capture(server.url, exclude = "#watched") }.exceptionOrNull()
            assertTrue("A completely excluded selection must be a region failure, got $error", error is TextRegionException)
            assertEquals(TextRegionIssue.EXCLUDED, (error as TextRegionException).issue)
        }
    }

    @Test fun theSameVisibleSelectionStillCapturesItsActualPixels() = runBlocking {
        TestPageServer(200, html).use { server ->
            val result = capture(server.url)
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)) { "Capture must decode as an image" }
            try {
                assertEquals(320, bitmap.width)
                assertEquals(240, bitmap.height)
                assertEquals("Visible blue content must not become a blank white crop", Color.BLUE, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
            } finally {
                bitmap.recycle()
            }
        }
    }

    private suspend fun capture(url: String, exclude: String = ""): ScreenshotCaptureOutcome.Success = withTimeout(30_000) {
        withContext(Dispatchers.Main.immediate) {
            val browser = AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create()
            try {
                val site = Site("Selection visibility", url, 0L, "selection-visibility", ColorGroup(0, 0), true, false)
                    .copy(captureMode = CaptureMode.SCREENSHOT, textIncludeSelector = "#watched", textExcludeSelector = exclude)
                browser.capture(site, 0) as ScreenshotCaptureOutcome.Success
            } finally {
                browser.close()
            }
        }
    }
}
