/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VisualFilterUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filterChoicesAreExplicitAndReopenWithTheSelectedValue() = exercise(1f)

    @Test fun filterChoicesRemainReachableAtLargeText() = exercise(1.8f)

    private fun exercise(scale: Float) {
        var changed = 0
        compose.setContent {
            var value by remember { mutableStateOf(0) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        VisualChangeFilterField(value, enabled = true) {
                            value = it
                            changed = it
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Visual change filter").performClick()
        compose.onNodeWithText("At least 1%").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(100, changed) }
        compose.onNodeWithText("Visual change filter").performClick()
        compose.onNode(hasText("At least 1%") and isSelected()).performScrollTo().assertIsSelected()
        val output = File("../build/desktop-review/visual-filter-${if (scale == 1f) "normal" else "large"}.png")
        output.parentFile.mkdirs()
        ImageIO.write(compose.onNode(isDialog()).captureToImage().toAwtImage(), "png", output)
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(100, changed) }
        compose.onNodeWithText("Visual change filter").performClick()
        compose.onNodeWithText("Off · no visual filtering").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, changed) }
    }
}
