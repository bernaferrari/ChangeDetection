package com.bernaferrari.changedetection.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

/*
  Created by bernardoferrari on 09/11/17.
  Inspired on FastHub implementation.
  TODO need to change some variable names and update some methods
 */

class DynamicRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var emptyView: StateLayout? = null
    private var parentView: View? = null

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
//        if (isInEditMode) return
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer)
            observer.onChanged()
        }
    }

    private fun showEmptyView() {
        if (adapter != null) {
            if (emptyView == null) {
                return
            }
            if (adapter.itemCount == 0) {
                showParentOrSelf(false)
            } else {
                showParentOrSelf(true)
            }
        } else {
            if (emptyView != null) {
                showParentOrSelf(false)
            }
        }
    }

    private fun showParentOrSelf(showRecyclerView: Boolean) {
        if (parentView != null) {
            parentView!!.visibility = View.VISIBLE
        }
        visibility = View.VISIBLE
        emptyView!!.visibility = if (!showRecyclerView) View.VISIBLE else View.GONE
    }

    fun setEmptyView(emptyView: StateLayout, parentView: View? = null) {
        this.emptyView = emptyView
        this.parentView = parentView
        showEmptyView()
    }
}
