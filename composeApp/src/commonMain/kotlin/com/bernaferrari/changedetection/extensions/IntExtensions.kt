/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.extensions

import kotlin.math.log10
import kotlin.math.pow

fun Int.readableFileSize(): String {
    if (this <= 0) return "EMPTY"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    val size = this / 1024.0.pow(digitGroups.toDouble())
    return "${formatDecimal(size)} ${units[digitGroups]}"
}

private fun formatDecimal(value: Double): String {
    // Simple formatting without java.text.DecimalFormat
    val rounded = (value * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
