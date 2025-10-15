package com.bernaferrari.changedetection.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils as AndroidColorUtils

object ColorUtils {
    
    /**
     * Blend two colors together
     */
    fun blendColors(color1: Int, color2: Int, ratio: Float = 0.5f): Int {
        return AndroidColorUtils.blendARGB(color1, color2, ratio)
    }
    
    /**
     * Get a lighter version of a color
     */
    fun lightenColor(color: Int, factor: Float = 0.2f): Int {
        return AndroidColorUtils.blendARGB(color, android.graphics.Color.WHITE, factor)
    }
    
    /**
     * Get a darker version of a color
     */
    fun darkenColor(color: Int, factor: Float = 0.2f): Int {
        return AndroidColorUtils.blendARGB(color, android.graphics.Color.BLACK, factor)
    }
    
    /**
     * Check if a color is considered "light"
     */
    fun isColorLight(color: Int): Boolean {
        return AndroidColorUtils.calculateLuminance(color) > 0.5
    }
    
    /**
     * Get appropriate text color for background
     */
    fun getTextColorForBackground(backgroundColor: Int): Int {
        return if (isColorLight(backgroundColor)) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
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
    fun generateContrastingColor(baseColor: Int): Int {
        val luminance = AndroidColorUtils.calculateLuminance(baseColor)
        return if (luminance > 0.5) {
            // Base is light, return dark
            darkenColor(baseColor, 0.6f)
        } else {
            // Base is dark, return light
            lightenColor(baseColor, 0.6f)
        }
    }
    
    /**
     * Create a material-style color palette from a single color
     */
    fun createMaterialPalette(primaryColor: Int): MaterialColorPalette {
        return MaterialColorPalette(
            primary = primaryColor,
            primaryVariant = darkenColor(primaryColor, 0.3f),
            secondary = generateContrastingColor(primaryColor),
            surface = lightenColor(primaryColor, 0.9f),
            background = lightenColor(primaryColor, 0.95f),
            onPrimary = getTextColorForBackground(primaryColor),
            onSecondary = getTextColorForBackground(generateContrastingColor(primaryColor)),
            onSurface = darkenColor(primaryColor, 0.8f),
            onBackground = darkenColor(primaryColor, 0.8f)
        )
    }
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
    val onBackground: Int
)