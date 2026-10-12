package com.bernaferrari.changedetection.browser

import kotlin.math.roundToInt

internal data class CaptureBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** A selected part is all-or-nothing, even when full-page captures have a height budget. */
internal fun selectedCaptureHeight(top: Double, bottom: Double, maximum: Double): Double {
    require(top.isFinite() && bottom.isFinite() && maximum.isFinite() && top >= 0 && bottom > top && maximum > 0) { "The selected part is outside the page." }
    val height = bottom - top
    require(height <= maximum) { "The selected area is too large for a screenshot. Choose a smaller part." }
    return height
}

internal data class ViewportCaptureRect(val left: Double, val top: Double, val right: Double, val bottom: Double)

/** CSS selections must fit the visible horizontal viewport and the document, without clipping. */
internal fun selectedViewportCaptureRect(left: Double, top: Double, right: Double, bottom: Double, viewportWidth: Double, pageHeight: Double, pointsPerCssPixel: Double, maximumPoints: Double): ViewportCaptureRect {
    require(listOf(left, top, right, bottom, viewportWidth, pageHeight, pointsPerCssPixel).all { it.isFinite() }) { "The selected part is outside the page." }
    require(viewportWidth > 0 && pageHeight > 0 && pointsPerCssPixel > 0 && left >= 0 && right <= viewportWidth && right > left && top >= 0 && bottom <= pageHeight && bottom > top) { "The selected part is outside the captured page." }
    selectedCaptureHeight(top * pointsPerCssPixel, bottom * pointsPerCssPixel, maximumPoints)
    require((right - left) * pointsPerCssPixel * 2 >= 1 && (bottom - top) * pointsPerCssPixel * 2 >= 1) { "The selected part is too small to capture." }
    return ViewportCaptureRect(left, top, right, bottom)
}

/** Reject invalid selections; clamping an absent rectangle can turn it into unrelated content. */
internal fun selectedCaptureBounds(rect: List<Double>, cssToView: Double, width: Int, height: Int): CaptureBounds {
    require(rect.size == 6 && rect.all { it.isFinite() } && cssToView.isFinite() && cssToView > 0)
    require(rect[0] >= 0 && rect[1] >= 0 && rect[2] > rect[0] && rect[3] > rect[1]) { "The selected part is outside the page." }
    val bounds = CaptureBounds((rect[0] * cssToView).roundToInt(), (rect[1] * cssToView).roundToInt(), (rect[2] * cssToView).roundToInt(), (rect[3] * cssToView).roundToInt())
    require(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height && bounds.right > bounds.left && bounds.bottom > bounds.top) { "The selected part is outside the captured page." }
    return bounds
}
