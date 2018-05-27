package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.doOnChanged
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_interval.*

class DialogItemInterval(
    val title: String,
    val initialDelay: Int,
    val listener: (Long) -> (Unit)
) : Item() {
    val minutes = arrayOf(15, 30, 45, 60, 120, 240, 360, 720, 1440, 2880)
    var progress: Int = minutes.indexOfFirst { it == initialDelay }

    override fun getLayout(): Int {
        return R.layout.dialog_item_interval
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.seekBar.progress = progress

        viewHolder.seekBar.doOnChanged { seekBar, seekbar_progress, fromUser ->
            progress = seekbar_progress
            listener.invoke(minutes[progress].toLong())
            viewHolder.progress.text = getTimeString()
        }
        viewHolder.progress.text = getTimeString()
        viewHolder.title.text = title
    }

    private fun getTimeString(): String {

        return minutes[progress].let {
            when {
                it < 60 -> return@let "$it min"
                it == 60 -> return@let "1 hour"
                else -> return@let "${it / 60} hours"
            }
        }
    }
}
