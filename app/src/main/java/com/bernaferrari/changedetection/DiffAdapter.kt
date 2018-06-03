package com.bernaferrari.changedetection

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.data.MinimalDiff

/**
 * A simple PagedListAdapter that binds Cheese items into CardViews.
 * <p>
 * PagedListAdapter is a RecyclerView.Adapter base class which can present the content of PagedLists
 * in a RecyclerView. It requests new pages as the user scrolls, and handles new PagedLists by
 * computing list differences on a background thread, and dispatching minimal, efficient updates to
 * the RecyclerView to ensure minimal UI thread work.
 * <p>
 * If you want to use your own Adapter base class, try using a PagedListAdapterHelper inside your
 * adapter instead.
 *
 * @see android.arch.paging.PagedListAdapter
 * @see android.arch.paging.AsyncPagedListDiffer
 */
class DiffAdapter(val callback: DiffFragment.RecyclerViewItemListener) :
    PagedListAdapter<MinimalDiff, DiffViewHolder>(
        diffCallback
    ) {
    val colorSelected = mutableMapOf<Int, Int>()

    fun setColor(color: Int, position: Int) {
        colorSelected[position] = color
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: DiffViewHolder, position: Int) {
        holder.bindTo(getItem(position), colorSelected.getOrDefault(position, 0))
    }

    fun getItemFromAdapter(position: Int): MinimalDiff? {
        return this.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiffViewHolder =
        DiffViewHolder(parent, this, callback)

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MinimalDiff>() {
            override fun areItemsTheSame(
                oldItem: MinimalDiff,
                newItem: MinimalDiff
            ): Boolean =
                oldItem.diffId == newItem.diffId

            override fun areContentsTheSame(
                oldItem: MinimalDiff,
                newItem: MinimalDiff
            ): Boolean =
                oldItem == newItem
        }
    }
}
