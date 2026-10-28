package com.bernaferrari.changedetection.model

import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.color_style_expressive
import com.bernaferrari.changedetection.shared.generated.resources.color_style_muted
import com.bernaferrari.changedetection.shared.generated.resources.color_style_tonal
import com.bernaferrari.changedetection.shared.generated.resources.color_style_vibrant
import com.materialkolor.PaletteStyle
import org.jetbrains.compose.resources.StringResource

/** How the chosen colour becomes a whole palette, the same four styles as Calcula and WiFi Weakspot. */
enum class ColorStyle(val titleRes: StringResource, val palette: PaletteStyle) {
    EXPRESSIVE(Res.string.color_style_expressive, PaletteStyle.Expressive),
    VIBRANT(Res.string.color_style_vibrant, PaletteStyle.Vibrant),
    TONAL(Res.string.color_style_tonal, PaletteStyle.TonalSpot),
    MUTED(Res.string.color_style_muted, PaletteStyle.Cmf()),
    ;

    companion object {
        fun fromStoredName(value: String?): ColorStyle = entries.firstOrNull { it.name == value } ?: EXPRESSIVE
    }
}
