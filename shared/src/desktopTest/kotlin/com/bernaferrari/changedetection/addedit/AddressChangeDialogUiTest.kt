package com.bernaferrari.changedetection.addedit

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddressChangeDialogUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultConfirmationKeepsHistoryAndSelectionAloneDoesNotResetIt() {
        var kept = 0
        var reset = 0
        compose.setContent {
            ChangeDetectionTheme { AddressChangeDialog({}, { kept++ }, { reset++ }) }
        }
        compose.onNodeWithText("Keep history").assertIsSelected()
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle {
            assertEquals(1, kept)
            assertEquals(0, reset)
        }
        compose.onNodeWithText("Start fresh").performClick()
        compose.runOnIdle { assertEquals(0, reset) }
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle {
            assertEquals(1, kept)
            assertEquals(1, reset)
        }
    }

    @Test fun cancellingAtLargeTextResetsChoiceWhenDialogIsOpenedAgain() {
        var open by mutableStateOf(true)
        var kept = 0
        var reset = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme {
                    if (open) AddressChangeDialog({ open = false }, { kept++ }, { reset++ })
                }
            }
        }
        compose.onNodeWithText("Start fresh").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(0, kept)
            assertEquals(0, reset)
            open = true
        }
        compose.onNodeWithText("Keep history").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, kept)
            assertEquals(0, reset)
        }
    }
}
