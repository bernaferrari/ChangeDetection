/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.logs_days_ago
import com.bernaferrari.changedetection.shared.generated.resources.logs_hr_ago
import com.bernaferrari.changedetection.shared.generated.resources.logs_just_now
import com.bernaferrari.changedetection.shared.generated.resources.logs_min_ago
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.time.Instant

fun formatTimestamp(timestamp: Long, includeSeconds: Boolean = false): String = localizedTimestamp(timestamp, includeSeconds)

/** Formats the calendar day used to group history entries without splitting every timestamp. */
fun formatDate(timestamp: Long): String = timestamp.toLocalDateTime().let { dateTime ->
    "${dateTime.year.toString().padStart(4, '0')}-${(dateTime.month.ordinal + 1).twoDigits()}-${dateTime.day.twoDigits()}"
}

fun formatTime(timestamp: Long, includeSeconds: Boolean = false): String = localizedTime(timestamp, includeSeconds)

private fun Long.toLocalDateTime() = Instant.fromEpochMilliseconds(this)
    .toLocalDateTime(TimeZone.currentSystemDefault())

private fun Int.twoDigits(): String = toString().padStart(2, '0')

@Composable
fun getRelativeTimeLabel(timestamp: Long): String {
    // A visible “just now” label must age even when the underlying record is unchanged.
    val now by produceState(initialValue = getCurrentTimeMillis(), key1 = timestamp) {
        while (true) {
            value = getCurrentTimeMillis()
            delay(60_000)
        }
    }
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> stringResource(Res.string.logs_just_now)
        minutes < 60 -> stringResource(Res.string.logs_min_ago, minutes)
        hours < 24 -> stringResource(Res.string.logs_hr_ago, hours)
        else -> pluralStringResource(Res.plurals.logs_days_ago, days.toInt(), days)
    }
}

fun getCurrentTimeMillis(): Long = kotlin.time.Clock.System
    .now()
    .toEpochMilliseconds()
