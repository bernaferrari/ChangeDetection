package com.bernaferrari.base.view

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView

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

/**
 * Allows calls like
 *
 * `viewGroup.inflate(R.layout.foo)`
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}

/** Get text from a view, useful when fragment might not be available */
fun View.getText(@StringRes res: Int) = this.resources.getText(res)

/** Get onScroll callback */
inline fun RecyclerView.onScroll(crossinline body: (dx: Int, dy: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int
        ) {
            body(dx, dy)
        }
    })
}


/**
 * Detect when key is pressed.
 */
inline fun View.onKey(crossinline body: (KeyEvent) -> Boolean) {
    setOnKeyListener { _, _, event -> body(event) }
}


/**
 * this will scroll to wanted index + or - one, giving a margin of one and allowing user to
 * keep tapping in the same place and RecyclerView keep scrolling.
 */
internal fun RecyclerView.scrollToIndexWithMargins(
    previousIndex: Int,
    index: Int,
    size: Int
) {
    if (previousIndex == -1) {
        // This will run on first iteration
        when (index) {
            in 1 until (size - 1) -> this.scrollToPosition(index - 1)
            (size - 1) -> this.scrollToPosition(index)
            else -> this.scrollToPosition(0)
        }
    } else {
        when (index) {
            in 1 until previousIndex -> this.scrollToPosition(index - 1)
            in previousIndex until (size - 1) -> this.scrollToPosition(index + 1)
            else -> this.scrollToPosition(index)
        }
    }
}
