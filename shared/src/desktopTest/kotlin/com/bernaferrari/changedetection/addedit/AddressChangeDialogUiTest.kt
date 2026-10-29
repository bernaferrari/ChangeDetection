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

    @Test fun addressConfirmationIsVisibleWhileReviewingAPage() {
        var editing by mutableStateOf(false)
        var confirm by mutableStateOf(false)
        var kept = 0
        compose.setContent {
            CompositionLocalProvider(LocalBrowserAvailable provides false, com.bernaferrari.changedetection.ui.kit.LocalPageIcons provides false) {
                ChangeDetectionTheme {
                    AddEditConfirmScreen(
                        url = "https://example.test/changed", urlError = null, title = "Page",
                        captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT,
                        isEditing = editing, isSaving = false, saveError = null,
                        onUrlChange = {}, onTitleChange = {}, onNavigateUp = {},
                        onSave = { confirm = true }, showUrlChangeDialog = confirm,
                        onKeepHistory = { kept++ },
                    )
                }
            }
        }
        compose.onNodeWithText("Open page").performClick()
        // The same review branch is used after an existing page's browser picker returns.
        compose.runOnIdle { editing = true }
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Keep history").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.runOnIdle { assertEquals(1, kept) }
    }

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
