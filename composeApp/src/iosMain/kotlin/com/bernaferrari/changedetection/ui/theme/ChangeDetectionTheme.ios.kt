/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * ChangeDetection iOS Theme.
 * Always uses MaterialKolor to generate schemes from SeedColor.
 */
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    // iOS doesn't have native Material You, always use MaterialKolor
    val colorScheme = rememberDynamicColorScheme(
        seedColor = SeedColor,
        isDark = darkTheme,
        style = PaletteStyle.TonalSpot,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content,
    )
}

/**
 * Convenience wrapper with default values for simpler call sites.
 */
@Composable
fun ChangeDetectionTheme(
    content: @Composable () -> Unit,
) {
    ChangeDetectionTheme(
        darkTheme = isSystemInDarkTheme(),
        dynamicColor = true,
        content = content,
    )
}
