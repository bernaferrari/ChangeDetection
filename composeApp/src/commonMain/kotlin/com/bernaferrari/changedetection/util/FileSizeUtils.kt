package com.bernaferrari.changedetection.util

import kotlin.math.log10
import kotlin.math.pow

/**
 * Converts a file size in bytes to a human-readable string format.
 */
fun Long.readableFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    val value = this / 1024.0.pow(digitGroups.toDouble())
    val roundedValue = (value * 10).toInt() / 10.0
    return "$roundedValue ${units[digitGroups]}"
}

