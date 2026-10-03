/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SiteOptionsKeyboardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun accessibleDismissRestoresFocusAndKeyboardCanActivateEdit() {
        var edited = 0
        compose.setContent {
            var open by remember { mutableStateOf(false) }
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Button(onClick = { open = true }) { Text("Page options") }
                if (open) {
                    SiteOptionsBottomSheet(
                        site = Site("Conference program", "https://example.test", 0L, "", "page", 0 to 0),
                        onDismiss = { open = false },
                        onRefresh = {},
                        onEdit = {
                            edited++
                            open = false
                        },
                        onDelete = {},
                        onOpenInBrowser = {},
                        onToggleMonitoring = {},
                        onToggleNotifications = {},
                        notificationDeliveryAvailable = false,
                        onNotificationSettings = {},
                    )
                }
            }
        }
        val launcher = compose.onNodeWithText("Page options")
        launcher.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        launcher.performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Refresh").assertIsDisplayed()
        compose.onNodeWithText("Refresh").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        // Desktop dialog dismissal is routed through the platform NavigationEventDispatcher,
        // outside this scene's synthetic key input. Exercise the sheet's accessibility
        // dismissal action here; native Escape remains a separate acceptance check.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        // The underlying window should recover its previous focus after the modal closes.
        launcher.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Refresh").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Refresh").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithText("Edit").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Edit").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, edited) }
        launcher.assertIsFocused()
    }
}
