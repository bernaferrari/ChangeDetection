package com.bernaferrari.changedetection.extensions

import com.github.marlonlom.utilities.timeago.TimeAgo

internal fun Long.convertTimestampToDate(): String = TimeAgo.using(this)
