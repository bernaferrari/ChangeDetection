package com.bernaferrari.changedetection.ui

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewWithEmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var emptyLayout: EmptyLayout? = null

    private val observer =
        object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateEmptyView()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            updateEmptyView()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            updateEmptyView()
        }
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        super.setAdapter(adapter)
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer)
            observer.onChanged()
        }
    }

    fun updateEmptyView() {
        if (emptyLayout == null) return

        if (adapter != null) {
            updateVisibility(adapter!!.itemCount != 0)
        } else {
            updateVisibility(false)
        }
    }

    private fun updateVisibility(showEmptyView: Boolean) {
        emptyLayout?.isVisible = !showEmptyView
    }

    /**
     * Sets the emptyView.
     */
    fun setEmptyView(emptyView: EmptyLayout) {
        this.emptyLayout = emptyView
        updateEmptyView()
    }
}
