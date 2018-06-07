package com.bernaferrari.changedetection.screenDiffImage

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.screenDiffText.TextFragment
import com.bernaferrari.changedetection.util.GlideRequests
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.diff_image_item_paging.view.*

class ImageViewHolder(
    parent: ViewGroup,
    val adapter: ImageAdapter,
    val itemHeight: Int,
    val callback: TextFragment.Companion.RecyclerViewItemListener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(
        R.layout.diff_image_item_paging,
        parent,
        false
    ).apply {
        val params = FrameLayout.LayoutParams(
            itemHeight,
            itemHeight
        )
        params.gravity = Gravity.CENTER
        this.layoutParams = params
    }
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

    var currentSnap: Snap? = null

    fun bindTo(
        snap: Snap?,
        glide: GlideRequests
    ) {
        currentSnap = snap
        if (snap != null) {
            glide
                .load(snap.content)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this.itemView.imageView)
        } else {
            glide.clear(this.itemView.imageView)
        }
    }
}