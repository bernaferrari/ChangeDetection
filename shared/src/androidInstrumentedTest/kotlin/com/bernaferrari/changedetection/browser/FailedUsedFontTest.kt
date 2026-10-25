package com.bernaferrari.changedetection.browser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class FailedUsedFontTest {
    @Test fun aFailedFontUsedByTheWatchedTextMustNotBecomeAFallbackScreenshotAndCanRecover() = runBlocking {
        FontFixtureServer().use { server ->
            val broken = """<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>@font-face{font-family:CaptureFixture;src:url('/missing.ttf');font-display:swap}</style></head><body><div id='watched' style='width:160px;height:120px;font:24px CaptureFixture,monospace'>Stable selected inventory</div><script>document.fonts.load('24px CaptureFixture').catch(function(){});</script></body></html>"""
            // The fixture deliberately returns HTTP 200 HTML for this path, not font data.
            // This exercises FontFace.status=error, independently of HTTP-error callbacks.
            server.customHtml = broken
            val error = runCatching { capture(server.url) }.exceptionOrNull()
            assertTrue("A failed used font must not yield a fallback PNG; got ${error?.javaClass?.simpleName ?: "successful capture"}", error is CaptureNotReadyException)
            // Compare the fixed name so this same test also compiles against the old enum,
            // where RESOURCE_FAILED did not yet exist, for a genuine failing baseline run.
            assertEquals("RESOURCE_FAILED", (error as CaptureNotReadyException).issue.name)

            server.customHtml = broken.replace("/missing.ttf", "/font.ttf")
            val recovered = capture(server.url)
            assertEquals("image/png", recovered.contentType)
            assertTrue("A healthy used font must still allow capture", recovered.bytes.isNotEmpty())
        }
    }

    @Test fun aFailedUnusedWeightInTheSameFamilyDoesNotBlockTheHealthyWatchedWeight() = runBlocking {
        FontFixtureServer().use { server ->
            server.customHtml = """<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>@font-face{font-family:CaptureFixture;font-weight:400;src:url('/font.ttf');font-display:swap}@font-face{font-family:CaptureFixture;font-weight:900;src:url('/missing.ttf');font-display:swap}</style></head><body><div id='watched' style='width:160px;height:120px;font:400 20px CaptureFixture,monospace'>Stable selected inventory</div><script>document.fonts.load('900 20px CaptureFixture').catch(function(){});</script></body></html>"""
            // The unused 900 face fails decoding HTML, but the selected text uses the healthy
            // 400 face. Sharing a family name must not make that unrelated failure blocking.
            val result = capture(server.url)
            assertEquals("image/png", result.contentType)
            assertTrue("A failed unused weight must not poison the healthy watched weight", result.bytes.isNotEmpty())
        }
    }

    private suspend fun capture(url: String): ScreenshotCaptureOutcome.Success = withTimeout(30_000) {
        withContext(Dispatchers.Main.immediate) {
            val browser = AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create()
            try {
                val site = Site("Used font", url, 0L, "failed-used-font", ColorGroup(0, 0), true, false)
                    .copy(captureMode = CaptureMode.SCREENSHOT, textIncludeSelector = "#watched")
                browser.capture(site, 0) as ScreenshotCaptureOutcome.Success
            } finally {
                browser.close()
            }
        }
    }
}
