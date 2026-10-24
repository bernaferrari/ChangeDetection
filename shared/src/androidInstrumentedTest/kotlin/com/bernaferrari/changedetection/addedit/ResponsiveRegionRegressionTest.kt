package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.detection.*
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class ResponsiveRegionRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectedResponsiveNodeSurvivesFromNarrowInteractiveBrowserToFreshCheck() = responsive(true)

    @Test fun aPageWithoutViewportMetaUsesTheSameLayoutForSetupAndChecks() = responsive(false)
    private fun responsive(hasMeta: Boolean) {
        val head = if (hasMeta) "<meta name='viewport' content='width=device-width,initial-scale=1'>" else ""
        TestPageServer(200, """$head<body><script>document.body.innerHTML='<p data-watch id="'+(innerWidth<350?'narrow':'wide')+'">Stable inventory 10</p>'</script></body>""").use { server ->
            val live = AtomicReference<LivePage>()
            val selector = mutableStateOf("")
            compose.setContent {
                ChangeDetectionTheme(false, false) {
                    Box(Modifier.width(280.dp).height(500.dp)) {
                        WatchBrowser(server.url, selector.value, emptyList(), { _, _ -> }, {}, { live.set(it) }, {}, "Watch page", captureMode = CaptureMode.TEXT)
                    }
                }
            }
            compose.waitUntil(40_000) {
                runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            val id = AtomicReference<String>()
            compose.runOnUiThread {
                requireNotNull(find(compose.activity.window.decorView)).evaluateJavascript("document.querySelector('[data-watch]').id") { id.set(it.trim('"')) }
            }
            compose.waitUntil(5_000) { id.get() != null }
            compose.runOnUiThread { selector.value = "#" + id.get() }
            compose.onNodeWithText("Watch page").performClick()
            compose.waitUntil(5_000) { live.get() != null }
            val first = pump { requireNotNull(live.get().capturePreview!!.invoke {}).firstVersion!! }
            val fresh = pump {
                withContext(Dispatchers.Main.immediate) {
                    val browser = AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create()
                    try {
                        browser.capture(Site("Fixture", server.url, 0L, "responsive", ColorGroup(0, 0), true, false).copy(captureMode = CaptureMode.TEXT, textIncludeSelector = selector.value), 0) as ScreenshotCaptureOutcome.Success
                    } finally {
                        browser.close()
                    }
                }
            }
            assertEquals(prepareTextCapture(first.data, first.contentType, TextRegion(selector.value)).contentHash, prepareTextCapture(fresh.bytes, fresh.contentType, TextRegion(selector.value)).contentHash)
        }
    }

    @Test fun nativeTouchCoordinatesStillReachTheDisplayedElementInANarrowBrowser() {
        TestPageServer(200, """<meta name="viewport" content="width=device-width,initial-scale=1"><body><button style="position:absolute;left:40px;top:40px;width:160px;height:80px" onclick="document.body.dataset.clicked='yes'">Pick this part</button></body>""").use { server ->
            compose.setContent {
                ChangeDetectionTheme(false, false) {
                    Box(Modifier.width(280.dp).height(500.dp)) {
                        WatchBrowser(server.url, "", emptyList(), { _, _ -> }, {}, {}, {}, "Watch page")
                    }
                }
            }
            compose.waitUntil(40_000) {
                runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            val node = compose.onNodeWithTag("watch-browser-content", useUnmergedTree = true)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            var pageScale = 0f
            compose.runOnUiThread {
                val web = requireNotNull(find(compose.activity.window.decorView))
                assertEquals(1080, web.width)
                assertEquals(1920, web.height)
                @Suppress("DEPRECATION")
                val scale = web.scale
                pageScale = scale
            }
            val fit = minOf(bounds.width / 1080f, bounds.height / 1920f)
            node.performTouchInput { click(androidx.compose.ui.geometry.Offset((bounds.width - 1080 * fit) / 2 + 120 * pageScale * fit, 80 * pageScale * fit)) }
            val clicked = AtomicReference(false)
            compose.waitUntil(5_000) {
                compose.runOnUiThread { requireNotNull(find(compose.activity.window.decorView)).evaluateJavascript("document.body.dataset.clicked==='yes'") { clicked.set(it == "true") } }
                clicked.get()
            }
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
        val job = scope.async { withTimeout(45_000) { block() } }
        try {
            compose.waitUntil(50_000) { job.isCompleted }
            return runBlocking { job.await() }
        } finally {
            scope.cancel()
        }
    }
}
