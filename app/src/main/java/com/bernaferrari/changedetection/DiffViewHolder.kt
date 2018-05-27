package com.bernaferrari.changedetection

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.bernaferrari.changedetection.data.DiffWithoutValue
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import kotlinx.android.synthetic.main.diff_item.view.*
import java.text.DecimalFormat
import java.util.*

/**
 * A simple ViewHolder that can bind a Cheese item. It also accepts null items since the data may
 * not have been fetched before it is bound.
 */
class DiffViewHolder(parent: ViewGroup, val adapter: DiffAdapter, val callback: DiffFragment.RecyclerViewItemListener) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.diff_item, parent, false)) {

    init {
        itemView.setOnClickListener {
            callback.onClickListener(this)
        }

        itemView.setOnLongClickListener {
            callback.onLongClickListener(this)
            true
        }
    }

    var diff : DiffWithoutValue? = null

    fun setColor(color: Int){
        colorSelected = color
        adapter.setColor(color, adapterPosition)
    }

    /**
     * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
     * ViewHolder when Item is loaded.
     */
    fun bindTo(diff : DiffWithoutValue?, colorSelected: Int) {
        this.diff = diff
        this.colorSelected = colorSelected

        if (this.diff == null){
            return
        }

        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        stringFromTimeAgo = TimeAgo.using(this.diff!!.timestamp, messages)
        readableFileSize = readableFileSize(this.diff!!.size)

        itemView.subtitleTextView.text = stringFromTimeAgo
        itemView.titleTextView.text = readableFileSize

        bindColors()
    }

    var colorSelected = 0
    private set

    var readableFileSize = ""
    var stringFromTimeAgo = ""

    private fun bindColors() {
        val context = itemView.container.context

        when (colorSelected) {
            1 -> setCardBackgroundAnimated(
                itemView.container,
                ContextCompat.getColor(context, R.color.md_orange_200).toDrawable()
            )
            2 -> setCardBackgroundAnimated(
                itemView.container,
                ContextCompat.getColor(context, R.color.md_amber_200).toDrawable()
            )
            else -> setCardBackgroundAnimated(
                itemView.container,
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