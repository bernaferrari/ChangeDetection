/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.detail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PageDetailUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val site = Site("Conference program", "https://example.test/program", 3L, "", "page", 0 to 0)
        .copy(isSyncEnabled = true, isSuccessful = false, isRead = false, lastFailureCode = "connection")
    private val snap = Snap("captured-before-new-update", "page", 1_700_000_000_000L, "text/html", "UTF-8", 100)
    private var opened: String? = null
    private var edited = 0
    private var turbo = 0
    private var checks = 0
    private var pauses = 0
    private var schedules = 0

    private fun show(item: SiteAndLastSnap?, fontScale: Float = 1f, dark: Boolean = false, preview: SavedPagePreview = SavedPagePreview.Text("The program is now open.")) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = true) {
                    Box(Modifier.width(360.dp)) {
                        PageDetailContent(item, false, preview, MonitoringSchedule(), TurboState(), {}, { opened = it }, { edited++ }, { turbo++ }, { schedules++ }, {}, { checks++ }, { pauses++ })
                    }
                }
            }
        }
    }

    @Test fun failedCheckKeepsSavedContentAndItsConcreteVersionAction() {
        show(SiteAndLastSnap(site, snap, 3))
        compose.onNodeWithText("The program is now open.").assertIsDisplayed()
        compose.runOnIdle { assertNull(opened) }
        compose.onNodeWithText("Compare").performClick()
        compose.runOnIdle { assertEquals(snap.snapId, opened) }
        compose.onNodeWithTag("page-details").performScrollToKey("turbo")
        compose.onNodeWithText("Turbo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, turbo) }
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Check now").performScrollTo().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Pause this page").performScrollTo().performClick()
        compose.onNodeWithText("Regular checks · all pages").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, checks)
            assertEquals(1, pauses)
            assertEquals(1, schedules)
        }
    }

    @Test fun largeDarkTextKeepsAllActionsReachableWhenPreviewIsUnavailable() {
        show(SiteAndLastSnap(site, snap, 3), fontScale = 2f, dark = true, preview = SavedPagePreview.Unavailable)
        compose.onNodeWithText("Compare").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("turbo")
        compose.onNodeWithText("Turbo").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Pause this page").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Edit").performClick()
        compose.runOnIdle {
            assertEquals(snap.snapId, opened)
            assertEquals(1, turbo)
            assertEquals(1, pauses)
            assertEquals(1, edited)
        }
    }

    @Test fun newPageOffersFirstCheckWithoutAnInventedPreview() {
        show(SiteAndLastSnap(site.copy(lastFailureCode = null), null))
        compose.onNodeWithText("No saved version yet").assertIsDisplayed()
        compose.onNodeWithText("Compare").assertDoesNotExist()
        compose.onAllNodesWithText("Check now").onFirst().performClick()
        compose.runOnIdle {
            assertEquals(1, checks)
            assertNull(opened)
        }
    }

    @Test fun deletedPageDoesNotOfferStaleControls() {
        show(null)
        compose.onNodeWithText("This page is no longer in your watchlist.").assertIsDisplayed()
        compose.onNodeWithText("Turbo").assertDoesNotExist()
        compose.onNodeWithText("Compare").assertDoesNotExist()
    }
}
