package com.bernaferrari.changedetection.ui

interface RecyclerViewItemListener {
    fun onClickListener(item: androidx.recyclerview.widget.RecyclerView.ViewHolder)
    fun onLongClickListener(item: androidx.recyclerview.widget.RecyclerView.ViewHolder)
}
