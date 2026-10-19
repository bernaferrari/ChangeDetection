@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bernaferrari.changedetection.sync

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.ui.comparison.VersionNavigator
import com.bernaferrari.changedetection.ui.settings.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RefinementUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun rangeRetainsBothDatesAt320dpAndLargeFont() {
        var large by mutableStateOf(false)
        val old = kotlin.time.Instant.parse("2025-12-30T12:00:00Z").toEpochMilliseconds()
        val new = kotlin.time.Instant.parse("2026-01-02T12:00:00Z").toEpochMilliseconds()
        val snaps = listOf(Snap("new", "page", new, "text/html", "UTF-8", 1), Snap("old", "page", old, "text/html", "UTF-8", 1))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
                ChangeDetectionTheme(false, false) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.width(320.dp).padding(12.dp)) {
                            Text("Synthetic comparison", style = MaterialTheme.typography.titleLarge)
                            VersionNavigator(snaps, 0, {})
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Before:", substring = true).assertExists()
        val description = compose.onNodeWithContentDescription("Before:", substring = true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
        assertTrue(description.contains("2025"))
        assertTrue(description.contains("2026"))
        shot("comparison-range-320.png")
        compose.runOnUiThread { large = true }
        compose.onNodeWithContentDescription("Before:", substring = true).assertIsDisplayed()
        shot("comparison-range-large.png")
    }

    @Test fun ordinaryAndRecentWeekdayRangesFitTheNarrowHeader() {
        val now = System.currentTimeMillis()
        var older by mutableStateOf(now - 60 * 60 * 1000)
        var newer by mutableStateOf(now)
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.width(320.dp).padding(12.dp)) {
                        VersionNavigator(listOf(Snap("new", "page", newer, "text/html", "UTF-8", 1), Snap("old", "page", older, "text/html", "UTF-8", 1)), 0, {})
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Before:", substring = true).assertIsDisplayed()
        shot("comparison-range-today.png")
        compose.runOnUiThread {
            older = now - 4 * 24 * 60 * 60 * 1000L
            newer = now - 3 * 24 * 60 * 60 * 1000L
        }
        compose.onNodeWithContentDescription("Before:", substring = true).assertIsDisplayed()
        shot("comparison-range-weekdays.png")
    }

    @Test fun healthRefreshesOnResumeWithoutChangingPermissions() {
        var state = NotificationHealth.Enabled
        var latest: NotificationHealth? = null
        compose.setContent {
            latest = rememberNotificationHealth { state }.value
            Text(latest?.name ?: "Loading")
        }
        compose.waitUntil(5_000) { latest == NotificationHealth.Enabled }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        state = NotificationHealth.ChannelDisabled
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { latest == NotificationHealth.ChannelDisabled }
        compose.onNodeWithText("ChannelDisabled").assertIsDisplayed()
    }

    @Test fun imagePreferenceIsAnExplicitOffByDefaultChoiceInRealSettings() {
        AlertFixture().use { f ->
            val scope = ApplicationScope()
            val monitoring = object : MonitoringManager {
                override val quickMonitorState = MutableStateFlow(TurboState())
                override fun getSyncStatusFlow() = flowOf(emptyList<SyncJobInfo>())
                override fun reloadSite(site: com.bernaferrari.changedetection.repo.Site) = Unit
                override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
                override fun stopQuickMonitor() = Unit
                override suspend fun updateSchedule() = Unit
                override fun cancelWork() = Unit
            }
            val model = SettingsViewModel(f.dataStore, monitoring, f.snaps, scope)
            try {
                compose.setContent { ChangeDetectionTheme(false, false) { SettingsScreen(model, onNotificationSettings = {}, supportsNotificationImages = true) } }
                compose.onNodeWithText("Images in change alerts").performScrollTo()
                assertFalse(f.prefs.value[NotificationPreferences.IMAGE_PREVIEWS] ?: false)
                shot("notification-settings-off.png")
                compose.onNodeWithText("Images in change alerts").performClick()
                compose.waitUntil(5_000) { f.prefs.value[NotificationPreferences.IMAGE_PREVIEWS] == true }
                shot("notification-settings-on.png")
            } finally {
                scope.scope.cancel()
                model.viewModelScope.cancel()
            }
        }
    }
}
