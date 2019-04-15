package com.bernaferrari.changedetection.addedit

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.core.KotlinEpoxyHolder
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.ui.ColorPickerView

/**
 * Creates a ColorPicker RecyclerView. This will be used on create/edit dialogs.
 *
 * @param isFirstIndex if true, adds extra padding to the first item
 * @param isSwitchOn if true, sets the item as selected
 * @param gradientColor the color pair which will be set for this item
 */
@EpoxyModelClass(layout = R.layout.colorpicker_item)
abstract class ColorPickerItemEpoxy : EpoxyModelWithHolder<ColorPickerItemEpoxy.Holder>() {

    @EpoxyAttribute
    var switchIsOn: Boolean = false

    @EpoxyAttribute
    lateinit var gradientColor: ColorGroup

    @EpoxyAttribute
    var allowDeselection: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: ((ColorGroup) -> Unit)? = null

    override fun bind(viewHolder: Holder) {
        viewHolder.bindAux()
    }

    private fun Holder.bindAux() {

        paintItem.setOnClickListener {
            // We don't want to allow deselection
            if (!switchIsOn || allowDeselection) {
                switchIsOn = !switchIsOn
                onClick?.invoke(gradientColor)

                paintItem.reverseSelection()
            }
        }

        // first time it loads, or if recycles, the gradientColor need to be set correctly.
        if (!paintItem.areColorsSet || paintItem.colors != gradientColor) {
            paintItem.colors = gradientColor
            paintItem.updateColor()

            // select/deselect without animation. Sometimes this will be called while scrolling,
            // so we want it to behave invisibly.
            when (switchIsOn) {
                true -> paintItem.selectIfDeselected(false)
                false -> paintItem.deselectIfSelected(false)
            }
        }

        // when deselectAndNotify is called, this is used to deselect with animation.
        when (switchIsOn) {
            true -> paintItem.selectIfDeselected(true)
            false -> paintItem.deselectIfSelected(true)
        }
    }


    //
    // Holder
    //

    class Holder : KotlinEpoxyHolder() {
        val paintItem by bind<ColorPickerView>(R.id.paintItem)
    }

}
