package com.bernaferrari.changedetection.groupie

import android.graphics.drawable.Drawable
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_simple.*

/**
 * Simple clickable item with image on the left and text on the remaining space
 */
class DialogItemSimple(
    val title: String,
    val drawable: Drawable,
    val kind: String
) : Item() {

    override fun getLayout(): Int = R.layout.dialog_item_simple

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = title
        viewHolder.image.setImageDrawable(drawable)
    }
}
