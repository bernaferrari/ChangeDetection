package com.bernaferrari.changedetection.groupie

import android.graphics.drawable.GradientDrawable
import com.bernaferrari.changedetection.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_title.*

class DialogItemTitle(
    val title: String,
    val subtitle: String,
    private val errorOnLastSync: Boolean,
    var gradientColors: Pair<Int, Int>
) : Item() {

    override fun getLayout(): Int = R.layout.dialog_item_title

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = title
        viewHolder.subtitle.text = subtitle

        if (gradientColors.first == 0 && gradientColors.second == 0) {
            if (errorOnLastSync) {
                viewHolder.containerView.background =
                        viewHolder.containerView.context.getDrawable(R.drawable.red_orange_drawable)
            } else {
                viewHolder.containerView.background =
                        viewHolder.containerView.context.getDrawable(R.drawable.light_blue_drawable)
            }
        } else {
            val shape = GradientDrawable(
                GradientDrawable.Orientation.TR_BL, intArrayOf(
                    gradientColors.first, gradientColors.second
                )
            )

            viewHolder.containerView.background = shape
        }
    }
}