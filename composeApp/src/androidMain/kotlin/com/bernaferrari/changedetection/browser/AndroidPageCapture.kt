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
        // Run through the page once so lazy images start loading, then wait until it has really finished.
        evaluate("(function(){var h=Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);var y=0;var step=window.innerHeight;var t=setInterval(function(){y+=step;window.scrollTo(0,y);if(y>=h){clearInterval(t);window.scrollTo(0,0);}},120);return 0;})()")
        delay(1_500)
        PageCleanup.awaitSettled({ script -> evaluate(script) })
        evaluate("window.scrollTo(0,0);0")
        evaluate(PageCleanup.MASK_RELATIVE_TIMES)
        awaitFrame()
        rendererFailure?.let { throw it }
        val clip = if (only.isNotBlank()) {
            evaluate(PageCleanup.rectOf(only)).trim('"').split(',').mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 6 }
                ?: throw IOException("The part you picked is no longer on the page")
        } else {
            null
        }
        val full = captureDocument(maxHeight)
        (if (clip == null) full else crop(full, clip)).copy(title = title, icon = icon)
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { result -> if (continuation.isActive) continuation.resumeWith(Result.success(result.orEmpty())) }
    }

    /** [clip] is left, top, right, bottom, documentHeight, documentWidth in CSS pixels. */
    private suspend fun crop(full: WebViewScreenshot, clip: List<Double>): WebViewScreenshot = withContext(Dispatchers.Default) {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(full.bytes, 0, full.bytes.size) ?: return@withContext full
        // Output is exactly CSS_TO_OUTPUT pixels per CSS pixel (and may be cut short on very long pages).
        val factor = CSS_TO_OUTPUT.toDouble()
        val sx = CSS_TO_OUTPUT.toDouble()
        val left = (clip[0] * sx).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (clip[1] * factor).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (clip[2] * sx).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (clip[3] * factor).roundToInt().coerceIn(top + 1, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val bytes = ByteArrayOutputStream().use { stream ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()
        WebViewScreenshot("image/png", bytes, right - left, bottom - top)
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
            view.loadUrl(url)
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
    private suspend fun captureDocument(maxHeight: Int): WebViewScreenshot {
        @Suppress("DEPRECATION")
        val pageHeight = maxOf(view.height, (view.contentHeight * view.scale).roundToInt())
        require(pageHeight in 1..100_000) { "This page is too tall for a screenshot. Use page text monitoring." }
        val density = view.resources.displayMetrics.density.coerceAtLeast(0.5f)
        val scale = CSS_TO_OUTPUT / density
        // Device pixels covering 8 CSS pixels; a whole number for every common density (2.625 → 21).
        val unit = (density * 8).roundToInt().coerceAtLeast(1)
        val pageWidth = view.width.coerceAtLeast(1)
        val outputWidth = (pageWidth * scale).roundToInt().coerceAtLeast(1)
        val budgetRows = minOf(maxHeight.toLong(), MAX_PIXELS / outputWidth).toInt()
        val capturedPageHeight = minOf(pageHeight, (budgetRows / scale).toInt() / unit * unit).coerceAtLeast(minOf(pageHeight, unit))
        val outputHeight = (capturedPageHeight * scale).roundToInt().coerceAtLeast(1)
        val step = (view.height / unit * unit).coerceAtLeast(unit)
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        output.eraseColor(android.graphics.Color.WHITE)
        val tile = try {
            Bitmap.createBitmap(outputWidth, (view.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
        val canvas = Canvas(output)
        val oldScroll = view.scrollY
        // The last aligned position that still shows a full viewport; Chromium clamps anything further.
        val lastTop = ((pageHeight - view.height).coerceAtLeast(0) / unit) * unit
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        try {
            var y = 0
            while (y < capturedPageHeight) {
                currentCoroutineContext().ensureActive()
                rendererFailure?.let { throw it }
                val viewportTop = minOf(y, lastTop)
                view.scrollTo(0, viewportTop)
                view.invalidate()
                awaitFrame()
                check(view.scrollY == viewportTop || viewportTop == 0) { "The page changed size while taking its screenshot. Try again." }
                val offset = y - viewportTop
                val capturedHeight = minOf(view.height - offset, capturedPageHeight - y)
                check(capturedHeight > 0) { "The page changed size while taking its screenshot. Try again." }
                tile.eraseColor(android.graphics.Color.WHITE)
                Canvas(tile).apply {
                    scale(scale, scale)
                    translate(-view.scrollX.toFloat(), -viewportTop.toFloat())
                    view.draw(this)
                }
                val srcTop = (offset * scale).roundToInt()
                val dstTop = (y * scale).roundToInt()
                val rows = minOf((capturedHeight * scale).roundToInt(), tile.height - srcTop, outputHeight - dstTop)
                if (rows > 0) canvas.drawBitmap(tile, Rect(0, srcTop, outputWidth, srcTop + rows), Rect(0, dstTop, outputWidth, dstTop + rows), null)
                if (y == 0) {
                    evaluate(PageCleanup.HIDE_PINNED)
                    awaitFrame()
                }
                if (viewportTop == lastTop) break
                y += minOf(step, capturedHeight)
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
                view.scrollTo(0, oldScroll)
            } finally {
                tile.recycle()
                output.recycle()
            }
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
