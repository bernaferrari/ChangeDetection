package com.bernaferrari.changedetection.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.pop
import org.junit.Assert.assertEquals
import org.junit.Test

class BackStackPopTest {
    @Test fun repeatedCloseTapsNeverEmptyTheStack() {
        val stack = NavBackStack<NavKey>(MainDestination, TurboDestination)
        // "Done" tapped three times while the Turbo screen animates out.
        repeat(3) { stack.pop(TurboDestination) }
        assertEquals(listOf<NavKey>(MainDestination), stack.toList())
        stack.pop()
        assertEquals(listOf<NavKey>(MainDestination), stack.toList())
    }

    @Test fun openingTheOwningPageReturnsToItsExistingOverview() {
        val stack = NavBackStack<NavKey>(MainDestination, PageDetailDestination("a"), ComparisonDestination("a"))
        stack.openPage("a")
        assertEquals(listOf<NavKey>(MainDestination, PageDetailDestination("a")), stack.toList())
        stack.pop()
        assertEquals(listOf<NavKey>(MainDestination), stack.toList())
    }

    @Test fun aChangeOpenedFromHomeCanOpenItsPageWithoutKeepingASiblingReader() {
        val stack = NavBackStack<NavKey>(MainDestination, ComparisonDestination("a"))
        stack.openPage("a")
        assertEquals(listOf<NavKey>(MainDestination, PageDetailDestination("a")), stack.toList())
    }

    @Test fun aScreenOnlyClosesItself() {
        val stack = NavBackStack<NavKey>(MainDestination, PageDetailDestination("a"), ComparisonDestination("a"))
        stack.pop(PageDetailDestination("a"))
        assertEquals(3, stack.size)
    }
}
