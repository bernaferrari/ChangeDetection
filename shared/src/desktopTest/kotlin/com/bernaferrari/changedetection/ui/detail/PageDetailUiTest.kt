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
import com.bernaferrari.changedetection.repo.CaptureMode
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

    private fun show(item: SiteAndLastSnap?, versions: List<Snap> = listOf(latest, older), fontScale: Float = 1f, schedule: MonitoringSchedule = MonitoringSchedule(), height: Int = 1600, textRecovery: TextRecovery? = null) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.width(400.dp).height(height.dp)) {
                        PageDetailContent(
                            item = item, isLoading = false, versions = versions, schedule = schedule, turbo = turbo,
                            onBack = {}, onCompare = { opened = it }, onEdit = { edited++ }, onTurbo = { turboOpened++ }, onSchedule = { schedules++ },
                            onOpenUrl = { browserOpened++ }, onCheck = { checks++ }, onMarkRead = { read++ }, onSetPaused = { paused = it },
                            onSetNotify = { notify = it }, notificationsAvailable = true, onDelete = {}, onHistory = { historyOpened = it }, textRecovery = textRecovery,
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

    @Test fun aRunOfFailuresSaysHowLongThePageHasGoneUnwatched() {
        show(SiteAndLastSnap(site.copy(failureCount = 4), latest, 2))
        compose.onNodeWithText("Failed checks in a row: 4", substring = true).assertIsDisplayed()
    }

    @Test fun aSingleFailureIsNotCalledARun() {
        show(SiteAndLastSnap(site.copy(failureCount = 1), latest, 2))
        compose.onNodeWithText("Failed checks in a row", substring = true).assertDoesNotExist()
    }

    @Test fun aRefusedCheckOffersRetryFirstAndSignInSecond() {
        show(SiteAndLastSnap(site.copy(lastFailureCode = "ACCESS_DENIED"), latest, 2))
        compose.onNodeWithText("Some sites block automated visits", substring = true).assertIsDisplayed()
        capture("detail-failure-refused")
        compose.onNodeWithText("Retry").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, checks)
            assertEquals(1, edited)
        }
    }

    @Test fun aWorkingRouteIsFoundQuietlyAndTakenOnlyWhenChosen() {
        var trials = 0
        var switched = 0
        val source = site.copy(captureMode = CaptureMode.SOURCE, lastFailureCode = "LOGIN", failureCount = 2)
        show(
            SiteAndLastSnap(source, latest, 2),
            textRecovery = TextRecovery(works = {
                trials++
                true
            }, switch = {
                switched++
                true
            }),
        )
        compose.onNodeWithText("Text works for this page").assertIsDisplayed()
        capture("detail-failure-recovery")
        compose.runOnIdle {
            assertEquals("the page is tried once, not on every redraw", 1, trials)
            assertEquals("finding a route must never change how the page is checked", 0, switched)
        }
        compose.onNodeWithText("Switch to Text").performClick()
        compose.runOnIdle { assertEquals(1, switched) }
    }

    @Test fun nothingIsOfferedWhenTheOtherRouteFailsToo() {
        val source = site.copy(captureMode = CaptureMode.SOURCE, lastFailureCode = "ACCESS_DENIED")
        show(SiteAndLastSnap(source, latest, 2), textRecovery = TextRecovery(works = { false }, switch = { error("nothing to switch to") }))
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Text works for this page").assertDoesNotExist()
        compose.onNodeWithText("Looking for another way to check this page…").assertDoesNotExist()
    }

    @Test fun aPageThatIsSimplyDownIsNotTriedAnotherWay() {
        var trials = 0
        show(
            SiteAndLastSnap(site.copy(lastFailureCode = "SERVER", failureCount = 6), latest, 2),
            textRecovery = TextRecovery(works = {
                trials++
                true
            }, switch = { true }),
        )
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Text works for this page").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, trials) }
    }

    @Test fun theTrialWaitsForARunningCheck() {
        var trials = 0
        var item by mutableStateOf(SiteAndLastSnap(site.copy(lastFailureCode = "PAGE_UNSETTLED"), latest, 2, isSyncing = true))
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                PageDetailContent(
                    item = item, isLoading = false, versions = listOf(latest, older), schedule = MonitoringSchedule(), turbo = turbo,
                    onBack = {}, onCompare = {}, onEdit = {}, onTurbo = {}, onSchedule = {}, onOpenUrl = {}, onCheck = {}, onMarkRead = {}, onSetPaused = {},
                    onSetNotify = {}, notificationsAvailable = true, onDelete = {},
                    textRecovery = TextRecovery(works = {
                        trials++
                        true
                    }, switch = { true }),
                )
            }
        }
        compose.onNodeWithText("Page didn’t finish loading").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, trials) }
        compose.runOnIdle { item = item.copy(isSyncing = false) }
        compose.onNodeWithText("Text works for this page").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, trials) }
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
