package com.bernaferrari.changedetection

/**
 * Some gradient colors are inspired from uiGradients:
 * https://uigradients.com/#Celestial
 *
 * Some gradient colors are inspired from Spark
 * https://github.com/TonnyL/Spark
 *
 * Some gradient colors are are mixing the them.
 *
 * Colors are right to left
 */
object GradientColors {


    fun getGradients(): List<Pair<Int, Int>> = listOf(
        // Light Blue A200 to Indigo A200
        Pair(0xff40C4FF.toInt(), 0xff536DFE.toInt()),

        Pair(0xffFBDA64.toInt(), 0xffF38181.toInt()), // Yellow -> Orange
        Pair(0xffF54EA2.toInt(), 0xffFF7676.toInt()), // Pink -> Pink
        Pair(0xff17EAD9.toInt(), 0xff448AFF.toInt()), // Cyan -> Blue
        Pair(0xff20e281.toInt(), 0xff00bf8f.toInt()), // Green -> Light-Green
        Pair(0xff7117EA.toInt(), 0xffEA6060.toInt()), // Purple -> Instagram-Purple
        Pair(0xffFEAC5E.toInt(), 0xff5d0058.toInt()), // Yellow -> Purple

        Pair(0xff65799B.toInt(), 0xff5E2563.toInt()), // Blue-Grey -> Purple
        Pair(0xff184E68.toInt(), 0xff57CA85.toInt()), // Dark Green -> Light Green
        Pair(0xfffeb47b.toInt(), 0xffff7e5f.toInt()), // Ed's Sunset Gradient
        Pair(0xff000428.toInt(), 0xff004e92.toInt()), // Frost
        Pair(0xff622774.toInt(), 0xffC53364.toInt())  // Dark-Purple -> Light-Purple
    )
}
