package com.bernaferrari.changedetection.groupie

import androidx.recyclerview.widget.LinearLayoutManager
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.ColorGroup
import com.bernaferrari.changedetection.extensions.itemAnimatorWithoutChangeAnimations
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.recyclerview.*

/**
 * Creates a ColorPicker RecyclerView. This will be used on create/edit dialogs.
 *
 * @param currentColors the current selected gradientColor for the item
 * @param gradientList the gradient list with all the color pairs
 * @param listener callback for transmitting back the results
 */
class ColorPickerRecyclerViewItem(
    private val currentColors: ColorGroup,
    private val gradientList: List<ColorGroup>,
    private val listener: (ColorGroup) -> (Unit)
) : Item() {
    override fun getLayout(): Int = R.layout.colorpicker_recyclerview

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val selectorList = mutableListOf<ColorPickerItem>()

        // Create each color picker item, checking for the first (because it needs extra margin)
        // and checking for the one which is selected (so it becomes selected)
        gradientList.mapIndexedTo(selectorList) { index, it ->

            ColorPickerItem(index == 0, currentColors == it, it) { itemClicked ->

                // When a value is selected, all others must be unselected.
                selectorList.forEach { listItem ->
                    if (listItem != itemClicked && listItem.isSwitchOn) {
                        listItem.deselectAndNotify()
                    }
                }

                listener.invoke(itemClicked.gradientColor)
            }
        }

        viewHolder.defaultRecycler.apply {
            this.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            this.adapter = GroupAdapter<ViewHolder>().apply {
                add(Section(selectorList))
            }
            this.itemAnimator = itemAnimatorWithoutChangeAnimations()

        }
    }
}
