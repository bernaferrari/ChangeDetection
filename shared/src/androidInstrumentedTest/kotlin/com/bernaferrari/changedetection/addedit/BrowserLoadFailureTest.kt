package com.bernaferrari.changedetection.addedit

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class BrowserLoadFailureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun mainFrameHttpFailureCannotBeWatchedAndRetryRecovers() {
        TestPageServer(503, "<title>Unavailable</title><p>Please try later</p>").use { server ->
            show(server.url)
            waitForPage("Unavailable")
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Watch page").assertIsNotEnabled()
            compose.onNodeWithText("Retry").assertExists()
            server.status = 200
            server.html = "<title>Recovered</title><p>Tickets available</p>"
            compose.onNodeWithText("Retry").performClick()
            waitForPage("Recovered")
            compose.onNodeWithText("Watch page").assertIsEnabled()
            compose.onNodeWithText("Retry").assertDoesNotExist()
        }
    }

    @Test fun failedImageDoesNotBlockTheMainDocument() {
        TestPageServer(503, "missing image").use { image ->
            TestPageServer(200, "<title>Healthy</title><p>Tickets available</p><img src='${image.url}'>").use { page ->
                show(page.url)
                waitForPage("Healthy")
                compose.onNodeWithText("Watch page").assertIsEnabled()
                compose.onNodeWithText("Retry").assertDoesNotExist()
            }
        }
    }

    @Test fun connectionFailureRecoversOnFreshNavigation() {
        val closed = java.net.ServerSocket(0).use { "http://127.0.0.1:${it.localPort}/" }
        show(closed)
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Watch page").assertIsNotEnabled()
        TestPageServer(200, "<title>New page</title><p>Applications open</p>").use { server ->
            compose.runOnUiThread { findWebView(compose.activity.window.decorView)!!.loadUrl(server.url) }
            waitForPage("New page")
            compose.onNodeWithText("Watch page").assertIsEnabled()
            compose.onNodeWithText("Retry").assertDoesNotExist()
        }
    }

    @Test fun liveViewerRetryRecreatesAFailedBrowser() {
        TestPageServer(503, "<title>Unavailable</title>").use { server ->
            compose.setContent {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    com.bernaferrari.changedetection.localBrowser.LiveBrowserPage(server.url, androidx.compose.ui.Modifier)
                }
            }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
            server.status = 200
            server.html = "<title>Viewer recovered</title><p>Page content</p>"
            compose.onNodeWithText("Retry").performClick()
            waitForPage("Viewer recovered")
            compose.onNodeWithText("Retry").assertDoesNotExist()
        }
    }

    private fun show(url: String) {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                WatchBrowser(url, "", emptyList(), { _, _ -> }, {}, {}, {}, "Watch page")
            }
        }
    }

    private fun waitForPage(title: String) {
        val loaded = AtomicBoolean(false)
        compose.waitUntil(20_000) {
            compose.runOnUiThread {
                val web = findWebView(compose.activity.window.decorView)
                loaded.set(web?.title == title && web.progress == 100)
            }
            loaded.get()
        }
        compose.waitForIdle()
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }
}
