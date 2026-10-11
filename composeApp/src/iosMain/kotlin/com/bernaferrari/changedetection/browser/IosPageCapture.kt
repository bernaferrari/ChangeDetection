/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.bernaferrari.changedetection.browser

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIWindow
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKSnapshotConfiguration
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** WebKit holds navigation delegates weakly; this keeps each one alive until its load ends. */
private val aliveDelegates = mutableSetOf<Any>()

/**
 * Full-page screenshots with WKWebView. It uses the app's persistent website data store, the same one the
 * in-app sign-in browser uses, so pages that need a login are captured signed in.
 */
object IosPageCapture {
    /** A phone-sized viewport, so pages render their mobile layout like they would in Safari. */
    private const val VIEWPORT_WIDTH = 390.0
    private const val VIEWPORT_HEIGHT = 844.0

    /**
     * Exactly two pixels per CSS point. A whole number matters: content that moves down by N points lands
     * exactly 2N rows lower, pixel for pixel, so the comparison sees it moved instead of re-rendered.
     */
    private const val OUTPUT_WIDTH_PX = VIEWPORT_WIDTH * 2

    /** 12,000 px tall at 780 px wide, about the memory the capture always used. */
    private const val MAX_PAGE_POINTS = 6_000.0
    private val single = Mutex()

    /** The `og:title` / `<title>` of the most recent capture; used to name new pages. */
    var lastTitle: String? = null
        private set

    /** The page's best icon URL from the most recent capture. */
    var lastIcon: String? = null
        private set

    /**
     * @param hide CSS selectors of parts to ignore; they are made invisible (layout kept) before capture.
     * @param only CSS selector of the one part to watch; the capture is cropped to it.
     */
    suspend fun capture(url: String, settleMs: Long, hide: String = "", only: String = ""): ByteArray = single.withLock {
        withContext(Dispatchers.Main) {
            withTimeout(90_000) { captureOnMain(url, settleMs, hide, only) }
        }
    }

    private suspend fun captureOnMain(url: String, settleMs: Long, hide: String, only: String): ByteArray {
        val configuration = WKWebViewConfiguration().apply { websiteDataStore = WKWebsiteDataStore.defaultDataStore() }
        val web = WKWebView(frame = CGRectMake(0.0, 0.0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT), configuration = configuration)
        // WebKit only paints views that are in a window. Keep it behind the app, invisible to the user.
        web.userInteractionEnabled = false
        hostWindow()?.insertSubview(web, atIndex = 0)
        try {
            val target = NSURL.URLWithString(url) ?: error("Invalid page address")
            awaitLoad(web) { web.loadRequest(NSURLRequest.requestWithURL(target)) }
            delay(settleMs.coerceIn(500, 15_000))
            lastTitle = (evaluate(web, PageCleanup.TITLE) as? String)?.takeIf { it.isNotBlank() }
            lastIcon = (evaluate(web, PageCleanup.ICON) as? String)?.takeIf { it.startsWith("http") }
            evaluate(web, PageCleanup.HIDE_CONSENT)
            evaluate(web, PageCleanup.HIDE_ADS)
            if (hide.isNotBlank()) evaluate(web, PageCleanup.hide(hide))
            // Lazy images load once they have been on screen: run through the page once.
            val fullHeight = pageHeight(web)
            var y = 0.0
            while (y < minOf(fullHeight, MAX_PAGE_POINTS)) {
                evaluate(web, "window.scrollTo(0, $y); 0")
                delay(120)
                y += VIEWPORT_HEIGHT
            }
            evaluate(web, "window.scrollTo(0, 0); 0")
            // Lazy images and late content: wait until the page has really finished loading.
            PageCleanup.awaitSettled({ script -> evaluate(web, script)?.toString() })
            evaluate(web, PageCleanup.MASK_RELATIVE_TIMES)
            delay(300)
            // Ads injected while scrolling, and bottom bars, are hidden right before the first tile.
            evaluate(web, PageCleanup.HIDE_ADS)
            evaluate(web, PageCleanup.HIDE_BOTTOM_FLOATING)
            val clip = if (only.isNotBlank()) elementRect(web, only) ?: error("The part you picked is no longer on the page") else null
            val top = clip?.top ?: 0.0
            val bottom = minOf(clip?.bottom ?: pageHeight(web), top + MAX_PAGE_POINTS)
            return stitch(web, top, bottom.coerceAtLeast(top + 1), clip?.left ?: 0.0, clip?.right ?: VIEWPORT_WIDTH)
        } finally {
            web.stopLoading()
            web.removeFromSuperview()
        }
    }

    private suspend fun pageHeight(web: WKWebView): Double = (evaluate(web, "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)") as? NSNumber)?.doubleValue ?: VIEWPORT_HEIGHT

    private data class Clip(val left: Double, val top: Double, val right: Double, val bottom: Double)

