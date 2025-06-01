package com.biblialibras.android.common.view

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.SeekBar

/** Represents the width and height of something. */
data class Size(
    val width: Int,
    val height: Int
) {
    override fun toString() = "${width}x$height"
}

/**
 * Returns a [Size] containing the width and height of the receiving view.
 */
fun View.size() = Size(measuredWidth, measuredHeight)

/**
 * Sets the receiving view's visibility to [VISIBLE].
 */
fun View.show() {
    visibility = VISIBLE
}

/**
 * Sets the receiving view's visibility to [GONE].
 */
fun View.hide() {
    visibility = GONE
}

/**
 * If show is true, calls [show] on the receiving view - else calls [hide].
 */
fun View.showOrHide(show: Boolean) = if (show) show() else hide()

/**
 * Calls the [cb] callback when the receiving SeekBar's value is changed.
 */
fun SeekBar.onProgressChanged(cb: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) = cb(progress)

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
}
