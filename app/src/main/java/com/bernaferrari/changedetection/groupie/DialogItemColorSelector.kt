package com.bernaferrari.changedetection.groupie

import android.content.res.Resources
import android.view.ViewGroup.MarginLayoutParams
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.colorpicker_item.*


class DialogItemColorSelector(
    private val isFirstIndex: Boolean,
    var isSwitchOn: Boolean,
    val colors: Pair<Int, Int>,
    private val listener: (DialogItemColorSelector) -> (Unit)
) : Item() {

    fun unselectAndNotify() {
        isSwitchOn = false
        notifyChanged()
    }

    private fun dp(value: Int, resources: Resources): Int {
        return (resources.displayMetrics.density * value).toInt()
    }

    override fun getLayout(): Int = R.layout.colorpicker_item

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.containerView.setOnClickListener {
            // We don't want to allow deselection
            if (!isSwitchOn) {
                isSwitchOn = true
                listener.invoke(this)
                viewHolder.paintItem.reverseSelection()
            }
        }

        // This is necessary for extra left padding on the first item, so it looks
        // visually identical to other items.
        // Since it is inside a recyclerView, the margin needs to be set at all the items.
        viewHolder.containerView.run {
            val dp8 = dp(8, resources)
            val dp16 = dp(16, resources)
            val dp32 = dp(32, resources)

            val layoutParams = layoutParams as MarginLayoutParams
            layoutParams.width = dp32
            layoutParams.height = dp32

            if (isFirstIndex) {
                layoutParams.setMargins(dp16, dp8, dp8, dp8)
            } else {
                layoutParams.setMargins(dp8, dp8, dp8, dp8)
            }

            viewHolder.containerView.layoutParams = layoutParams
        }

        // First time it loads, or if recycles, the colors need to be set correctly.
        if (!viewHolder.paintItem.areColorsSet() || viewHolder.paintItem.colors != colors) {
            viewHolder.paintItem.colors = colors
            viewHolder.paintItem.updateColor()
        }

        when (isSwitchOn) {
            true -> viewHolder.paintItem.selectIfDeselected(true)
            false -> viewHolder.paintItem.deselectIfSelected(true)
        }
    }
}