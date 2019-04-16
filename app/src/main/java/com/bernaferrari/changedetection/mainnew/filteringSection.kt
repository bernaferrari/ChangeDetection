package com.bernaferrari.changedetection.mainnew

import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyRecyclerView
import com.bernaferrari.base.view.inflate
import com.bernaferrari.changedetection.R

fun ViewGroup.inflateFilter(): EpoxyRecyclerView? {
    return this.inflate(R.layout.filters, false).also {
        this.addView(it)
    } as? EpoxyRecyclerView
}