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

/** A saved change stays visible until the user explicitly marks its comparison as read. */
internal fun SiteAndLastSnap.hasDetectedChange(): Boolean = snapshotCount > 1 && snap != null && !site.isRead
