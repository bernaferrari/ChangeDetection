package com.bernaferrari.changedetection.groupie

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import com.bernaferrari.changedetection.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.default_recycler_grey_200.*

class DialogItemColorRecycler(
    private val currentColors: Pair<Int, Int>,
    private val gradientList: List<Pair<Int, Int>>,
    private val listener: (Pair<Int, Int>) -> (Unit)
) : Item() {
    override fun getLayout(): Int = R.layout.colorpicker_recyclerview

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.defaultRecycler.apply {
            val selectorList = mutableListOf<DialogItemColorSelector>()

            gradientList.mapIndexedTo(selectorList) { index, it ->
                DialogItemColorSelector(index == 0, currentColors == it, it) { itemClicked ->
                    // When a value is selected, all others must be unselected.
                    selectorList.forEach { listItem ->
                        if (listItem != itemClicked && listItem.isSwitchOn) {
                            listItem.unselectAndNotify()
                        }
                    }

                    listener.invoke(itemClicked.colors)
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
