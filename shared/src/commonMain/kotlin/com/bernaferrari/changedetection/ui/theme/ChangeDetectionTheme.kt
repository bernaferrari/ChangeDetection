package com.bernaferrari.changedetection.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary seed color for MaterialKolor fallback schemes.
internal val SeedColor = Color(0xFF17160F)

/**
 * ChangeDetection app theme with platform-specific dynamic color support.
 * On Android 12+: Uses native wallpaper-extracted colors for Material You.
 * On older Android/iOS: Uses MaterialKolor to generate schemes from SeedColor.
 */
@Composable
expect fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color = SeedColor,
    content: @Composable () -> Unit,
)
