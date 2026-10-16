package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewState

internal actual suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion): SyncEngine.FetchResult.Success? = null

internal actual fun createWatchWebView(param: com.multiplatform.webview.web.WebViewFactoryParam): com.multiplatform.webview.web.NativeWebView =
    com.multiplatform.webview.web.defaultWebViewFactory(param)
