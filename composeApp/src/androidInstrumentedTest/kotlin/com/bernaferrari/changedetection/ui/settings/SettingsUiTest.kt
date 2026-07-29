/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression coverage for the monitoring preferences' visible, selectable choices. */
@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun monitoringShowsAndSelectsBothComparisonMethodsFromHtmlDefault() {
        var selectedMethod by mutableStateOf(ComparisonMethod.HTML)

        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                MonitoringSettingsSection(
                    uiState = SettingsUiState(comparisonMethod = selectedMethod),
                    onBackgroundSyncClick = {},
                    onBrowserDelaySelected = {},
                    onComparisonMethodSelected = { selectedMethod = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Page text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Screenshots").performClick()

        composeTestRule.runOnIdle {
            assertEquals(ComparisonMethod.SCREENSHOT, selectedMethod)
        }
    }
}
