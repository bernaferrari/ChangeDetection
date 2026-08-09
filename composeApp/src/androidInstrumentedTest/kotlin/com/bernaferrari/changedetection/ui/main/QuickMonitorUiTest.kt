/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Semantic regression tests for the reusable main-list surface. */
@RunWith(AndroidJUnit4::class)
class QuickMonitorUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun siteRowExposesStatus() {
        val title = "Synthetic price watch"
        val changed = "Updated just now"

        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SiteRow(
                    site = syntheticSite(title),
                    status = SiteStatus.Changed("just now"),
                    isQuickMonitored = true,
                    onClick = {},
                    onLongClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(changed).assertIsDisplayed()
    }

    private fun syntheticSite(title: String) = Site(
        title = title,
        url = "https://example.test/price",
        timestamp = 0,
        id = "synthetic-price-watch",
        isSuccessful = true,
        isBrowser = false,
        threshold = 0,
        isRead = false,
        isSyncEnabled = true,
        isNotificationEnabled = true,
        notes = "",
        colors = 0xFF00897B.toInt() to 0xFF26A69A.toInt(),
        constraints = ConstraintsRequired(false, false, false, false),
    )
}
