/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import com.bernaferrari.changedetection.repo.SiteAndLastSnap

internal enum class MonitorFilter { All, Changed, Errors, Paused }
internal data class MonitorCounts(val all: Int, val changed: Int, val errors: Int, val paused: Int, val waiting: Int)

/** Global monitoring health stays separate from the search scope shown by filter chips. */
internal data class WatchlistProjection(
    val totalCounts: MonitorCounts,
    val matchingCounts: MonitorCounts,
    val visibleSites: List<SiteAndLastSnap>,
)

internal fun projectWatchlist(sites: List<SiteAndLastSnap>, query: String, filter: MonitorFilter, failedSiteIds: Set<String>): WatchlistProjection {
    val search = query.trim()
    val matching = if (search.isEmpty()) {
        sites
    } else {
        sites.filter {
            it.site.title.orEmpty().contains(search, ignoreCase = true) ||
                it.site.url.contains(search, ignoreCase = true) ||
                it.site.notes.contains(search, ignoreCase = true)
        }
    }
    val total = sites.counts(failedSiteIds)
    return WatchlistProjection(
        totalCounts = total,
        matchingCounts = if (matching === sites) total else matching.counts(failedSiteIds),
        visibleSites = matching.filter {
            when (filter) {
                MonitorFilter.All -> true
                MonitorFilter.Changed -> it.hasDetectedChange()
                MonitorFilter.Errors -> it.site.id in failedSiteIds
                MonitorFilter.Paused -> !it.site.isSyncEnabled
            }
        }.sortedBy { it.site.id }, // Checks/read changes must not move otherwise visible rows.
    )
}

private fun List<SiteAndLastSnap>.counts(failedSiteIds: Set<String>) = MonitorCounts(
    all = size,
    changed = count { it.hasDetectedChange() },
    errors = count { it.site.id in failedSiteIds },
    paused = count { !it.site.isSyncEnabled },
    waiting = count { it.site.isSyncEnabled && it.snap == null && it.snapshotCount == 0 },
)
