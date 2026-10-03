/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TurboUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = System.currentTimeMillis()
    private val pages = listOf(
        page("1", "Concert tickets", "tickets.example.com/summer", successful = true),
        page("2", "Camera restock", "shop.example.com/camera", successful = false),
        page("3", "Application results", "university.example.com/results", successful = true),
    )

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

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/$name.png"),
        ).use { it.readBytes() }
    }

    private fun page(id: String, title: String, address: String, successful: Boolean) = SiteAndLastSnap(
        site = Site(title, "https://$address", now - 10_000, id, ColorGroup(0, 0), true, true).copy(isSuccessful = successful, isRead = true),
        snap = null,
    )
}
