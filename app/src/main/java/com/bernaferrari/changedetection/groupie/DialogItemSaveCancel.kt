package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_save_cancel.*

/**
 * Dialog header with title and subtitle
 *
 * @param title              for item title
 * @param subtitle           for item subtitle
 */
class DialogItemSaveCancel(val cancel: () -> (Unit), val positive: () -> (Unit)) : Item() {

    override fun getLayout(): Int = R.layout.dialog_save_cancel

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.cancel.setOnClickListener { cancel.invoke() }

        viewHolder.positive.setOnClickListener { positive.invoke() }
    }
}
