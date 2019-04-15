package com.bernaferrari.base.misc

import android.content.res.Resources

/**
 * If the receiver equals [whenIs], then return [shouldReturn]. Else return the value of
 * the receiver.
 */
fun Int.otherwise(
    whenIs: Int = 0,
    shouldReturn: Int
) = if (this == whenIs) {
    shouldReturn
} else {
    this
}

/** Convert dp to pixel. For example, 8dp becomes 24 */
fun Int.toDp(resources: Resources): Int {
    return (resources.displayMetrics.density * this).toInt()
}