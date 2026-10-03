/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CaptureModeUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unavailableSavedModeStaysVisibleUntilAnExplicitSupportedChoice() {
        var chosen = CaptureMode.SCREENSHOT
        compose.setContent {
            var mode by remember { mutableStateOf(CaptureMode.SCREENSHOT) }
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                CaptureModeField(mode, {
                    mode = it
                    chosen = it
                }, enabled = true, isEditing = true)
            }
        }
        compose.onNodeWithText("Screenshot").performClick()
        compose.onNodeWithText("This saved mode is unavailable on this platform. It stays unchanged unless you choose another mode.").assertIsDisplayed()
        compose.onNode(isSelected()).assertIsNotEnabled()
        val output = File("../build/desktop-review/capture-mode-preserved.png")
        output.parentFile.mkdirs()
        ImageIO.write(compose.onNode(isDialog()).captureToImage().toAwtImage(), "png", output)
        compose.runOnIdle { assertEquals(CaptureMode.SCREENSHOT, chosen) }
        compose.onNodeWithText("Readable text").performClick()
        compose.runOnIdle { assertEquals(CaptureMode.TEXT, chosen) }
        compose.onNodeWithText("Readable text").assertIsDisplayed()
    }
}
