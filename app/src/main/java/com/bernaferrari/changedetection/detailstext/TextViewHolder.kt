package com.bernaferrari.changedetection.detailstext

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.consume
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.getColorFromAttr
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.ui.RecyclerViewItemListener
import kotlinx.android.synthetic.main.item_text_selector.view.*

/**
 * A simple ViewHolder that can bind a Cheese item. It also accepts null items since the data may
 * not have been fetched before it is bound.
 */
class TextViewHolder(
    parent: ViewGroup,
    val adapter: TextAdapter,
    val callback: RecyclerViewItemListener
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.item_text_selector, parent, false)
) {

    init {
        itemView.setOnClickListener {
            callback.onClickListener(this)
        }

        itemView.setOnLongClickListener {
            consume { callback.onLongClickListener(this) }
        }
    }

    var snap: Snap? = null

    fun setColor(color: ItemSelected) {
        colorSelected = color
        adapter.setColor(color, adapterPosition)
    }

    /**
     * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
     * ViewHolder when Item is loaded.
     */
    fun bindTo(snap: Snap?, colorSelected: ItemSelected) {
        this.snap = snap
        this.colorSelected = colorSelected

        this.snap?.also {
            stringFromTimeAgo = it.timestamp.convertTimestampToDate()
            readableFileSize = it.contentSize.readableFileSize()
        } ?: return

        itemView.subtitleTextView.text = stringFromTimeAgo
        itemView.titleTextView.text = readableFileSize

        bindColors()
    }

    var colorSelected = ItemSelected.NONE
        private set

    var readableFileSize = ""
    var stringFromTimeAgo = ""

    private fun bindColors() {
        val context = itemView.container.context

        when (colorSelected) {
            ItemSelected.REVISED -> setCardBackgroundAnimated(
                itemView.container,
                context.getColorFromAttr(R.attr.code_addition_diff).toDrawable()
            )
            ItemSelected.ORIGINAL -> setCardBackgroundAnimated(
                itemView.container,
                context.getColorFromAttr(R.attr.code_deletion_diff).toDrawable()
            )
            else -> setCardBackgroundAnimated(
                itemView.container,
                context.getColorFromAttr(R.attr.windowBackground).toDrawable()
            )
        }
    }

    private fun setCardBackgroundAnimated(
        cardView: androidx.cardview.widget.CardView,
        color: Drawable
    ) {
        cardView.background = TransitionDrawable(arrayOf(cardView.background, color)).apply {
            startTransition(100)
        }
    }
}
