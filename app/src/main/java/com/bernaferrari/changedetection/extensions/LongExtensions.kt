package com.bernaferrari.changedetection.extensions

import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import java.util.*

fun Long.convertTimestampToDate(): String {
    val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
    return TimeAgo.using(this, messages)
}