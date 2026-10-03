/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Native semantic roles derived from the supplied v4 design, with darker readable muted text. */
internal fun referenceColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFB4A4FF), onPrimary = Color(0xFF25145C),
        primaryContainer = Color(0xFF302943), onPrimaryContainer = Color(0xFFE5DDFF),
        secondary = Color(0xFFDED5F0), onSecondary = Color(0xFF373044),
        secondaryContainer = Color(0xFF373044), onSecondaryContainer = Color(0xFFDED5F0),
        tertiary = Color(0xFFAAD3B6), onTertiary = Color(0xFF153824),
        background = Color(0xFF19191D), onBackground = Color(0xFFEEECEF),
        surface = Color(0xFF19191D), onSurface = Color(0xFFEEECEF),
        surfaceContainerLowest = Color(0xFF17171B), surfaceContainerLow = Color(0xFF232328),
        surfaceContainer = Color(0xFF2A2930), surfaceContainerHigh = Color(0xFF33323A), surfaceContainerHighest = Color(0xFF3D3C44),
        surfaceVariant = Color(0xFF2A2930), onSurfaceVariant = Color(0xFFB0ADB8),
        outline = Color(0xFF898591), outlineVariant = Color(0xFF3B3943),
        surfaceDim = Color(0xFF19191D), surfaceBright = Color(0xFF3F3E46),
        inverseSurface = Color(0xFFE7E1E9), inverseOnSurface = Color(0xFF302D35), inversePrimary = Color(0xFF6352CA),
        surfaceTint = Color(0xFFB4A4FF),
        tertiaryContainer = Color(0xFF243D30), onTertiaryContainer = Color(0xFFC7ECD4),
        error = Color(0xFFF1B4AA), onError = Color(0xFF5E1916),
        errorContainer = Color(0xFF4D2829), onErrorContainer = Color(0xFFFFDAD6),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF5F4FC6), onPrimary = Color.White,
        primaryContainer = Color(0xFFEEEAFB), onPrimaryContainer = Color(0xFF483798),
        secondary = Color(0xFF585064), onSecondary = Color.White,
        secondaryContainer = Color(0xFFF0EBF8), onSecondaryContainer = Color(0xFF585064),
        tertiary = Color(0xFF357358), onTertiary = Color.White,
        background = Color(0xFFF7F7F5), onBackground = Color(0xFF29272E),
        surface = Color(0xFFF7F7F5), onSurface = Color(0xFF29272E),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
        surfaceContainer = Color(0xFFEDEAF1), surfaceContainerHigh = Color(0xFFE6E3EB), surfaceContainerHighest = Color(0xFFDFDCE5),
        surfaceVariant = Color(0xFFF2F0F5), onSurfaceVariant = Color(0xFF625E69),
        outline = Color(0xFF898591), outlineVariant = Color(0xFFE3DFE8),
        surfaceDim = Color(0xFFDEDCE1), surfaceBright = Color(0xFFF7F7F5),
        inverseSurface = Color(0xFF302D35), inverseOnSurface = Color(0xFFF5EFF7), inversePrimary = Color(0xFFB4A4FF),
        surfaceTint = Color(0xFF6352CA),
        tertiaryContainer = Color(0xFFE4F2E9), onTertiaryContainer = Color(0xFF224F37),
        error = Color(0xFFA43C35), onError = Color.White,
        errorContainer = Color(0xFFFBEAE7), onErrorContainer = Color(0xFF713129),
    )
}
