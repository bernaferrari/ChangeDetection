package com.bernaferrari.changedetection.detailsText

import android.view.View
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bernaferrari.base.misc.getColorFromAttr
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.core.KotlinEpoxyHolder
import com.google.android.material.card.MaterialCardView

/**
 * Creates a ColorPicker RecyclerView. This will be used on create/edit dialogs.
 *
 * @param isFirstIndex if true, adds extra padding to the first item
 * @param isSwitchOn if true, sets the item as selected
 * @param gradientColor the color pair which will be set for this item
 */
@EpoxyModelClass(layout = R.layout.item_text_selector)
abstract class SelectorItem : EpoxyModelWithHolder<SelectorItem.Holder>() {

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    lateinit var subtitle: String

    @EpoxyAttribute
    var colorSelected = ItemSelected.NONE

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: View.OnClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onLongClick: View.OnLongClickListener? = null

    override fun bind(viewHolder: Holder) {
        viewHolder.bindAux()
    }

    private fun Holder.bindAux() {

        subtitleTextView.text = subtitle
        titleTextView.text = title

        bindColors()
    }

    private fun Holder.bindColors() {
        val context = container.context

        val id = when (colorSelected) {
            ItemSelected.REVISED -> R.attr.code_addition_diff
            ItemSelected.ORIGINAL -> R.attr.code_deletion_diff
            else -> R.attr.windowBackground
        }

        container.setCardBackgroundColor(context.getColorFromAttr(id))
        container.setOnClickListener(onClick)
        container.setOnLongClickListener(onLongClick)
    }

    //
    // Holder
    //

    class Holder : KotlinEpoxyHolder() {
        val container by bind<MaterialCardView>(R.id.container)
        val titleTextView by bind<TextView>(R.id.titleTextView)
        val subtitleTextView by bind<TextView>(R.id.subtitleTextView)
    }
}

