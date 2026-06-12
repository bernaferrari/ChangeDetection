/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.webwright.core.AndroidScreenshotController
import com.webwright.core.Viewport
import com.webwright.core.WebwrightConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

internal suspend fun WebView.captureFullPagePng(
    maxHeightPixels: Int,
): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
    ensureLaidOut()

    val controller = AndroidScreenshotController(
        webView = this@captureFullPagePng,
        config = WebwrightConfig(
            viewport = Viewport(
                width = width.takeIf { it > 0 } ?: DEFAULT_VIEWPORT_WIDTH,
                height = height.takeIf { it > 0 } ?: DEFAULT_VIEWPORT_HEIGHT,
                isMobile = true,
                hasTouch = true,
            ),
        ),
    )
    val pageDimensions = controller.getPageDimensions()
    val scale = if (pageDimensions.contentHeight > maxHeightPixels) {
        (maxHeightPixels.toDouble() / pageDimensions.contentHeight.toDouble()).coerceIn(MIN_SCREENSHOT_SCALE, 1.0)
    } else {
        1.0
    }
    val screenshot = controller.captureFullPage(scale)

    WebViewScreenshot(
        contentType = "image/png",
        bytes = screenshot.bytes,
        width = screenshot.width,
        height = screenshot.height,
    )
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

private const val DEFAULT_VIEWPORT_WIDTH = 1080
private const val DEFAULT_VIEWPORT_HEIGHT = 1920
private const val MIN_SCREENSHOT_SCALE = 0.1
