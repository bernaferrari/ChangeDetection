package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * ChangeDetection Android theme with Material You dynamic colors where available.
 * - Android 12+ with wallpaper colors: Android's own scheme, exactly, so the app matches the rest of the phone
 *   (Android has its own style setting for it); the color style shapes only the app's own colors
 * - All other cases: Uses MaterialKolor to generate schemes from SeedColor
 */
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color,
    colorStyle: com.bernaferrari.changedetection.model.ColorStyle,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Resource configuration changes (including the system palette) invalidate this composition.
    LocalConfiguration.current
    val wallpaperScheme =
        if (supportsDynamicColors()) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    val colorScheme = if (dynamicColor && wallpaperScheme != null) wallpaperScheme else remember(seedColor, darkTheme, colorStyle) { brandColorScheme(seedColor, darkTheme, colorStyle) }

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
