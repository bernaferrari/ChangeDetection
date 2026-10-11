/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.kit

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.cadence_day
import com.bernaferrari.changedetection.shared.generated.resources.cadence_days
import com.bernaferrari.changedetection.shared.generated.resources.cadence_hour
import com.bernaferrari.changedetection.shared.generated.resources.cadence_hours
import com.bernaferrari.changedetection.shared.generated.resources.cadence_minutes
import com.bernaferrari.changedetection.shared.generated.resources.cadence_week
import com.bernaferrari.changedetection.shared.generated.resources.day_time
import com.bernaferrari.changedetection.shared.generated.resources.history_yesterday
import com.bernaferrari.changedetection.shared.generated.resources.today
import com.bernaferrari.changedetection.shared.generated.resources.weekday_names
import com.bernaferrari.changedetection.util.formatTime
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringArrayResource
import com.bernaferrari.changedetection.localization.stringResource

fun Long.localDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date

/** "Today", "Yesterday", "Monday" within a week, otherwise the full native calendar date. */
@Composable
fun friendlyDay(timestamp: Long): String {
    val date = timestamp.localDate()
    val days = date.daysUntil(getCurrentTimeMillis().localDate())
    val weekdays = stringArrayResource(Res.array.weekday_names)
    return when {
        days <= 0 -> stringResource(Res.string.today)
        days == 1 -> stringResource(Res.string.history_yesterday)
        days < 7 && weekdays.size == 7 -> weekdays[date.dayOfWeek.ordinal]
        else -> com.bernaferrari.changedetection.util.localizedCalendarDate(timestamp)
    }
}

/** "Today, 09:12". */
@Composable
fun friendlyDateTime(timestamp: Long): String = stringResource(Res.string.day_time, friendlyDay(timestamp), formatTime(timestamp))

/** "every 15 min", "every hour", "every 6 hours", "every day", "every 3 days" — lower case, for sentences. */
@Composable
fun cadence(minutes: Long): String = when {
    minutes == 60L -> stringResource(Res.string.cadence_hour)
    minutes == 1440L -> stringResource(Res.string.cadence_day)
    minutes == 10080L -> stringResource(Res.string.cadence_week)
    minutes % 1440L == 0L -> stringResource(Res.string.cadence_days, (minutes / 1440L).toInt())
    minutes % 60L == 0L -> stringResource(Res.string.cadence_hours, (minutes / 60L).toInt())
    else -> stringResource(Res.string.cadence_minutes, minutes.toInt())
}
