package com.bernaferrari.changedetection.groupie

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Diff
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.diff_item.*
import java.text.DecimalFormat
import java.util.*


class DiffItem(
    val diff: Diff
) : Item() {
    var colorSelected = 0
    var readableFileSize = ""
    var stringFromTimeAgo = ""

    fun setColor(color: Int){
        colorSelected = color
        notifyChanged()
    }

    override fun getLayout() = R.layout.diff_item

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        bind(holder, position)
        colorSelected = (payloads.firstOrNull() as? Int) ?: colorSelected
        bindColors(holder)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        stringFromTimeAgo = TimeAgo.using(diff.timestamp, messages)
        readableFileSize = readableFileSize(diff.size)

        viewHolder.subtitleTextView.text = stringFromTimeAgo
        viewHolder.titleTextView.text = readableFileSize
    }

    private fun bindColors(holder: ViewHolder) {
        val context = holder.containerView.context

        when (colorSelected) {
            1 -> setCardBackgroundAnimated(
                holder.container,
                ContextCompat.getColor(context, R.color.md_orange_200).toDrawable()
            )
            2 -> setCardBackgroundAnimated(
                holder.container,
                ContextCompat.getColor(context, R.color.md_amber_200).toDrawable()
            )
            else -> setCardBackgroundAnimated(
                holder.container,
                ContextCompat.getColor(context, R.color.grey_100).toDrawable()
            )
        }
    }

    private fun setCardBackgroundAnimated(cardView: CardView, color: Drawable) {
        cardView.background = TransitionDrawable(arrayOf(cardView.background, color)).apply {
            startTransition(100)
        }
    }

    private fun readableFileSize(size: Int): String {
        if (size <= 0) return "EMPTY"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
    }
}
