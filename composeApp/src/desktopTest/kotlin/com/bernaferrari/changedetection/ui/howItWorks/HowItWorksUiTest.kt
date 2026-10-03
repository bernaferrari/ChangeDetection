/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.background.MonitoringCapabilities
import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HowItWorksUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun desktopExplainsExecutionAndReachableReturnAtLargeText() {
        var back = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme {
                    Box(Modifier.size(320.dp, 500.dp)) {
                        HowItWorksContent({ back++ }, MonitoringCapabilities(MonitoringExecution.WhileOpen))
                    }
                }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Checks run only while the app is open."))
        compose.onNodeWithText("Checks run only while the app is open.").assertIsDisplayed()
        compose.onNodeWithText("Use battery-friendly background checks for everyday tracking.").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Turbo stops when its timer ends. You can stop it sooner at any time."))
        compose.onNodeWithText("Turbo stops when its timer ends. You can stop it sooner at any time.").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Back to app"))
        compose.onNodeWithText("Back to app").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test fun previewDoesNotPromiseUnavailableMonitoring() {
        compose.setContent {
            ChangeDetectionTheme {
                Box(Modifier.size(390.dp, 700.dp)) {
                    HowItWorksContent({}, MonitoringCapabilities(MonitoringExecution.Unavailable, turbo = false))
                }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Automatic checks are unavailable in this preview."))
        compose.onNodeWithText("Automatic checks are unavailable in this preview.").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Turbo is unavailable in this preview"))
        compose.onNodeWithText("Turbo is unavailable in this preview").assertIsDisplayed()
        compose.onNodeWithText("Select specific pages and check them every 15 seconds to 2 minutes for up to 3 hours.").assertDoesNotExist()
    }
}
