/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

internal actual fun localizedCalendarDate(timestamp: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
internal actual fun localizedTimestamp(timestamp: Long, includeSeconds: Boolean): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, if (includeSeconds) DateFormat.MEDIUM else DateFormat.SHORT, Locale.getDefault()).format(Date(timestamp))
internal actual fun localizedTime(timestamp: Long, includeSeconds: Boolean): String = DateFormat.getTimeInstance(if (includeSeconds) DateFormat.MEDIUM else DateFormat.SHORT, Locale.getDefault()).format(Date(timestamp))
internal actual fun localizedNumber(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    isGroupingUsed = false
    maximumFractionDigits = maxFractionDigits
    minimumFractionDigits = minFractionDigits
}.format(value)

internal actual fun localizedPercent(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NumberFormat.getPercentInstance(Locale.getDefault()).apply {
    isGroupingUsed = false
    maximumFractionDigits = maxFractionDigits
    minimumFractionDigits = minFractionDigits
}.format(value / 100)
