/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.http.SslError
import android.view.Choreographer
import android.webkit.*
import java.io.ByteArrayOutputStream
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
            evaluate(PageCleanup.rectOf(only)).trim('"').split(',').mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 6 }
                ?: throw IOException("The part you picked is no longer on the page")
        } else {
            null
        }
        captureDocument(maxHeight, clip).copy(title = title, icon = icon)
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

    /**
     * Tile only the visible viewport, with a hard allocation limit regardless of page height.
     *
     * The output is exactly [CSS_TO_OUTPUT] pixels per CSS pixel, and every tile starts on a whole output row.
     * Content that moves down by N CSS pixels then lands exactly 2N rows lower, pixel for pixel, so the
     * comparison sees it moved instead of re-rendered. (A fractional scale blurs moved content differently
     * and makes everything below a new banner look changed.) Very long pages are cut, never squeezed.
     */
    private suspend fun captureDocument(maxHeight: Int, clip: List<Double>?): WebViewScreenshot {
        @Suppress("DEPRECATION")
        val cssToView = view.scale.coerceAtLeast(0.01f)
        val pageHeight = maxOf(view.height, (maxOf(view.contentHeight.toDouble(), clip?.getOrNull(4) ?: 0.0) * cssToView).roundToInt())
        require(pageHeight > 0 && (clip != null || pageHeight <= 100_000)) { "This page is too tall for a screenshot. Use page text monitoring." }
        // WebView's scale includes overview zoom. Device density alone is insufficient on wide-layout pages.
        val scale = CSS_TO_OUTPUT / cssToView
        val unit = (cssToView * 8).roundToInt().coerceAtLeast(1)
        var bounds = if (clip == null) {
            CaptureBounds(0, 0, view.width.coerceAtLeast(1), pageHeight)
        } else {
            // CSS layout rounds viewport edges independently of native pixels (at most one CSS pixel).
            val rounding = kotlin.math.ceil(cssToView.toDouble()).toInt()
            selectedCaptureBounds(clip, cssToView.toDouble(), view.width + rounding, pageHeight + rounding)
                .let { it.copy(right = it.right.coerceAtMost(view.width), bottom = it.bottom.coerceAtMost(pageHeight)) }
        }
        require(bounds.right > bounds.left && bounds.bottom > bounds.top) { "The selected area is outside the screenshot." }
        val outputWidth = ((bounds.right - bounds.left) * scale).roundToInt().coerceAtLeast(1)
        val budgetRows = minOf(maxHeight.toLong(), MAX_PIXELS / outputWidth).toInt()
        require(budgetRows > 0) { "The selected area is too wide for a screenshot." }
        if (clip != null && (bounds.bottom - bounds.top) * scale > budgetRows) throw IOException("The selected area is too large for a screenshot. Choose a smaller part.")
        val capturedHeight = if (clip != null) bounds.bottom - bounds.top else minOf(pageHeight, (budgetRows / scale).toInt() / unit * unit).coerceAtLeast(1)
        val outputHeight = (capturedHeight * scale).roundToInt().coerceAtLeast(1)
        require(outputWidth.toLong() * outputHeight <= MAX_PIXELS && outputHeight <= maxHeight) { "The screenshot exceeds the capture budget." }
        val step = (view.height / unit * unit).coerceAtLeast(1)
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        output.eraseColor(android.graphics.Color.WHITE)
        val tile = try {
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
        val canvas = Canvas(output)
        val oldScroll = view.scrollY
        val lastTop = (pageHeight - view.height).coerceAtLeast(0)
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        try {
            if (clip != null) {
                // Put the selected rectangle in the drawable viewport without reflowing its layout.
                // A detached WebView may omit far-offscreen content from its software display list.
                evaluate("(function(){var e=document.documentElement;window.__cdCaptureTransform=[e.style.getPropertyValue('transform'),e.style.getPropertyPriority('transform')];var t=getComputedStyle(e).transform;e.style.setProperty('transform','translate(${-clip[0]}px,${-clip[1]}px) '+(t==='none'?'':t),'important');window.scrollTo({left:0,top:0,behavior:'instant'});return 0;})()")
                awaitFrame()
                awaitFrame()
                bounds = CaptureBounds(0, 0, bounds.right - bounds.left, bounds.bottom - bounds.top)
            }
            var y = bounds.top
            val capturedEnd = bounds.top + capturedHeight
            while (y < capturedEnd) {
                currentCoroutineContext().ensureActive()
                rendererFailure?.let { throw it }
                val viewportTop = minOf(y, lastTop)
                evaluate("window.scrollTo({left:0,top:${viewportTop / cssToView},behavior:'instant'});0")
                awaitFrame()
                awaitFrame()
                view.invalidate()
                awaitVisualState()
                awaitFrame()
                // Chromium can deliver an older scroll callback while frames settle. Set the final native
                // position only now; do not suspend between positioning, verifying and drawing this tile.
                view.scrollTo(0, viewportTop)
                check(view.scrollY == viewportTop) { "The page changed size while taking its screenshot. Try again." }
                val offset = y - viewportTop
                val rowsInView = minOf(view.height - offset, capturedEnd - y)
                check(rowsInView > 0) { "The page changed size while taking its screenshot. Try again." }
                tile.eraseColor(android.graphics.Color.WHITE)
                Canvas(tile).apply {
                    translate(0f, -viewportTop.toFloat())
                    view.draw(this)
                }
                val dstTop = ((y - bounds.top) * scale).roundToInt()
                val rows = minOf((rowsInView * scale).roundToInt(), outputHeight - dstTop)
                if (rows > 0) canvas.drawBitmap(tile, Rect(bounds.left, offset, bounds.right, offset + rowsInView), Rect(0, dstTop, outputWidth, dstTop + rows), null)
                if (y == bounds.top) {
                    evaluate(PageCleanup.HIDE_PINNED)
                    awaitFrame()
                }
                if (viewportTop == lastTop) break
                y += minOf(step, rowsInView)
            }
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { stream ->
                    check(output.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Could not encode the page screenshot" }
                    stream.toByteArray()
                }
            }
            return WebViewScreenshot("image/png", bytes, outputWidth, outputHeight)
        } finally {
            // A renderer failure can also make view cleanup throw. Always release native bitmaps.
            try {
                if (clip != null) evaluate("(function(){var s=window.__cdCaptureTransform;if(s)document.documentElement.style.setProperty('transform',s[0],s[1]);delete window.__cdCaptureTransform;return 0;})()")
                view.scrollTo(0, oldScroll)
            } finally {
                tile.recycle()
                output.recycle()
            }
        }
    }

    /** Choreographer frames alone do not establish that Chromium's asynchronous DOM updates are drawable. */
    private suspend fun awaitVisualState() = withTimeout(5_000) {
        suspendCancellableCoroutine { continuation ->
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

    private suspend fun awaitFrame() = suspendCancellableCoroutine { continuation ->
        val choreographer = Choreographer.getInstance()
        val callback = Choreographer.FrameCallback { if (continuation.isActive) continuation.resume(Unit) }
        choreographer.postFrameCallback(callback)
        continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }

    private companion object {
        const val MAX_PIXELS = 7_000_000L

        /** Output pixels per CSS pixel: sharp text, and a whole number so moved content stays pixel-identical. */
        const val CSS_TO_OUTPUT = 2f
    }
}
