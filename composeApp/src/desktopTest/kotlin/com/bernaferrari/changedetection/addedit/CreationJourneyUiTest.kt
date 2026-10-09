/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreationJourneyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun saveInProgressDisablesInputsAndNavigation() {
        var saving by mutableStateOf(false)
        compose.setContent {
            ChangeDetectionTheme {
                Box(Modifier.width(390.dp).height(844.dp)) {
                    AddEditConfirmScreen(
                        url = "https://example.test/program", urlError = null, title = "Program", captureMode = CaptureMode.TEXT,
                        isEditing = true, isSaving = saving, saveError = null, onUrlChange = {}, onTitleChange = {},
                        onNavigateUp = { error("must not dismiss a pending save") }, onSave = { saving = true },
                    )
                }
            }
        }
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Saving…").assertIsNotEnabled()
        compose.onNodeWithText("Name").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Navigate back").assertIsNotEnabled()
        compose.onNodeWithText("Page address").assertIsNotEnabled()
    }

    @Test fun pageEntryPreviewBackAndSavePreserveInput() {
        var previews = 0
        var saves = 0
        compose.setContent {
            var url by remember { mutableStateOf("") }
            var title by remember { mutableStateOf("") }
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                androidx.compose.runtime.CompositionLocalProvider(LocalBrowserAvailable provides false) {
                    Box(Modifier.width(390.dp).height(844.dp)) {
                        AddEditConfirmScreen(
                            url = url, urlError = null, title = title, captureMode = CaptureMode.TEXT,
                            isSaving = false, saveError = null, onUrlChange = { url = it }, onTitleChange = { title = it },
                            onNavigateUp = {}, onSave = { saves++ }, onPreview = { previews++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("What should we watch?").assertIsDisplayed()
        compose.onNodeWithText("Open page").assertIsNotEnabled()
        capture("creation-page-light")
        compose.onNode(hasSetTextAction() and hasText("Page address")).performTextInput("https://example.test/program")
        compose.onNodeWithText("Open page").performClick()
        compose.waitUntil(3_000) { previews == 1 }
        compose.onNodeWithText("Name").performTextInput("Program")
        capture("creation-content-light")
        compose.runOnIdle { assertEquals(0, saves) }
        compose.onNodeWithText("Start watching").performClick()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    private fun capture(name: String) {
        val file = File("../build/desktop-review/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", file)
    }
}
