/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.home

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeSelectionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun choosingSelectPagesStartsEmptyAndDeselectingLastPageKeepsModeOpen() {
        val entered = HomeSelection().enter()
        assertTrue(entered.active)
        assertTrue(entered.ids.isEmpty())

        val selected = entered.toggle("visible-page")
        assertEquals(setOf("visible-page"), selected.ids)
        val deselected = selected.toggle("visible-page")
        assertTrue(deselected.active)
        assertTrue(deselected.ids.isEmpty())

        val closed = deselected.clear()
        assertFalse(closed.active)
        assertTrue(closed.ids.isEmpty())
    }

    @Test fun removingTheLastSelectedPageKeepsSelectionAvailableForOtherPages() {
        val selection = HomeSelection().toggle("deleted-page").retain(setOf("other-page"))
        assertTrue(selection.active)
        assertTrue(selection.ids.isEmpty())
    }

    @Test fun emptySelectionDisablesBatchActionsAndKeepsCloseReachable() {
        var closed = false
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.width(320.dp)) {
                    SelectionBar(
                        count = 0,
                        rows = emptyList(),
                        turboAvailable = true,
                        notificationsAvailable = true,
                        onClose = { closed = true },
                        onCheck = { error("Empty selection must not check pages") },
                        onTurbo = { error("Empty selection must not start Turbo") },
                        onDelete = { error("Empty selection must not delete pages") },
                        onSetPaused = { error("Empty selection must not pause pages") },
                        onSetNotify = { error("Empty selection must not change notifications") },
                        onEdit = { error("Empty selection must not edit a page") },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("0 pages selected").assertIsDisplayed()
        listOf("Check now", "Turbo", "Delete").forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsNotEnabled()
        }
        compose.onNodeWithContentDescription("Clear selection").performClick()
        compose.runOnIdle { assertTrue(closed) }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Pause checks").assertIsNotEnabled()
        compose.onNodeWithText("Turn notifications on").assertIsNotEnabled()
    }
}
