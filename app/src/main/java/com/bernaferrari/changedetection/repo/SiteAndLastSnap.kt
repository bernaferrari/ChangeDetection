package com.bernaferrari.changedetection.repo

data class SiteAndLastSnap(val site: Site, val snap: Snap?, val isSyncing: Boolean = false)
