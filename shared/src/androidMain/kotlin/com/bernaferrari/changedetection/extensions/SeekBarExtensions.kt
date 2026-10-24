package com.bernaferrari.changedetection.extensions

import android.widget.SeekBar

/**
 * Set the listener to this SeekBar using the provided actions.
 */
fun SeekBar.setOnSeekBarChangeListener(
    onStartTracking: ((seekBar: SeekBar) -> Unit)? = null,
    onStopTracking: ((seekBar: SeekBar) -> Unit)? = null,
    onChanged: ((seekBar: SeekBar, progress: Int, fromUser: Boolean) -> Unit)? = null,
): SeekBar.OnSeekBarChangeListener {
    val listener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean,
            ) {
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
