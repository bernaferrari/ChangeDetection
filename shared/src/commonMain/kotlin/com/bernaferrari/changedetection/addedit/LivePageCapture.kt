package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.diagnostics.*
import com.bernaferrari.changedetection.browser.PageCleanup
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    onPreview: (CapturePreview) -> Unit = {},
): CapturePreview? = recordDiagnostic(mode, region.include.isNotBlank(), region.exclude.isNotBlank(), DiagnosticTrigger.INTERACTIVE_PREVIEW) {
    withTimeout(90_000) {
        // Source monitoring needs the original HTTP response, rather than the browser's modified DOM.
        if (mode == CaptureMode.SOURCE || mode == CaptureMode.DEFAULT) return@withTimeout null
        if (mode != CaptureMode.SCREENSHOT) {
            // Read the URL and DOM in the same JavaScript turn. Picker styles are restored even
            // if serialization fails or the caller is cancelled before the callback arrives.
            val raw = diagnosticStage(DiagnosticStage.EXTRACT_TEXT) { navigator.readScript(liveTextCaptureScript(region)) }
            return@withTimeout withContext(Dispatchers.Default) {
                val result = decodeLiveTextCapture(raw, url) ?: return@withContext null
                previewFromCapture(result, url, mode, region)
            }
        }
        navigator.readScript("window.__cdPick&&window.__cdPick.mark('','');window.__cdPick&&window.__cdPick.disable();0")
        try {
            val result = diagnosticStage(DiagnosticStage.CAPTURE_PIXELS) { captureLoadedPageScreenshot(state, region, onPreview) } ?: return@withTimeout null
            // Screenshot tiling spans multiple frames, so check navigation again after capture.
            val live = decodeLivePage(navigator.readScript("window.location.href")) ?: return@withTimeout null
            if (live.url != url) return@withTimeout null
            previewFromCapture(result, url, mode, region)
        } finally {
            navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.mark(${PageCleanup.js(region.include)},${PageCleanup.js(region.exclude)});0")
        }
    }
}

internal fun liveTextCaptureScript(region: TextRegion): String = """
    (function(){
        var picker=window.__cdPick;
        try {
            if(picker){picker.mark('','');picker.disable();}
            return JSON.stringify([window.location.href,document.documentElement.outerHTML]);
        } finally {
            if(picker)picker.mark(${PageCleanup.js(region.include)},${PageCleanup.js(region.exclude)});
        }
    })()
""".trimIndent()

/** Accept both Android's JSON-encoded callback and WebKit's raw string result. */
internal fun decodeLiveTextCapture(value: String, url: String): SyncEngine.FetchResult.Success? {
    require(value.length <= 12_000_000) { "The page is too large to preview" }
    val raw = runCatching { Json.decodeFromString<String>(value) }.getOrElse { value }
    val parts = runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull() ?: return null
    if (parts.size != 2 || decodeBrowserUrl(parts[0]) != url) return null
    val html = parts[1]
    require(html.isNotBlank()) { "The page returned no content" }
    return SyncEngine.FetchResult.Success("text/html", html.encodeToByteArray())
}

private suspend fun WebViewNavigator.readScript(script: String): String = suspendCancellableCoroutine { continuation ->
    evaluateJavaScript(script) { value -> if (continuation.isActive) continuation.resume(value) }
}

internal expect suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion, onPreview: (CapturePreview) -> Unit): SyncEngine.FetchResult.Success?

/** Platform factory for the live browser, including its reusable Android capture context. */
internal expect fun createWatchWebView(param: com.multiplatform.webview.web.WebViewFactoryParam): com.multiplatform.webview.web.NativeWebView
