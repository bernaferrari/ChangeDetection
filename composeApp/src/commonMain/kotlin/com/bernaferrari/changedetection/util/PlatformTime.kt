package com.bernaferrari.changedetection.util

/**
 * Expect function for getting current time in milliseconds.
 * Platform implementations use native time APIs.
 */
expect fun platformCurrentTimeMillis(): Long
