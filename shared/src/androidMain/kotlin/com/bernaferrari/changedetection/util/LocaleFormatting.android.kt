/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import android.content.Context
import android.text.format.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.mp.KoinPlatformTools

private fun appContext(): Context? = KoinPlatformTools.defaultContext().getOrNull()?.getOrNull<Context>()
private fun displayLocale(): Locale = appContext()?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
private fun timeSkeleton(includeSeconds: Boolean): String {
    val twentyFourHour = appContext()?.let(DateFormat::is24HourFormat) ?: false
    return (if (twentyFourHour) "Hm" else "hm") + if (includeSeconds) "s" else ""
}
private fun displayDate(timestamp: Long, skeleton: String): String = displayLocale().let { locale -> SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(timestamp)) }
internal actual fun localizedTimestamp(timestamp: Long, includeSeconds: Boolean): String = if (appContext() == null) java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, if (includeSeconds) java.text.DateFormat.MEDIUM else java.text.DateFormat.SHORT, displayLocale()).format(Date(timestamp)) else displayDate(timestamp, "yMMMd" + timeSkeleton(includeSeconds))
internal actual fun localizedTime(timestamp: Long, includeSeconds: Boolean): String = if (appContext() == null) java.text.DateFormat.getTimeInstance(if (includeSeconds) java.text.DateFormat.MEDIUM else java.text.DateFormat.SHORT, displayLocale()).format(Date(timestamp)) else displayDate(timestamp, timeSkeleton(includeSeconds))
internal actual fun localizedNumber(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NumberFormat.getNumberInstance(displayLocale()).apply {
    isGroupingUsed = false
    maximumFractionDigits = maxFractionDigits
    minimumFractionDigits = minFractionDigits
}.format(value)

internal actual fun localizedPercent(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NumberFormat.getPercentInstance(displayLocale()).apply {
    isGroupingUsed = false
    maximumFractionDigits = maxFractionDigits
    minimumFractionDigits = minFractionDigits
}.format(value / 100)
