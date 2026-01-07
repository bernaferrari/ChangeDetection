package com.bernaferrari.changedetection.util


import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    
    return "$month $day, $hour:$minute:$second"
}

fun getRelativeTimeLabel(timestamp: Long): String {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days day ago"
    }
}

fun getCurrentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
