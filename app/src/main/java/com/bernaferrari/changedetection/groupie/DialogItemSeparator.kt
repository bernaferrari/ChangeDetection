package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.settings_item_separator.*

/**
 * Used to create a separator between settings items.
 */
class DialogItemSeparator(val title: String) : Item() {

    override fun getLayout(): Int = R.layout.settings_item_separator

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.title.text = title
    }
}
