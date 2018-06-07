package com.bernaferrari.changedetection.data

// Same as Snap, but without value, which can be heavy and can cause OOM error on CursorAdapter when
// fetching too many items.
data class MinimalSnap(
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentType: String,
    val contentSize: Int
)