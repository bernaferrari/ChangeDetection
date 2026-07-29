/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Covers the compact comparison toolbar used by the image-diff experience. */
@RunWith(AndroidJUnit4::class)
class ComparisonUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun imageModesRemainSelectableWhileShareStaysAvailable() {
        var selectedMode by mutableStateOf(ComparisonMode.ImageDiff)

        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                ComparisonModeSelector(
                    modes = listOf(
                        ComparisonMode.ImageDiff,
                        ComparisonMode.ImageSwipe,
                        ComparisonMode.ImageBefore,
                        ComparisonMode.ImageAfter,
                    ),
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it },
                    canShare = true,
                    onShare = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Swipe").performClick()
        composeTestRule.onNodeWithContentDescription("Share comparison").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(ComparisonMode.ImageSwipe, selectedMode)
        }
    }
}
