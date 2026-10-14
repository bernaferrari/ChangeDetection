package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.model.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsHierarchyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedVersionsOpensStorageDetailsAndCanRetryWithoutClosing() {
        var state by mutableStateOf(SettingsUiState(storageBytes = 4 * 1_048_576L))
        var retries = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Column(Modifier.width(390.dp)) {
                    SavedVersionsRow(state, {
                        retries++
                        state = state.copy(storageLoadFailed = false, storageBytes = null)
                    }, onClearOlder = {})
                }
            }
        }
        compose.onNodeWithText("Saved versions").assertHasClickAction().performClick()
        compose.onNodeWithText("Saved history").assertIsDisplayed()
        compose.onNodeWithText("4 MB of saved versions").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(storageLoadFailed = true, storageBytes = null) }
        compose.onNodeWithText("Could not load storage usage.").assertIsDisplayed()
        compose.onAllNodesWithText("Retry").onLast().performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            state = state.copy(storageBytes = 2 * 1_048_576L)
        }
        compose.onNodeWithText("2 MB of saved versions").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Saved history").assertDoesNotExist()
    }

    @Test fun storageFailureOffersRetryInsideAndOutsideItsDialog() {
        var failed by mutableStateOf(true)
        var retries = 0
        compose.setContent {
            ChangeDetectionTheme {
                Column(Modifier.width(390.dp)) {
                    DeviceSettingsSection(SettingsUiState(storageBytes = if (failed) null else 0L, storageLoadFailed = failed), {}, {}, onRetryStorage = {
                        retries++
                        failed = false
                    })
                }
            }
        }
        compose.onNodeWithText("Could not load storage usage.").assertIsDisplayed()
        val capture = File("../build/desktop-review/settings-storage-retry.png")
        capture.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", capture)
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("0 MB of saved versions").assertIsDisplayed()
        compose.runOnIdle { failed = true }
        compose.onNodeWithText("Could not load storage usage.").performClick()
        compose.onAllNodesWithText("Retry").onLast().assertIsDisplayed().performClick()
        compose.onAllNodesWithText("0 MB of saved versions").onLast().assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(2, retries) }
    }

    @Test fun pendingStorageDoesNotClaimZeroAndOpenSummaryUpdatesWhenMeasured() {
        var bytes by mutableStateOf<Long?>(null)
        compose.setContent {
            ChangeDetectionTheme {
                Column(Modifier.width(390.dp)) { DeviceSettingsSection(SettingsUiState(storageBytes = bytes), {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Loading…").assertIsDisplayed().performClick()
        compose.onNodeWithText("0 MB of saved versions").assertDoesNotExist()
        compose.onNodeWithText("Keeps up to 100 versions per page.", substring = true).assertIsDisplayed()
        compose.runOnIdle { bytes = 0L }
        compose.onAllNodesWithText("0 MB of saved versions").onLast().assertIsDisplayed()
        compose.runOnIdle { bytes = 4L * 1_048_576L }
        compose.onAllNodesWithText("4 MB of saved versions").onLast().assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("4 MB of saved versions").assertIsDisplayed()
    }

    @Test fun longSettingValuesAndExplanationsRemainReadableAtLargeType() {
        val title = "Background sync"
        val value = "Sync every 7 days while this application is open"
        val explanation = "Checks use the same schedule for every watched page. Paused pages keep their saved history."
        var opened = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.width(280.dp).height(500.dp)) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            SettingsGroup {
                                SettingsActionRow(Icons.Outlined.Schedule, title, explanation, value, onClick = { opened++ })
                            }
                        }
                    }
                }
            }
        }
        for (text in listOf(title, value, explanation)) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("Text overflow: $text", layout.hasVisualOverflow)
            for (line in 0 until layout.lineCount) {
                assertFalse(layout.isLineEllipsized(line))
                val end = layout.getLineEnd(line, visibleEnd = true)
                assertTrue("Word split in $text at $end", end == text.length || text[end].isWhitespace() || (end > 0 && text[end - 1].isWhitespace()))
            }
        }
        compose.onNodeWithText(value, useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        val file = File("../build/desktop-review/settings-large-dark.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun scheduleAndStorageUseReadableSummaries() {
        var scheduleOpened = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.width(390.dp).height(700.dp)) {
                    Column {
                        MonitoringSettingsSection(SettingsUiState(syncInterval = 10080), { scheduleOpened++ }, {}, {})
                        DeviceSettingsSection(SettingsUiState(storageBytes = 512_000), {}, {}, {})
                        AboutSettingsSection("Desktop development", {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Sync every 7 days", substring = true).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, scheduleOpened) }
        compose.onNodeWithText("Browser delay").assertDoesNotExist()
        compose.onNodeWithText("Automatic checks").assertIsDisplayed()
        compose.onNodeWithText("Checks run only while the app is open.").assertIsDisplayed()
        compose.onNodeWithText("<1 MB of saved versions", substring = true).assertIsDisplayed()
        val file = File("../build/desktop-review/settings-groups-light.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
        compose.onNodeWithText("<1 MB of saved versions", substring = true).performClick()
        compose.onNodeWithText("Keeps up to 100 versions per page.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Keeps up to 100 versions per page.", substring = true).assertDoesNotExist()
    }
}
