package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.util.*
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.datetime.*
import kotlin.time.Instant

internal data class ComparisonRange(val label: String, val before: String, val after: String, val from: String = "", val to: String = "")

/** Calendar comparisons use the chosen zone, not elapsed 24-hour buckets (which break across DST). */
internal fun comparisonRange(
    older: Long,
    newer: Long,
    now: Long,
    zone: TimeZone,
    olderDay: String,
    newerDay: String,
    time: (Long) -> String = ::formatTime,
    full: (Long) -> String = { formatTimestamp(it) },
    /** Joins a day and a time the way the rest of the app writes them ("Today, 7:00 PM"). */
    dayTime: (day: String, time: String) -> String = { day, time -> "$day $time" },
): ComparisonRange {
    fun date(value: Long) = Instant.fromEpochMilliseconds(value).toLocalDateTime(zone).date
    val sameDay = date(older) == date(newer)
    val today = date(newer) == date(now)
    val beforeOffset = zone.offsetAt(Instant.fromEpochMilliseconds(older))
    val afterOffset = zone.offsetAt(Instant.fromEpochMilliseconds(newer))
    val offsets = beforeOffset != afterOffset
    val beforeTime = time(older) + if (offsets) " $beforeOffset" else ""
    val afterTime = time(newer) + if (offsets) " $afterOffset" else ""
    val before = if (sameDay && today) beforeTime else dayTime(olderDay, beforeTime)
    val after = if (sameDay) afterTime else dayTime(newerDay, afterTime)
    return ComparisonRange("\u2068$before\u2069 → \u2068$after\u2069", full(older) + " $beforeOffset", full(newer) + " $afterOffset", from = before, to = after)
}

@Composable
internal fun comparisonRange(older: Long, newer: Long): ComparisonRange {
    val now = getCurrentTimeMillis()
    val zone = TimeZone.currentSystemDefault()
    // Same wording as version lists and the page screen: "Oct 14, 2026, 12:41 AM → Today, 7:00 PM".
    val pattern = stringResource(Res.string.day_time, DAY_SLOT, TIME_SLOT)
    return comparisonRange(older, newer, now, zone, com.bernaferrari.changedetection.ui.kit.friendlyDay(older), com.bernaferrari.changedetection.ui.kit.friendlyDay(newer), dayTime = { d, t -> pattern.replace(DAY_SLOT, d).replace(TIME_SLOT, t) })
}

private const val DAY_SLOT = "\u0001"
private const val TIME_SLOT = "\u0002"
