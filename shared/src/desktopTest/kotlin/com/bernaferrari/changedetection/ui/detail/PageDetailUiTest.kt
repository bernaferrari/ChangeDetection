/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PageDetailUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = System.currentTimeMillis()
    private val site = Site("Conference program", "https://example.test/program", now - 60_000, "", "page", 0 to 0)
        .copy(isSyncEnabled = true, isSuccessful = false, isRead = false, lastFailureCode = "CONNECTION")
    private val older = Snap("older", "page", now - 3 * 86_400_000L, "text/html", "UTF-8", 100)
    private val latest = Snap("latest", "page", now - 3_600_000L, "text/html", "UTF-8", 100)
    private var historyOpened: String? = null
    private var opened: String? = null
    private var edited = 0
    private var checks = 0
    private var paused: Boolean? = null
    private var notify: Boolean? = null
    private var schedules = 0
    private var read = 0
    private var turbo by mutableStateOf(TurboState())
    private var turboOpened = 0
    private var browserOpened = 0

    private fun show(item: SiteAndLastSnap?, versions: List<Snap> = listOf(latest, older), fontScale: Float = 1f, schedule: MonitoringSchedule = MonitoringSchedule(), height: Int = 1600) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.width(400.dp).height(height.dp)) {
                        PageDetailContent(
                            item = item, isLoading = false, versions = versions, schedule = schedule, turbo = turbo,
                            onBack = {}, onCompare = { opened = it }, onEdit = { edited++ }, onTurbo = { turboOpened++ }, onSchedule = { schedules++ },
                            onOpenUrl = { browserOpened++ }, onCheck = { checks++ }, onMarkRead = { read++ }, onSetPaused = { paused = it },
                            onSetNotify = { notify = it }, notificationsAvailable = true, onDelete = {}, onHistory = { historyOpened = it },
                        )
                    }
                }
            }
        }
    }

    @Test fun statusIsInformationalAndTurboOnlyLooksActiveForItsOwnPage() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), latest, 2))
        compose.onNodeWithTag("page-check-status").assertHasNoClickAction()
        compose.onNodeWithText("Turbo").performClick()
        compose.onNodeWithText("Open").performClick()
        compose.runOnIdle {
            assertEquals(1, turboOpened)
            assertEquals(1, browserOpened)
            turbo = TurboState(isActive = true, siteIds = setOf("another-page"))
        }
        compose.onNodeWithText("Turbo").assertIsDisplayed()
        compose.onNodeWithText("Turbo is on").assertDoesNotExist()
        capture("detail-actions-idle")
        compose.runOnIdle { turbo = turbo.copy(siteIds = setOf(site.id)) }
        compose.onNodeWithText("Turbo is on").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, turboOpened) }
        capture("detail-actions-active")
    }

    @Test fun topBarShowsThePageNameAfterItScrollsOutAndClearsOnReturn() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), latest, 2), height = 760)
        compose.onNodeWithTag("app-bar-title").assertDoesNotExist()
        val list = compose.onNodeWithTag("page-details")
        val name = compose.onNodeWithTag("page-name").fetchSemanticsNode().boundsInRoot
        val distance = name.bottom - list.fetchSemanticsNode().boundsInRoot.top + 1f
        list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, distance) }
        compose.onNodeWithTag("app-bar-title").assertTextEquals("Conference program").assertIsDisplayed()
        compose.onNodeWithTag("page-check-status").assertIsDisplayed()
        list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -distance) }
        compose.onNodeWithTag("app-bar-title").assertDoesNotExist()
        compose.onNodeWithTag("page-name").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val file = File("../build/desktop-review/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun failingPageExplainsAndRetriesWhileKeepingHistory() {
        show(SiteAndLastSnap(site, latest, 2))
        compose.onNodeWithText("Retry").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, checks) }
        compose.onNodeWithText("Latest change").assertIsDisplayed()
        compose.onAllNodesWithText("Compare").onFirst().performClick()
        compose.runOnIdle { assertEquals(latest.snapId, opened) }
    }

    @Test fun savedVersionsHeadingAndSeeAllOpenHistoryInsteadOfComparison() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), latest, 2), height = 760)
        compose.onNodeWithTag("page-details").performScrollToKey("versions-h")
        compose.onNodeWithText("Versions").performScrollTo().assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(latest.snapId, historyOpened)
            assertNull(opened)
            historyOpened = null
        }
        compose.onNodeWithText("See all").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(latest.snapId, historyOpened)
            assertNull(opened)
        }
    }

    @Test fun regionFailureLeadsToSettingsInsteadOfARetry() {
        show(SiteAndLastSnap(site.copy(lastFailureCode = "REGION"), latest, 2))
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithText("Fix settings").performClick()
        compose.runOnIdle { assertEquals(1, edited) }
    }

    @Test fun unreadChangeCanBeMarkedRead() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), latest, 2))
        compose.onNodeWithText("New").assertIsDisplayed()
        compose.onNodeWithText("Mark read").performClick()
        compose.runOnIdle { assertEquals(1, read) }
    }

    @Test fun newPageOffersAFirstCheckInsteadOfAnInventedPreview() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null), null), versions = emptyList())
        compose.onNodeWithText("No saved version yet").assertIsDisplayed()
        compose.onNodeWithText("Mark read").assertDoesNotExist()
        compose.onAllNodesWithText("Check now").onFirst().performClick()
        compose.runOnIdle {
            assertEquals(1, checks)
            assertNull(opened)
        }
    }

    @Test fun settingsRowsToggleNotificationsPauseAndOpenSchedule() {
        show(SiteAndLastSnap(site.copy(isSuccessful = true, lastFailureCode = null, isNotificationEnabled = true), latest, 2), fontScale = 1.6f)
        compose.onNodeWithTag("page-details").performScrollToKey("watch")
        compose.onNodeWithText("Notify me").performScrollTo().performClick()
        compose.onNodeWithText("Pause checks").performScrollTo().performClick()
        compose.onNodeWithText("App check schedule").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(false, notify)
            assertEquals(true, paused)
            assertEquals(1, schedules)
        }
    }

    @Test fun deletedPageDoesNotOfferStaleControls() {
        show(null)
        compose.onNodeWithText("This page is no longer in your watchlist.").assertIsDisplayed()
        compose.onNodeWithText("Turbo").assertDoesNotExist()
        compose.onNodeWithText("Compare").assertDoesNotExist()
    }
}
