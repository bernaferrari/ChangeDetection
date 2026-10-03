/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.detail.SavedPagePreview
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test

class SetupReceiptUiTest {
    @get:Rule val compose = createComposeRule()
    private val site = Site("Conference program", "https://example.test/program", 0L, "", "page", 0 to 0)
    private var opened = 0
    private var done = 0
    private fun show(page: SiteAndLastSnap?, removed: Boolean = false, scale: Float = 1f, dark: Boolean = scale > 1f, unavailable: Boolean = false, loading: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = false) {
                    Box(Modifier.width(360.dp).height(780.dp)) {
                        SetupReceiptContent(site, page, unavailable, removed, { opened++ }, { done++ }, SavedPagePreview.Text("Conference program\nWelcome to the annual conference.\nOpening keynote · 10:30\nMain auditorium\nWorkshops · 14:00"), isLoading = loading)
                    }
                }
            }
        }
    }
    private fun capture(name: String) {
        val file = File("../build/desktop-review/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
    }

    @Test fun initialReadDoesNotInventWaitingOrSavedBaseline() {
        show(null, loading = true)
        compose.onNodeWithText("Loading…").assertIsDisplayed()
        compose.onNodeWithText("Waiting for first check").assertDoesNotExist()
        compose.onNodeWithText("First version saved").assertDoesNotExist()
        compose.onNodeWithText("Page added").assertIsDisplayed()
        compose.onNodeWithText("Go to watchlist").performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test fun failedReadKeepsConfirmedBaselineAndOpenAction() {
        show(SiteAndLastSnap(site, Snap("saved", "page", 1_700_000_000_000L, "text/html", "UTF-8", 10), 1), unavailable = true)
        compose.onNodeWithText("We couldn’t load the latest check status. Your page is saved.").assertIsDisplayed()
        compose.onNodeWithText("First version saved").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Open page").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun initialReadFailureKeepsAnExitToTheSavedPage() {
        show(null, unavailable = true)
        compose.onNodeWithText("Waiting for first check").assertDoesNotExist()
        compose.onNodeWithText("First version saved").assertDoesNotExist()
        compose.onNodeWithText("Open page").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun savedPageIsTheFocus() {
        show(SiteAndLastSnap(site, Snap("saved", "page", 1_700_000_000_000L, "text/html", "UTF-8", 10), 1))
        compose.onNodeWithText("Saved document").assertIsDisplayed()
        capture("setup-receipt-reference-light")
    }

    @Test fun savedPageDarkReference() {
        show(SiteAndLastSnap(site, Snap("saved", "page", 1_700_000_000_000L, "text/html", "UTF-8", 10), 1), dark = true)
        compose.onNodeWithText("Saved document").assertIsDisplayed()
        capture("setup-receipt-reference-dark")
    }

    @Test fun pendingCheckDoesNotInventSuccessfulBaseline() {
        show(null)
        compose.onNodeWithText("Waiting for first check").assertIsDisplayed()
        compose.onNodeWithText("First version saved").assertDoesNotExist()
        compose.onNodeWithText("Open page").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        capture("setup-receipt-waiting")
    }

    @Test fun savedBaselineKeepsLargeTypeActionsReachable() {
        show(SiteAndLastSnap(site, Snap("saved", "page", 1_700_000_000_000L, "text/html", "UTF-8", 10), 1), scale = 2f)
        val title = compose.onAllNodesWithText("Conference program", useUnmergedTree = true).onLast()
        title.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().didOverflowHeight)
        repeat(layouts.single().lineCount) { assertFalse(layouts.single().isLineEllipsized(it)) }
        capture("setup-receipt-preview-large-dark")
        compose.onNodeWithText("First version saved").performScrollTo().assertIsDisplayed()
        capture("setup-receipt-large-dark")
        compose.onNodeWithText("Open page").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Go to watchlist").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, done)
        }
    }

    @Test fun readFailureDoesNotRestoreDestinationAfterConfirmedDeletion() {
        show(null, removed = true, unavailable = true)
        compose.onNodeWithText("This page is no longer in your watchlist.").assertIsDisplayed()
        compose.onNodeWithText("Open page").assertDoesNotExist()
        compose.onNodeWithText("Go to watchlist").performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test fun deletedPageOffersExitWithoutStaleDestination() {
        show(null, removed = true)
        compose.onNodeWithText("This page is no longer in your watchlist.").assertIsDisplayed()
        compose.onNodeWithText("Open page").assertDoesNotExist()
        compose.onNodeWithText("Go to watchlist").performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }
}
