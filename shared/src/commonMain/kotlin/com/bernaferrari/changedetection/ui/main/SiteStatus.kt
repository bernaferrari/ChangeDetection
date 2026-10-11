/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.util.getRelativeTimeLabel

sealed interface SiteStatus {
    data object Waiting : SiteStatus
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

    snap == null && snapshotCount == 0 -> SiteStatus.Waiting

    hasDetectedChange() -> SiteStatus.Changed(
        ago = getRelativeTimeLabel(requireNotNull(snap).timestamp),
    )

    else -> SiteStatus.Unchanged(
        lastCheck = site.timestamp.takeIf { it > 0 }?.let { timestamp ->
            getRelativeTimeLabel(timestamp)
        }.orEmpty(),
    )
}

/** A saved change stays visible until the user explicitly marks its comparison as read. */
internal fun SiteAndLastSnap.hasDetectedChange(): Boolean = snapshotCount > 1 && snap != null && !site.isRead
