package com.bernaferrari.changedetection.extensions

import android.widget.SeekBar

/**
 * Add an action which will be invoked when the SeekBar has started tracking touch.
 *
 * @return the [SeekBar.OnSeekBarChangeListener] added to the SeekBar
 * @see SeekBar.OnSeekBarChangeListener.onStartTrackingTouch
 */
fun SeekBar.doOnStartTracking(action: (seekBar: SeekBar) -> Unit): SeekBar.OnSeekBarChangeListener =
    setOnSeekBarChangeListener(onStartTracking = action)

/**
 * Add an action which will be invoked when the SeekBar has stopped tracking touch.
 *
 * @return the [SeekBar.OnSeekBarChangeListener] added to the SeekBar
 * @see SeekBar.OnSeekBarChangeListener.onStopTrackingTouch
 */
fun SeekBar.doOnStopTracking(action: (seekBar: SeekBar) -> Unit): SeekBar.OnSeekBarChangeListener =
    setOnSeekBarChangeListener(onStopTracking = action)

/**
 * Add an action which will be invoked when the SeekBar has the progress changed.
 *
 * @return the [SeekBar.OnSeekBarChangeListener] added to the SeekBar
 * @see SeekBar.OnSeekBarChangeListener.onProgressChanged
 */
fun SeekBar.doOnChanged(action: (seekBar: SeekBar, progress: Int, fromUser: Boolean) -> Unit):
        SeekBar.OnSeekBarChangeListener = setOnSeekBarChangeListener(onChanged = action)

/**
 * Set the listener to this SeekBar using the provided actions.
 */
fun SeekBar.setOnSeekBarChangeListener(
    onStartTracking: ((seekBar: SeekBar) -> Unit)? = null,
    onStopTracking: ((seekBar: SeekBar) -> Unit)? = null,
    onChanged: ((seekBar: SeekBar, progress: Int, fromUser: Boolean) -> Unit)? = null
): SeekBar.OnSeekBarChangeListener {
    val listener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            onChanged?.invoke(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            onStartTracking?.invoke(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            onStopTracking?.invoke(seekBar)
        }
    }
    setOnSeekBarChangeListener(listener)
    return listener
}
