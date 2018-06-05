package com.bernaferrari.changedetection.groupie

import android.graphics.drawable.Drawable
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_switch.*

/**
 * Clickable item with a switch
 *
 * @param title         for item title
 * @param drawable      for ImageView on the left
 * @param isSwitchOn    selects the switch when on
 * @param listener      callback when switch state changes
 */
class DialogItemSwitch(
    val title: String,
    val drawable: Drawable,
    var isSwitchOn: Boolean = false,
    val listener: (DialogItemSwitch) -> (Unit)
) : Item() {

    override fun getLayout(): Int = R.layout.item_switch

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
