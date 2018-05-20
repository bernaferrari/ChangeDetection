package com.example.changedetection.groupie

import android.graphics.drawable.Drawable
import android.view.View
import com.example.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_switch.*

class DialogItemSwitch(
    val title: String,
    val drawable: Drawable,
    val kind: String,
    val isSwitchVisible: Boolean = false,
    val isSwitchOn: Boolean = false,
    val listener: (DialogItemSwitch) -> (Unit)
) : Item() {

    override fun getLayout(): Int {
        return R.layout.dialog_item_switch
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.containerView.setOnClickListener {
            listener.invoke(this)

            if (isSwitchVisible) {
                viewHolder.item_switch.isChecked = !viewHolder.item_switch.isChecked
            }
        }

        viewHolder.title.text = title
        viewHolder.image.setImageDrawable(drawable)

        if (isSwitchVisible) {
            viewHolder.item_switch.visibility = View.VISIBLE
            viewHolder.item_switch.isChecked = isSwitchOn
        }
    }
}
