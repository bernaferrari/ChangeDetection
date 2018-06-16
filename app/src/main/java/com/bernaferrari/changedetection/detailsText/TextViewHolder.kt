package com.bernaferrari.changedetection.detailsText

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import kotlinx.android.synthetic.main.details_item.view.*

/**
 * A simple ViewHolder that can bind a Cheese item. It also accepts null items since the data may
 * not have been fetched before it is bound.
 */
class TextViewHolder(
    parent: ViewGroup,
    val adapter: TextAdapter,
    val callback: TextFragment.Companion.RecyclerViewItemListener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.details_item, parent, false)
) {

    init {
        itemView.setOnClickListener {
            callback.onClickListener(this)
        }

        itemView.setOnLongClickListener {
            callback.onLongClickListener(this)
            true
        }
    }

    var snap: Snap? = null

    fun setColor(color: Int) {
        colorSelected = color
        adapter.setColor(color, adapterPosition)
    }

    /**
     * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
     * ViewHolder when Item is loaded.
     */
    fun bindTo(snap: Snap?, colorSelected: Int) {
        this.snap = snap
        this.colorSelected = colorSelected

        if (this.snap == null) {
            return
        }

        stringFromTimeAgo = this.snap!!.timestamp.convertTimestampToDate()
        readableFileSize = this.snap!!.contentSize.readableFileSize()

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
                ContextCompat.getColor(context,
                    R.color.md_orange_200
                ).toDrawable()
            )
            2 -> setCardBackgroundAnimated(
                itemView.container,
                ContextCompat.getColor(context,
                    R.color.md_amber_200
                ).toDrawable()
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
}