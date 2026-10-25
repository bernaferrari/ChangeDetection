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
import platform.Foundation.valueForKey
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

    data class CapturedPage(val bytes: ByteArray, val title: String?, val icon: String?, val contentType: String = "image/png", val validateCapture: suspend () -> Unit = {})

    /**
     * @param hide CSS selectors of parts to ignore; they are made invisible (layout kept) before capture.
     * @param only CSS selector of the one part to watch; the capture is cropped to it.
     */
    suspend fun capture(url: String, settleMs: Long, hide: String = "", only: String = ""): ByteArray = capturePage(url, settleMs, hide, only).bytes

    suspend fun capturePage(url: String, settleMs: Long, hide: String = "", only: String = "", text: Boolean = false): CapturedPage = single.withLock {
        withContext(Dispatchers.Main) {
            withTimeout(90_000) {
                val configuration = WKWebViewConfiguration().apply { websiteDataStore = WKWebsiteDataStore.defaultDataStore() }
                val web = WKWebView(frame = CGRectMake(0.0, 0.0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT), configuration = configuration)
                web.userInteractionEnabled = false
                // DOM capture works without a scene/window (background refresh and test hosts).
                // Only pixel capture needs a host; never enumerate UIKit windows for Text.
                if (!text) hostWindow()?.insertSubview(web, atIndex = 0)
                try {
                    val navigation = awaitLoad(web, keepAlive = true) { web.loadRequest(captureRequest(NSURL.URLWithString(url) ?: error("Invalid page address"))) }
                    try {
                        navigation.validate()
                        delay(settleMs.coerceIn(0, 30_000))
                        val session = kotlinx.serialization.json.Json.parseToJsonElement(evaluate(web, RenderedCaptureScripts.SESSION) as? String ?: error("Could not read the page")).let { it as kotlinx.serialization.json.JsonArray }
                        validateBrowserSession(url, (session[0] as kotlinx.serialization.json.JsonPrimitive).content, (session[1] as kotlinx.serialization.json.JsonPrimitive).content == "true")
                        val result = if (text) {
                            awaitSelected(web, only, requireText = true)
                            val html = evaluate(web, RenderedCaptureScripts.HTML) as? String ?: error("Could not read the page")
                            require(html.length <= RenderedCaptureScripts.MAX_HTML_CHARS) { "The page is too large to preview" }
                            require(com.bernaferrari.changedetection.util.readableSnapshotText(html, true).isNotBlank()) { "The page returned no readable text" }
                            CapturedPage(html.encodeToByteArray(), readTitle(web), readIcon(web), "text/html")
                        } else {
                            captureDocument(web, hide, only)
                        }
                        navigation.validate()
                        result.copy(validateCapture = navigation::validate)
                    } finally {
                        navigation.release()
                    }
                } finally {
                    web.stopLoading()
                    web.navigationDelegate = null
                    web.removeFromSuperview()
                }
            }
        }
    }

    /** Capture the open browser with the same viewport and preparation as a scheduled check. */
    suspend fun captureLoaded(web: WKWebView, hide: String = "", only: String = ""): ByteArray = single.withLock {
        withContext(Dispatchers.Main) {
            val frame = web.frame
            val scrollOffset = web.scrollView.contentOffset
            val zoom = web.scrollView.zoomScale
            try {
                web.setFrame(CGRectMake(0.0, 0.0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT))
                web.scrollView.setZoomScale(1.0, animated = false)
                withTimeout(90_000) { captureDocument(web, hide, only, preservePage = true).bytes }
            } finally {
                web.setFrame(frame)
                web.scrollView.setZoomScale(zoom, animated = false)
                web.scrollView.setContentOffset(scrollOffset, animated = false)
            }
        }
    }

    private suspend fun captureDocument(web: WKWebView, hide: String, only: String, preservePage: Boolean = false): CapturedPage {
        suspend fun clean(script: String) {
            evaluate(web, if (preservePage) PageCleanup.reversible(script) else script)
        }
        try {
            evaluate(web, RenderedCaptureScripts.RESET_RESOURCES)
            val title = readTitle(web)
            val icon = readIcon(web)
            awaitSelected(web, only, requireText = false)
            PageCleanup.STABILIZE.forEach { clean(it) }
            clean(PageCleanup.HIDE_CONSENT)
            clean(PageCleanup.HIDE_ADS)
            if (hide.isNotBlank()) clean(PageCleanup.hide(hide))
            ensureVisualSelectionVisible(web, only)
            // Warm only the area being captured, including a selection below the full-page budget.
            val cssViewportWidth = (evaluate(web, "window.visualViewport ? window.visualViewport.width : window.innerWidth") as? NSNumber)?.doubleValue ?: error("Could not measure the page viewport")
            require(cssViewportWidth.isFinite() && cssViewportWidth > 0) { "Invalid page viewport" }
            val pointsPerCssPixel = VIEWPORT_WIDTH / cssViewportWidth
            val fullHeight = pageHeight(web)
            val initialClip = if (only.isNotBlank()) elementRect(web, only) ?: throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.REGION_MISSING) else null
            val maxPageCssHeight = MAX_PAGE_POINTS / pointsPerCssPixel
            require(initialClip == null || initialClip.bottom - initialClip.top <= maxPageCssHeight) { "This selection is too tall for a screenshot. Use page text monitoring." }
            var y = initialClip?.top ?: 0.0
            val bottom = initialClip?.bottom ?: minOf(fullHeight, maxPageCssHeight)
            while (y < bottom) {
                evaluate(web, "window.scrollTo(0, $y); 0")
                delay(120)
                y += VIEWPORT_HEIGHT / pointsPerCssPixel
            }
            evaluate(web, "window.scrollTo(0, 0); 0")
            // Lazy images and late content: wait until the page has really finished loading.
            val resources = RenderedCaptureScripts.resources(only, maxPageCssHeight)
            awaitResources(web, resources)
            clean(PageCleanup.MASK_RELATIVE_TIMES)
            delay(100)
            // Ads injected while scrolling, and bottom bars, are hidden right before the first tile.
            clean(PageCleanup.HIDE_ADS)
            clean(PageCleanup.HIDE_APP_BANNERS)
            clean(PageCleanup.HIDE_BOTTOM_FLOATING)
            ensureVisualSelectionVisible(web, only)
            val clip = if (only.isNotBlank()) elementRect(web, only) ?: throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.REGION_MISSING) else null
            val region = clip?.let { Rectangle(it.left, it.top, it.right - it.left, it.bottom - it.top) }
            val before = evaluate(web, resources) as? String
            val shot = captureWebViewDocument(
                web,
                clip = region,
                pixelsPerCssPixel = 2 * pointsPerCssPixel,
                maxPixels = 9_360_000,
                maxHeight = 12_000,
                truncateFullPage = true,
                viewportWidthOnly = true,

            ).bytes
            val after = evaluate(web, resources) as? String
            if (before != after) throw CaptureNotReadyException(CaptureIssue.LAYOUT_CHANGED)
            return CapturedPage(shot, title, icon)
        } finally {
            withContext(kotlinx.coroutines.NonCancellable) {
                kotlinx.coroutines.withTimeoutOrNull(2_000) { evaluate(web, RenderedCaptureScripts.RESET_RESOURCES) }
            }
            if (preservePage) {
                withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.withTimeoutOrNull(2_000) { evaluate(web, PageCleanup.RESTORE_CAPTURE) }
                }
            }
        }
    }

    private suspend fun pageHeight(web: WKWebView): Double = (evaluate(web, "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)") as? NSNumber)?.doubleValue ?: VIEWPORT_HEIGHT

    private data class Clip(val left: Double, val top: Double, val right: Double, val bottom: Double)

    private suspend fun readTitle(web: WKWebView) = (evaluate(web, PageCleanup.TITLE) as? String)?.takeIf { it.isNotBlank() }
    private suspend fun readIcon(web: WKWebView) = (evaluate(web, PageCleanup.ICON) as? String)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

    private suspend fun awaitSelected(web: WKWebView, selector: String, requireText: Boolean) {
        if (selector.isBlank()) return
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        while (true) {
            val value = evaluate(web, RenderedCaptureScripts.region(selector)) as? String ?: ""
            if (value == "-1") throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.INVALID_SELECTOR)
            val parts = value.split(',').mapNotNull(String::toIntOrNull)
            if (parts.size == 2 && parts[0] > 0 && (!requireText || parts[1] > 0)) return
            if (started.elapsedNow().inWholeMilliseconds >= 8_000) throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.REGION_MISSING)
            delay(100)
        }
    }

    private suspend fun awaitResources(web: WKWebView, resources: String) {
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        var previous = ""
        var quiet = 0
        while (true) {
            val raw = evaluate(web, resources) as? String ?: ""
            val state = raw.split(',').mapNotNull(String::toDoubleOrNull)
            if (state.size != 6) throw CaptureNotReadyException(CaptureIssue.VIEWPORT_NOT_READY)
            if (state[1] > 0 || state[2] < 0) throw CaptureNotReadyException(CaptureIssue.RESOURCE_FAILED)
            quiet = if (state.take(4).all { it == 0.0 } && raw == previous) quiet + 1 else 0
            if (quiet >= 2) return
            previous = raw
            if (started.elapsedNow().inWholeMilliseconds >= 8_000) throw CaptureNotReadyException(if (state[2] > 0) CaptureIssue.FONTS_LOADING else CaptureIssue.PENDING_IMAGES)
            delay(250)
        }
    }

    /** A rule that hides its entire watched area is invalid, not a new blank version. */
    private suspend fun ensureVisualSelectionVisible(web: WKWebView, selector: String) {
        val selected = if (selector.isBlank()) "document.body" else "document.querySelector(${jsString(selector)})"
        val script = """(function(){try{var e=$selected;if(!e)return 'missing';var s=getComputedStyle(e);if(s.visibility==='hidden'||s.visibility==='collapse')return 'excluded';for(var p=e;p;p=p.parentElement){s=getComputedStyle(p);if(s.display==='none'||parseFloat(s.opacity)===0||s.contentVisibility==='hidden')return 'excluded'}return 'visible'}catch(e){return 'invalid'}})()"""
        when (evaluate(web, script) as? String) {
            "visible" -> Unit
            "excluded" -> throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.REGION_MISSING, issue = com.bernaferrari.changedetection.detection.TextRegionIssue.EXCLUDED)
            "missing" -> throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.REGION_MISSING)
            else -> throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.INVALID_SELECTOR)
        }
    }

    private suspend fun elementRect(web: WKWebView, selector: String): Clip? {
        val script = "(function(){try{var a=document.querySelectorAll(${jsString(selector)}),e=a[0];if(!e)return '';var r=e.getBoundingClientRect();return [a.length,r.left+window.scrollX,r.top+window.scrollY,r.right+window.scrollX,r.bottom+window.scrollY].join(',')}catch(e){return '-1'}})()"
        val result = evaluate(web, script) as? String ?: return null
        if (result == "-1") throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.INVALID_SELECTOR)
        val parts = result.split(',').mapNotNull(String::toDoubleOrNull)
        if (parts.firstOrNull()?.let { it > 1 } == true) throw com.bernaferrari.changedetection.detection.TextRegionException(com.bernaferrari.changedetection.detection.TextRegionFailure.INVALID_SELECTOR, issue = com.bernaferrari.changedetection.detection.TextRegionIssue.MULTIPLE_MATCHES)
        if (parts.size != 5 || parts[3] - parts[1] < 1 || parts[4] - parts[2] < 1) return null
        return Clip(parts[1], parts[2], parts[3], parts[4])
    }

    private fun jsString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    /**
     * UIKit's SDK annotates windows/connectedScenes as nonnull, but an early/background
     * app host can return nil. KVC exposes the object result as Any? before collection
     * bridging, so no Kotlin collection method ever receives a native nil receiver.
     */
    private fun hostWindow(): UIWindow? {
        val application = UIApplication.sharedApplication
        val keyWindow = application.keyWindow
        val scenes = (application.valueForKey("connectedScenes") as? Set<*>).orEmpty()
        val sceneWindows = scenes.mapNotNull { it as? platform.UIKit.UIWindowScene }
            .filter { it.activationState == platform.UIKit.UISceneActivationStateForegroundActive || it.activationState == platform.UIKit.UISceneActivationStateForegroundInactive }
            .flatMap { windowsFromNullableObject(it.valueForKey("windows")) }
        return sceneWindows.firstOrNull { it == keyWindow }
            ?: sceneWindows.firstOrNull { !it.hidden && it.alpha > 0.0 }
            ?: keyWindow
            ?: windowsFromNullableObject(application.valueForKey("windows")).firstOrNull { !it.hidden && it.alpha > 0.0 }
    }

    private fun windowsFromNullableObject(value: Any?): List<UIWindow> = (value as? List<*>)?.mapNotNull { it as? UIWindow }.orEmpty()

    internal suspend fun loadPage(web: WKWebView, url: String) {
        val target = NSURL.URLWithString(url) ?: error("Invalid page address")
        awaitLoad(web) { web.loadRequest(captureRequest(target)) }
    }

    private suspend fun awaitLoad(web: WKWebView, keepAlive: Boolean = false, start: () -> Unit): LoadDelegate {
        lateinit var delegate: LoadDelegate
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                delegate = LoadDelegate(continuation, keepAlive)
                web.navigationDelegate = delegate
                continuation.invokeOnCancellation { delegate.release() }
                aliveDelegates += delegate
                start()
            }
            return delegate
        } catch (error: Throwable) {
            runCatching { delegate.release() }
            throw error
        }
    }

    private suspend fun evaluate(web: WKWebView, script: String): Any? = suspendCancellableCoroutine { continuation ->
        web.evaluateJavaScript(script) { result, _ -> if (continuation.isActive) continuation.resume(result) }
    }

    private class LoadDelegate(continuation: CancellableContinuation<Unit>, private val keepAlive: Boolean = false) :
        NSObject(),
        WKNavigationDelegateProtocol {
        private var pending: CancellableContinuation<Unit>? = continuation
        private var completed = false

        @kotlin.concurrent.Volatile private var failure: Exception? = null
        fun validate() {
            failure?.let { throw it }
        }

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            if (completed) failure = IllegalStateException("The page navigated during capture")
        }

        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationResponse: WKNavigationResponse,
            decisionHandler: (WKNavigationResponsePolicy) -> Unit,
        ) {
            val response = decidePolicyForNavigationResponse.response as? NSHTTPURLResponse
            val rejected = decidePolicyForNavigationResponse.isForMainFrame() && response != null && response.statusCode >= 400
            if (rejected) {
                // Preserve the HTTP failure before cancellation can produce a generic WebKit error.
                failure = com.bernaferrari.changedetection.error.NetworkError(response.statusCode.toInt(), "The page returned HTTP ${response.statusCode}")
                completeLoad(failure!!)
                if (!keepAlive) release()
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
            } else {
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
            }
        }

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            completed = true
            completeLoad()
            if (!keepAlive) release()
        }

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) = fail(withError)

        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) = fail(withError)

        private fun fail(error: NSError) {
            // A cancelled sub-navigation (code -999) is not a failed page.
            if (error.code == -999L) return
            val tls = generateSequence(error) { it.userInfo[platform.Foundation.NSUnderlyingErrorKey] as? NSError }.take(4).any { it.domain == platform.Foundation.NSURLErrorDomain && it.code in -1206L..-1200L }
            failure = if (tls) com.bernaferrari.changedetection.error.IosCaptureTlsFailure() else IllegalStateException("The page could not be loaded")
            completeLoad(failure!!)
            if (!keepAlive) release()
        }

        // A returned capture keeps validate() alive, not the completed coroutine stack.
        private fun completeLoad(error: Throwable? = null) {
            val waiting = pending
            pending = null
            if (waiting?.isActive == true) {
                if (error == null) waiting.resume(Unit) else waiting.resumeWithException(error)
            }
        }

        fun release() {
            val clear = {
                pending = null
                aliveDelegates -= this
            }
            if (platform.Foundation.NSThread.isMainThread) clear() else platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) { clear() }
        }
    }
}

/** Refresh content without discarding the persistent browser session or its cookies. */
internal fun captureRequest(url: NSURL): NSURLRequest = NSURLRequest.requestWithURL(
    url,
    cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
    timeoutInterval = 60.0,
)
