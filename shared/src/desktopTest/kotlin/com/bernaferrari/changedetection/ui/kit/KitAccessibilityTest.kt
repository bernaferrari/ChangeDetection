package com.bernaferrari.changedetection.ui.kit

import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.marksFor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KitAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wholeSwitchRowExposesAndUpdatesItsCheckedState() {
        val enabled = mutableStateOf(false)
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                GroupRow("Notify me", checked = enabled.value, role = Role.Switch, onClick = { enabled.value = !enabled.value })
            }
        }
        compose.onNodeWithText("Notify me").assertIsOff().performClick().assertIsOn()
    }

    @Test fun recoveryActionsWrapAtLargeTextInsteadOfLeavingTheCard() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    Callout("Connection problem", "Try again when you are online.", modifier = Modifier.width(320.dp), actions = {
                        FilledTonalButton(onClick = {}) { Text("Retry") }
                        FilledTonalButton(onClick = {}) { Text("Open in browser") }
                    })
                }
            }
        }
        val retry = compose.onNodeWithText("Retry").fetchSemanticsNode().boundsInRoot
        val browser = compose.onNodeWithText("Open in browser").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Open in browser").assertIsDisplayed()
        assertTrue("The second action wraps below the first", browser.top > retry.bottom)
        assertTrue(browser.right <= compose.onRoot().fetchSemanticsNode().boundsInRoot.right)
    }

    @Test fun changedPreviewDescribesBothVersionsWithoutRelyingOnColor() {
        val text = editText(
            listOf(
                EditSegment("Price ", EditSegment.Kind.Same),
                EditSegment("1599", EditSegment.Kind.Removed),
                EditSegment("1349", EditSegment.Kind.Added),
            ),
            marksFor(false),
        )
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) { DiffText(text) }
        }
        compose.onNodeWithContentDescription("Price  Before: 1599.  Now: 1349. ").assertExists()
    }
}
