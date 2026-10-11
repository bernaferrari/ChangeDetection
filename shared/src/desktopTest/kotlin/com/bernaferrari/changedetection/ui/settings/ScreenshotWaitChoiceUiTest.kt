/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScreenshotWaitChoiceUiTest {
    @get:Rule val compose = createComposeRule()
    private var saved by mutableStateOf(10_000L)

    private fun show() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Column(Modifier.width(320.dp)) { ScreenshotWaitChoice(saved) { saved = it } }
            }
        }
    }

    @Test fun customWaitCanBeEditedThenChangedToPresetWithoutMovingTheRow() {
        show()
        val preset = compose.onNode(hasText("15 s") and hasClickAction())
        val before = preset.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Custom…").assertIsSelected().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("9")
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(9000L, saved) }
        assertEquals(before, preset.fetchSemanticsNode().boundsInRoot)
        preset.performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(15000L, saved) }
        assertEquals(before, preset.fetchSemanticsNode().boundsInRoot)
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test fun invalidCustomWaitCannotOverwriteTheSavedValueAndCancelKeepsIt() {
        show()
        compose.onNodeWithContentDescription("Custom…").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("61")
        compose.onNodeWithText("Done").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(10000L, saved) }
        compose.onNodeWithContentDescription("Custom…").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(0L, saved) }
    }
}
