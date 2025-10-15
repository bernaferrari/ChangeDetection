package com.bernaferrari.changedetection.extensions

// Simplified placeholder - remove dependency on deleted ItemSelected
// This class appears to be unused in current codebase

/**
 * Returns a new map containing the first value it finds.
 **/
fun <K, V> Map<out K, V>.firstKey(): K? {
    for ((key, _) in this) {
        return key
    }
    return null
}
