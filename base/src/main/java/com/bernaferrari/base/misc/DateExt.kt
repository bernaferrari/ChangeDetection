package com.bernaferrari.base.misc

import java.text.SimpleDateFormat
import java.util.*

/** Converts a Date to a timestamp string that can be used for file names, etc. */
fun Date.timestampString(): String {
    if (time <= 0) {
        return "Invalid"
    }
    val df = SimpleDateFormat("MMMMd-hhmmssa", Locale.US)
    return df.format(this)
}

/** Converts milliseconds into a human readable date string. */
fun Long.friendlyDate(): String {
    if (this <= 0) {
        return "Invalid"
    }
    val df = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
    return df.format(this)
}