    private suspend fun elementRect(web: WKWebView, selector: String): Clip? {
        val script = "(function(){var e=document.querySelector(${jsString(selector)});if(!e)return '';var r=e.getBoundingClientRect();return [r.left,r.top+window.scrollY,r.right,r.bottom+window.scrollY].join(',');})()"
        val parts = (evaluate(web, script) as? String)?.split(',')?.mapNotNull { it.toDoubleOrNull() } ?: return null
        if (parts.size != 4 || parts[3] - parts[1] < 1) return null
        return Clip(parts[0].coerceIn(0.0, VIEWPORT_WIDTH), parts[1].coerceAtLeast(0.0), parts[2].coerceIn(0.0, VIEWPORT_WIDTH), parts[3])
    }

    /**
     * WebKit returns an empty image for snapshots much taller than the screen, so the page is captured one
     * screen at a time and stitched. Fixed and sticky elements are hidden after the first screen so a header
     * does not repeat down the page.
     */
    private suspend fun stitch(web: WKWebView, top: Double, bottom: Double, left: Double, right: Double): ByteArray {
        val scale = OUTPUT_WIDTH_PX / VIEWPORT_WIDTH
        val outWidth = ((right - left) * scale).toInt().coerceAtLeast(1)
        val outHeight = ((bottom - top) * scale).toInt().coerceAtLeast(1)
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(outWidth, outHeight)
        val canvas = surface.canvas
        canvas.clear(org.jetbrains.skia.Color.WHITE)
        var y = top
        var first = true
        while (y < bottom) {
            evaluate(web, "window.scrollTo(0, $y); 0")
            delay(180)
            val actual = (evaluate(web, "window.scrollY") as? NSNumber)?.doubleValue ?: y
            val shot = snapshot(web, CGRectMake(0.0, 0.0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT))
            val image = org.jetbrains.skia.Image.makeFromEncoded(shot)
            // The screen shows page rows [actual, actual + viewport); draw the part that falls in [top, bottom).
            val dest = org.jetbrains.skia.Rect.makeXYWH(((0 - left) * scale).toFloat(), ((actual - top) * scale).toFloat(), (VIEWPORT_WIDTH * scale).toFloat(), (VIEWPORT_HEIGHT * scale).toFloat())
            canvas.drawImageRect(image, dest)
            image.close()
            if (first) {
                evaluate(web, PageCleanup.HIDE_PINNED)
                first = false
            }
            if (actual + VIEWPORT_HEIGHT >= bottom) break
            y = actual + VIEWPORT_HEIGHT
        }
        val png = surface.makeImageSnapshot().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes ?: error("Could not encode the capture")
        surface.close()
        return png
    }

    private fun jsString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    private fun hostWindow(): UIWindow? = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow

    private suspend fun awaitLoad(web: WKWebView, start: () -> Unit) = suspendCancellableCoroutine { continuation ->
        val delegate = LoadDelegate(continuation)
        web.navigationDelegate = delegate
        // The delegate is weak in WebKit; keep it alive until the load finishes.
        continuation.invokeOnCancellation { delegate.release() }
        aliveDelegates += delegate
        start()
    }

    private suspend fun evaluate(web: WKWebView, script: String): Any? = suspendCancellableCoroutine { continuation ->
        web.evaluateJavaScript(script) { result, _ -> if (continuation.isActive) continuation.resume(result) }
    }

    private suspend fun snapshot(web: WKWebView, rect: kotlinx.cinterop.CValue<platform.CoreGraphics.CGRect>): ByteArray = suspendCancellableCoroutine { continuation ->
        val configuration = WKSnapshotConfiguration().apply {
            this.rect = rect
            // snapshotWidth is in points and WebKit multiplies by the screen scale; aim for exactly 780 px wide.
            snapshotWidth = NSNumber(OUTPUT_WIDTH_PX / platform.UIKit.UIScreen.mainScreen.scale)
            afterScreenUpdates = true
        }
        web.takeSnapshotWithConfiguration(configuration) { image, error ->
            if (!continuation.isActive) return@takeSnapshotWithConfiguration
            val data = image?.let { UIImagePNGRepresentation(it) }
            if (data == null) {
                continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "Could not capture the page"))
                return@takeSnapshotWithConfiguration
            }
            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            continuation.resume(bytes)
        }
    }

    private class LoadDelegate(private val continuation: CancellableContinuation<Unit>) :
        NSObject(),
        WKNavigationDelegateProtocol {
        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            if (continuation.isActive) continuation.resume(Unit)
            release()
        }

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = fail(withError)

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) = fail(withError)

        private fun fail(error: NSError) {
            // A cancelled sub-navigation (code -999) is not a failed page.
            if (error.code == -999L) return
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error.localizedDescription))
            release()
        }

        fun release() {
            aliveDelegates -= this
        }
    }
}
