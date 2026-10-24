package com.bernaferrari.changedetection.addedit

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class LiveZoomCaptureRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun browserZoomDoesNotChangeSavedPixelsAndReturnsToTheUsersZoom() {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:white;color:black;font-family:sans-serif"><h1>Stable responsive inventory heading</h1><p>Same price and availability</p><div style="height:700px;background:#abcdef"></div></body>""").use { server ->
            val live = AtomicReference<LivePage>()
            compose.setContent { ChangeDetectionTheme(false, false) { WatchBrowser(server.url, "", emptyList(), { _, _ -> }, {}, { live.set(it) }, {}, "Watch page") } }
            compose.waitUntil(40_000) {
                runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithText("Watch page").performClick()
            compose.waitUntil(5_000) { live.get() != null }
            val first = pump { requireNotNull(live.get().capturePreview!!.invoke {}).firstVersion!!.data }
            val web = AtomicReference<WebView>()
            val initial = AtomicReference<Float>()
            compose.runOnUiThread {
                web.set(requireNotNull(find(compose.activity.window.decorView)))
                @Suppress("DEPRECATION")
                initial.set(web.get().scale)
                web.get().zoomBy(1.5f)
            }
            compose.waitUntil(8_000) {
                var zoomed = false
                compose.runOnUiThread {
                    @Suppress("DEPRECATION")
                    val scale = web.get().scale
                    zoomed = scale > initial.get() * 1.1
                }
                zoomed
            }
            val userScale = AtomicReference<Float>()
            compose.runOnUiThread {
                @Suppress("DEPRECATION")
                userScale.set(web.get().scale)
            }
            val second = pump { requireNotNull(live.get().capturePreview!!.invoke {}).firstVersion!!.data }
            compose.runOnUiThread {
                @Suppress("DEPRECATION")
                assertEquals(userScale.get().toDouble(), web.get().scale.toDouble(), .01)
            }
            assertArrayEquals("Zoom is a browser interaction, not a website change", first, second)
        }
    }
    private fun find(v: View): WebView? = if (v is WebView) {
        v
    } else if (v is ViewGroup) {
        (0 until v.childCount).firstNotNullOfOrNull { find(v.getChildAt(it)) }
    } else {
        null
    }
    private fun <T> pump(block: suspend () -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = scope.async { withTimeout(40_000) { block() } }
        try {
            compose.waitUntil(45_000) { job.isCompleted }
            return runBlocking { job.await() }
        } finally {
            scope.cancel()
        }
    }
}
