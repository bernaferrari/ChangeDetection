/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

/**
 * Cross-platform interface for background work scheduling.
 *
 * Android: Implemented using WorkManager
 * iOS: Implemented using BGTaskScheduler
 */
interface BackgroundScheduler {
    /**
     * Schedule periodic background sync for all enabled sites.
     * @param intervalMinutes Minimum interval between syncs (min 15 minutes on both platforms)
     * @param constraints Work constraints (wifi, charging, etc.)
     */
    fun schedulePeriodicSync(
        intervalMinutes: Long,
        constraints: SyncConstraints = SyncConstraints(),
    )

    /**
     * Cancel all periodic background sync.
     */
    fun cancelPeriodicSync()

    /**
     * Request an immediate sync for a specific site.
     * @param siteId The site ID to sync
     */
    fun requestImmediateSync(siteId: String)

    /**
     * Start quick monitoring mode for rapid polling.
     * @param siteIds Sites to monitor
     * @param intervalSeconds Interval between checks (platform may enforce minimums)
     */
    fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int = 15,
    )

    /**
     * Stop quick monitoring mode.
     */
    fun stopQuickMonitor()

    /**
     * Check if periodic sync is currently scheduled.
     */
    fun isPeriodicSyncScheduled(): Boolean
}

/**
 * Constraints for background sync work.
 * Both Android and iOS respect these constraints (to varying degrees).
 */
data class SyncConstraints(
    val requiresNetwork: Boolean = true,
    val requiresWifi: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
)

/**
 * Platform-specific factory to get the BackgroundScheduler implementation.
 */
expect fun createBackgroundScheduler(): BackgroundScheduler
