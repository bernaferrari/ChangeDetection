/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceChoicesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themeAndPaletteChoicesApplyImmediately() = check(1f)

    @Test fun largeTextKeepsEveryChoiceReachable() = check(2f)

    private fun check(scale: Float) {
        var theme by mutableStateOf(ThemeMode.SYSTEM)
        var palette by mutableStateOf(AppPalette.DEFAULT)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = theme == ThemeMode.DARK, dynamicColor = false) {
                    AppearanceSheet(theme, palette, { theme = it }, { palette = it }, {})
                }
            }
        }
        compose.onNodeWithText("Dark").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, theme) }
        compose.onNodeWithContentDescription("Blue").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AppPalette.BLUE, palette) }
        compose.onNodeWithText("Indigo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AppPalette.DEFAULT, palette) }
    }
}
