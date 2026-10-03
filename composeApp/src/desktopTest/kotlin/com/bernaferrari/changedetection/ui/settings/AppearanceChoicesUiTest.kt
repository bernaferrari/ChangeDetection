/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.model.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class AppearanceChoicesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactLargeTextKeepsThemeNamesAndPaletteActionsReachable() = checkChoices(2f, 280)

    @Test fun regularWidthKeepsConnectedThemeChoicesUsable() = checkChoices(1f, 390)

    @Test fun appearanceSectionReopensWithTheChosenThemeAndPalette() {
        var theme by mutableStateOf(ThemeMode.SYSTEM)
        var palette by mutableStateOf(AppPalette.DEFAULT)
        compose.setContent {
            ChangeDetectionTheme(darkTheme = theme == ThemeMode.DARK, dynamicColor = false) {
                Column(Modifier.width(390.dp)) {
                    DeviceSettingsSection(SettingsUiState(themeMode = theme, appPalette = palette, storageBytes = 0), { theme = it }, { palette = it }, {})
                }
            }
        }
        compose.onNodeWithText("System · Default").performClick()
        compose.onNode(hasText("Dark") and isToggleable()).performClick().assertIsOn()
        compose.onNodeWithContentDescription("Blue").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithContentDescription("Blue").assertDoesNotExist()
        compose.onNodeWithText("Dark · Blue").assertIsDisplayed().performClick()
        compose.onNode(hasText("Dark") and isToggleable()).assertIsOn()
        compose.onNodeWithContentDescription("Blue").performScrollTo().assertIsSelected()
        compose.runOnIdle {
            assertEquals(ThemeMode.DARK, theme)
            assertEquals(AppPalette.BLUE, palette)
        }
        compose.onNodeWithText("Done").performClick()
    }

    @Test fun failedWriteCanBeRetriedInsideTheAppearanceDialog() {
        var failed by mutableStateOf(true)
        var retried = 0
        compose.setContent {
            ChangeDetectionTheme {
                Column(Modifier.width(390.dp)) {
                    DeviceSettingsSection(SettingsUiState(storageBytes = 0), {}, {}, {}, failed, {
                        retried++
                        failed = false
                    })
                }
            }
        }
        compose.onNodeWithText("System · Default").performClick()
        compose.onNodeWithText("Could not save your choice. Try again.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Could not save your choice. Try again.").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retried) }
        compose.onNodeWithText("Done").performClick()
    }

    private fun checkChoices(scale: Float, width: Int) {
        var theme by mutableStateOf(ThemeMode.SYSTEM)
        var palette by mutableStateOf(AppPalette.DEFAULT)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.width(width.dp).height(600.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            ThemeSelector(theme, palette, { theme = it }, { palette = it })
                        }
                    }
                }
            }
        }
        for (name in listOf("Light", "Dark", "System")) {
            val text = compose.onNodeWithText(name, useUnmergedTree = true)
            text.performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals(1, layout.lineCount)
            assertFalse(layout.isLineEllipsized(0))
            assertTrue(layout.getLineRight(0) <= layout.size.width + 0.5f)
        }
        compose.onNodeWithText("Dark").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, theme) }
        compose.onNodeWithContentDescription("Blue").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(AppPalette.BLUE, palette) }
        compose.onNodeWithText("Light").performScrollTo()
        val output = File("../build/desktop-review/appearance-${if (scale > 1f) "large" else "regular"}-dark.png")
        output.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", output)
    }
}
