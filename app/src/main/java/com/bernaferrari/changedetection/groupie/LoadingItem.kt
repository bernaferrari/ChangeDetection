package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

class LoadingItem : Item() {
    override fun getLayout() = R.layout.item_loading
    override fun bind(viewHolder: ViewHolder, position: Int) = Unit
}
