package com.bernaferrari.changedetection.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View


class RecyclerViewWithEmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var emptyLayout: EmptyLayout? = null

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            showEmptyView()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            showEmptyView()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            showEmptyView()
        }
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        super.setAdapter(adapter)
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer)
            observer.onChanged()
        }
    }

    private fun showEmptyView() {
        if (emptyLayout == null) return

        if (adapter != null) {
            updateVisibility(adapter!!.itemCount != 0)
        } else {
            updateVisibility(false)
        }
    }

    private fun updateVisibility(showEmptyView: Boolean) {
        emptyLayout?.visibility = if (!showEmptyView) View.VISIBLE else View.GONE
    }

    fun setEmptyView(emptyView: EmptyLayout) {
        this.emptyLayout = emptyView
        showEmptyView()
    }
}
