package com.bernaferrari.changedetection.browser

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bernaferrari.changedetection.localBrowser.LiveBrowserPage
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test

class RetainedLocalBrowserTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun failedPageIsRetriedWithAFreshSession() {
        TestPageServer(503, "Unavailable").use { broken ->
            TestPageServer(200, "Recovered").use { good ->
                show(broken.url)
                awaitRetry()
                // Keep the original URL: Retry must reload it, not reuse the failed document.
                broken.status = 302
                broken.redirectTo = good.url
                compose.onNodeWithText("Retry").performClick()
                compose.waitUntil(20_000) {
                    compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isEmpty() &&
                        compose.runOnUiThread { compose.activity.findWebViewUrl() == good.url }
                }
            }
        }
    }

    @Test fun anUntrustedTlsPageOffersRetry() {
        untrustedTlsPage().use { server ->
            show(server.url)
            awaitRetry()
        }
    }

    @Test fun rendererCrashOffersRetryWithoutClosingTheApp() {
        show("chrome://crash")
        awaitRetry()
        compose.onNodeWithText("Retry").performClick()
        awaitRetry()
    }

    private fun show(url: String) {
        compose.setContent { ChangeDetectionTheme(false, false) { LiveBrowserPage(url, Modifier.fillMaxSize()) } }
    }

    private fun awaitRetry() = compose.waitUntil(20_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }

    private fun ComponentActivity.findWebViewUrl(): String? = window.decorView.findWebView()?.url

    private fun android.view.View.findWebView(): android.webkit.WebView? {
        if (this is android.webkit.WebView) return this
        if (this is android.view.ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).findWebView()?.let { return it }
            }
        }
        return null
    }
}
