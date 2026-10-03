/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Highlighter": warm paper, near-black ink and one marker yellow.
 *
 * Primary is ink, so filled buttons read as confident black pills. The primary container is the
 * marker, so FABs, the selected navigation item and "new" accents share the one color that means
 * "something changed".
 */
internal fun referenceColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFF3F1E9), onPrimary = Color(0xFF14130E),
        primaryContainer = Color(0xFFFFE14A), onPrimaryContainer = Color(0xFF14130E),
        inversePrimary = Color(0xFF17160F),
        secondary = Color(0xFFB7B4A7), onSecondary = Color(0xFF14130E),
        secondaryContainer = Color(0xFF2E2D26), onSecondaryContainer = Color(0xFFF3F1E9),
        tertiary = Color(0xFF74D9A0), onTertiary = Color(0xFF08311B),
        tertiaryContainer = Color(0xFF1E3A2A), onTertiaryContainer = Color(0xFFBFEFD2),
        background = Color(0xFF11100C), onBackground = Color(0xFFF3F1E9),
        surface = Color(0xFF11100C), onSurface = Color(0xFFF3F1E9),
        surfaceVariant = Color(0xFF24231D), onSurfaceVariant = Color(0xFFB7B4A7),
        surfaceContainerLowest = Color(0xFF0C0B08),
        surfaceContainerLow = Color(0xFF1B1A15),
        surfaceContainer = Color(0xFF22211B),
        surfaceContainerHigh = Color(0xFF2A2922),
        surfaceContainerHighest = Color(0xFF34332B),
        surfaceDim = Color(0xFF11100C), surfaceBright = Color(0xFF3A392F),
        surfaceTint = Color(0xFFF3F1E9),
        outline = Color(0xFF878478), outlineVariant = Color(0xFF2F2D26),
        inverseSurface = Color(0xFFF3F1E9), inverseOnSurface = Color(0xFF14130E),
        error = Color(0xFFFF8E78), onError = Color(0xFF4A0F04),
        errorContainer = Color(0xFF3D1C15), onErrorContainer = Color(0xFFFFD9CF),
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = Color(0xFF17160F), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE14A), onPrimaryContainer = Color(0xFF17160F),
        inversePrimary = Color(0xFFFFE14A),
        secondary = Color(0xFF55534A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAE7DF), onSecondaryContainer = Color(0xFF17160F),
        tertiary = Color(0xFF1D7448), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDDF0E4), onTertiaryContainer = Color(0xFF0F3D25),
        background = Color(0xFFF6F5F0), onBackground = Color(0xFF17160F),
        surface = Color(0xFFF6F5F0), onSurface = Color(0xFF17160F),
        surfaceVariant = Color(0xFFEDEBE4), onSurfaceVariant = Color(0xFF55534A),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFBFAF7),
        surfaceContainer = Color(0xFFEFEDE6),
        surfaceContainerHigh = Color(0xFFE8E5DD),
        surfaceContainerHighest = Color(0xFFE1DED4),
        surfaceDim = Color(0xFFE3E0D7), surfaceBright = Color.White,
        surfaceTint = Color(0xFF17160F),
        outline = Color(0xFF86837A), outlineVariant = Color(0xFFE4E1D8),
        inverseSurface = Color(0xFF17160F), inverseOnSurface = Color(0xFFF6F5F0),
        error = Color(0xFFB23A24), onError = Color.White,
        errorContainer = Color(0xFFFBE5DF), onErrorContainer = Color(0xFF6B1E10),
        scrim = Color.Black,
    )
}
