package com.bernaferrari.changedetection.util

import platform.Foundation.*

private fun displayLocale(): NSLocale {
    val language = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String ?: "en"
    val region = NSLocale.currentLocale.countryCode
    return NSLocale.localeWithLocaleIdentifier(if (region != null) "$language-$region" else language)
}
private fun dateFormatter(includeSeconds: Boolean): NSDateFormatter = NSDateFormatter().apply {
    locale = displayLocale()
    timeStyle = if (includeSeconds) NSDateFormatterMediumStyle else NSDateFormatterShortStyle
}
internal actual fun localizedCalendarDate(timestamp: Long): String = NSDateFormatter().apply {
    locale = displayLocale()
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterNoStyle
}.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0))
internal actual fun localizedTimestamp(timestamp: Long, includeSeconds: Boolean): String = dateFormatter(includeSeconds).apply { dateStyle = NSDateFormatterMediumStyle }.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0))
internal actual fun localizedTime(timestamp: Long, includeSeconds: Boolean): String = dateFormatter(includeSeconds).stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0))
internal actual fun localizedNumber(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NSNumberFormatter().apply {
    locale = displayLocale()
    numberStyle = NSNumberFormatterDecimalStyle
    usesGroupingSeparator = false
    maximumFractionDigits = maxFractionDigits.toULong()
    minimumFractionDigits = minFractionDigits.toULong()
}.stringFromNumber(NSNumber(value)) ?: value.toString()

internal actual fun localizedPercent(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = NSNumberFormatter().apply {
    locale = displayLocale()
    numberStyle = NSNumberFormatterPercentStyle
    maximumFractionDigits = maxFractionDigits.toULong()
    minimumFractionDigits = minFractionDigits.toULong()
}.stringFromNumber(NSNumber(value / 100)) ?: "$value%"
