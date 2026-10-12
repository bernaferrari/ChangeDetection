package com.bernaferrari.changedetection.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object ColorUtils {
    /**
     * Blend two colors together
     */
    fun blendColors(
        color1: Int,
        color2: Int,
        ratio: Float = 0.5f,
    ): Int {
        val inverseRatio = 1f - ratio
        val a = (Color(color1).alpha * inverseRatio + Color(color2).alpha * ratio)
        val r = (Color(color1).red * inverseRatio + Color(color2).red * ratio)
        val g = (Color(color1).green * inverseRatio + Color(color2).green * ratio)
        val b = (Color(color1).blue * inverseRatio + Color(color2).blue * ratio)
        return Color(r, g, b, a).toArgb()
    }

    /**
     * Get a lighter version of a color
     */
    fun lightenColor(
        color: Int,
        factor: Float = 0.2f,
    ): Int {
        val c = Color(color)
        val r = (c.red + (1 - c.red) * factor).coerceIn(0f, 1f)
        val g = (c.green + (1 - c.green) * factor).coerceIn(0f, 1f)
        val b = (c.blue + (1 - c.blue) * factor).coerceIn(0f, 1f)
        return Color(r, g, b, c.alpha).toArgb()
    }

    /**
     * Get a darker version of a color
     */
    fun darkenColor(
        color: Int,
        factor: Float = 0.2f,
    ): Int {
        val c = Color(color)
        val r = (c.red * (1 - factor)).coerceIn(0f, 1f)
        val g = (c.green * (1 - factor)).coerceIn(0f, 1f)
        val b = (c.blue * (1 - factor)).coerceIn(0f, 1f)
        return Color(r, g, b, c.alpha).toArgb()
    }

    /**
     * Check if a color is considered "light"
     * Calculates luminance based on W3C standard
     */
    fun isColorLight(color: Int): Boolean {
        val c = Color(color)
        val luminance = 0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue
        return luminance > 0.5
    }

    /**
     * Get appropriate text color for background
     */
    fun getTextColorForBackground(backgroundColor: Int): Int = if (isColorLight(backgroundColor)) {
        0xFF000000.toInt() // BLACK
    } else {
        0xFFFFFFFF.toInt() // WHITE
    }

    /**
     * Convert Compose Color to Int
     */
    fun Color.toColorInt(): Int = this.toArgb()

    /**
     * Convert Int color to Compose Color
     */
    fun Int.toComposeColor(): Color = Color(this)

    /**
     * Generate a contrasting color
     */
    fun generateContrastingColor(baseColor: Int): Int = if (isColorLight(baseColor)) {
        // Base is light, return dark
        darkenColor(baseColor, 0.6f)
    } else {
        // Base is dark, return light
        lightenColor(baseColor, 0.6f)
    }

    /**
     * Create a material-style color palette from a single color
     */
    fun createMaterialPalette(primaryColor: Int): MaterialColorPalette = MaterialColorPalette(
        primary = primaryColor,
        primaryVariant = darkenColor(primaryColor, 0.3f),
        secondary = generateContrastingColor(primaryColor),
        surface = lightenColor(primaryColor, 0.9f),
        background = lightenColor(primaryColor, 0.95f),
        onPrimary = getTextColorForBackground(primaryColor),
        onSecondary = getTextColorForBackground(generateContrastingColor(primaryColor)),
        onSurface = darkenColor(primaryColor, 0.8f),
        onBackground = darkenColor(primaryColor, 0.8f),
    )
}

data class MaterialColorPalette(
    val primary: Int,
    val primaryVariant: Int,
    val secondary: Int,
    val surface: Int,
    val background: Int,
    val onPrimary: Int,
    val onSecondary: Int,
    val onSurface: Int,
    val onBackground: Int,
)
