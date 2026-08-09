/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.model

import androidx.compose.ui.graphics.Color
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.palette_amber
import com.bernaferrari.changedetection.shared.generated.resources.palette_blue
import com.bernaferrari.changedetection.shared.generated.resources.palette_cyan
import com.bernaferrari.changedetection.shared.generated.resources.palette_dynamic
import com.bernaferrari.changedetection.shared.generated.resources.palette_green
import com.bernaferrari.changedetection.shared.generated.resources.palette_indigo
import com.bernaferrari.changedetection.shared.generated.resources.palette_lime
import com.bernaferrari.changedetection.shared.generated.resources.palette_orange
import com.bernaferrari.changedetection.shared.generated.resources.palette_pink
import com.bernaferrari.changedetection.shared.generated.resources.palette_purple
import com.bernaferrari.changedetection.shared.generated.resources.palette_teal
import org.jetbrains.compose.resources.StringResource

/** Material Kolor seeds ordered warm to cool, matching QuietGuard's appearance picker. */
enum class AppPalette(
    val titleRes: StringResource,
    val seedColor: Color,
    val usesSystemColors: Boolean = false,
) {
    DYNAMIC(Res.string.palette_dynamic, Color(0xFF009688), usesSystemColors = true),
    ORANGE(Res.string.palette_orange, Color(0xFFFF5722)),
    AMBER(Res.string.palette_amber, Color(0xFFFFC107)),
    LIME(Res.string.palette_lime, Color(0xFF8BC34A)),
    GREEN(Res.string.palette_green, Color(0xFF4CAF50)),
    TEAL(Res.string.palette_teal, Color(0xFF009688)),
    CYAN(Res.string.palette_cyan, Color(0xFF00BCD4)),
    BLUE(Res.string.palette_blue, Color(0xFF2196F3)),
    INDIGO(Res.string.palette_indigo, Color(0xFF3F51B5)),
    PURPLE(Res.string.palette_purple, Color(0xFF9C27B0)),
    PINK(Res.string.palette_pink, Color(0xFFE91E63)),
    ;

    companion object {
        fun fromStoredName(value: String?): AppPalette = entries.firstOrNull { it.name == value } ?: TEAL
    }
}
