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
import com.bernaferrari.changedetection.localization.getString
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
        val original = runBlocking { getString(Res.string.icon_original) }
        // The sheet animates in; wait for its content before checking it.
        compose.waitUntil(5_000) { compose.onAllNodesWithText(original).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(original).assertIsDisplayed()
        val circleChoice = compose.onNodeWithContentDescription(circle).performScrollTo().assertIsNotSelected()
        val targetBounds = circleChoice.fetchSemanticsNode().boundsInRoot
        circleChoice.performMouseInput { moveTo(center) }
        compose.waitForIdle()
        circleChoice.assertIsNotSelected()
        Assert.assertEquals("Hover keeps the hit target still", targetBounds, circleChoice.fetchSemanticsNode().boundsInRoot)
        val hoverRoot = compose.onAllNodes(isRoot()).onLast()
        val hoverOutput = File("../build/i18n/screens/picker-hover-$language-$scale.png").apply { parentFile.mkdirs() }
        ImageIO.write(hoverRoot.captureToImage().toAwtImage(), "png", hoverOutput)
        circleChoice.performClick().assertIsSelected()
        compose.onNodeWithContentDescription(pink).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        val root = compose.onAllNodes(isRoot()).onLast()
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

    @Test fun translatedTimingAndFormatsRemainUsableWithLocalKeyboardDigits() {
        val minutes = runBlocking { getString(Res.string.settings_sync_unit_minutes) }
        val source = runBlocking { getString(Res.string.capture_mode_source) }
        val off = runBlocking { getString(Res.string.ignore_small_off) }
        val custom = runBlocking { getString(Res.string.checks_custom) }
        val done = runBlocking { getString(Res.string.done) }
        val digits15 = when (language) {
            "hi" -> "१५"
            "bn" -> "১৫"
            "zh" -> "１５"
            else -> "15"
        }
        val digits7 = when (language) {
            "hi" -> "७"
            "bn" -> "৭"
            "zh" -> "７"
            else -> "7"
        }
        var interval = 60L
        var wait = 10_000L
        var format = com.bernaferrari.changedetection.repo.CaptureMode.TEXT
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Column(Modifier.size(320.dp, 760.dp).verticalScroll(rememberScrollState())) {
                        com.bernaferrari.changedetection.ui.settings.SyncIntervalInput(60L, { interval = it })
                        com.bernaferrari.changedetection.ui.settings.ScreenshotWaitChoice(10_000L, { wait = it })
                        com.bernaferrari.changedetection.addedit.OtherFormatsField(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, { format = it }, enabled = true, isEditing = false)
                        com.bernaferrari.changedetection.addedit.VisualChangeFilterField(0, true, {})
                    }
                }
            }
        }
        compose.onNodeWithText(minutes).performScrollTo().performClick().assertIsSelected()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement(digits15)
        compose.runOnIdle { Assert.assertEquals(15L, interval) }
        compose.onNodeWithText(source).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { Assert.assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.SOURCE, format) }
        val text = compose.onNodeWithText(off, useUnmergedTree = true).performScrollTo()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        Assert.assertFalse("$language off label clips at $scale", layouts.single().hasVisualOverflow)
        for (seconds in listOf(2, 5, 15)) {
            val waitLabel = runBlocking { getString(Res.string.turbo_every_seconds_short, seconds) }
            val waitLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(waitLabel, useUnmergedTree = true).performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(waitLayouts) }
            Assert.assertFalse("$language wait preset clips at $scale: ${waitLayouts.single().size}", waitLayouts.single().hasVisualOverflow)
            Assert.assertEquals("$language wait preset should keep its word intact at $scale", 1, waitLayouts.single().lineCount)
        }
        saveScreen("settings-localized")
        compose.onNodeWithContentDescription(custom).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText(com.bernaferrari.changedetection.util.localizedInteger(10))).performTextReplacement(digits7)
        compose.onNodeWithText(done).performClick()
        compose.runOnIdle { Assert.assertEquals(7000L, wait) }
    }

    @Test fun automaticChecksKeepTheirLayoutAndSaveTheChosenIntervalInEveryLanguage() {
        val daily = runBlocking { getString(Res.string.checks_daily) }
        val weekly = runBlocking { getString(Res.string.checks_weekly) }
        val fifteen = runBlocking { getString(Res.string.checks_minutes_short, 15) }
        val save = runBlocking { getString(Res.string.save) }
        var interval: Long? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    com.bernaferrari.changedetection.ui.settings.BackgroundSyncDialogContent(
                        com.bernaferrari.changedetection.ui.settings.SettingsUiState(syncInterval = 15L),
                        {},
                        { interval = it.syncInterval },
                    )
                }
            }
        }
        val original = compose.onNodeWithTag("checks-toggle-card").fetchSemanticsNode().boundsInRoot
        for (label in listOf(daily, weekly, fifteen)) {
            compose.onNodeWithText(label).performScrollTo().performClick().assertIsSelected()
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            Assert.assertFalse("$language/$label clips at $scale: ${layouts.single().size}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}", layouts.single().hasVisualOverflow)
            Assert.assertEquals(original.height, compose.onNodeWithTag("checks-toggle-card").fetchSemanticsNode().boundsInRoot.height)
        }
        val title = runBlocking { getString(Res.string.checks_title) }
        val output = File("../build/i18n/screens/checks-localized-$language-$scale.png").apply { parentFile.mkdirs() }
        val root = compose.onNode(isRoot() and hasAnyDescendant(hasText(title)), useUnmergedTree = true)
        ImageIO.write(root.captureToImage().toAwtImage(), "png", output)
        compose.onNodeWithText(save).performClick()
        compose.runOnIdle { Assert.assertEquals(15L, interval) }
    }

    @Test fun errorBannersUseTranslatedRecoveryTextAndKeepRetryWorking() {
        val result = com.bernaferrari.changedetection.error.ErrorHandler().handleError(com.bernaferrari.changedetection.error.NetworkError(503, "private server response"))
        val presentation = requireNotNull(result.presentation)
        val title = runBlocking { getString(presentation.title) }
        val body = runBlocking { getString(requireNotNull(presentation.body)) }
        val retry = runBlocking { getString(Res.string.retry) }
        var dismissed = false
        var retried = false
        val event = com.bernaferrari.changedetection.error.ErrorEvent(
            "test", result.type, result.severity, result.userMessage, result.description,
            com.bernaferrari.changedetection.error.ErrorContext.GENERAL, null, null, 0L, result.isRetryable,
            result.recoveryAction, result.suggestedActions, presentation,
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(320.dp, 760.dp)) {
                        com.bernaferrari.changedetection.ui.components.ErrorBanner(event, { dismissed = true }, { retried = true })
                    }
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText(body).assertIsDisplayed()
        compose.onNodeWithText("private server response", substring = true).assertDoesNotExist()
        for (label in listOf(title, body)) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            Assert.assertFalse("$language error text clips at $scale", layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithText(retry).performClick()
        compose.runOnIdle { Assert.assertTrue(dismissed && retried) }
        saveScreen("error-localized")
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
