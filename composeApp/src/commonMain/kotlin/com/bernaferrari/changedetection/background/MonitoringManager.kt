package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.flow.Flow

interface MonitoringManager {
    fun getSyncStatusFlow(): Flow<List<SyncJobInfo>>
    fun reloadSite(site: Site)
    fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int)
    fun stopQuickMonitor()
    suspend fun updateWorkerWithConstraints(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        cancelCurrentWork: Boolean = true
    )

    fun cancelWork()
}

data class SyncJobInfo(
    val state: SyncState,
    val tags: Set<String>
)

enum class SyncState {
    RUNNING, ENQUEUED, SUCCEEDED, FAILED, CANCELLED, BLOCKED;

    fun isRunningOrEnqueued(): Boolean = this == RUNNING || this == ENQUEUED
}
