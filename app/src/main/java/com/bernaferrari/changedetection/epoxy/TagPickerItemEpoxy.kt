package com.bernaferrari.changedetection.epoxy

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.core.KotlinEpoxyHolder

/**
 * Creates a ColorPicker RecyclerView. This will be used on create/edit dialogs.
 *
 * @param isFirstIndex if true, adds extra padding to the first item
 * @param isSwitchOn if true, sets the item as selected
 * @param gradientColor the color pair which will be set for this item
 */
@EpoxyModelClass(layout = R.layout.list_item_tag)
abstract class TagPickerItemEpoxy : EpoxyModelWithHolder<TagPickerItemEpoxy.Holder>() {

    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute
    lateinit var name: String

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: View.OnClickListener? = null

    override fun bind(viewHolder: Holder) {
        viewHolder.bindAux()
    }

    private fun Holder.bindAux() {

        chip.setOnClickListener(onClick)
        chip.text = name
        chip.setTextColor(
            if (checked) {
                Color.WHITE
            } else {
                ContextCompat.getColor(chip.context, R.color.unchecked_chip_text)
            }
        )
        chip.setBackgroundResource(
            if (checked) {
                R.drawable.checked_chip_selector
            } else {
                R.drawable.unchecked_chip_selector
            }
        )
    }

    //
    // Holder
    //

    class Holder : KotlinEpoxyHolder() {
        val chip by bind<TextView>(R.id.chip)
    }

}
