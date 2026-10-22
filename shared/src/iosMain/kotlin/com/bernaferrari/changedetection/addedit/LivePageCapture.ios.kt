package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.browser.IosPageCapture
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewState

internal actual suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion, onPreview: (CapturePreview) -> Unit): SyncEngine.FetchResult.Success? {
    val bytes = IosPageCapture.captureLoaded(state.nativeWebView, hide = region.exclude, only = region.include)
    return SyncEngine.FetchResult.Success("image/png", bytes, capturedWithBrowser = true)
}

internal actual fun createWatchWebView(param: com.multiplatform.webview.web.WebViewFactoryParam): com.multiplatform.webview.web.NativeWebView = com.multiplatform.webview.web.defaultWebViewFactory(param)
