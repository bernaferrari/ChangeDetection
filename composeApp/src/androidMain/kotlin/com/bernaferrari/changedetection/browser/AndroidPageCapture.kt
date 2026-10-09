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
import kotlin.math.sqrt

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
        evaluate(PageCleanup.HIDE_CONSENT)
        evaluate(PageCleanup.HIDE_ADS)
        evaluate(PageCleanup.HIDE_BOTTOM_FLOATING)
        if (hide.isNotBlank()) evaluate(PageCleanup.hide(hide))
        awaitFrame()
        rendererFailure?.let { throw it }
        val clip = if (only.isNotBlank()) {
            evaluate(PageCleanup.rectOf(only)).trim('"').split(',').mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 6 }
                ?: throw IOException("The part you picked is no longer on the page")
        } else {
            null
        }
        val full = captureDocument(maxHeight)
        (if (clip == null) full else crop(full, clip)).copy(title = title)
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { result -> if (continuation.isActive) continuation.resumeWith(Result.success(result.orEmpty())) }
    }

    /** [clip] is left, top, right, bottom, documentHeight, documentWidth in CSS pixels. */
    private suspend fun crop(full: WebViewScreenshot, clip: List<Double>): WebViewScreenshot = withContext(Dispatchers.Default) {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(full.bytes, 0, full.bytes.size) ?: return@withContext full
        val factor = full.height / clip[4].coerceAtLeast(1.0)
        val sx = full.width / clip[5].coerceAtLeast(1.0)
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

    /** Tile only the visible viewport, with a hard allocation limit regardless of page height. */
    private suspend fun captureDocument(maxHeight: Int): WebViewScreenshot {
        @Suppress("DEPRECATION")
        val pageHeight = maxOf(view.height, (view.contentHeight * view.scale).roundToInt())
        require(pageHeight in 1..100_000) { "This page is too tall for a screenshot. Use page text monitoring." }
        val pageWidth = view.width.coerceAtLeast(1)
        val scale = minOf(1.0, maxHeight.toDouble() / pageHeight, sqrt(MAX_PIXELS.toDouble() / (pageWidth.toLong() * pageHeight)))
        val outputWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
        val outputHeight = (pageHeight * scale).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val tile = try {
            Bitmap.createBitmap(outputWidth, (view.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
        val canvas = Canvas(output)
        val oldScroll = view.scrollY
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        try {
            var y = 0
            while (y < pageHeight) {
                currentCoroutineContext().ensureActive()
                rendererFailure?.let { throw it }
                // View.scrollTo does not clamp to the document's scroll range. Chromium does,
                // so requesting y past the final viewport misaligns the canvas and omits the footer.
                val viewportTop = minOf(y, (pageHeight - view.height).coerceAtLeast(0))
                view.scrollTo(0, viewportTop)
                view.invalidate()
                awaitFrame()
                val offset = (y - view.scrollY).coerceAtLeast(0)
                val capturedHeight = minOf(view.height - offset, pageHeight - y)
                check(capturedHeight > 0) { "The page changed size while taking its screenshot. Try again." }
                tile.eraseColor(android.graphics.Color.WHITE)
                Canvas(tile).apply {
                    scale(scale.toFloat(), scale.toFloat())
                    translate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
                    view.draw(this)
                }
                canvas.drawBitmap(
                    tile,
                    Rect(0, (offset * scale).roundToInt(), outputWidth, ((offset + capturedHeight) * scale).roundToInt().coerceAtMost(tile.height)),
                    Rect(0, (y * scale).roundToInt(), outputWidth, ((y + capturedHeight) * scale).roundToInt().coerceAtMost(outputHeight)),
                    null,
                )
                if (y == 0) {
                    evaluate(PageCleanup.HIDE_PINNED)
                    awaitFrame()
                }
                y += capturedHeight
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
        const val MAX_PIXELS = 4_000_000L
    }
}
