package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_empty_items.*

/**
 * Empty item showing there is no content available
 */
class EmptyItem(
    val color: Int
) : Item() {

    override fun getLayout(): Int = R.layout.item_empty_items

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.imageView.drawable.setTint(color)
    }
}
