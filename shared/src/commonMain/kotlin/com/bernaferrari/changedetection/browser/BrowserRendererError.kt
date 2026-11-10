package com.bernaferrari.changedetection.browser

/**
 * Error code reported in `WebViewState.errorsForCurrentRequest` when the platform renderer process died.
 * The browser view that reported it is unusable and must be replaced, not reloaded.
 */
internal const val BROWSER_RENDERER_GONE = -1000
