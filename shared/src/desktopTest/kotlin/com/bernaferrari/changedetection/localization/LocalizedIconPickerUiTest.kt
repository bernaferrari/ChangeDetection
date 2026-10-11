/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.bernaferrari.changedetection.ui.home.HomeEmptyState
import com.bernaferrari.changedetection.ui.settings.AppearanceControls
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.model.ThemeMode
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.addedit.PageIconShapePicker
import com.bernaferrari.changedetection.repo.PageIconColor
import com.bernaferrari.changedetection.repo.PageIconShape
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/** Actual rendering, scrolling and selection in every locale at normal and enlarged text. */
@RunWith(Parameterized::class)
class LocalizedIconPickerUiTest(private val language: String, private val scale: Float) {
    @get:Rule val compose = createComposeRule()
    private lateinit var previous: Locale

    @Before fun selectLocale() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(language))
    }

    @After fun restoreLocale() {
        Locale.setDefault(previous)
    }

    @Test fun translatedShapesAndAppearanceColorsRemainReachable() {
        val title = runBlocking { getString(Res.string.editor_icon_shape_title) }
        val circle = runBlocking { getString(Res.string.icon_shape_circle) }
        val pink = runBlocking { getString(Res.string.palette_pink) }
        compose.setContent {
            var shape by remember { mutableStateOf(PageIconShape.PUFFY) }
            var color by remember { mutableStateOf(PageIconColor.BLUE.colors) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 760.dp)) {
                        PageIconShapePicker("Example", "example", null, shape, { shape = it }, color, { color = it }, {})
                    }
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription(circle).performScrollTo().assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithContentDescription(pink).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        val root = compose.onNode(isRoot() and hasAnyDescendant(hasText(title)), useUnmergedTree = true)
        val output = File("../build/i18n/screens/picker-$language-$scale.png").apply { parentFile.mkdirs() }
        ImageIO.write(root.captureToImage().toAwtImage(), "png", output)
    }

    @Test fun translatedEmptyStateCanStartWatchingAtLargeText() {
        val action = runBlocking { getString(Res.string.home_watch_page) }
        var added = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 760.dp)) { HomeEmptyState({ added = true }, {}) }
                }
            }
        }
        compose.onNodeWithText(action).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { Assert.assertTrue(added) }
        saveScreen("empty")
    }

    @Test fun translatedAppearanceCanChangeThemeAndPaletteAtLargeText() {
        val dark = runBlocking { getString(Res.string.theme_dark_title) }
        val pink = runBlocking { getString(Res.string.palette_pink) }
        compose.setContent {
            var theme by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var palette by remember { mutableStateOf(AppPalette.DEFAULT) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Column(Modifier.size(400.dp, 760.dp).verticalScroll(rememberScrollState())) {
                        AppearanceControls(theme, palette, { theme = it }, { palette = it })
                    }
                }
            }
        }
        compose.onNodeWithText(dark).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithContentDescription(pink).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        saveScreen("appearance")
    }

    private fun saveScreen(name: String) {
        val output = File("../build/i18n/screens/$name-$language-$scale.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", output)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, font scale {1}")
        fun locales(): List<Array<Any>> = listOf("en", "es", "it", "pt", "de", "hi", "bn", "zh").flatMap { language -> listOf(1f, 1.8f).map { arrayOf(language, it) } }
    }
}
