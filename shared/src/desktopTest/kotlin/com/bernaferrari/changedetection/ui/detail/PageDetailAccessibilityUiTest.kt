package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PageDetailAccessibilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchesExposeCurrentStateOnceAndRowsToggleTheExpectedOption() {
        var notify: Boolean? = null
        var paused: Boolean? = null
        val site = Site("Example page", "https://example.com", getCurrentTimeMillis(), "", "example", 0 to 0)
            .copy(isNotificationEnabled = true, isSyncEnabled = true)
        content(SiteAndLastSnap(site, null), onNotify = { notify = it }, onPause = { paused = it })

        compose.onNodeWithTag("page-details").performScrollToKey("watch")
        compose.onNodeWithText("Notify me").performScrollTo().assertIsOn().performClick()
        compose.onNodeWithText("Pause checks").performScrollTo().assertIsOff().performClick()
        compose.runOnIdle {
            assertEquals(false, notify)
            assertEquals(true, paused)
        }
        assertEquals(2, compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)).fetchSemanticsNodes().size)
    }

    @Test fun failedCheckIsNotPresentedAsASuccessAtLargeTextSize() {
        val site = Site("Example page", "https://example.com", getCurrentTimeMillis(), "", "example", 0 to 0)
            .copy(isSuccessful = false, lastFailureCode = "CONNECTION")
        content(SiteAndLastSnap(site, null), fontScale = 2f)
        compose.onNodeWithText("Last check failed", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Open in browser").performScrollTo().assertIsDisplayed()
    }

    private fun content(item: SiteAndLastSnap, fontScale: Float = 1f, onNotify: (Boolean) -> Unit = {}, onPause: (Boolean) -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Surface(Modifier.width(320.dp).height(760.dp)) {
                        PageDetailContent(
                            item = item,
                            isLoading = false,
                            versions = emptyList(),
                            schedule = MonitoringSchedule(),
                            turbo = TurboState(),
                            onBack = {},
                            onCompare = {},
                            onEdit = {},
                            onTurbo = {},
                            onSchedule = {},
                            onOpenUrl = {},
                            onCheck = {},
                            onMarkRead = {},
                            onSetPaused = onPause,
                            onSetNotify = onNotify,
                            notificationsAvailable = true,
                            onDelete = {},
                        )
                    }
                }
            }
        }
    }
}
