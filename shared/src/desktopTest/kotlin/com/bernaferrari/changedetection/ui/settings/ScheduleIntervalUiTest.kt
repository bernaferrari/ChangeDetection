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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.toAwtImage
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.CompletableDeferred

class ScheduleIntervalUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun delayedCustomScheduleShowsItsSavedValue() {
        var source by mutableStateOf(SettingsUiState(isLoaded = false))
        compose.setContent {
            ChangeDetectionTheme {
                BackgroundSyncDialogContent(source, {}, {})
            }
        }
        compose.runOnIdle { source = SettingsUiState(syncInterval = 90L) }
        compose.onNodeWithText("Custom…").performScrollTo().assertIsSelected()
        compose.onNode(hasSetTextAction()).performScrollTo().assertTextContains("90")
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test fun savingScheduleDisablesFurtherEditsUntilItCompletes() {
        val finishSave = CompletableDeferred<Unit>()
        var saved: SettingsUiState? = null
        compose.setContent {
            ChangeDetectionTheme {
                BackgroundSyncDialogContent(SettingsUiState(syncInterval = 60L), {}, {
                    saved = it
                    finishSave.await()
                })
            }
        }
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("15 min").assertIsNotEnabled()
        compose.onNodeWithText("Custom…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(60L, saved?.syncInterval)
            finishSave.complete(Unit)
        }
    }

    @Test fun delayedPreferencesInitializeDraftOnceAndCannotSaveDefaults() {
        var source by mutableStateOf(SettingsUiState(isLoaded = false))
        var saved: SettingsUiState? = null
        var dismissed = false
        compose.setContent {
            ChangeDetectionTheme {
                BackgroundSyncDialogContent(source, { dismissed = true }, { saved = it }, { source = SettingsUiState(syncInterval = 360L) })
            }
        }
        compose.onNodeWithText("Loading…").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertNull(saved)
            source = SettingsUiState(syncInterval = 10080L)
        }
        compose.onNodeWithText("Weekly").assertIsSelected()
        compose.onNodeWithText("15 min").performClick()
        compose.runOnIdle { source = source.copy(storageBytes = 8000L, syncInterval = 360L) }
        compose.onNodeWithText("15 min").assertIsSelected()
        compose.runOnIdle { source = source.copy(storageBytes = null, storageLoadFailed = true) }
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithText("15 min").assertIsSelected()
        compose.runOnIdle { source = SettingsUiState(isLoaded = false, loadFailed = true) }
        compose.onNodeWithText("Could not load settings. Try again.").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("15 min").assertIsSelected()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil { saved != null }
        compose.runOnIdle {
            assertEquals(15L, saved?.syncInterval)
            assertTrue(dismissed)
        }
    }

    @Test fun largeTypeUnitsRemainWholeAndPresetsExposeCompleteDurations() {
        var interval by mutableStateOf(60L)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.width(280.dp).height(700.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            SyncIntervalInput(interval, { interval = it })
                        }
                    }
                }
            }
        }
        for (unit in listOf("minutes", "hours", "days")) {
            val node = compose.onNodeWithText(unit, useUnmergedTree = true)
            node.performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            assertFalse("$unit: ${layouts.single().size} right=${layouts.single().getLineRight(0)} bottom=${layouts.single().getLineBottom(0)} w=${layouts.single().didOverflowWidth} h=${layouts.single().didOverflowHeight}", layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithContentDescription("Sync every 7 days").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(10080L, interval) }
        compose.onNodeWithContentDescription("Sync every 15 minutes").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(15L, interval) }
        val file = File("../build/desktop-review/schedule-interval-large-dark.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun invalidInputKeepsLastValidIntervalAndPresetRecovers() {
        var interval by mutableStateOf(60L)
        var valid = true
        compose.setContent {
            ChangeDetectionTheme {
                Column(Modifier.width(400.dp).verticalScroll(rememberScrollState())) {
                    SyncIntervalInput(interval, { interval = it }, { valid = it })
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        compose.runOnIdle {
            assertFalse(valid)
            assertEquals(60L, interval)
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("999999999999999999999999")
        compose.runOnIdle {
            assertFalse(valid)
            assertEquals(60L, interval)
        }
        compose.onNodeWithContentDescription("Sync every 7 days").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(valid)
            assertEquals(10080L, interval)
        }
    }
}
