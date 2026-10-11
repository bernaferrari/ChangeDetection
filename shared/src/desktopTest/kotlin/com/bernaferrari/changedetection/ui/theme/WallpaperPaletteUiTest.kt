/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.ui.components.paletteSwatchColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class WallpaperPaletteUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wallpaperPreviewUsesItsOwnGeneratedSchemeWhileAnotherPaletteIsActive() {
        var dark by mutableStateOf(false)
        var preview = emptyList<Color>()
        compose.setContent {
            val wallpaper = brandColorScheme(Color(0xFFE65100), dark)
            CompositionLocalProvider(LocalWallpaperColorScheme provides wallpaper) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = false, seedColor = AppPalette.BLUE.seedColor) {
                    preview = paletteSwatchColors(AppPalette.DYNAMIC)
                    Text("Wallpaper preview")
                }
            }
        }
        for (mode in listOf(false, true)) {
            compose.runOnIdle { dark = mode }
            compose.runOnIdle {
                val wallpaper = brandColorScheme(Color(0xFFE65100), mode)
                val fixed = brandColorScheme(AppPalette.BLUE.seedColor, mode)
                assertEquals(listOf(wallpaper.primary, wallpaper.secondary, wallpaper.tertiary), preview)
                assertNotEquals(fixed.primary, preview.first())
            }
        }
    }
}
