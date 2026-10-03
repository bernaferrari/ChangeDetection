/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toAwtImage
import javax.imageio.ImageIO
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TurboUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = System.currentTimeMillis()
    private val pages = listOf(
        page("1", "Concert tickets", "tickets.example.com/summer", successful = true),
        page("2", "Camera restock", "shop.example.com/camera", successful = false),
        page("3", "Application results", "university.example.com/results", successful = true),
    )

    @Test fun unavailablePreviewExplainsTheLimitWithoutOfferingStartAndStillAllowsStop() {
        var active by mutableStateOf(false)
        var stops = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TurboScreen(
                    sites = pages,
                    quickMonitorState = QuickMonitorState(isActive = active, siteIds = setOf("1")),
                    onBack = {},
                    onAddSite = { error("Unavailable preview must not offer setup") },
                    onStartTurbo = { _, _, _ -> error("Unavailable preview must not start") },
                    onStopTurbo = { stops++ },
                    onOpenSite = {},
                    turboAvailable = false,
                )
            }
        }
        compose.onNodeWithText("Turbo is unavailable in this preview").assertIsDisplayed()
        compose.onNodeWithText("Use the Android or desktop app to start a monitoring session.").assertIsDisplayed()
        compose.onNodeWithText("Start Turbo").assertDoesNotExist()
        compose.onNodeWithContentDescription("Every 30 seconds").assertDoesNotExist()
        capture("turbo-preview-unavailable")
        compose.runOnIdle { active = true }
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, stops) }
    }

    @Test fun pausedPageOpensItsDetailsWithoutStartingOrSelectingIt() {
        var opened: String? = null
        var started = false
        val paused = pages.first().let { it.copy(site = it.site.copy(isSyncEnabled = false)) }
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.size(400.dp, 850.dp)) {
                    TurboScreen(sites = listOf(paused), quickMonitorState = QuickMonitorState(), onAddSite = {}, onStartTurbo = { _, _, _ -> started = true }, onStopTurbo = {}, onOpenSite = { opened = it })
                }
            }
        }
        compose.onNodeWithText("Start Turbo").assertIsNotEnabled()
        compose.onNodeWithText("Paused · Open to resume", substring = true).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(paused.site.id, opened)
            assertFalse(started)
        }
    }

    @Test fun oldUnreadVersionIsNotPresentedAsAnUpdateFromThisSession() {
        val started = now - 60_000
        var snapshotTime by mutableLongStateOf(started - 1)
        compose.setContent {
            val row = pages.first().copy(
                site = pages.first().site.copy(timestamp = now, isRead = false),
                snap = com.bernaferrari.changedetection.repo.Snap("version", "1", snapshotTime, "text/plain", "UTF-8", 10),
                snapshotCount = 2,
            )
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                com.bernaferrari.changedetection.ui.main.components.TurboSiteRow(
                    row,
                    true,
                    false,
                    started,
                    com.bernaferrari.changedetection.ui.components.SegmentPosition.Only,
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("Changed", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Last check:", substring = true).assertIsDisplayed()
        compose.runOnIdle { snapshotTime = started + 1 }
        compose.onNodeWithText("Changed", substring = true).assertIsDisplayed()
    }

    @Test fun watchlistKeepsRunningSessionReachableWhenSearchHidesItsPage() = checkWatchlistSession(1f)

    @Test fun largeTextKeepsWatchlistSessionCountdownReadable() = checkWatchlistSession(2f)

    private fun checkWatchlistSession(fontScale: Float) {
        var active by mutableStateOf(true)
        var opened = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 850.dp)) {
                        MonitorWatchlist(
                            true, 60, null, {}, pages, "no matching page", null,
                            QuickMonitorState(isActive = active, siteIds = setOf("1"), durationMinutes = 180, remainingTimeSource = { 3_723_000L }),
                            emptySet(), emptySet(), false, {}, {}, onOpenTurbo = { opened++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Turbo is on", substring = true).assertIsDisplayed().performClick()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("1:02:03", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        capture(if (fontScale == 1f) "watchlist-active-turbo" else "watchlist-active-turbo-large")
        val clockLayout = layouts.single()
        // Paragraph width can retain its available width; check the actual rendered line bounds.
        assertTrue(clockLayout.getLineRight(0) <= clockLayout.size.width + 1f)
        assertTrue(clockLayout.getLineBottom(0) <= clockLayout.size.height + 1f)
        assertFalse(clockLayout.isLineEllipsized(0))
        compose.runOnIdle {
            assertEquals(1, opened)
            active = false
        }
        compose.onNodeWithText("Turbo is on", substring = true).assertDoesNotExist()
    }

    @Test fun activeSessionCanAlwaysBeStoppedWhilePagesLoadOrDisappear() {
        var loading by mutableStateOf(true)
        var currentPages by mutableStateOf(pages)
        var stops = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.size(400.dp, 700.dp)) {
                    TurboScreen(
                        sites = currentPages,
                        isLoading = loading,
                        quickMonitorState = QuickMonitorState(isActive = true, siteIds = setOf("1")),
                        onAddSite = {},
                        onStartTurbo = { _, _, _ -> },
                        onStopTurbo = { stops++ },
                        onOpenSite = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle {
            loading = false
            currentPages = emptyList()
        }
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, stops) }
    }

    @Test fun countdownUsesTheLiveSessionClockInsteadOfAStaleWallDeadline() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                com.bernaferrari.changedetection.ui.main.components.TurboHero(
                    state = QuickMonitorState(isActive = true, deadlineTime = 1, durationMinutes = 15, remainingTimeSource = { 60_000L }),
                    intervalSeconds = 30,
                    onIntervalChange = {},
                )
            }
        }
        compose.onNodeWithText("0:01:00").assertIsDisplayed()
        compose.onNodeWithText("0:00:00").assertDoesNotExist()
    }

    @Test fun shortSessionExpiryIsVisibleAndDoesNotClaimThreeHours() {
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.size(400.dp, 700.dp)) {
                    TurboScreen(sites = pages, quickMonitorState = QuickMonitorState(durationMinutes = 15, endReason = com.bernaferrari.changedetection.background.TurboEndReason.Expired), onAddSite = {}, onStartTurbo = { _, _, _ -> }, onStopTurbo = {}, onOpenSite = {})
                }
            }
        }
        compose.onNodeWithText("Session finished. Start again when you need faster checks.").assertIsDisplayed()
        compose.onNodeWithText("Three hours complete.", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Start Turbo").assertIsDisplayed()
        capture("turbo-short-session-expired")
    }

    @Test fun runningSummarySeparatesSavedUpdatesFromChecksAndFailures() = assertSessionHealth(1f)

    @Test fun largeTextKeepsSessionHealthAndStopReachable() = assertSessionHealth(2f)

    private fun assertSessionHealth(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 850.dp)) {
                        TurboScreen(sites = pages, quickMonitorState = QuickMonitorState(isActive = true, siteIds = setOf("1", "2", "3"), startTime = now - 4 * 60_000, deadlineTime = now + 11 * 60_000, durationMinutes = 15, successfulChecks = 24, failedChecks = 3, updatesSaved = 2), onAddSite = {}, onStartTurbo = { _, _, _ -> }, onStopTurbo = {}, onOpenSite = {})
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("How it works").assertIsDisplayed()
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed()
        compose.onNodeWithText("Session: 15 minutes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2 updates saved").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("24 successful checks").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 failed checks").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed()
        capture(if (fontScale == 1f) "turbo-session-health-dark" else "turbo-session-health-large")
    }

    @Test fun setupStartsWithOnlyEligiblePagesAndChosenCadence() {
        var started: Set<String>? = null
        var cadence = 0
        show(
            pages = pages + page("4", "Paused page", "example.com/paused", true).let { it.copy(site = it.site.copy(isSyncEnabled = false)) },
            onStart = { ids, seconds ->
                started = ids
                cadence = seconds
            },
        )
        compose.onNodeWithContentDescription("Every 30 seconds").performClick()
        // Setup must expose page choice without first scrolling past a decorative numeral.
        val secondPage = compose.onNodeWithText("Camera restock").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val startAction = compose.onNodeWithText("Start Turbo").fetchSemanticsNode().boundsInRoot
        assertTrue("Second page must remain above the pinned action", secondPage.bottom <= startAction.top)
        capture("turbo-setup-light")
        compose.onNodeWithText("Start Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(setOf("1", "2", "3"), started)
            assertEquals(30, cadence)
        }
    }

    @Test fun setupPassesChosenDurationAndPageSelection() {
        var duration = 0
        var ids = emptySet<String>()
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TurboScreen(
                    sites = pages,
                    quickMonitorState = QuickMonitorState(),
                    onAddSite = {},
                    onStartTurbo = { selected, _, minutes ->
                        ids = selected
                        duration = minutes
                    },
                    onStopTurbo = {},
                    onOpenSite = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Session: 15 minutes").performScrollTo().performClick()
        compose.onNodeWithText("Camera restock").performScrollTo().performClick()
        compose.onNodeWithText("Start Turbo").performClick()
        compose.runOnIdle {
            assertEquals(15, duration)
            assertEquals(setOf("1", "3"), ids)
        }
    }

    @Test fun runningPageOpensResultAndStopIsAlwaysVisible() {
        var opened: String? = null
        var stopped = false
        show(dark = true, running = true, onOpen = { opened = it }, onStop = { stopped = true })
        compose.onNodeWithText("Check failed · open page details").assertExists()
        capture("turbo-running-dark")
        compose.onNodeWithTag("turbo-content").performScrollToKey("1")
        compose.onNodeWithText("Concert tickets").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("1", opened) }
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(stopped) }
    }

    @Test fun runningCadenceUpdatesOnceAndKeepsSelectedPages() {
        val updates = mutableListOf<Pair<Set<String>, Int>>()
        var stopped = false
        show(running = true, onStart = { ids, seconds -> updates += ids to seconds }, onStop = { stopped = true })
        compose.onNodeWithContentDescription("Every 30 seconds").performClick()
        compose.runOnIdle { assertTrue(updates.isEmpty()) }
        compose.onNodeWithContentDescription("Every 1 minute").performClick()
        compose.onNodeWithContentDescription("Every 1 minute").assertIsSelected().performClick()
        compose.runOnIdle {
            assertEquals(listOf(setOf("1", "2", "3") to 60), updates)
            assertFalse(stopped)
        }
    }

    @Test fun keyboardCanChooseCadenceDurationAndStart() = assertKeyboardSetup(1f)

    @Test fun keyboardCanReachStartWithLargeText() = assertKeyboardSetup(1.8f)

    private fun assertKeyboardSetup(fontScale: Float) {
        var started: Triple<Set<String>, Int, Int>? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(400.dp, 768.dp)) {
                        TurboScreen(
                            sites = pages,
                            quickMonitorState = QuickMonitorState(),
                            onAddSite = {},
                            onStartTurbo = { ids, seconds, minutes -> started = Triple(ids, seconds, minutes) },
                            onStopTurbo = {},
                            onOpenSite = {},
                        )
                    }
                }
            }
        }
        // Establish entry focus once; all subsequent navigation and activation uses keys.
        compose.onNodeWithContentDescription("Every 15 seconds").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onRoot().performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithContentDescription("Every 30 seconds").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithContentDescription("Every 30 seconds").assertIsSelected()
        fun tabTo(target: SemanticsMatcher) {
            repeat(24) {
                if (compose.onAllNodes(target and isFocused()).fetchSemanticsNodes().isNotEmpty()) return
                compose.onRoot().performKeyInput { pressKey(Key.Tab) }
            }
            compose.onNode(target).assertIsFocused()
        }
        tabTo(hasContentDescription("Session: 15 minutes"))
        compose.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        compose.onNodeWithContentDescription("Session: 15 minutes").assertIsSelected()
        tabTo(hasText("Start Turbo"))
        compose.onNodeWithText("Start Turbo").assertIsDisplayed().assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(Triple(setOf("1", "2", "3"), 30, 15), started) }
    }

    @Test fun largeTypeKeepsTheActionReachableAndIntervalsSelectable() {
        show(largeType = true)
        compose.onNodeWithText("Start Turbo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Every 2 minutes").performScrollTo().performClick()
        capture("turbo-large-type")
    }

    @Test fun startingAndStoppingPreserveCadenceAndReachableActions() = assertSessionTransition(1f)

    @Test fun largeTextStartingAndStoppingPreserveCadenceAndReachableActions() = assertSessionTransition(1.8f)

    private fun assertSessionTransition(fontScale: Float) {
        compose.setContent {
            var running by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.width(400.dp).height(850.dp)) {
                        TurboScreen(
                            sites = pages,
                            quickMonitorState = QuickMonitorState(
                                isActive = running,
                                siteIds = setOf("1", "2", "3"),
                                startTime = now,
                                deadlineTime = now + 3 * 3600_000,
                                intervalSeconds = 30,
                            ),
                            onAddSite = {},
                            onStartTurbo = { _, _, _ -> running = true },
                            onStopTurbo = { running = false },
                            onOpenSite = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Every 30 seconds").assertIsSelected()
        compose.onNodeWithText("Start Turbo").performClick()
        compose.onNodeWithText("Stop Turbo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Every 30 seconds").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("turbo-content").performScrollToKey("1")
        compose.onNodeWithText("Concert tickets").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Stop Turbo").performClick()
        compose.onNodeWithText("Start Turbo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Every 30 seconds").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("turbo-content").performScrollToKey("1")
        compose.onNodeWithText("Concert tickets").performScrollTo().assertIsDisplayed()
    }

    @Test fun restoredConfigurationAppearsWhenPreferenceLoadingFinishes() {
        var state by mutableStateOf(QuickMonitorState())
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                TurboScreen(
                    sites = pages,
                    quickMonitorState = state,
                    onAddSite = {},
                    onStartTurbo = { _, _, _ -> },
                    onStopTurbo = {},
                    onOpenSite = {},
                )
            }
        }
        compose.runOnIdle {
            state = QuickMonitorState(siteIds = setOf("2"), intervalSeconds = 15, endReason = com.bernaferrari.changedetection.background.TurboEndReason.Interrupted)
        }
        compose.onNodeWithText("1 page selected").assertExists()
        compose.onNodeWithContentDescription("Every 15 seconds").assertIsSelected()
        compose.onNodeWithText("Start Turbo").assertIsDisplayed()
    }

    private fun show(
        pages: List<SiteAndLastSnap> = this.pages,
        dark: Boolean = false,
        running: Boolean = false,
        largeType: Boolean = false,
        onStart: (Set<String>, Int) -> Unit = { _, _ -> },
        onOpen: (String) -> Unit = {},
        onStop: () -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeType) 1.8f else 1f)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = false) {
                    Box(Modifier.width(400.dp).height(850.dp)) {
                        TurboScreen(
                            sites = pages,
                            quickMonitorState = QuickMonitorState(
                                isActive = running,
                                siteIds = if (running) setOf("1", "2", "3") else emptySet(),
                                startTime = now - 60_000,
                                deadlineTime = now + 2 * 3600_000 + 43 * 60_000,
                                intervalSeconds = 30,
                                successfulChecks = 24,
                            ),
                            onAddSite = {},
                            onStartTurbo = { ids, seconds, _ -> onStart(ids, seconds) },
                            onStopTurbo = onStop,
                            onOpenSite = onOpen,
                        )
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        val target = File("../build/desktop-review/$name.png")
        target.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", target)
    }

    private fun page(id: String, title: String, address: String, successful: Boolean) = SiteAndLastSnap(
        site = Site(title, "https://$address", now - 10_000, id, ColorGroup(0, 0), true, true).copy(isSuccessful = successful, isRead = true),
        snap = null,
    )
}
