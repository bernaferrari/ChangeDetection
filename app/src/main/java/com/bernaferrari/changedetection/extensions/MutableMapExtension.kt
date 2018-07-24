package com.bernaferrari.changedetection.extensions

import com.bernaferrari.changedetection.detailsText.ItemSelected

/**
 * Returns a new map containing the first value it finds.
 **/
fun <K, V> Map<out K, V>.firstKey(): K? {
    for ((key, _) in this) {
        return key
    }
    return null
}

internal fun <K, V> Map<out K, V>.getPositionForAdapter(color: ItemSelected): K? {
    return this.filter { it.value == color }.firstKey()
}