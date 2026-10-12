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
    private val runner: com.bernaferrari.changedetection.sync.IosCheckRunner,
) : MonitoringManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val backgroundScheduler = createBackgroundScheduler()

    private val turboSession = TurboSession(scope, runner)
    override val quickMonitorState = turboSession.state
    private val _syncStatus = MutableStateFlow<List<SyncJobInfo>>(emptyList())

    init {
        scope.launch {
            persistTurboSession(turboSession, dataStore, onFailure = {
                Napier.w("Could not save Turbo state; retrying", it)
            })
        }
    }

    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = _syncStatus.asStateFlow()

    override fun reloadSite(site: Site) {
        Napier.d("iOS: Reloading site ${site.id}")
        scope.launch {
            updateSyncStatus(site.id, SyncState.RUNNING)
            try {
                val result = runner.check(site.id)
                updateSyncStatus(site.id, if (result is com.bernaferrari.changedetection.sync.SyncResult.Success) SyncState.SUCCEEDED else SyncState.FAILED)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Napier.e("iOS: Failed to sync site ${site.id}", e)
                updateSyncStatus(site.id, SyncState.FAILED)
            }
        }
    }

    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) {
        turboSession.start(siteIds.toSet(), intervalSeconds, durationMinutes)
    }

    override fun stopQuickMonitor() {
        turboSession.stop()
        backgroundScheduler.stopQuickMonitor()
    }

    override suspend fun updateSchedule() {
        val schedule = MonitoringSchedule.from(dataStore.data.first())
        if (schedule.enabled) {
            backgroundScheduler.schedulePeriodicSync(
                intervalMinutes = schedule.intervalMinutes,
                constraints = SyncConstraints(),
            )
        } else {
            backgroundScheduler.cancelPeriodicSync()
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
