package com.bernaferrari.changedetection.ui.comparison

import kotlinx.datetime.*
import kotlin.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ComparisonRangeTest {
    private fun ms(value: String) = Instant.parse(value).toEpochMilliseconds()
    private fun range(old: String, new: String, now: String = new, zone: String = "UTC", oldDay: String = "Fri", newDay: String = "Today") = comparisonRange(
        ms(old),
        ms(new),
        ms(now),
        TimeZone.of(zone),
        oldDay,
        newDay,
        time = { value -> Instant.fromEpochMilliseconds(value).toLocalDateTime(TimeZone.of(zone)).time.toString() },
        full = { Instant.fromEpochMilliseconds(it).toString() },
    )

    @Test fun sameDayTodayDoesNotRepeatDayButKeepsOldNewOrder() {
        val r = range("2026-10-19T16:00:00Z", "2026-10-19T16:30:00Z")
        assertTrue(r.label.indexOf("16:00") < r.label.indexOf("16:30"))
        assertFalse(r.label.contains("Today"))
        assertTrue(r.before.contains("16:00"))
        assertTrue(r.after.contains("16:30"))
    }

    @Test fun differentDaysLabelBothEndpoints() {
        val r = range("2026-10-16T16:00:00Z", "2026-10-19T16:30:00Z")
        assertTrue(r.label.contains("Fri"))
        assertTrue(r.label.contains("Today"))
    }

    @Test fun sameHistoricalDayShowsItsDateOnlyOnce() {
        val r = range("2025-10-16T16:00:00Z", "2025-10-16T16:30:00Z", "2026-10-19T16:30:00Z", oldDay = "16 Oct 2025")
        assertEquals(1, Regex("2025").findAll(r.label).count())
    }

    @Test fun crossYearKeepsBothYears() {
        val r = range("2025-12-31T23:00:00Z", "2026-01-01T01:00:00Z", oldDay = "31 Dec 2025", newDay = "1 Jan 2026")
        assertTrue(r.label.contains("2025"))
        assertTrue(r.label.contains("2026"))
    }

    @Test fun fallBackHourShowsBothOffsetsInsteadOfAnApparentlyReversedTime() {
        val r = range("2026-11-01T05:45:00Z", "2026-11-01T06:15:00Z", zone = "America/New_York")
        assertTrue(r.label.contains("-04:00"))
        assertTrue(r.label.contains("-05:00"))
        assertTrue(r.before.contains("-04:00"))
        assertTrue(r.after.contains("-05:00"))
    }

    @Test fun midnightIsDecidedInTheDisplayZone() {
        val r = range("2026-10-18T23:30:00Z", "2026-10-19T00:30:00Z", zone = "America/Sao_Paulo")
        assertFalse(r.label.contains("Fri"))
        assertFalse(r.label.contains("Today"))
    }

    @Test fun suppliedLocaleAndTwelveHourFormattingArePreserved() {
        val r = comparisonRange(1, 2, 2, TimeZone.UTC, "Yesterday", "Today", time = { if (it == 1L) "4:00 PM" else "4:30 PM" }, full = { "Full $it" })
        assertTrue(r.label.contains("4:00 PM"))
        assertTrue(r.label.contains("4:30 PM"))
        assertTrue(r.before.startsWith("Full 1"))
    }
}
