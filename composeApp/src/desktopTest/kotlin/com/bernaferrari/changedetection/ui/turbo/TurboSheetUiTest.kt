/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.turbo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
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
        compose.onNodeWithText("Start Turbo on 2 pages").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Triple(setOf("p0", "p1"), 15, 60), started) }
    }

    @Test fun cannotStartWithoutPages() {
        compose.setContent {
            ChangeDetectionTheme {
                TurboSetupSheet(pages, preselected = emptySet(), state = TurboState(), onDismiss = {}) { _, _, _ -> error("must not start") }
            }
        }
        compose.onNodeWithText("Pick at least one page").performScrollTo().assertIsNotEnabled()
    }
}
