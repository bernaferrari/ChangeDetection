/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.BackHandler
import com.bernaferrari.changedetection.util.DesktopBackDispatcher
import com.bernaferrari.changedetection.util.LocalDesktopBackDispatcher
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class WatchlistSearchKeyboardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun escapeClearsSearchAndReturnsFocusToItsLauncher() {
        compose.setContent {
            val back = remember { DesktopBackDispatcher() }
            CompositionLocalProvider(LocalDesktopBackDispatcher provides back) {
                var active by remember { mutableStateOf(false) }
                var query by remember { mutableStateOf("") }
                val inputFocus = remember { FocusRequester() }
                val close = {
                    active = false
                    query = ""
                }
                BackHandler(active, close)
                LaunchedEffect(active) { if (active) inputFocus.requestFocus() }
                ChangeDetectionTheme {
                    Box(Modifier.width(430.dp).onPreviewKeyEvent { it.key == Key.Escape && it.type == KeyEventType.KeyDown && back.dispatch() }) {
                        MonitorTopBar(3, active, query, { query = it }, { active = true }, close, { query = "" }, false, {}, inputFocus, {}, TopAppBarDefaults.pinnedScrollBehavior())
                    }
                }
            }
        }
        val launcher = compose.onNodeWithContentDescription("Search pages")
        launcher.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        launcher.performKeyInput { pressKey(Key.Enter) }
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("Workshop")
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        launcher.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.onNode(hasSetTextAction()).assertIsFocused().assertTextEquals("", "Search pages")
        compose.onNodeWithContentDescription("Close search").performClick()
        launcher.assertIsFocused()
    }
}
