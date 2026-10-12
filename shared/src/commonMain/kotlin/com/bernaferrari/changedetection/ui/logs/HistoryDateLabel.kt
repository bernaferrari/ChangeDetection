package com.bernaferrari.changedetection.ui.logs

import androidx.compose.runtime.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import com.bernaferrari.changedetection.localization.stringResource
import kotlin.time.Clock

/** Calendar dates, rather than elapsed 24-hour periods, handle DST and year boundaries. */
@Composable
internal fun historyDateLabel(date: String, today: LocalDate): String {
    val day = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() } ?: return date
    return when (day.daysUntil(today)) {
        0 -> stringResource(Res.string.history_today)

        1 -> stringResource(Res.string.history_yesterday)

        else -> {
            val month = stringArrayResource(Res.array.history_months)[day.month.ordinal]
            if (day.year == today.year) {
                stringResource(Res.string.history_calendar_day, month, day.day)
            } else {
                stringResource(Res.string.history_calendar_day_year, month, day.day, day.year)
            }
        }
    }
}

@Composable
internal fun rememberToday(): LocalDate {
    // Keep an open history screen honest across midnight and a system timezone change.
    fun currentDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val today by produceState(currentDate()) {
        while (true) {
            delay(60_000)
            value = currentDate()
        }
    }
    return today
}
