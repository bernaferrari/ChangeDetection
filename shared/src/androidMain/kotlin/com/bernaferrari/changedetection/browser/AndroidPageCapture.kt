package com.bernaferrari.changedetection.browser

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

/** The app needs a cancellable page capture, not a second browser automation runtime. */
internal class AndroidPageCapture(private val view: WebView) {
    private var rendererFailure: IOException? = null

    /**
     * @param hide CSS selectors of parts to ignore; made invisible (layout kept) before capture.
     * @param only CSS selector of the single part to watch; the capture is cropped to it.
     */
    suspend fun capture(url: String, settleDelayMs: Long, maxHeight: Int, hide: String = "", only: String = ""): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        navigate(url)
        delay(settleDelayMs.coerceIn(0, 30_000))
        val title = evaluate(PageCleanup.TITLE).trim('"').takeIf { it.isNotBlank() && it != "null" }
        val icon = evaluate(PageCleanup.ICON).trim('"').takeIf { it.startsWith("http") }
        evaluate(PageCleanup.HIDE_CONSENT)
        evaluate(PageCleanup.HIDE_ADS)
        evaluate(PageCleanup.HIDE_BOTTOM_FLOATING)
        if (hide.isNotBlank()) evaluate(PageCleanup.hide(hide))
        // Await a bounded traversal on the capture coroutine. No page timer may keep scrolling while tiles draw.
        val viewport = evaluate("window.innerHeight").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0
        repeat(12) { step ->
            currentCoroutineContext().ensureActive()
            evaluate("window.scrollTo({left:0,top:${(step + 1) * viewport},behavior:'instant'});0")
            delay(120)
        }
        evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
        if (only.isNotBlank()) evaluate("(function(){var e=document.querySelector(${PageCleanup.js(only)});if(e)e.scrollIntoView();return 0;})()")
        PageCleanup.awaitSettled({ script -> evaluate(script) })
        evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
        evaluate(PageCleanup.MASK_RELATIVE_TIMES)
        awaitFrame()
        rendererFailure?.let { throw it }
        val clip = if (only.isNotBlank()) {
            evaluate(PageCleanup.rectOf(only)).trim('"').split(',').mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 4 }
                ?: throw IOException("The part you picked is no longer on the page")
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
            pixelsPerCssPixel = 2.0,
            maxPixels = 7_000_000,
            maxHeight = maxHeight,
            truncateFullPage = true,
            viewportWidthOnly = true,

            afterFirstTile = { evaluate(PageCleanup.HIDE_PINNED) },
        )
        WebViewScreenshot("image/png", shot.bytes, shot.width, shot.height, title, icon)
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { result -> if (continuation.isActive) continuation.resumeWith(Result.success(result.orEmpty())) }
    }

    private suspend fun navigate(url: String) = withTimeout(30_000) {
        val loaded = CompletableDeferred<Unit>()
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(webView: WebView, finishedUrl: String) {
                if (finishedUrl != "about:blank") loaded.complete(Unit)
            }

            override fun onReceivedError(webView: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) loaded.completeExceptionally(IOException(error.description.toString()))
            }

            override fun onReceivedHttpError(webView: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) loaded.completeExceptionally(IOException("HTTP ${response.statusCode}: ${response.reasonPhrase}"))
            }

            override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                loaded.completeExceptionally(SSLException("The page has an invalid TLS certificate"))
            }

            override fun onRenderProcessGone(webView: WebView, detail: RenderProcessGoneDetail): Boolean {
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
