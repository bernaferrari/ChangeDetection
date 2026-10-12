package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.flow.Flow

interface MonitoringManager {
    val quickMonitorState: kotlinx.coroutines.flow.StateFlow<TurboState>

    fun getSyncStatusFlow(): Flow<List<SyncJobInfo>>

    fun reloadSite(site: Site)

    fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int,
        durationMinutes: Int = 180,
    )

    fun stopQuickMonitor()

    /** Reconcile persistent scheduling with the stored policy without aborting an active check. */
    suspend fun updateSchedule()

    fun cancelWork()
}

data class SyncJobInfo(
    val state: SyncState,
    val tags: Set<String>,
    val isPeriodic: Boolean = false,
    /** Earliest eligibility, not a promised execution time. */
    val nextEligibleTime: Long? = null,
)

enum class SyncState {
    RUNNING,
    ENQUEUED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BLOCKED,
    ;

    fun isRunningOrEnqueued(): Boolean = this == RUNNING || this == ENQUEUED
}
