package com.bernaferrari.changedetection.ui.turbo

import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TurboSheetUiTest {
    @get:Rule val compose = createComposeRule()

    private val pages = listOf("Exam results", "Studio Display", "Release notes", "Design roles").mapIndexed { i, name ->
        SiteAndLastSnap(Site(name, "https://example.test/$i", 0L, "", "p$i", 0 to 0).copy(isSyncEnabled = true), null)
    } + SiteAndLastSnap(Site("Paused page", "https://example.test/paused", 0L, "", "paused", 0 to 0).copy(isSyncEnabled = false), null)

    @Test fun startsOnlyWithChosenPagesAndCadence() {
        var started: Triple<Set<String>, Int, Int>? = null
        compose.setContent {
            ChangeDetectionTheme {
                TurboSetupSheet(pages, preselected = setOf("p0"), state = TurboState(), onDismiss = {}) { ids, interval, minutes -> started = Triple(ids, interval, minutes) }
            }
        }
        compose.onNodeWithText("Paused page").assertDoesNotExist()
        compose.onNodeWithText("Studio Display").performClick()
        compose.onNodeWithText("15 s").performClick()
        compose.onNodeWithText("1 hour").performScrollTo().performClick()
        compose.onNodeWithText("Start Turbo on 2 pages").performClick()
        compose.runOnIdle { assertEquals(Triple(setOf("p0", "p1"), 15, 60), started) }
    }

    @Test fun cannotStartWithoutPages() {
        compose.setContent {
            ChangeDetectionTheme {
                TurboSetupSheet(pages, preselected = emptySet(), state = TurboState(), onDismiss = {}) { _, _, _ -> error("must not start") }
            }
        }
        compose.onNodeWithText("Pick at least one page").assertIsNotEnabled()
    }

    @Test fun narrowLargeTypeChoicesKeepTheirLabelsAndSelectedState() {
        var selected by mutableStateOf("Screenshot")
        val choices = listOf("Text", "Screenshot", "Follow app setting")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme {
                    Box(Modifier.width(280.dp)) {
                        ConnectedChoice(choices, selected, { selected = it }) { it }
                    }
                }
            }
        }
        choices.forEach { label ->
            val node = compose.onNodeWithText(label, useUnmergedTree = true)
            node.assertIsDisplayed()
            val results = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout = results.single()
            assertFalse("$label is clipped: size=${layout.size}, lines=${layout.lineCount}, widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}, lastLineBottom=${layout.getLineBottom(layout.lineCount - 1)}", layout.hasVisualOverflow)
        }
        compose.onNodeWithText("Screenshot").assertIsSelected().performClick().assertIsSelected()
        compose.onNodeWithText("Text").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("Text", selected) }
    }

    @Test fun largeTypeKeepsStartVisibleWhileChoicesScroll() {
        var started = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme {
                    TurboSetupSheet(
                        pages,
                        preselected = setOf("p0"),
                        state = TurboState(),
                        onDismiss = {},
                        modifier = Modifier.width(280.dp).height(600.dp),
                    ) { _, _, _ -> started = true }
                }
            }
        }
        compose.onNodeWithText("Start Turbo on 1 page").assertIsDisplayed()
        compose.onNodeWithText("3 hours").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start Turbo on 1 page").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, started) }
    }

    @Test fun pausedPreselectionNeverStartsUnrelatedPages() {
        compose.setContent {
            ChangeDetectionTheme {
                TurboSetupSheet(pages, preselected = setOf("paused"), state = TurboState(), onDismiss = {}) { _, _, _ -> error("must not start") }
            }
        }
        compose.onNodeWithText("Paused pages are not included.").assertIsDisplayed()
        compose.onNodeWithText("Pick at least one page").assertIsNotEnabled()
    }

    @Test fun pagePausedWhileChoosingCannotBeSubmitted() {
        var source by mutableStateOf(pages)
        compose.setContent {
            ChangeDetectionTheme {
                TurboSetupSheet(source, preselected = setOf("p0"), state = TurboState(), onDismiss = {}) { _, _, _ -> error("must not start") }
            }
        }
        compose.onNodeWithText("Start Turbo on 1 page").assertIsEnabled()
        compose.runOnIdle { source = source.map { it.copy(site = it.site.copy(isSyncEnabled = false)) } }
        compose.onNodeWithText("No active pages yet. Resume a page before starting Turbo.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pick at least one page").assertIsNotEnabled()
    }

    @Test fun endedSessionUsesItsSavedCountAndExcludesLaterChanges() {
        val state = TurboState(siteIds = setOf("p0"), startTime = 1_000L, lastCheckTime = 2_000L, updatesSaved = 3)
        val later = pages.first().copy(snap = Snap("later", "p0", 3_000L, "text/plain", "UTF-8", 20), snapshotCount = 2)
        compose.setContent {
            ChangeDetectionTheme {
                TurboLiveScreen(state, listOf(later), {}, {}, {}, {}, { _, _ -> error("not a session change") })
            }
        }
        compose.onNodeWithText("3 changes saved").assertIsDisplayed()
        compose.onNodeWithText("Changed").assertDoesNotExist()
    }
}
