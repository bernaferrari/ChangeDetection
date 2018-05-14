package com.example.changedetection.groupie

import android.support.v4.content.ContextCompat
import com.example.changedetection.R
import com.example.changedetection.data.Diff
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

    override fun getLayout() = R.layout.diff_item

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        bind(holder, position)
        colorSelected = (payloads.firstOrNull() as? Int) ?: colorSelected
        bindColors(holder)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        val text = TimeAgo.using(diff.timestamp, messages)
        viewHolder.titleTextView.text = readableFileSize(diff.value.count())
        viewHolder.subtitleTextView.text = text
    }

    private fun bindColors(holder: ViewHolder) {
        val context = holder.containerView.context

        when (colorSelected){
            1 -> holder.container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_amber_200))
            2 -> holder.container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_orange_200))
            else -> holder.container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey_100))
        }
    }

    private fun readableFileSize(size: Int): String {
        if (size <= 0) return "0"
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
