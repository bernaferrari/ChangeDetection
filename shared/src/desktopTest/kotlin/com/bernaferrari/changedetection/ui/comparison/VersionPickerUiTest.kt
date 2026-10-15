package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VersionPickerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun theOldestVersionCanBeSelectedFromMultipleVersions() {
        val snaps = listOf(
            Snap("new", "page", 1_700_000_060_000L, "text/plain", "UTF-8", 10),
            Snap("baseline", "page", 1_700_000_000_000L, "text/plain", "UTF-8", 10),
        )
        var selected by mutableIntStateOf(-1)
        compose.setContent {
            ChangeDetectionTheme {
                VersionSheet(snaps, selectedIndex = if (selected < 0) 0 else -1, selectedSingleSnapId = snaps.getOrNull(selected)?.snapId, onDismiss = {}, onSelect = { selected = it })
            }
        }
        compose.onNodeWithTag("version-list").performScrollToNode(hasText("First version"))
        compose.onNode(hasText("First version") and hasClickAction()).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, selected) }
        compose.onNodeWithText("Before").assertDoesNotExist()
        compose.onNodeWithText("Selected").assertExists()
    }
}
