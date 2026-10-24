package com.bernaferrari.changedetection.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHighlightTest {
    @Test fun findsEveryCaseInsensitiveMatch() {
        assertEquals(listOf(0..5, 11..16), matchRanges("Kotlin and kotlin", "KOTLIN"))
    }

    @Test fun trimsTheQueryAndIgnoresBlankOnes() {
        assertEquals(listOf(4..8), matchRanges("New Price list", " price "))
        assertTrue(matchRanges("Anything", "   ").isEmpty())
        assertFalse(matchesQuery("Anything", ""))
    }

    @Test fun overlappingRepeatsAreMarkedOnceEach() {
        assertEquals(listOf(0..1, 2..3), matchRanges("aaaa", "aa"))
    }
}
