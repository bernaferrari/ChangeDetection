package com.bernaferrari.changedetection.background

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation of BackgroundScheduler using WorkerHelper.
 */
actual fun createBackgroundScheduler(): BackgroundScheduler = AndroidBackgroundScheduler()

class AndroidBackgroundScheduler :
    BackgroundScheduler,
    KoinComponent {
    private val monitoringManager: MonitoringManager by inject()

    override fun schedulePeriodicSync(
        intervalMinutes: Long,
        constraints: SyncConstraints,
    ) {
        // Handled via WorkerHelper / MonitoringManager through Koin
    }

    override fun cancelPeriodicSync() {
        monitoringManager.cancelWork()
    }

    override fun requestImmediateSync(siteId: String) {
        // WorkManager handles immediate sync via WorkerHelper
    }

    override fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int,
    ) {
        monitoringManager.startQuickMonitor(siteIds, intervalSeconds)
    }

    override fun stopQuickMonitor() {
        monitoringManager.stopQuickMonitor()
    }

    override fun isPeriodicSyncScheduled(): Boolean {
        return false // Could be implemented via WorkManager query
    }
}
