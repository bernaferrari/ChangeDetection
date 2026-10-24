package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import android.net.http.SslError
import android.view.Choreographer
import android.webkit.*
import com.bernaferrari.kmpwright.core.Rectangle
import com.bernaferrari.kmpwright.core.captureWebViewDocument
import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** The app needs a cancellable page capture, not a second browser automation runtime. */
internal class AndroidPageCapture(private val view: WebView) {
    private val health = WebViewCaptureHealth.of(view)
    fun captureValidator(): suspend () -> Unit {
        val epoch = health.epoch()
        return { health.validate(epoch) }
    }
    private var rendererFailure: IOException? = null
    private var navigationFailure: Exception? = null

    /**
     * @param hide CSS selectors of parts to ignore; made invisible (layout kept) before capture.
     * @param only CSS selector of the single part to watch; the capture is cropped to it.
     */
    suspend fun capture(url: String, settleDelayMs: Long, maxHeight: Int, hide: String = "", only: String = ""): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        diagnosticMetric(DiagnosticStage.CREATE_BROWSER, DiagnosticMetric.ATTACHED, if (view.isAttachedToWindow) 1 else 0)
        diagnosticStage(DiagnosticStage.NAVIGATE) { navigate(url) }
        diagnosticStage(DiagnosticStage.SETTLE) { delay(settleDelayMs.coerceIn(0, 30_000)) }
        diagnosticStage(DiagnosticStage.CAPTURE_PIXELS) { captureLoaded(maxHeight, hide, only) }
    }

    /** Text watches must read the rendered DOM, just like the initial live preview.
     * Keep screenshot cleanup out of this path: it mutates the content being watched.
     */
    suspend fun captureText(url: String, settleDelayMs: Long, only: String = ""): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        diagnosticMetric(DiagnosticStage.CREATE_BROWSER, DiagnosticMetric.ATTACHED, if (view.isAttachedToWindow) 1 else 0)
        diagnosticStage(DiagnosticStage.NAVIGATE) { navigate(url) }
        diagnosticStage(DiagnosticStage.SETTLE) { delay(settleDelayMs.coerceIn(0, 30_000)) }
        // A configured delay may be zero while the selected node is still arriving.
        // Give missing content a bounded grace period, then let normal region validation
        // report a genuinely removed selection. Never fall back to the whole page.
        kotlinx.coroutines.withTimeoutOrNull(2_000) {
            val ready = """(function(){try{var e=${if (only.isBlank()) "document.body" else "document.querySelector(${PageCleanup.js(only)})"};return !!e && !!e.innerText.trim();}catch(e){return true;}})()"""
            while (evaluate(ready) != "true") {
                navigationFailure?.let { throw it }
                rendererFailure?.let { throw it }
                delay(100)
            }
        }
        navigationFailure?.let { throw it }
        rendererFailure?.let { throw it }
        val epoch = health.epoch()
        health.validate(epoch)
        val raw = diagnosticStage(DiagnosticStage.EXTRACT_TEXT) { evaluate("document.documentElement.outerHTML") }
        require(raw.length <= 12_000_000) { "The page is too large to preview" }
        val html = kotlinx.serialization.json.Json.decodeFromString<String>(raw)
        withContext(Dispatchers.Default) {
            require(com.bernaferrari.changedetection.util.readableSnapshotText(html, true).isNotBlank()) { "The page returned no readable text" }
        }
        health.validate(epoch)
        WebViewScreenshot("text/html", html.encodeToByteArray(), 0, 0)
    }

    /** Uses the already-loaded document, with the same cleanup and tiling as scheduled checks. */
    suspend fun captureLoaded(maxHeight: Int, hide: String = "", only: String = "", preservePage: Boolean = false): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        val epoch = health.epoch()
        health.validate(epoch)
        suspend fun clean(script: String) {
            evaluate(if (preservePage) PageCleanup.reversible(script) else script)
        }
        try {
            val title = evaluate(PageCleanup.TITLE).trim('"').takeIf { it.isNotBlank() && it != "null" }
            val icon = evaluate(PageCleanup.ICON).trim('"').takeIf { it.startsWith("http") }
            // One renderer round trip; reversible() still records all mutations in order.
            clean(
                "(function(){" + buildList {
                    add(PageCleanup.HIDE_CONSENT)
                    add(PageCleanup.HIDE_ADS)
                    add(PageCleanup.HIDE_BOTTOM_FLOATING)
                    if (hide.isNotBlank()) add(PageCleanup.hide(hide))
                }.joinToString(";") + ";return 0;})()",
            )
            // Use the same limits as the tiler. Loading content below its final row cannot
            // improve the capture, and may keep an otherwise ready page waiting indefinitely.
            @Suppress("DEPRECATION")
            val cssWidth = view.width / view.scale.toDouble()
            val captureHeight = minOf(maxHeight.toLong(), MAX_PIXELS / (cssWidth * PIXEL_SCALE).roundToInt().coerceAtLeast(1)) / PIXEL_SCALE
            suspend fun captureRange(): Pair<Double, Double> {
                val rect = if (only.isNotBlank()) readClip(only) else null
                return if (rect == null) 0.0 to captureHeight else rect[1] to rect[3]
            }
            if (preservePage) {
                var range = captureRange()
                evaluate("window.scrollTo({left:0,top:${range.first},behavior:'instant'});0")
                for (step in 0 until 12) {
                    currentCoroutineContext().ensureActive()
                    val bounds = evaluate("[window.scrollY,window.innerHeight,Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)].join(',')")
                        .trim('"').split(',').mapNotNull { it.toDoubleOrNull() }
                    range = captureRange()
                    if (bounds.size != 3 || bounds[1] <= 0 || bounds[0] + bounds[1] >= minOf(bounds[2], range.second) - 1) break
                    evaluate("window.scrollTo({left:0,top:${bounds[0] + bounds[1]},behavior:'instant'});0")
                    // Let Chromium paint and activate lazy resources; no fixed pause per screen.
                    awaitVisualState()
                }
            } else {
                val viewport = evaluate("window.innerHeight").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
                repeat(12) { step ->
                    currentCoroutineContext().ensureActive()
                    evaluate("window.scrollTo({left:0,top:${(step + 1) * viewport},behavior:'instant'});0")
                    delay(120)
                }
            }
            evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
            if (only.isNotBlank()) evaluate("(function(){var e=document.querySelector(${PageCleanup.js(only)});if(e)e.scrollIntoView();return 0;})()")
            val loadStateScript = if (preservePage) {
                val range = captureRange()
                PageCleanup.loadState(range.first, range.second)
            } else {
                PageCleanup.LOAD_STATE
            }
            PageCleanup.awaitSettled(
                { script -> evaluate(script) },
                maxMs = if (preservePage) 2_000 else 8_000,
                pollMs = if (preservePage) 100 else 500,
                loadStateScript = loadStateScript,
            )
            evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
            clean(PageCleanup.MASK_RELATIVE_TIMES)
            awaitFrame()
            navigationFailure?.let { throw it }
            rendererFailure?.let { throw it }
            val clip = if (only.isNotBlank()) {
                readClip(only)
            } else {
                null
            }
            if (clip == null) {
                @Suppress("DEPRECATION")
                val pageHeight = view.contentHeight.toDouble() * view.scale
                require(pageHeight <= 100_000) { "This page is too tall for a screenshot. Use page text monitoring." }
            }
            val region = clip?.let { Rectangle(it[0], it[1], it[2] - it[0], it[3] - it[1]) }
            val shot = captureWebViewDocument(
                view,
                clip = region,
                pixelsPerCssPixel = PIXEL_SCALE,
                maxPixels = MAX_PIXELS,
                maxHeight = maxHeight,
                truncateFullPage = true,
                viewportWidthOnly = true,

                afterFirstTile = { clean(PageCleanup.HIDE_PINNED) },
            )
            health.validate(epoch)
            WebViewScreenshot("image/png", shot.bytes, shot.width, shot.height, title, icon)
        } finally {
            if (preservePage) {
                withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.withTimeoutOrNull(2_000) { evaluate(PageCleanup.RESTORE_CAPTURE) }
                }
            }
        }
    }

    private suspend fun readClip(selector: String): List<Double> = evaluate(PageCleanup.rectOf(selector)).trim('"').split(',').mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 4 }
        ?: throw IOException("The part you picked is no longer on the page")

    private suspend fun awaitVisualState() = withTimeout(2_000) {
        suspendCancellableCoroutine<Unit> { continuation ->
            view.postVisualStateCallback(
                0,
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                },
            )
        }
    }

    private companion object {
        const val PIXEL_SCALE = 2.0
        const val MAX_PIXELS = 7_000_000L
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { result ->
            if (result == null || result == "null") continuation.context[DiagnosticSpan]?.callback(DiagnosticStage.EXTRACT_TEXT, DiagnosticMetric.CALLBACK_NULL, 1)
            if (continuation.isActive) continuation.resumeWith(Result.success(result.orEmpty()))
        }
    }

    private suspend fun navigate(url: String) = withTimeout(30_000) {
        val trace = currentCoroutineContext()[DiagnosticSpan]
        val loaded = CompletableDeferred<Unit>()
        navigationFailure = null
        health.begin(url)
        fun fail(error: Exception) {
            // Readiness can complete before a redirect or HTTP error arrives. Keep the
            // error for the capture as well as the initial navigation awaiter.
            navigationFailure = error
            loaded.completeExceptionally(error)
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? {
                health.request(request.url.toString(), request.isForMainFrame)
                return null
            }
            override fun onPageStarted(webView: WebView, url: String?, favicon: android.graphics.Bitmap?) { health.started(url) }
            override fun onPageCommitVisible(webView: WebView, url: String?) { health.committed(url) }
            override fun onPageFinished(webView: WebView, finishedUrl: String) {
                health.finished()
                if (finishedUrl != "about:blank") {
                    trace?.callback(DiagnosticStage.DOM_READY, DiagnosticMetric.DOM_READY, 1)
                    loaded.complete(Unit)
                }
            }

            override fun onReceivedError(webView: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.WEBVIEW_ERROR, error.errorCode)
                    fail(IOException(error.description.toString()))
                }
            }

            override fun onReceivedHttpError(webView: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) {
                    trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, response.statusCode)
                    fail(com.bernaferrari.changedetection.error.NetworkError(response.statusCode, "HTTP ${response.statusCode}"))
                }
            }

            override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                trace?.tls(health.ssl(error.url, error.primaryError, (0..5).filter(error::hasError)))
                fail(SSLException("The page has an invalid TLS certificate"))
            }

            override fun onRenderProcessGone(webView: WebView, detail: RenderProcessGoneDetail): Boolean {
                trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.RENDERER_CRASH, if (detail.didCrash()) 1 else 0)
                val error = IOException("The browser renderer stopped. Try the page again.")
                rendererFailure = error
                loaded.completeExceptionally(error)
                return true
            }
        }
        try {
            // A check must fetch today's page even when Chromium considers its cached response fresh.
            view.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            view.loadUrl(url)
            // Slow images and analytics can hold onPageFinished forever after the document is usable.
            // The capture's settle delay and awaitSettled still give styles and lazy images time to arrive.
            while (!loaded.isCompleted) {
                val ready = evaluate("location.href !== 'about:blank' && document.body !== null && document.readyState !== 'loading'")
                if (ready == "true") loaded.complete(Unit) else delay(100)
            }
            loaded.await()
        } finally {
            if (!loaded.isCompleted) view.stopLoading()
        }
    }

    private suspend fun awaitFrame() = suspendCancellableCoroutine { continuation ->
        val choreographer = Choreographer.getInstance()
        val callback = Choreographer.FrameCallback { if (continuation.isActive) continuation.resume(Unit) }
        choreographer.postFrameCallback(callback)
        continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }
}
