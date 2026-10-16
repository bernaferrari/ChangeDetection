package com.bernaferrari.changedetection.addedit

import android.view.View
import android.content.MutableContextWrapper
import com.bernaferrari.changedetection.browser.AndroidPageCapture
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun captureLoadedPageScreenshot(state: WebViewState, region: TextRegion): SyncEngine.FetchResult.Success? = withContext(Dispatchers.Main.immediate) {
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
