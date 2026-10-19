package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.util.*
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.datetime.*
import kotlin.time.Instant

internal data class ComparisonRange(val label: String, val before: String, val after: String)

/** Calendar comparisons use the chosen zone, not elapsed 24-hour buckets (which break across DST). */
internal fun comparisonRange(older: Long, newer: Long, now: Long, zone: TimeZone, olderDay: String, newerDay: String, time: (Long) -> String = ::formatTime, full: (Long) -> String = { formatTimestamp(it) }): ComparisonRange {
    fun date(value: Long) = Instant.fromEpochMilliseconds(value).toLocalDateTime(zone).date
    val sameDay = date(older) == date(newer)
    val today = date(newer) == date(now)
    val beforeOffset = zone.offsetAt(Instant.fromEpochMilliseconds(older))
    val afterOffset = zone.offsetAt(Instant.fromEpochMilliseconds(newer))
    val offsets = beforeOffset != afterOffset
    val beforeTime = time(older) + if (offsets) " $beforeOffset" else ""
    val afterTime = time(newer) + if (offsets) " $afterOffset" else ""
    val before = if (sameDay && today) beforeTime else "$olderDay $beforeTime"
    val after = if (sameDay) afterTime else "$newerDay $afterTime"
    return ComparisonRange("\u2068$before\u2069 → \u2068$after\u2069", full(older) + " $beforeOffset", full(newer) + " $afterOffset")
}

@Composable
internal fun comparisonRange(older: Long, newer: Long): ComparisonRange {
    val now = getCurrentTimeMillis()
    val zone = TimeZone.currentSystemDefault()
    val currentDate = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date

    @Composable fun day(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone).date
        return when (date.daysUntil(currentDate)) {
            0 -> stringResource(Res.string.today)
            1 -> stringResource(Res.string.history_yesterday)
            in 2..6 -> localizedShortWeekday(timestamp)
            else -> localizedCalendarDate(timestamp)
        }
    }
    return comparisonRange(older, newer, now, zone, day(older), day(newer))
}
