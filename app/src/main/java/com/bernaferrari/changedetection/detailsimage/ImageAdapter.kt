package com.bernaferrari.changedetection.detailsimage

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.detailstext.TextFragment
import com.bernaferrari.changedetection.util.GlideRequests

class ImageAdapter(
    val callback: TextFragment.Companion.RecyclerViewItemListener,
    private val itemHeight: Int,
    private val itemWidth: Int,
    private val glide: GlideRequests
) :
    PagedListAdapter<Snap, ImageViewHolder>(
        diffCallback
    ) {

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindTo(getItem(position), position, glide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder =
        ImageViewHolder(parent, itemHeight, itemWidth, callback)

    fun getItemFromAdapter(position: Int): Snap? {
        return this.getItem(position)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Snap>() {
            override fun areItemsTheSame(
                oldItem: Snap,
                newItem: Snap
            ): Boolean =
                oldItem.snapId == newItem.snapId

            override fun areContentsTheSame(
                oldItem: Snap,
                newItem: Snap
            ): Boolean =
                oldItem == newItem
        }
    }
}