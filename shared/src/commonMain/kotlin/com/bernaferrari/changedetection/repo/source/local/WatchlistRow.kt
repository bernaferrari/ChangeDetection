package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.Embedded
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.SiteAndLastSnap

/** Database projection; a page without history has no snapshot and a zero count. */
data class WatchlistRow(
    @Embedded val site: Site,
    @Embedded(prefix = "latest_") val snap: Snap?,
    val snapshotCount: Int,
) {
    fun toSiteAndLastSnap() = SiteAndLastSnap(site, snap, snapshotCount)
}
