/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Material shapes used by the expressive theme on every platform.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Primary seed color for MaterialKolor fallback schemes.
internal val SeedColor = Color(0xFF0D28E3)

/**
 * ChangeDetection app theme with platform-specific dynamic color support.
 * On Android 12+: Uses native wallpaper-extracted colors for Material You.
 * On older Android/iOS: Uses MaterialKolor to generate schemes from SeedColor.
 */
@Composable
expect fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
)
