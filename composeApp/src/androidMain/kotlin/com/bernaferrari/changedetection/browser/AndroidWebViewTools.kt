/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.webwright.core.AndroidPlatformContext
import com.webwright.core.AndroidScreenshotController
import com.webwright.core.Browser
import com.webwright.core.GotoOptions
import com.webwright.core.LoadState
import com.webwright.core.Page
import com.webwright.core.ReloadOptions
import com.webwright.core.ScreenshotOptions
import com.webwright.core.Viewport
import com.webwright.core.WebwrightConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WebViewScreenshot(
    val contentType: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

class AndroidWebwrightSession private constructor(
    private val webView: WebView,
    private val browser: Browser,
    val page: Page,
    private val config: WebwrightConfig,
) {
    suspend fun goto(url: String) = page.goto(
        url,
        GotoOptions(
            waitUntil = LoadState.Load,
            timeoutMs = NAVIGATION_TIMEOUT_MS,
        ),
    )

    suspend fun waitForPageToSettle(delayMs: Long) {
        runCatching { page.waitForLoadState(LoadState.Load, LOAD_STATE_TIMEOUT_MS) }
        if (delayMs > 0) {
            page.waitForTimeout(delayMs)
        }
    }

    suspend fun evaluate(script: String): String = page.evaluate(script).value

    suspend fun cookieHeader(): String {
        val cookies = page.getAllCookies()
            .joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" }
        return cookies.ifBlank {
            page.url.value?.let(::readCookiesForUrl).orEmpty()
        }
    }

    suspend fun captureFullPagePng(maxHeightPixels: Int): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        webView.ensureLaidOut()
        val controller = AndroidScreenshotController(webView, config)
        val pageDimensions = controller.getPageDimensions()
        val scale = if (pageDimensions.contentHeight > maxHeightPixels) {
            (maxHeightPixels.toDouble() / pageDimensions.contentHeight.toDouble()).coerceIn(MIN_SCREENSHOT_SCALE, 1.0)
        } else {
            1.0
        }
        val screenshot = page.screenshot(
            ScreenshotOptions(
                fullPage = true,
                scale = scale,
            ),
        )

        WebViewScreenshot(
            contentType = "image/png",
            bytes = screenshot.bytes,
            width = screenshot.width,
            height = screenshot.height,
        )
    }

    suspend fun back() = page.back()

    suspend fun forward() = page.forward()

    suspend fun reload() = page.reload(ReloadOptions(timeoutMs = NAVIGATION_TIMEOUT_MS))

    fun stopLoading() {
        webView.stopLoading()
    }

    fun userAgent(): String = webView.settings.userAgentString.orEmpty()

    fun close() {
        browser.close()
    }

    companion object {
        fun attach(
            context: Context,
            webView: WebView,
            enableZoom: Boolean = false,
            overviewMode: Boolean = true,
            acceptThirdPartyCookies: Boolean = false,
            delegateWebViewClient: WebViewClient? = null,
            delegateWebChromeClient: WebChromeClient? = null,
        ): AndroidWebwrightSession {
            webView.configureChangeDetectionWebView(
                enableZoom = enableZoom,
                overviewMode = overviewMode,
                acceptThirdPartyCookies = acceptThirdPartyCookies,
            )
            webView.ensureLaidOut()

            val config = WebwrightConfig(
                viewport = Viewport(
                    width = webView.width.takeIf { it > 0 } ?: DEFAULT_VIEWPORT_WIDTH,
                    height = webView.height.takeIf { it > 0 } ?: DEFAULT_VIEWPORT_HEIGHT,
                    isMobile = true,
                    hasTouch = true,
                ),
            )
            val browser = Browser(
                AndroidPlatformContext(
                    context = context,
                    webView = webView,
                    delegateWebViewClient = delegateWebViewClient,
                    delegateWebChromeClient = delegateWebChromeClient,
                ),
                config,
            )

            return AndroidWebwrightSession(
                webView = webView,
                browser = browser,
                page = browser.page(),
                config = config,
            )
        }
    }
}

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

internal fun readCookiesForUrl(url: String): String = CookieManager.getInstance().getCookie(url).orEmpty()

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
private const val NAVIGATION_TIMEOUT_MS = 30_000L
private const val LOAD_STATE_TIMEOUT_MS = 10_000L
