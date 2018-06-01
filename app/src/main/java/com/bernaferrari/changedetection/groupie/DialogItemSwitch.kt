package com.bernaferrari.changedetection.groupie

import android.graphics.drawable.Drawable
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_switch.*

class DialogItemSwitch(
    val title: String,
    val drawable: Drawable,
    var isSwitchOn: Boolean = false,
    val listener: (DialogItemSwitch) -> (Unit)
) : Item() {

    override fun getLayout(): Int {
        return R.layout.dialog_item_switch
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.containerView.setOnClickListener {
            viewHolder.item_switch.isChecked = !viewHolder.item_switch.isChecked
            isSwitchOn = viewHolder.item_switch.isChecked
            listener.invoke(this)
        }

        viewHolder.title.text = title
        viewHolder.image.setImageDrawable(drawable)

        viewHolder.item_switch.isChecked = isSwitchOn
    }
}
