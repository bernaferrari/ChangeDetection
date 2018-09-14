package com.bernaferrari.changedetection.util

import android.graphics.drawable.GradientDrawable
import com.bernaferrari.changedetection.extensions.ColorGroup

/**
 * Some gradient gradientColor are inspired from uiGradients:
 * https://uigradients.com/#Celestial
 *
 * Some gradient gradientColor are inspired from Spark:
 * https://github.com/TonnyL/Spark
 *
 * Some gradient gradientColor are inspired from Gradients.io:
 * http://www.gradients.io/
 *
 * Some gradient gradientColor are are mixing the them.
 *
 * Colors are right to left
 */
object GradientColors {

    val gradients: List<ColorGroup> by lazy {
        listOf(
            // Light Blue A200 to Indigo A200
            Pair(0xff40C4FF.toInt(), 0xff536DFE.toInt()),

            Pair(0xfffeb47b.toInt(), 0xffff7e5f.toInt()), // Ed's Sunset Gradient
            Pair(0xffF54EA2.toInt(), 0xffFF7676.toInt()), // Pink -> Pink
            Pair(0xff17EAD9.toInt(), 0xff448AFF.toInt()), // Cyan -> Blue
            Pair(0xff20e281.toInt(), 0xff00bf8f.toInt()), // Green -> Light-Green
            Pair(0xff7117EA.toInt(), 0xffEA6060.toInt()), // Purple -> Instagram-Purple

            Pair(0xffFBB03B.toInt(), 0xffD4145A.toInt()), // Sanguine
            Pair(0xffFEAC5E.toInt(), 0xff5d0058.toInt()), // Yellow -> Purple
            Pair(0xffffb88c.toInt(), 0xffde6262.toInt()), // A Lost Memory
            Pair(0xff1e3c72.toInt(), 0xff2a5298.toInt()), // Eternal Constance

            Pair(0xff3AA17E.toInt(), 0xff00537E.toInt()), // New Leaf
            Pair(0xff29ABE2.toInt(), 0xff4F00BC.toInt()), // Deep Sea
            Pair(0xffEBC08D.toInt(), 0xffF24645.toInt()), // Fizzy Peach
            Pair(0xff622774.toInt(), 0xffC53364.toInt())  // Dark-Purple -> Light-Purple
        )
    }

    fun getGradientDrawable(firstColor: Int, secondColor: Int): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TR_BL, intArrayOf(
            firstColor, secondColor
        )
    )
}
