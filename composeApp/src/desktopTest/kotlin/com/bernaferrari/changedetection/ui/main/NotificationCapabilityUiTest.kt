/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.sync.NoOpChangeNotifier
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NotificationCapabilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unsupportedDeliveryDoesNotOfferInertNotificationActions() {
        assertFalse(NoOpChangeNotifier.supportsDelivery)
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface { Column { NotificationPreference(NoOpChangeNotifier.supportsDelivery, true, { error("Inert toggle") }, { error("Inert settings") }) } }
            }
        }
        compose.onNodeWithText("Changes are saved in the app.", substring = true).assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test fun availableDeliveryKeepsToggleAndSystemRecoveryReachable() {
        var toggles = 0
        var settings = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface { Column { NotificationPreference(true, false, { toggles++ }, { settings++ }) } }
            }
        }
        compose.onNodeWithText("Enable notifications").performClick()
        compose.onNodeWithText("System notification settings").performClick()
        compose.runOnIdle {
            assertEquals(1, toggles)
            assertEquals(1, settings)
        }
    }
}
