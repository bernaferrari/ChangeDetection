package com.bernaferrari.changedetection.extensions

import com.github.marlonlom.utilities.timeago.TimeAgo

actual fun Long.convertTimestampToDate(): String {
    return if (this > 0) {
        TimeAgo.using(this)
    } else {
        "-"
    }
}
