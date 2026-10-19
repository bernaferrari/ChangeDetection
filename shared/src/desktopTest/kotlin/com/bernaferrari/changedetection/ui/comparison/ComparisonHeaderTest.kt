package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test

class ComparisonHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun multipleModesLeaveRoomForTheSavedComparison() {
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                Column(Modifier.size(320.dp, 600.dp)) {
                    ComparisonHeader(
                        listOf(Snap("new", "page", 2000, "image/png", "", 1), Snap("old", "page", 1000, "image/png", "", 1)),
                        0,
                        listOf(ComparisonMode.ImageChanges, ComparisonMode.ImageDiff, ComparisonMode.ImageSwipe),
                        ComparisonMode.ImageChanges,
                        {},
                        {},
                    )
                    Text("Saved comparison", Modifier.weight(1f).fillMaxWidth().testTag("saved-reader"))
                }
            }
        }
        compose.onNodeWithContentDescription("Changes").assertHeightIsEqualTo(52.dp)
        compose.onNodeWithTag("saved-reader").assertHeightIsAtLeast(400.dp)
    }
}
