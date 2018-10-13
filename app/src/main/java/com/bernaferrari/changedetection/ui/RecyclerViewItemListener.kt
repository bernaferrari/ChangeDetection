package com.bernaferrari.changedetection.ui

import android.support.v7.widget.RecyclerView

interface RecyclerViewItemListener {
    fun onClickListener(item: RecyclerView.ViewHolder)
    fun onLongClickListener(item: RecyclerView.ViewHolder)
}
