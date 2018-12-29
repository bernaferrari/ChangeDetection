package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_title.*

/**
 * Dialog header with title and subtitle
 *
 * @param title              for item title
 * @param subtitle           for item subtitle
 */
class DialogItemTitle(
    val title: String,
    val subtitle: String
) : Item() {

    override fun getLayout(): Int = R.layout.dialog_item_title

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = title
        viewHolder.subtitle.text = subtitle
    }
}
