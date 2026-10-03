/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * ChangeDetection Android theme with Material You dynamic colors where available.
 * - Android 12+: Uses native wallpaper-extracted colors when dynamicColor=true
 * - All other cases: Uses MaterialKolor to generate schemes from SeedColor
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Use native Android dynamic colors on Android 12+ (Material You)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // All other cases: generate from SeedColor using MaterialKolor
        else -> {
            if (seedColor == SeedColor) {
                referenceColorScheme(darkTheme)
            } else {
                dynamicColorScheme(
                    seedColor = seedColor,
                    isDark = darkTheme,
                    style = PaletteStyle.TonalSpot,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                )
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(LocalAppSpacing provides AppSpacing()) {
            content()
        }
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
