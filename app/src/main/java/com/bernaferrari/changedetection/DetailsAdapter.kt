package com.bernaferrari.changedetection

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.data.MinimalSnap

/**
 * A simple PagedListAdapter that binds Snap items into CardViews.
 *
 * PagedListAdapter is a RecyclerView.Adapter base class which can present the content of PagedLists
 * in a RecyclerView. It requests new pages as the user scrolls, and handles new PagedLists by
 * computing list differences on a background thread, and dispatching minimal, efficient updates to
 * the RecyclerView to ensure minimal UI thread work.
 **/
class DetailsAdapter(val callback: DetailsFragment.RecyclerViewItemListener) :
    PagedListAdapter<MinimalSnap, DetailsViewHolder>(
        diffCallback
    ) {
    val colorSelected = mutableMapOf<Int, Int>()

    fun setColor(color: Int, position: Int) {
        colorSelected[position] = color
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: DetailsViewHolder, position: Int) {
        holder.bindTo(getItem(position), colorSelected.getOrDefault(position, 0))
    }

    fun getItemFromAdapter(position: Int): MinimalSnap? {
        return this.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsViewHolder =
        DetailsViewHolder(parent, this, callback)

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MinimalSnap>() {
            override fun areItemsTheSame(
                oldItem: MinimalSnap,
                newItem: MinimalSnap
            ): Boolean =
                oldItem.snapId == newItem.snapId

            override fun areContentsTheSame(
                oldItem: MinimalSnap,
                newItem: MinimalSnap
            ): Boolean =
                oldItem == newItem
        }
    }
}
