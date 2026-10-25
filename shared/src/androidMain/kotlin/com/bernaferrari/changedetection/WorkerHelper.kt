package com.bernaferrari.changedetection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.SyncJobInfo
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkerHelper(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val session: com.bernaferrari.changedetection.background.TurboSession,
    private val applicationScope: com.bernaferrari.changedetection.util.ApplicationScope,
) : MonitoringManager {
    private val scheduler = com.bernaferrari.changedetection.background.AndroidMonitoringScheduler(WorkManager.getInstance(context), dataStore)
    override val quickMonitorState = session.state

    init {
        WorkManager.getInstance(context).cancelAllWorkByTag("quick_monitor")
        applicationScope.scope.launch(Dispatchers.Main.immediate) {
            com.bernaferrari.changedetection.background.persistTurboSession(
                session = session,
                preferences = dataStore,
                afterPublish = { com.bernaferrari.changedetection.widgets.QuickMonitorWidget().updateAll(context) },
                onFailure = { appLog.w("Could not publish Turbo state; retrying", it) },
            )
        }
    }

    companion object {
        const val UNIQUEWORK = com.bernaferrari.changedetection.background.AndroidMonitoringScheduler.WORK_NAME
        const val TURBO_WORK = "turbo_work"
        const val WIFI = "wifi"
        const val CHARGING = "charging"
        const val BATTERYNOTLOW = "batteryNotLow"
        const val IDLE = "idle"
        const val DELAY = "delay"
        const val BACKGROUND_SYNC = "backgroundSync"
    }

    override suspend fun updateSchedule() = scheduler.reconcile()

    override fun cancelWork() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUEWORK)
    }

    fun getWorkInfoFlow() = WorkManager.getInstance(context).getWorkInfosByTagFlow(UNIQUEWORK)

    override fun reloadSite(site: Site) {
        appLog.d("Scheduling one-time work for site: ${site.id}")

        val work =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf("id" to site.id, "isSingle" to true))
                .addTag("singleEvent")
                .addTag("reload_${site.id}")
                .addTag(site.id)
                .addTag(UNIQUEWORK)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        WorkManager
            .getInstance(context)
            .beginUniqueWork("reload_${site.id}", ExistingWorkPolicy.REPLACE, work)
            .enqueue()

        appLog.d("One-time work scheduled for site: ${site.id}")
    }

    override fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int,
        durationMinutes: Int,
    ) {
        appLog.d("Starting Quick Monitor for ${siteIds.size} sites: $siteIds (${intervalSeconds}s interval)")

        try {
            if (siteIds.isEmpty()) {
                appLog.e("Cannot start Quick Monitor with empty site list")
                return
            }

            QuickMonitorService.start(context, siteIds, intervalSeconds, durationMinutes)
            appLog.d("QuickMonitorService.start() called successfully")
        } catch (e: Exception) {
            appLog.e("Failed to start QuickMonitorService: ${e.message}")
            com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, e, stage = com.bernaferrari.changedetection.diagnostics.AppEventStage.TURBO_LAUNCH)
            throw e
        }
    }

    override fun stopQuickMonitor() {
        appLog.d("Stopping Quick Monitor")
        WorkManager.getInstance(context).cancelAllWorkByTag("quick_monitor")
        session.stop()
        QuickMonitorService.stop(context)
    }

    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = getWorkInfoFlow().map { workInfoList ->
        // getWorkInfosByTagFlow includes terminal history. Surfacing those rows
        // made an old failed request keep a recovered page red indefinitely.
        // Durable success/failure lives on Site; this flow represents live work.
        workInfoList.filter { !it.state.isFinished }.map { workInfo ->
            SyncJobInfo(
                state =
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.ENQUEUED -> SyncState.ENQUEUED
                    androidx.work.WorkInfo.State.RUNNING -> SyncState.RUNNING
                    androidx.work.WorkInfo.State.SUCCEEDED -> SyncState.SUCCEEDED
                    androidx.work.WorkInfo.State.FAILED -> SyncState.FAILED
                    androidx.work.WorkInfo.State.BLOCKED -> SyncState.BLOCKED
                    androidx.work.WorkInfo.State.CANCELLED -> SyncState.CANCELLED
                },
                isPeriodic = workInfo.periodicityInfo != null,
                nextEligibleTime = workInfo.nextScheduleTimeMillis.takeIf { workInfo.periodicityInfo != null && it != Long.MAX_VALUE },
                tags = workInfo.tags.filter { it.startsWith("reload_") }.mapTo(mutableSetOf()) { it.removePrefix("reload_") },
            )
        }
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("WorkerHelper")
