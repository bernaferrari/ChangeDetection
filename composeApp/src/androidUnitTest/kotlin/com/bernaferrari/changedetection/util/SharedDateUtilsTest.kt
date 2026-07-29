/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
