/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
    @get:Rule val compose = createComposeRule()
    private val site = Site("Conference program", "https://example.test/program", 3L, "", "page", 0 to 0)
        .copy(isSyncEnabled = true, isSuccessful = false, isRead = false, lastFailureCode = "CONNECTION")
    private val snap = Snap("captured-before-new-update", "page", 1_700_000_000_000L, "text/html", "UTF-8", 100)
    private var opened: String? = null
    private var historyOpened: String? = null
    private var edited = 0
    private var turbo = 0
    private var checks = 0
    private var pauses = 0
    private var schedules = 0

    private fun show(item: SiteAndLastSnap?, width: Dp = 360.dp, fontScale: Float = 1f, dark: Boolean = false, preview: SavedPagePreview = SavedPagePreview.Text("The program is now open."), schedule: MonitoringSchedule = MonitoringSchedule(), session: TurboState = TurboState()) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = true) {
                    Box(Modifier.width(width).testTag("detail-test-root")) {
                        PageDetailContent(item, false, preview, schedule, session, {}, { opened = it }, { edited++ }, { turbo++ }, { schedules++ }, {}, { checks++ }, { pauses++ }, onHistory = { historyOpened = it })
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        val target = File("../build/desktop-review/$name.png")
        target.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("detail-test-root").captureToImage().toAwtImage(), "png", target)
    }

    @Test fun savedVersionsOpensHistoryForTheDisplayedCaptureWithoutOpeningComparison() {
        show(SiteAndLastSnap(site, snap, 3), width = 320.dp, fontScale = 2f)
        compose.onNodeWithTag("page-details").performScrollToKey("history")
        compose.onNodeWithText("Saved versions").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(snap.snapId, historyOpened)
            assertNull(opened)
        }
    }

    @Test fun turboOnAnotherPageDoesNotClaimThisPageIsRunning() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true), snap, 3), session = TurboState(isActive = true, siteIds = setOf("another-page")))
        compose.onNodeWithTag("page-details").performScrollToKey("turbo")
        compose.onNodeWithText("A session is running. View its pages and controls.").assertDoesNotExist()
        compose.onNodeWithText("A closer watch, every few seconds.").assertIsDisplayed()
        compose.onNodeWithText("Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, turbo) }
    }

    @Test fun failedCheckKeepsSavedContentAndItsConcreteVersionAction() {
        show(SiteAndLastSnap(site, snap, 3))
        capture("detail-light")
        compose.onNodeWithText("The program is now open.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(opened) }
        compose.onNodeWithText("Compare").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(snap.snapId, opened) }
        compose.onNodeWithTag("page-details").performScrollToKey("turbo")
        compose.onNodeWithText("Turbo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, turbo) }
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Check now").performScrollTo().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Pause this page").performScrollTo().performClick()
        compose.onNodeWithText("Regular checks · all pages").performScrollTo().performClick()
        capture("detail-preferences-light")
        compose.runOnIdle {
            assertEquals(1, checks)
            assertEquals(1, pauses)
            assertEquals(1, schedules)
        }
    }

    @Test fun largeDarkTextKeepsAllActionsReachableWhenPreviewIsUnavailable() {
        show(SiteAndLastSnap(site, snap, 3), fontScale = 2f, dark = true, preview = SavedPagePreview.Unavailable)
        capture("detail-large-dark")
        compose.onNodeWithTag("page-details").performScrollToKey("saved")
        compose.onNodeWithText("Compare").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("turbo")
        compose.onNodeWithText("Turbo").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Pause this page").performScrollTo().assertIsDisplayed().performClick()
        capture("detail-preferences-large-dark")
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

    @Test fun regionFailureOffersEditWithoutLosingSavedVersion() {
        show(SiteAndLastSnap(site.copy(lastFailureCode = "REGION"), snap, 3))
        compose.onNodeWithText("Selected region unavailable").assertIsDisplayed()
        compose.onNodeWithText("Edit").performClick()
        compose.runOnIdle { assertEquals(1, edited) }
        compose.onNodeWithText("Compare").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(snap.snapId, opened) }
    }

    @Test fun failedCheckOffersRetryAndRetainsEvidence() {
        show(SiteAndLastSnap(site, snap, 3))
        compose.onNodeWithText("Retry").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, checks) }
        compose.onNodeWithText("Compare").performScrollTo().assertIsDisplayed()
    }

    @Test fun weekScheduleUsesDaysAndKeepsGlobalScheduleAction() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true), snap, 3), schedule = MonitoringSchedule(intervalMinutes = 10080))
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Sync every 7 days").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, schedules) }
    }

    @Test fun narrowLargeTextTitleKeepsWholeWordsAndSavedContentReachable() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true), snap, 3), width = 280.dp, fontScale = 2f, dark = true)
        val title = requireNotNull(site.title)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertFalse(layout.hasVisualOverflow)
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line, visibleEnd = true)
            assertTrue("Title line should end at a word boundary: ${title.substring(0, end)} ($end)", end == title.length || title[end].isWhitespace() || (end > 0 && title[end - 1].isWhitespace()))
            assertFalse(layout.isLineEllipsized(line))
        }
        capture("detail-narrow-large-dark")
        compose.onNodeWithText("Compare").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(snap.snapId, opened) }
    }

    @Test fun checkingDisablesRepeatedCheckButKeepsPauseAvailable() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), snap, 3, isSyncing = true))
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Checking…").performScrollTo().assertIsDisplayed().assertIsNotEnabled().performClick()
        compose.onNodeWithText("Pause this page").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(0, checks)
            assertEquals(1, pauses)
        }
    }

    @Test fun pausedPageKeepsItsUnreadChangeAndShowsTheActualSharedSchedule() {
        show(SiteAndLastSnap(site.copy(isSyncEnabled = false, isSuccessful = true, lastFailureCode = null), snap, 3), width = 320.dp, fontScale = 2f, schedule = MonitoringSchedule(intervalMinutes = 10080))
        compose.onNodeWithText("Paused").assertIsDisplayed()
        compose.onNodeWithText("New change").assertIsDisplayed()
        capture("detail-paused-large")
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Sync every 7 days").performScrollTo().performClick()
        compose.onNodeWithText("Resume this page").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, schedules) }
    }

    @Test fun disabledSharedScheduleIsDistinctFromAnIndividuallyPausedPage() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true), snap, 3), schedule = MonitoringSchedule(enabled = false))
        compose.onNodeWithText("Paused").assertDoesNotExist()
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Disabled").performScrollTo().performClick()
        compose.onNodeWithText("Pause this page").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, schedules) }
    }

    @Test fun unavailableCaptureOpensSettingsInsteadOfRetryingAnUnsupportedCheck() {
        show(SiteAndLastSnap(site.copy(lastFailureCode = "CAPTURE_UNAVAILABLE"), snap, 3))
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, edited)
            assertEquals(0, checks)
        }
        compose.onNodeWithText("Compare").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(snap.snapId, opened) }
    }

    @Test fun pausedPageOffersResumeAndStillAllowsManualCheck() {
        show(SiteAndLastSnap(site.copy(isSyncEnabled = false, isSuccessful = true, lastFailureCode = null), snap, 3))
        compose.onNodeWithTag("page-details").performScrollToKey("preferences")
        compose.onNodeWithText("Check now").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Resume this page").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Pause this page").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, checks)
            assertEquals(1, pauses)
        }
    }

    @Test fun deletedPageDoesNotOfferStaleControls() {
        show(null)
        compose.onNodeWithText("This page is no longer in your watchlist.").assertIsDisplayed()
        compose.onNodeWithText("Turbo").assertDoesNotExist()
        compose.onNodeWithText("Compare").assertDoesNotExist()
    }
}
