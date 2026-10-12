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
        var showComparisonMethodDialog by mutableStateOf(false)

        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                MonitoringSettingsSection(
                    uiState = SettingsUiState(comparisonMethod = selectedMethod),
                    onBackgroundSyncClick = {},
                    onBrowserDelayClick = {},
                    onComparisonMethodClick = { showComparisonMethodDialog = true },
                )
                if (showComparisonMethodDialog) {
                    ComparisonMethodChoiceDialog(
                        selectedMethod = selectedMethod,
                        onDismiss = { showComparisonMethodDialog = false },
                        onConfirm = {
                            selectedMethod = it
                            showComparisonMethodDialog = false
                        },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Page text").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Screenshots").performClick()
        composeTestRule.onNodeWithText("Done").performClick()

        composeTestRule.runOnIdle {
            assertEquals(ComparisonMethod.SCREENSHOT, selectedMethod)
        }
    }
}
