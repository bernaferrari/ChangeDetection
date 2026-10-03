/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
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

        composeTestRule.onNodeWithText(title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(changed).assertIsDisplayed()
    }

    @Test
    fun failedCheckStillExposesSavedChange() {
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SiteRow(
                    site = syntheticSite("Previously changed page").copy(isSuccessful = false),
                    status = SiteStatus.Error,
                    isQuickMonitored = false,
                    hasUnreadChange = true,
                    onClick = {},
                    onLongClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Saved change needs review").assertIsDisplayed()
    }

    @Test
    fun eligibleScheduleDoesNotPromiseAnExactCheck() {
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                ScheduleHealth(
                    intervalMinutes = 15,
                    job = com.bernaferrari.changedetection.background.SyncJobInfo(
                        state = com.bernaferrari.changedetection.background.SyncState.ENQUEUED,
                        tags = emptySet(),
                        isPeriodic = true,
                        nextEligibleTime = 1L,
                    ),
                )
            }
        }
        composeTestRule.onNodeWithText("Sync every 15 minutes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eligible now · waiting for the device and your conditions").assertIsDisplayed()
    }

    @Test
    fun unobservedPageDoesNotClaimItWasChecked() {
        composeTestRule.setContent {
            val item = com.bernaferrari.changedetection.repo.SiteAndLastSnap(site = syntheticSite("New monitor"), snap = null)
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                SiteRow(
                    site = item.site,
                    status = item.toSiteStatus(isLoading = false, hasError = false),
                    isQuickMonitored = false,
                    onClick = {},
                    onLongClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Waiting for first check").assertIsDisplayed()
    }

    @Test
    fun failedSummaryRoutesToErrorsAndKeepsSavedChangesActionableWhileChecking() {
        var errors = 0
        var changes = 0
        composeTestRule.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                WatchlistSummary(
                    scheduledEnabled = true,
                    counts = MonitorCounts(all = 3, changed = 1, errors = 2, paused = 0, waiting = 0),
                    isRefreshing = true,
                    onReviewChanges = { changes++ },
                    onReviewErrors = { errors++ },
                )
            }
        }
        composeTestRule.onNodeWithText("Review failed pages").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Review changes").assertIsDisplayed().performClick()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(1, errors)
            org.junit.Assert.assertEquals(1, changes)
        }
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
