package com.bernaferrari.changedetection.groupie

import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.ColorGroup
import com.bernaferrari.changedetection.extensions.toDp
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.colorpicker_item.*

/**
 * Creates a ColorPicker RecyclerView. This will be used on create/edit dialogs.
 *
 * @param isFirstIndex if true, adds extra padding to the first item
 * @param isSwitchOn if true, sets the item as selected
 * @param gradientColor the color pair which will be set for this item
 */
class ColorPickerItem(
    private val isFirstIndex: Boolean,
    var isSwitchOn: Boolean,
    val gradientColor: ColorGroup,
    private val allowReselection: Boolean = false,
    private val listener: (ColorPickerItem) -> (Unit)
) : Item() {

    override fun getLayout(): Int = R.layout.colorpicker_item

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.containerView.setOnClickListener {
            // We don't want to allow deselection
            if (!isSwitchOn || allowReselection) {
                isSwitchOn = !isSwitchOn
                listener.invoke(this)
                viewHolder.paintItem.reverseSelection()
            }
        }

        // This is necessary for extra left padding on the first item, so it looks
        // visually identical to other items.
        // Since it is inside a recyclerView, the margin needs to be set at all the items.
        viewHolder.containerView.apply {
            val dp8 = 8.toDp(resources)
            val dp32 = 32.toDp(resources)

            viewHolder.containerView.updateLayoutParams<MarginLayoutParams> {
                this.width = dp32
                this.height = dp32
                this.setMargins(if (isFirstIndex) 16.toDp(resources) else dp8, dp8, dp8, dp8)
            }
        }

        // first time it loads, or if recycles, the gradientColor need to be set correctly.
        if (!viewHolder.paintItem.areColorsSet || viewHolder.paintItem.colors != gradientColor) {
            viewHolder.paintItem.colors = gradientColor
            viewHolder.paintItem.updateColor()

            // select/deselect without animation. Sometimes this will be called while scrolling,
            // so we want it to behave invisibly.
            when (isSwitchOn) {
                true -> viewHolder.paintItem.selectIfDeselected(false)
                false -> viewHolder.paintItem.deselectIfSelected(false)
            }
        }

        // when deselectAndNotify is called, this is used to deselect with animation.
        when (isSwitchOn) {
            true -> viewHolder.paintItem.selectIfDeselected(true)
            false -> viewHolder.paintItem.deselectIfSelected(true)
        }
    }

    fun deselectAndNotify() {
        isSwitchOn = false
        notifyChanged()
    }
}
