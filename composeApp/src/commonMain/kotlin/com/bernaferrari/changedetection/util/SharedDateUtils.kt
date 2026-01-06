package com.bernaferrari.changedetection.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

expect fun formatTimestamp(timestamp: Long): String

fun getRelativeTimeLabel(timestamp: Long): String {
    val now = kotlin.time.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    val then = kotlin.time.Instant.fromEpochMilliseconds(timestamp)
    val period = then.periodUntil(now, TimeZone.currentSystemDefault())

    return when {
        period.days >= 7 -> then.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            // Simplified for now, could use a platform-specific formatter for better results
            "${it.month.name.take(3)} ${it.day}"
        }

        period.days >= 1 -> "${period.days}d ago"
        period.hours >= 1 -> "${period.hours}h ago"
        period.minutes >= 1 -> "${period.minutes}m ago"
        else -> "Just now"
    }
}
