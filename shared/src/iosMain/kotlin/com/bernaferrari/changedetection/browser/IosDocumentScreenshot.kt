@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.browser

import com.bernaferrari.kmpwright.core.Rectangle
import com.bernaferrari.kmpwright.core.captureWebViewDocument
import platform.WebKit.WKWebView
import kotlin.math.roundToInt

internal suspend fun captureIosDocumentScreenshot(
    web: WKWebView,
    clip: IosPageCapture.Clip?,
    viewportCssWidth: Double,
    pointsPerCss: Double,
    maxPageCssHeight: Double,
): ByteArray {
    val scale = pointsPerCss * 2
    val visibleWidth = (viewportCssWidth * scale).roundToInt()
    val pixels = minOf(9_360_000L, visibleWidth.toLong() * (maxPageCssHeight * scale).roundToInt())
    val bytes = try {
        captureWebViewDocument(
            web,
            clip = clip?.let { Rectangle(it.left, it.top, it.right - it.left, it.bottom - it.top) },
            pixelsPerCssPixel = scale,
            maxPixels = if (clip == null) pixels else 9_360_000L,
            maxHeight = (maxPageCssHeight * scale).roundToInt(),
            truncateFullPage = true,
            viewportWidthOnly = true,
        ).bytes
    } catch (error: IllegalStateException) {
        if (error.isScreenshotLayoutChange()) throw CaptureNotReadyException(CaptureIssue.LAYOUT_CHANGED)
        throw error
    }
    if (bytes.size > com.bernaferrari.changedetection.util.MAX_SNAPSHOT_BYTES) {
        throw com.bernaferrari.changedetection.error.SyncError(com.bernaferrari.changedetection.error.SyncError.Reason.CONTENT_TOO_LARGE, "Page capture is too large")
    }
    return bytes
}
