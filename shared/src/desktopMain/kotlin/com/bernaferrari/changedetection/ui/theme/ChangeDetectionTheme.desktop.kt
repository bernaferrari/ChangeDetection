/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * ChangeDetection Desktop Theme.
 * Always uses MaterialKolor to generate schemes from SeedColor.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit,
) {
    // Desktop doesn't have native Material You, always use MaterialKolor
    val colorScheme = brandColorScheme(seedColor, darkTheme)

    AppMaterialTheme(colorScheme, darkTheme, content)
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
