package com.bernaferrari.changedetection.localBrowser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bernaferrari.kmpwright.compose.KmpwrightRetainedWebView
import com.bernaferrari.kmpwright.compose.KmpwrightWebSession
import com.bernaferrari.kmpwright.compose.rememberKmpwrightController

@Composable
internal actual fun LiveBrowserPage(url: String, modifier: Modifier) {
    val browser = viewModel(key = "live-browser:$url") { LiveBrowserSession() }
    Box(modifier) {
        val session = browser.session
        key(session) {
            val controller = rememberKmpwrightController()
            if (browser.failed || controller.lastError != null) {
                LiveBrowserError(onRetry = browser::retry)
            } else {
                KmpwrightRetainedWebView(
                    session = session,
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    startUrl = url,
                    captureBackPresses = false,
                    onCreated = { webView ->
                        webView.webViewClient = LiveBrowserClient(webView.url ?: url, browser::fail)
                    },
                )
            }
        }
    }
}

/** Owned by the navigation entry, not by the Activity or native view. */
private class LiveBrowserSession : ViewModel() {
    var session by mutableStateOf(KmpwrightWebSession())
        private set
    var failed by mutableStateOf(false)
        private set

    fun fail() {
        failed = true
    }

    fun retry() {
        session.close()
        session = KmpwrightWebSession()
        failed = false
    }

    override fun onCleared() {
        session.close()
    }
}

/** KMPwright forwards these callbacks from its controller to the host client. */
private class LiveBrowserClient(startUrl: String, private val onMainFailure: () -> Unit) : WebViewClient() {
    private var mainUrl = startUrl

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        url?.let { mainUrl = it }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) onMainFailure()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onMainFailure()
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        // This callback has no frame information. Main-document failures also arrive in
        // onReceivedError; recognizing the pending URL makes TLS failure visible sooner.
        if (error.url.substringBefore('#') == mainUrl.substringBefore('#')) onMainFailure()
    }
}
