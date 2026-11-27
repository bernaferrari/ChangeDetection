package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import android.net.http.SslError
import android.view.Choreographer
import android.webkit.*
import com.bernaferrari.kmpwright.core.Rectangle
import com.bernaferrari.kmpwright.core.captureWebViewDocument
import java.io.IOException
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.detection.TextRegionFailure
import com.bernaferrari.changedetection.detection.TextRegionIssue
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
    private val health = WebViewCaptureHealth.of(view)
    private var pixelValidation: Pair<Long, PageCaptureHealth.DependencyScope>? = null

    fun captureValidator(): suspend () -> Unit {
        val textOnly = contentStage == DiagnosticStage.EXTRACT_TEXT
        val pixel = if (textOnly) null else pixelValidation
        val epoch = pixel?.first ?: health.epoch()
        val extent = captureExtent
        return { health.validate(epoch, extent, textOnly, scope = pixel?.second) }
    }
    private var rendererFailure: IOException? = null
    private var navigationFailure: Exception? = null
    private var contentStage = DiagnosticStage.CAPTURE_PIXELS
    private var captureExtent: CaptureExtent? = null

    /**
     * @param hide CSS selectors of parts to ignore; made invisible (layout kept) before capture.
     * @param only CSS selector of the single part to watch; the capture is cropped to it.
     */
    suspend fun capture(url: String, settleDelayMs: Long, maxHeight: Int, hide: String = "", only: String = ""): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        diagnosticMetric(DiagnosticStage.CREATE_BROWSER, DiagnosticMetric.ATTACHED, if (view.isAttachedToWindow) 1 else 0)
        diagnosticStage(DiagnosticStage.NAVIGATE) { navigate(url) }
        diagnosticStage(DiagnosticStage.SETTLE) { delay(settleDelayMs.coerceIn(0, 30_000)) }
        validateBrowserSessionState(url, evaluate(RenderedCaptureScripts.SESSION))
        diagnosticStage(DiagnosticStage.CAPTURE_PIXELS) { captureLoaded(maxHeight, hide, only) }
    }

    /** Text watches must read the rendered DOM, just like the initial live preview.
     * Keep screenshot cleanup out of this path: it mutates the content being watched.
     */
    suspend fun captureText(url: String, settleDelayMs: Long, only: String = ""): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        contentStage = DiagnosticStage.EXTRACT_TEXT
        captureExtent = null
        diagnosticMetric(DiagnosticStage.CREATE_BROWSER, DiagnosticMetric.ATTACHED, if (view.isAttachedToWindow) 1 else 0)
        diagnosticStage(DiagnosticStage.NAVIGATE) { navigate(url) }
        diagnosticStage(DiagnosticStage.SETTLE) { delay(settleDelayMs.coerceIn(0, 30_000)) }
        validateBrowserSessionState(url, evaluate(RenderedCaptureScripts.SESSION))
        // A configured delay may be zero while the selected node is still arriving.
        // Give missing content a bounded grace period, then let normal region validation
        // report a genuinely removed selection. Never fall back to the whole page.
        if (only.isNotBlank()) {
            awaitSelectedRegion(only, requireText = true)
        } else {
            kotlinx.coroutines.withTimeoutOrNull(READY_TIMEOUT_MS) {
                while (evaluate("!!document.body && !!document.body.innerText.trim()") != "true") {
                    checkNavigation()
                    delay(100)
                }
            }
        }
        navigationFailure?.let { throw it }
        rendererFailure?.let { throw it }
        val epoch = health.epoch()
        registerResourceDependencies(only)
        health.validate(epoch, textOnly = true)
        diagnosticCapture(captureContext(only), contentStage)
        val raw = diagnosticStage(DiagnosticStage.EXTRACT_TEXT) { evaluate("document.documentElement.outerHTML") }
        require(raw.length <= 12_000_000) { "The page is too large to preview" }
        val html = kotlinx.serialization.json.Json.decodeFromString<String>(raw)
        withContext(Dispatchers.Default) {
            require(com.bernaferrari.changedetection.util.readableSnapshotText(html, true).isNotBlank()) { "The page returned no readable text" }
        }
        health.validate(epoch, textOnly = true)
        WebViewScreenshot("text/html", html.encodeToByteArray(), 0, 0)
    }

    /** Uses the already-loaded document, with the same cleanup and tiling as scheduled checks. */
    suspend fun captureLoaded(maxHeight: Int, hide: String = "", only: String = "", preservePage: Boolean = false): WebViewScreenshot = withContext(Dispatchers.Main.immediate) {
        contentStage = DiagnosticStage.CAPTURE_PIXELS
        captureExtent = CaptureExtent.Empty
        val epoch = health.epoch()
        val scope = PageCaptureHealth.DependencyScope()
        pixelValidation = epoch to scope
        health.validateDocument(epoch)
        suspend fun clean(script: String) {
            evaluate(if (preservePage) PageCleanup.reversible(script) else script)
        }

        @Suppress("DEPRECATION")
        val userScale = view.scale.toDouble()
        val userTextZoom = view.settings.textZoom
        try {
            // Native layout and Chromium layout are asynchronous, particularly after live-preview resizing.
            if (view.settings.textZoom != 100) view.settings.textZoom = 100
            awaitPaint()
            if (only.isNotBlank()) awaitSelectedRegion(only, requireText = false)
            normalizeCaptureZoom()
            val title = evaluate(PageCleanup.TITLE).trim('"').takeIf { it.isNotBlank() && it != "null" }
            val icon = evaluate(PageCleanup.ICON).trim('"').takeIf { it.startsWith("http") }
            // One renderer round trip; reversible() still records all mutations in order.
            clean(
                "(function(){" + buildList {
                    addAll(PageCleanup.STABILIZE)
                    add(PageCleanup.HIDE_CONSENT)
                    add(PageCleanup.HIDE_ADS)
                    add(PageCleanup.HIDE_BOTTOM_FLOATING)
                    if (hide.isNotBlank()) add(PageCleanup.hide(hide))
                }.joinToString(";") + ";return 0;})()",
            )
            suspend fun captureRange() = pixelResourceBounds(maxHeight, if (only.isNotBlank()) readClip(only).toRectangle() else null)
            var range = captureRange()
            evaluate("window.scrollTo({left:0,top:${range.y},behavior:'instant'});0")
            for (step in 0 until 12) {
                currentCoroutineContext().ensureActive()
                val bounds = evaluate("[window.scrollY,window.innerHeight,Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)].join(',')")
                    .trim('"').split(',').mapNotNull { it.toDoubleOrNull() }
                range = captureRange()
                if (bounds.size != 3 || bounds[1] <= 0 || bounds[0] + bounds[1] >= minOf(bounds[2], range.y + range.height) - 1) break
                evaluate("window.scrollTo({left:0,top:${bounds[0] + bounds[1]},behavior:'instant'});0")
                awaitPaint()
            }
            evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
            if (only.isNotBlank()) evaluate("(function(){var e=document.querySelector(${PageCleanup.js(only)});if(e)e.scrollIntoView();return 0;})()")
            val captureBounds = captureRange()
            awaitResources(captureBounds, only, epoch, scope)
            // A late font or responsive style can alter the initial overview scale.
            normalizeCaptureZoom()
            val settledBounds = captureRange()
            if (settledBounds != captureBounds) awaitResources(settledBounds, only, epoch, scope)
            awaitNetworkQuiet()
            evaluate("window.scrollTo({left:0,top:0,behavior:'instant'});0")
            clean(PageCleanup.MASK_RELATIVE_TIMES)
            // Promotional banners are often injected after load; one that arrived since the first pass goes now.
            clean(PageCleanup.HIDE_APP_BANNERS)
            awaitFrame()
            navigationFailure?.let { throw it }
            rendererFailure?.let { throw it }
            val region = if (only.isNotBlank()) readClip(only).toRectangle() else null
            if (region == null) {
                @Suppress("DEPRECATION")
                val pageHeight = view.contentHeight.toDouble() * view.scale
                require(pageHeight <= 100_000) { "This page is too tall for a screenshot. Use page text monitoring." }
            }
            val dependencyBounds = pixelResourceBounds(maxHeight, region)
            registerResourceDependencies(only, dependencyBounds, scope)
            health.validate(epoch, captureExtent, scope = scope)
            val before = captureContext(only)
            diagnosticCapture(before)
            val shot = try {
                captureWebViewDocument(
                    view,
                    clip = region,
                    pixelsPerCssPixel = PIXEL_SCALE,
                    maxPixels = MAX_PIXELS,
                    maxHeight = maxHeight,
                    truncateFullPage = true,
                    viewportWidthOnly = true,
                    afterFirstTile = { clean(PageCleanup.HIDE_PINNED) },
                )
            } catch (error: IllegalStateException) {
                if (error.isScreenshotLayoutChange()) notReady(CaptureIssue.LAYOUT_CHANGED)
                throw error
            }
            captureExtent = CaptureExtent(shot.width, shot.height)
            val after = captureContext(only)
            diagnosticCapture(after)
            if (before.nativeWidth != after.nativeWidth || before.nativeHeight != after.nativeHeight ||
                kotlin.math.abs(before.layoutWidthTenths - after.layoutWidthTenths) > 5 ||
                kotlin.math.abs(before.visualWidthTenths - after.visualWidthTenths) > 5 ||
                kotlin.math.abs(before.nativeScaleMilli - after.nativeScaleMilli) > 2 ||
                (only.isNotBlank() && (before.selectedWidthTenths != after.selectedWidthTenths || before.selectedHeightTenths != after.selectedHeightTenths))
            ) {
                notReady(CaptureIssue.LAYOUT_CHANGED)
            }
            registerResourceDependencies(only, dependencyBounds, scope)
            health.validate(epoch, captureExtent, scope = scope)
            WebViewScreenshot("image/png", shot.bytes, shot.width, shot.height, title, icon)
        } finally {
            if (preservePage) {
                withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.withTimeoutOrNull(2_000) {
                        evaluate(PageCleanup.RESTORE_CAPTURE)
                        view.settings.textZoom = userTextZoom
                        @Suppress("DEPRECATION")
                        val currentScale = view.scale.toDouble()
                        if (userScale > 0 && currentScale > 0 && kotlin.math.abs(currentScale / userScale - 1) > .001) {
                            view.zoomBy((userScale / currentScale).toFloat())
                            awaitPaint()
                        }
                    }
                }
            }
        }
    }

    private fun checkNavigation() {
        navigationFailure?.let { throw it }
        rendererFailure?.let { throw it }
    }

    private suspend fun notReady(issue: CaptureIssue, resources: List<CaptureResourceFailure> = emptyList()): Nothing {
        diagnosticMetric(contentStage, DiagnosticMetric.CAPTURE_ISSUE, issue.ordinal)
        throw CaptureNotReadyException(issue, resources, captureExtent)
    }

    private suspend fun awaitPaint() {
        checkNavigation()
        if (view.isAttachedToWindow) {
            try {
                awaitVisualState()
            } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                notReady(CaptureIssue.VIEWPORT_NOT_READY)
            }
        }
        awaitFrame()
    }

    /** Restore the page's initial viewing scale, not the user's pinch zoom. No image resampling or reload. */
    private suspend fun normalizeCaptureZoom() {
        val raw = evaluate("""(function(){var m=document.querySelector('meta[name="viewport"]'),v=m?m.content:'',i=/(?:^|[,;])\s*initial-scale\s*=\s*([0-9.]+)/i.exec(v);return [i?parseFloat(i[1]):0,Math.max(document.documentElement.clientWidth,document.documentElement.scrollWidth,document.body?document.body.scrollWidth:0),window.devicePixelRatio||1].join(',')})()""")
            .trim('"').split(',').mapNotNull { it.toDoubleOrNull()?.takeIf(Double::isFinite) }
        if (raw.size != 3 || raw[1] <= 0 || raw[2] <= 0) notReady(CaptureIssue.VIEWPORT_NOT_READY)
        val target = if (raw[0] > 0) raw[0] * raw[2] else view.width / raw[1]

        @Suppress("DEPRECATION")
        val scale = view.scale.toDouble()
        if (!target.isFinite() || !scale.isFinite() || target <= 0 || scale <= 0) notReady(CaptureIssue.VIEWPORT_NOT_READY)
        if (kotlin.math.abs(scale / target - 1) > .001) {
            view.zoomBy((target / scale).toFloat().coerceIn(.01f, 100f))
            awaitPaint()
        }
    }

    private suspend fun regionState(selector: String): List<Double> {
        val raw = evaluate("""(function(){try{var a=document.querySelectorAll(${PageCleanup.js(selector)}),e=a[0];if(!e)return '0';var r=e.getBoundingClientRect();return [a.length,r.left+scrollX,r.top+scrollY,r.right+scrollX,r.bottom+scrollY,e.innerText&&e.innerText.trim()?1:0].join(',')}catch(e){return '-1'}})()""")
        return raw.trim('"').split(',').mapNotNull { it.toDoubleOrNull()?.takeIf(Double::isFinite) }
    }

    private suspend fun regionFailure(failure: TextRegionFailure, issue: TextRegionIssue = if (failure == TextRegionFailure.INVALID_SELECTOR) TextRegionIssue.INVALID_SYNTAX else TextRegionIssue.NO_MATCH): Nothing {
        diagnosticMetric(contentStage, DiagnosticMetric.REGION_RESULT, issue.ordinal)
        throw TextRegionException(failure, issue = issue)
    }

    private suspend fun visibleVisualRegion(selector: String): Boolean = evaluate("""(function(){var e=document.querySelector(${PageCleanup.js(selector)});if(!e)return false;var v=getComputedStyle(e).visibility;if(v==='hidden'||v==='collapse')return false;for(var a=e;a;a=a.parentElement){var s=getComputedStyle(a);if(s.display==='none'||s.contentVisibility==='hidden'||parseFloat(s.opacity)===0)return false;}return true})()""") == "true"

    private suspend fun awaitSelectedRegion(selector: String, requireText: Boolean) {
        val started = android.os.SystemClock.elapsedRealtime()
        while (true) {
            checkNavigation()
            val state = regionState(selector)
            if (state.firstOrNull() == -1.0) regionFailure(TextRegionFailure.INVALID_SELECTOR)
            if (!requireText && state.firstOrNull()?.let { it > 1 } == true) regionFailure(TextRegionFailure.INVALID_SELECTOR, TextRegionIssue.MULTIPLE_MATCHES)
            if (state.size == 6 && state[0] > 0 && (if (requireText) state[5] > 0 else state[3] > state[1] && state[4] > state[2] && visibleVisualRegion(selector))) return
            if (android.os.SystemClock.elapsedRealtime() - started >= READY_TIMEOUT_MS) {
                diagnosticCapture(captureContext(selector), contentStage)
                regionFailure(
                    TextRegionFailure.REGION_MISSING,
                    if (state.size == 6 && state[0] > 0) {
                        if (requireText) TextRegionIssue.EMPTY_TEXT else TextRegionIssue.NOT_VISIBLE
                    } else {
                        TextRegionIssue.NO_MATCH
                    },
                )
            }
            delay(100)
        }
    }

    private suspend fun readClip(selector: String): List<Double> {
        val state = regionState(selector)
        if (state.firstOrNull() == -1.0) regionFailure(TextRegionFailure.INVALID_SELECTOR)
        if (state.firstOrNull()?.let { it > 1 } == true) regionFailure(TextRegionFailure.INVALID_SELECTOR, TextRegionIssue.MULTIPLE_MATCHES)
        if (state.size != 6 || state[0] <= 0 || state[3] <= state[1] || state[4] <= state[2]) regionFailure(TextRegionFailure.REGION_MISSING)
        if (!visibleVisualRegion(selector)) regionFailure(TextRegionFailure.REGION_MISSING, TextRegionIssue.EXCLUDED)
        return state.subList(1, 5)
    }

    /** URLs stay bounded and in memory; exported diagnostics contain only fixed capture failure reasons. */
    internal suspend fun validateCurrentTextResources() = withContext(Dispatchers.Main.immediate) {
        registerResourceDependencies("")
        health.validate(textOnly = true)
    }

    private fun List<Double>.toRectangle() = Rectangle(this[0], this[1], this[2] - this[0], this[3] - this[1])

    // Match captureWebViewDocument: complete selected clip, or viewport-width full page limited by its pixel budget.
    private fun pixelResourceBounds(maxHeight: Int, region: Rectangle?): Rectangle {
        if (region != null) return region
        @Suppress("DEPRECATION")
        val nativePerCss = view.scale.toDouble().takeIf { it.isFinite() && it > 0 } ?: 1.0
        val cssWidth = view.width / nativePerCss
        val pixelWidth = (cssWidth * PIXEL_SCALE).roundToInt().coerceAtLeast(1)
        val cssHeight = minOf(maxHeight.toLong(), MAX_PIXELS / pixelWidth) / PIXEL_SCALE
        return Rectangle(0.0, 0.0, cssWidth, cssHeight)
    }

    private fun pixelResourceVisibility(bounds: Rectangle) = """
        function isInCapturedPixels(e,r){
            if(r.right+scrollX<=${bounds.x}||r.left+scrollX>=${bounds.x + bounds.width}||
                r.bottom+scrollY<=${bounds.y}||r.top+scrollY>=${bounds.y + bounds.height})return false;
            for(var a=e;a;a=a.parentElement)if(parseFloat(getComputedStyle(a).opacity)===0)return false;
            return true;
        }
    """.trimIndent()

    private suspend fun registerResourceDependencies(selector: String, bounds: Rectangle? = null, scope: PageCaptureHealth.DependencyScope? = null) {
        val raw = evaluate(
            """(function(){
            ${bounds?.let(::pixelResourceVisibility).orEmpty()}
            var urls=new Map();
            function add(url,kind){if(url&&/^https?:/i.test(url)&&!urls.has(url))urls.set(url,kind);}
            document.querySelectorAll('link[rel~=stylesheet][href]').forEach(function(e){add(e.href,'STYLESHEET');});
            var roots=${if (selector.isBlank()) "[document.body]" else "Array.from(document.querySelectorAll(${PageCleanup.js(selector)}))"};
            roots.forEach(function(root){
                if(!root)return;
                var elements=[root].concat(Array.from(root.querySelectorAll('*'))).slice(0,5000);
                elements.forEach(function(e){
                    var r=e.getBoundingClientRect(),s=getComputedStyle(e);
                    if(r.width<2||r.height<2||s.visibility==='hidden'||s.display==='none'||parseFloat(s.opacity)===0${if (bounds == null) "" else "||!isInCapturedPixels(e,r)"})return;
                    if(e.tagName==='IMG')add(e.currentSrc||e.src,'IMAGE');
                    if(e.tagName==='IFRAME')add(e.src,'FRAME');
                    var re=/url\((?:"([^"]*)"|'([^']*)'|([^)]*))\)/g,m;
                    while((m=re.exec(s.backgroundImage)))add(m[1]||m[2]||m[3],'IMAGE');
                });
            });
            var result={};
            Array.from(urls).slice(0,513).forEach(function(entry){result[entry[0]]=entry[1];});
            return JSON.stringify(result);
            })()""",
        )
        val values = kotlinx.serialization.json.Json.decodeFromString<Map<String, CaptureResourceKind>>(kotlinx.serialization.json.Json.decodeFromString<String>(raw))
        health.dependencies(values, scope)
    }

    private fun resourceScript(bounds: Rectangle, selector: String) = """(function(){
        var p=0, fontChecks=[], root=${if (selector.isBlank()) "document.body" else "document.querySelector(${PageCleanup.js(selector)})"};
        if(!root)return '0,0,0,0,0,0';
        ${pixelResourceVisibility(bounds)}
        function visible(e){var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>=2&&r.height>=2&&s.visibility!=='hidden'&&s.display!=='none'&&isInCapturedPixels(e,r);}
        var images=Array.from(root.querySelectorAll('img'));if(root.tagName==='IMG')images.push(root);
        images.forEach(function(i){if(visible(i)&&(!i.complete||i.naturalWidth===0))p++});
        // Some CSS, such as all-small-caps, cannot serialize as the font shorthand.
        // Retain face selection in that case so an unused weight cannot block the screenshot.
        var stretchKeywords={'50%':'ultra-condensed','62.5%':'extra-condensed','75%':'condensed','87.5%':'semi-condensed','100%':'normal','112.5%':'semi-expanded','125%':'expanded','150%':'extra-expanded','200%':'ultra-expanded'};
        function rebuildFontQuery(style){
            var stretch=style.fontStretch||'normal';
            if(stretch.endsWith('%'))stretch=stretchKeywords[stretch]||'normal';
            return [style.fontStyle,style.fontWeight,stretch,style.fontSize,style.fontFamily].join(' ');
        }
        var walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT),n,count=0;
        while((n=walker.nextNode())&&count++<5000){
            var e=n.parentElement;if(!n.nodeValue.trim()||!e||!visible(e))continue;
            var style=getComputedStyle(e),font=style.font;
            fontChecks.push([font||rebuildFontQuery(style),n.nodeValue.slice(0,128),font?0:1]);
        }
        var fonts=0,failure=0,rebuilt=0;
        if(document.fonts)fontChecks.some(function(q){
            try{
                if(document.fonts.check(q[0],q[1]))return false;
                if(document.fonts.status==='loading'){fonts++;return false;}
                failure=1;
            }catch(e){failure=e&&e.name==='SyntaxError'?2:3;}
            rebuilt=q[2];
            return true;
        });
        return [p,fonts,document.documentElement.clientWidth,Math.min(Math.max(document.documentElement.scrollHeight,document.body.scrollHeight),${bounds.y + bounds.height}),failure,rebuilt].join(',')
    })()"""

    private suspend fun awaitResources(bounds: Rectangle, selector: String, epoch: Long, scope: PageCaptureHealth.DependencyScope) {
        val started = android.os.SystemClock.elapsedRealtime()
        var quiet = 0
        var previous = ""
        while (true) {
            checkNavigation()
            val state = evaluate(resourceScript(bounds, selector)).trim('"').split(',').mapNotNull(String::toDoubleOrNull)
            if (state.size != 6) notReady(CaptureIssue.VIEWPORT_NOT_READY)
            val fontFailure = when (state[4]) {
                1.0 -> FontProbeFailureReason.MATCH_NOT_READY
                2.0 -> FontProbeFailureReason.INVALID_QUERY
                3.0 -> FontProbeFailureReason.CHECK_ERROR
                else -> null
            }
            if (fontFailure != null) {
                val resource = CaptureResourceFailure(CaptureResourceKind.FONT, fontProbe = FontProbeFailure(fontFailure, rebuiltShorthand = state[5] == 1.0))
                notReady(CaptureIssue.RESOURCE_FAILED, listOf(resource))
            }
            val signature = state.subList(2, 4).joinToString(",") + if (selector.isBlank()) "" else readClip(selector).joinToString(",")
            quiet = if (state[0] == 0.0 && state[1] == 0.0 && signature == previous) quiet + 1 else 0
            previous = signature
            if (quiet >= 2) return
            if (android.os.SystemClock.elapsedRealtime() - started >= READY_TIMEOUT_MS) {
                diagnosticCapture(captureContext(selector).copy(pendingImages = state[0].toInt(), fontsLoading = state[1] > 0).bounded())
                registerResourceDependencies(selector, bounds, scope)
                health.validate(epoch, captureExtent, scope = scope)
                notReady(
                    if (state[1] > 0) {
                        CaptureIssue.FONTS_LOADING
                    } else if (state[0] > 0) {
                        CaptureIssue.PENDING_IMAGES
                    } else {
                        CaptureIssue.LAYOUT_CHANGED
                    },
                )
            }
            delay(250)
        }
    }

    /**
     * Pages keep filling themselves in after they look settled: blocks fetched late (related articles, comments) and
     * script upgrades of the first HTML (Wikipedia's mobile site turns plain sections into collapsible ones). Neither
     * shows up as a pending image or a taller page. Continue once no new request has started and the page's scripts
     * have not changed its content for about [QUIET_POLLS] polls, or after [NETWORK_QUIET_MAX_MS] regardless: best
     * effort only, so a page that polls or animates forever is still captured.
     */
    private suspend fun awaitNetworkQuiet() {
        // Counts the page's own content changes; style changes (carousels, our cleanup) are not content.
        evaluate("(function(){if(window.__cdQuiet)return 0;window.__cdMutations=0;window.__cdQuiet=new MutationObserver(function(r){window.__cdMutations+=r.length});window.__cdQuiet.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['class','hidden','open','aria-expanded','src','srcset']});return 0;})()")
        try {
            val started = android.os.SystemClock.elapsedRealtime()
            var previous = ""
            var quiet = 0
            while (android.os.SystemClock.elapsedRealtime() - started < NETWORK_QUIET_MAX_MS) {
                checkNavigation()
                val activity =
                    evaluate("performance.getEntriesByType('resource').length+','+(window.__cdMutations||0)")
                quiet = if (activity == previous) quiet + 1 else 0
                previous = activity
                if (quiet >= QUIET_POLLS) return
                delay(200)
            }
        } finally {
            evaluate("(function(){if(window.__cdQuiet){window.__cdQuiet.disconnect();delete window.__cdQuiet;}return 0;})()")
        }
    }

    private suspend fun captureContext(selector: String): CaptureDiagnostic {
        val values = evaluate("""[document.documentElement.clientWidth,window.visualViewport?visualViewport.width:innerWidth,window.devicePixelRatio||1,document.fonts&&document.fonts.status==='loading'?1:0].join(',')""")
            .trim('"').split(',').mapNotNull { it.toDoubleOrNull()?.takeIf(Double::isFinite) }
        val region = if (selector.isNotBlank()) regionState(selector) else emptyList()
        fun value(i: Int, factor: Int = 1) = (values.getOrElse(i) { 0.0 } * factor).roundToInt()

        @Suppress("DEPRECATION")
        val scale = (view.scale * 1000).roundToInt()
        return CaptureDiagnostic(
            view.width, view.height, value(0, 10), value(1, 10), scale, value(2, 1000), region.firstOrNull()?.toInt()?.coerceAtLeast(0) ?: 0,
            if (region.size == 6) ((region[3] - region[1]) * 10).roundToInt() else 0,
            if (region.size == 6) ((region[4] - region[2]) * 10).roundToInt() else 0, value(3) > 0, 0,
        ).bounded()
    }

    private suspend fun awaitVisualState() = withTimeout(2_000) {
        suspendCancellableCoroutine<Unit> { continuation ->
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

    private companion object {
        const val READY_TIMEOUT_MS = 8_000L
        const val NETWORK_QUIET_MAX_MS = 1_500L
        const val QUIET_POLLS = 3
        const val PIXEL_SCALE = 2.0
        const val MAX_PIXELS = 7_000_000L
    }

    /**
     * WebView reports only failing HTTP statuses. The document's own status comes from the Navigation Timing entry,
     * so a report shows "200" for a page that loaded and a check that failed later, as the plain fetch already does.
     */
    private suspend fun recordDocumentStatus() {
        val status = try {
            evaluate("(performance.getEntriesByType('navigation')[0]||{}).responseStatus||0").toIntOrNull() ?: 0
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            0
        }
        if (status > 0) diagnosticMetric(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, status)
    }

    private suspend fun evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { result ->
            if (result == null || result == "null") continuation.context[DiagnosticSpan]?.callback(DiagnosticStage.EXTRACT_TEXT, DiagnosticMetric.CALLBACK_NULL, 1)
            if (continuation.isActive) continuation.resumeWith(Result.success(result.orEmpty()))
        }
    }

    private suspend fun navigate(url: String) = withTimeout(30_000) {
        val trace = currentCoroutineContext()[DiagnosticSpan]
        val loaded = CompletableDeferred<Unit>()
        navigationFailure = null
        health.begin(url)
        fun fail(error: Exception) {
            // Readiness can complete before a redirect or HTTP error arrives. Keep the
            // error for the capture as well as the initial navigation awaiter.
            navigationFailure = error
            loaded.completeExceptionally(error)
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? {
                health.request(request.url.toString(), request.isForMainFrame)
                return null
            }
            override fun onPageStarted(webView: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                health.started(url)
            }
            override fun onPageCommitVisible(webView: WebView, url: String?) {
                health.committed(url)
            }
            override fun onPageFinished(webView: WebView, finishedUrl: String) {
                if (finishedUrl == "about:blank" || finishedUrl != webView.url) return
                health.finished()
                trace?.callback(DiagnosticStage.DOM_READY, DiagnosticMetric.DOM_READY, 1)
                loaded.complete(Unit)
            }

            override fun onReceivedError(webView: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame) health.resourceError(request.url.toString())
                if (request.isForMainFrame) {
                    trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.WEBVIEW_ERROR, error.errorCode)
                    fail(IOException(error.description.toString()))
                }
            }

            override fun onReceivedHttpError(webView: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (!request.isForMainFrame) health.resourceError(request.url.toString(), httpStatus = response.statusCode)
                if (request.isForMainFrame) {
                    trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, response.statusCode)
                    fail(com.bernaferrari.changedetection.error.NetworkError(response.statusCode, "HTTP ${response.statusCode}"))
                }
            }

            override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
                // Always refuse invalid TLS. Only a failed main document aborts navigation; captured assets are checked later.
                handler.cancel()
                val tls = health.ssl(error.url, error.primaryError, (0..5).filter(error::hasError))
                // A few refused resources explain a capture; dozens would crowd the check's own stages out of the report.
                if (tls.blocking || health.blockedResources() <= 3) trace?.tls(tls)
                if (tls.blocking) fail(SSLException("The page has an invalid TLS certificate"))
            }

            override fun onRenderProcessGone(webView: WebView, detail: RenderProcessGoneDetail): Boolean {
                trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.RENDERER_CRASH, if (detail.didCrash()) 1 else 0)
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
            // Allow the main navigation to finish while keeping usable pages with a stalled
            // secondary resource capturable. A redirect starts a fresh grace period.
            var readySince: kotlin.time.TimeMark? = null
            var observedEpoch = health.epoch()
            while (!loaded.isCompleted) {
                val ready = evaluate("location.href !== 'about:blank' && document.body !== null && document.readyState !== 'loading'")
                val epoch = health.epoch()
                if (observedEpoch != epoch || ready != "true") readySince = null
                observedEpoch = epoch
                if (ready == "true") {
                    val mark = readySince ?: kotlin.time.TimeSource.Monotonic.markNow().also { readySince = it }
                    if (mark.elapsedNow().inWholeMilliseconds >= 1_500) loaded.complete(Unit)
                }
                if (!loaded.isCompleted) delay(100)
            }
            loaded.await()
            recordDocumentStatus()
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
