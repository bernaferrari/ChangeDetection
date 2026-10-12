package com.bernaferrari.changedetection.background

private object DemoBackgroundScheduler : BackgroundScheduler {
    override fun schedulePeriodicSync(intervalMinutes: Long, constraints: SyncConstraints) = Unit
    override fun cancelPeriodicSync() = Unit
    override fun requestImmediateSync(siteId: String) = Unit
    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int) = Unit
    override fun stopQuickMonitor() = Unit
    override fun isPeriodicSyncScheduled(): Boolean = false
}

actual fun createBackgroundScheduler(): BackgroundScheduler = DemoBackgroundScheduler
