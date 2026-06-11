/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.SyncEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit

/**
 * iOS implementation of MonitoringManager.
 *
 * Uses a combination of:
 * - BGTaskScheduler for background periodic sync (via IosBackgroundScheduler)
 * - Foreground coroutines for quick monitor mode
 * - SyncEngine for actual network operations
 */
class IosMonitoringManager(
    private val syncEngine: SyncEngine,
    private val dataStore: DataStore<Preferences>,
) : MonitoringManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val backgroundScheduler = createBackgroundScheduler()

    private var quickMonitorJob: Job? = null
    private val _syncStatus = MutableStateFlow<List<SyncJobInfo>>(emptyList())

    companion object {
        private val BACKGROUND_SYNC_KEY = booleanPreferencesKey("background_sync_enabled")
    }

    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = _syncStatus.asStateFlow()

    override fun reloadSite(site: Site) {
        Napier.d("iOS: Reloading site ${site.id}")
        scope.launch {
            updateSyncStatus(site.id, SyncState.RUNNING)
            try {
                syncEngine.syncSite(site.id)
                updateSyncStatus(site.id, SyncState.SUCCEEDED)
            } catch (e: Exception) {
                Napier.e("iOS: Failed to sync site ${site.id}", e)
                updateSyncStatus(site.id, SyncState.FAILED)
            }
        }
    }

    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int) {
        Napier.d("iOS: Starting quick monitor for ${siteIds.size} sites")

        quickMonitorJob?.cancel()
        quickMonitorJob = scope.launch {
            while (isActive) {
                siteIds.forEach { siteId ->
                    try {
                        updateSyncStatus(siteId, SyncState.RUNNING)
                        syncEngine.syncSite(siteId)
                        updateSyncStatus(siteId, SyncState.SUCCEEDED)
                    } catch (e: Exception) {
                        Napier.e("iOS: Quick monitor failed for $siteId", e)
                        updateSyncStatus(siteId, SyncState.FAILED)
                    }
                    delay(500L) // Small delay between sites
                }

                val delayMs = (intervalSeconds * 1000L).coerceAtLeast(5000L)
                delay(delayMs)
            }
        }

        // Also schedule background task as backup
        backgroundScheduler.startQuickMonitor(siteIds, intervalSeconds)
    }

    override fun stopQuickMonitor() {
        Napier.d("iOS: Stopping quick monitor")
        quickMonitorJob?.cancel()
        quickMonitorJob = null
        backgroundScheduler.stopQuickMonitor()
    }

    override suspend fun updateWorkerWithConstraints(
        dataStore: DataStore<Preferences>,
        cancelCurrentWork: Boolean,
    ) {
        val preferences = dataStore.data.first()
        val isEnabled = preferences[BACKGROUND_SYNC_KEY] ?: false

        if (cancelCurrentWork) {
            cancelWork()
        }

        if (isEnabled) {
            // Schedule periodic sync using BGTaskScheduler
            // iOS enforces minimum 15 minute intervals
            backgroundScheduler.schedulePeriodicSync(
                intervalMinutes = 15,
                constraints = SyncConstraints(),
            )
        }
    }

    override fun cancelWork() {
        backgroundScheduler.cancelPeriodicSync()
        stopQuickMonitor()
    }

    private fun updateSyncStatus(siteId: String, state: SyncState) {
        // Use update for atomic modification
        _syncStatus.update { currentList ->
            val mutableList = currentList.toMutableList()
            val existingIndex = mutableList.indexOfFirst { siteId in it.tags }

            val newInfo = SyncJobInfo(state, setOf(siteId))

            if (existingIndex >= 0) {
                mutableList[existingIndex] = newInfo
            } else {
                mutableList.add(newInfo)
            }
            mutableList.toList()
        }

        // Auto-clear non-running states after a delay
        if (state != SyncState.RUNNING && state != SyncState.ENQUEUED) {
            scope.launch {
                delay(3000L)
                _syncStatus.update { currentList ->
                    currentList.filterNot { siteId in it.tags && it.state == state }
                }
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
