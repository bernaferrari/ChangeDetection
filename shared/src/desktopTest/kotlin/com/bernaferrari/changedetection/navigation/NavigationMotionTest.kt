package com.bernaferrari.changedetection.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationMotionTest {
    private var durationScale = 1f

    @get:Rule val compose = createComposeRule(
        effectContext = object : MotionDurationScale {
            override val scaleFactor: Float get() = durationScale
        },
    )

    @Test fun leftToRightPushAndPopUseRightEdge() = verify(LayoutDirection.Ltr)

    @Test fun rightToLeftPushAndPopUseLeftEdge() = verify(LayoutDirection.Rtl)

    @Test fun changedReadingDirectionUpdatesRememberedMotion() = verify(LayoutDirection.Rtl, changeDirection = true)

    @Test fun zeroMotionStillCompletesNavigation() = verify(LayoutDirection.Rtl, reducedMotion = true)

    private fun verify(direction: LayoutDirection, changeDirection: Boolean = false, reducedMotion: Boolean = false) {
        durationScale = if (reducedMotion) 0f else 1f
        val stack = mutableStateListOf("home")
        var readingDirection by mutableStateOf(if (changeDirection) LayoutDirection.Ltr else direction)
        var detailX = Float.NaN
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    val metadata = rememberForwardTransitionMetadata()
                    NavDisplay(
                        backStack = stack,
                        onBack = { stack.removeLastOrNull() },
                        modifier = Modifier.size(320.dp, 480.dp),
                        entryProvider = entryProvider {
                            entry("home") { Box(Modifier.fillMaxSize()) { Text("Home") } }
                            entry("detail", metadata = metadata) {
                                Box(Modifier.fillMaxSize().onGloballyPositioned { detailX = it.positionInRoot().x }) { Text("Detail") }
                            }
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
        if (changeDirection) compose.runOnIdle { readingDirection = direction }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { stack.add("detail") }
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()
        if (reducedMotion) {
            assertEquals("Zero motion must settle within the first frames", 0f, detailX, 1f)
        } else {
            assertTrue("Entry offset $detailX for $direction", if (direction == LayoutDirection.Rtl) detailX < -1f else detailX > 1f)
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        assertEquals(0f, detailX, 1f)
        compose.onNodeWithText("Detail").assertIsDisplayed()
        compose.runOnIdle { stack.removeLastOrNull() }
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()
        if (reducedMotion) {
            compose.onNodeWithText("Detail").assertDoesNotExist()
        } else {
            assertTrue("Exit offset $detailX for $direction", if (direction == LayoutDirection.Rtl) detailX < -1f else detailX > 1f)
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.waitForIdle()
        compose.onNodeWithText("Detail").assertDoesNotExist()
        compose.onNodeWithText("Home").assertIsDisplayed()
    }
}
