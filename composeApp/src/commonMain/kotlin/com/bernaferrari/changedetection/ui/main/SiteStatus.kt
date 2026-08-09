/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.util.getRelativeTimeLabel

sealed interface SiteStatus {
    data object Checking : SiteStatus
    data object Error : SiteStatus
    data object Paused : SiteStatus
    data class Changed(val ago: String) : SiteStatus
    data class Unchanged(val lastCheck: String) : SiteStatus
}

@Composable
internal fun SiteAndLastSnap.toSiteStatus(
    isLoading: Boolean,
    hasError: Boolean,
): SiteStatus = when {
    isLoading || isSyncing -> SiteStatus.Checking

    hasError || !site.isSuccessful -> SiteStatus.Error

    !site.isSyncEnabled -> SiteStatus.Paused

    hasDetectedChange() -> SiteStatus.Changed(
        ago = getRelativeTimeLabel(requireNotNull(snap).timestamp),
    )

    else -> SiteStatus.Unchanged(
        lastCheck = site.timestamp.takeIf { it > 0 }?.let { timestamp ->
            getRelativeTimeLabel(timestamp)
        }.orEmpty(),
    )
}

/** A snapshot written during the latest successful check means that check found new content. */
internal fun SiteAndLastSnap.hasDetectedChange(): Boolean = site.isSuccessful && snap != null && snap.timestamp >= site.timestamp - RECENT_CHANGE_WINDOW_MS

private const val RECENT_CHANGE_WINDOW_MS = 60_000L
