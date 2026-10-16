package com.bernaferrari.changedetection.addedit

import android.view.View
import android.content.MutableContextWrapper
import com.bernaferrari.changedetection.browser.AndroidPageCapture
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion, onPreview: (CapturePreview) -> Unit): SyncEngine.FetchResult.Success? = withContext(Dispatchers.Main.immediate) {
    val view = state.nativeWebView
    val left = view.left
    val top = view.top
    val width = view.width
    val height = view.height
    val scrollX = view.scrollX
    val scrollY = view.scrollY
    val captureContext = view.context as? MutableContextWrapper ?: return@withContext null
    val originalContext = captureContext.baseContext
    try {
        if (region.isEmpty && width > 0 && height > 0) {
            // Reuse the viewport already on screen. This is display-only; saving joins the
            // full capture below and never persists these lower-resolution bytes.
            val scale = minOf(1f, 512f / width)
            val thumbnail = android.graphics.Bitmap.createBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
            try {
                android.graphics.Canvas(thumbnail).apply { scale(scale, scale); view.draw(this) }
                val bytes = withContext(Dispatchers.Default) {
                    java.io.ByteArrayOutputStream().use { output ->
                        check(thumbnail.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                        output.toByteArray()
                    }
                }
                onPreview(CapturePreview.Image(bytes, isPreparing = true))
            } finally {
                thumbnail.recycle()
            }
        }
        // Keep Chromium attached so it can acknowledge painted frames. During capture the
        // application context selects direct WebView drawing instead of copying the Activity
        // window (which also contains the loading overlay).
        captureContext.baseContext = originalContext.applicationContext
        // Match scheduled checks' viewport so the first comparison does not detect a layout change.
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 1080, 1920)
        val shot = AndroidPageCapture(view).captureLoaded(maxHeight = 10_000, hide = region.exclude, only = region.include, preservePage = true)
        SyncEngine.FetchResult.Success(shot.contentType, shot.bytes, capturedWithBrowser = true)
    } finally {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(left, top, left + width, top + height)
        captureContext.baseContext = originalContext
        view.scrollTo(scrollX, scrollY)
    }
}

internal actual fun createWatchWebView(param: com.multiplatform.webview.web.WebViewFactoryParam): com.multiplatform.webview.web.NativeWebView =
    android.webkit.WebView(MutableContextWrapper(param.context))
