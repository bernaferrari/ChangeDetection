package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun BrowserRuntimeHost(modifier: Modifier, content: @Composable () -> Unit) = content()

@Composable
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val interceptor = rememberNavigationInterceptor(onNavigationRequest)
    val navigator = com.multiplatform.webview.web.rememberWebViewNavigator(requestInterceptor = interceptor)
    com.multiplatform.webview.web.WebView(state = state, modifier = modifier, navigator = navigator)
    androidx.compose.runtime.LaunchedEffect(state.loadingState, highlightUpdate) {
        if (state.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            highlightUpdate?.let { navigator.evaluateJavaScript(it.script()) }
        }
    }
}

internal actual val supportsPageRegionSelection: Boolean = true

@Composable
internal actual fun rememberBrowserPlatformParams(): com.multiplatform.webview.web.PlatformWebViewParams? = null
