/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/** The brand hue: a clear indigo that sits opposite the yellow highlighter, so changes always pop. */
internal val BrandSeed = Color(0xFF4F5BD5)

/**
 * A tonal Material 3 scheme. [SeedColor] is the stored "app default" marker and maps to [BrandSeed];
 * every other seed comes from the Appearance picker.
 */
internal fun brandColorScheme(seed: Color, dark: Boolean): ColorScheme = dynamicColorScheme(
    seedColor = if (seed == SeedColor) BrandSeed else seed,
    isDark = dark,
    style = PaletteStyle.TonalSpot,
    specVersion = ColorSpec.SpecVersion.SPEC_2025,
)

internal fun referenceColorScheme(dark: Boolean) = brandColorScheme(SeedColor, dark)

/** The one place every platform builds the theme from, so type, shape and marks never drift. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppMaterialTheme(colorScheme: ColorScheme, dark: Boolean, content: @Composable () -> Unit) {
    // Screens sit on the container tone and content cards on the bright tone (the Android 16 look),
    // so grouped content reads as crisp tiles in light and dark.
    val screen = colorScheme.copy(surface = colorScheme.surfaceContainer, background = colorScheme.surfaceContainer)
    MaterialExpressiveTheme(
        colorScheme = screen,
        motionScheme = MotionScheme.expressive(),
        typography = appTypography(),
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(LocalAppSpacing provides AppSpacing(), LocalMarks provides marksFor(dark)) {
            content()
        }
    }
}
