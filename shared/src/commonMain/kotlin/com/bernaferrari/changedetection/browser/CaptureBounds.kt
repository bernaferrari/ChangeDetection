/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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

/** Reject invalid selections; clamping an absent rectangle can turn it into unrelated content. */
internal fun selectedCaptureBounds(rect: List<Double>, cssToView: Double, width: Int, height: Int): CaptureBounds {
    require(rect.size == 6 && rect.all { it.isFinite() } && cssToView.isFinite() && cssToView > 0)
    require(rect[0] >= 0 && rect[1] >= 0 && rect[2] > rect[0] && rect[3] > rect[1]) { "The selected part is outside the page." }
    val bounds = CaptureBounds((rect[0] * cssToView).roundToInt(), (rect[1] * cssToView).roundToInt(), (rect[2] * cssToView).roundToInt(), (rect[3] * cssToView).roundToInt())
    require(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height && bounds.right > bounds.left && bounds.bottom > bounds.top) { "The selected part is outside the captured page." }
    return bounds
}
