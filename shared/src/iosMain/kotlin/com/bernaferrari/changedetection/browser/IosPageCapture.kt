@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.bernaferrari.changedetection.browser

import com.bernaferrari.kmpwright.core.Rectangle
import com.bernaferrari.kmpwright.core.captureWebViewDocument

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
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
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

    /** Full-page capture and lazy-load traversal are bounded to 6,000 native points. */
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
            loadPage(web, url)
            delay(settleMs.coerceIn(500, 15_000))
            lastTitle = (evaluate(web, PageCleanup.TITLE) as? String)?.takeIf { it.isNotBlank() }
            lastIcon = (evaluate(web, PageCleanup.ICON) as? String)?.takeIf { it.startsWith("http") }
            evaluate(web, PageCleanup.HIDE_CONSENT)
            evaluate(web, PageCleanup.HIDE_ADS)
            if (hide.isNotBlank()) evaluate(web, PageCleanup.hide(hide))
            // Lazy images load once they have been on screen: run through the page once.
            val cssViewportWidth = (evaluate(web, "window.visualViewport ? window.visualViewport.width : window.innerWidth") as? NSNumber)?.doubleValue ?: error("Could not measure the page viewport")
            require(cssViewportWidth.isFinite() && cssViewportWidth > 0) { "Invalid page viewport" }
            val pointsPerCssPixel = VIEWPORT_WIDTH / cssViewportWidth
            val fullHeight = pageHeight(web)
            var y = 0.0
            while (y < minOf(fullHeight, MAX_PAGE_POINTS / pointsPerCssPixel)) {
                evaluate(web, "window.scrollTo(0, $y); 0")
                delay(120)
                y += VIEWPORT_HEIGHT / pointsPerCssPixel
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
            val region = clip?.let { Rectangle(it.left, it.top, it.right - it.left, it.bottom - it.top) }
            return captureWebViewDocument(
                web,
                clip = region,
                pixelsPerCssPixel = 2 * pointsPerCssPixel,
                maxPixels = 9_360_000,
                maxHeight = 12_000,
                truncateFullPage = true,
                viewportWidthOnly = true,

            ).bytes
        } finally {
            web.stopLoading()
            web.removeFromSuperview()
        }
    }

    private suspend fun pageHeight(web: WKWebView): Double = (evaluate(web, "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)") as? NSNumber)?.doubleValue ?: VIEWPORT_HEIGHT

    private data class Clip(val left: Double, val top: Double, val right: Double, val bottom: Double)

    private suspend fun elementRect(web: WKWebView, selector: String): Clip? {
        val script = "(function(){var e=document.querySelector(${jsString(selector)});if(!e)return '';var r=e.getBoundingClientRect();return [r.left+window.scrollX,r.top+window.scrollY,r.right+window.scrollX,r.bottom+window.scrollY].join(',');})()"
        val parts = (evaluate(web, script) as? String)?.split(',')?.mapNotNull { it.toDoubleOrNull() } ?: return null
        if (parts.size != 4 || parts[3] - parts[1] < 1) return null
        return Clip(parts[0], parts[1], parts[2], parts[3])
    }

    private fun jsString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    private fun hostWindow(): UIWindow? = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow

    internal suspend fun loadPage(web: WKWebView, url: String) {
        val target = NSURL.URLWithString(url) ?: error("Invalid page address")
        awaitLoad(web) { web.loadRequest(captureRequest(target)) }
    }

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

    private class LoadDelegate(private val continuation: CancellableContinuation<Unit>) :
        NSObject(),
        WKNavigationDelegateProtocol {
        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationResponse: WKNavigationResponse,
            decisionHandler: (WKNavigationResponsePolicy) -> Unit,
        ) {
            val response = decidePolicyForNavigationResponse.response as? NSHTTPURLResponse
            val rejected = decidePolicyForNavigationResponse.isForMainFrame() && response != null && response.statusCode >= 400
            if (rejected) {
                // Preserve the HTTP failure before cancellation can produce a generic WebKit error.
                if (continuation.isActive) continuation.resumeWithException(com.bernaferrari.changedetection.error.NetworkError(response.statusCode.toInt(), "The page returned HTTP ${response.statusCode}"))
                release()
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
            } else {
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
            }
        }

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

/** Refresh content without discarding the persistent browser session or its cookies. */
internal fun captureRequest(url: NSURL): NSURLRequest = NSURLRequest.requestWithURL(
    url,
    cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
    timeoutInterval = 60.0,
)
