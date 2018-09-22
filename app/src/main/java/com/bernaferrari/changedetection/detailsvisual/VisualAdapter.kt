package com.bernaferrari.changedetection.detailsvisual

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.RecyclerViewItemListener
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.util.GlideRequests

class VisualAdapter(
    val callback: RecyclerViewItemListener,
    private val itemHeight: Int,
    private val itemWidth: Int,
    private val context: Context,
    private val isPdf: Boolean,
    private val glide: GlideRequests
) :
    PagedListAdapter<Snap, VisualViewHolder>(
        diffCallback
    ) {

    override fun onBindViewHolder(holder: VisualViewHolder, position: Int) {
        holder.bindTo(getItem(position), position, context, glide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisualViewHolder =
        VisualViewHolder(parent, itemHeight, itemWidth, isPdf, callback)

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