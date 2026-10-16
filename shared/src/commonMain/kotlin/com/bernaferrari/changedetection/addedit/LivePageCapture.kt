package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/** Capture the loaded document; never navigate or make a second request for the page. */
internal suspend fun captureLivePagePreview(
    state: WebViewState,
    navigator: WebViewNavigator,
    url: String,
    mode: CaptureMode,
    region: TextRegion,
): CapturePreview? = withTimeout(90_000) {
    // Source monitoring needs the original HTTP response, rather than the browser's modified DOM.
    if (mode == CaptureMode.SOURCE || mode == CaptureMode.DEFAULT) return@withTimeout null
    navigator.readScript("window.__cdPick&&window.__cdPick.mark('','');window.__cdPick&&window.__cdPick.disable();0")
    try {
        val result = if (mode == CaptureMode.SCREENSHOT) {
            captureLoadedPageScreenshot(state, region) ?: return@withTimeout null
        } else {
            val raw = navigator.readScript("document.documentElement.outerHTML")
            require(raw.length <= 12_000_000) { "The page is too large to preview" }
            val html = runCatching { Json.decodeFromString<String>(raw) }.getOrElse { raw }
            require(html.isNotBlank()) { "The page returned no content" }
            SyncEngine.FetchResult.Success("text/html", html.encodeToByteArray())
        }
        // A redirect or navigation during capture must not bind another page's bytes to this URL.
        val live = decodeLivePage(navigator.readScript("window.location.href")) ?: return@withTimeout null
        if (live.url != url) return@withTimeout null
        previewFromCapture(result, url, mode, region)
    } finally {
        navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.mark(${com.bernaferrari.changedetection.browser.PageCleanup.js(region.include)},${com.bernaferrari.changedetection.browser.PageCleanup.js(region.exclude)});0")
    }
}

private suspend fun WebViewNavigator.readScript(script: String): String = suspendCancellableCoroutine { continuation ->
    evaluateJavaScript(script) { value -> if (continuation.isActive) continuation.resume(value) }
}

internal expect suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion): SyncEngine.FetchResult.Success?

/** Platform factory for the live browser, including its reusable Android capture context. */
internal expect fun createWatchWebView(param: com.multiplatform.webview.web.WebViewFactoryParam): com.multiplatform.webview.web.NativeWebView
