package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SettingsLargeTextUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowRowPreservesTitleValueAndActionAtDoubleTextSize() {
        var clicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.width(280.dp)) {
                        SettingsActionRow(Icons.Outlined.Sync, "Background sync", value = "Sync every 7 days", onClick = { clicks++ })
                    }
                }
            }
        }
        listOf("Background sync", "Sync every 7 days").forEach { label ->
            val node = compose.onNodeWithText(label, useUnmergedTree = true)
            node.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            layouts.forEach { layout ->
                assertFalse("$label: size=${layout.size}, lines=${layout.lineCount}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
                repeat(layout.lineCount) { assertFalse(layout.isLineEllipsized(it)) }
            }
        }
        compose.onNodeWithText("Background sync", substring = true).performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
