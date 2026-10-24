package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun BrowserRuntimeHost(modifier: Modifier, content: @Composable () -> Unit) = content()

@Composable
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val interceptor = rememberNavigationInterceptor(onNavigationRequest)
    val navigator = com.multiplatform.webview.web.rememberWebViewNavigator(requestInterceptor = interceptor)
    com.multiplatform.webview.web.WebView(state = state, modifier = modifier, navigator = navigator, platformWebViewParams = rememberBrowserPlatformParams())
    androidx.compose.runtime.LaunchedEffect(state.loadingState, highlightUpdate) {
        if (state.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            highlightUpdate?.let { navigator.evaluateJavaScript(it.script()) }
        }
    }
}

internal actual val supportsPageRegionSelection: Boolean = true

@Composable
internal actual fun rememberBrowserPlatformParams(): com.multiplatform.webview.web.PlatformWebViewParams? = androidx.compose.runtime.remember {
    com.multiplatform.webview.web.PlatformWebViewParams(
        client = object : com.multiplatform.webview.web.AccompanistWebViewClient() {
            private fun tlsError() = com.multiplatform.webview.web.WebViewError(android.webkit.WebViewClient.ERROR_FAILED_SSL_HANDSHAKE, "Certificate validation failed", true)
            private val pendingErrors = mutableListOf<Pair<String, com.multiplatform.webview.web.WebViewError>>()

            private fun fail(url: String, error: com.multiplatform.webview.web.WebViewError) {
                pendingErrors.add(url to error)
                state.errorsForCurrentRequest.add(error)
            }

            override fun shouldInterceptRequest(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                if (view != null && request != null) WebViewCaptureHealth.of(view).request(request.url.toString(), request.isForMainFrame)
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageCommitVisible(view: android.webkit.WebView, url: String?) {
                WebViewCaptureHealth.of(view).committed(url)
                super.onPageCommitVisible(view, url)
            }

            override fun onPageStarted(view: android.webkit.WebView, url: String?, favicon: android.graphics.Bitmap?) {
                // Chromium can report the HTTP failure before onPageStarted. The library clears
                // errors here, so carry only this navigation's early failures across that reset.
                val health = WebViewCaptureHealth.of(view)
                health.started(url)
                val early = pendingErrors.filter { it.first == url }.map { it.second }
                pendingErrors.clear()
                super.onPageStarted(view, url, favicon)
                state.errorsForCurrentRequest.addAll(early)
                if (health.hasBlockingFailure()) state.errorsForCurrentRequest.add(tlsError())
            }

            override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                pendingErrors.clear()
                WebViewCaptureHealth.of(view).finished()
                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpError(view: android.webkit.WebView, request: android.webkit.WebResourceRequest, response: android.webkit.WebResourceResponse) {
                super.onReceivedHttpError(view, request, response)
                if (request.isForMainFrame) {
                    fail(request.url.toString(), com.multiplatform.webview.web.WebViewError(response.statusCode, response.reasonPhrase, true))
                }
            }

            override fun onReceivedSslError(view: android.webkit.WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                // Same rule as scheduled checks: a refused image or frame leaves the page usable; the page itself
                // failing TLS blocks watching it.
                val tls = WebViewCaptureHealth.of(view).ssl(error.url, error.primaryError, (0..5).filter(error::hasError))
                if (tls.blocking) fail(error.url, tlsError())
            }
        },
    )
}
