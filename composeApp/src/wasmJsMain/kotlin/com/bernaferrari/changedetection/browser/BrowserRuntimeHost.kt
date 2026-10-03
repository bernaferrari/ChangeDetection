/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
}

internal actual val supportsPageRegionSelection: Boolean = false
