/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import org.junit.Assert.*
import org.junit.Test

class SharedDateUtilsTest {
    @Test
    fun formatDateKeepsSnapshotsFromTheSameMinuteRangeTogether() {
        assertEquals(formatDate(0), formatDate(59_000))
    }

    @Test
    fun formatDateChangesAtTheNextCalendarDay() {
        assertNotEquals(formatDate(0), formatDate(86_400_000))
    }

    @Test fun `history timestamps distinguish captures within the same minute`() {
        val first = 1_700_000_000_000L
        val second = first + 15_000L
        assertEquals(formatTime(first), formatTime(second))
        assertNotEquals(formatTime(first, includeSeconds = true), formatTime(second, includeSeconds = true))
        assertEquals(formatTimestamp(first), formatTimestamp(second))
        assertNotEquals(formatTimestamp(first, includeSeconds = true), formatTimestamp(second, includeSeconds = true))
        assertTrue(formatTimestamp(first, includeSeconds = true).contains(":20"))
        assertTrue(formatTimestamp(second, includeSeconds = true).contains(":35"))
    }
}
