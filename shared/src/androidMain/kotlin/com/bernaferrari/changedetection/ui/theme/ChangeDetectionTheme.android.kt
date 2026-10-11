/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * ChangeDetection Android theme with Material You dynamic colors where available.
 * - Android 12+: Uses the system wallpaper accent as a MaterialKolor seed when dynamicColor=true
 * - All other cases: Uses MaterialKolor to generate schemes from SeedColor
 */
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Resource configuration changes (including the system palette) invalidate this composition.
    LocalConfiguration.current
    val wallpaperSeed = if (supportsDynamicColors()) Color(context.getColor(android.R.color.system_accent1_500)) else null
    val wallpaperScheme = remember(wallpaperSeed, darkTheme) { wallpaperSeed?.let { brandColorScheme(it, darkTheme) } }
    val colorScheme = if (dynamicColor && wallpaperScheme != null) wallpaperScheme else remember(seedColor, darkTheme) { brandColorScheme(seedColor, darkTheme) }

    CompositionLocalProvider(LocalWallpaperColorScheme provides wallpaperScheme) {
        AppMaterialTheme(colorScheme, darkTheme, content)
    }
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
        seedColor = SeedColor,
        content = content,
    )
}
