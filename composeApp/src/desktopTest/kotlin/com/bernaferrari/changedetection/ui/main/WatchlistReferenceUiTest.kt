/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionNavigationSuite
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import java.io.File
import javax.imageio.ImageIO
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

@OptIn(ExperimentalMaterial3Api::class)
class WatchlistReferenceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun watchlistReferenceKeepsPageAndOptionsActions() = checkWatchlist(true)

    @Test fun lightWatchlistKeepsPageAndOptionsActions() = checkWatchlist(false)

    @Test fun searchAndFilterEmptyStateAgreeAndRecoverWhenSearchIsCleared() {
        var query by mutableStateOf("Workshop")
        val pages = listOf("Workshop", "Conference").mapIndexed { index, name ->
            val site = Site(name, "https://example.test/$index", 1L, "", "page-$index", 0 to 0).copy(isRead = index == 0, isSuccessful = true)
            SiteAndLastSnap(site, Snap("snap-$index", site.id, 1L, "text/html", "UTF-8", 10), if (index == 0) 1 else 2)
        }
        compose.setContent {
            ChangeDetectionTheme {
                Box(Modifier.size(390.dp, 700.dp)) {
                    MonitorWatchlist(true, 60, null, {}, pages, query, null, com.bernaferrari.changedetection.background.TurboState(), emptySet(), emptySet(), false, {}, {})
                }
            }
        }
        compose.onNodeWithText("All  1").assertIsDisplayed()
        compose.onNodeWithText("New  0").performClick()
        compose.onNodeWithText("No new changes").assertIsDisplayed()
        compose.onNodeWithText("No matching pages").assertDoesNotExist()
        val capture = File("../build/desktop-review/watchlist-search-filter.png")
        capture.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", capture)
        compose.runOnIdle { query = "" }
        compose.onNodeWithText("New  1").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithText("Conference", substring = true).assertIsDisplayed()
        compose.onNodeWithText("No new changes").assertDoesNotExist()
    }

    private fun checkWatchlist(dark: Boolean) {
        var opened = 0
        var options = 0
        val now = getCurrentTimeMillis()
        val pages = listOf("Conference program", "Design roles", "Exam results", "Release notes", "Fellowship guide", "Event poster").mapIndexed { i, name ->
            val site = Site(name, "https://example.test/program", now, "", "page-$i", 0 to 0)
                .copy(isRead = i != 0, lastSuccessfulCheck = now - 60_000, isSuccessful = true, isSyncEnabled = i != 5)
            SiteAndLastSnap(site, Snap("snap-$i", site.id, if (i == 0) now - 86_400_000 else now - 60_000, "text/html", "UTF-8", 10), if (i == 0) 2 else 1)
        }
        compose.setContent {
            ChangeDetectionTheme(darkTheme = dark, dynamicColor = false) {
                Box(Modifier.width(430.dp).height(872.dp)) {
                    val stack = remember { NavBackStack<NavKey>(MainDestination) }
                    ChangeDetectionNavigationSuite(stack) {
                        Scaffold(floatingActionButton = { AddPageFab({}) }, topBar = {
                            MonitorTopBar(pages.size, false, "", {}, {}, {}, {}, false, {}, remember { FocusRequester() }, {}, TopAppBarDefaults.pinnedScrollBehavior())
                        }) { padding ->
                            Box(Modifier.padding(padding)) {
                                MonitorWatchlist(
                                    true, 60, null, {}, pages, "", null,
                                    com.bernaferrari.changedetection.background.TurboState(), emptySet(), emptySet(), false,
                                    { opened++ }, { options++ },
                                )
                            }
                        }
                    }
                }
            }
        }
        val file = File("../build/desktop-review/watchlist-reference-${if (dark) "dark" else "light"}.png")
        file.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", file)
        compose.onNodeWithText("Checked 1 min ago", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Conference program", substring = true).performClick()
        compose.onNodeWithContentDescription("More options for Conference program").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, options)
        }
    }
}
