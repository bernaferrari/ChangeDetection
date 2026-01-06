package com.bernaferrari.changedetection.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS actual implementation for getting current time.
 */
actual fun platformCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
