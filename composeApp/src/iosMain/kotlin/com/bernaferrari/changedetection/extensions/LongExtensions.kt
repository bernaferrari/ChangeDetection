package com.bernaferrari.changedetection.extensions

import platform.Foundation.*

actual fun Long.convertTimestampToDate(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this.toDouble() / 1000.0)
    val now = NSDate()
    val seconds = now.timeIntervalSinceDate(date)

    return when {
        seconds < 60 -> "Just now"
        seconds < 3600 -> "${(seconds / 60).toInt()}m ago"
        seconds < 86400 -> "${(seconds / 3600).toInt()}h ago"
        seconds < 2592000 -> "${(seconds / 86400).toInt()}d ago"
        else -> {
            val formatter = NSDateFormatter()
            formatter.dateFormat = "MMM dd, yyyy"
            formatter.stringFromDate(date)
        }
    }
}
