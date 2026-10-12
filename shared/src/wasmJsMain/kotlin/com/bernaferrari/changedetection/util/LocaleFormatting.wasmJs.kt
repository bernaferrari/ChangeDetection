@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.bernaferrari.changedetection.util

import kotlin.js.JsString

private fun formatDateJs(timestamp: Double, seconds: Boolean, withDate: Boolean): JsString = js("new Intl.DateTimeFormat(navigator.language, withDate ? { dateStyle: 'medium', timeStyle: seconds ? 'medium' : 'short' } : { timeStyle: seconds ? 'medium' : 'short' }).format(new Date(timestamp))")
private fun formatNumberJs(value: Double, maxDigits: Int, minDigits: Int): JsString = js("new Intl.NumberFormat(navigator.language, {useGrouping: false, maximumFractionDigits: maxDigits, minimumFractionDigits: minDigits}).format(value)")
private fun formatCalendarDateJs(timestamp: Double): JsString = js("new Intl.DateTimeFormat(navigator.language, {dateStyle: 'medium'}).format(new Date(timestamp))")
internal actual fun localizedCalendarDate(timestamp: Long): String = formatCalendarDateJs(timestamp.toDouble()).toString()
internal actual fun localizedTimestamp(timestamp: Long, includeSeconds: Boolean): String = formatDateJs(timestamp.toDouble(), includeSeconds, true).toString()
internal actual fun localizedTime(timestamp: Long, includeSeconds: Boolean): String = formatDateJs(timestamp.toDouble(), includeSeconds, false).toString()
internal actual fun localizedNumber(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = formatNumberJs(value, maxFractionDigits, minFractionDigits).toString()

private fun formatPercentJs(value: Double, maxDigits: Int, minDigits: Int): JsString = js("new Intl.NumberFormat(navigator.language, {style: 'percent', maximumFractionDigits: maxDigits, minimumFractionDigits: minDigits}).format(value / 100)")
internal actual fun localizedPercent(value: Double, maxFractionDigits: Int, minFractionDigits: Int): String = formatPercentJs(value, maxFractionDigits, minFractionDigits).toString()
