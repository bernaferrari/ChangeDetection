package com.bernaferrari.changedetection.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toLocalDateTime

expect fun formatTimestamp(timestamp: Long): String

fun getRelativeTimeLabel(timestamp: Long): String {
    val now = Clock.System.now()
    val then = Instant.fromEpochMilliseconds(timestamp)
    val period = then.periodUntil(now, TimeZone.currentSystemDefault())

    return when {
        period.days >= 7 -> then.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            // Simplified for now, could use a platform-specific formatter for better results
            "${it.month.name.take(3)} ${it.dayOfMonth}"
        }

        period.days >= 1 -> "${period.days}d ago"
        period.hours >= 1 -> "${period.hours}h ago"
        period.minutes >= 1 -> "${period.minutes}m ago"
        else -> "Just now"
    }
}
