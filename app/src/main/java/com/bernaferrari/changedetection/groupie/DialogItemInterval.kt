package com.bernaferrari.changedetection.groupie

import android.content.Context
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.doOnChanged
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.settings_item_interval.*

/**
 * Creates an interval item. This will be used on settings to track the sync period.
 *
 * @param title the item title
 * @param initialDelay the initial delay when item is created
 * @param listener callback for the result
 */
class DialogItemInterval(
    val title: String,
    val initialDelay: Int,
    val listener: (Long) -> (Unit)
) : Item() {
    val minutes = arrayOf(15, 30, 45, 60, 120, 240, 360, 720, 1440, 2880)
    var progress: Int = minutes.indexOfFirst { it == initialDelay }

    override fun getLayout(): Int {
        return R.layout.settings_item_interval
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.seekBar.progress = progress

        viewHolder.seekBar.doOnChanged { _, seekbar_progress, _ ->
            progress = seekbar_progress
            listener.invoke(minutes[progress].toLong())
            viewHolder.progress.text = getTimeString(viewHolder.seekBar.context)
        }
        viewHolder.progress.text = getTimeString(viewHolder.seekBar.context)
        viewHolder.title.text = title
    }

    private fun getTimeString(context: Context): String = minutes[progress].let {
        when {
            it < 60 -> "$it " + context.getString(R.string.min)
            it == 60 -> context.getString(R.string.hour)
            else -> "${it / 60} " + context.getString(R.string.hours)
        }
    }
}
