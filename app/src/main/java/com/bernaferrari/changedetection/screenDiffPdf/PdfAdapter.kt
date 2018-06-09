package com.bernaferrari.changedetection.screenDiffPdf

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.screenDiffText.TextFragment

class PdfAdapter(
    val callback: TextFragment.Companion.RecyclerViewItemListener,
    private val itemHeight: Int,
    private val itemWidth: Int,
    private val context: Context
) :
    PagedListAdapter<Snap, PdfViewHolder>(
        diffCallback
    ) {

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bindTo(getItem(position), position, context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder =
        PdfViewHolder(parent, itemHeight, itemWidth, callback)

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