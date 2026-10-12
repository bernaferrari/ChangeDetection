package com.bernaferrari.changedetection.background

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Desktop development host executes real checks while open; it does not emulate Android services. */
class DesktopMonitoringManager(
    private val engine: SyncEngine,
    private val preferences: DataStore<Preferences>,
    applicationScope: CoroutineScope,
) : MonitoringManager {
    private val scope = CoroutineScope(applicationScope.coroutineContext + Dispatchers.Main.immediate)
    private val session = TurboSession(scope, CheckRunner { engine.syncSite(it) })
    override val quickMonitorState = session.state
    init {
        scope.launch {
            persistTurboSession(session, preferences, onFailure = {
                io.github.aakira.napier.Napier.w("Could not save Turbo state; retrying", it)
            })
        }
    }
    private val jobs = MutableStateFlow<List<SyncJobInfo>>(emptyList())
    private var periodic: Job? = null
    private var policy: MonitoringSchedule? = null
    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = jobs
    override fun reloadSite(site: Site) {
        scope.launch {
            jobs.update { it.filterNot { job -> site.id in job.tags } + SyncJobInfo(SyncState.RUNNING, setOf(site.id)) }
            val outcome = try {
                engine.syncSite(site.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            jobs.update { it.filterNot { job -> site.id in job.tags } + SyncJobInfo(if (outcome is SyncResult.Success) SyncState.SUCCEEDED else SyncState.FAILED, setOf(site.id)) }
        }
    }
    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = session.start(siteIds.toSet(), intervalSeconds, durationMinutes)
    override fun stopQuickMonitor() = session.stop()
    override suspend fun updateSchedule() {
        val next = MonitoringSchedule.from(preferences.data.first())
        if (next == policy && periodic?.isActive == true) return
        periodic?.cancelAndJoin()
        policy = next
        jobs.update { it.filterNot(SyncJobInfo::isPeriodic) }
        if (!next.enabled) return
        periodic = scope.launch {
            while (isActive) {
                jobs.update {
                    it.filterNot(SyncJobInfo::isPeriodic) + SyncJobInfo(
                        SyncState.ENQUEUED,
                        emptySet(),
                        isPeriodic = true,
                        nextEligibleTime = System.currentTimeMillis() + next.intervalMinutes * 60_000,
                    )
                }
                delay(next.intervalMinutes * 60_000)
                jobs.update { it.filterNot(SyncJobInfo::isPeriodic) + SyncJobInfo(SyncState.RUNNING, emptySet(), isPeriodic = true) }
                try {
                    engine.syncAllSites()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { }
                jobs.update { it.filterNot(SyncJobInfo::isPeriodic) }
            }
        }
    }
    override fun cancelWork() {
        periodic?.cancel()
        periodic = null
        policy = null
        jobs.update { it.filterNot(SyncJobInfo::isPeriodic) }
        stopQuickMonitor()
    }
}

actual fun createBackgroundScheduler(): BackgroundScheduler = object : BackgroundScheduler {
    // Shared production UI uses MonitoringManager. This legacy adapter is not an OS scheduler.
    override fun schedulePeriodicSync(intervalMinutes: Long, constraints: SyncConstraints) = Unit
    override fun cancelPeriodicSync() = Unit
    override fun requestImmediateSync(siteId: String) = Unit
    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int) = Unit
    override fun stopQuickMonitor() = Unit
    override fun isPeriodicSyncScheduled() = false
}
