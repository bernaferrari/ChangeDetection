package com.bernaferrari.changedetection.extensions

import com.bernaferrari.changedetection.util.getRelativeTimeLabel

fun Long.convertTimestampToDate(): String = getRelativeTimeLabel(this)
