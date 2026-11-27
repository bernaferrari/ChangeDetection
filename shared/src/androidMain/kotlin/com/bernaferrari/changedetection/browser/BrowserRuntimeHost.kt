package com.bernaferrari.changedetection.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.doOnDetach
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.logging.AppLog
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.android_browser_start_failed
import com.bernaferrari.changedetection.shared.generated.resources.browser_load_error_body
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_error_title
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import kotlinx.coroutines.CancellationException

@Composable
internal actual fun BrowserRuntimeHost(modifier: Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    AndroidBrowserRuntimeHost(modifier, start = { AndroidWebViewStartup.ensureStarted(context) }, content = content)
}

private enum class StartupState { Starting, Ready, Failed }

private val browserLog = AppLog.tagged("BrowserRuntimeHost")

@Composable
internal fun AndroidBrowserRuntimeHost(modifier: Modifier, start: () -> Unit, content: @Composable () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(StartupState.Starting) }
    LaunchedEffect(attempt) {
        state = try {
            start()
            StartupState.Ready
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            browserLog.w("Android WebView could not start", error)
            StartupState.Failed
        }
    }
    when (state) {
        StartupState.Starting -> Box(modifier)

        StartupState.Ready -> content()

        StartupState.Failed -> SharedEmptyState(
            icon = Icons.Rounded.Refresh,
            title = stringResource(Res.string.local_browser_error_title),
            description = stringResource(Res.string.android_browser_start_failed),
            modifier = modifier,
            primaryActionLabel = stringResource(Res.string.retry),
            onPrimaryAction = {
                state = StartupState.Starting
                attempt++
            },
        )
    }
}

@Composable
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val interceptor = rememberNavigationInterceptor(onNavigationRequest)
    val navigator = com.multiplatform.webview.web.rememberWebViewNavigator(requestInterceptor = interceptor)
    var generation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    if (state.errorsForCurrentRequest.any { it.code == BROWSER_RENDERER_GONE }) {
        // The renderer process died. Its view must leave the hierarchy; Retry builds a new one, which reloads the content.
        SharedEmptyState(
            icon = Icons.Rounded.Refresh,
            title = stringResource(Res.string.local_browser_error_title),
            description = stringResource(Res.string.browser_load_error_body),
            modifier = modifier,
            primaryActionLabel = stringResource(Res.string.retry),
            onPrimaryAction = {
                state.errorsForCurrentRequest.removeAll { it.code == BROWSER_RENDERER_GONE }
                generation++
            },
        )
        return
    }
    androidx.compose.runtime.key(generation) {
        com.multiplatform.webview.web.WebView(state = state, modifier = modifier, navigator = navigator, platformWebViewParams = rememberBrowserPlatformParams())
    }
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

            override fun onReceivedError(view: android.webkit.WebView, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                request?.takeIf { !it.isForMainFrame }?.let { WebViewCaptureHealth.of(view).resourceError(it.url.toString()) }
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(view: android.webkit.WebView, request: android.webkit.WebResourceRequest, response: android.webkit.WebResourceResponse) {
                super.onReceivedHttpError(view, request, response)
                if (!request.isForMainFrame) WebViewCaptureHealth.of(view).resourceError(request.url.toString(), httpStatus = response.statusCode)
                if (request.isForMainFrame) {
                    fail(request.url.toString(), com.multiplatform.webview.web.WebViewError(response.statusCode, response.reasonPhrase, true))
                }
            }

            override fun onReceivedSslError(view: android.webkit.WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                handler.cancel()
                // Match scheduled checks: reject the main document now, validate captured resources before saving.
                val tls = WebViewCaptureHealth.of(view).ssl(error.url, error.primaryError, (0..5).filter(error::hasError))
                if (tls.blocking) fail(error.url, tlsError())
            }

            override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                // Returning true keeps the app alive. This view is now unusable; the composable replaces it on Retry.
                pendingErrors.clear()
                view.doOnDetach {
                    // The Compose holder detaches first. Destroy the native view once it is out of its parent.
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        (view.parent as? android.view.ViewGroup)?.removeView(view)
                        view.destroy()
                    }
                }
                state.errorsForCurrentRequest.add(com.multiplatform.webview.web.WebViewError(BROWSER_RENDERER_GONE, "The browser renderer stopped", true))
                return true
            }
        },
    )
}
