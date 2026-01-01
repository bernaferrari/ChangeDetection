package com.bernaferrari.changedetection.extensions

/**
 * Platform-specific timestamp to human-readable "time ago" string.
 */
expect fun Long.convertTimestampToDate(): String
