package com.bernaferrari.changedetection.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.library.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickMonitorUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bannerDisplaysLiveBadgeAndStopAction() {
        val liveBadge = composeTestRule.activity.getString(R.string.quick_monitor_banner_live_badge)
        val stopLabel = composeTestRule.activity.getString(R.string.action_stop_monitoring)

        composeTestRule.setContent {
            QuickMonitorBanner(
                isActive = true,
                sitesCount = 3,
                intervalSeconds = 30,
                onToggle = {},
                startTime = null,
                deadlineTime = null
            )
        }

        composeTestRule.onNodeWithText(liveBadge).assertIsDisplayed()
        composeTestRule.onNodeWithText(stopLabel).assertIsDisplayed()
    }

    @Test
    fun cardShowsStopAndForegroundStateWhenActive() {
        val stopTurbo = composeTestRule.activity.getString(R.string.turbo_mode_button_stop)
        val foregroundLabel = composeTestRule.activity.getString(R.string.quick_monitor_alert_state_on)

        composeTestRule.setContent {
            QuickMonitorCard(
                isActive = true,
                sitesCount = 2,
                totalSitesCount = 5,
                intervalSeconds = 45,
                onStart = {},
                onStop = {},
                onExtend = {}
            )
        }

        composeTestRule.onNodeWithText(stopTurbo).assertIsDisplayed()
        composeTestRule.onNodeWithText(foregroundLabel).assertIsDisplayed()
    }

    @Test
    fun bannerProgressUpdatesWithTick() {
        val contentDesc = composeTestRule.activity.getString(
            R.string.quick_monitor_banner_active_details,
            1,
            30,
            "--:--",
            "--:--"
        )

        composeTestRule.setContent {
            QuickMonitorBanner(
                isActive = true,
                sitesCount = 1,
                intervalSeconds = 30,
                onToggle = {},
                startTime = null,
                deadlineTime = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(contentDesc).assertExists()
    }
}
