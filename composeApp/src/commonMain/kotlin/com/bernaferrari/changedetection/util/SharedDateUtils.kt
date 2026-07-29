/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.logs_day_ago
import com.bernaferrari.changedetection.shared.generated.resources.logs_hr_ago
import com.bernaferrari.changedetection.shared.generated.resources.logs_just_now
import com.bernaferrari.changedetection.shared.generated.resources.logs_min_ago
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    // Custom formatting since KMP doesn't support java.text.SimpleDateFormat
    // Format: MMM dd, HH:mm:ss
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val month = monthNames[localDateTime.month.ordinal]

    val day = localDateTime.day.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')

    return "$month $day, $hour:$minute"
}

/** Formats the calendar day used to group history entries without splitting every timestamp. */
fun formatDate(timestamp: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val month = monthNames[localDateTime.month.ordinal]
    val day = localDateTime.day.toString().padStart(2, '0')

    return "$month $day"
}

@Composable
fun getRelativeTimeLabel(timestamp: Long): String {
    val now =
        kotlin.time.Clock.System
            .now()
            .toEpochMilliseconds()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> stringResource(Res.string.logs_just_now)
        minutes < 60 -> stringResource(Res.string.logs_min_ago, minutes)
        hours < 24 -> stringResource(Res.string.logs_hr_ago, hours)
        else -> stringResource(Res.string.logs_day_ago, days)
    }
}

fun getCurrentTimeMillis(): Long = kotlin.time.Clock.System
    .now()
    .toEpochMilliseconds()
