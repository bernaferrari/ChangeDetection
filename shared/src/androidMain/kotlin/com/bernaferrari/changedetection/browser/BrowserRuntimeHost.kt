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
            private val pendingErrors = mutableListOf<Pair<String, com.multiplatform.webview.web.WebViewError>>()

            private fun fail(url: String, error: com.multiplatform.webview.web.WebViewError) {
                pendingErrors.add(url to error)
                state.errorsForCurrentRequest.add(error)
            }

            override fun onPageStarted(view: android.webkit.WebView, url: String?, favicon: android.graphics.Bitmap?) {
                // Chromium can report the HTTP failure before onPageStarted. The library clears
                // errors here, so carry only this navigation's early failures across that reset.
                val early = pendingErrors.filter { it.first == url }.map { it.second }
                pendingErrors.clear()
                super.onPageStarted(view, url, favicon)
                state.errorsForCurrentRequest.addAll(early)
            }

            override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                pendingErrors.clear()
                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpError(view: android.webkit.WebView, request: android.webkit.WebResourceRequest, response: android.webkit.WebResourceResponse) {
                super.onReceivedHttpError(view, request, response)
                if (request.isForMainFrame) {
                    fail(request.url.toString(), com.multiplatform.webview.web.WebViewError(response.statusCode, response.reasonPhrase, true))
                }
            }

            override fun onReceivedSslError(view: android.webkit.WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                // Keep WebView's default certificate rejection; never offer a bypass.
                super.onReceivedSslError(view, handler, error)
                if (error.url == state.lastLoadedUrl) {
                    fail(error.url, com.multiplatform.webview.web.WebViewError(android.webkit.WebViewClient.ERROR_FAILED_SSL_HANDSHAKE, "Certificate validation failed", true))
                }
            }
        },
    )
}
