package com.bernaferrari.changedetection.groupie

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import com.bernaferrari.changedetection.R
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
    private val currentColors: Pair<Int, Int>,
    private val gradientList: List<Pair<Int, Int>>,
    private val listener: (Pair<Int, Int>) -> (Unit)
) : Item() {
    override fun getLayout(): Int = R.layout.colorpicker_recyclerview

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.defaultRecycler.apply {
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

            this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = GroupAdapter<ViewHolder>().apply {
                add(Section(selectorList))
            }
            this.itemAnimator = this.itemAnimator.apply {
                // From https://stackoverflow.com/a/33302517/4418073
                if (this is SimpleItemAnimator) {
                    this.supportsChangeAnimations = false
                }
            }

        }
    }
}
