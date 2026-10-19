package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Ensures a platform renderer is ready before creating browser UI. */
@Composable
internal expect fun BrowserRuntimeHost(modifier: Modifier = Modifier, content: @Composable () -> Unit)

@Composable
internal expect fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier = Modifier, onNavigationRequest: ((String) -> Boolean)? = null, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate? = null)

internal expect val supportsPageRegionSelection: Boolean

/** Adds platform navigation failures that the shared browser library does not surface. */
@Composable
internal expect fun rememberBrowserPlatformParams(): com.multiplatform.webview.web.PlatformWebViewParams?
