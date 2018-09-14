package com.bernaferrari.changedetection.groupie

import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.ColorGroup
import com.bernaferrari.changedetection.util.GradientColors
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.dialog_item_title.*

/**
 * Dialog header with title, subtitle and a colorful background
 *
 * @param title              for item title
 * @param subtitle           for item subtitle
 * @param gradientColors     colors that will be used for background
 */
class DialogItemTitle(
    val title: String,
    val subtitle: String,
    var gradientColors: ColorGroup
) : Item() {

    override fun getLayout(): Int = R.layout.dialog_item_title

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = title
        viewHolder.subtitle.text = subtitle

        val shape = GradientColors.getGradientDrawable(gradientColors.first, gradientColors.second)

        viewHolder.containerView.background = shape
    }
}