package com.bernaferrari.base.misc

private const val KILOBYTE = 1_024L
private const val MEGABYTE = 1_048_576L
private const val GIGABYTE = 1_073_741_824L

/** Converts byte length into a human-readable unit string, like 5GB. */
fun Long.friendlySize(): String {
    return when {
        this >= GIGABYTE -> "${this / GIGABYTE}GB"
        this >= MEGABYTE -> "${this / MEGABYTE}MB"
        this >= KILOBYTE -> "${this / KILOBYTE}KB"
        else -> "${this}B"
    }
}

/**
 * provides a String representation of the given time
 * @return `millis` in hh:mm:ss format
 */
fun Long.formatTime(): String {
    return when {
        this % 86400 / 3600 > 0 -> String.format(
            "%02d:%02d:%02d",
            this % 86400 / 3600,
            this % 3600 / 60,
            this % 60
        )
        this % 3600 / 60 > 0 -> String.format("%d:%02d", this % 3600 / 60, this % 60)
        else -> String.format("0:%02d", this % 60)
    }
}

