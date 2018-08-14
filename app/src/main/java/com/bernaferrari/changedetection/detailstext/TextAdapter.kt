package com.bernaferrari.changedetection.detailstext

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.data.Snap

/**
 * A simple PagedListAdapter that binds Snap items into CardViews.
 *
 * PagedListAdapter is a RecyclerView.Adapter base class which can present the content of PagedLists
 * in a RecyclerView. It requests new pages as the user scrolls, and handles new PagedLists by
 * computing list differences on a background thread, and dispatching minimal, efficient updates to
 * the RecyclerView to ensure minimal UI thread work.
 **/
class TextAdapter(val callback: TextFragment.Companion.RecyclerViewItemListener) :
    PagedListAdapter<Snap, TextViewHolder>(
        diffCallback
    ) {
    val colorSelected = mutableMapOf<Int, ItemSelected>()

    fun setColor(color: ItemSelected, position: Int) {
        colorSelected[position] = color
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bindTo(getItem(position), colorSelected[position] ?: ItemSelected.NONE)
    }

    fun getItemFromAdapter(position: Int): Snap? {
        return this.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder =
        TextViewHolder(parent, this, callback)

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
