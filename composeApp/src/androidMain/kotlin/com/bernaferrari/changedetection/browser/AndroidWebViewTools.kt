/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class WebViewScreenshot(
    val contentType: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

internal fun WebView.configureChangeDetectionWebView(
    enableZoom: Boolean = false,
    overviewMode: Boolean = true,
    acceptThirdPartyCookies: Boolean = false,
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        loadWithOverviewMode = overviewMode
        useWideViewPort = overviewMode
        setSupportZoom(enableZoom)
        builtInZoomControls = enableZoom
        displayZoomControls = false
    }

    if (acceptThirdPartyCookies) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@configureChangeDetectionWebView, true)
        }
    }
}

internal fun WebView.destroySafely(
    vararg javaScriptInterfaces: String,
    clearCache: Boolean = true,
) {
    try {
        stopLoading()
        javaScriptInterfaces.forEach(::removeJavascriptInterface)
        clearHistory()
        if (clearCache) {
            clearCache(true)
        }
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        removeAllViews()
        destroy()
    } catch (e: Exception) {
        Napier.e("Failed to destroy WebView", e)
    }
}

internal fun WebView.evaluateJavascriptSafely(
    script: String,
    callback: ((String?) -> Unit)? = null,
) {
    try {
        evaluateJavascript(script, callback)
    } catch (e: Exception) {
        Napier.e("Failed to evaluate JavaScript", e)
        callback?.invoke(null)
    }
}

internal fun readCookiesForUrl(url: String): String = CookieManager.getInstance().getCookie(url).orEmpty()

internal suspend fun WebView.captureFullPageWebp(
    maxHeightPixels: Int,
    quality: Int = 80,
): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
    ensureLaidOut()

    val pageSize = queryPageSize()
    val captureWidth = width.takeIf { it > 0 } ?: pageSize.width
    val captureHeight = pageSize.height.coerceAtMost(maxHeightPixels)

    if (captureWidth <= 0 || captureHeight <= 0) {
        error("Invalid WebView dimensions: ${captureWidth}x$captureHeight")
    }

    var bitmap: Bitmap? = null
    try {
        val captureBitmap = Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.RGB_565)
        bitmap = captureBitmap
        drawAtSize(captureWidth, captureHeight, Canvas(captureBitmap))

        val bytes = withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { stream ->
                @Suppress("DEPRECATION")
                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                captureBitmap.compress(format, quality, stream)
                stream.toByteArray()
            }
        }

        WebViewScreenshot(
            contentType = "image/webp",
            bytes = bytes,
            width = captureWidth,
            height = captureHeight,
        )
    } finally {
        bitmap?.recycle()
    }
}

private fun WebView.drawAtSize(width: Int, height: Int, canvas: Canvas) {
    val originalLeft = left
    val originalTop = top
    val originalRight = right
    val originalBottom = bottom
    val shouldRelayout = this.width != width || this.height != height

    try {
        if (shouldRelayout) {
            measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY),
            )
            layout(originalLeft, originalTop, originalLeft + width, originalTop + height)
        }
        draw(canvas)
    } finally {
        if (shouldRelayout) {
            layout(originalLeft, originalTop, originalRight, originalBottom)
        }
    }
}

private fun WebView.ensureLaidOut() {
    if (width > 0 && height > 0) return

    val viewportWidth = layoutParams?.width?.takeIf { it > 0 }
        ?: measuredWidth.takeIf { it > 0 }
        ?: DEFAULT_VIEWPORT_WIDTH
    val viewportHeight = layoutParams?.height?.takeIf { it > 0 }
        ?: measuredHeight.takeIf { it > 0 }
        ?: DEFAULT_VIEWPORT_HEIGHT

    measure(
        android.view.View.MeasureSpec.makeMeasureSpec(viewportWidth, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(viewportHeight, android.view.View.MeasureSpec.EXACTLY),
    )
    layout(0, 0, viewportWidth, viewportHeight)
}

private suspend fun WebView.queryPageSize(): PageSize {
    val fallback = fallbackPageSize()
    val raw = evaluateJavaScriptForResult(PAGE_SIZE_SCRIPT) ?: return fallback

    return try {
        val parsed = JSONObject(raw)
        val scrollWidth = parsed.optDouble("scrollWidth", fallback.width.toDouble())
        val scrollHeight = parsed.optDouble("scrollHeight", fallback.height.toDouble())
        val innerWidth = parsed.optDouble("innerWidth", width.toDouble())
        val widthScale = if (innerWidth > 0.0 && width > 0) {
            width.toDouble() / innerWidth
        } else {
            webViewScale()
        }

        PageSize(
            width = ceil(scrollWidth * widthScale)
                .toInt()
                .coerceAtLeast(fallback.width),
            height = ceil(scrollHeight * widthScale)
                .toInt()
                .coerceAtLeast(fallback.height),
        )
    } catch (_: Exception) {
        fallback
    }
}

private fun WebView.fallbackPageSize(): PageSize {
    val scale = webViewScale()
    val contentHeightPx = (contentHeight * scale).roundToInt()
    return PageSize(
        width = width.takeIf { it > 0 } ?: measuredWidth,
        height = maxOf(contentHeightPx, height, measuredHeight),
    )
}

private fun WebView.webViewScale(): Double {
    @Suppress("DEPRECATION")
    return scale.takeIf { it > 0f }?.toDouble() ?: resources.displayMetrics.density.toDouble()
}

private suspend fun WebView.evaluateJavaScriptForResult(script: String): String? = suspendCancellableCoroutine { continuation ->
    evaluateJavascriptSafely(script) { result ->
        if (continuation.isActive) {
            continuation.resume(normalizeJavaScriptResult(result))
        }
    }
}

private fun normalizeJavaScriptResult(result: String?): String? {
    val trimmed = result?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed == "null") return null

    return if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        try {
            (JSONTokener(trimmed).nextValue() as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    } else {
        trimmed
    }
}

private data class PageSize(
    val width: Int,
    val height: Int,
)

private const val DEFAULT_VIEWPORT_WIDTH = 1080
private const val DEFAULT_VIEWPORT_HEIGHT = 1920

private const val PAGE_SIZE_SCRIPT = """
    (function() {
        var doc = document.documentElement || {};
        var body = document.body || {};
        return {
            scrollWidth: Math.max(
                body.scrollWidth || 0,
                doc.scrollWidth || 0,
                body.offsetWidth || 0,
                doc.offsetWidth || 0,
                doc.clientWidth || 0
            ),
            scrollHeight: Math.max(
                body.scrollHeight || 0,
                doc.scrollHeight || 0,
                body.offsetHeight || 0,
                doc.offsetHeight || 0,
                doc.clientHeight || 0
            ),
            innerWidth: window.innerWidth || doc.clientWidth || 0
        };
    })()
"""
