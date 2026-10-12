package com.bernaferrari.changedetection.repo

data class SiteAndLastSnap(
    val site: Site,
    val snap: Snap?,
    val snapshotCount: Int = 0,
    val isSyncing: Boolean = false,
)
